<script setup lang="ts">
import { computed } from 'vue';
import { Clock3, Server } from '@lucide/vue';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import CopyText from './CopyText.vue';
import { auth } from '@/stores/auth';
import type { Executor, Task } from '@/types/models';
const props = defineProps<{
  kind: 'times' | 'nodes';
  task: Task | null;
  executor: Executor | null;
  lines: string[];
}>();
const open = defineModel<boolean>({ default: false });
const emit = defineEmits<{ edit: [task: Task] }>();
const manual = computed(() => props.task?.scheduleType === 'NONE');
const schedule = computed(() =>
  props.task?.scheduleType === 'FIX_RATE'
    ? `每 ${props.task.scheduleConf} 秒`
    : props.task?.scheduleConf || '未配置',
);
const emptyState = computed(() => {
  if (props.kind === 'nodes')
    return {
      title: '暂无注册节点',
      description:
        props.executor?.addressType === 1
          ? '尚未配置节点地址，请在执行器设置中添加。'
          : '执行器上线并完成注册后，节点地址会显示在这里。',
    };
  // 手动任务没有自动触发时间属于正常配置，不应作为计算失败提示。
  if (manual.value)
    return {
      title: '此任务不自动调度',
      description: '仅在手动触发或被其他任务调用时执行，无需计算下次触发时间。',
    };
  return {
    title: '暂无后续触发时间',
    description: '当前调度规则没有返回未来的触发时间，可检查任务中的调度配置。',
  };
});
function editTask() {
  if (!props.task) return;
  open.value = false;
  emit('edit', props.task);
}
</script>
<template>
  <Dialog v-model:open="open">
    <DialogContent
      class="max-h-[85svh] gap-5 overflow-y-auto p-6 sm:max-w-[480px]"
    >
      <DialogHeader>
        <DialogTitle>
          {{ kind === 'times' ? '下次触发时间' : '注册节点' }}
        </DialogTitle>
        <DialogDescription class="break-words">
          {{ task?.taskDesc }}
        </DialogDescription>
      </DialogHeader>

      <div
        v-if="kind === 'nodes' && executor"
        class="flex items-start justify-between gap-3"
      >
        <div class="min-w-0 space-y-1">
          <p class="text-xs text-muted-foreground">执行器</p>
          <p class="break-words text-sm font-medium">{{ executor.title }}</p>
          <p class="break-all font-mono text-xs text-muted-foreground">
            {{ executor.appname }}
          </p>
        </div>
        <Badge variant="outline" class="shrink-0">
          {{ executor.addressType === 0 ? '自动注册' : '手动录入' }}
        </Badge>
      </div>
      <div
        v-else-if="kind === 'times' && !manual"
        class="flex flex-wrap items-center gap-2 rounded-lg bg-muted/50 px-3 py-2.5"
      >
        <span class="text-xs text-muted-foreground">
          {{ task?.scheduleType === 'FIX_RATE' ? '固定频率' : 'Cron' }}
        </span>
        <code class="min-w-0 break-all text-xs">{{ schedule }}</code>
      </div>

      <template v-if="lines.length">
        <div v-if="kind === 'nodes'" class="space-y-3">
          <p class="text-xs text-muted-foreground">
            已注册 {{ lines.length }} 个节点
          </p>
          <div class="max-h-64 space-y-2 overflow-y-auto">
            <CopyText v-for="line in lines" :key="line" :text="line" />
          </div>
        </div>
        <div v-else class="space-y-3">
          <p class="text-xs leading-5 text-muted-foreground">
            {{
              task?.triggerStatus === 1
                ? '按当前规则预估，实际触发以调度记录为准。'
                : '任务已暂停，以下时间仅为规则预估，启用后才会自动调度。'
            }}
          </p>
          <ol class="max-h-64 divide-y overflow-y-auto rounded-lg border">
            <li
              v-for="(line, index) in lines"
              :key="line"
              class="flex items-center gap-4 px-4 py-3"
            >
              <span
                class="w-4 shrink-0 text-xs tabular-nums text-muted-foreground"
              >
                {{ String(index + 1).padStart(2, '0') }}
              </span>
              <time
                :datetime="line.replace(' ', 'T')"
                class="font-mono text-xs tabular-nums sm:text-sm"
              >
                {{ line }}
              </time>
            </li>
          </ol>
        </div>
      </template>
      <div
        v-else
        class="flex items-start gap-3 rounded-lg border border-dashed bg-muted/20 p-4"
      >
        <span
          class="flex size-8 shrink-0 items-center justify-center rounded-md border bg-background text-muted-foreground"
        >
          <Clock3 v-if="kind === 'times'" class="size-4" />
          <Server v-else class="size-4" />
        </span>
        <div class="min-w-0 space-y-1">
          <p class="text-sm font-medium">{{ emptyState.title }}</p>
          <p class="text-xs leading-5 text-muted-foreground">
            {{ emptyState.description }}
          </p>
        </div>
      </div>

      <DialogFooter class="-mx-6 -mb-6 px-6 py-4">
        <Button variant="outline" @click="open = false">关闭窗口</Button>
        <Button v-if="kind === 'times' && task" @click="editTask">
          编辑任务
        </Button>
        <Button v-else-if="auth.user?.isAdmin" as-child>
          <RouterLink to="/executor" @click="open = false">
            管理执行器
          </RouterLink>
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
