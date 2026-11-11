import { beforeEach, expect, it, vi } from 'vitest';
const bootstrap = vi.hoisted(() => vi.fn());
vi.mock('@/services/api', () => ({ frontendApi: { bootstrap } }));
beforeEach(() => {
  vi.resetModules();
  bootstrap.mockReset();
  localStorage.clear();
  sessionStorage.clear();
});
it('没有凭证时不请求 bootstrap，进入未登录状态', async () => {
  const store = await import('@/stores/auth');
  await store.initializeAuth();
  expect(bootstrap).not.toHaveBeenCalled();
  expect(store.auth.initialized).toBe(true);
  expect(store.auth.user).toBeNull();
});
it('并发路由初始化只发出一次身份请求', async () => {
  localStorage.setItem('task-pilot-access-token', 'test');
  bootstrap.mockResolvedValue({
    code: 200,
    data: { user: { userName: 'admin', isAdmin: true }, menus: [] },
  });
  const store = await import('@/stores/auth');
  await Promise.all([store.initializeAuth(), store.initializeAuth()]);
  expect(bootstrap).toHaveBeenCalledTimes(1);
  expect(store.auth.user?.isAdmin).toBe(true);
});
it('退出登录后在途身份响应不能恢复旧用户', async () => {
  localStorage.setItem('task-pilot-access-token', 'test');
  let resolve!: (value: unknown) => void;
  bootstrap.mockReturnValue(
    new Promise((r) => {
      resolve = r;
    }),
  );
  const store = await import('@/stores/auth');
  const pending = store.initializeAuth();
  store.logout();
  resolve({
    code: 200,
    data: { user: { userName: 'old-user', isAdmin: true } },
  });
  await pending;
  expect(store.auth.user).toBeNull();
  expect(localStorage.getItem('task-pilot-access-token')).toBeNull();
});
it('临时网络错误后可重试身份初始化', async () => {
  localStorage.setItem('task-pilot-access-token', 'test');
  bootstrap
    .mockRejectedValueOnce(new Error('network'))
    .mockResolvedValueOnce({
      code: 200,
      data: { user: { userName: 'recovered' } },
    });
  const store = await import('@/stores/auth');
  await expect(store.initializeAuth()).rejects.toThrow('network');
  await store.initializeAuth();
  expect(store.auth.user?.userName).toBe('recovered');
});
