// API module alias for Next consumers: @/lib/api/auth
import {
  loginWithGoogle as postGoogleLogin,
  logoutAuth as postLogoutAuth,
  refreshAuthToken as postRefreshAuthToken,
} from "./api/auth.ts";

export const AUTH_ACCESS_TOKEN_KEY = "aquila-office.auth.accessToken";
export const AUTH_REFRESH_TOKEN_KEY = "aquila-office.auth.refreshToken";
export const AUTH_USER_KEY = "aquila-office.auth.user";
export const AUTH_STORAGE_KEYS: readonly string[] = [
  AUTH_ACCESS_TOKEN_KEY,
  AUTH_REFRESH_TOKEN_KEY,
  AUTH_USER_KEY,
];

const LEGACY_SESSION_KEY = "aquila-office.auth";

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
export type AuthUser = Pick<AuthTokens, "memberId" | "name" | "email" | "role">;

const DEV_ACCESS_TOKEN = process.env.NEXT_PUBLIC_DEV_ACCESS_TOKEN ?? "";

export async function loginWithGoogle(idToken: string): Promise<AuthTokens> {
  const tokens = await postGoogleLogin(idToken);
  saveAuthTokens(tokens);
  return tokens;
}

export async function refreshAuthToken(refreshToken: string): Promise<RefreshedAuthTokens> {
  return postRefreshAuthToken(refreshToken);
}

export async function logoutAuth(refreshToken: string): Promise<void> {
  await postLogoutAuth(refreshToken);
}

export async function logout(): Promise<void> {
  const tokens = readAuthTokens();
  try {
    if (tokens) {
      await logoutAuth(tokens.refreshToken);
    }
  } catch {
    // 로그아웃 API 실패와 무관하게 브라우저 세션은 즉시 정리한다.
  } finally {
    clearAuthTokens();
  }
}

export function readAuthTokens(storage: Storage | undefined = browserStorage()): AuthTokens | null {
  if (!storage) {
    return null;
  }

  const accessToken = storage.getItem(AUTH_ACCESS_TOKEN_KEY);
  const refreshToken = storage.getItem(AUTH_REFRESH_TOKEN_KEY);
  const user = readAuthUser(storage);
  if (accessToken && refreshToken && user) {
    return {
      ...user,
      accessToken,
      refreshToken,
    };
  }

  return readLegacyAuthTokens(storage);
}

export function readAuthUser(storage: Storage | undefined = browserStorage()): AuthUser | null {
  if (!storage) {
    return null;
  }

  const rawUser = storage.getItem(AUTH_USER_KEY);
  const user = parseJson(rawUser);
  if (isAuthUser(user)) {
    return user;
  }

  const legacyTokens = readLegacyAuthTokens(storage);
  return legacyTokens ? toAuthUser(legacyTokens) : null;
}

export function saveAuthTokens(
  tokens: AuthTokens,
  storage: Storage | undefined = browserStorage(),
): void {
  if (!storage) {
    return;
  }

  storage.setItem(AUTH_ACCESS_TOKEN_KEY, tokens.accessToken);
  storage.setItem(AUTH_REFRESH_TOKEN_KEY, tokens.refreshToken);
  storage.setItem(AUTH_USER_KEY, JSON.stringify(toAuthUser(tokens)));
  storage.removeItem(LEGACY_SESSION_KEY);
}

export function clearAuthTokens(storage: Storage | undefined = browserStorage()): void {
  storage?.removeItem(AUTH_ACCESS_TOKEN_KEY);
  storage?.removeItem(AUTH_REFRESH_TOKEN_KEY);
  storage?.removeItem(AUTH_USER_KEY);
  storage?.removeItem(LEGACY_SESSION_KEY);
}

export function resolveAccessToken(storage: Storage | undefined = browserStorage()): string {
  if (!storage) {
    return DEV_ACCESS_TOKEN;
  }

  return storage.getItem(AUTH_ACCESS_TOKEN_KEY) ?? readLegacyAuthTokens(storage)?.accessToken ?? DEV_ACCESS_TOKEN;
}

export function isAuthenticated(storage: Storage | undefined = browserStorage()): boolean {
  if (!storage) {
    return false;
  }

  return Boolean(storage.getItem(AUTH_ACCESS_TOKEN_KEY) ?? readLegacyAuthTokens(storage)?.accessToken);
}

function browserStorage(): Storage | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }

  return window.localStorage;
}

function readLegacyAuthTokens(storage: Storage): AuthTokens | null {
  const rawValue = storage.getItem(LEGACY_SESSION_KEY);
  const parsed = parseJson(rawValue);
  return isAuthTokens(parsed) ? parsed : null;
}

function parseJson(value: string | null): unknown {
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

function toAuthUser(tokens: AuthTokens): AuthUser {
  return {
    memberId: tokens.memberId,
    name: tokens.name,
    email: tokens.email,
    role: tokens.role,
  };
}

function isAuthUser(value: unknown): value is AuthUser {
  if (!value || typeof value !== "object") {
    return false;
  }

  const record = value as Record<string, unknown>;
  return (
    typeof record.memberId === "number" &&
    typeof record.name === "string" &&
    typeof record.email === "string" &&
    (record.role === "ADMIN" || record.role === "USER")
  );
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
