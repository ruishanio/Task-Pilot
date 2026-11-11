<script setup lang="ts">
import { useRoute } from 'vue-router';
import { LoaderCircle } from '@lucide/vue';
import { Toaster } from '@/components/ui/sonner';
import 'vue-sonner/style.css';
const route = useRoute();
</script>
<template>
  <RouterView v-if="route.matched.length" />
  <div
    v-else
    role="status"
    class="flex min-h-svh items-center justify-center gap-3 text-sm text-muted-foreground"
  >
    <LoaderCircle class="size-4 animate-spin" />
    正在加载 Task Pilot…
  </div>
  <Toaster position="top-right" theme="light" :close-button="true" />
</template>
