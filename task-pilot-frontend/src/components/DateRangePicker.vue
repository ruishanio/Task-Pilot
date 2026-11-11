<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import dayjs from 'dayjs';
import { CalendarDays, ChevronDown } from '@lucide/vue';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
} from '@/components/ui/popover';
import FormField from './FormField.vue';
const start = defineModel<string>('start', { required: true });
const end = defineModel<string>('end', { required: true });
const emit = defineEmits<{ apply: [] }>();
const open = ref(false),
  error = ref('');
const draft = reactive({ start: '', end: '' });
const label = computed(
  () =>
    `${dayjs(start.value).format('YYYY/MM/DD')} – ${dayjs(end.value).format(dayjs(start.value).year() === dayjs(end.value).year() ? 'MM/DD' : 'YYYY/MM/DD')}`,
);
watch(open, (value) => {
  if (value) {
    Object.assign(draft, { start: start.value, end: end.value });
    error.value = '';
  }
});
function apply() {
  if (
    !draft.start ||
    !draft.end ||
    !dayjs(draft.start).isValid() ||
    !dayjs(draft.end).isValid()
  ) {
    error.value = '请选择有效的开始与结束时间';
    return;
  }
  if (dayjs(draft.start).isAfter(dayjs(draft.end))) {
    error.value = '结束时间不能早于开始时间';
    return;
  }
  // 只有点击应用或快捷范围时才提交，关闭浮层会放弃未应用的日期。
  start.value = draft.start;
  end.value = draft.end;
  open.value = false;
  emit('apply');
}
function preset(days: number) {
  draft.start = dayjs()
    .subtract(days - 1, 'day')
    .startOf('day')
    .format('YYYY-MM-DDTHH:mm:ss');
  draft.end = dayjs().endOf('day').format('YYYY-MM-DDTHH:mm:ss');
  apply();
}
</script>
<template>
  <Popover v-model:open="open">
    <PopoverTrigger as-child>
      <Button
        type="button"
        variant="outline"
        aria-label="选择时间范围"
        class="font-normal"
      >
        <CalendarDays class="text-muted-foreground" />
        <span>{{ label }}</span>
        <ChevronDown class="ml-1 size-3.5 text-muted-foreground" />
      </Button>
    </PopoverTrigger>
    <PopoverContent align="end" class="w-[340px] max-w-[calc(100vw-2rem)] p-4">
      <div class="mb-4 space-y-1">
        <h2 class="font-medium">时间范围</h2>
        <p class="text-xs text-muted-foreground">
          选择快捷范围，或精确指定日期与时间。
        </p>
      </div>
      <div class="mb-4 flex gap-2">
        <Button size="sm" variant="outline" @click="preset(1)">今天</Button>
        <Button size="sm" variant="outline" @click="preset(7)">
          最近 7 天
        </Button>
        <Button size="sm" variant="outline" @click="preset(30)">
          最近 30 天
        </Button>
      </div>
      <form class="space-y-4" novalidate @submit.prevent="apply">
        <FormField id="range-start" label="开始时间">
          <Input
            id="range-start"
            v-model="draft.start"
            type="datetime-local"
            step="1"
            required
            :max="draft.end"
          />
        </FormField>
        <FormField id="range-end" label="结束时间">
          <Input
            id="range-end"
            v-model="draft.end"
            type="datetime-local"
            step="1"
            required
            :min="draft.start"
          />
        </FormField>
        <p v-if="error" role="alert" class="text-xs text-destructive">
          {{ error }}
        </p>
        <div class="flex justify-end gap-2 border-t pt-3">
          <Button type="button" variant="ghost" @click="open = false">
            取消
          </Button>
          <Button type="submit">应用范围</Button>
        </div>
      </form>
    </PopoverContent>
  </Popover>
</template>
