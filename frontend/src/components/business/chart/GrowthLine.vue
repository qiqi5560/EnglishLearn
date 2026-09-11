<template>
  <VChart class="chart" :option="option" autoresize />
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  dates: string[]
  scores: number[]
}>()

const option = computed(() => ({
  grid: { left: 36, right: 16, top: 24, bottom: 24 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: props.dates, boundaryGap: false },
  yAxis: { type: 'value', min: 0, max: 100 },
  series: [
    {
      type: 'line',
      data: props.scores,
      smooth: true,
      lineStyle: { color: '#3b6fe0', width: 3 },
      itemStyle: { color: '#3b6fe0' },
      areaStyle: { color: 'rgba(59, 111, 224, 0.12)' },
    },
  ],
}))
</script>

<style scoped>
.chart {
  width: 100%;
  height: 240px;
}
</style>
