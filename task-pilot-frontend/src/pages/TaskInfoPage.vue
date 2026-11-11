<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  Plus,
  Search,
  SlidersHorizontal,
  X,
  RefreshCw,
  Clock3,
} from '@lucide/vue';
import { useRouter } from 'vue-router';
import PageHeading from '@/components/PageHeading.vue';
import ColumnVisibility from '@/components/ColumnVisibility.vue';
import TaskStatus from '@/components/TaskStatus.vue';
import TaskActions from '@/components/TaskActions.vue';
import TaskDetails from '@/components/TaskDetails.vue';
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
} from '@/components/ui/popover';
import TaskInfoDialog from '@/components/TaskInfoDialog.vue';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
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
import FormField from '@/components/FormField.vue';
import AppSelect from '@/components/AppSelect.vue';
import DataState from '@/components/DataState.vue';
import PagePagination from '@/components/PagePagination.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import TaskEditor from '@/components/TaskEditor.vue';
import { usePage } from '@/composables/usePage';
import { useAutoSearch } from '@/composables/useAutoSearch';
import { useMutation } from '@/composables/useMutation';
import { taskInfoApi, executorApi } from '@/services/api';
import { formatDateTime, getErrorMessage } from '@/utils/format';
import type { Task, TaskMeta, Executor } from '@/types/models';
const router = useRouter();
const visibleColumns = ref(['status', 'schedule', 'author', 'next']);
const columns = [
  { key: 'id', label: '任务 ID' },
  { key: 'executor', label: '执行器' },
  { key: 'status', label: '状态', default: true },
  { key: 'schedule', label: '调度规则', default: true },
  { key: 'handler', label: '执行器处理器' },
  { key: 'author', label: '负责人', default: true },
  { key: 'next', label: '下次触发', default: true },
];
const filterOpen = ref(false),
  detailOpen = ref(false),
  detailTask = ref<Task | null>(null);
const extraFilters = reactive({
  triggerStatus: '-1',
  taskDesc: '',
  executorHandler: '',
  author: '',
});
const { rows, total, page, size, loading, error, load } = usePage<Task>(
  taskInfoApi.page,
);
const meta = ref<TaskMeta | null>(null),
  metaError = ref(''),
  metaLoading = ref(true);
const filters = reactive({
  executorId: '',
  triggerStatus: '-1',
  taskDesc: '',
  taskName: '',
  executorHandler: '',
  author: '',
});
const appliedSearch = ref('');
const extraCount = computed(
  () =>
    [filters.taskDesc, filters.executorHandler, filters.author].filter(Boolean)
      .length + Number(filters.triggerStatus !== '-1'),
);
const hasFilters = computed(
  () => !!appliedSearch.value || extraCount.value > 0,
);
const editorOpen = ref(false),
  editing = ref<Task | null>(null);
const { busy, error: mutationError, run } = useMutation();
const selected = ref<Task | null>(null),
  action = ref<'remove' | 'start' | 'stop'>('remove'),
  confirmOpen = ref(false);
const triggerOpen = ref(false);
const triggerForm = ref({ executorParam: '', addressList: '' });
const infoOpen = ref(false),
  infoTask = ref<Task | null>(null),
  infoExecutor = ref<Executor | null>(null),
  infoLines = ref<string[]>([]);
const infoKind = ref<'times' | 'nodes'>('times');
const stateLoading = computed(() => loading.value || metaLoading.value);
const confirmTitle = computed(
  () =>
    ({ remove: '删除任务？', start: '启用任务？', stop: '暂停任务？' })[
      action.value
    ],
);
const confirmDescription = computed(() =>
  action.value === 'remove'
    ? `将永久删除「${selected.value?.taskDesc}」及其执行日志、脚本历史，无法撤销。`
    : action.value === 'start'
      ? `启用「${selected.value?.taskDesc}」后，将按已配置的调度规则执行。`
      : `暂停「${selected.value?.taskDesc}」的自动调度。已触发的执行不会被终止。`,
);
async function init() {
  metaLoading.value = true;
  metaError.value = '';
  try {
    const { data } = await taskInfoApi.meta();
    if (!data) throw new Error('无法获取任务配置');
    meta.value = data;
    filters.executorId = String(data.selectedExecutorId);
    await query();
  } catch (e) {
    metaError.value = getErrorMessage(e);
  } finally {
    metaLoading.value = false;
  }
}
const { search: query, onEnter: searchOnEnter } = useAutoSearch(
  () => filters.taskName,
  () => {
    if (!meta.value || !Number(filters.executorId)) return;
    appliedSearch.value = filters.taskName;
    return load({ ...filters, executorId: Number(filters.executorId) }, true);
  },
);
function paginate(p: number, s: number) {
  page.value = p;
  size.value = s;
  void load();
}
function edit(row?: Task) {
  editing.value = row || null;
  editorOpen.value = true;
}
function ask(row: Task, kind: typeof action.value) {
  selected.value = row;
  action.value = kind;
  mutationError.value = '';
  confirmOpen.value = true;
}
async function confirm() {
  if (
    selected.value &&
    (await run(
      () => taskInfoApi[action.value](selected.value!.id),
      action.value === 'remove'
        ? '任务已删除'
        : action.value === 'start'
          ? '任务已启用'
          : '任务已暂停',
    ))
  ) {
    confirmOpen.value = false;
    await load();
  }
}
function trigger(row: Task) {
  selected.value = row;
  Object.assign(triggerForm.value, {
    executorParam: row.executorParam || '',
    addressList: '',
  });
  mutationError.value = '';
  triggerOpen.value = true;
}
async function execute() {
  if (
    selected.value &&
    (await run(
      () =>
        taskInfoApi.trigger({ id: selected.value!.id, ...triggerForm.value }),
      '已发送执行请求，可在执行日志中查看结果',
    ))
  )
    triggerOpen.value = false;
}
async function nextTimes(row: Task) {
  await run(async () => {
    const { data } = await taskInfoApi.nextTriggerTime({
      scheduleType: row.scheduleType,
      scheduleConf: row.scheduleConf,
    });
    infoKind.value = 'times';
    infoTask.value = row;
    infoExecutor.value = null;
    infoLines.value = data || [];
    infoOpen.value = true;
  });
}
async function nodes(row: Task) {
  await run(async () => {
    const { data } = await executorApi.loadById(row.executorId);
    if (!data) throw new Error('执行器信息暂不可用');
    infoKind.value = 'nodes';
    infoTask.value = row;
    infoExecutor.value = data;
    infoLines.value = data.registryList || [];
    infoOpen.value = true;
  });
}
function reset() {
  Object.assign(filters, {
    executorId: String(meta.value?.selectedExecutorId || ''),
    triggerStatus: '-1',
    taskName: '',
    taskDesc: '',
    executorHandler: '',
    author: '',
  });
  void query();
}
async function toggle(row: Task) {
  if (
    await run(
      () => taskInfoApi[row.triggerStatus === 1 ? 'stop' : 'start'](row.id),
      row.triggerStatus === 1 ? '任务已暂停' : '任务已启用',
    )
  )
    await load();
}
function applyExtra() {
  // 状态与其他高级条件一起应用，关闭浮层时不提交尚未确认的草稿。
  Object.assign(filters, extraFilters);
  filterOpen.value = false;
  void query();
}
function openDetails(row: Task) {
  detailTask.value = row;
  detailOpen.value = true;
}
function logs(row: Task) {
  void router.push({
    path: '/task_log',
    query: { executorId: row.executorId, taskId: row.id },
  });
}
function scheduleLabel(row: Task) {
  return row.scheduleType === 'NONE'
    ? '手动触发'
    : row.scheduleType === 'FIX_RATE'
      ? `每 ${row.scheduleConf} 秒`
      : row.scheduleConf;
}
onMounted(init);
</script>
<template>
  <div class="space-y-6">
    <PageHeading
      title="任务管理"
      description="配置调度规则，管理任务的启停与执行。"
    >
      <Button
        variant="outline"
        size="icon"
        aria-label="刷新任务"
        :disabled="stateLoading"
        @click="metaError ? init() : load()"
      >
        <RefreshCw :class="{ 'animate-spin': stateLoading }" />
      </Button>
      <Button :disabled="!meta?.executors.length" @click="edit()">
        <Plus />
        新建任务
      </Button>
    </PageHeading>
    <div class="space-y-4">
      <div
        class="flex flex-wrap items-center gap-2"
        role="search"
        aria-label="任务筛选"
      >
        <div class="relative min-w-0 flex-1 sm:max-w-72">
          <Search
            class="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            v-model="filters.taskName"
            @keydown.enter="searchOnEnter"
            aria-label="搜索任务名称"
            placeholder="搜索任务名称…"
            class="pl-9"
          />
        </div>
        <div class="w-full sm:w-48">
          <AppSelect
            v-model="filters.executorId"
            label="筛选执行器"
            :disabled="!meta"
            :options="
              meta?.executors.map((e) => ({ label: e.title, value: e.id })) ||
              []
            "
            @update:model-value="query"
          />
        </div>
        <Popover
          v-model:open="filterOpen"
          @update:open="
            (v) => {
              if (v)
                Object.assign(extraFilters, {
                  triggerStatus: filters.triggerStatus,
                  taskDesc: filters.taskDesc,
                  executorHandler: filters.executorHandler,
                  author: filters.author,
                });
            }
          "
        >
          <PopoverTrigger as-child>
            <Button type="button" variant="outline" :disabled="!meta">
              <SlidersHorizontal />
              筛选
              <Badge v-if="extraCount" variant="secondary" class="ml-1 px-1.5">
                {{ extraCount }}
              </Badge>
            </Button>
          </PopoverTrigger>
          <PopoverContent
            align="start"
            class="max-h-(--reka-popover-content-available-height) w-80 max-w-[calc(100vw-2rem)] overflow-y-auto p-4"
          >
            <h2 class="mb-4 font-medium">更多筛选</h2>
            <div class="space-y-4">
              <FormField id="filter-status" label="任务状态">
                <AppSelect
                  id="filter-status"
                  v-model="extraFilters.triggerStatus"
                  label="任务状态"
                  :options="[
                    { label: '全部状态', value: '-1' },
                    { label: '已启用', value: '1' },
                    { label: '已暂停', value: '0' },
                  ]"
                />
              </FormField>
              <FormField id="filter-desc" label="任务描述">
                <Input
                  id="filter-desc"
                  v-model="extraFilters.taskDesc"
                  @keydown.enter="searchOnEnter($event, applyExtra)"
                />
              </FormField>
              <FormField id="filter-handler" label="执行器处理器">
                <Input
                  id="filter-handler"
                  v-model="extraFilters.executorHandler"
                  @keydown.enter="searchOnEnter($event, applyExtra)"
                />
              </FormField>
              <FormField id="filter-author" label="负责人">
                <Input
                  id="filter-author"
                  v-model="extraFilters.author"
                  @keydown.enter="searchOnEnter($event, applyExtra)"
                />
              </FormField>
              <div class="flex justify-end gap-2 border-t pt-3">
                <Button
                  variant="ghost"
                  @click="
                    Object.assign(extraFilters, {
                      triggerStatus: '-1',
                      taskDesc: '',
                      executorHandler: '',
                      author: '',
                    });
                    applyExtra();
                  "
                >
                  清空
                </Button>
                <Button @click="applyExtra">应用筛选</Button>
              </div>
            </div>
          </PopoverContent>
        </Popover>
        <Button v-if="hasFilters" type="button" variant="ghost" @click="reset">
          <X />
          重置
        </Button>
        <div class="ml-auto flex items-center gap-3">
          <span
            class="hidden text-xs whitespace-nowrap text-muted-foreground sm:block"
          >
            {{ stateLoading ? '正在加载…' : `共 ${total} 个任务` }}
          </span>
          <div class="hidden lg:block">
            <ColumnVisibility v-model="visibleColumns" :columns="columns" />
          </div>
        </div>
      </div>
      <div
        v-if="extraCount"
        class="flex flex-wrap gap-2 text-xs text-muted-foreground"
      >
        <span
          v-if="filters.triggerStatus !== '-1'"
          class="rounded-md border px-2 py-1"
        >
          状态：{{ filters.triggerStatus === '1' ? '已启用' : '已暂停' }}
        </span>
        <span v-if="filters.taskDesc" class="rounded-md border px-2 py-1">
          描述：{{ filters.taskDesc }}
        </span>
        <span
          v-if="filters.executorHandler"
          class="rounded-md border px-2 py-1"
        >
          处理器：{{ filters.executorHandler }}
        </span>
        <span v-if="filters.author" class="rounded-md border px-2 py-1">
          负责人：{{ filters.author }}
        </span>
      </div>
      <div class="overflow-hidden rounded-xl border">
        <DataState
          :loading="stateLoading"
          :error="metaError || error"
          :empty="!rows.length"
          :empty-text="
            hasFilters
              ? '没有符合条件的任务，试试调整筛选。'
              : '当前执行器还没有任务。'
          "
          @retry="metaError || !meta ? init() : load()"
        >
          <template #empty-action>
            <Button v-if="hasFilters" variant="outline" @click="reset">
              重置筛选
            </Button>
            <Button v-else :disabled="!meta?.executors.length" @click="edit()">
              <Plus />
              新建任务
            </Button>
          </template>
          <div class="hidden lg:block">
            <Table
              class="table-fixed"
              :class="
                visibleColumns.length > 4 ? 'min-w-[1100px]' : 'min-w-[780px]'
              "
            >
              <TableHeader class="bg-muted/40">
                <TableRow>
                  <TableHead
                    v-if="visibleColumns.includes('id')"
                    class="w-16 pl-4"
                  >
                    ID
                  </TableHead>
                  <TableHead class="w-[30%] pl-4">任务</TableHead>
                  <TableHead
                    v-if="visibleColumns.includes('executor')"
                    class="w-40"
                  >
                    执行器
                  </TableHead>
                  <TableHead
                    v-if="visibleColumns.includes('status')"
                    class="w-28"
                  >
                    状态
                  </TableHead>
                  <TableHead
                    v-if="visibleColumns.includes('schedule')"
                    class="w-40"
                  >
                    调度规则
                  </TableHead>
                  <TableHead
                    v-if="visibleColumns.includes('handler')"
                    class="w-44"
                  >
                    执行器处理器
                  </TableHead>
                  <TableHead
                    v-if="visibleColumns.includes('author')"
                    class="w-24"
                  >
                    负责人
                  </TableHead>
                  <TableHead
                    v-if="visibleColumns.includes('next')"
                    class="w-40"
                  >
                    下次触发
                  </TableHead>
                  <TableHead class="w-24 pr-4 text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="row in rows" :key="row.id">
                  <TableCell
                    v-if="visibleColumns.includes('id')"
                    class="pl-4 text-xs text-muted-foreground"
                  >
                    {{ row.id }}
                  </TableCell>
                  <TableCell class="py-4 pl-4">
                    <button
                      class="block max-w-full truncate text-left font-medium hover:underline focus-visible:outline-2 focus-visible:outline-ring"
                      :title="row.taskDesc"
                      @click="openDetails(row)"
                    >
                      {{ row.taskDesc }}
                    </button>
                    <p
                      class="mt-1 truncate font-mono text-[11px] text-muted-foreground"
                      :title="row.taskName"
                    >
                      {{ row.taskName }}
                    </p>
                  </TableCell>
                  <TableCell
                    v-if="visibleColumns.includes('executor')"
                    class="truncate text-xs"
                  >
                    {{
                      meta?.executors.find((e) => e.id === row.executorId)
                        ?.title || row.executorId
                    }}
                  </TableCell>
                  <TableCell v-if="visibleColumns.includes('status')">
                    <TaskStatus :enabled="row.triggerStatus === 1" />
                  </TableCell>
                  <TableCell v-if="visibleColumns.includes('schedule')">
                    <p
                      class="truncate font-mono text-xs"
                      :title="scheduleLabel(row)"
                    >
                      {{ scheduleLabel(row) }}
                    </p>
                    <p class="mt-1 text-[11px] text-muted-foreground">
                      {{
                        row.scheduleType === 'CRON'
                          ? 'Cron 定时调度'
                          : row.scheduleType === 'FIX_RATE'
                            ? '固定频率'
                            : '无自动调度'
                      }}
                    </p>
                  </TableCell>
                  <TableCell
                    v-if="visibleColumns.includes('handler')"
                    class="truncate font-mono text-xs"
                    :title="row.executorHandler"
                  >
                    {{ row.executorHandler || '—' }}
                  </TableCell>
                  <TableCell
                    v-if="visibleColumns.includes('author')"
                    class="truncate text-xs"
                  >
                    {{ row.author || '—' }}
                  </TableCell>
                  <TableCell
                    v-if="visibleColumns.includes('next')"
                    class="text-xs text-muted-foreground"
                  >
                    {{
                      row.triggerStatus === 1 && Number(row.triggerNextTime) > 0
                        ? formatDateTime(Number(row.triggerNextTime))
                        : '—'
                    }}
                  </TableCell>
                  <TableCell class="pr-3">
                    <div class="flex justify-end">
                      <TaskActions
                        :task="row"
                        :busy="busy"
                        @edit="edit(row)"
                        @trigger="trigger(row)"
                        @toggle="toggle(row)"
                        @times="nextTimes(row)"
                        @logs="logs(row)"
                        @nodes="nodes(row)"
                        @remove="ask(row, 'remove')"
                      />
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
          <div class="divide-y lg:hidden">
            <article v-for="row in rows" :key="row.id" class="space-y-3 p-4">
              <div class="flex items-start justify-between gap-3">
                <button class="min-w-0 text-left" @click="openDetails(row)">
                  <span class="block font-medium leading-6">
                    {{ row.taskDesc }}
                  </span>
                  <span
                    class="mt-1 block break-all font-mono text-[11px] text-muted-foreground"
                  >
                    {{ row.taskName }}
                  </span>
                </button>
                <TaskActions
                  :task="row"
                  :busy="busy"
                  @edit="edit(row)"
                  @trigger="trigger(row)"
                  @toggle="toggle(row)"
                  @times="nextTimes(row)"
                  @logs="logs(row)"
                  @nodes="nodes(row)"
                  @remove="ask(row, 'remove')"
                />
              </div>
              <div class="flex flex-wrap items-center gap-3">
                <TaskStatus :enabled="row.triggerStatus === 1" />
                <span
                  class="flex items-center gap-1.5 text-xs text-muted-foreground"
                >
                  <Clock3 class="size-3.5" />
                  {{ scheduleLabel(row) }}
                </span>
              </div>
            </article>
          </div>
        </DataState>
      </div>
      <PagePagination
        :page="page"
        :size="size"
        :total="total"
        :loading="stateLoading"
        @change="paginate"
      />
    </div>
  </div>
  <TaskDetails
    v-model="detailOpen"
    :task="detailTask"
    :meta="meta"
    @edit="edit"
  />
  <TaskEditor
    v-if="meta"
    v-model="editorOpen"
    :task="editing"
    :meta="meta"
    :executor-id="Number(filters.executorId)"
    @saved="load()"
  />
  <ConfirmDialog
    v-model="confirmOpen"
    :title="confirmTitle"
    :description="confirmDescription"
    :busy="busy"
    :error="mutationError"
    :destructive="action === 'remove'"
    @confirm="confirm"
  />
  <Dialog
    :open="triggerOpen"
    @update:open="
      (v) => {
        if (!busy) triggerOpen = v;
      }
    "
  >
    <DialogContent>
      <DialogHeader>
        <DialogTitle>执行任务：{{ selected?.taskDesc }}</DialogTitle>
        <DialogDescription>
          立即触发「{{ selected?.taskDesc }}」，不改变自动调度状态。
        </DialogDescription>
      </DialogHeader>
      <form class="space-y-5" @submit.prevent="execute">
        <FormField id="trigger-param" label="执行参数">
          <Textarea
            id="trigger-param"
            v-model="triggerForm.executorParam"
            rows="4"
            class="font-mono text-xs"
          />
        </FormField>
        <FormField
          id="trigger-address"
          label="指定地址"
          hint="留空使用已注册节点；多个地址以英文逗号分隔"
        >
          <Textarea
            rows="3"
            id="trigger-address"
            v-model="triggerForm.addressList"
            placeholder="http://127.0.0.1:9999"
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
            @click="triggerOpen = false"
          >
            取消
          </Button>
          <Button type="submit" :disabled="busy">
            {{ busy ? '提交中…' : '确认执行' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
  <TaskInfoDialog
    v-model="infoOpen"
    :kind="infoKind"
    :task="infoTask"
    :executor="infoExecutor"
    :lines="infoLines"
    @edit="edit"
  />
</template>
