export type ApiEnvelope<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export type ApiFetchOptions = RequestInit & {
  accessToken?: string;
  baseUrl?: string;
  json?: unknown;
  skipAuthRecovery?: boolean;
};
