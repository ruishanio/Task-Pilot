import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import App from './App';
import { AuthProvider } from './contexts/AuthContext';
import './styles/index.less';

dayjs.locale('zh-cn');

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('未找到根节点 #root');
}

ReactDOM.createRoot(rootElement).render(
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
        {/* 管理端统一挂在 `/web/*` 下，使用 basename 保持前后端地址语义一致。 */}
        <BrowserRouter basename="/web">
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>,
);
