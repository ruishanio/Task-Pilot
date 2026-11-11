<script setup lang="ts">
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
const open = defineModel<boolean>({ default: false });
defineProps<{
  title: string;
  description: string;
  busy?: boolean;
  destructive?: boolean;
  error?: string;
}>();
defineEmits<{ confirm: [] }>();
</script>
<template>
  <AlertDialog
    :open="open"
    @update:open="
      (value) => {
        if (!busy) open = value;
      }
    "
  >
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>{{ title }}</AlertDialogTitle>
        <AlertDialogDescription>{{ description }}</AlertDialogDescription>
      </AlertDialogHeader>
      <p v-if="error" role="alert" class="text-sm text-destructive">
        {{ error }}
      </p>
      <AlertDialogFooter>
        <Button variant="outline" :disabled="busy" @click="open = false">
          取消
        </Button>
        <Button
          :variant="destructive ? 'destructive' : 'default'"
          :disabled="busy"
          @click="$emit('confirm')"
        >
          {{ busy ? '处理中…' : '确认' }}
        </Button>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
