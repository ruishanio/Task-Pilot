import dayjs from 'dayjs';

export function formatDateTime(value, fallback = '-') {
  if (value === undefined || value === null || value === '') {
    return fallback;
  }

  const time = typeof value === 'number' ? dayjs(value) : dayjs(String(value));
  return time.isValid() ? time.format('YYYY-MM-DD HH:mm:ss') : fallback;
}

export function parsePagePayload(payload) {
  return {
    list: payload?.data?.data || [],
    total: payload?.data?.total || 0,
  };
}

export function getErrorMessage(error, fallback = '请求失败，请稍后再试') {
  return error?.payload?.msg || error?.message || fallback;
}

export function joinPermissions(values) {
  return Array.isArray(values) ? values.join(',') : values || '';
}

export function parsePermissions(value) {
  if (!value) {
    return [];
  }
  return String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}
