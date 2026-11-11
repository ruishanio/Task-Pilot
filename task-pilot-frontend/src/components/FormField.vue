<script setup lang="ts">
import { Label } from '@/components/ui/label';
defineProps<{ id: string; label: string; hint?: string; error?: string }>();
</script>
<template>
  <div class="grid content-start gap-2">
    <Label :for="id">{{ label }}</Label>
    <slot />
    <p
      v-if="error"
      :id="`${id}-error`"
      role="alert"
      class="text-xs text-destructive"
    >
      {{ error }}
    </p>
    <p v-else-if="hint" class="text-xs leading-relaxed text-muted-foreground">
      {{ hint }}
    </p>
  </div>
</template>
