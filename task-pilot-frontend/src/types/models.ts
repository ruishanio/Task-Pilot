export interface Option {
  label: string;
  value: string | number;
}
export interface Executor {
  id: number;
  appname: string;
  title: string;
  addressType: number;
  addressList: string;
  registryList?: string[];
  updateTime?: string;
}
export interface Task {
  id: number;
  executorId: number;
  taskName: string;
  taskDesc: string;
  author: string;
  alarmEmail: string;
  scheduleType: string;
  scheduleConf: string;
  glueType: string;
  executorHandler: string;
  executorParam: string;
  executorRouteStrategy: string;
  childTaskId: string;
  misfireStrategy: string;
  executorBlockStrategy: string;
  executorTimeout: number;
  executorFailRetryCount: number;
  glueRemark: string;
  glueSource: string;
  triggerStatus: number;
  triggerNextTime?: number | string;
}
export interface TaskMeta {
  executors: Pick<Executor, 'id' | 'title'>[];
  selectedExecutorId: number;
  triggerStatusOptions: Option[];
  scheduleTypeOptions: Option[];
  glueTypeOptions: Option[];
  executorRouteStrategyOptions: Option[];
  executorBlockStrategyOptions: Option[];
  misfireStrategyOptions: Option[];
}
export interface TaskLog {
  id: number;
  taskId: number;
  triggerTime?: string | number;
  triggerCode: number;
  handleCode: number;
  executorAddress?: string;
  executorHandler?: string;
  triggerMsg?: string;
  handleMsg?: string;
}
export interface LogMeta {
  executors: Pick<Executor, 'id' | 'title'>[];
  tasks: { id: number; taskDesc: string }[];
  selectedExecutorId: number;
  selectedTaskId: number;
  logStatusOptions: Option[];
  clearLogOptions: Option[];
}
export interface LiveLog {
  fromLineNum: number;
  toLineNum: number;
  logContent?: string;
  end?: boolean;
}
export interface User {
  id: number;
  username: string;
  role: number;
  permission: string;
}
export interface UserMeta {
  executors: Pick<Executor, 'id' | 'title' | 'appname'>[];
  roleOptions: Option[];
}
export interface Dashboard {
  taskInfoCount: number;
  taskLogCount: number;
  taskLogSuccessCount: number;
  executorCount: number;
}
export interface ChartData {
  triggerDayList: string[];
  triggerDayCountSucList: number[];
  triggerDayCountFailList: number[];
  triggerDayCountRunningList: number[];
  triggerCountSucTotal: number;
  triggerCountFailTotal: number;
  triggerCountRunningTotal: number;
}
