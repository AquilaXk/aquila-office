import { apiFetch } from "./index.ts";
import type { AuthTokens, RefreshedAuthTokens } from "../auth.ts";

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
