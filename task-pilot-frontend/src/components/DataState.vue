<script setup lang="ts">
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { SearchX, CircleAlert } from '@lucide/vue';
defineProps<{
  loading: boolean;
  error?: string;
  empty: boolean;
  emptyText?: string;
}>();
defineEmits<{ retry: [] }>();
</script>
<template>
  <div v-if="loading" class="space-y-5 p-6" role="status" aria-label="正在加载">
    <Skeleton v-for="i in 5" :key="i" class="h-8 w-full" />
  </div>
  <div
    v-else-if="error"
    class="flex flex-col items-center gap-3 px-6 py-14 text-center"
    role="alert"
  >
    <CircleAlert class="size-6 text-destructive" />
    <p class="text-sm">{{ error }}</p>
    <Button variant="outline" size="sm" @click="$emit('retry')">
      重新加载
    </Button>
  </div>
  <div
    v-else-if="empty"
    class="flex flex-col items-center gap-3 px-6 py-16 text-center"
  >
    <SearchX class="size-7 text-muted-foreground" />
    <p class="text-sm text-muted-foreground">
      {{ emptyText || '暂无数据，试试调整筛选条件' }}
    </p>
    <slot name="empty-action" />
  </div>
  <slot v-else />
</template>
