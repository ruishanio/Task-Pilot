import type { TaskLog } from '@/types/models';
/** 触发失败时执行码仍为 0，必须先判断触发码，防止误报为运行中。 */
export function logStatus(row: Pick<TaskLog, 'triggerCode' | 'handleCode'>) {
  if (row.triggerCode !== 200)
    return { label: '触发失败', kind: 'failure' } as const;
  if (row.handleCode === 200)
    return { label: '执行成功', kind: 'success' } as const;
  if (row.handleCode > 0)
    return { label: '执行失败', kind: 'failure' } as const;
  return { label: '运行中', kind: 'running' } as const;
}
