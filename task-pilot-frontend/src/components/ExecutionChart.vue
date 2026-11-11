<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { init, use, type EChartsType } from 'echarts/core';
import { LineChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { ChartData } from '@/types/models';
use([
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CanvasRenderer,
]);
const props = defineProps<{ data: ChartData }>();
const element = ref<HTMLDivElement>();
let chart: EChartsType | undefined, observer: ResizeObserver | undefined;
function render() {
  chart?.setOption({
    animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    color: ['#262626', '#dc2626', '#a3a3a3'],
    tooltip: { trigger: 'axis', confine: true },
    legend: {
      top: 0,
      left: 'center',
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#737373', fontSize: 12 },
    },
    grid: { top: 48, left: 42, right: 16, bottom: 28 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.data.triggerDayList,
      axisLabel: {
        color: '#737373',
        fontSize: 11,
        formatter: (value: string) => value.slice(5),
      },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#737373', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
    },
    series: [
      {
        name: '成功',
        type: 'line',
        data: props.data.triggerDayCountSucList,
        symbolSize: 5,
        smooth: true,
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.08 },
      },
      {
        name: '失败',
        type: 'line',
        data: props.data.triggerDayCountFailList,
        symbolSize: 5,
        smooth: true,
        areaStyle: { opacity: 0.08 },
        lineStyle: { width: 1.5 },
      },
      {
        name: '运行中',
        type: 'line',
        data: props.data.triggerDayCountRunningList,
        symbolSize: 5,
        smooth: true,
        areaStyle: { opacity: 0.08 },
        lineStyle: { width: 1.5, type: 'dashed' },
      },
    ],
  });
}
onMounted(() => {
  if (!element.value) return;
  chart = init(element.value);
  observer = new ResizeObserver(() => chart?.resize());
  observer.observe(element.value);
  render();
});
watch(() => props.data, render, { deep: true });
onBeforeUnmount(() => {
  observer?.disconnect();
  chart?.dispose();
});
</script>
<template>
  <div
    ref="element"
    class="h-[280px] min-w-0 w-full sm:h-[340px]"
    role="img"
    :aria-label="`执行趋势，成功 ${data.triggerCountSucTotal} 次，失败 ${data.triggerCountFailTotal} 次，运行中 ${data.triggerCountRunningTotal} 次`"
  />
</template>
