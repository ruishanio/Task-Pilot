import { defineConfig, loadEnv, type ProxyOptions } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080';

  // 开发环境通过本地代理转发接口时，主动移除浏览器 Origin，
  // 避免后端把“代理请求”误判成跨域直连请求。
  function createProxyConfig(): ProxyOptions {
    return {
      target: proxyTarget,
      changeOrigin: true,
      configure(proxy) {
        proxy.on('proxyReq', (proxyReq) => {
          proxyReq.removeHeader('origin');
        });
      },
    };
  }

  return {
    // 管理端前端统一以 `/web/*` 对外提供，资源路径也固定落在该前缀下。
    base: '/web/',
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          // 图表库仅在仪表盘使用，单独拆包可显著降低非仪表盘页面首屏体积。
          manualChunks(id: string) {
            if (!id.includes('node_modules')) {
              return undefined;
            }
            if (id.includes('/echarts/') || id.includes('echarts-for-react')) {
              return 'chart-vendor';
            }
            return undefined;
          },
        },
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/api/manage': createProxyConfig(),
      },
    },
  };
});
