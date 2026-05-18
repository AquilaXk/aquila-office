// API module alias for Next consumers: @/lib/api/auth
import {
  loginWithGoogle as postGoogleLogin,
  logoutAuth as postLogoutAuth,
  refreshAuthToken as postRefreshAuthToken,
} from "./api/auth.ts";

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
  return postGoogleLogin(idToken);
}

export async function refreshAuthToken(refreshToken: string): Promise<RefreshedAuthTokens> {
  return postRefreshAuthToken(refreshToken);
}

export async function logoutAuth(refreshToken: string): Promise<void> {
  await postLogoutAuth(refreshToken);
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

export function isAuthenticated(storage: Storage | undefined = browserStorage()): boolean {
  return Boolean(readAuthTokens(storage)?.accessToken);
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
