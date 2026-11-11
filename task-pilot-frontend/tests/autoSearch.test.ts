import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { effectScope, reactive, ref, type EffectScope } from 'vue';
import { useAutoSearch } from '@/composables/useAutoSearch';
const scopes: EffectScope[] = [];
function setup<T>(factory: () => T): T {
  const scope = effectScope();
  scopes.push(scope);
  return scope.run(factory)!;
}
beforeEach(() => vi.useFakeTimers());
afterEach(() => {
  scopes.splice(0).forEach((scope) => scope.stop());
  vi.useRealTimers();
});
describe('列表自动搜索', () => {
  it('连续修改多个文本字段时，仅在停顿后使用最后的条件搜索', () => {
    const filters = reactive({ name: '', title: '' });
    const request = vi.fn(() => ({ ...filters }));
    setup(() =>
      useAutoSearch(
        () => [filters.name, filters.title],
        () => {
          request();
        },
      ),
    );
    filters.name = 'sample';
    vi.advanceTimersByTime(200);
    filters.title = '通用';
    vi.advanceTimersByTime(299);
    expect(request).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(request).toHaveBeenCalledTimes(1);
    expect(request.mock.results[0]?.value).toEqual({
      name: 'sample',
      title: '通用',
    });
  });
  it('回车立即搜索并取消待执行的延迟搜索', () => {
    const keyword = ref('');
    const request = vi.fn();
    const search = setup(() => useAutoSearch(keyword, request));
    keyword.value = 'daily';
    search.onEnter(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(request).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(500);
    expect(request).toHaveBeenCalledTimes(1);
  });
  it('中文输入法确认候选时不触发回车搜索，也不提交高级筛选', () => {
    const request = vi.fn(),
      apply = vi.fn();
    const search = setup(() => useAutoSearch(ref(''), request));
    search.onEnter(
      new KeyboardEvent('keydown', { key: 'Enter', isComposing: true }),
    );
    search.onEnter(
      new KeyboardEvent('keydown', { key: 'Enter', isComposing: true }),
      apply,
    );
    vi.advanceTimersByTime(500);
    expect(request).not.toHaveBeenCalled();
    expect(apply).not.toHaveBeenCalled();
  });
  it('重置和立即切换筛选后不会再次发出旧的延迟请求', () => {
    const keyword = ref('');
    const request = vi.fn(() => keyword.value);
    const search = setup(() =>
      useAutoSearch(keyword, () => {
        request();
      }),
    );
    keyword.value = 'daily';
    keyword.value = '';
    search.search();
    vi.advanceTimersByTime(500);
    expect(request).toHaveBeenCalledTimes(1);
    expect(request.mock.results[0]?.value).toBe('');
  });
  it('离开页面后取消尚未发出的请求', () => {
    const keyword = ref('');
    const request = vi.fn();
    setup(() => useAutoSearch(keyword, request));
    keyword.value = 'daily';
    scopes[0]?.stop();
    vi.advanceTimersByTime(500);
    expect(request).not.toHaveBeenCalled();
  });
});
