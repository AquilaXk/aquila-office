import { apiFetch } from "./api/index.ts";

export const AUTH_STORAGE_KEY = "aquila-office.auth";

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
  return apiFetch<T>(path, {
    method: "POST",
    json: payload,
  });
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
