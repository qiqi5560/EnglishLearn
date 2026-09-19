<template>
  <span class="level-tag" :class="`lv-${level}`">{{ level }}</span>
</template>

<script setup lang="ts">
defineProps<{ level: string }>()
</script>

<style scoped lang="scss">
.level-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  border-radius: 999px;
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.35s ease;
}

.level-tag:hover {
  transform: translateY(-1px) scale(1.04);
}

.lv-A1,
.lv-A2 {
  color: #16845b;
  background: rgba(34, 160, 107, 0.12);
}

.lv-B1,
.lv-B2 {
  color: var(--primary);
  background: rgba(59, 111, 224, 0.13);
}

.lv-C1,
.lv-C2 {
  color: #b3752a;
  background: rgba(232, 162, 61, 0.16);
}
</style>