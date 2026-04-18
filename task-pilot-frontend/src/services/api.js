import http from './http';

export const frontendApi = {
  bootstrap: () => http.get('/api/frontend/bootstrap'),
  dashboard: () => http.get('/api/frontend/dashboard'),
  jobInfoMeta: (params) => http.get('/api/frontend/jobinfo/meta', { params }),
  jobLogMeta: (params) => http.get('/api/frontend/joblog/meta', { params }),
  userMeta: () => http.get('/api/frontend/user/meta'),
  chartInfo: (params) => http.formPost('/chartInfo', params),
};

export const authApi = {
  login: (payload) => http.formPost('/auth/doLogin', payload),
  logout: () => http.formPost('/auth/logout'),
  updatePassword: (payload) => http.formPost('/auth/updatePwd', payload),
};

export const jobGroupApi = {
  pageList: (params) => http.get('/jobgroup/pageList', { params }),
  create: (payload) => http.formPost('/jobgroup/insert', payload),
  update: (payload) => http.formPost('/jobgroup/update', payload),
  remove: (id) => http.formPost('/jobgroup/delete', { 'ids[]': [id] }),
  loadById: (id) => http.get('/jobgroup/loadById', { params: { id } }),
};

export const jobInfoApi = {
  pageList: (params) => http.get('/jobinfo/pageList', { params }),
  create: (payload) => http.formPost('/jobinfo/insert', payload),
  update: (payload) => http.formPost('/jobinfo/update', payload),
  remove: (id) => http.formPost('/jobinfo/delete', { 'ids[]': [id] }),
  start: (id) => http.formPost('/jobinfo/start', { 'ids[]': [id] }),
  stop: (id) => http.formPost('/jobinfo/stop', { 'ids[]': [id] }),
  trigger: (payload) => http.formPost('/jobinfo/trigger', payload),
  nextTriggerTime: (payload) => http.get('/jobinfo/nextTriggerTime', { params: payload }),
};

export const jobLogApi = {
  pageList: (params) => http.get('/joblog/pageList', { params }),
  kill: (id) => http.formPost('/joblog/logKill', { id }),
  clear: (payload) => http.formPost('/joblog/clearLog', payload),
  detailCat: (payload) => http.formPost('/joblog/logDetailCat', payload),
};

export const userApi = {
  pageList: (params) => http.get('/user/pageList', { params }),
  create: (payload) => http.formPost('/user/insert', payload),
  update: (payload) => http.formPost('/user/update', payload),
  remove: (id) => http.formPost('/user/delete', { 'ids[]': [id] }),
};
