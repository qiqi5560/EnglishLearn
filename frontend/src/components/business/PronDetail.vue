<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { PhonemeEvalDto, PronIssueDto } from '@/types/api'

/** 音素级评测详情：单词染色（可点击展开音标明细） + 按词分组的折叠纠错清单 */
const props = defineProps<{ phoneme: PhonemeEvalDto }>()

function chipClass(score: number) {
  if (score >= 85) return 'is-ok'
  if (score >= 60) return 'is-warn'
  return 'is-bad'
}

interface IssueGroup {
  word: string
  items: PronIssueDto[]
}

/** 同一单词的问题聚合在一起，保持出现顺序 */
const groups = computed<IssueGroup[]>(() => {
  const map = new Map<string, PronIssueDto[]>()
  for (const it of props.phoneme.issues) {
    const key = it.word || '整句'
    const list = map.get(key)
    if (list) list.push(it)
    else map.set(key, [it])
  }
  return [...map.entries()].map(([word, items]) => ({ word, items }))
})

const groupMap = computed(() => new Map(groups.value.map((g) => [g.word, g])))

/** 当前选中（展开明细）的词：不选时下方不显示任何清单 */
const selected = ref<string | null>(null)

function reset() {
  selected.value = null
}
watch(() => props.phoneme, reset)

/** 点词卡：有问题的词才响应，再点一次收起 */
function onChip(word: string) {
  if (!groupMap.value.has(word)) return
  selected.value = selected.value === word ? null : word
}

/** 折叠态的一句摘要，展示在词名后面 */
function brief(it: PronIssueDto) {
  if (it.type === 'sub') return `/${it.expected}/ 读成了 /${it.got}/`
  if (it.type === 'del') return `/${it.expected}/ 没读出来`
  return `多读了 /${it.got}/`
}

/** 把一条音素错误说成人话：读错给出「读成了什么」，漏读/多读单独描述 */
function arrow(it: PronIssueDto) {
  if (it.type === 'sub') return `读成了 /${it.got}/`
  if (it.type === 'del') return '没读出来'
  return '多读了'
}
</script>

<template>
  <div class="pron-detail">
    <p class="pron-title">
      音素级检测
      <span class="pron-meta">
        语速 {{ props.phoneme.speed }} 音素/秒 · 时长 {{ props.phoneme.duration }}s
      </span>
    </p>

    <p v-if="groups.length" class="pron-summary">
      <span>有 {{ props.phoneme.issues.length }} 处发音可以更准，涉及 {{ groups.length }} 个词</span>
      <span class="pron-summary-hint">点击单词查看音标明细</span>
    </p>

    <div class="word-chips">
      <span
        v-for="(w, i) in props.phoneme.words"
        :key="i"
        class="word-chip"
        :class="[
          chipClass(w.score),
          { 'is-clickable': groupMap.has(w.word), 'is-open': selected === w.word },
        ]"
        :title="
          groupMap.has(w.word)
            ? `${w.word} ${w.score} 分 · 点击展开音标明细`
            : `${w.word} ${w.score} 分`
        "
        @click="onChip(w.word)"
      >
        {{ w.word }}<em>{{ w.score }}</em>
      </span>
    </div>

    <div v-if="selected && groupMap.has(selected)" class="issue-group">
      <button type="button" class="issue-head" @click="onChip(selected)">
        <span class="issue-word">{{ selected }}</span>
        <span class="issue-brief">{{ brief(groupMap.get(selected)!.items[0]) }}</span>
        <span
          v-if="groupMap.get(selected)!.items.length > 1"
          class="issue-count"
        >{{ groupMap.get(selected)!.items.length }} 处</span>
        <span class="issue-close" title="收起">×</span>
      </button>

      <ul class="issue-list">
        <li v-for="(it, i) in groupMap.get(selected)!.items" :key="i">
          <template v-if="it.type === 'ins'">
            <span class="issue-arrow">多读了</span>
            <span class="issue-ph">/{{ it.got }}/</span>
          </template>
          <template v-else>
            <span class="issue-ph">/{{ it.expected }}/</span>
            <span class="issue-arrow">{{ arrow(it) }}</span>
          </template>
          <span v-if="it.hint" class="issue-hint">{{ it.hint }}</span>
        </li>
      </ul>
    </div>

    <p v-if="props.phoneme.skipped.length" class="pron-skip">
      未参与评测的词：{{ props.phoneme.skipped.join('、') }}
    </p>
  </div>
</template>

<style scoped>
.pron-detail {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md, 12px);
  background: rgba(59, 111, 224, 0.05);
  border: 1px solid rgba(59, 111, 224, 0.14);
}

.pron-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
}

.pron-meta {
  font-weight: 400;
  font-size: 11px;
  color: var(--ink-3, #8a93a6);
}

.word-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.word-chip {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
  background: rgba(140, 150, 170, 0.12);
}

.word-chip em {
  font-style: normal;
  font-size: 10px;
  opacity: 0.7;
}

.word-chip.is-ok {
  background: rgba(46, 190, 130, 0.16);
  color: #1f9c68;
}

.word-chip.is-warn {
  background: rgba(245, 175, 60, 0.18);
  color: #c98213;
}

.word-chip.is-bad {
  background: rgba(238, 96, 96, 0.16);
  color: #d9534f;
}

/* 有问题的词：底部红线提示可点击展开 */
.word-chip.is-clickable {
  cursor: pointer;
  box-shadow: inset 0 -2px 0 rgba(238, 96, 96, 0.55);
}

.word-chip.is-open {
  outline: 2px solid rgba(59, 111, 224, 0.35);
}

.pron-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--ink-2, #5a6377);
}

.pron-summary-hint {
  font-size: 11px;
  color: var(--ink-3, #8a93a6);
}

.issue-group {
  margin-top: 9px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
}

.issue-head {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 5px 8px;
  border: 0;
  border-radius: 8px;
  background: none;
  font-size: 12px;
  color: var(--ink-2, #5a6377);
  text-align: left;
  cursor: pointer;
}

.issue-close {
  margin-left: 4px;
  flex-shrink: 0;
  color: var(--ink-3, #8a93a6);
  font-size: 14px;
  line-height: 1;
}

.issue-brief {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issue-count {
  margin-left: auto;
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 10px;
  color: #d9534f;
  background: rgba(238, 96, 96, 0.12);
}

.issue-list {
  margin: 0;
  padding: 2px 8px 8px 24px;
  list-style: none;
  display: grid;
  gap: 5px;
}

.issue-list li {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-2, #5a6377);
}

.issue-word {
  font-weight: 700;
  color: var(--ink);
}

.issue-ph {
  padding: 1px 6px;
  border-radius: 6px;
  background: rgba(59, 111, 224, 0.1);
  color: var(--primary);
  font-family: 'Times New Roman', serif;
}

.issue-arrow {
  color: #d9534f;
}

.issue-hint {
  color: var(--ink-3, #8a93a6);
  font-size: 11px;
}

.pron-skip {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--ink-3, #8a93a6);
}
</style>
