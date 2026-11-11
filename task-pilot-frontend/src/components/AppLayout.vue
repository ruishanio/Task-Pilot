<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ChevronRight } from '@lucide/vue';
import AppSidebar from './AppSidebar.vue';
import {
  SidebarProvider,
  SidebarInset,
  SidebarTrigger,
} from '@/components/ui/sidebar';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import FormField from './FormField.vue';
import { auth, logout } from '@/stores/auth';
import { authApi } from '@/services/api';
import { useMutation } from '@/composables/useMutation';
const route = useRoute(),
  router = useRouter();
const currentTitle = computed(() => {
  const path = route.path.startsWith('/task_code') ? '/task_info' : route.path;
  const menu = auth.menus.find((item) => path.startsWith(item.url));
  return menu?.url === '/dashboard' ? '工作台' : menu?.name || route.meta.title;
});
const passwordOpen = ref(false),
  validation = ref('');
const password = reactive({ oldPassword: '', password: '', confirm: '' });
const { busy, error, run } = useMutation();
function editPassword() {
  Object.assign(password, { oldPassword: '', password: '', confirm: '' });
  validation.value = '';
  error.value = '';
  passwordOpen.value = true;
}
async function savePassword() {
  validation.value =
    password.password !== password.confirm ? '两次输入的新密码不一致' : '';
  if (validation.value) return;
  if (
    await run(
      () =>
        authApi.updatePassword({
          oldPassword: password.oldPassword,
          password: password.password,
        }),
      '密码已更新',
    )
  ) {
    passwordOpen.value = false;
    Object.assign(password, { oldPassword: '', password: '', confirm: '' });
  }
}
async function signOut() {
  logout();
  await router.replace('/login');
}
</script>
<template>
  <SidebarProvider :style="{ '--sidebar-width': '240px' }" class="bg-sidebar">
    <a
      href="#main-content"
      class="sr-only z-50 rounded bg-background p-3 focus:not-sr-only focus:fixed"
    >
      跳到主要内容
    </a>
    <AppSidebar @password="editPassword" @logout="signOut" />
    <SidebarInset class="min-w-0">
      <header
        class="flex h-14 shrink-0 items-center gap-3 border-b px-4 md:px-6"
      >
        <SidebarTrigger aria-label="切换导航" />
        <Separator orientation="vertical" class="h-4!" />
        <nav aria-label="当前位置" class="flex items-center gap-2 text-sm">
          <span class="hidden text-muted-foreground sm:inline">控制台</span>
          <ChevronRight
            class="hidden size-3.5 text-muted-foreground sm:block"
          />
          <span class="font-medium">{{ currentTitle }}</span>
        </nav>
        <Badge
          v-if="auth.version"
          variant="outline"
          class="ml-auto font-mono font-normal text-muted-foreground"
          :title="`当前版本：${auth.version}`"
          :aria-label="`当前版本：${auth.version}`"
        >
          v{{ auth.version }}
        </Badge>
      </header>
      <div id="main-content" class="min-w-0 flex-1 px-4 py-6 md:px-6 lg:px-8">
        <div class="mx-auto w-full max-w-[1440px]"><RouterView /></div>
      </div>
    </SidebarInset>
  </SidebarProvider>
  <Dialog
    :open="passwordOpen"
    @update:open="
      (v) => {
        if (!busy) passwordOpen = v;
      }
    "
  >
    <DialogContent>
      <DialogHeader>
        <DialogTitle>修改密码</DialogTitle>
        <DialogDescription>请输入旧密码和新密码。</DialogDescription>
      </DialogHeader>
      <form class="space-y-5" @submit.prevent="savePassword">
        <FormField id="old-password" label="旧密码">
          <Input
            id="old-password"
            v-model="password.oldPassword"
            type="password"
            autocomplete="current-password"
            required
          />
        </FormField>
        <FormField id="new-password" label="新密码" hint="4–20 个字符">
          <Input
            id="new-password"
            v-model="password.password"
            type="password"
            autocomplete="new-password"
            required
            minlength="4"
            maxlength="20"
          />
        </FormField>
        <FormField id="confirm-password" label="确认新密码">
          <Input
            id="confirm-password"
            v-model="password.confirm"
            type="password"
            autocomplete="new-password"
            required
            minlength="4"
            maxlength="20"
          />
        </FormField>
        <p
          v-if="validation || error"
          role="alert"
          class="text-sm text-destructive"
        >
          {{ validation || error }}
        </p>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            :disabled="busy"
            @click="passwordOpen = false"
          >
            取消
          </Button>
          <Button type="submit" :disabled="busy">
            {{ busy ? '保存中…' : '保存密码' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>
