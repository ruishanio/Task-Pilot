import http from './http';
import type { BootstrapPayload } from '../types/domain';
import type { AppResponse, FormBodyRecord } from '../types/http';

const manageApiPrefix = '/api/manage';

export const frontendApi = {
  bootstrap: () => http.get<AppResponse<BootstrapPayload>>(`${manageApiPrefix}/system/bootstrap`),
  dashboard: () => http.get<AppResponse>(`${manageApiPrefix}/stat/dashboard`),
  chartInfo: (params?: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/chartInfo`, params),
};

export const authApi = {
  login: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/auth/doLogin`, payload),
  logout: () => http.formPost<AppResponse>(`${manageApiPrefix}/auth/logout`),
  updatePassword: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/auth/updatePwd`, payload),
};

export const jobGroupApi = {
  pageList: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/executor/pageList`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/executor/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/executor/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/executor/delete`, { 'ids[]': [id] }),
  loadById: (id: number | string) =>
    http.get<AppResponse>(`${manageApiPrefix}/executor/loadById`, { params: { id } }),
};

export const jobInfoApi = {
  meta: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_info/meta`, { params }),
  pageList: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_info/pageList`, { params }),
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
    http.get<AppResponse>(`${manageApiPrefix}/task_info/nextTriggerTime`, { params: payload }),
};

export const jobLogApi = {
  meta: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_log/meta`, { params }),
  pageList: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/task_log/pageList`, { params }),
  kill: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_log/logKill`, { id }),
  clear: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_log/clearLog`, payload),
  detailCat: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/task_log/logDetailCat`, payload),
};

export const userApi = {
  meta: () => http.get<AppResponse>(`${manageApiPrefix}/user/meta`),
  pageList: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/user/pageList`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/delete`, { 'ids[]': [id] }),
};
