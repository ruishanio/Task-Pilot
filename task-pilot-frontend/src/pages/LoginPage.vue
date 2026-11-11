<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LoaderCircle } from '@lucide/vue';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from '@/components/ui/card';
import FormField from '@/components/FormField.vue';
import BrandLogo from '@/components/BrandLogo.vue';
import { authApi } from '@/services/api';
import { storeAccessToken } from '@/services/authToken';
import { auth, initializeAuth } from '@/stores/auth';
import { safeRedirect } from '@/utils/navigation';
import { useMutation } from '@/composables/useMutation';
const route = useRoute(),
  router = useRouter();
const form = reactive({ userName: '', password: '' });
const remember = ref(true);
const { busy, error, run } = useMutation();
async function submit() {
  const ok = await run(async () => {
    const { data } = await authApi.login({
      ...form,
      ifRemember: remember.value ? 'on' : '',
    });
    if (!data?.accessToken) throw new Error('登录失败，服务器未返回登录凭证');
    storeAccessToken(data.accessToken, remember.value);
    await initializeAuth(true);
    if (!auth.user) throw new Error('无法获取用户信息，请重新登录');
  });
  if (ok) await router.replace(safeRedirect(route.query.redirect));
}
</script>
<template>
  <div class="flex min-h-svh flex-col bg-muted/30 p-6">
    <main class="flex flex-1 items-center justify-center py-12">
      <div class="w-full max-w-[368px]">
        <Card>
          <CardHeader class="space-y-1 text-center">
            <BrandLogo class="mx-auto mb-2 size-12" />
            <CardTitle class="text-xl">TASK PILOT</CardTitle>
            <CardDescription>任务调度平台</CardDescription>
          </CardHeader>
          <CardContent>
            <form class="space-y-5" @submit.prevent="submit">
              <p
                v-if="route.query.reason === 'bootstrap'"
                role="alert"
                class="text-sm text-destructive"
              >
                连接服务失败，请重试登录。
              </p>
              <FormField id="username" label="用户名">
                <Input
                  id="username"
                  v-model="form.userName"
                  autocomplete="username"
                  placeholder="输入用户名"
                  required
                  minlength="4"
                  maxlength="20"
                  autofocus
                />
              </FormField>
              <FormField id="password" label="密码">
                <Input
                  id="password"
                  v-model="form.password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="输入密码"
                  required
                  minlength="4"
                  maxlength="20"
                />
              </FormField>
              <label class="flex items-center gap-2 text-sm">
                <Checkbox v-model="remember" />
                自动登录
              </label>
              <p v-if="error" role="alert" class="text-sm text-destructive">
                {{ error }}
              </p>
              <Button type="submit" class="w-full" :disabled="busy">
                <LoaderCircle v-if="busy" class="animate-spin" />
                {{ busy ? '正在登录…' : '登录' }}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  </div>
</template>
