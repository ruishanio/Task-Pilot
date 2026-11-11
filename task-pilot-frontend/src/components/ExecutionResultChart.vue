<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { init, use, type EChartsType } from 'echarts/core';
import { PieChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { ChartData } from '@/types/models';

use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer]);
const props = defineProps<{ data: ChartData }>();
const items = computed(() => [
  {
    name: '成功',
    value: props.data.triggerCountSucTotal || 0,
    color: '#262626',
  },
  {
    name: '失败',
    value: props.data.triggerCountFailTotal || 0,
    color: '#dc2626',
  },
  {
    name: '运行中',
    value: props.data.triggerCountRunningTotal || 0,
    color: '#a3a3a3',
  },
]);
const total = computed(() =>
  items.value.reduce((sum, item) => sum + item.value, 0),
);
const element = ref<HTMLDivElement>();
let chart: EChartsType | undefined, observer: ResizeObserver | undefined;

function percentage(value: number): string {
  return total.value ? `${((value / total.value) * 100).toFixed(1)}%` : '—';
}
function render() {
  chart?.setOption({
    animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    tooltip: {
      show: total.value > 0,
      trigger: 'item',
      confine: true,
      formatter: '{b}：{c} 次（{d}%）',
    },
    legend: {
      bottom: 0,
      data: total.value ? items.value.map((item) => item.name) : [],
      textStyle: { color: '#737373' },
    },
    series: [
      {
        name: '执行结果',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '46%'],
        // 标签贴近图表边界，窄列中仍能完整显示名称与占比。
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: '#737373',
          alignTo: 'edge',
          edgeDistance: 8,
          distanceToLabelLine: 4,
          fontSize: 11,
        },
        labelLine: { show: true, length: 10, length2: 8 },
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 3 },
        emphasis: { scaleSize: 3 },
        // 全部为零时只画中性空环，禁止饼图默认将零值等分成三块。
        stillShowZeroSum: false,
        showEmptyCircle: true,
        emptyCircleStyle: {
          color: '#f5f5f5',
          borderColor: '#e5e5e5',
          borderWidth: 1,
        },
        data: total.value
          ? items.value.map((item) => ({
              name: item.name,
              value: item.value,
              itemStyle: { color: item.color },
            }))
          : [],
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
  <div class="relative min-w-0">
    <div
      ref="element"
      class="h-[280px] w-full sm:h-[340px]"
      role="img"
      :aria-label="
        total
          ? `执行结果占比，${items.map((item) => `${item.name} ${item.value} 次，占 ${percentage(item.value)}`).join('，')}`
          : '执行结果占比，所选时间段暂无执行数据'
      "
    />
    <div
      v-if="!total"
      class="absolute inset-x-0 bottom-0 flex justify-center gap-4 text-xs text-muted-foreground"
      aria-hidden="true"
    >
      <span
        v-for="item in items"
        :key="item.name"
        class="flex items-center gap-1.5"
      >
        <span
          class="h-2.5 w-3.5 rounded-sm"
          :style="{ backgroundColor: item.color }"
        />
        {{ item.name }}
      </span>
    </div>
    <p
      v-if="!total"
      class="pointer-events-none absolute inset-x-0 top-[44%] text-center text-xs text-muted-foreground"
    >
      暂无执行数据
    </p>
  </div>
</template>
