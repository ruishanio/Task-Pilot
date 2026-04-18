import http from './http';

const manageApiPrefix = '/api/manage';

export const frontendApi = {
  bootstrap: () => http.get(`${manageApiPrefix}/frontend/bootstrap`),
  dashboard: () => http.get(`${manageApiPrefix}/frontend/dashboard`),
  jobInfoMeta: (params) => http.get(`${manageApiPrefix}/frontend/jobinfo/meta`, { params }),
  jobLogMeta: (params) => http.get(`${manageApiPrefix}/frontend/joblog/meta`, { params }),
  userMeta: () => http.get(`${manageApiPrefix}/frontend/user/meta`),
  chartInfo: (params) => http.formPost(`${manageApiPrefix}/chartInfo`, params),
};

export const authApi = {
  login: (payload) => http.formPost(`${manageApiPrefix}/auth/doLogin`, payload),
  logout: () => http.formPost(`${manageApiPrefix}/auth/logout`),
  updatePassword: (payload) => http.formPost(`${manageApiPrefix}/auth/updatePwd`, payload),
};

export const jobGroupApi = {
  pageList: (params) => http.get(`${manageApiPrefix}/jobgroup/pageList`, { params }),
  create: (payload) => http.formPost(`${manageApiPrefix}/jobgroup/insert`, payload),
  update: (payload) => http.formPost(`${manageApiPrefix}/jobgroup/update`, payload),
  remove: (id) => http.formPost(`${manageApiPrefix}/jobgroup/delete`, { 'ids[]': [id] }),
  loadById: (id) => http.get(`${manageApiPrefix}/jobgroup/loadById`, { params: { id } }),
};

export const jobInfoApi = {
  pageList: (params) => http.get(`${manageApiPrefix}/jobinfo/pageList`, { params }),
  create: (payload) => http.formPost(`${manageApiPrefix}/jobinfo/insert`, payload),
  update: (payload) => http.formPost(`${manageApiPrefix}/jobinfo/update`, payload),
  remove: (id) => http.formPost(`${manageApiPrefix}/jobinfo/delete`, { 'ids[]': [id] }),
  start: (id) => http.formPost(`${manageApiPrefix}/jobinfo/start`, { 'ids[]': [id] }),
  stop: (id) => http.formPost(`${manageApiPrefix}/jobinfo/stop`, { 'ids[]': [id] }),
  trigger: (payload) => http.formPost(`${manageApiPrefix}/jobinfo/trigger`, payload),
  nextTriggerTime: (payload) => http.get(`${manageApiPrefix}/jobinfo/nextTriggerTime`, { params: payload }),
};

export const jobLogApi = {
  pageList: (params) => http.get(`${manageApiPrefix}/joblog/pageList`, { params }),
  kill: (id) => http.formPost(`${manageApiPrefix}/joblog/logKill`, { id }),
  clear: (payload) => http.formPost(`${manageApiPrefix}/joblog/clearLog`, payload),
  detailCat: (payload) => http.formPost(`${manageApiPrefix}/joblog/logDetailCat`, payload),
};

export const userApi = {
  pageList: (params) => http.get(`${manageApiPrefix}/user/pageList`, { params }),
  create: (payload) => http.formPost(`${manageApiPrefix}/user/insert`, payload),
  update: (payload) => http.formPost(`${manageApiPrefix}/user/update`, payload),
  remove: (id) => http.formPost(`${manageApiPrefix}/user/delete`, { 'ids[]': [id] }),
};
