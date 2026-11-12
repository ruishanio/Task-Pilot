<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import dayjs from 'dayjs';
import { sanitizeLogContent } from '@/utils/logContent';
import {
  RefreshCw,
  Trash2,
  Square,
  FileText,
  CircleCheck,
  CircleX,
  Clock3,
} from '@lucide/vue';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import DateRangePicker from '@/components/DateRangePicker.vue';
import PageHeading from '@/components/PageHeading.vue';
import ColumnVisibility from '@/components/ColumnVisibility.vue';
import FormField from '@/components/FormField.vue';
import AppSelect from '@/components/AppSelect.vue';
import DataState from '@/components/DataState.vue';
import PagePagination from '@/components/PagePagination.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import { usePage } from '@/composables/usePage';
import { useMutation } from '@/composables/useMutation';
import { useLiveLog } from '@/composables/useLiveLog';
import { taskLogApi } from '@/services/api';
import { formatDateTime, getErrorMessage } from '@/utils/format';
import { logStatus } from '@/utils/logStatus';
import type { TaskLog, LogMeta } from '@/types/models';
const route = useRoute();
const { rows, total, page, size, loading, error, load, invalidate } =
  usePage<TaskLog>(taskLogApi.page);
const meta = ref<LogMeta | null>(null),
  metaLoading = ref(false),
  metaError = ref(''),
  dateError = ref('');
const filters = reactive({
  executorId: '',
  taskId: '',
  logStatus: '-1',
  start: dayjs()
    .subtract(6, 'day')
    .startOf('day')
    .format('YYYY-MM-DDTHH:mm:ss'),
  end: dayjs().endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
});
let metaGeneration = 0;
const { busy, error: mutationError, run } = useMutation();
const clearOpen = ref(false),
  clearType = ref('1'),
  killOpen = ref(false),
  selected = ref<TaskLog | null>(null),
  detailOpen = ref(false);
const live = useLiveLog();
const detailTab = ref('execution');
const visibleColumns = ref<string[]>([]);
const columns = [
  { key: 'address', label: '执行器地址' },
  { key: 'handler', label: '执行器处理器' },
];
// 调度失败时执行器尚未开始处理，处理结果应与调度状态分开表达。
function handleLabel(row: TaskLog) {
  return row.triggerCode === 200 ? logStatus(row).label : '未执行';
}
function taskLabel(id: number) {
  return (
    meta.value?.tasks.find((task) => task.id === id)?.taskDesc || `任务 #${id}`
  );
}
const safeHandle = computed(() =>
  sanitizeLogContent(selected.value?.handleMsg || ''),
);
// 只保留后端约定的换行和加粗标记，日志文本不能创建链接、图片或脚本。
const safeContent = computed(() => sanitizeLogContent(live.content.value));
const safeTrigger = computed(() =>
  sanitizeLogContent(selected.value?.triggerMsg || ''),
);
function resetFilters() {
  filters.taskId = String(meta.value?.selectedTaskId || 0);
  filters.logStatus = '-1';
  filters.start = dayjs()
    .subtract(6, 'day')
    .startOf('day')
    .format('YYYY-MM-DDTHH:mm:ss');
  filters.end = dayjs().endOf('day').format('YYYY-MM-DDTHH:mm:ss');
}
function reset() {
  resetFilters();
  void query();
}
async function init(
  executorId = Number(route.query.executorId || 0),
  taskId = Number(route.query.taskId || 0),
) {
  const generation = ++metaGeneration;
  invalidate();
  metaLoading.value = true;
  metaError.value = '';
  meta.value = null;
  try {
    const { data } = await taskLogApi.meta({ executorId, taskId });
    if (generation !== metaGeneration) return;
    if (!data) throw new Error('无法获取日志配置');
    meta.value = data;
    filters.executorId = String(data.selectedExecutorId);
    filters.taskId = String(data.selectedTaskId);
    resetFilters();
    await query();
  } catch (e) {
    if (generation === metaGeneration) metaError.value = getErrorMessage(e);
  } finally {
    if (generation === metaGeneration) metaLoading.value = false;
  }
}
async function query() {
  dateError.value =
    !filters.start || !filters.end || filters.start > filters.end
      ? '请选择有效的开始与结束时间'
      : '';
  if (dateError.value) {
    invalidate();
    return;
  }
  if (!Number(filters.taskId)) {
    invalidate();
    return;
  }
  await load(
    {
      executorId: Number(filters.executorId),
      taskId: Number(filters.taskId),
      logStatus: filters.logStatus,
      filterTime: `${dayjs(filters.start).format('YYYY-MM-DD HH:mm:ss')} - ${dayjs(filters.end).format('YYYY-MM-DD HH:mm:ss')}`,
    },
    true,
  );
}
function paginate(p: number, s: number) {
  page.value = p;
  size.value = s;
  void load();
}
function showDetail(row: TaskLog) {
  selected.value = row;
  detailTab.value = row.triggerCode === 200 ? 'execution' : 'trigger';
  detailOpen.value = true;
  void live.start(row);
}
watch(detailOpen, (open) => {
  if (!open) live.stop();
});
function askKill(row: TaskLog) {
  selected.value = row;
  mutationError.value = '';
  killOpen.value = true;
}
async function kill() {
  if (
    selected.value &&
    (await run(() => taskLogApi.kill(selected.value!.id), '终止请求已发送'))
  ) {
    killOpen.value = false;
    await load();
  }
}
function askClear() {
  clearType.value = '1';
  mutationError.value = '';
  clearOpen.value = true;
}
async function clear() {
  if (
    await run(
      () =>
        taskLogApi.clear({
          executorId: filters.executorId,
          taskId: filters.taskId,
          type: clearType.value,
        }),
      '日志已清理',
    )
  ) {
    clearOpen.value = false;
    await query();
  }
}
watch(
  () => route.fullPath,
  () => init(),
  { immediate: true },
);
onBeforeUnmount(() => {
  metaGeneration++;
});
</script>
<template>
  <div class="space-y-6">
    <PageHeading
      title="调度日志"
      description="追踪任务执行结果，查看日志与调度详情。"
    >
      <Button
        variant="outline"
        :disabled="!meta || !Number(filters.taskId)"
        @click="askClear"
      >
        <Trash2 />
        清理日志
      </Button>
      <Button
        variant="outline"
        size="icon"
        aria-label="刷新日志"
        :disabled="loading || metaLoading"
        @click="query"
      >
        <RefreshCw :class="{ 'animate-spin': loading }" />
      </Button>
    </PageHeading>
    <form class="flex flex-wrap items-center gap-2" @submit.prevent="query">
      <div class="w-full sm:w-44">
        <AppSelect
          v-model="filters.executorId"
          label="日志执行器"
          :disabled="metaLoading"
          :options="
            meta?.executors.map((e) => ({ label: e.title, value: e.id })) || []
          "
          @update:model-value="init(Number($event), 0)"
        />
      </div>
      <div class="w-full sm:w-52">
        <AppSelect
          v-model="filters.taskId"
          label="日志任务"
          :disabled="!meta?.tasks.length"
          :options="
            meta?.tasks.map((t) => ({ label: t.taskDesc, value: t.id })) || []
          "
          @update:model-value="query"
        />
      </div>
      <div class="w-32">
        <AppSelect
          v-model="filters.logStatus"
          label="日志状态"
          :options="meta?.logStatusOptions || []"
          @update:model-value="query"
        />
      </div>
      <DateRangePicker
        v-model:start="filters.start"
        v-model:end="filters.end"
        @apply="query"
      />
      <Button type="button" variant="ghost" :disabled="!meta" @click="reset">
        重置
      </Button>
      <div class="ml-auto hidden lg:block">
        <ColumnVisibility v-model="visibleColumns" :columns="columns" />
      </div>
      <p v-if="dateError" role="alert" class="w-full text-sm text-destructive">
        {{ dateError }}
      </p>
    </form>
    <div>
      <div class="overflow-hidden rounded-xl border bg-card">
        <DataState
          :loading="loading || metaLoading"
          :error="metaError || error"
          :empty="!rows.length"
          :empty-text="
            meta && !meta.tasks.length
              ? '该执行器暂无任务'
              : '所选任务在此时间段暂无执行记录'
          "
          @retry="init()"
        >
          <div class="hidden lg:block">
            <Table
              :class="
                visibleColumns.length ? 'min-w-[1100px]' : 'min-w-[760px]'
              "
            >
              <TableHeader>
                <TableRow>
                  <TableHead class="pl-5">执行记录</TableHead>
                  <TableHead>触发时间</TableHead>
                  <TableHead>调度状态</TableHead>
                  <TableHead>处理结果</TableHead>
                  <TableHead v-if="visibleColumns.includes('address')">
                    执行器地址
                  </TableHead>
                  <TableHead v-if="visibleColumns.includes('handler')">
                    执行器处理器
                  </TableHead>
                  <TableHead class="w-32 pr-5 text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="row in rows" :key="row.id">
                  <TableCell class="max-w-64 py-4 pl-5">
                    <button
                      class="block max-w-full truncate text-left font-medium hover:underline"
                      @click="showDetail(row)"
                    >
                      {{ taskLabel(row.taskId) }}
                    </button>
                    <span
                      class="mt-1 block font-mono text-xs text-muted-foreground"
                    >
                      #{{ row.id }}
                    </span>
                  </TableCell>
                  <TableCell class="text-xs tabular-nums">
                    {{ formatDateTime(row.triggerTime) }}
                  </TableCell>
                  <TableCell>
                    <span
                      class="inline-flex items-center gap-1.5 text-xs"
                      :class="
                        row.triggerCode === 200
                          ? 'text-muted-foreground'
                          : 'text-destructive'
                      "
                    >
                      <CircleCheck
                        v-if="row.triggerCode === 200"
                        class="size-3.5"
                      />
                      <CircleX v-else class="size-3.5" />
                      {{ row.triggerCode === 200 ? '调度成功' : '调度失败' }}
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge
                      :variant="
                        row.triggerCode === 200 &&
                        logStatus(row).kind === 'failure'
                          ? 'destructive'
                          : 'outline'
                      "
                    >
                      <Clock3 v-if="logStatus(row).kind === 'running'" />
                      {{ handleLabel(row) }}
                    </Badge>
                  </TableCell>
                  <TableCell
                    v-if="visibleColumns.includes('address')"
                    class="font-mono text-xs"
                  >
                    {{ row.executorAddress || '—' }}
                  </TableCell>
                  <TableCell
                    v-if="visibleColumns.includes('handler')"
                    class="text-xs"
                  >
                    {{ row.executorHandler || '—' }}
                  </TableCell>
                  <TableCell class="pr-5 text-right">
                    <Button
                      size="icon-sm"
                      variant="ghost"
                      :aria-label="`查看日志 ${row.id}`"
                      @click="showDetail(row)"
                    >
                      <FileText />
                    </Button>
                    <Button
                      v-if="logStatus(row).kind === 'running'"
                      size="icon-sm"
                      variant="ghost"
                      class="text-destructive"
                      :aria-label="`终止执行 ${row.id}`"
                      :disabled="busy"
                      @click="askKill(row)"
                    >
                      <Square />
                    </Button>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
          <div class="divide-y lg:hidden">
            <article v-for="row in rows" :key="row.id" class="space-y-3 p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <button
                    class="max-w-full truncate text-left text-sm font-medium hover:underline"
                    @click="showDetail(row)"
                  >
                    {{ taskLabel(row.taskId) }}
                  </button>
                  <p class="mt-1 text-xs text-muted-foreground">
                    #{{ row.id }} · {{ formatDateTime(row.triggerTime) }}
                  </p>
                </div>
                <Button
                  size="icon-sm"
                  variant="ghost"
                  :aria-label="`查看日志 ${row.id}`"
                  @click="showDetail(row)"
                >
                  <FileText />
                </Button>
              </div>
              <div class="flex items-center justify-between">
                <Badge variant="outline">{{ handleLabel(row) }}</Badge>
                <span
                  class="text-xs"
                  :class="
                    row.triggerCode === 200
                      ? 'text-muted-foreground'
                      : 'text-destructive'
                  "
                >
                  {{ row.triggerCode === 200 ? '调度成功' : '调度失败' }}
                </span>
                <Button
                  v-if="logStatus(row).kind === 'running'"
                  size="sm"
                  variant="ghost"
                  :disabled="busy"
                  @click="askKill(row)"
                >
                  <Square />
                  终止
                </Button>
              </div>
            </article>
          </div>
        </DataState>
      </div>
      <PagePagination
        :page="page"
        :size="size"
        :total="total"
        :loading="loading || metaLoading"
        @change="paginate"
      />
    </div>
  </div>
  <Dialog v-model:open="detailOpen">
    <!-- 固定弹窗高度并限制在视口内，切换标签或日志更新时仅让内容区滚动。 -->
    <DialogContent
      class="flex h-[640px] max-h-[92svh] flex-col gap-0 overflow-hidden p-0 sm:max-w-[960px]"
    >
      <DialogHeader class="shrink-0 border-b px-6 py-5">
        <DialogTitle>执行日志 #{{ selected?.id }}</DialogTitle>
        <DialogDescription>
          {{ selected ? taskLabel(selected.taskId) : '' }} ·
          {{ formatDateTime(selected?.triggerTime) }}
        </DialogDescription>
      </DialogHeader>
      <div class="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto p-6">
        <dl
          class="grid shrink-0 gap-4 rounded-lg border bg-muted/20 p-4 text-xs sm:grid-cols-3"
        >
          <div>
            <dt class="mb-2 text-muted-foreground">处理结果</dt>
            <dd>{{ selected ? handleLabel(selected) : '—' }}</dd>
          </div>
          <div>
            <dt class="mb-2 text-muted-foreground">执行器地址</dt>
            <dd class="break-all font-mono">
              {{ selected?.executorAddress || '—' }}
            </dd>
          </div>
          <div>
            <dt class="mb-2 text-muted-foreground">执行器处理器</dt>
            <dd class="break-all font-mono">
              {{ selected?.executorHandler || '—' }}
            </dd>
          </div>
        </dl>
        <Tabs v-model="detailTab" class="min-h-48 flex-1 gap-0">
          <div class="flex shrink-0 flex-wrap items-center justify-between gap-2">
            <TabsList>
              <TabsTrigger value="execution">执行日志</TabsTrigger>
              <TabsTrigger value="trigger">调度信息</TabsTrigger>
            </TabsList>
            <span
              v-if="live.following.value"
              class="text-xs text-muted-foreground"
              role="status"
            >
              每 3 秒自动更新
            </span>
            <Button
              v-else-if="selected?.triggerCode === 200"
              size="sm"
              variant="ghost"
              :disabled="live.loading.value"
              @click="live.start(selected!)"
            >
              <RefreshCw />
              重新加载
            </Button>
          </div>
          <div
            :key="detailTab"
            class="mt-3 min-h-0 flex-1 overflow-y-auto [scrollbar-gutter:stable]"
          >
            <TabsContent
              value="execution"
              class="m-0 flex min-h-full flex-col gap-3"
            >
              <div
                v-if="selected?.triggerCode === 200"
                class="min-h-64 flex-1 rounded-lg border bg-muted/40 p-4 font-mono text-xs leading-6 break-words whitespace-pre-wrap"
                v-html="
                  safeContent ||
                  (live.loading.value ? '正在读取日志…' : '暂无日志内容')
                "
              />
              <div
                v-else
                class="flex min-h-64 flex-1 flex-col items-center justify-center rounded-lg border border-dashed p-4 text-center text-sm text-muted-foreground"
              >
                任务未成功调度，暂无执行日志。
                <Button
                  variant="link"
                  class="block w-full"
                  @click="detailTab = 'trigger'"
                >
                  查看调度失败原因
                </Button>
              </div>
              <p
                v-if="live.error.value"
                role="alert"
                class="shrink-0 text-sm text-destructive"
              >
                {{ live.error.value }}
              </p>
              <div
                v-if="safeHandle"
                class="shrink-0 rounded-lg border p-4 text-xs leading-6 break-words"
                v-html="safeHandle"
              />
            </TabsContent>
            <TabsContent value="trigger" class="m-0 flex min-h-full flex-col">
              <div
                class="min-h-64 flex-1 rounded-lg border bg-muted/40 p-4 font-mono text-xs leading-6 break-words whitespace-pre-wrap"
                v-html="safeTrigger || '暂无调度详情'"
              />
            </TabsContent>
          </div>
        </Tabs>
      </div>
    </DialogContent>
  </Dialog>
  <Dialog
    :open="clearOpen"
    @update:open="
      (v) => {
        if (!busy) clearOpen = v;
      }
    "
  >
    <DialogContent>
      <DialogHeader>
        <DialogTitle>清理执行日志</DialogTitle>
        <DialogDescription>
          仅清理当前所选任务的日志。此操作无法撤销。
        </DialogDescription>
      </DialogHeader>
      <form class="space-y-5" @submit.prevent="clear">
        <FormField id="clear-executor" label="执行器">
          <Input
            id="clear-executor"
            :model-value="
              meta?.executors.find((e) => String(e.id) === filters.executorId)
                ?.title || filters.executorId
            "
            readonly
          />
        </FormField>
        <FormField id="clear-task" label="任务">
          <Input
            id="clear-task"
            :model-value="
              meta?.tasks.find((t) => String(t.id) === filters.taskId)
                ?.taskDesc || filters.taskId
            "
            readonly
          />
        </FormField>
        <FormField id="clear-type" label="清理策略">
          <AppSelect
            id="clear-type"
            v-model="clearType"
            label="清理策略"
            :options="meta?.clearLogOptions || []"
          />
        </FormField>
        <p v-if="mutationError" role="alert" class="text-sm text-destructive">
          {{ mutationError }}
        </p>
        <DialogFooter>
          <Button
            variant="outline"
            type="button"
            :disabled="busy"
            @click="clearOpen = false"
          >
            取消
          </Button>
          <Button variant="destructive" type="submit" :disabled="busy">
            {{ busy ? '清理中…' : '确认清理' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
  <ConfirmDialog
    v-model="killOpen"
    title="终止本次执行？"
    :description="`将向执行器发送终止请求，日志 #${selected?.id}。`"
    :busy="busy"
    :error="mutationError"
    destructive
    @confirm="kill"
  />
</template>
