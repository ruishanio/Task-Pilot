<script setup lang="ts">
import { Copy } from '@lucide/vue';
import { toast } from 'vue-sonner';
import { Button } from '@/components/ui/button';
const props = defineProps<{ text: string }>();
async function copy() {
  try {
    await navigator.clipboard.writeText(props.text);
    toast.success('已复制');
  } catch {
    toast.error('复制失败，请手动选择并复制');
  }
}
</script>
<template>
  <div class="flex items-start gap-2 rounded-md bg-muted p-3">
    <span
      class="min-w-0 flex-1 select-text break-all font-mono text-xs leading-7"
    >
      {{ text }}
    </span>
    <Button
      size="icon-sm"
      variant="ghost"
      :aria-label="`复制 ${text}`"
      @click="copy"
    >
      <Copy />
    </Button>
  </div>
</template>
