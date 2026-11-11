<script setup lang="ts">
import { CircleCheck, CirclePause } from '@lucide/vue';
import { Badge } from '@/components/ui/badge';
defineProps<{ enabled: boolean }>();
</script>
<template>
  <Badge
    variant="outline"
    class="gap-1.5 border-transparent font-normal"
    :class="
      enabled
        ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
        : 'bg-muted text-muted-foreground'
    "
  >
    <CircleCheck v-if="enabled" class="size-3" />
    <CirclePause v-else class="size-3" />
    {{ enabled ? '已启用' : '已暂停' }}
  </Badge>
</template>
