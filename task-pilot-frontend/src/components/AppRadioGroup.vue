<script setup lang="ts">
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import type { Option } from '@/types/models';
defineProps<{
  id: string;
  label: string;
  options: readonly Option[];
  segmented?: boolean;
}>();
const value = defineModel<string>({ required: true });
</script>
<template>
  <RadioGroup
    :id="id"
    v-model="value"
    :aria-label="label"
    class="flex flex-wrap gap-2"
    :class="segmented ? 'gap-0 rounded-md border p-0.5' : 'gap-5'"
  >
    <label
      v-for="option in options"
      :key="option.value"
      class="flex cursor-pointer items-center gap-2 text-sm"
      :class="
        segmented
          ? [
              'rounded px-3 py-1.5 has-focus-visible:ring-2 has-focus-visible:ring-ring',
              value === String(option.value)
                ? 'bg-primary text-primary-foreground'
                : 'hover:bg-muted',
            ]
          : ''
      "
    >
      <RadioGroupItem
        :value="String(option.value)"
        :class="segmented ? 'sr-only' : ''"
      />
      {{ option.label }}
    </label>
  </RadioGroup>
</template>
