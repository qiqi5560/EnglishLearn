<template>
  <VChart class="chart" :option="option" autoresize />
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  indicators: { name: string; max: number }[]
  values: number[]
}>()

const option = computed(() => ({
  radar: {
    indicator: props.indicators,
    radius: '68%',
    axisName: { color: '#4a5268' },
    splitArea: { areaStyle: { color: ['#fff', '#f6f7fb'] } },
  },
  series: [
    {
      type: 'radar',
      data: [
        {
          value: props.values,
          name: '能力维度',
          areaStyle: { color: 'rgba(59, 111, 224, 0.2)' },
          lineStyle: { color: '#3b6fe0' },
          itemStyle: { color: '#3b6fe0' },
        },
      ],
    },
  ],
}))
</script>

<style scoped>
.chart {
  width: 100%;
  height: 260px;
}
</style>
