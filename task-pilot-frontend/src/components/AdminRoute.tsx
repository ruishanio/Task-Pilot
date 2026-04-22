import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface AdminRouteProps {
  children: ReactNode;
}

function AdminRoute({ children }: AdminRouteProps) {
  const location = useLocation();
  const { user } = useAuth();

  /**
   * 管理员页面的菜单已由后端裁剪，这里额外兜住手输地址直达的场景。
   */
  if (!user?.isAdmin) {
    return <Navigate to="/dashboard" replace state={{ from: location }} />;
  }

  return children;
}

export default AdminRoute;
