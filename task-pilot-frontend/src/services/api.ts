import http from './http';
import type { BootstrapPayload } from '../types/domain';
import type { AppResponse, FormBodyRecord } from '../types/http';

const manageApiPrefix = '/api/manage';

export const frontendApi = {
  bootstrap: () => http.get<AppResponse<BootstrapPayload>>(`${manageApiPrefix}/frontend/bootstrap`),
  dashboard: () => http.get<AppResponse>(`${manageApiPrefix}/frontend/dashboard`),
  jobInfoMeta: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/frontend/jobinfo/meta`, { params }),
  jobLogMeta: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/frontend/joblog/meta`, { params }),
  userMeta: () => http.get<AppResponse>(`${manageApiPrefix}/frontend/user/meta`),
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
    http.get<AppResponse>(`${manageApiPrefix}/jobgroup/pageList`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobgroup/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobgroup/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobgroup/delete`, { 'ids[]': [id] }),
  loadById: (id: number | string) =>
    http.get<AppResponse>(`${manageApiPrefix}/jobgroup/loadById`, { params: { id } }),
};

export const jobInfoApi = {
  pageList: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/jobinfo/pageList`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobinfo/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobinfo/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobinfo/delete`, { 'ids[]': [id] }),
  start: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobinfo/start`, { 'ids[]': [id] }),
  stop: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobinfo/stop`, { 'ids[]': [id] }),
  trigger: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/jobinfo/trigger`, payload),
  nextTriggerTime: (payload?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/jobinfo/nextTriggerTime`, { params: payload }),
};

export const jobLogApi = {
  pageList: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/joblog/pageList`, { params }),
  kill: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/joblog/logKill`, { id }),
  clear: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/joblog/clearLog`, payload),
  detailCat: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/joblog/logDetailCat`, payload),
};

export const userApi = {
  pageList: (params?: Record<string, unknown>) =>
    http.get<AppResponse>(`${manageApiPrefix}/user/pageList`, { params }),
  create: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/insert`, payload),
  update: (payload: FormBodyRecord) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/update`, payload),
  remove: (id: number | string) =>
    http.formPost<AppResponse>(`${manageApiPrefix}/user/delete`, { 'ids[]': [id] }),
};
