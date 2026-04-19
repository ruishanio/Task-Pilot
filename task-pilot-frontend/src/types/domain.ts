export interface AuthUser {
  id?: number | string;
  userName?: string;
  permission?: string;
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

export interface PagePayload<T = Record<string, unknown>> {
  data?: T[];
  total?: number;
  [key: string]: unknown;
}
