<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Plus, RefreshCw, Search } from '@lucide/vue';
import PageHeading from '@/components/PageHeading.vue';
import RecordActions from '@/components/RecordActions.vue';
import CopyText from '@/components/CopyText.vue';
import AppRadioGroup from '@/components/AppRadioGroup.vue';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
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
import FormField from '@/components/FormField.vue';
import DataState from '@/components/DataState.vue';
import PagePagination from '@/components/PagePagination.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import { usePage } from '@/composables/usePage';
import { useAutoSearch } from '@/composables/useAutoSearch';
import { useMutation } from '@/composables/useMutation';
import { executorApi } from '@/services/api';
import { formatDateTime } from '@/utils/format';
import type { Executor } from '@/types/models';
const { rows, total, page, size, loading, error, load } = usePage<Executor>(
  executorApi.page,
);
const filters = reactive({ appname: '', title: '' });
const { search: query, onEnter: searchOnEnter } = useAutoSearch(
  () => [filters.appname, filters.title],
  () => load(filters, true),
);
const { busy, error: mutationError, run } = useMutation();
const open = ref(false),
  editing = ref<Executor | null>(null),
  deleting = ref<Executor | null>(null),
  deleteOpen = ref(false),
  nodes = ref<Executor | null>(null);
const executorForm = reactive({
  appname: '',
  title: '',
  addressType: '0',
  addressList: '',
});
const addressHint = computed(() =>
  executorForm.addressType === '0'
    ? '自动注册模式下只读'
    : '多个地址使用英文逗号分隔',
);
const nodeOpen = computed({
  get: () => !!nodes.value,
  set: (v) => {
    if (!v) nodes.value = null;
  },
});
function edit(row?: Executor) {
  editing.value = row || null;
  Object.assign(executorForm, {
    appname: row?.appname || '',
    title: row?.title || '',
    addressType: String(row?.addressType || 0),
    addressList: row?.addressList || '',
  });
  mutationError.value = '';
  open.value = true;
}
async function save() {
  if (
    await run(
      () =>
        (editing.value ? executorApi.update : executorApi.create)({
          ...executorForm,
          id: editing.value?.id,
        }),
      '执行器已保存',
    )
  ) {
    open.value = false;
    await load();
  }
}
function askDelete(row: Executor) {
  deleting.value = row;
  mutationError.value = '';
  deleteOpen.value = true;
}
async function remove() {
  if (
    deleting.value &&
    (await run(() => executorApi.remove(deleting.value!.id), '执行器已删除'))
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
  Object.assign(filters, { appname: '', title: '' });
  void query();
}
onMounted(query);
</script>
<template>
  <div class="space-y-6">
    <PageHeading
      title="执行器管理"
      description="连接执行器，管理注册方式与运行节点。"
    >
      <Button
        variant="outline"
        size="icon"
        aria-label="刷新执行器"
        :disabled="loading"
        @click="load()"
      >
        <RefreshCw :class="{ 'animate-spin': loading }" />
      </Button>
      <Button @click="edit()">
        <Plus />
        新增执行器
      </Button>
    </PageHeading>
    <div
      class="flex flex-wrap items-center gap-2"
      role="search"
      aria-label="执行器筛选"
    >
      <div class="relative w-full sm:w-64">
        <Search
          class="pointer-events-none absolute top-2.5 left-3 size-4 text-muted-foreground"
        />
        <Input
          v-model="filters.appname"
          @keydown.enter="searchOnEnter"
          aria-label="AppName 筛选"
          placeholder="搜索 AppName…"
          class="pl-9"
        />
      </div>
      <Input
        v-model="filters.title"
        @keydown.enter="searchOnEnter"
        aria-label="执行器名称筛选"
        placeholder="执行器名称"
        class="w-full sm:w-48"
      />
      <Button
        v-if="filters.appname || filters.title"
        type="button"
        variant="ghost"
        @click="reset"
      >
        重置
      </Button>
      <span class="ml-auto hidden text-xs text-muted-foreground sm:block">
        共 {{ total }} 个执行器
      </span>
    </div>
    <div>
      <div class="overflow-hidden rounded-xl border bg-card">
        <DataState
          :loading="loading"
          :error="error"
          :empty="!rows.length"
          @retry="load()"
        >
          <template #empty-action>
            <Button
              variant="outline"
              size="sm"
              @click="filters.appname || filters.title ? reset() : edit()"
            >
              {{ filters.appname || filters.title ? '清除筛选' : '新增执行器' }}
            </Button>
          </template>
          <div class="hidden lg:block">
            <Table class="min-w-[680px]">
              <TableHeader>
                <TableRow>
                  <TableHead class="pl-5">执行器</TableHead>
                  <TableHead>地址方式</TableHead>
                  <TableHead>在线节点</TableHead>
                  <TableHead>更新时间</TableHead>
                  <TableHead class="w-16 pr-5 text-right">
                    <span class="sr-only">操作</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="row in rows" :key="row.id">
                  <TableCell class="max-w-80 py-4 pl-5">
                    <button
                      class="block max-w-full truncate text-left font-medium hover:underline"
                      @click="edit(row)"
                    >
                      {{ row.title }}
                    </button>
                    <span
                      class="mt-1 block truncate font-mono text-xs text-muted-foreground"
                    >
                      {{ row.appname }}
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">
                      {{ row.addressType === 0 ? '自动注册' : '手动录入' }}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Button
                      variant="ghost"
                      size="sm"
                      class="-ml-2"
                      @click="nodes = row"
                    >
                      <span
                        class="size-1.5 rounded-full"
                        :class="
                          row.registryList?.length
                            ? 'bg-emerald-600'
                            : 'bg-muted-foreground/40'
                        "
                      />
                      {{ row.registryList?.length || 0 }} 个节点
                    </Button>
                  </TableCell>
                  <TableCell class="text-xs text-muted-foreground tabular-nums">
                    {{ formatDateTime(row.updateTime) }}
                  </TableCell>
                  <TableCell class="pr-5 text-right">
                    <RecordActions
                      :label="row.title"
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
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <button
                    class="max-w-full truncate text-left text-sm font-medium"
                    @click="edit(row)"
                  >
                    {{ row.title }}
                  </button>
                  <p
                    class="mt-1 truncate font-mono text-xs text-muted-foreground"
                  >
                    {{ row.appname }}
                  </p>
                </div>
                <RecordActions
                  :label="row.title"
                  :busy="busy"
                  @edit="edit(row)"
                  @remove="askDelete(row)"
                />
              </div>
              <div class="flex items-center justify-between">
                <Badge variant="outline">
                  {{ row.addressType === 0 ? '自动注册' : '手动录入' }}
                </Badge>
                <Button variant="ghost" size="sm" @click="nodes = row">
                  {{ row.registryList?.length || 0 }} 个节点
                </Button>
              </div>
              <p class="text-xs text-muted-foreground">
                更新于 {{ formatDateTime(row.updateTime) }}
              </p>
            </article>
          </div>
        </DataState>
      </div>
      <PagePagination
        :page="page"
        :size="size"
        :total="total"
        :loading="loading"
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
    <DialogContent class="max-h-[90svh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle>{{ editing ? '编辑执行器' : '新增执行器' }}</DialogTitle>
        <DialogDescription>
          执行器通过 AppName 与调度平台关联。
        </DialogDescription>
      </DialogHeader>
      <form class="space-y-5" @submit.prevent="save">
        <FormField
          id="executor-app"
          label="AppName"
          hint="4–64 个字符，与执行器配置保持一致"
        >
          <Input
            id="executor-app"
            v-model="executorForm.appname"
            required
            minlength="4"
            maxlength="64"
          />
        </FormField>
        <FormField id="executor-title" label="执行器名称">
          <Input
            id="executor-title"
            v-model="executorForm.title"
            required
            maxlength="64"
          />
        </FormField>
        <FormField id="address-type" label="地址方式">
          <AppRadioGroup
            id="address-type"
            v-model="executorForm.addressType"
            label="地址方式"
            :options="[
              { label: '自动注册', value: 0 },
              { label: '手动录入', value: 1 },
            ]"
          />
        </FormField>
        <FormField id="addresses" label="节点地址" :hint="addressHint">
          <Textarea
            id="addresses"
            v-model="executorForm.addressList"
            :disabled="executorForm.addressType === '0'"
            :required="executorForm.addressType === '1'"
            placeholder="请输入节点地址，多个地址用英文逗号分隔"
          />
        </FormField>
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
            {{ busy ? '保存中…' : '保存' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
  <Dialog v-model:open="nodeOpen">
    <DialogContent class="max-h-[90svh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle>在线节点</DialogTitle>
        <DialogDescription>{{ nodes?.title }}</DialogDescription>
      </DialogHeader>
      <div
        v-if="nodes?.registryList?.length"
        class="max-h-80 space-y-2 overflow-auto"
      >
        <CopyText
          v-for="address in nodes.registryList"
          :key="address"
          :text="address"
        />
      </div>
      <p v-else class="py-6 text-center text-sm text-muted-foreground">
        当前没有在线节点
      </p>
    </DialogContent>
  </Dialog>
  <ConfirmDialog
    v-model="deleteOpen"
    title="删除执行器？"
    :description="`将删除「${deleting?.title || ''}」。请先确保该执行器没有关联任务。`"
    :busy="busy"
    :error="mutationError"
    destructive
    @confirm="remove"
  />
</template>
