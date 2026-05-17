export const AUTH_STORAGE_KEY = "aquila-office.auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type AuthMemberRole = "ADMIN" | "USER";

export type AuthTokens = {
  memberId: number;
  name: string;
  email: string;
  role: AuthMemberRole;
  accessToken: string;
  refreshToken: string;
};

export type RefreshedAuthTokens = Pick<AuthTokens, "accessToken" | "refreshToken">;

const DEV_ACCESS_TOKEN = process.env.NEXT_PUBLIC_DEV_ACCESS_TOKEN ?? "";

export async function loginWithGoogle(idToken: string): Promise<AuthTokens> {
  return authPost<AuthTokens>("/api/v1/auth/google/login", { idToken });
}

export async function refreshAuthToken(refreshToken: string): Promise<RefreshedAuthTokens> {
  return authPost<RefreshedAuthTokens>("/api/v1/auth/token/refresh", { refreshToken });
}

export async function logoutAuth(refreshToken: string): Promise<void> {
  await authPost<void>("/api/v1/auth/logout", { refreshToken });
}

async function authPost<T>(path: string, payload: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
  const body = await response.json();

  if (!response.ok) {
    throw new Error(messageFromEnvelope(body, "인증 요청을 처리하지 못했습니다."));
  }

  if (isApiEnvelope<T>(body)) {
    if (!body.success) {
      throw new Error(body.message);
    }

    return body.data;
  }

  return body as T;
}

export function readAuthTokens(storage: Storage | undefined = browserStorage()): AuthTokens | null {
  if (!storage) {
    return null;
  }

  const rawValue = storage.getItem(AUTH_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawValue);
    return isAuthTokens(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

export function saveAuthTokens(
  tokens: AuthTokens,
  storage: Storage | undefined = browserStorage(),
): void {
  if (!storage) {
    return;
  }

  storage.setItem(AUTH_STORAGE_KEY, JSON.stringify(tokens));
}

export function clearAuthTokens(storage: Storage | undefined = browserStorage()): void {
  storage?.removeItem(AUTH_STORAGE_KEY);
}

export function resolveAccessToken(storage: Storage | undefined = browserStorage()): string {
  return readAuthTokens(storage)?.accessToken ?? DEV_ACCESS_TOKEN;
}

function browserStorage(): Storage | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }

  return window.localStorage;
}

type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};

function isApiEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  return (
    typeof value === "object" &&
    value !== null &&
    "success" in value &&
    "message" in value &&
    "data" in value
  );
}

function messageFromEnvelope(value: unknown, fallback: string): string {
  if (typeof value === "object" && value !== null && "message" in value) {
    const message = (value as { message?: unknown }).message;
    if (typeof message === "string" && message.length > 0) {
      return message;
    }
  }

  return fallback;
}

function isAuthTokens(value: unknown): value is AuthTokens {
  if (!value || typeof value !== "object") {
    return false;
  }

  const record = value as Record<string, unknown>;
  return (
    typeof record.memberId === "number" &&
    typeof record.name === "string" &&
    typeof record.email === "string" &&
    (record.role === "ADMIN" || record.role === "USER") &&
    typeof record.accessToken === "string" &&
    record.accessToken.length > 0 &&
    typeof record.refreshToken === "string" &&
    record.refreshToken.length > 0
  );
}
