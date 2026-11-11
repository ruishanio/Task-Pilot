<script setup lang="ts">
import type { SelectContentEmits, SelectContentProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { reactiveOmit } from '@vueuse/core';
import { cn } from 'cn';
import {
  SelectContent,
  SelectPortal,
  SelectViewport,
  useForwardPropsEmits,
} from 'reka-ui';

defineOptions({
  inheritAttrs: false,
});

const props = withDefaults(
  defineProps<SelectContentProps & { class?: HTMLAttributes['class'] }>(),
  {
    // 统一锚定输入框展开，避免按选中项定位时覆盖输入框。
    position: 'popper',
    align: 'start',
    sideOffset: 6,
    collisionPadding: 8,
  },
);
const emits = defineEmits<SelectContentEmits>();

const delegatedProps = reactiveOmit(props, 'class');

const forwarded = useForwardPropsEmits(delegatedProps, emits);
</script>

<template>
  <SelectPortal>
    <SelectContent
      data-slot="select-content"
      :data-align-trigger="position === 'item-aligned'"
      v-bind="{ ...$attrs, ...forwarded }"
      :class="
        cn(
          'bg-popover text-popover-foreground data-open:animate-in data-closed:animate-out data-closed:fade-out-0 data-open:fade-in-0 data-closed:zoom-out-95 data-open:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 ring-foreground/10 min-w-36 rounded-lg shadow-md ring-1 duration-100 data-[side=inline-start]:slide-in-from-right-2 data-[side=inline-end]:slide-in-from-left-2 cn-menu-translucent relative z-50 max-h-[min(20rem,var(--reka-select-content-available-height,20rem))] origin-(--reka-select-content-transform-origin) overflow-hidden data-[align-trigger=true]:animate-none',
          position === 'popper' &&
            'w-(--reka-select-trigger-width) min-w-(--reka-select-trigger-width) max-w-[calc(100vw-1rem)]',
          props.class,
        )
      "
    >
      <!-- 固定滚动区域，避免滚动箭头显隐改变高度后裁切键盘聚焦的选项。 -->
      <SelectViewport
        class="p-1"
        style="scrollbar-width: thin; scrollbar-color: var(--border) transparent"
      >
        <slot />
      </SelectViewport>
    </SelectContent>
  </SelectPortal>
</template>
