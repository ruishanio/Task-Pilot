<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { inject, ref } from 'vue';
import { XIcon } from '@lucide/vue';
import { cn } from 'cn';
import { DialogClose } from 'reka-ui';
import { Button } from '@/components/ui/button';
import { dialogCloseButtonKey } from './context';

const props = defineProps<{
  class?: HTMLAttributes['class'];
}>();

const showCloseButton = inject(dialogCloseButtonKey, ref(false));
</script>

<template>
  <div
    data-slot="dialog-header"
    :class="cn('flex items-start gap-4', props.class)"
  >
    <div class="flex min-w-0 flex-1 flex-col gap-2">
      <slot />
    </div>
    <DialogClose v-if="showCloseButton" data-slot="dialog-close" as-child>
      <!-- 32px 点击区域与标题首行的 24px 行高居中对齐，长标题在独立列换行。 -->
      <Button
        type="button"
        variant="ghost"
        size="icon"
        class="-mt-1 text-muted-foreground"
      >
        <XIcon class="size-3.5" :stroke-width="1.75" />
        <span class="sr-only">关闭</span>
      </Button>
    </DialogClose>
  </div>
</template>
