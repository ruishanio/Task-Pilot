import { createRouter, createWebHistory } from 'vue-router';
import { auth, initializeAuth, logout } from '@/stores/auth';
import { registerAuthFailureHandler } from '@/services/http';
import { safeRedirect } from '@/utils/navigation';

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      component: () => import('@/pages/LoginPage.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          component: () => import('@/pages/DashboardPage.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'task_info',
          alias: '/task_code',
          component: () => import('@/pages/TaskInfoPage.vue'),
          meta: { title: '任务管理' },
        },
        {
          path: 'task_log',
          alias: '/task_log/detail',
          component: () => import('@/pages/TaskLogPage.vue'),
          meta: { title: '执行日志' },
        },
        {
          path: 'executor',
          component: () => import('@/pages/ExecutorPage.vue'),
          meta: { title: '执行器', admin: true },
        },
        {
          path: 'user',
          component: () => import('@/pages/UserPage.vue'),
          meta: { title: '用户管理', admin: true },
        },
        {
          path: ':pathMatch(.*)*',
          component: () => import('@/pages/NotFoundPage.vue'),
          meta: { title: '页面不存在' },
        },
      ],
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
});
router.beforeEach(async (to) => {
  try {
    await initializeAuth();
  } catch {
    if (!to.meta.public)
      return {
        path: '/login',
        query: { redirect: to.fullPath, reason: 'bootstrap' },
      };
  }
  if (!to.meta.public && !auth.user)
    return { path: '/login', query: { redirect: to.fullPath } };
  if (to.meta.admin && !auth.user?.isAdmin) return '/dashboard';
  if (to.path === '/login' && auth.user) return safeRedirect(to.query.redirect);
});
router.afterEach((to) => {
  document.title = `${to.meta.title || 'Task Pilot'} · Task Pilot`;
});
registerAuthFailureHandler(() => {
  logout();
  const route = router.currentRoute.value;
  if (!route.meta.public)
    void router.replace({
      path: '/login',
      query: { redirect: route.fullPath },
    });
});
