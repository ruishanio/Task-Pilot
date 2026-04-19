import { Result } from 'antd';
import { Link } from 'react-router-dom';

function NotFoundPage() {
  return (
    <Result
      status="404"
      title="页面不存在"
      subTitle="当前路由未匹配到对应页面。"
      extra={<Link to="/dashboard">返回控制台首页</Link>}
    />
  );
}

export default NotFoundPage;
