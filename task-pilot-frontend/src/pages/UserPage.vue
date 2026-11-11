<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Plus, RefreshCw, Search, UserRound } from '@lucide/vue';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import PageHeading from '@/components/PageHeading.vue';
import RecordActions from '@/components/RecordActions.vue';
import AppSelect from '@/components/AppSelect.vue';
import AppRadioGroup from '@/components/AppRadioGroup.vue';
import FormField from '@/components/FormField.vue';
import DataState from '@/components/DataState.vue';
import PagePagination from '@/components/PagePagination.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import { usePage } from '@/composables/usePage';
import { useAutoSearch } from '@/composables/useAutoSearch';
import { useMutation } from '@/composables/useMutation';
import { userApi } from '@/services/api';
import { getErrorMessage, parsePermissions } from '@/utils/format';
import type { User, UserMeta } from '@/types/models';
const { rows, total, page, size, loading, error, load } = usePage<User>(
  userApi.page,
);
const meta = ref<UserMeta | null>(null),
  metaError = ref(''),
  metaLoading = ref(true);
const filters = reactive({ username: '', role: '-1' });
const { search: query, onEnter: searchOnEnter } = useAutoSearch(
  () => filters.username,
  () => {
    if (meta.value) return load(filters, true);
  },
);
const { busy, error: mutationError, run } = useMutation();
const open = ref(false),
  editing = ref<User | null>(null),
  deleting = ref<User | null>(null),
  deleteOpen = ref(false);
const form = reactive({
  username: '',
  password: '',
  role: '0',
  permissions: [] as string[],
});
async function init() {
  metaLoading.value = true;
  metaError.value = '';
  try {
    const { data } = await userApi.meta();
    if (!data) throw new Error('无法获取用户配置');
    meta.value = data;
    await query();
  } catch (e) {
    metaError.value = getErrorMessage(e);
  } finally {
    metaLoading.value = false;
  }
}
function edit(row?: User) {
  editing.value = row || null;
  Object.assign(form, {
    username: row?.username || '',
    password: '',
    role: String(row?.role || 0),
    permissions: parsePermissions(row?.permission),
  });
  mutationError.value = '';
  open.value = true;
}
async function save() {
  if (
    await run(
      () =>
        (editing.value ? userApi.update : userApi.create)({
          id: editing.value?.id,
          username: form.username,
          password: form.password,
          role: form.role,
          // 保留角色切换前的权限配置，管理员权限由服务端判定。
          permission: form.permissions.join(','),
        }),
      '用户已保存',
    )
  ) {
    open.value = false;
    await load();
  }
}
function togglePermission(id: string, checked: boolean | 'indeterminate') {
  form.permissions =
    checked === true
      ? [...new Set([...form.permissions, id])]
      : form.permissions.filter((v) => v !== id);
}
function permissionLabel(row: User) {
  if (row.role === 1) return '全部执行器';
  const ids = parsePermissions(row.permission);
  return ids.length ? `${ids.length} 个执行器` : '全部受限';
}
function askDelete(row: User) {
  deleting.value = row;
  mutationError.value = '';
  deleteOpen.value = true;
}
async function remove() {
  if (
    deleting.value &&
    (await run(() => userApi.remove(deleting.value!.id), '用户已删除'))
  ) {
    deleteOpen.value = false;
    await load();
  }
}
function paginate(p: number, s: number) {
  page.value = p;
  size.value = s;
  void load();
}
function reset() {
  Object.assign(filters, { username: '', role: '-1' });
  void query();
}
onMounted(init);
</script>
<template>
  <div class="space-y-6">
    <PageHeading title="用户管理" description="管理团队成员与执行器访问权限。">
      <Button
        variant="outline"
        size="icon"
        aria-label="刷新用户"
        :disabled="loading || metaLoading"
        @click="init"
      >
        <RefreshCw :class="{ 'animate-spin': loading || metaLoading }" />
      </Button>
      <Button :disabled="!meta" @click="edit()">
        <Plus />
        新增用户
      </Button>
    </PageHeading>
    <div
      class="flex flex-wrap items-center gap-2"
      role="search"
      aria-label="用户筛选"
    >
      <div class="relative w-full sm:w-72">
        <Search
          class="pointer-events-none absolute top-2.5 left-3 size-4 text-muted-foreground"
        />
        <Input
          v-model="filters.username"
          @keydown.enter="searchOnEnter"
          aria-label="用户名筛选"
          placeholder="搜索用户名…"
          class="pl-9"
        />
      </div>
      <div class="w-36">
        <AppSelect
          v-model="filters.role"
          label="用户角色"
          :disabled="!meta"
          :options="[
            { label: '全部角色', value: '-1' },
            { label: '管理员', value: '1' },
            { label: '普通用户', value: '0' },
          ]"
          @update:model-value="query"
        />
      </div>
      <Button
        v-if="filters.username || filters.role !== '-1'"
        type="button"
        variant="ghost"
        :disabled="!meta"
        @click="reset"
      >
        重置
      </Button>
      <span
        class="ml-auto hidden text-xs whitespace-nowrap text-muted-foreground sm:block"
      >
        共 {{ total }} 位用户
      </span>
    </div>
    <div>
      <div class="overflow-hidden rounded-xl border bg-card">
        <DataState
          :loading="loading || metaLoading"
          :error="metaError || error"
          :empty="!rows.length"
          @retry="init"
        >
          <template #empty-action>
            <Button variant="outline" size="sm" @click="reset">清除筛选</Button>
          </template>
          <div class="hidden lg:block">
            <Table class="min-w-[600px]">
              <TableHeader>
                <TableRow>
                  <TableHead class="pl-5">用户</TableHead>
                  <TableHead>角色</TableHead>
                  <TableHead>执行器权限</TableHead>
                  <TableHead class="w-16 pr-5 text-right">
                    <span class="sr-only">操作</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="row in rows" :key="row.id">
                  <TableCell class="py-4 pl-5">
                    <div class="flex items-center gap-3">
                      <span
                        class="flex size-9 items-center justify-center rounded-full bg-muted"
                      >
                        <UserRound class="size-4 text-muted-foreground" />
                      </span>
                      <button
                        class="text-left font-medium hover:underline"
                        @click="edit(row)"
                      >
                        {{ row.username }}
                      </button>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge :variant="row.role === 1 ? 'secondary' : 'outline'">
                      {{ row.role === 1 ? '管理员' : '普通用户' }}
                    </Badge>
                  </TableCell>
                  <TableCell class="text-muted-foreground">
                    {{ permissionLabel(row) }}
                  </TableCell>
                  <TableCell class="pr-5 text-right">
                    <RecordActions
                      :label="row.username"
                      :busy="busy"
                      @edit="edit(row)"
                      @remove="askDelete(row)"
                    />
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
          <div class="divide-y lg:hidden">
            <article v-for="row in rows" :key="row.id" class="space-y-3 p-4">
              <div class="flex items-center justify-between">
                <button class="text-sm font-medium" @click="edit(row)">
                  {{ row.username }}
                </button>
                <RecordActions
                  :label="row.username"
                  :busy="busy"
                  @edit="edit(row)"
                  @remove="askDelete(row)"
                />
              </div>
              <div class="flex items-center justify-between">
                <Badge :variant="row.role === 1 ? 'secondary' : 'outline'">
                  {{ row.role === 1 ? '管理员' : '普通用户' }}
                </Badge>
                <span class="text-xs text-muted-foreground">
                  {{ permissionLabel(row) }}
                </span>
              </div>
            </article>
          </div>
        </DataState>
      </div>
      <PagePagination
        :page="page"
        :size="size"
        :total="total"
        :loading="loading || metaLoading"
        @change="paginate"
      />
    </div>
  </div>
  <Dialog
    :open="open"
    @update:open="
      (v) => {
        if (!busy) open = v;
      }
    "
  >
    <DialogContent class="max-h-[90svh] overflow-y-auto sm:max-w-[560px]">
      <DialogHeader>
        <DialogTitle>{{ editing ? '编辑用户' : '新增用户' }}</DialogTitle>
        <DialogDescription>
          管理员可访问所有执行器和管理功能。
        </DialogDescription>
      </DialogHeader>
      <form class="space-y-5" @submit.prevent="save">
        <FormField
          id="user-name"
          label="用户名"
          hint="4–20 位小写字母或数字，以字母开头"
        >
          <Input
            id="user-name"
            v-model="form.username"
            :disabled="!!editing"
            required
            minlength="4"
            maxlength="20"
            pattern="[a-z][a-z0-9]{3,19}"
            autocomplete="off"
          />
        </FormField>
        <FormField
          id="user-password"
          label="密码"
          :hint="
            editing ? '留空保持原密码；修改时输入 4–20 个字符' : '4–20 个字符'
          "
        >
          <Input
            id="user-password"
            v-model="form.password"
            type="password"
            :required="!editing"
            minlength="4"
            maxlength="20"
            autocomplete="new-password"
          />
        </FormField>
        <FormField id="user-role" label="角色">
          <AppRadioGroup
            id="user-role"
            v-model="form.role"
            label="角色"
            :options="[
              { label: '管理员', value: 1 },
              { label: '普通用户', value: 0 },
            ]"
          />
        </FormField>
        <fieldset class="space-y-3">
          <legend class="mb-3 text-sm font-medium">执行器权限</legend>
          <p v-if="form.role === '1'" class="text-xs text-muted-foreground">
            管理员可访问全部执行器。以下配置会保留，并在切换为普通用户时生效。
          </p>
          <div class="max-h-52 space-y-3 overflow-y-auto rounded-lg border p-3">
            <label
              v-for="executor in meta?.executors"
              :key="executor.id"
              class="flex items-start gap-3 text-sm"
            >
              <Checkbox
                :model-value="form.permissions.includes(String(executor.id))"
                @update:model-value="
                  togglePermission(String(executor.id), $event)
                "
              />
              <span>
                {{ executor.title }}
                <span class="mt-1 block text-xs text-muted-foreground">
                  {{ executor.appname }}
                </span>
              </span>
            </label>
            <p
              v-if="!meta?.executors.length"
              class="text-sm text-muted-foreground"
            >
              暂无可分配的执行器
            </p>
          </div>
        </fieldset>
        <p v-if="mutationError" role="alert" class="text-sm text-destructive">
          {{ mutationError }}
        </p>
        <DialogFooter>
          <Button
            variant="outline"
            type="button"
            :disabled="busy"
            @click="open = false"
          >
            取消
          </Button>
          <Button type="submit" :disabled="busy">
            {{ busy ? '保存中…' : '保存用户' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
  <ConfirmDialog
    v-model="deleteOpen"
    title="删除用户？"
    :description="`用户「${deleting?.username || ''}」将无法再登录。`"
    :busy="busy"
    :error="mutationError"
    destructive
    @confirm="remove"
  />
</template>
