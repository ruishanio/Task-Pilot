<script setup lang="ts">
import { computed } from 'vue';
import { ChevronLeft, ChevronRight } from '@lucide/vue';
import { Button } from '@/components/ui/button';
import AppSelect from './AppSelect.vue';
const props = defineProps<{
  page: number;
  size: number;
  total: number;
  loading?: boolean;
}>();
const emit = defineEmits<{ change: [page: number, size: number] }>();
const sizeValue = computed({
  get: () => String(props.size),
  set: (value) => emit('change', 1, Number(value)),
});
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)));
</script>
<template>
  <div
    class="flex flex-wrap items-center justify-between gap-3 py-4 text-xs text-muted-foreground"
  >
    <span aria-live="polite">
      {{ total ? (page - 1) * size + 1 : 0 }}–{{
        Math.min(page * size, total)
      }}
      条，共 {{ total }} 条
    </span>
    <div class="flex items-center gap-3">
      <div class="w-28">
        <AppSelect
          v-model="sizeValue"
          label="每页条数"
          :disabled="loading"
          :options="
            [10, 20, 50].map((n) => ({ label: `${n} 条 / 页`, value: n }))
          "
        />
      </div>
      <span class="min-w-14 text-center tabular-nums">
        {{ page }} / {{ pages }}
      </span>
      <Button
        variant="outline"
        size="icon-sm"
        aria-label="上一页"
        :disabled="loading || page <= 1"
        @click="emit('change', page - 1, size)"
      >
        <ChevronLeft />
      </Button>
      <Button
        variant="outline"
        size="icon-sm"
        aria-label="下一页"
        :disabled="loading || page >= pages"
        @click="emit('change', page + 1, size)"
      >
        <ChevronRight />
      </Button>
    </div>
  </div>
</template>
