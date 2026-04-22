const ACCESS_TOKEN_KEY = 'task-pilot-access-token';

/**
 * Bearer token 存储统一收口到一处，避免登录页、请求层和退出逻辑各自操作不同介质。
 */
export function getAccessToken(): string | null {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY) || window.sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

/**
 * 勾选自动登录时写入 localStorage，否则只保留在会话级别的 sessionStorage。
 */
export function storeAccessToken(accessToken: string, rememberLogin: boolean): void {
  if (rememberLogin) {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    return;
  }

  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
}

export function clearAccessToken(): void {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
}
