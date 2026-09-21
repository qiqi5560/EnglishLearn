<script setup lang="ts">
import type { PhonemeEvalDto, PronIssueDto } from '@/types/api'

/** 音素级评测详情：单词染色 + 音标级纠错清单 */
const props = defineProps<{ phoneme: PhonemeEvalDto }>()

function chipClass(score: number) {
  if (score >= 85) return 'is-ok'
  if (score >= 60) return 'is-warn'
  return 'is-bad'
}

/** 把一条音素错误说成人话：读错给出「读成了什么」，漏读/多读单独描述 */
function arrow(it: PronIssueDto) {
  if (it.type === 'sub') return `→ /${it.got}/`
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

    <div class="word-chips">
      <span
        v-for="(w, i) in props.phoneme.words"
        :key="i"
        class="word-chip"
        :class="chipClass(w.score)"
        :title="`${w.word} ${w.score} 分`"
      >
        {{ w.word }}<em>{{ w.score }}</em>
      </span>
    </div>

    <ul v-if="props.phoneme.issues.length" class="issue-list">
      <li v-for="(it, i) in props.phoneme.issues.slice(0, 6)" :key="i">
        <span class="issue-word">{{ it.word || '—' }}</span>
        <span class="issue-ph">/{{ it.expected }}/</span>
        <span class="issue-arrow">{{ arrow(it) }}</span>
        <span v-if="it.hint" class="issue-hint">{{ it.hint }}</span>
      </li>
    </ul>

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

.issue-list {
  margin: 9px 0 0;
  padding: 0;
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
