<script setup lang="ts">
defineProps<{ title: string; description: string }>();
</script>
<template>
  <div class="flex flex-wrap items-start justify-between gap-4">
    <div class="space-y-1.5">
      <h1 class="text-2xl font-semibold tracking-tight">{{ title }}</h1>
      <p class="text-sm leading-6 text-muted-foreground">{{ description }}</p>
    </div>
    <div v-if="$slots.default" class="flex flex-wrap items-center gap-2">
      <slot />
    </div>
  </div>
</template>
