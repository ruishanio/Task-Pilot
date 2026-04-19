import { Suspense, lazy } from 'react';
import { Spin } from 'antd';
import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';

const AppLayout = lazy(() => import('./components/AppLayout'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const JobGroupPage = lazy(() => import('./pages/JobGroupPage'));
const JobInfoPage = lazy(() => import('./pages/JobInfoPage'));
const JobLogPage = lazy(() => import('./pages/JobLogPage'));
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
          <Route path="jobgroup" element={<JobGroupPage />} />
          <Route path="jobinfo" element={<JobInfoPage />} />
          <Route path="jobcode" element={<JobInfoPage />} />
          <Route path="joblog" element={<JobLogPage />} />
          <Route path="joblog/detail" element={<JobLogPage />} />
          <Route path="user" element={<UserPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}

export default App;
