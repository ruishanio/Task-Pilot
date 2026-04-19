import { useMemo, useState } from 'react';
import type { MenuProps } from 'antd';
import {
  AppstoreOutlined,
  ClockCircleOutlined,
  CloudServerOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  LogoutOutlined,
  ReloadOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  Avatar,
  Dropdown,
  Form,
  Input,
  Layout,
  Menu,
  Modal,
  Space,
  Typography,
  message,
} from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { getErrorMessage } from '../utils/format';
import type { MenuItem } from '../types/domain';

const { Header, Content, Footer, Sider } = Layout;
const iconMap = {
  'fa-home': <DashboardOutlined />,
  'fa-clock-o': <ClockCircleOutlined />,
  'fa-database': <DatabaseOutlined />,
  'fa-cloud': <CloudServerOutlined />,
  'fa-users': <TeamOutlined />,
};

interface PasswordFormValues {
  oldPassword: string;
  password: string;
  confirmPassword: string;
}

function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { appName, user, menus, logout, refreshBootstrap } = useAuth();

  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordForm] = Form.useForm<PasswordFormValues>();

  /**
   * 兼容 `/jobcode`、`/joblog/detail` 这类历史页面别名，确保侧边栏高亮仍落在主功能页上。
   */
  function normalizeMenuPath(pathname: string): string {
    if (pathname.startsWith('/jobcode')) {
      return '/jobinfo';
    }
    if (pathname.startsWith('/joblog/detail')) {
      return '/joblog';
    }
    return pathname;
  }

  // 统一把后端菜单结构转换成 Ant Design 可消费的数据。
  const menuItems = useMemo(
    () =>
      menus.map((item: MenuItem) => ({
        key: item.url,
        icon: iconMap[item.icon as keyof typeof iconMap] || <AppstoreOutlined />,
        label: item.url === '/dashboard' ? '工作台' : item.name,
      })),
    [menus],
  );

  const currentPath = normalizeMenuPath(location.pathname);
  const currentMenu =
    menuItems.find((item) => currentPath.startsWith(item.key)) || menuItems[0];
  const footerText = appName || 'TASK PILOT';

  async function handleLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  async function handleUpdatePassword() {
    try {
      const values = await passwordForm.validateFields();
      setPasswordSubmitting(true);
      await authApi.updatePassword({
        oldPassword: values.oldPassword,
        password: values.password,
      });
      message.success('密码已更新');
      passwordForm.resetFields();
      setPasswordModalOpen(false);
    } catch (error) {
      if ((error as { errorFields?: unknown })?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '密码更新失败'));
    } finally {
      setPasswordSubmitting(false);
    }
  }

  const dropdownItems: MenuProps['items'] = [
    {
      key: 'refresh',
      icon: <ReloadOutlined />,
      label: '刷新菜单',
      onClick: () => refreshBootstrap({ silent: true }),
    },
    {
      key: 'password',
      icon: <UserOutlined />,
      label: '修改密码',
      onClick: () => setPasswordModalOpen(true),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  return (
    <Layout className="app-shell">
      <Sider
        className="app-sider"
        width={248}
        theme="dark"
      >
        <div className="app-logo">
          <div className="app-logo-text">
            <strong>TASK PILOT</strong>
            <span>任务调度平台</span>
          </div>
        </div>
        <Menu
          className="app-menu"
          theme="dark"
          mode="inline"
          selectedKeys={[currentMenu?.key || '/dashboard']}
          items={menuItems}
          onClick={({ key }) => navigate(String(key))}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Space size="middle">
            <Typography.Text className="app-header-title">
              {currentMenu?.label || appName}
            </Typography.Text>
          </Space>
          <Dropdown menu={{ items: dropdownItems }} placement="bottomRight" trigger={['click']}>
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <Typography.Text strong>{user?.userName}</Typography.Text>
            </Space>
          </Dropdown>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
        <Footer className="app-footer">{footerText}</Footer>
      </Layout>

      <Modal
        title="修改密码"
        open={passwordModalOpen}
        confirmLoading={passwordSubmitting}
        onOk={handleUpdatePassword}
        onCancel={() => {
          setPasswordModalOpen(false);
          passwordForm.resetFields();
        }}
        destroyOnClose
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            label="旧密码"
            name="oldPassword"
            rules={[{ required: true, message: '请输入旧密码' }]}
          >
            <Input.Password placeholder="请输入当前密码" />
          </Form.Item>
          <Form.Item
            label="新密码"
            name="password"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 4, max: 20, message: '密码长度需在 4 到 20 位之间' },
            ]}
          >
            <Input.Password placeholder="请输入 4 到 20 位新密码" />
          </Form.Item>
          <Form.Item
            label="确认新密码"
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的新密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  );
}

export default AppLayout;
