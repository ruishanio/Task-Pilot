import http from './http';
import type { BootstrapPayload, LoginTokenPayload } from '../types/domain';
import type { AppResponse, FormBodyRecord } from '../types/http';

const manageApiPrefix = '/api/manage';

export const frontendApi = {
  bootstrap: () => http.get<AppResponse<BootstrapPayload>>(`${manageApiPrefix}/system/bootstrap`),
  dashboard: () => http.get<AppResponse>(`${manageApiPrefix}/stat/dashboard`),
  chartInfo: (params?: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/chart_info`, params),
};

export const authApi = {
  login: (payload: FormBodyRecord) =>
    http.formPost<AppResponse<LoginTokenPayload>>(`${manageApiPrefix}/auth/login`, payload),
  logout: () => http.formPost<AppResponse>(`${manageApiPrefix}/auth/logout`),
  updatePassword: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/auth/update_password`, payload),
};

export const executorApi = {
  page: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/executor/page`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/executor/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/executor/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/executor/delete`, { 'ids[]': [id] }),
  loadById: (id: number | string) =>
    http.get<AppResponse>(`${manageApiPrefix}/executor/load_by_id`, { params: { id } }),
};

export const taskInfoApi = {
  meta: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_info/meta`, { params }),
  page: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_info/page`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_info/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_info/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_info/delete`, { 'ids[]': [id] }),
  start: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_info/start`, { 'ids[]': [id] }),
  stop: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_info/stop`, { 'ids[]': [id] }),
  trigger: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_info/trigger`, payload),
  nextTriggerTime: (payload?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_info/next_trigger_time`, { params: payload }),
};

export const taskLogApi = {
  meta: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_log/meta`, { params }),
  page: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_log/page`, { params }),
  kill: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_log/log_kill`, { id }),
  clear: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_log/clear_log`, payload),
  detailCat: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_log/log_detail_cat`, payload),
};

export const taskCodeApi = {
  save: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_code/save`, payload),
};

export const userApi = {
  meta: () => http.get<AppResponse>(`${manageApiPrefix}/user/meta`),
  page: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/user/page`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/delete`, { 'ids[]': [id] }),
};
