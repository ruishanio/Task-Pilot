import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080';

  // 开发环境通过本地代理转发接口时，主动移除浏览器 Origin，
  // 避免后端把“代理请求”误判成跨域直连请求。
  function createProxyConfig() {
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
    // 生产产物会被放回 Spring Boot 的静态目录中访问，因此资源路径需要保持相对路径。
    base: './',
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          // 图表库仅在仪表盘使用，单独拆包可显著降低非仪表盘页面首屏体积。
          manualChunks(id) {
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
        '/api': createProxyConfig(),
        '/auth': createProxyConfig(),
        '/jobgroup': createProxyConfig(),
        '/jobinfo': createProxyConfig(),
        '/joblog': createProxyConfig(),
        '/user': createProxyConfig(),
        '/chartInfo': createProxyConfig(),
      },
    },
  };
});
