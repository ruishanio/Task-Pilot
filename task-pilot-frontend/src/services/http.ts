import axios, { AxiosHeaders, type AxiosRequestConfig } from 'axios';
import { clearAccessToken, getAccessToken } from './authToken';
import type { AppError, AppResponse, FormBodyRecord } from '../types/http';

const http = axios.create({
  // 开发环境通过 Vite 代理转发到后端，生产环境则与后端同域部署，因此统一使用相对接口地址。
  baseURL: '',
  timeout: 20000,
});

type AuthFailureHandler = ((payload?: AppResponse<any>) => void) | null;

let authFailureHandler: AuthFailureHandler = null;

export function registerAuthFailureHandler(handler: AuthFailureHandler): void {
  authFailureHandler = handler;
}

function createAppError(payload?: AppResponse<any>): AppError {
  const error = new Error(payload?.msg || '请求失败') as AppError;
  error.code = payload?.code;
  error.payload = payload;
  return error;
}

http.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  if (accessToken) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set('Authorization', `Bearer ${accessToken}`);
    config.headers = headers;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data as AppResponse<any>;
    if (payload && typeof payload === 'object' && typeof payload.code === 'number') {
      if (payload.code === 200) {
        return payload as any;
      }

      if (payload.code === 401) {
        clearAccessToken();
        authFailureHandler?.(payload);
      }
      return Promise.reject(createAppError(payload));
    }

    return payload as any;
  },
  (error: unknown) => Promise.reject(error),
);

export function toFormBody(values: FormBodyRecord = {}): URLSearchParams {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return;
    }

    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null) {
          params.append(key, String(item));
        }
      });
      return;
    }

    params.append(key, String(value));
  });
  return params;
}

const request = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return http.get<T, T>(url, config);
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return http.post<T, T>(url, data, config);
  },
  formPost<T>(url: string, data?: FormBodyRecord, config: AxiosRequestConfig = {}) {
    return http.post<T, T>(url, toFormBody(data), {
      ...config,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        ...config.headers,
      },
    });
  },
};

export default request;
