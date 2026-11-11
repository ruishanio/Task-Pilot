<script setup lang="ts">
import { computed } from 'vue';
import { Pencil, Logs } from '@lucide/vue';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import TaskStatus from './TaskStatus.vue';
import { formatDateTime } from '@/utils/format';
import type { Option, Task, TaskMeta } from '@/types/models';
const props = defineProps<{ task: Task | null; meta: TaskMeta | null }>();
const open = defineModel<boolean>({ default: false });
defineEmits<{ edit: [task: Task] }>();
function optionLabel(options: Option[] | undefined, value: string) {
  return options?.find((item) => item.value === value)?.label || value;
}

const scheduleName = computed(() => {
  const type = props.task?.scheduleType;
  if (type === 'NONE') return '手动触发';
  if (type === 'CRON') return 'Cron 定时调度';
  if (type === 'FIX_RATE') return '固定频率';
  return optionLabel(props.meta?.scheduleTypeOptions, type || '') || '未配置';
});

const scheduleRule = computed(() => {
  const t = props.task;
  if (!t || t.scheduleType === 'NONE') return '';
  if (!t.scheduleConf) return '未配置调度规则';
  return t.scheduleType === 'FIX_RATE'
    ? `每 ${t.scheduleConf} 秒`
    : t.scheduleConf;
});

const nextTrigger = computed(() => {
  const t = props.task;
  if (!t) return '暂无触发时间';
  // 手动任务不展示自动时间，暂停任务不展示已失效的下次触发时间。
  if (t.scheduleType === 'NONE') return '不自动触发';
  if (t.triggerStatus !== 1) return '调度已暂停';
  return Number(t.triggerNextTime) > 0
    ? formatDateTime(Number(t.triggerNextTime), '暂无触发时间')
    : '暂无触发时间';
});

const executionFields = computed(() => {
  const t = props.task;
  if (!t) return [];
  return [
    {
      label: '执行器',
      value:
        props.meta?.executors.find((e) => e.id === t.executorId)?.title ||
        String(t.executorId),
    },
    { label: '负责人', value: t.author || '未设置' },
    {
      label: '执行器处理器',
      value: t.executorHandler || '未配置',
      mono: true,
    },
    {
      label: 'GLUE 类型',
      value: optionLabel(props.meta?.glueTypeOptions, t.glueType),
    },
  ];
});

const strategyFields = computed(() => {
  const t = props.task;
  if (!t) return [];
  return [
    {
      label: '路由策略',
      value: optionLabel(props.meta?.executorRouteStrategyOptions, t.executorRouteStrategy),
    },
    {
      label: '阻塞策略',
      value: optionLabel(props.meta?.executorBlockStrategyOptions, t.executorBlockStrategy),
    },
    {
      label: '过期策略',
      value: optionLabel(props.meta?.misfireStrategyOptions, t.misfireStrategy),
    },
    {
      label: '执行超时',
      value: t.executorTimeout ? `${t.executorTimeout} 秒` : '不限制',
    },
    { label: '失败重试', value: `${t.executorFailRetryCount} 次` },
    { label: '子任务 ID', value: t.childTaskId || '未配置' },
  ];
});
</script>
<template>
  <Dialog v-model:open="open">
    <DialogContent
      class="flex max-h-[85svh] flex-col gap-0 overflow-hidden p-0 sm:max-w-[680px]"
    >
      <DialogHeader class="shrink-0 border-b px-6 py-5">
        <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
          <DialogTitle class="min-w-0 break-words">
            {{ task?.taskDesc || task?.taskName || '任务详情' }}
          </DialogTitle>
          <TaskStatus v-if="task" :enabled="task.triggerStatus === 1" />
        </div>
        <DialogDescription
          class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs"
        >
          <span v-if="task" class="shrink-0 tabular-nums">
            ID #{{ task.id }}
          </span>
          <span class="min-w-0 break-all font-mono">{{ task?.taskName }}</span>
        </DialogDescription>
      </DialogHeader>
      <!-- 长详情仅在内容区滚动，标题与底部操作保持可见。 -->
      <div v-if="task" class="min-h-0 flex-1 space-y-5 overflow-y-auto p-6">
        <section
          aria-label="调度安排"
          class="rounded-lg border bg-muted/30 p-4"
        >
          <dl class="grid gap-4 sm:grid-cols-2 sm:gap-6">
            <div class="min-w-0 space-y-1.5">
              <dt class="text-xs text-muted-foreground">调度方式</dt>
              <dd class="space-y-1.5">
                <p class="text-sm font-medium">{{ scheduleName }}</p>
                <p
                  v-if="scheduleRule"
                  class="break-all font-mono text-xs leading-5"
                >
                  {{ scheduleRule }}
                </p>
                <p v-else class="text-xs leading-5 text-muted-foreground">
                  由手动操作或其他任务调用触发。
                </p>
              </dd>
            </div>
            <div
              class="min-w-0 space-y-1.5 border-t pt-3 sm:border-t-0 sm:border-l sm:pt-0 sm:pl-6"
            >
              <dt class="text-xs text-muted-foreground">下次触发</dt>
              <dd class="break-words text-sm font-medium tabular-nums">
                {{ nextTrigger }}
              </dd>
            </div>
          </dl>
        </section>

        <section aria-label="执行配置" class="space-y-3">
          <h3 class="text-sm font-medium">执行配置</h3>
          <dl class="grid gap-x-6 gap-y-3 sm:grid-cols-2">
            <div
              v-for="field in executionFields"
              :key="field.label"
              class="min-w-0 space-y-1"
            >
              <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
              <dd
                class="break-words leading-5 [overflow-wrap:anywhere]"
                :class="field.mono ? 'font-mono text-xs' : 'text-sm'"
              >
                {{ field.value }}
              </dd>
            </div>
          </dl>
        </section>

        <section aria-label="运行策略" class="space-y-3 border-t pt-4">
          <h3 class="text-sm font-medium">运行策略</h3>
          <dl class="grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-3">
            <div
              v-for="field in strategyFields"
              :key="field.label"
              class="min-w-0 space-y-1"
            >
              <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
              <dd class="break-words text-sm leading-5 [overflow-wrap:anywhere]">
                {{ field.value || '未配置' }}
              </dd>
            </div>
          </dl>
        </section>

        <section aria-label="告警与参数" class="space-y-3 border-t pt-4">
          <h3 class="text-sm font-medium">告警与参数</h3>
          <dl class="grid gap-4 sm:grid-cols-2">
            <div class="space-y-1">
              <dt class="text-xs text-muted-foreground">报警邮箱</dt>
              <dd class="break-words text-sm leading-5 [overflow-wrap:anywhere]">
                {{ task.alarmEmail || '未设置' }}
              </dd>
            </div>
            <div
              class="space-y-1.5"
              :class="task.executorParam && 'sm:col-span-2'"
            >
              <dt class="text-xs text-muted-foreground">任务参数</dt>
              <dd>
                <pre
                  v-if="task.executorParam"
                  class="max-h-48 overflow-y-auto rounded-lg bg-muted/40 p-3 font-mono text-xs leading-5 whitespace-pre-wrap [overflow-wrap:anywhere]"
                  >{{ task.executorParam }}</pre
                >
                <span v-else class="text-sm text-muted-foreground">未配置</span>
              </dd>
            </div>
          </dl>
        </section>

        <section
          v-if="task.glueType !== 'BEAN'"
          aria-label="GLUE 源码"
          class="border-t pt-5"
        >
          <h3 class="mb-3 text-sm font-medium">GLUE 源码</h3>
          <p class="mb-2 text-xs text-muted-foreground">
            {{ task.glueRemark }}
          </p>
          <pre
            class="max-h-72 overflow-auto rounded-lg border bg-muted/40 p-3 font-mono text-xs leading-5 whitespace-pre-wrap [overflow-wrap:anywhere]"
            >{{ task.glueSource || '暂无源码' }}</pre
          >
        </section>
      </div>
      <DialogFooter v-if="task" class="m-0 shrink-0 rounded-none px-6 py-4">
        <Button variant="outline" as-child>
          <RouterLink
            :to="{
              path: '/task_log',
              query: { executorId: task.executorId, taskId: task.id },
            }"
            @click="open = false"
          >
            <Logs />
            执行日志
          </RouterLink>
        </Button>
        <Button
          @click="
            open = false;
            $emit('edit', task);
          "
        >
          <Pencil />
          编辑任务
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
