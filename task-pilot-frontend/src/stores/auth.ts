import { reactive, readonly } from 'vue';
import { frontendApi } from '@/services/api';
import { getAccessToken, clearAccessToken } from '@/services/authToken';
import type { AuthUser, MenuItem } from '@/types/domain';

const state = reactive({
  initialized: false,
  user: null as AuthUser | null,
  menus: [] as MenuItem[],
  appName: 'Task Pilot',
  version: '',
});
let generation = 0;
let initialization: Promise<void> | undefined;
export const auth = readonly(state);
export function logout() {
  generation++;
  initialization = undefined;
  clearAccessToken();
  state.user = null;
  state.menus = [];
  state.version = '';
  state.initialized = true;
}
/**
 * 初始化当前身份，合并并发请求；网络错误向上抛出以允许重试。
 * @param force 是否忽略已初始化状态，用于登录后重新获取身份。
 * @returns 身份初始化完成的 Promise，无凭证时保持未登录状态。
 */
export async function initializeAuth(force = false): Promise<void> {
  if (initialization) return initialization;
  if (state.initialized && !force) return;
  if (!getAccessToken()) {
    logout();
    return;
  }
  const current = ++generation;
  initialization = (async () => {
    const { data } = await frontendApi.bootstrap();
    // 退出登录或身份切换后，旧 bootstrap 响应不能恢复上一个用户。
    if (current !== generation) return;
    state.user = data?.user || null;
    state.menus = data?.menus || [];
    state.appName = data?.appName || 'Task Pilot';
    // 版本号取自服务端构建信息，避免与前端包版本脱节。
    state.version = data?.version?.trim() || '';
    state.initialized = true;
  })();
  try {
    await initialization;
  } finally {
    if (current === generation) initialization = undefined;
  }
}
