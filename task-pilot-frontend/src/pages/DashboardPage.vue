<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import dayjs from 'dayjs';
import {
  ListTodo,
  Server,
  PlayCircle,
  RefreshCw,
  ArrowUpRight,
} from '@lucide/vue';
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import PageHeading from '@/components/PageHeading.vue';
import DateRangePicker from '@/components/DateRangePicker.vue';
import { Button } from '@/components/ui/button';
import { auth } from '@/stores/auth';
import ExecutionChart from '@/components/ExecutionChart.vue';
import ExecutionResultChart from '@/components/ExecutionResultChart.vue';
import DataState from '@/components/DataState.vue';
import { frontendApi } from '@/services/api';
import { getErrorMessage } from '@/utils/format';
import type { Dashboard, ChartData } from '@/types/models';
const summary = ref<Dashboard | null>(null),
  chart = ref<ChartData | null>(null);
const loading = ref(true),
  chartLoading = ref(true),
  error = ref(''),
  chartError = ref('');
const range = reactive({
  start: dayjs()
    .subtract(6, 'day')
    .startOf('day')
    .format('YYYY-MM-DDTHH:mm:ss'),
  end: dayjs().endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
});
let generation = 0,
  disposed = false;
const metrics = computed(() => [
  {
    title: '任务总数',
    value: summary.value?.taskInfoCount,
    unit: '个',
    description: '当前已配置的调度任务',
    to: '/task_info',
    icon: ListTodo,
  },
  {
    title: '调度次数',
    value: summary.value?.taskLogCount,
    unit: '次',
    description: '累计调度记录',
    to: '/task_log',
    icon: PlayCircle,
  },
  {
    title: '在线执行器',
    value: summary.value?.executorCount,
    unit: '台',
    description: '当前在线执行器节点',
    to: auth.user?.isAdmin ? '/executor' : undefined,
    icon: Server,
  },
]);
async function loadSummary() {
  loading.value = true;
  error.value = '';
  try {
    const { data } = await frontendApi.dashboard();
    if (disposed) return;
    if (!data) throw new Error('统计数据暂不可用');
    summary.value = data;
  } catch (e) {
    if (!disposed) error.value = getErrorMessage(e);
  } finally {
    if (!disposed) loading.value = false;
  }
}
async function loadChart() {
  // 连续修改时间区间时，仅应用最后一次请求，避免旧数据覆盖新范围。
  const current = ++generation;
  chartError.value = '';
  if (!range.start || !range.end || range.start > range.end) {
    chartError.value = '请选择有效的开始与结束时间';
    chartLoading.value = false;
    return;
  }
  chartLoading.value = true;
  try {
    const { data } = await frontendApi.chartInfo({
      startDate: dayjs(range.start).format('YYYY-MM-DD HH:mm:ss'),
      endDate: dayjs(range.end).format('YYYY-MM-DD HH:mm:ss'),
    });
    if (current !== generation) return;
    if (!data) throw new Error('图表数据暂不可用');
    chart.value = data;
  } catch (e) {
    if (current === generation) chartError.value = getErrorMessage(e);
  } finally {
    if (current === generation) chartLoading.value = false;
  }
}
onMounted(() => {
  void loadSummary();
  void loadChart();
});
onBeforeUnmount(() => {
  generation++;
  disposed = true;
});
</script>
<template>
  <div class="space-y-7">
    <PageHeading
      title="工作台"
      description="掌握调度运行情况，从这里开始日常管理。"
    >
      <Button
        variant="outline"
        :disabled="loading || chartLoading"
        @click="
          loadSummary();
          loadChart();
        "
      >
        <RefreshCw :class="{ 'animate-spin': loading || chartLoading }" />
        刷新
      </Button>
      <Button as-child>
        <RouterLink to="/task_info">
          管理任务
          <ArrowUpRight />
        </RouterLink>
      </Button>
    </PageHeading>
    <DataState
      v-if="error"
      :loading="false"
      :error="error"
      :empty="false"
      @retry="loadSummary"
    />
    <div class="grid grid-cols-3 gap-3 sm:gap-4">
      <Card
        v-for="metric in metrics"
        :key="metric.title"
        class="gap-2 py-4 md:gap-3 md:py-5"
      >
        <CardHeader class="flex items-center justify-between px-3 md:px-4">
          <CardDescription class="text-xs md:text-sm">
            {{ metric.title }}
          </CardDescription>
          <component
            :is="metric.icon"
            class="hidden size-4 text-muted-foreground sm:block"
          />
        </CardHeader>
        <CardContent class="space-y-4 px-3 md:px-4">
          <Skeleton v-if="loading" class="h-10 w-full max-w-24" />
          <p
            v-else
            class="flex flex-wrap items-baseline gap-x-1 gap-y-0 md:gap-x-2"
          >
            <span
              class="max-w-full text-xl font-semibold tracking-tight break-all tabular-nums md:text-3xl"
            >
              {{ metric.value?.toLocaleString() ?? '—' }}
            </span>
            <span class="text-sm text-muted-foreground">{{ metric.unit }}</span>
          </p>
          <div class="hidden items-center justify-between md:flex">
            <p class="text-xs text-muted-foreground">
              {{ metric.description }}
            </p>
            <RouterLink
              v-if="metric.to"
              :to="metric.to"
              :aria-label="`查看${metric.title}`"
              class="rounded-sm p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-ring"
            >
              <ArrowUpRight class="size-3.5" />
            </RouterLink>
          </div>
        </CardContent>
      </Card>
    </div>
    <section class="space-y-4" aria-labelledby="report-title">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="report-title" class="text-base font-semibold">调度报表</h2>
          <p class="mt-1 text-xs text-muted-foreground">
            所选时间区间的趋势与结果分布
          </p>
        </div>
        <DateRangePicker
          v-model:start="range.start"
          v-model:end="range.end"
          @apply="loadChart"
        />
      </div>
      <DataState
        v-if="chartError"
        :loading="false"
        :error="chartError"
        :empty="false"
        @retry="loadChart"
      />
      <div
        v-else
        class="grid gap-4 xl:grid-cols-[minmax(0,1.8fr)_minmax(320px,1fr)]"
      >
        <Card class="min-w-0 py-5">
          <CardHeader class="space-y-1">
            <CardTitle>调度趋势</CardTitle>
            <CardDescription>按日查看成功、失败与运行中的次数</CardDescription>
          </CardHeader>
          <CardContent>
            <Skeleton
              v-if="chartLoading"
              class="h-[280px] w-full sm:h-[340px]"
            />
            <ExecutionChart v-else-if="chart" :data="chart" />
            <div
              class="mt-3 flex items-center justify-between border-t pt-4 text-xs"
            >
              <span class="text-muted-foreground">区间调度总量</span>
              <span class="font-medium tabular-nums">
                {{
                  chartLoading
                    ? '—'
                    : (
                        (chart?.triggerCountSucTotal || 0) +
                        (chart?.triggerCountFailTotal || 0) +
                        (chart?.triggerCountRunningTotal || 0)
                      ).toLocaleString()
                }}
                次
              </span>
            </div>
          </CardContent>
        </Card>
        <Card class="min-w-0 py-5">
          <CardHeader class="space-y-1">
            <CardTitle>执行结果</CardTitle>
            <CardDescription>各状态在所选时间区间的占比</CardDescription>
          </CardHeader>
          <CardContent>
            <Skeleton
              v-if="chartLoading"
              class="h-[280px] w-full sm:h-[340px]"
            />
            <ExecutionResultChart v-else-if="chart" :data="chart" />
            <div class="mt-3 grid grid-cols-3 gap-2 border-t pt-4 text-xs">
              <div>
                <span class="text-muted-foreground">成功</span>
                <span class="ml-2 font-medium tabular-nums">
                  {{ chartLoading ? '—' : chart?.triggerCountSucTotal || 0 }}
                </span>
              </div>
              <div>
                <span class="text-muted-foreground">失败</span>
                <span class="ml-2 font-medium tabular-nums">
                  {{ chartLoading ? '—' : chart?.triggerCountFailTotal || 0 }}
                </span>
              </div>
              <div>
                <span class="text-muted-foreground">运行中</span>
                <span class="ml-2 font-medium tabular-nums">
                  {{
                    chartLoading ? '—' : chart?.triggerCountRunningTotal || 0
                  }}
                </span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </section>
  </div>
</template>
