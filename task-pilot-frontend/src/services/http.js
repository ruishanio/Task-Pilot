import axios from 'axios';

const http = axios.create({
  // 开发环境通过 Vite 代理转发到后端，生产环境则与后端同域部署，因此统一使用相对接口地址。
  baseURL: '',
  timeout: 20000,
  withCredentials: true,
});

let authFailureHandler = null;

export function registerAuthFailureHandler(handler) {
  authFailureHandler = handler;
}

function createAppError(payload) {
  const error = new Error(payload?.msg || '请求失败');
  error.code = payload?.code;
  error.payload = payload;
  return error;
}

http.interceptors.response.use(
  (response) => {
    const payload = response.data;
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 200) {
        return payload;
      }

      if (payload.code === 401) {
        authFailureHandler?.(payload);
      }
      return Promise.reject(createAppError(payload));
    }

    return payload;
  },
  (error) => Promise.reject(error),
);

export function toFormBody(values = {}) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return;
    }

    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null) {
          params.append(key, item);
        }
      });
      return;
    }

    params.append(key, value);
  });
  return params;
}

export default {
  get(url, config) {
    return http.get(url, config);
  },
  post(url, data, config) {
    return http.post(url, data, config);
  },
  formPost(url, data, config = {}) {
    return http.post(url, toFormBody(data), {
      ...config,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        ...config.headers,
      },
    });
  },
};
