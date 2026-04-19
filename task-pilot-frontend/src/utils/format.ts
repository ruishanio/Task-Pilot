import dayjs from 'dayjs';
import type { AppError, AppResponse } from '../types/http';

export function formatDateTime(value: unknown, fallback = '-'): string {
  if (value === undefined || value === null || value === '') {
    return fallback;
  }

  const time = typeof value === 'number' ? dayjs(value) : dayjs(String(value));
  return time.isValid() ? time.format('YYYY-MM-DD HH:mm:ss') : fallback;
}

export function parsePagePayload<T = Record<string, unknown>>(
  payload?: AppResponse<{
    data?: T[];
    total?: number;
  }>,
): { list: T[]; total: number } {
  return {
    list: payload?.data?.data || [],
    total: payload?.data?.total || 0,
  };
}

export function getErrorMessage(error: unknown, fallback = '请求失败，请稍后再试'): string {
  const appError = error as Partial<AppError>;
  return appError?.payload?.msg || appError?.message || fallback;
}

export function joinPermissions(values?: string[] | string | null): string {
  return Array.isArray(values) ? values.join(',') : values || '';
}

export function parsePermissions(value: unknown): string[] {
  if (!value) {
    return [];
  }
  return String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}
