export interface AuthUser {
  userId?: number | string;
  userName?: string;
  roleList?: string[];
  permissionList?: string[];
  extraInfo?: Record<string, string>;
  isAdmin?: boolean;
  [key: string]: unknown;
}

export interface MenuItem {
  name?: string;
  icon?: string;
  url: string;
  permission?: string;
  [key: string]: unknown;
}

export interface BootstrapPayload {
  appName?: string;
  appNameFull?: string;
  user?: AuthUser | null;
  menus?: MenuItem[];
  [key: string]: unknown;
}

export interface LoginTokenPayload {
  accessToken?: string;
  tokenType?: string;
  expiresAt?: number;
  [key: string]: unknown;
}

export interface PagePayload<T = Record<string, unknown>> {
  data?: T[];
  total?: number;
  [key: string]: unknown;
}
