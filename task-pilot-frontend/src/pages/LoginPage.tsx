import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Checkbox, Form, Input, Typography, message } from 'antd';
import { authApi } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { storeAccessToken } from '../services/authToken';
import { getErrorMessage } from '../utils/format';

interface LoginFormValues {
  userName: string;
  password: string;
  ifRemember?: boolean;
}

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, refreshBootstrap } = useAuth();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (user) {
      navigate('/dashboard', { replace: true });
    }
  }, [navigate, user]);

  /**
   * 登录成功后先持久化 JWT，再用 bootstrap 拉起当前登录用户与菜单。
   */
  async function handleSubmit(values: LoginFormValues) {
    try {
      setSubmitting(true);
      const response = await authApi.login({
        userName: values.userName,
        password: values.password,
        ifRemember: values.ifRemember ? 'on' : '',
      });
      const accessToken = response.data?.accessToken;
      if (!accessToken) {
        throw new Error('登录响应缺少 accessToken');
      }
      storeAccessToken(accessToken, Boolean(values.ifRemember));
      await refreshBootstrap();
      message.success('登录成功');
      const targetPath = location.state?.from?.pathname || '/dashboard';
      navigate(targetPath, { replace: true });
    } catch (error) {
      message.error(getErrorMessage(error, '登录失败'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-background auth-background-left" />
      <div className="auth-background auth-background-right" />
      <div className="auth-main">
        <div className="auth-panel">
          <div className="auth-card">
            <div className="auth-card-header">
              <Typography.Title level={2} className="auth-title">
                TASK PILOT
              </Typography.Title>
              <Typography.Paragraph className="auth-subtitle">任务调度平台</Typography.Paragraph>
            </div>
            <Form
              className="auth-form"
              size="large"
              initialValues={{ ifRemember: true }}
              onFinish={handleSubmit}
            >
              <Form.Item
                name="userName"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 4, max: 20, message: '用户名长度需在 4 到 20 位之间' },
                ]}
              >
                <Input placeholder="用户名" />
              </Form.Item>
              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 4, max: 20, message: '密码长度需在 4 到 20 位之间' },
                ]}
              >
                <Input.Password placeholder="密码" />
              </Form.Item>
              <div className="auth-form-extra">
                <Form.Item name="ifRemember" valuePropName="checked" noStyle>
                  <Checkbox>自动登录</Checkbox>
                </Form.Item>
              </div>
              <Button
                type="primary"
                htmlType="submit"
                block
                loading={submitting}
                className="auth-submit"
              >
                登 录
              </Button>
            </Form>
          </div>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
