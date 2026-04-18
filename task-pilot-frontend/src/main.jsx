import React from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import App from './App';
import { AuthProvider } from './contexts/AuthContext';
import './styles/index.css';

dayjs.locale('zh-cn');

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        // 贴近原生 Ant Design / Pro 的默认视觉基线，减少过度定制化观感。
        token: {
          colorPrimary: '#1677ff',
          colorInfo: '#1677ff',
          borderRadius: 8,
          fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif',
        },
      }}
    >
      <AntdApp>
        {/* 生产环境会以静态文件方式挂回后端，因此使用 HashRouter 避免依赖服务端路由回退。 */}
        <HashRouter>
          <AuthProvider>
            <App />
          </AuthProvider>
        </HashRouter>
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>,
);
