import { Suspense, lazy } from 'react';
import { Spin } from 'antd';
import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';

const AppLayout = lazy(() => import('./components/AppLayout'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const ExecutorPage = lazy(() => import('./pages/ExecutorPage'));
const TaskInfoPage = lazy(() => import('./pages/TaskInfoPage'));
const TaskLogPage = lazy(() => import('./pages/TaskLogPage'));
const UserPage = lazy(() => import('./pages/UserPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));

function RouteLoading() {
  return (
    <div className="page-loading">
      <Spin size="large" />
    </div>
  );
}

function App() {
  return (
    <Suspense fallback={<RouteLoading />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="executor" element={<ExecutorPage />} />
          <Route path="task_info" element={<TaskInfoPage />} />
          <Route path="task_code" element={<TaskInfoPage />} />
          <Route path="task_log" element={<TaskLogPage />} />
          <Route path="task_log/detail" element={<TaskLogPage />} />
          <Route path="user" element={<UserPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}

export default App;
