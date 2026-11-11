import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createApp, defineComponent, type App } from 'vue';
import { usePage } from '@/composables/usePage';
import { useLiveLog } from '@/composables/useLiveLog';
import { taskLogApi } from '@/services/api';
import { logStatus } from '@/utils/logStatus';
import { safeRedirect } from '@/utils/navigation';
import {
  storeAccessToken,
  getAccessToken,
  clearAccessToken,
} from '@/services/authToken';
import { toFormBody } from '@/services/http';
import type { AppResponse } from '@/types/http';
import type { PagePayload } from '@/types/domain';
import type { TaskLog } from '@/types/models';
vi.mock('@/services/api', () => ({ taskLogApi: { detailCat: vi.fn() } }));
const wrappers: App[] = [];
function setup<T>(factory: () => T): T {
  let result!: T;
  const app = createApp(
    defineComponent({
      setup() {
        result = factory();
        return () => null;
      },
    }),
  );
  app.mount(document.createElement('div'));
  wrappers.push(app);
  return result;
}
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
}
afterEach(() => {
  wrappers.splice(0).forEach((w) => w.unmount());
  vi.useRealTimers();
});
beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
});
describe('登录与后端参数兼容', () => {
  it('切换保持登录时移除另一种介质中的旧凭证', () => {
    storeAccessToken('local-token', true);
    storeAccessToken('session-token', false);
    expect(localStorage.getItem('task-pilot-access-token')).toBeNull();
    expect(getAccessToken()).toBe('session-token');
    storeAccessToken('new-local', true);
    expect(sessionStorage.getItem('task-pilot-access-token')).toBeNull();
    clearAccessToken();
    expect(getAccessToken()).toBeNull();
  });
  it('站内回跳保留查询参数并拒绝外部地址', () => {
    expect(safeRedirect('/task_log?executorId=1&taskId=4')).toBe(
      '/task_log?executorId=1&taskId=4',
    );
    for (const value of [
      '//evil.example',
      'https://evil.example',
      '/\\evil.example',
      '/login',
      null,
    ])
      expect(safeRedirect(value)).toBe('/dashboard');
  });
  it('多选 ID 使用后端约定的重复表单键，保留 0 和 false', () => {
    const body = toFormBody({
      'ids[]': [1, 2],
      role: 0,
      enabled: false,
      empty: null,
    });
    expect(body.getAll('ids[]')).toEqual(['1', '2']);
    expect(body.get('role')).toBe('0');
    expect(body.get('enabled')).toBe('false');
    expect(body.has('empty')).toBe(false);
  });
});
describe('列表请求竞争', () => {
  it('筛选快速变化时只显示最后一次查询结果', async () => {
    const old = deferred<AppResponse<PagePayload<{ id: number }>>>();
    const fetcher = vi
      .fn()
      .mockReturnValueOnce(old.promise)
      .mockResolvedValueOnce({
        code: 200,
        data: { data: [{ id: 2 }], total: 1 },
      });
    const page = setup(() => usePage<{ id: number }>(fetcher));
    const pending = page.load({ executorId: 1 });
    await page.load({ executorId: 2 }, true);
    old.resolve({ code: 200, data: { data: [{ id: 1 }], total: 1 } });
    await pending;
    expect(page.rows.value).toEqual([{ id: 2 }]);
    expect(page.loading.value).toBe(false);
  });
  it('删除最后一条后回退到有效页码', async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce({ code: 200, data: { data: [], total: 10 } })
      .mockResolvedValueOnce({
        code: 200,
        data: { data: [{ id: 1 }], total: 10 },
      });
    const state = setup(() => usePage<{ id: number }>(fetcher));
    state.page.value = 2;
    await state.load();
    expect(state.page.value).toBe(1);
    expect(fetcher.mock.calls[1][0].offset).toBe(0);
    expect(state.rows.value).toEqual([{ id: 1 }]);
    expect(state.loading.value).toBe(false);
  });
});
describe('日志状态与轮询', () => {
  it('触发失败不显示运行中，已完成任务状态准确', () => {
    expect(logStatus({ triggerCode: 500, handleCode: 0 }).label).toBe(
      '触发失败',
    );
    expect(logStatus({ triggerCode: 200, handleCode: 0 }).kind).toBe('running');
    expect(logStatus({ triggerCode: 200, handleCode: 200 }).kind).toBe(
      'success',
    );
    expect(logStatus({ triggerCode: 200, handleCode: 500 }).label).toBe(
      '执行失败',
    );
  });
  it('关闭抽屉后忽略在途日志并取消轮询', async () => {
    vi.useFakeTimers();
    const pending =
      deferred<
        AppResponse<{
          fromLineNum: number;
          toLineNum: number;
          logContent: string;
        }>
      >();
    vi.mocked(taskLogApi.detailCat).mockReturnValueOnce(pending.promise);
    const live = setup(useLiveLog);
    const start = live.start({
      id: 1,
      triggerCode: 200,
      handleCode: 0,
    } as TaskLog);
    live.stop();
    pending.resolve({
      code: 200,
      data: { fromLineNum: 1, toLineNum: 1, logContent: '旧日志' },
    });
    await start;
    expect(live.content.value).toBe('');
    expect(live.following.value).toBe(false);
    expect(vi.getTimerCount()).toBe(0);
  });
  it('按行追加日志，到结束标记后停止请求', async () => {
    vi.useFakeTimers();
    vi.mocked(taskLogApi.detailCat)
      .mockResolvedValueOnce({
        code: 200,
        data: {
          fromLineNum: 1,
          toLineNum: 2,
          logContent: '第一段\n',
          end: false,
        },
      })
      .mockResolvedValueOnce({
        code: 200,
        data: { fromLineNum: 3, toLineNum: 3, logContent: '第二段', end: true },
      });
    const live = setup(useLiveLog);
    await live.start({ id: 1, triggerCode: 200, handleCode: 0 } as TaskLog);
    await vi.advanceTimersByTimeAsync(3000);
    expect(taskLogApi.detailCat).toHaveBeenLastCalledWith({
      logId: 1,
      fromLineNum: 3,
    });
    expect(live.content.value).toBe('第一段\n第二段');
    expect(live.following.value).toBe(false);
    expect(vi.getTimerCount()).toBe(0);
  });
});
