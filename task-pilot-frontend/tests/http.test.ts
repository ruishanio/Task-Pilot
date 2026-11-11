import { afterEach, expect, it, vi } from 'vitest';
import { AxiosError, AxiosHeaders } from 'axios';
import request, { registerAuthFailureHandler } from '@/services/http';
import { getAccessToken, storeAccessToken } from '@/services/authToken';
import { sanitizeLogContent } from '@/utils/logContent';
afterEach(() => {
  registerAuthFailureHandler(null);
  localStorage.clear();
  sessionStorage.clear();
});
it('业务 401 清除凭证并通知路由层', async () => {
  storeAccessToken('expired', false);
  const failed = vi.fn();
  registerAuthFailureHandler(failed);
  await expect(
    request.get('/api/manage/test', {
      adapter: async (config) => ({
        config,
        status: 200,
        statusText: 'OK',
        headers: {},
        data: { code: 401, msg: '登录过期' },
      }),
    }),
  ).rejects.toThrow('登录过期');
  expect(getAccessToken()).toBeNull();
  expect(failed).toHaveBeenCalledTimes(1);
});
it('网关 HTTP 401 同样清除登录态', async () => {
  storeAccessToken('expired', true);
  const failed = vi.fn();
  registerAuthFailureHandler(failed);
  await expect(
    request.get('/api/manage/test', {
      adapter: async (config) => {
        throw new AxiosError(
          'Unauthorized',
          'ERR_BAD_REQUEST',
          config,
          undefined,
          {
            config,
            status: 401,
            statusText: 'Unauthorized',
            headers: {},
            data: '',
          },
        );
      },
    }),
  ).rejects.toThrow('Unauthorized');
  expect(getAccessToken()).toBeNull();
  expect(failed).toHaveBeenCalledTimes(1);
});
it('Bearer 头与业务响应解包保持兼容', async () => {
  storeAccessToken('test-token', false);
  const data = await request.get('/api/manage/test', {
    adapter: async (config) => {
      expect(AxiosHeaders.from(config.headers).get('Authorization')).toBe(
        'Bearer test-token',
      );
      return {
        config,
        status: 200,
        statusText: 'OK',
        headers: {},
        data: { code: 200, data: { id: 7 } },
      };
    },
  });
  expect(data).toEqual({ code: 200, data: { id: 7 } });
});
it('日志保留可读格式但无法注入可执行 HTML', () => {
  const content = sanitizeLogContent(
    '<b onclick="alert(1)">结果</b><br><img src=x onerror=alert(1)><script>alert(1)</script><a href="javascript:alert(1)">链接文本</a>',
  );
  expect(content).toBe('<b>结果</b><br>链接文本');
  expect(sanitizeLogContent('&lt;script&gt;文本&lt;/script&gt;')).toContain(
    '&lt;script&gt;',
  );
});
