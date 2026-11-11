<script setup lang="ts">
import {
  Ellipsis,
  Pencil,
  Play,
  Pause,
  Power,
  Clock3,
  Logs,
  Server,
  Trash2,
} from '@lucide/vue';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import type { Task } from '@/types/models';
defineProps<{ task: Task; busy?: boolean }>();
defineEmits<{
  edit: [];
  trigger: [];
  toggle: [];
  times: [];
  logs: [];
  nodes: [];
  remove: [];
}>();
</script>
<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button
        size="icon-sm"
        variant="ghost"
        :aria-label="`任务操作 ${task.taskName}`"
      >
        <Ellipsis />
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" class="w-44">
      <DropdownMenuItem @select="$emit('edit')">
        <Pencil />
        编辑任务
      </DropdownMenuItem>
      <DropdownMenuItem :disabled="busy" @select="$emit('trigger')">
        <Play />
        执行一次
      </DropdownMenuItem>
      <DropdownMenuItem :disabled="busy" @select="$emit('toggle')">
        <Pause v-if="task.triggerStatus === 1" />
        <Power v-else />
        {{ task.triggerStatus === 1 ? '暂停调度' : '启用调度' }}
      </DropdownMenuItem>
      <DropdownMenuSeparator />
      <DropdownMenuItem @select="$emit('logs')">
        <Logs />
        执行日志
      </DropdownMenuItem>
      <DropdownMenuItem :disabled="busy" @select="$emit('times')">
        <Clock3 />
        下次触发时间
      </DropdownMenuItem>
      <DropdownMenuItem :disabled="busy" @select="$emit('nodes')">
        <Server />
        注册节点
      </DropdownMenuItem>
      <DropdownMenuSeparator />
      <DropdownMenuItem class="text-destructive" @select="$emit('remove')">
        <Trash2 />
        删除任务
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
