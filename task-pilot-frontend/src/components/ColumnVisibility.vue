<script setup lang="ts">
import { Columns3, RotateCcw } from '@lucide/vue';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuCheckboxItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuItem,
} from '@/components/ui/dropdown-menu';
const props = defineProps<{
  columns: { key: string; label: string; default?: boolean }[];
}>();
const value = defineModel<string[]>({ required: true });
function toggle(key: string, checked: boolean | 'indeterminate') {
  value.value =
    checked === true
      ? [...new Set([...value.value, key])]
      : value.value.filter((item) => item !== key);
}
</script>
<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button variant="outline" size="sm">
        <Columns3 />
        显示列
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" class="w-44">
      <DropdownMenuLabel>自定义列表</DropdownMenuLabel>
      <DropdownMenuSeparator />
      <DropdownMenuCheckboxItem
        v-for="column in columns"
        :key="column.key"
        :model-value="value.includes(column.key)"
        @update:model-value="toggle(column.key, $event)"
        @select.prevent
      >
        {{ column.label }}
      </DropdownMenuCheckboxItem>
      <DropdownMenuSeparator />
      <DropdownMenuItem
        @select="
          value = props.columns.filter((c) => c.default).map((c) => c.key)
        "
      >
        <RotateCcw />
        恢复默认
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
