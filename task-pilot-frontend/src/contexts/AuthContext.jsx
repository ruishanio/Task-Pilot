import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, frontendApi } from '../services/api';
import { registerAuthFailureHandler } from '../services/http';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [state, setState] = useState({
    initialized: false,
    loading: true,
    appName: 'Task Pilot',
    appNameFull: 'Task Pilot',
    version: '',
    user: null,
    menus: [],
  });

  async function refreshBootstrap(options = {}) {
    const silent = options.silent ?? false;
    setState((previous) => ({
      ...previous,
      loading: !silent || !previous.initialized,
    }));

    try {
      const response = await frontendApi.bootstrap();
      const payload = response.data || {};

      setState({
        initialized: true,
        loading: false,
        appName: payload.appName || 'Task Pilot',
        appNameFull: payload.appNameFull || payload.appName || 'Task Pilot',
        version: payload.version || '',
        user: payload.user || null,
        menus: payload.menus || [],
      });
      return payload;
    } catch (error) {
      setState((previous) => ({
        ...previous,
        initialized: true,
        loading: false,
        user: null,
        menus: [],
      }));
      return null;
    }
  }

  function clearAuthState() {
    setState((previous) => ({
      ...previous,
      initialized: true,
      loading: false,
      user: null,
      menus: [],
    }));
  }

  async function logout() {
    try {
      await authApi.logout();
    } finally {
      clearAuthState();
    }
  }

  useEffect(() => {
    registerAuthFailureHandler(() => {
      clearAuthState();
    });

    refreshBootstrap({ silent: true });
    return () => {
      registerAuthFailureHandler(null);
    };
  }, []);

  const contextValue = useMemo(
    () => ({
      ...state,
      refreshBootstrap,
      clearAuthState,
      logout,
    }),
    [state],
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
