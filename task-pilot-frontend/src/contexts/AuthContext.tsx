import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi, frontendApi } from '../services/api';
import { clearAccessToken, getAccessToken } from '../services/authToken';
import { registerAuthFailureHandler } from '../services/http';
import type { AuthUser, BootstrapPayload, MenuItem } from '../types/domain';

interface BootstrapOptions {
  silent?: boolean;
}

interface AuthState {
  initialized: boolean;
  loading: boolean;
  appName: string;
  appNameFull: string;
  user: AuthUser | null;
  menus: MenuItem[];
}

interface AuthContextValue extends AuthState {
  refreshBootstrap: (options?: BootstrapOptions) => Promise<BootstrapPayload | null>;
  clearAuthState: () => void;
  logout: () => Promise<void>;
}

interface AuthProviderProps {
  children: ReactNode;
}

const defaultAuthState: AuthState = {
  initialized: false,
  loading: true,
  appName: 'Task Pilot',
  appNameFull: 'Task Pilot',
  user: null,
  menus: [],
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: AuthProviderProps) {
  const [state, setState] = useState<AuthState>(defaultAuthState);

  async function refreshBootstrap(options: BootstrapOptions = {}): Promise<BootstrapPayload | null> {
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
        user: payload.user || null,
        menus: payload.menus || [],
      });
      return payload;
    } catch {
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

  function clearAuthState(): void {
    setState((previous) => ({
      ...previous,
      initialized: true,
      loading: false,
      user: null,
      menus: [],
    }));
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout();
    } finally {
      clearAccessToken();
      clearAuthState();
    }
  }

  useEffect(() => {
    registerAuthFailureHandler(() => {
      clearAccessToken();
      clearAuthState();
    });

    if (getAccessToken()) {
      refreshBootstrap({ silent: true });
    } else {
      clearAuthState();
    }
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

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth 必须在 AuthProvider 内部使用');
  }
  return context;
}
