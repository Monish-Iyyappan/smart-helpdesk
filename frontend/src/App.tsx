import React from 'react';
import { MotionConfig } from 'motion/react';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { LoadingScreen } from './components/LoadingScreen';
import { StudentPage } from './pages/StudentPage';
import { AgentPage } from './pages/AgentPage';
import { AdminPage } from './pages/AdminPage';

// Lazy-load LoginPage to keep main dashboards lightweight
const LoginPage = React.lazy(() =>
  import('./pages/LoginPage').then((m) => ({ default: m.LoginPage }))
);

const AppRouter: React.FC = () => {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (!user) {
    return (
      <React.Suspense fallback={<LoadingScreen />}>
        <LoginPage />
      </React.Suspense>
    );
  }

  switch (user.role) {
    case 'ADMIN':
      return <AdminPage />;
    case 'AGENT':
      return <AgentPage />;
    case 'STUDENT':
    default:
      return <StudentPage />;
  }
};

export default function App(): React.ReactElement {
  return (
    <MotionConfig reducedMotion="user">
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </MotionConfig>
  );
}
