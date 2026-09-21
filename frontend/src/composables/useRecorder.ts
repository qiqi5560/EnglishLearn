import { ref } from 'vue'

/**
 * 麦克风录音：采集原始 PCM 并编码成 WAV，供后端做音素级评测。
 *
 * 为什么不直接用 MediaRecorder 的 webm：后端是纯 Java，解码 webm/opus 需要额外依赖。
 * 这里走 Web Audio 直接拿到 Float32 采样，自己封成 16bit PCM WAV，后端零依赖即可解析，
 * 同时顺手把采样率统一到 16kHz（音素模型的要求）。
 */

/** 音素模型要求的采样率 */
const TARGET_RATE = 16000

export interface RecordingResult {
  blob: Blob
  durationMs: number
}

export function useRecorder() {
  const isRecording = ref(false)

  let ctx: AudioContext | null = null
  let stream: MediaStream | null = null
  let processor: ScriptProcessorNode | null = null
  let chunks: Float32Array[] = []
  let startedAt = 0

  /** 开始录音。拿不到麦克风时返回 false，调用方应回退到纯文本评分 */
  async function start(): Promise<boolean> {
    if (!navigator.mediaDevices?.getUserMedia) return false
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true },
      })
    } catch {
      return false
    }
    try {
      const Ctx = window.AudioContext || (window as any).webkitAudioContext
      // 指定采样率并非所有浏览器都支持，实际以 ctx.sampleRate 为准，后端会做重采样兜底
      ctx = new Ctx({ sampleRate: TARGET_RATE })
      const source = ctx.createMediaStreamSource(stream)
      processor = ctx.createScriptProcessor(4096, 1, 1)
      chunks = []
      processor.onaudioprocess = (e: AudioProcessingEvent) => {
        // 复制一份：AudioBuffer 是复用的，直接持有会被后续帧覆盖
        chunks.push(new Float32Array(e.inputBuffer.getChannelData(0)))
      }
      source.connect(processor)
      // ScriptProcessor 必须接到 destination 才会触发 onaudioprocess
      processor.connect(ctx.destination)
      startedAt = Date.now()
      isRecording.value = true
      return true
    } catch {
      release()
      return false
    }
  }

  /** 结束录音并返回 WAV；没有录到内容时返回 null */
  function stop(): RecordingResult | null {
    const durationMs = startedAt ? Date.now() - startedAt : 0
    const sampleRate = ctx?.sampleRate ?? TARGET_RATE
    const collected = chunks
    release()
    if (!collected.length) return null

    let total = 0
    for (const c of collected) total += c.length
    if (total < sampleRate * 0.2) return null // 短于 0.2 秒视为无效录音

    const pcm = new Float32Array(total)
    let offset = 0
    for (const c of collected) {
      pcm.set(c, offset)
      offset += c.length
    }
    return { blob: encodeWav(pcm, sampleRate), durationMs }
  }

  function release() {
    try {
      processor?.disconnect()
    } catch {
      /* 忽略 */
    }
    processor = null
    try {
      ctx?.close()
    } catch {
      /* 忽略 */
    }
    ctx = null
    stream?.getTracks().forEach((t) => t.stop())
    stream = null
    chunks = []
    isRecording.value = false
  }

  return { isRecording, start, stop, release }
}

/** Float32 采样 → 16bit PCM WAV（单声道） */
function encodeWav(samples: Float32Array, sampleRate: number): Blob {
  const buffer = new ArrayBuffer(44 + samples.length * 2)
  const view = new DataView(buffer)

  const writeStr = (offset: number, s: string) => {
    for (let i = 0; i < s.length; i += 1) view.setUint8(offset + i, s.charCodeAt(i))
  }

  writeStr(0, 'RIFF')
  view.setUint32(4, 36 + samples.length * 2, true)
  writeStr(8, 'WAVE')
  writeStr(12, 'fmt ')
  view.setUint32(16, 16, true) // fmt chunk 长度
  view.setUint16(20, 1, true) // PCM
  view.setUint16(22, 1, true) // 单声道
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true) // byteRate
  view.setUint16(32, 2, true) // blockAlign
  view.setUint16(34, 16, true) // bitsPerSample
  writeStr(36, 'data')
  view.setUint32(40, samples.length * 2, true)

  let offset = 44
  for (let i = 0; i < samples.length; i += 1) {
    const s = Math.max(-1, Math.min(1, samples[i]))
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true)
    offset += 2
  }
  return new Blob([buffer], { type: 'audio/wav' })
}
