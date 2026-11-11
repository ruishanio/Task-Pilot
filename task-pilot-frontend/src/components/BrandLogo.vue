<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { cn } from 'cn';

defineProps<{ class?: HTMLAttributes['class'] }>();
// 使用部署基路径，确保 /web/ 下的侧栏与登录页共用同一矢量标志。
const logoUrl = `${import.meta.env.BASE_URL}brand/task-pilot-icon.svg`;
</script>

<template>
  <img
    :src="logoUrl"
    alt=""
    aria-hidden="true"
    width="36"
    height="36"
    :class="cn('size-9 shrink-0', $props.class)"
  />
</template>
