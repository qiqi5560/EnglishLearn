import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 浏览器语音能力封装：
 * - 语音识别（Speech Recognition）用于语音转文字，说话作答；
 * - 语音合成（Speech Synthesis）用于朗读。
 *
 * 注意：依赖浏览器 Web Speech API，请使用 Chrome / Edge 内核。
 * 语音识别走云端服务，需要联网；朗读使用系统语音，无需联网。
 */

type SpeechRecognitionLike = {
  lang: string
  continuous: boolean
  interimResults: boolean
  maxAlternatives: number
  start: () => void
  stop: () => void
  abort: () => void
  onstart: (() => void) | null
  onresult: ((e: any) => void) | null
  onerror: ((e: any) => void) | null
  onend: (() => void) | null
  onnomatch: (() => void) | null
}

function getRecognitionCtor(): (new () => SpeechRecognitionLike) | null {
  const w = window as any
  return w.SpeechRecognition || w.webkitSpeechRecognition || null
}

/* ============ 录音结束策略：连续收音 + 静音计时 ============ */

/** 静音持续多久（毫秒）判定为「说完了」：犹豫 3 秒内不会自动结束 */
const SILENCE_MS = 3000
/** 单次录音最长时长（毫秒），兜底防止一直不结束 */
const MAX_RECORD_MS = 60000
/** 音量阈值（RMS 0~1）：超过它算「有声音」。环境吵调大，总被截断调小 */
const VOICE_THRESHOLD = 0.02

type VolumeMonitor = { stop: () => void }

/**
 * 打开麦克风做实时音量检测（每 100ms 算一次 RMS）。
 * 拿不到权限 / 非安全上下文时返回 null，调用方自动退化为「按识别结果判断」。
 */
async function startVolumeMonitor(onVoice: (level: number) => void): Promise<VolumeMonitor | null> {
  try {
    if (!navigator.mediaDevices?.getUserMedia) return null
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const Ctx = window.AudioContext || (window as any).webkitAudioContext
    if (!Ctx) {
      stream.getTracks().forEach((t) => t.stop())
      return null
    }
    const ctx: AudioContext = new Ctx()
    const source = ctx.createMediaStreamSource(stream)
    const analyser = ctx.createAnalyser()
    analyser.fftSize = 1024
    source.connect(analyser)

    const buf = new Uint8Array(analyser.fftSize)
    const timer = window.setInterval(() => {
      analyser.getByteTimeDomainData(buf)
      let sum = 0
      for (let i = 0; i < buf.length; i += 1) {
        const v = (buf[i] - 128) / 128
        sum += v * v
      }
      onVoice(Math.sqrt(sum / buf.length))
    }, 100)

    return {
      stop: () => {
        window.clearInterval(timer)
        try {
          source.disconnect()
        } catch {
          /* 忽略 */
        }
        try {
          ctx.close()
        } catch {
          /* 忽略 */
        }
        stream.getTracks().forEach((t) => t.stop())
      },
    }
  } catch {
    return null
  }
}

/* ============ 语音合成：voices 异步加载 + 自动挑选语音 ============ */

let voicesCache: SpeechSynthesisVoice[] = []
let voicesLoaded = false
let voicesPromise: Promise<SpeechSynthesisVoice[]> | null = null

/** getVoices() 首次常返回空数组，需等 voiceschanged 事件 */
function loadVoices(): Promise<SpeechSynthesisVoice[]> {
  const synth = window.speechSynthesis
  if (!synth) return Promise.resolve([])
  if (voicesLoaded && voicesCache.length) return Promise.resolve(voicesCache)

  const immediate = synth.getVoices()
  if (immediate.length) {
    voicesCache = immediate
    voicesLoaded = true
    return Promise.resolve(immediate)
  }
  if (voicesPromise) return voicesPromise

  voicesPromise = new Promise((resolve) => {
    let settled = false
    const finish = () => {
      if (settled) return
      settled = true
      voicesCache = synth.getVoices()
      voicesLoaded = true
      synth.removeEventListener('voiceschanged', finish)
      voicesPromise = null
      resolve(voicesCache)
    }
    synth.addEventListener('voiceschanged', finish)
    // 部分浏览器不触发 voiceschanged，兜底轮询最多约 2 秒
    let tries = 0
    const timer = window.setInterval(() => {
      tries += 1
      if (synth.getVoices().length || tries >= 10) {
        window.clearInterval(timer)
        finish()
      }
    }, 200)
  })
  return voicesPromise
}

function isChinese(text: string): boolean {
  return /[\u4e00-\u9fa5]/.test(text)
}

/** 按文本语言挑最合适的语音：中文选 zh，英文优先 en-US 本地语音 */
function pickVoice(text: string, voices: SpeechSynthesisVoice[]): SpeechSynthesisVoice | null {
  if (!voices.length) return null
  const wantZh = isChinese(text)

  const scored = voices
    .map((v) => {
      const lang = (v.lang || '').toLowerCase().replace('_', '-')
      let score = 0
      if (wantZh) {
        if (lang.startsWith('zh-cn') || lang.startsWith('cmn-hans')) score += 100
        else if (lang.startsWith('zh')) score += 70
        if (/xiaoxiao|yunxi|yaoyao|huihui|xiaoyi|kangkang|xiaohan/i.test(v.name)) score += 8
      } else {
        if (lang.startsWith('en-us')) score += 100
        else if (lang.startsWith('en-gb')) score += 85
        else if (lang.startsWith('en-au')) score += 75
        else if (lang.startsWith('en')) score += 60
        if (/zira|aria|jenny|guy|libby|sonia|hazel|susan|ryan|george|ava|andrew/i.test(v.name)) score += 6
      }
      if (score === 0) return { v, score: -1 }
      if (v.localService) score += 25
      if (/natural|neural/i.test(v.name)) score += 8
      return { v, score }
    })
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score)

  return scored[0]?.v ?? null
}

/* ============ Chrome 长文本被自动暂停的兜底 ============ */

let resumeTimer: number | null = null

/** 朗读会话代号：stopSpeaking() 自增。cancel() 同样会触发当前 utterance 的
 *  onend/onerror，用代号区分「自然读完」和「被停止」，防止停止后回调继续执行 */
let speakToken = 0

function startResumeGuard() {
  stopResumeGuard()
  resumeTimer = window.setInterval(() => {
    const synth = window.speechSynthesis
    if (!synth) return
    if (!synth.speaking) {
      stopResumeGuard()
      return
    }
    if (synth.paused) synth.resume()
  }, 5000)
}

function stopResumeGuard() {
  if (resumeTimer !== null) {
    window.clearInterval(resumeTimer)
    resumeTimer = null
  }
}

/* ============ 自动播放限制：首次交互时静默解锁 ============ */

let unlocked = false

function unlockAudio() {
  const synth = window.speechSynthesis
  if (!synth || unlocked) return
  try {
    const u = new SpeechSynthesisUtterance('')
    u.volume = 0
    synth.speak(u)
    unlocked = true
  } catch {
    /* 解锁失败不影响后续 */
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener('pointerdown', unlockAudio, { once: true })
  window.addEventListener('keydown', unlockAudio, { once: true })
  // 提前拉取语音列表，避免首次朗读时才去等 voiceschanged
  if (window.speechSynthesis) loadVoices()
}

/* ============ 对外能力 ============ */

export function useSpeech() {
  const isListening = ref(false)
  const isSpeaking = ref(false)
  const recognitionSupported = ref(!!getRecognitionCtor())
  const lastError = ref<string>('')

  let recognition: SpeechRecognitionLike | null = null
  /** 当前录音的「正常结束」入口：先交出已识别内容，再收尾 */
  let finishCurrent: (() => void) | null = null

  /**
   * 开始语音识别：连续收音，静音满 SILENCE_MS（默认 3 秒）才结束。
   * 中间犹豫、停顿不会中断；结束或手动停止时一次性回调完整文本。
   */
  function start(onResult: (text: string) => void, onEnd?: () => void): boolean {
    const Ctor = getRecognitionCtor()
    if (!Ctor) {
      ElMessage.warning('当前浏览器不支持语音识别，请使用 Edge 或 Chrome')
      return false
    }
    if (recognition) {
      try {
        recognition.stop()
      } catch {
        /* 忽略 */
      }
      recognition = null
    }
    finishCurrent = null

    const rec = new Ctor()
    rec.lang = 'en-US'
    rec.continuous = true // 连续收音：不再「一停就结束」
    rec.interimResults = true
    rec.maxAlternatives = 1

    let settled = false
    let finalText = ''
    let startedAt = Date.now()
    let lastVoiceAt = Date.now()
    let monitor: VolumeMonitor | null = null
    let tick: number | null = null
    let restarts = 0

    /** 统一收尾：只执行一次，保证已识别内容不丢 */
    const finish = () => {
      if (settled) return
      settled = true
      if (tick !== null) {
        window.clearInterval(tick)
        tick = null
      }
      if (monitor) {
        monitor.stop()
        monitor = null
      }
      try {
        rec.stop()
      } catch {
        /* 忽略 */
      }
      if (recognition === rec) recognition = null
      if (finishCurrent === finish) finishCurrent = null
      isListening.value = false
      const text = finalText.trim()
      if (text) onResult(text)
      onEnd?.()
    }

    // 真实音量检测：有声音就刷新「最后发声时间」
    startVolumeMonitor((level) => {
      if (level > VOICE_THRESHOLD) lastVoiceAt = Date.now()
    }).then((m) => {
      if (settled) {
        m?.stop()
        return
      }
      monitor = m
    })

    // 每 200ms 检查：静音够久 / 超时 → 结束
    tick = window.setInterval(() => {
      const now = Date.now()
      if (now - lastVoiceAt >= SILENCE_MS || now - startedAt >= MAX_RECORD_MS) finish()
    }, 200)

    rec.onstart = () => {
      isListening.value = true
    }

    rec.onresult = (e: any) => {
      // 还在出识别结果 = 还在说话（音量检测不可用时的兜底依据）
      lastVoiceAt = Date.now()
      finalText = ''
      const results = e?.results
      for (let i = 0; i < (results?.length ?? 0); i += 1) {
        const r = results[i]
        if (r?.isFinal) finalText += (r[0]?.transcript ?? '')
      }
    }

    rec.onerror = (e: any) => {
      const code: string = e?.error ?? 'unknown'
      lastError.value = code
      if (code === 'aborted') return // 主动 stop 触发，交给 finish 收尾
      if (code === 'no-speech') {
        // 静音超时常被浏览器上报成 no-speech：已识别到的内容仍然有效
        if (finalText.trim()) {
          finish()
          return
        }
        ElMessage.warning('没听到声音，请靠近麦克风再说一次')
      } else if (code === 'not-allowed' || code === 'service-not-allowed') {
        ElMessage.error('麦克风权限被拒绝，请点击地址栏左侧的麦克风图标允许后重试')
      } else if (code === 'audio-capture') {
        ElMessage.error('没有检测到麦克风设备')
      } else if (code === 'network') {
        ElMessage.error('语音识别服务连接失败（需要联网），请改用文字输入')
      } else {
        ElMessage.warning('语音识别出错：' + code)
      }
      finish()
    }

    rec.onend = () => {
      if (settled) return
      // Chrome 偶尔会自己结束识别：只要还没静音满 3 秒就重新拉起，继续听
      const silentEnough = Date.now() - lastVoiceAt >= SILENCE_MS
      const tooLong = Date.now() - startedAt >= MAX_RECORD_MS
      if (!silentEnough && !tooLong && restarts < 5) {
        restarts += 1
        try {
          rec.start()
          return
        } catch {
          /* 拉不起来就正常收尾 */
        }
      }
      finish()
    }

    recognition = rec
    finishCurrent = finish
    try {
      rec.start()
      isListening.value = true
      return true
    } catch {
      settled = true
      if (tick !== null) {
        window.clearInterval(tick)
        tick = null
      }
      recognition = null
      finishCurrent = null
      isListening.value = false
      ElMessage.warning('无法启动语音识别，请检查麦克风权限')
      return false
    }
  }

  /** 停止语音识别（等价于「我说完了」，已识别内容照样交出去） */
  function stop() {
    if (finishCurrent) {
      finishCurrent()
      return
    }
    if (recognition) {
      try {
        recognition.stop()
      } catch {
        /* 忽略 */
      }
      recognition = null
    }
    isListening.value = false
  }

  /** 停止朗读 */
  function stopSpeaking() {
    const synth = window.speechSynthesis
    if (!synth) return
    speakToken += 1 // 作废在播回调，防止 cancel() 的 onend 继续触发 onEnd
    stopResumeGuard()
    synth.cancel()
    isSpeaking.value = false
  }

  /**
   * 朗读文本，自动按语言挑选语音。
   * 返回是否成功发起（不代表播报完成）。
   */
  async function speak(text: string, opts?: { rate?: number; onEnd?: () => void }): Promise<boolean> {
    const synth = window.speechSynthesis
    const content = (text || '').trim()
    if (!content) return false
    if (!synth) {
      ElMessage.warning('当前浏览器不支持语音朗读，请使用 Edge 或 Chrome')
      return false
    }

    unlockAudio()
    try {
      speakToken += 1 // 新一轮朗读作废上一轮的回调
      stopResumeGuard()
      synth.cancel()
      // 关键：cancel() 后必须让出一次事件循环，否则紧随的 speak 会被 Chrome 丢弃
      await new Promise((resolve) => setTimeout(resolve, 80))
      const token = speakToken

      const voices = await loadVoices()
      const voice = pickVoice(content, voices)

      const utter = new SpeechSynthesisUtterance(content)
      utter.lang = voice?.lang || (isChinese(content) ? 'zh-CN' : 'en-US')
      if (voice) utter.voice = voice
      utter.rate = opts?.rate ?? 1
      utter.pitch = 1
      utter.volume = 1

      utter.onstart = () => {
        isSpeaking.value = true
      }
      utter.onend = () => {
        if (token !== speakToken) return // 已被 stopSpeaking / 新朗读取代
        isSpeaking.value = false
        stopResumeGuard()
        opts?.onEnd?.()
      }
      utter.onerror = (e: any) => {
        if (token !== speakToken) return
        const code: string = e?.error ?? 'unknown'
        isSpeaking.value = false
        stopResumeGuard()
        if (code === 'interrupted' || code === 'canceled') return
        if (code === 'not-allowed') {
          ElMessage.warning('浏览器拦截了自动播放，请点击页面任意位置后再试')
        } else {
          ElMessage.warning('朗读失败：' + code)
        }
      }

      isSpeaking.value = true
      synth.speak(utter)
      startResumeGuard()
      return true
    } catch {
      isSpeaking.value = false
      ElMessage.warning('朗读失败，请刷新页面重试')
      return false
    }
  }

  return {
    isListening,
    isSpeaking,
    recognitionSupported,
    lastError,
    start,
    stop,
    speak,
    stopSpeaking,
    unlockAudio,
  }
}
