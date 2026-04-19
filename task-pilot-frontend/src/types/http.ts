export interface AppResponse<T = unknown> {
  code: number;
  msg?: string;
  data?: T;
  [key: string]: unknown;
}

export interface AppError extends Error {
  code?: number;
  payload?: AppResponse<unknown>;
}

export type FormValue = string | number | boolean;

export type FormBodyValue =
  | FormValue
  | null
  | undefined
  | Array<FormValue | null | undefined>;

export type FormBodyRecord = Record<string, FormBodyValue>;
