<script setup lang="ts">
import { Button } from '@/components/ui/button';
</script>
<template>
  <div class="py-24 text-center">
    <p class="mb-3 text-sm text-muted-foreground">404</p>
    <h1 class="mb-6 text-xl font-semibold">这个页面不存在</h1>
    <Button as-child>
      <RouterLink to="/dashboard">返回工作台</RouterLink>
    </Button>
  </div>
</template>
