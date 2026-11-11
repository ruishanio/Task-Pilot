<script setup lang="ts">
import { Input } from '@/components/ui/input';
const start = defineModel<string>('start', { required: true });
const end = defineModel<string>('end', { required: true });
const emit = defineEmits<{ change: [] }>();
</script>
<template>
  <div class="flex min-w-0 flex-wrap items-center gap-2">
    <Input
      v-model="start"
      type="datetime-local"
      step="1"
      required
      :max="end"
      aria-label="开始时间"
      class="w-full sm:w-[220px]"
      @change="emit('change')"
    />
    <span class="text-xs text-muted-foreground">至</span>
    <Input
      v-model="end"
      type="datetime-local"
      step="1"
      required
      :min="start"
      aria-label="结束时间"
      class="w-full sm:w-[220px]"
      @change="emit('change')"
    />
  </div>
</template>
