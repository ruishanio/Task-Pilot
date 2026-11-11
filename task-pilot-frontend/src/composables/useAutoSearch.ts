import { onScopeDispose, watch, type WatchSource } from 'vue';

/**
 * 文本输入停顿后搜索，离开页面时取消尚未发出的请求。
 * @param source 需要自动搜索的文本字段，不包含立即生效的下拉筛选。
 * @param run 使用当前筛选条件加载第一页的函数。
 * @param delay 输入停顿时间，单位毫秒。
 * @returns 立即搜索方法，以及跳过中文输入法候选确认的回车处理方法。
 */
export function useAutoSearch(
  source: WatchSource<unknown>,
  run: () => void | Promise<unknown>,
  delay = 300,
) {
  let timer: ReturnType<typeof setTimeout> | undefined;
  function cancel() {
    clearTimeout(timer);
    timer = undefined;
  }
  function search() {
    cancel();
    return run();
  }
  function onEnter(event: KeyboardEvent, action = search) {
    // 输入法确认候选字时不提交，等待 v-model 在组合输入结束后更新。
    if (event.isComposing || event.keyCode === 229) return;
    event.preventDefault();
    void action();
  }
  watch(
    source,
    () => {
      cancel();
      timer = setTimeout(() => {
        void search();
      }, delay);
    },
    { flush: 'sync' },
  );
  // 同步监听使重置和立即筛选能够取消本次变更的延迟请求，避免重复加载。
  onScopeDispose(cancel);
  return { search, onEnter };
}
