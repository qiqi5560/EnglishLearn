<template>
  <div class="quote-page page">
    <AppHeader title="名句跟读" />
    <FireworksBurst ref="fireworksRef" :shells="9" />

    <main class="page-shell quote-shell">
      <!-- ==================== 顶部：说明 + 数据 ==================== -->
      <section class="hero-strip glass-card peek-host">
        <div class="hero-mascot">
          <PeekMascot variant="dog" :size="58" />
        </div>
        <div class="hero-copy">
          <p class="eyebrow">QUOTE SHADOWING</p>
          <h1>名句跟读 · 把好句子读进语感里</h1>
          <p class="hero-desc text-muted">
            选一句喜欢的台词，让 AI 人声带你逐句读；也可以导入自己的短文，自动分段、逐段一键翻译。
          </p>
        </div>
        <div class="hero-stats">
          <div class="stat">
            <b>{{ quotes.length }}</b>
            <span>名句素材</span>
          </div>
          <div class="stat">
            <b>{{ docs.length }}</b>
            <span>我的素材</span>
          </div>
          <div class="stat">
            <b>{{ finishedCount }}</b>
            <span>完成跟读</span>
          </div>
        </div>
      </section>

      <div class="quote-grid">
        <!-- ==================== 左：素材库 ==================== -->
        <aside class="lib-panel glass-card">
          <div class="lib-tabs">
            <button
              type="button"
              class="lib-tab"
              :class="{ active: tab === 'quote' }"
              @click="tab = 'quote'"
            >
              名句库
            </button>
            <button
              type="button"
              class="lib-tab"
              :class="{ active: tab === 'doc' }"
              @click="tab = 'doc'"
            >
              我的素材
            </button>
          </div>

          <!-- 名句库 -->
          <template v-if="tab === 'quote'">
            <el-input
              v-model="keyword"
              class="lib-search"
              placeholder="搜索名句 / 出处"
              clearable
              @keyup.enter="loadQuotes"
              @clear="loadQuotes"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>

            <div class="chip-row">
              <button
                v-for="c in categories"
                :key="c.value"
                type="button"
                class="chip"
                :class="{ active: category === c.value }"
                @click="onCategory(c.value)"
              >
                {{ c.label }}
              </button>
            </div>

            <div v-loading="loadingQuotes" class="lib-list">
              <button
                v-for="q in quotes"
                :key="q.quoteId"
                type="button"
                class="lib-item"
                :class="{ active: activeQuote?.quoteId === q.quoteId }"
                @click="selectQuote(q)"
              >
                <span class="lib-item-title">{{ q.title }}</span>
                <span class="lib-item-meta">
                  <em>{{ q.source }}</em>
                  <i>{{ q.wordCount }} 词</i>
                </span>
                <el-icon v-if="!q.builtin" class="lib-item-del" @click.stop="removeQuote(q)">
                  <Delete />
                </el-icon>
              </button>
              <p v-if="!loadingQuotes && !quotes.length" class="lib-empty text-muted">没有找到匹配的名句</p>
            </div>

            <button class="lib-add" type="button" @click="quoteVisible = true">
              <el-icon><Plus /></el-icon> 添加我的名句
            </button>
          </template>

          <!-- 我的素材 -->
          <template v-else>
            <el-upload
              class="upload-box"
              drag
              :show-file-list="false"
              accept=".txt,.md,.docx"
              :http-request="onUpload"
            >
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <p class="upload-text">拖入或点击上传</p>
              <p class="upload-hint text-muted">支持 .txt / .md / .docx</p>
            </el-upload>

            <button class="lib-add" type="button" @click="pasteVisible = true">
              <el-icon><Document /></el-icon> 粘贴文本导入
            </button>

            <div v-loading="loadingDocs" class="lib-list doc-list">
              <button
                v-for="d in docs"
                :key="d.docId"
                type="button"
                class="lib-item"
                :class="{ active: activeDoc?.docId === d.docId }"
                @click="openDoc(d.docId)"
              >
                <span class="lib-item-title">{{ d.title }}</span>
                <span class="lib-item-meta">
                  <em>{{ d.sourceType.toUpperCase() }}</em>
                  <i>{{ d.paragraphCount }} 段</i>
                </span>
                <el-icon class="lib-item-del" @click.stop="removeDoc(d)">
                  <Delete />
                </el-icon>
              </button>
              <p v-if="!loadingDocs && !docs.length" class="lib-empty text-muted">还没有导入素材，先从上方上传一段短文吧</p>
            </div>
          </template>
        </aside>

        <!-- ==================== 右：跟读工作区 ==================== -->
        <section class="work-panel">
          <!-- ---------- 名句模式 ---------- -->
          <template v-if="tab === 'quote'">
            <article v-if="activeQuote" class="read-card glass-card sheen">
              <header class="read-head">
                <div>
                  <p class="eyebrow">{{ activeQuote.source }}</p>
                  <h2>{{ activeQuote.title }}</h2>
                </div>
                <div class="read-tags">
                  <LevelTag :level="activeQuote.level || 'B1'" />
                  <span class="tag-soft">{{ activeQuote.wordCount }} 词</span>
                </div>
              </header>

              <p class="read-en">
                <span
                  v-for="(s, i) in sentences"
                  :key="i"
                  class="sentence"
                  :class="{ 'is-current': sentenceIndex === i }"
                >{{ s }}</span>
              </p>

              <transition name="fade-slide">
                <p v-if="currentZh" class="read-zh">{{ currentZh }}</p>
              </transition>

              <div class="read-actions">
                <el-button type="primary" round :disabled="evaluating" @click="speakAll">
                  <el-icon><Headset /></el-icon>&nbsp;{{ speaking ? '重新带读' : 'AI 带读' }}
                </el-button>
                <el-button v-if="speaking" round @click="stopSpeak">
                  <el-icon><VideoPause /></el-icon>&nbsp;停止
                </el-button>
                <el-button round :loading="evaluating" @click="toggleTranslate">
                  <el-icon><MagicStick /></el-icon>&nbsp;{{ currentZh ? '收起译文' : '一键翻译' }}
                </el-button>
                <div class="rate-picker">
                  <span class="text-muted">语速</span>
                  <el-radio-group v-model="rate" size="small">
                    <el-radio-button :value="0.8">慢</el-radio-button>
                    <el-radio-button :value="1">常速</el-radio-button>
                    <el-radio-button :value="1.2">快</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
            </article>

            <article v-else class="read-card glass-card empty-read peek-host">
              <PeekMascot variant="cat" :size="62" />
              <p class="empty-title">从左边挑一句开始吧</p>
              <p class="text-muted">点开任意台词，AI 会带你逐句读一遍。</p>
            </article>

            <!-- 跟读打分 -->
            <article class="record-card glass-card">
              <div class="record-left">
                <button
                  type="button"
                  class="mic-btn"
                  :class="{ recording: isListening }"
                  :disabled="!activeQuote || evaluating"
                  @click="onRecord"
                >
                  <el-icon :size="26"><Microphone /></el-icon>
                </button>
                <p class="mic-hint text-muted">
                  {{
                    isListening
                      ? '正在听你说…'
                      : isRecording
                        ? '正在录音，读完再点一次结束'
                        : evaluating
                          ? 'AI 正在评分…'
                          : '点击麦克风，跟读上面这段'
                  }}
                </p>
                <p v-if="!recognitionSupported" class="mic-warn">
                  当前浏览器不支持语音识别，建议使用 Chrome / Edge
                </p>
              </div>

              <div class="record-right">
                <template v-if="evalResult">
                  <div class="score-row">
                    <ScoreRing :score="Math.round(evalResult.pron)" label="发音" :size="64" :stroke="6" />
                    <ScoreRing :score="Math.round(evalResult.fluency)" label="流利度" :size="64" :stroke="6" />
                    <ScoreRing :score="Math.round(evalResult.natural)" label="语调" :size="64" :stroke="6" />
                    <ScoreRing :score="Math.round(evalResult.completion)" label="完整度" :size="64" :stroke="6" />
                    <ScoreRing :score="Math.round(evalResult.total)" label="总分" :size="72" :stroke="7" />
                  </div>
                  <p class="eval-feedback">{{ evalResult.feedback }}</p>
                  <ul v-if="evalResult.tips.length" class="eval-tips">
                    <li v-for="(t, i) in evalResult.tips" :key="i">{{ t }}</li>
                  </ul>
                  <p v-if="spokenText" class="eval-spoken text-muted">识别到：{{ spokenText }}</p>
                  <PronDetail v-if="evalResult?.phoneme" :phoneme="evalResult.phoneme" />
                </template>
                <div v-else class="record-placeholder text-muted">
                  跟读后这里会显示发音、流利度、语调与完整度四项评分。
                </div>
              </div>
            </article>

            <div class="finish-row">
              <el-button
                class="finish-btn"
                type="primary"
                size="large"
                round
                :disabled="!activeQuote"
                @click="finishReading"
              >
                <el-icon><Select /></el-icon>&nbsp;完成阅读
              </el-button>
              <span class="text-muted">读完点一下，会有小惊喜</span>
            </div>
          </template>

          <!-- ---------- 我的素材模式 ---------- -->
          <template v-else>
            <article v-if="activeDoc" class="doc-card glass-card">
              <header class="read-head">
                <div>
                  <p class="eyebrow">{{ activeDoc.sourceType.toUpperCase() }} · 自动分段</p>
                  <h2>{{ activeDoc.title }}</h2>
                </div>
                <div class="read-tags">
                  <span class="tag-soft">{{ activeDoc.paragraphCount }} 段</span>
                </div>
              </header>

              <div class="doc-toolbar">
                <el-button
                  type="primary"
                  round
                  :loading="translatingAll"
                  @click="translateAll"
                >
                  <el-icon><MagicStick /></el-icon>&nbsp;整篇一键翻译
                </el-button>
                <el-button round :disabled="!activeDoc.paragraphs.length" @click="speakDoc">
                  <el-icon><Headset /></el-icon>&nbsp;{{ speaking ? '停止带读' : '全文带读' }}
                </el-button>
                <el-button v-if="speaking" round @click="stopSpeak">
                  <el-icon><VideoPause /></el-icon>&nbsp;停止
                </el-button>
              </div>

              <div class="para-list">
                <article
                  v-for="(p, i) in activeDoc.paragraphs"
                  :key="i"
                  class="para-card peek-host"
                  :class="{ 'is-current': paraIndex === i, 'is-done': !!p.zh }"
                >
                  <span class="para-no">{{ String(i + 1).padStart(2, '0') }}</span>
                  <p class="para-en">{{ p.en }}</p>

                  <transition name="fade-slide">
                    <p v-if="p.zh" class="para-zh">{{ p.zh }}</p>
                  </transition>

                  <footer class="para-foot">
                    <el-button
                      text
                      size="small"
                      class="para-act"
                      :loading="!!paraTranslating[i]"
                      @click="toggleParaZh(i)"
                    >
                      <el-icon><MagicStick /></el-icon>&nbsp;{{ p.zh ? '收起译文' : '一键翻译' }}
                    </el-button>
                    <el-button text size="small" class="para-act" @click="speakParagraph(i)">
                      <el-icon><Headset /></el-icon>&nbsp;朗读本段
                    </el-button>
                    <el-button
                      text
                      size="small"
                      class="para-act"
                      :class="{ 'is-recording': recordingPara === i }"
                      :loading="paraEvalTarget === i"
                      @click="recordParagraph(i)"
                    >
                      <el-icon><Microphone /></el-icon>&nbsp;{{ recordingPara === i ? '正在听你说…' : '跟读本段' }}
                    </el-button>
                  </footer>

                  <div v-if="paraEval[i]" class="para-eval">
                    <div class="para-eval-head">
                      <span class="para-eval-title">
                        <el-icon><DataAnalysis /></el-icon>&nbsp;智能检测
                      </span>
                      <button type="button" class="para-eval-close" title="收起" @click="clearParaEval(i)">
                        <el-icon><Close /></el-icon>
                      </button>
                    </div>
                    <div class="para-eval-body">
                      <div class="score-row">
                        <ScoreRing :score="Math.round(paraEval[i].pron)" label="发音" :size="60" :stroke="6" />
                        <ScoreRing :score="Math.round(paraEval[i].fluency)" label="流利度" :size="60" :stroke="6" />
                        <ScoreRing :score="Math.round(paraEval[i].natural)" label="语调" :size="60" :stroke="6" />
                        <ScoreRing :score="Math.round(paraEval[i].completion)" label="完整度" :size="60" :stroke="6" />
                        <ScoreRing :score="Math.round(paraEval[i].total)" label="总分" :size="68" :stroke="7" />
                      </div>
                      <div class="para-eval-text">
                        <p class="eval-feedback">{{ paraEval[i].feedback }}</p>
                        <ul v-if="paraEval[i].tips.length" class="eval-tips">
                          <li v-for="(t, ti) in paraEval[i].tips" :key="ti">{{ t }}</li>
                        </ul>
                        <p v-if="paraSpoken[i]" class="eval-spoken text-muted">识别到：{{ paraSpoken[i] }}</p>
                        <PronDetail v-if="paraEval[i]?.phoneme" :phoneme="paraEval[i]!.phoneme!" />
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </article>

            <article v-else class="read-card glass-card empty-read peek-host">
              <PeekMascot variant="dog" :size="62" />
              <p class="empty-title">导入一段你喜欢的短文</p>
              <p class="text-muted">支持 Word / txt / Markdown，导入后自动分段，逐段一键翻译。</p>
            </article>
          </template>
        </section>
      </div>
    </main>

    <!-- ==================== 添加名句 ==================== -->
    <el-dialog v-model="quoteVisible" title="添加我的名句" width="560">
      <el-form label-width="72px" label-position="left">
        <el-form-item label="标题">
          <el-input v-model="quoteForm.title" placeholder="例如：关于坚持的一句" />
        </el-form-item>
        <el-form-item label="出处">
          <el-input v-model="quoteForm.source" placeholder="例如：《当幸福来敲门》" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="quoteForm.category" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="英文">
          <el-input v-model="quoteForm.textEn" type="textarea" :rows="4" placeholder="粘贴英文原文（建议 30–60 词）" />
        </el-form-item>
        <el-form-item label="中文">
          <el-input v-model="quoteForm.textZh" type="textarea" :rows="3" placeholder="可留空，之后用一键翻译补全" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quoteVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingQuote" @click="submitQuote">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 粘贴导入 ==================== -->
    <el-dialog v-model="pasteVisible" title="粘贴文本导入" width="640">
      <el-form label-width="72px" label-position="left">
        <el-form-item label="标题">
          <el-input v-model="pasteForm.title" placeholder="给这段素材起个名字" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input
            v-model="pasteForm.text"
            type="textarea"
            :rows="10"
            placeholder="粘贴英文短文 / 名句集，段落之间空一行即可自动分段"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pasteVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPaste" @click="submitPaste">导入并分段</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import ScoreRing from '@/components/base/ScoreRing.vue'
import FireworksBurst from '@/components/base/FireworksBurst.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { useSpeech } from '@/composables/useSpeech'
import { useRecorder } from '@/composables/useRecorder'
import PronDetail from '@/components/business/PronDetail.vue'
import {
  createDoc,
  createQuote,
  deleteDoc,
  deleteQuote,
  docDetail,
  evaluateReading,
  evaluateReadingAudio,
  listDocs,
  listQuotes,
  translateDoc,
  translateText,
  uploadDoc,
} from '@/api/modules/quote'
import type { DocBriefDto, DocDto, QuoteDto, ReadEvalDto } from '@/types/api'

const route = useRoute()
const fireworksRef = ref<InstanceType<typeof FireworksBurst> | null>(null)

const tab = ref<'quote' | 'doc'>('quote')
const rate = ref(1)
const finishedCount = ref(0)

// ---------------- 名句库 ----------------
const quotes = ref<QuoteDto[]>([])
const loadingQuotes = ref(false)
const keyword = ref('')
const category = ref('')
const activeQuote = ref<QuoteDto | null>(null)
const quoteZh = ref('')
const translatingQuote = ref(false)

const categories = [
  { label: '全部', value: '' },
  { label: '电影台词', value: '电影台词' },
  { label: '英语美句', value: '英语美句' },
  { label: '励志名言', value: '励志名言' },
]
const categoryOptions = ['电影台词', '英语美句', '励志名言']

// ---------------- 我的素材 ----------------
const docs = ref<DocBriefDto[]>([])
const loadingDocs = ref(false)
const activeDoc = ref<DocDto | null>(null)
const translatingAll = ref(false)
const paraTranslating = reactive<Record<number, boolean>>({})
const paraEval = reactive<Record<number, ReadEvalDto>>({})
const paraSpoken = reactive<Record<number, string>>({})
const paraEvalTarget = ref(-1)
const recordingPara = ref(-1)

// ---------------- 朗读 / 跟读 ----------------
const { isListening, recognitionSupported, start, stop } = useSpeech()
// 录音用于音素级评测：浏览器支持语音识别时与识别并行收音，
// 不支持时（Firefox / Safari）也能只靠录音完成音素打分
const { isRecording, start: startRecording, stop: stopRecording } = useRecorder()
const speaking = ref(false)
const sentenceIndex = ref(-1)
const paraIndex = ref(-1)
const evaluating = ref(false)
const evalResult = ref<ReadEvalDto | null>(null)
const spokenText = ref('')

const sentences = computed(() => splitSentences(activeQuote.value?.textEn ?? ''))
const currentZh = computed(() => quoteZh.value)

function splitSentences(text: string): string[] {
  return (text.match(/[^.!?]+[.!?]*/g) ?? []).map((s) => s.trim()).filter(Boolean)
}

// ==================== 名句库 ====================
async function loadQuotes() {
  loadingQuotes.value = true
  try {
    const res = await listQuotes({ category: category.value || undefined, keyword: keyword.value || undefined })
    quotes.value = res ?? []
    // 保持当前选中项；若失效则回退到列表首条
    const still = quotes.value.find((q) => q.quoteId === activeQuote.value?.quoteId)
    if (still) {
      activeQuote.value = still
    } else if (quotes.value.length) {
      selectQuote(quotes.value[0])
    } else {
      activeQuote.value = null
      quoteZh.value = ''
    }
  } catch {
    /* 请求层已提示 */
  } finally {
    loadingQuotes.value = false
  }
}

function onCategory(value: string) {
  category.value = value
  loadQuotes()
}

function selectQuote(q: QuoteDto) {
  if (activeQuote.value?.quoteId === q.quoteId) return
  stopSpeak()
  activeQuote.value = q
  quoteZh.value = q.textZh || ''
  evalResult.value = null
  spokenText.value = ''
  sentenceIndex.value = -1
}

async function removeQuote(q: QuoteDto) {
  try {
    await ElMessageBox.confirm(`确定删除「${q.title}」吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteQuote(q.quoteId)
    ElMessage.success('已删除')
    if (activeQuote.value?.quoteId === q.quoteId) {
      activeQuote.value = null
      quoteZh.value = ''
    }
    await loadQuotes()
  } catch {
    /* 请求层已提示 */
  }
}

async function toggleTranslate() {
  const q = activeQuote.value
  if (!q) return
  if (quoteZh.value) {
    quoteZh.value = ''
    return
  }
  if (q.textZh) {
    quoteZh.value = q.textZh
    return
  }
  translatingQuote.value = true
  try {
    const res = await translateText(q.textEn)
    quoteZh.value = res.textZh || ''
    if (!quoteZh.value) ElMessage.warning('翻译结果为空，请稍后重试')
  } catch {
    /* 请求层已提示 */
  } finally {
    translatingQuote.value = false
  }
}

// ==================== 我的素材 ====================
async function loadDocs() {
  loadingDocs.value = true
  try {
    docs.value = (await listDocs()) ?? []
  } catch {
    /* 请求层已提示 */
  } finally {
    loadingDocs.value = false
  }
}

async function openDoc(docId: number) {
  if (activeDoc.value?.docId === docId) return
  stopSpeak()
  try {
    activeDoc.value = await docDetail(docId)
    Object.keys(paraEval).forEach((k) => delete paraEval[Number(k)])
    Object.keys(paraSpoken).forEach((k) => delete paraSpoken[Number(k)])
    paraEvalTarget.value = -1
    recordingPara.value = -1
  } catch {
    /* 请求层已提示 */
  }
}

async function removeDoc(d: DocBriefDto) {
  try {
    await ElMessageBox.confirm(`确定删除素材「${d.title}」吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteDoc(d.docId)
    ElMessage.success('已删除')
    if (activeDoc.value?.docId === d.docId) activeDoc.value = null
    await loadDocs()
  } catch {
    /* 请求层已提示 */
  }
}

async function onUpload(options: { file: File | Blob }) {
  try {
    const doc = await uploadDoc(options.file as File)
    ElMessage.success('已导入并自动分段')
    tab.value = 'doc'
    await loadDocs()
    await openDoc(doc.docId)
  } catch {
    /* 请求层已提示 */
  }
}

async function toggleParaZh(index: number) {
  const doc = activeDoc.value
  if (!doc) return
  const para = doc.paragraphs[index]
  if (para.zh) {
    para.zh = null
    return
  }
  paraTranslating[index] = true
  try {
    const res = await translateText(para.en)
    para.zh = res.textZh || ''
  } catch {
    /* 请求层已提示 */
  } finally {
    paraTranslating[index] = false
  }
}

async function translateAll() {
  const doc = activeDoc.value
  if (!doc) return
  translatingAll.value = true
  try {
    const res = await translateDoc(doc.docId, true)
    activeDoc.value = { ...doc, paragraphs: res.paragraphs ?? doc.paragraphs }
    ElMessage.success('整篇翻译完成')
    fireworksRef.value?.burst(7)
  } catch {
    /* 请求层已提示 */
  } finally {
    translatingAll.value = false
  }
}

// ==================== 朗读 ====================
/** 朗读会话代号：每次 stopSpeak 自增。cancel() 也会触发 onend，
 *  用代号判断这次 onend 是「自然读完」还是「被停止」，后者不再续播下一句 */
let speakSession = 0

function utter(text: string, onEnd: () => void) {
  const synth = window.speechSynthesis
  if (!synth) {
    onEnd()
    return
  }
  const session = speakSession
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'en-US'
  u.rate = rate.value
  const done = () => {
    if (session !== speakSession) return // 已点停止 / 已开新带读，链路作废
    onEnd()
  }
  u.onend = done
  u.onerror = done
  synth.speak(u)
}

function speakAll() {
  const list = sentences.value
  if (!list.length) {
    ElMessage.warning('这句还没有英文内容')
    return
  }
  stopSpeak()
  speaking.value = true
  paraIndex.value = -1
  playSentence(0)
}

function playSentence(i: number) {
  const list = sentences.value
  if (i >= list.length) {
    stopSpeak()
    return
  }
  sentenceIndex.value = i
  utter(list[i], () => playSentence(i + 1))
}

function speakParagraph(i: number) {
  const doc = activeDoc.value
  if (!doc) return
  const list = splitSentences(doc.paragraphs[i].en)
  if (!list.length) return
  stopSpeak()
  speaking.value = true
  sentenceIndex.value = -1
  paraIndex.value = i
  playParagraphSentence(i, list, 0)
}

function playParagraphSentence(para: number, list: string[], i: number) {
  if (i >= list.length) {
    stopSpeak()
    return
  }
  paraIndex.value = para
  utter(list[i], () => playParagraphSentence(para, list, i + 1))
}

function speakDoc() {
  const doc = activeDoc.value
  if (!doc) return
  if (speaking.value) {
    stopSpeak()
    return
  }
  const list = doc.paragraphs.map((p) => splitSentences(p.en)).filter((s) => s.length)
  if (!list.length) return
  stopSpeak()
  speaking.value = true
  playDoc(list, 0, 0)
}

function playDoc(list: string[][], para: number, i: number) {
  if (para >= list.length) {
    stopSpeak()
    return
  }
  if (i >= list[para].length) {
    playDoc(list, para + 1, 0)
    return
  }
  paraIndex.value = para
  utter(list[para][i], () => playDoc(list, para, i + 1))
}

function stopSpeak() {
  speakSession += 1 // 立即作废当前朗读链，防止 cancel() 触发的 onend 续播下一句
  window.speechSynthesis?.cancel()
  speaking.value = false
  sentenceIndex.value = -1
  paraIndex.value = -1
}

// ==================== 跟读评分 ====================
/** 打开麦克风，仅凭录音评分（浏览器不支持语音识别时的路径） */
async function beginRecordingOnly() {
  const ok = await startRecording()
  if (!ok) {
    ElMessage.warning('无法访问麦克风，请检查浏览器权限')
    return false
  }
  ElMessage.info('开始录音，读完再点一次结束')
  return true
}

function onRecord() {
  const target = activeQuote.value?.textEn
  if (!target) return
  if (isListening.value) {
    stop()
    return
  }
  if (isRecording.value) {
    runEval(target, '')
    return
  }
  if (!recognitionSupported.value) {
    beginRecordingOnly()
    return
  }
  void startRecording() // 录音与语音识别并行，不阻塞识别启动
  const ok = start((text) => {
    spokenText.value = text
    runEval(target, text)
  })
  if (!ok) ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome / Edge')
}

async function recordParagraph(index: number) {
  const doc = activeDoc.value
  if (!doc) return
  if (isListening.value) {
    stop()
    recordingPara.value = -1
    return
  }
  const target = doc.paragraphs[index].en
  if (isRecording.value) {
    runEval(target, '', index)
    return
  }
  if (!recognitionSupported.value) {
    if (await beginRecordingOnly()) recordingPara.value = index
    return
  }
  await startRecording()
  const ok = start(
    (text) => {
      paraSpoken[index] = text
      runEval(target, text, index)
    },
    () => {
      recordingPara.value = -1
    },
  )
  if (!ok) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome / Edge')
    return
  }
  recordingPara.value = index
}

/** 收起某段的智能检测结果 */
function clearParaEval(index: number) {
  delete paraEval[index]
  delete paraSpoken[index]
}

/**
 * 优先用录音做音素级评测（逐音素判定），
 * 拿不到录音或后端音素引擎不可用时，回退到文本比对评分。
 */
async function evalWithAudio(target: string, spoken: string): Promise<ReadEvalDto | null> {
  const rec = stopRecording()
  if (rec) {
    try {
      return await evaluateReadingAudio(rec.blob, target)
    } catch {
      /* 音素评测失败：静默回退到文本评分 */
    }
  }
  if (!spoken.trim()) {
    ElMessage.warning('没听到你说的内容，请再靠近麦克风读一次')
    return null
  }
  return await evaluateReading(target, spoken)
}

async function runEval(target: string, spoken: string, paraIdx = -1) {
  evaluating.value = true
  if (paraIdx >= 0) paraEvalTarget.value = paraIdx
  try {
    const res = await evalWithAudio(target, spoken)
    if (!res) return
    if (paraIdx >= 0) {
      paraEval[paraIdx] = res
    } else {
      evalResult.value = res
    }
    if (res.total >= 85) {
      fireworksRef.value?.burst(6)
      ElMessage.success('读得很棒，继续保持！')
    }
  } catch {
    /* 请求层已提示 */
  } finally {
    evaluating.value = false
    paraEvalTarget.value = -1
  }
}

// ==================== 完成阅读 ====================
function finishReading() {
  stopSpeak()
  fireworksRef.value?.burst(14)
  finishedCount.value += 1
  const praise = ['读得真好，语感在长出来', '这一遍比上一遍更自然了', '坚持读下去，开口会越来越轻松']
  ElMessage.success(praise[Math.floor(Math.random() * praise.length)])
  // 回到页面顶部，方便直接看打分与音素级检测结果
  const main = document.querySelector('.app-main')
  if (main) {
    main.scrollTo({ top: 0, behavior: 'smooth' })
  } else {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// ==================== 添加名句 / 粘贴导入 ====================
const quoteVisible = ref(false)
const savingQuote = ref(false)
const quoteForm = reactive({ title: '', source: '', category: '英语美句', level: 'B1', textEn: '', textZh: '' })

async function submitQuote() {
  if (!quoteForm.title.trim() || !quoteForm.textEn.trim()) {
    ElMessage.warning('标题和英文原文都要填哦')
    return
  }
  savingQuote.value = true
  try {
    const created = await createQuote({ ...quoteForm })
    quoteVisible.value = false
    Object.assign(quoteForm, { title: '', source: '', category: '英语美句', level: 'B1', textEn: '', textZh: '' })
    ElMessage.success('已加入你的名句库')
    await loadQuotes()
    const hit = quotes.value.find((q) => q.quoteId === created?.quoteId)
    if (hit) selectQuote(hit)
  } catch {
    /* 请求层已提示 */
  } finally {
    savingQuote.value = false
  }
}

const pasteVisible = ref(false)
const savingPaste = ref(false)
const pasteForm = reactive({ title: '', text: '' })

async function submitPaste() {
  if (!pasteForm.title.trim() || !pasteForm.text.trim()) {
    ElMessage.warning('标题和正文都要填哦')
    return
  }
  savingPaste.value = true
  try {
    const doc = await createDoc({ title: pasteForm.title, text: pasteForm.text, sourceType: 'paste' })
    pasteVisible.value = false
    Object.assign(pasteForm, { title: '', text: '' })
    ElMessage.success('已导入并自动分段')
    await loadDocs()
    await openDoc(doc.docId)
  } catch {
    /* 请求层已提示 */
  } finally {
    savingPaste.value = false
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  await Promise.all([loadQuotes(), loadDocs()])
  // 从首页台词角点进来：按片名定位到对应名句
  const film = typeof route.query.film === 'string' ? route.query.film : ''
  if (film) {
    const hit =
      quotes.value.find((q) => q.source === film) ??
      quotes.value.find((q) => q.source?.includes(film.replace(/[《》]/g, '')) || film.includes(q.source ?? ''))
    if (hit) selectQuote(hit)
  }
})

onBeforeUnmount(() => {
  stopSpeak()
  stop()
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.quote-page {
  background: var(--surface);
}

.quote-shell {
  padding-top: 26px;
  padding-bottom: 64px;
}

.eyebrow {
  margin: 0 0 7px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: var(--ink);
  letter-spacing: -0.03em;
}

h1 {
  font-size: clamp(24px, 1.9vw, 29px);
  font-weight: 700;
  line-height: 1.2;
}

h2 {
  font-size: 18px;
  font-weight: 700;
}

// ------------------------------------------------------------
// 顶部说明条
// ------------------------------------------------------------
.hero-strip {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 20px 26px;
}

.hero-mascot {
  flex-shrink: 0;
  align-self: center;
  margin-left: -6px;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-desc {
  max-width: 560px;
  margin: 8px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
}

.hero-stats {
  display: flex;
  gap: 26px;
  flex-shrink: 0;
}

.stat {
  display: grid;
  gap: 2px;
  text-align: center;

  b {
    color: var(--primary);
    font-size: 21px;
    font-weight: 800;
    letter-spacing: -0.03em;
  }

  span {
    color: var(--muted);
    font-size: 11px;
  }
}

// ------------------------------------------------------------
// 两栏
// ------------------------------------------------------------
.quote-grid {
  display: grid;
  grid-template-columns: 322px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
  margin-top: 20px;
}

// ---------------- 左：素材库 ----------------
.lib-panel {
  position: sticky;
  top: 84px;
  padding: 16px 16px 18px;
}

.lib-tabs {
  display: flex;
  gap: 6px;
  padding: 4px;
  background: rgba(59, 111, 224, 0.07);
  border-radius: 999px;
}

.lib-tab {
  flex: 1;
  padding: 8px 10px;
  color: #55617e;
  background: transparent;
  border: 0;
  border-radius: 999px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.3s $ease-apple,
    color 0.3s ease,
    box-shadow 0.3s ease;

  &.active {
    color: var(--primary);
    background: #fff;
    box-shadow: 0 2px 10px rgba(31, 42, 68, 0.1);
  }
}

.lib-search {
  margin-top: 14px;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.chip {
  padding: 5px 11px;
  color: var(--muted);
  background: rgba(31, 42, 68, 0.04);
  border: 1px solid transparent;
  border-radius: 999px;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition:
    color 0.28s ease,
    background 0.28s ease,
    border-color 0.28s ease;

  &:hover {
    color: var(--primary);
  }

  &.active {
    color: var(--primary);
    background: rgba(59, 111, 224, 0.1);
    border-color: rgba(59, 111, 224, 0.28);
  }
}

.lib-list {
  display: grid;
  gap: 7px;
  max-height: 420px;
  margin-top: 14px;
  overflow-y: auto;
  padding-right: 2px;
}

.doc-list {
  max-height: 300px;
}

.lib-item {
  position: relative;
  display: grid;
  gap: 4px;
  padding: 11px 13px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.3s $ease-apple,
    border-color 0.3s ease,
    box-shadow 0.3s ease,
    background 0.3s ease;

  &:hover {
    transform: translateX(3px);
    border-color: rgba(59, 111, 224, 0.3);
  }

  &.active {
    background: rgba(59, 111, 224, 0.09);
    border-color: rgba(59, 111, 224, 0.4);
    box-shadow: 0 8px 20px rgba(59, 111, 224, 0.14);
  }
}

.lib-item-title {
  padding-right: 18px;
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.45;
}

.lib-item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  em {
    color: var(--primary);
    font-size: 11px;
    font-style: normal;
    font-weight: 600;
  }

  i {
    color: var(--muted);
    font-size: 11px;
    font-style: normal;
  }
}

.lib-item-del {
  position: absolute;
  top: 9px;
  right: 9px;
  color: #c2c9d8;
  font-size: 13px;
  transition: color 0.25s ease;

  &:hover {
    color: var(--danger);
  }
}

.lib-empty {
  padding: 18px 4px;
  font-size: 12.5px;
  line-height: 1.7;
  text-align: center;
}

.lib-add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 12px;
  padding: 10px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.08);
  border: 1px dashed rgba(59, 111, 224, 0.35);
  border-radius: var(--radius-md);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.28s ease,
    transform 0.3s $ease-apple;

  &:hover {
    background: rgba(59, 111, 224, 0.14);
    transform: translateY(-1px);
  }
}

.upload-box {
  margin-top: 14px;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    padding: 18px 12px;
    border-radius: var(--radius-md);
    border-color: rgba(59, 111, 224, 0.3);
    background: rgba(255, 255, 255, 0.6);
    transition: border-color 0.3s ease, background 0.3s ease;
  }

  :deep(.el-upload-dragger:hover) {
    border-color: var(--primary);
    background: rgba(59, 111, 224, 0.06);
  }
}

.upload-icon {
  color: var(--primary);
  font-size: 24px;
}

.upload-text {
  margin: 6px 0 2px;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.upload-hint {
  margin: 0;
  font-size: 11px;
}

// ---------------- 右：工作区 ----------------
.work-panel {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.read-card {
  padding: 22px 26px 20px;
}

.read-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.read-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.tag-soft {
  padding: 3px 10px;
  color: var(--muted);
  background: rgba(31, 42, 68, 0.05);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.read-en {
  margin: 16px 0 0;
  color: var(--ink);
  font-family: var(--font-reading);
  font-size: 19px;
  line-height: 1.85;
  letter-spacing: -0.01em;
}

.sentence {
  margin-right: 0.28em;
  border-radius: 4px;
  transition:
    background 0.35s ease,
    box-shadow 0.35s ease;

  &.is-current {
    background: rgba(59, 111, 224, 0.14);
    box-shadow: 0 0 0 4px rgba(59, 111, 224, 0.14);
  }
}

.read-zh {
  margin: 14px 0 0;
  padding-left: 14px;
  color: var(--muted);
  border-left: 3px solid rgba(59, 111, 224, 0.28);
  font-size: 13.5px;
  line-height: 1.85;
}

.read-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.rate-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  font-size: 12px;
}

.empty-read {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 40px 24px 44px;
  text-align: center;
}

.empty-title {
  margin: 4px 0 0;
  color: var(--ink);
  font-size: 15px;
  font-weight: 700;
}

// ---------------- 跟读打分 ----------------
.record-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 26px;
  padding: 24px 26px;
}

.record-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding-right: 24px;
  border-right: 1px solid var(--border);
}

.mic-btn {
  display: grid;
  place-items: center;
  width: 66px;
  height: 66px;
  color: #fff;
  background: linear-gradient(135deg, #ff8a6d, var(--accent));
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 14px 30px rgba(255, 106, 77, 0.3);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s ease;

  &:hover:not(:disabled) {
    transform: translateY(-2px) scale(1.05);
    box-shadow: 0 20px 38px rgba(255, 106, 77, 0.4);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &.recording {
    animation: mic-pulse 1.5s ease-in-out infinite;
  }
}

@keyframes mic-pulse {
  0%,
  100% {
    box-shadow: 0 14px 30px rgba(255, 106, 77, 0.3);
  }
  50% {
    box-shadow: 0 14px 30px rgba(255, 106, 77, 0.3), 0 0 0 14px rgba(255, 106, 77, 0.12);
  }
}

.mic-hint {
  margin: 0;
  font-size: 12.5px;
  text-align: center;
}

.mic-warn {
  margin: 0;
  color: var(--warning);
  font-size: 11.5px;
  text-align: center;
  line-height: 1.6;
}

.record-right {
  min-width: 0;
}

.score-row {
  display: flex;
  align-items: flex-end;
  gap: 22px;
}

.eval-feedback {
  margin: 16px 0 0;
  color: var(--ink);
  font-size: 13.5px;
  line-height: 1.75;
}

.eval-tips {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.8;
}

.eval-spoken {
  margin: 12px 0 0;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
  font-size: 12px;
}

.record-placeholder {
  display: grid;
  place-items: center;
  height: 100%;
  font-size: 13px;
  line-height: 1.8;
  text-align: center;
}

.finish-row {
  display: flex;
  align-items: center;
  gap: 14px;
  justify-content: center;
  padding: 4px 0 2px;

  span {
    font-size: 12.5px;
  }
}

.finish-btn.el-button {
  padding: 14px 30px;
  font-size: 15px;
  font-weight: 700;
  border: 0;
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0 60%, #2f5bb3);
  box-shadow: 0 14px 30px rgba(59, 111, 224, 0.3);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s ease;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 20px 40px rgba(59, 111, 224, 0.4);
  }
}

// ---------------- 我的素材 ----------------
.doc-card {
  padding: 22px 26px 26px;
}

.doc-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border);
}

.para-list {
  display: grid;
  gap: 28px;
  margin-top: 26px;
}

.para-card {
  position: relative;
  padding: 20px 22px 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition:
    border-color 0.35s ease,
    box-shadow 0.35s ease,
    transform 0.35s $ease-apple;

  &:hover {
    border-color: rgba(59, 111, 224, 0.26);
    box-shadow: 0 12px 28px rgba(31, 42, 68, 0.08);
  }

  &.is-current {
    border-color: rgba(59, 111, 224, 0.45);
    box-shadow: 0 0 0 4px rgba(59, 111, 224, 0.1);
  }
}

.para-no {
  position: absolute;
  top: -11px;
  left: 20px;
  padding: 2px 10px;
  color: #fff;
  background: linear-gradient(135deg, #5b8bf0, #2f5bb3);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.para-en {
  margin: 6px 0 0;
  color: var(--ink);
  font-family: var(--font-reading);
  font-size: 16.5px;
  line-height: 1.95;
}

.para-zh {
  margin: 12px 0 0;
  padding-left: 14px;
  color: var(--muted);
  border-left: 3px solid rgba(59, 111, 224, 0.28);
  font-size: 13.5px;
  line-height: 1.9;
}

.para-foot {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}

.para-act.el-button {
  color: var(--primary);
  font-size: 12.5px;

  &:hover {
    color: var(--primary);
    background: rgba(59, 111, 224, 0.08);
  }

  // 录音中：按钮呼吸提示，避免「点了没反应」的错觉
  &.is-recording {
    color: var(--accent);
    animation: rec-breathe 1.4s ease-in-out infinite;
  }
}

@keyframes rec-breathe {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

.para-eval {
  margin-top: 14px;
  padding: 14px 16px 16px;
  background: linear-gradient(135deg, rgba(59, 111, 224, 0.08), rgba(59, 111, 224, 0.03));
  border: 1px solid rgba(59, 111, 224, 0.16);
  border-radius: var(--radius-md);
  animation: rise-in 0.45s $ease-apple both;
}

.para-eval-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.para-eval-title {
  display: inline-flex;
  align-items: center;
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.para-eval-close {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  color: var(--muted);
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  transition:
    color 0.25s ease,
    background 0.25s ease;

  &:hover {
    color: var(--ink);
    background: rgba(31, 42, 68, 0.07);
  }
}

.para-eval-body {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 22px;
  align-items: center;
}

.para-eval-text {
  min-width: 0;

  .eval-feedback {
    margin: 0;
  }

  .eval-tips {
    margin-top: 6px;
  }

  .eval-spoken {
    margin-top: 8px;
    padding-top: 8px;
  }
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

// ---------------- 过渡 ----------------
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity 0.35s $ease-apple,
    transform 0.35s $ease-apple;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

// ---------------- 响应式 ----------------
@media (max-width: 1120px) {
  .quote-grid {
    grid-template-columns: 1fr;
  }

  .lib-panel {
    position: static;
  }

  .lib-list,
  .doc-list {
    max-height: 260px;
  }
}

@media (max-width: 860px) {
  .hero-strip {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .hero-mascot {
    display: none;
  }

  .record-card {
    grid-template-columns: 1fr;
  }

  .record-left {
    padding-right: 0;
    padding-bottom: 18px;
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }

  .para-eval-body {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .score-row {
    flex-wrap: wrap;
    justify-content: center;
    gap: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mic-btn,
  .para-eval,
  .para-act.is-recording {
    animation: none;
  }
}
</style>
