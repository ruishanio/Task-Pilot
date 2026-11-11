import { onBeforeUnmount, ref, shallowRef } from 'vue';
import type { AppResponse } from '@/types/http';
import type { PagePayload } from '@/types/domain';
import { getErrorMessage, parsePagePayload } from '@/utils/format';

/**
 * 管理分页请求，筛选变化和离开页面后丢弃过期响应。
 * @param fetcher 接收筛选条件、offset 和 pagesize 的后端查询函数。
 * @returns 列表、分页、加载与错误状态，以及加载和作废请求的方法。
 */
export function usePage<T>(
  fetcher: (
    params: Record<string, unknown>,
  ) => Promise<AppResponse<PagePayload<T>>>,
) {
  const rows = shallowRef<T[]>([]),
    total = ref(0),
    page = ref(1),
    size = ref(10),
    loading = ref(false),
    error = ref('');
  let generation = 0;
  let lastFilters: Record<string, unknown> = {};
  async function load(filters = lastFilters, reset = false) {
    lastFilters = { ...filters };
    if (reset) page.value = 1;
    const current = ++generation;
    loading.value = true;
    error.value = '';
    rows.value = [];
    try {
      const response = await fetcher({
        ...lastFilters,
        offset: (page.value - 1) * size.value,
        pagesize: size.value,
      });
      if (current !== generation) return;
      const result = parsePagePayload<T>(response);
      rows.value = result.list;
      total.value = result.total;
      // 删除最后一页的最后一项时，自动回到仍然存在的页码。
      const lastPage = Math.max(1, Math.ceil(total.value / size.value));
      if (page.value > lastPage) {
        page.value = lastPage;
        await load();
      }
    } catch (e) {
      if (current === generation) {
        error.value = getErrorMessage(e);
        total.value = 0;
      }
    } finally {
      if (current === generation) loading.value = false;
    }
  }
  function invalidate() {
    generation++;
    rows.value = [];
    total.value = 0;
    loading.value = false;
  }
  onBeforeUnmount(invalidate);
  return { rows, total, page, size, loading, error, load, invalidate };
}
