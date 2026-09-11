import { ref } from 'vue'

/**
 * 浏览器语音能力封装：
 * - 语音识别（Speech Recognition）用于语音转文字，说话作答；
 * - 语音合成（Speech Synthesis）用于朗读英文回复。
 *
 * 注意：语音识别依赖浏览器 Web Speech API，请使用 Chrome / Edge 内核，且需连接网络。
 */

type SpeechRecognitionLike = {
  lang: string
  continuous: boolean
  interimResults: boolean
  start: () => void
  stop: () => void
  onresult: ((e: any) => void) | null
  onerror: ((e: any) => void) | null
  onend: (() => void) | null
}

function getRecognitionCtor(): (new () => SpeechRecognitionLike) | null {
  const w = window as any
  return w.SpeechRecognition || w.webkitSpeechRecognition || null
}

export function useSpeech() {
  const isListening = ref(false)
  const recognitionSupported = ref(!!getRecognitionCtor())
  let recognition: SpeechRecognitionLike | null = null

  /** 开始语音识别，识别到最终结果后回调 text */
  function start(onResult: (text: string) => void, onEnd?: () => void): boolean {
    const Ctor = getRecognitionCtor()
    if (!Ctor) return false
    if (recognition) {
      recognition.stop()
      recognition = null
    }
    recognition = new Ctor()
    recognition.lang = 'en-US'
    recognition.continuous = false
    recognition.interimResults = false
    recognition.onresult = (e: any) => {
      const transcript: string = e.results?.[0]?.[0]?.transcript ?? ''
      if (transcript) onResult(transcript.trim())
    }
    recognition.onerror = () => {
      /* 识别失败时静默结束 */
    }
    recognition.onend = () => {
      isListening.value = false
      recognition = null
      onEnd?.()
    }
    try {
      recognition.start()
      isListening.value = true
      return true
    } catch {
      recognition = null
      isListening.value = false
      return false
    }
  }

  /** 停止语音识别 */
  function stop() {
    if (recognition) {
      recognition.stop()
      recognition = null
    }
    isListening.value = false
  }

  /** 朗读英文文本 */
  function speak(text: string) {
    const synth = window.speechSynthesis
    if (!synth || !text) return
    synth.cancel()
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = 'en-US'
    utter.rate = 1.0
    synth.speak(utter)
  }

  return { isListening, recognitionSupported, start, stop, speak }
}