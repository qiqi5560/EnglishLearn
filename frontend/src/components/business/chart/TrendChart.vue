<template>
  <VChart class="chart" :style="{ height: `${height}px` }" :option="option" autoresize />
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface TrendSeries {
  name: string
  values: number[]
  color: string
  type?: 'line' | 'bar'
  area?: boolean
}

const props = withDefaults(
  defineProps<{
    labels: string[]
    series: TrendSeries[]
    height?: number
  }>(),
  { height: 260 },
)

const option = computed(() => ({
  grid: { left: 40, right: 18, top: 34, bottom: 26 },
  tooltip: { trigger: 'axis' },
  legend: {
    show: props.series.length > 1,
    top: 0,
    right: 0,
    icon: 'roundRect',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#8a93a6', fontSize: 12 },
  },
  xAxis: {
    type: 'category',
    data: props.labels,
    boundaryGap: props.series.some((s) => s.type === 'bar'),
    axisLine: { lineStyle: { color: '#e6e9f0' } },
    axisLabel: { color: '#8a93a6', fontSize: 11 },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    min: 0,
    minInterval: 1,
    axisLine: { show: false },
    axisLabel: { color: '#8a93a6', fontSize: 11 },
    splitLine: { lineStyle: { color: '#eef1f6' } },
  },
  series: props.series.map((s) => {
    const isBar = (s.type ?? 'line') === 'bar'
    return {
      name: s.name,
      type: isBar ? 'bar' : 'line',
      data: s.values,
      smooth: true,
      showSymbol: false,
      barMaxWidth: 14,
      itemStyle: isBar ? { color: s.color, borderRadius: [4, 4, 0, 0] } : { color: s.color },
      lineStyle: { color: s.color, width: 2.5 },
      ...(!isBar && s.area ? { areaStyle: { color: s.color, opacity: 0.12 } } : {}),
    }
  }),
}))
</script>

<style scoped>
.chart {
  width: 100%;
}
</style>
