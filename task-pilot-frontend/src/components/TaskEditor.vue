<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue';
import { LoaderCircle } from '@lucide/vue';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import FormField from './FormField.vue';
import AppSelect from './AppSelect.vue';
import { taskInfoApi, taskCodeApi } from '@/services/api';
import { getErrorMessage } from '@/utils/format';
import { GLUE_SOURCE_TEMPLATES } from '@/utils/glueTemplates';
import { useMutation } from '@/composables/useMutation';
import type { Task, TaskMeta } from '@/types/models';
const props = defineProps<{
  task: Task | null;
  meta: TaskMeta;
  executorId: number;
}>();
const open = defineModel<boolean>({ default: false });
const emit = defineEmits<{ saved: [] }>();
const { busy, error, run } = useMutation();
const section = ref('basic');
const formElement = ref<HTMLFormElement | null>(null);
const sectionScrollElement = ref<HTMLElement | null>(null);
// 页签共用滚动容器，切换后从首项开始；校验逻辑随后仍可聚焦具体错误字段。
watch(
  section,
  () => {
    if (sectionScrollElement.value) sectionScrollElement.value.scrollTop = 0;
  },
  { flush: 'post' },
);
const form = reactive({
  executorId: '',
  taskName: '',
  taskDesc: '',
  author: '',
  alarmEmail: '',
  scheduleType: 'CRON',
  scheduleConf: '',
  glueType: 'BEAN',
  executorHandler: '',
  executorParam: '',
  executorRouteStrategy: 'FIRST',
  executorBlockStrategy: 'SERIAL_EXECUTION',
  misfireStrategy: 'DO_NOTHING',
  executorTimeout: 0,
  executorFailRetryCount: 0,
  childTaskId: '',
  glueRemark: 'GLUE代码初始化',
  glueSource: '',
});
watch(open, (value) => {
  if (!value) return;
  error.value = '';
  section.value = 'basic';
  const t = props.task;
  Object.assign(form, {
    executorId: String(t?.executorId || props.executorId),
    taskName: t?.taskName || '',
    taskDesc: t?.taskDesc || '',
    author: t?.author || '',
    alarmEmail: t?.alarmEmail || '',
    scheduleType: t?.scheduleType || 'CRON',
    scheduleConf: t?.scheduleConf || '',
    glueType: t?.glueType || 'BEAN',
    executorHandler: t?.executorHandler || '',
    executorParam: t?.executorParam || '',
    executorRouteStrategy:
      t?.executorRouteStrategy ||
      String(props.meta.executorRouteStrategyOptions[0]?.value || 'FIRST'),
    executorBlockStrategy:
      t?.executorBlockStrategy ||
      String(
        props.meta.executorBlockStrategyOptions[0]?.value || 'SERIAL_EXECUTION',
      ),
    misfireStrategy:
      t?.misfireStrategy ||
      String(props.meta.misfireStrategyOptions[0]?.value || 'DO_NOTHING'),
    executorTimeout: t?.executorTimeout || 0,
    executorFailRetryCount: t?.executorFailRetryCount || 0,
    childTaskId: t?.childTaskId || '',
    glueRemark: t?.glueRemark || 'GLUE代码初始化',
    glueSource: t?.glueSource || '',
  });
});
watch(
  () => form.glueType,
  (next, previous) => {
    // 新建任务切换脚本语言时只替换默认模板，保留用户已经输入的代码。
    if (
      !props.task &&
      (!form.glueSource || form.glueSource === GLUE_SOURCE_TEMPLATES[previous])
    )
      form.glueSource = GLUE_SOURCE_TEMPLATES[next] || '';
  },
);
/**
 * 保存表单，编辑脚本时同时调用 GLUE 专用接口。
 * 参数来自当前表单；返回保存完成的 Promise，失败时保留弹窗和输入内容。
 */
async function save() {
  if (busy.value) return;
  // 未激活的页签保留表单值；保存时先切换到首个无效字段，避免浏览器无法聚焦隐藏输入框。
  const invalid = formElement.value?.querySelector<
    HTMLInputElement | HTMLTextAreaElement
  >('input:invalid, textarea:invalid');
  if (invalid) {
    section.value =
      invalid.closest<HTMLElement>('[data-editor-section]')?.dataset
        .editorSection || 'basic';
    await nextTick();
    invalid.reportValidity();
    invalid.focus();
    return;
  }
  const task = props.task;
  const payload = {
    ...form,
    id: task?.id,
    executorId: Number(form.executorId),
    scheduleConf: form.scheduleType === 'NONE' ? '' : form.scheduleConf,
  };
  const changedScript =
    task &&
    form.glueType !== 'BEAN' &&
    (form.glueSource !== task.glueSource ||
      form.glueRemark !== task.glueRemark);
  if (
    await run(async () => {
      await (task ? taskInfoApi.update : taskInfoApi.create)(payload);
      // 任务更新接口忽略 GLUE 字段，必须单独保存，且明确报告部分成功。
      if (changedScript) {
        try {
          await taskCodeApi.save({
            id: task.id,
            glueSource: form.glueSource,
            glueRemark: form.glueRemark,
          });
        } catch (e) {
          emit('saved');
          throw new Error(
            `任务配置已保存，但 GLUE 源码保存失败：${getErrorMessage(e)}。请重试保存。`,
          );
        }
      }
    }, '任务已保存')
  ) {
    open.value = false;
    emit('saved');
  }
}
</script>
<template>
  <Dialog
    :open="open"
    @update:open="
      (value) => {
        if (!busy) open = value;
      }
    "
  >
    <!-- 弹窗高度不随页签内容变化；小屏按视口收缩，较长表单仅在内容区滚动。 -->
    <DialogContent
      class="flex h-[680px] max-h-[92svh] flex-col gap-0 overflow-hidden p-0 sm:max-w-[760px]"
    >
      <DialogHeader class="shrink-0 border-b px-6 py-5">
        <DialogTitle>{{ task ? '编辑任务' : '新建任务' }}</DialogTitle>
        <DialogDescription>
          {{
            task
              ? `更新 ${task.taskName} 的任务配置。`
              : '配置任务信息与调度规则，创建后可手动启用。'
          }}
        </DialogDescription>
      </DialogHeader>
      <form
        ref="formElement"
        class="flex min-h-0 flex-1 flex-col"
        novalidate
        @submit.prevent="save"
      >
        <Tabs v-model="section" class="min-h-0 flex-1 gap-0">
          <div class="shrink-0 px-6 pt-4 pb-1">
            <TabsList class="w-full">
              <TabsTrigger value="basic" class="flex-1">基础信息</TabsTrigger>
              <TabsTrigger value="execution" class="flex-1">
                调度与执行
              </TabsTrigger>
              <TabsTrigger value="strategy" class="flex-1">
                告警与策略
              </TabsTrigger>
            </TabsList>
          </div>
          <div
            ref="sectionScrollElement"
            class="min-h-0 flex-1 overflow-y-auto px-6 py-5 [scrollbar-gutter:stable]"
          >
            <TabsContent
              value="basic"
              force-mount
              v-show="section === 'basic'"
              data-editor-section="basic"
              class="space-y-5"
            >
              <FormField
                id="task-executor"
                label="执行器"
                hint="选择负责运行此任务的执行器。"
              >
                <AppSelect
                  id="task-executor"
                  v-model="form.executorId"
                  label="执行器"
                  :options="
                    meta.executors.map((e) => ({ label: e.title, value: e.id }))
                  "
                />
              </FormField>
              <FormField
                id="task-name"
                label="任务名称"
                hint="用于唯一标识任务，创建后建议保持稳定。"
              >
                <Input
                  id="task-name"
                  v-model="form.taskName"
                  required
                  maxlength="128"
                  placeholder="例如 daily-report"
                />
              </FormField>
              <FormField id="task-desc" label="任务描述">
                <Input
                  id="task-desc"
                  v-model="form.taskDesc"
                  required
                  maxlength="255"
                  placeholder="例如 每日业务报表汇总"
                />
              </FormField>
              <FormField id="task-author" label="负责人">
                <Input
                  id="task-author"
                  v-model="form.author"
                  required
                  maxlength="64"
                  placeholder="请输入负责人"
                />
              </FormField>
            </TabsContent>
            <TabsContent
              value="execution"
              force-mount
              v-show="section === 'execution'"
              data-editor-section="execution"
              class="space-y-5"
            >
              <div class="grid gap-5 sm:grid-cols-2">
                <FormField id="schedule-type" label="调度类型">
                  <AppSelect
                    id="schedule-type"
                    v-model="form.scheduleType"
                    label="调度类型"
                    :options="meta.scheduleTypeOptions"
                  />
                </FormField>
                <FormField
                  id="schedule-conf"
                  :label="
                    form.scheduleType === 'FIX_RATE'
                      ? '固定频率（秒）'
                      : form.scheduleType === 'NONE'
                        ? '调度配置'
                        : 'Cron 表达式'
                  "
                >
                  <Input
                    id="schedule-conf"
                    v-model="form.scheduleConf"
                    :type="form.scheduleType === 'FIX_RATE' ? 'number' : 'text'"
                    :required="form.scheduleType !== 'NONE'"
                    :disabled="form.scheduleType === 'NONE'"
                    :placeholder="
                      form.scheduleType === 'NONE'
                        ? '仅手动触发或由子任务调用'
                        : form.scheduleType === 'FIX_RATE'
                          ? '例如 60'
                          : '例如 0 0 9 * * ?'
                    "
                    min="1"
                    step="1"
                  />
                </FormField>
              </div>
              <div class="border-t pt-5">
                <div class="grid gap-5 sm:grid-cols-2">
                  <FormField
                    id="glue-type"
                    label="运行模式"
                    :hint="task ? '创建后不可更改运行模式。' : undefined"
                  >
                    <AppSelect
                      id="glue-type"
                      v-model="form.glueType"
                      label="运行模式"
                      :disabled="!!task"
                      :options="meta.glueTypeOptions"
                    />
                  </FormField>
                  <FormField
                    id="task-handler"
                    label="执行器处理器"
                    :hint="
                      form.glueType === 'BEAN'
                        ? '填写执行器注册的 Handler 名称。'
                        : '脚本模式下可留空。'
                    "
                  >
                    <Input
                      id="task-handler"
                      v-model="form.executorHandler"
                      :required="form.glueType === 'BEAN'"
                      maxlength="255"
                      placeholder="例如 sampleHandler"
                    />
                  </FormField>
                </div>
              </div>
              <FormField
                id="executor-param"
                label="任务参数"
                hint="可选，执行时原样传入处理器。"
              >
                <Textarea
                  id="executor-param"
                  v-model="form.executorParam"
                  rows="3"
                  placeholder="输入任务需要的参数"
                  class="font-mono text-xs"
                />
              </FormField>
              <template v-if="form.glueType !== 'BEAN'">
                <FormField
                  id="glue-remark"
                  label="GLUE 备注"
                  hint="4–100 个字符，记录本次脚本的用途或修改内容。"
                >
                  <Input
                    id="glue-remark"
                    v-model="form.glueRemark"
                    required
                    minlength="4"
                    maxlength="100"
                  />
                </FormField>
                <FormField id="glue-source" label="GLUE 源码">
                  <Textarea
                    id="glue-source"
                    v-model="form.glueSource"
                    required
                    rows="10"
                    class="font-mono text-xs"
                    spellcheck="false"
                  />
                </FormField>
              </template>
            </TabsContent>
            <TabsContent
              value="strategy"
              force-mount
              v-show="section === 'strategy'"
              data-editor-section="strategy"
              class="space-y-5"
            >
              <FormField
                id="alarm-email"
                label="报警邮箱"
                hint="可选，多个邮箱使用英文逗号分隔。"
              >
                <Input
                  id="alarm-email"
                  v-model="form.alarmEmail"
                  placeholder="name@example.com"
                />
              </FormField>
              <div class="grid gap-5 border-t pt-5 sm:grid-cols-2">
                <FormField id="route-strategy" label="路由策略">
                  <AppSelect
                    id="route-strategy"
                    v-model="form.executorRouteStrategy"
                    label="路由策略"
                    :options="meta.executorRouteStrategyOptions"
                  />
                </FormField>
                <FormField id="block-strategy" label="阻塞策略">
                  <AppSelect
                    id="block-strategy"
                    v-model="form.executorBlockStrategy"
                    label="阻塞策略"
                    :options="meta.executorBlockStrategyOptions"
                  />
                </FormField>
                <FormField id="misfire-strategy" label="过期策略">
                  <AppSelect
                    id="misfire-strategy"
                    v-model="form.misfireStrategy"
                    label="过期策略"
                    :options="meta.misfireStrategyOptions"
                  />
                </FormField>
                <FormField
                  id="child-task"
                  label="子任务 ID"
                  hint="可选，多个 ID 使用英文逗号分隔。"
                >
                  <Input
                    id="child-task"
                    v-model="form.childTaskId"
                    placeholder="例如 12,18"
                  />
                </FormField>
                <FormField
                  id="task-timeout"
                  label="超时时间（秒）"
                  hint="0 表示不限制执行时间。"
                >
                  <Input
                    id="task-timeout"
                    v-model="form.executorTimeout"
                    type="number"
                    required
                    min="0"
                    step="1"
                  />
                </FormField>
                <FormField
                  id="task-retry"
                  label="失败重试次数"
                  hint="0 表示不自动重试。"
                >
                  <Input
                    id="task-retry"
                    v-model="form.executorFailRetryCount"
                    type="number"
                    required
                    min="0"
                    step="1"
                  />
                </FormField>
              </div>
            </TabsContent>
          </div>
        </Tabs>
        <div class="shrink-0 border-t bg-muted/20 px-6 py-4">
          <p v-if="error" role="alert" class="mb-3 text-sm text-destructive">
            {{ error }}
          </p>
          <DialogFooter class="m-0 rounded-none border-0 bg-transparent p-0">
            <Button
              type="button"
              variant="outline"
              :disabled="busy"
              @click="open = false"
            >
              取消
            </Button>
            <Button type="submit" :disabled="busy">
              <LoaderCircle v-if="busy" class="animate-spin" />
              {{ busy ? '保存中…' : '保存任务' }}
            </Button>
          </DialogFooter>
        </div>
      </form>
    </DialogContent>
  </Dialog>
</template>
