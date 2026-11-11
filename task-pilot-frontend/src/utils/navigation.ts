/** 只接受站内路由，避免登录回跳参数将用户带到外部站点。 */
export function safeRedirect(value: unknown): string {
  if (
    typeof value !== 'string' ||
    !value.startsWith('/') ||
    value.startsWith('//') ||
    value.includes('\\') ||
    value.startsWith('/login')
  )
    return '/dashboard';
  return value;
}
