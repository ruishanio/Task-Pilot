import { onBeforeUnmount, ref } from 'vue';
import { taskLogApi } from '@/services/api';
import { getErrorMessage } from '@/utils/format';
import type { TaskLog } from '@/types/models';

export function useLiveLog() {
  const content = ref(''),
    error = ref(''),
    loading = ref(false),
    following = ref(false);
  let generation = 0;
  let timer: ReturnType<typeof setTimeout> | undefined;
  function stop() {
    generation++;
    clearTimeout(timer);
    loading.value = false;
    following.value = false;
  }
  /**
   * 从第 1 行读取日志并串行轮询，关闭或切换日志后忽略旧响应。
   * @param row 当前执行记录，包含日志 ID、触发码和处理码。
   * @returns 首次读取完成的 Promise；后续轮询可通过 stop 取消。
   */
  async function start(row: TaskLog) {
    stop();
    content.value = '';
    error.value = '';
    if (row.triggerCode !== 200) return;
    const current = generation;
    let line = 1;
    following.value = row.handleCode <= 0;
    async function pull() {
      if (current !== generation) return;
      loading.value = true;
      try {
        const { data } = await taskLogApi.detailCat({
          logId: row.id,
          fromLineNum: line,
        });
        if (current !== generation) return;
        if (!data || data.fromLineNum !== line)
          throw new Error('日志返回的行号不连续，请重新加载');
        if (data.toLineNum >= line) {
          content.value += data.logContent || '';
          line = data.toLineNum + 1;
        }
        if (data.end) following.value = false;
        // 已结束的任务也可能一次只返回部分日志，持续读取到 end 或无新行。
        if (
          !data.end &&
          (following.value || data.toLineNum >= data.fromLineNum)
        )
          timer = setTimeout(pull, following.value ? 3000 : 0);
      } catch (e) {
        if (current === generation) {
          error.value = getErrorMessage(e);
          following.value = false;
        }
      } finally {
        if (current === generation) loading.value = false;
      }
    }
    await pull();
  }
  onBeforeUnmount(stop);
  return { content, error, loading, following, start, stop };
}
