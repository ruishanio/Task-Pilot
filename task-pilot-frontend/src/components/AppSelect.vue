<script setup lang="ts">
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { Option } from '@/types/models';
const model = defineModel<string>({ required: true });
defineProps<{
  options: Option[];
  label: string;
  disabled?: boolean;
  id?: string;
}>();
</script>
<template>
  <Select v-model="model" :disabled="disabled">
    <SelectTrigger :id="id" :aria-label="label" class="w-full">
      <SelectValue :placeholder="label" />
    </SelectTrigger>
    <SelectContent>
      <SelectItem
        v-for="option in options"
        :key="String(option.value)"
        :value="String(option.value)"
      >
        {{ option.label }}
      </SelectItem>
    </SelectContent>
  </Select>
</template>
