import test from "node:test";
import assert from "node:assert/strict";
import {
  AUTH_ACCESS_TOKEN_KEY,
  AUTH_USER_KEY,
  clearAuthTokens,
  loginWithGoogle,
  logoutAuth,
  readAuthTokens,
  readAuthUser,
  recoverAccessTokenAfterUnauthorized,
  refreshAuthToken,
  resolveAccessToken,
  saveAuthTokens,
  type AuthTokens,
} from "./auth.ts";
import { ApiError } from "./api-client.ts";

const authTokens: AuthTokens = {
  memberId: 1,
  name: "Aquila User",
  email: "aquila@example.com",
  role: "ADMIN",
  accessToken: "aquila-access.token",
};

test("loginWithGoogle posts id token and returns issued tokens", async () => {
  const originalFetch = globalThis.fetch;
  let capturedPath = "";
  let capturedBody: unknown = null;

  globalThis.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    capturedPath = String(input);
    capturedBody = JSON.parse(String(init?.body));
    return jsonResponse(authTokens);
  };

  try {
    const result = await loginWithGoogle(" google-1|aquila@example.com|Aquila User ");

    assert.equal(capturedPath, "http://localhost:8080/api/v1/auth/google/login");
    assert.deepEqual(capturedBody, {
      idToken: " google-1|aquila@example.com|Aquila User ",
    });
    assert.deepEqual(result, authTokens);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("refreshAuthToken and logoutAuth use the HttpOnly refresh cookie", async () => {
  const originalFetch = globalThis.fetch;
  const requests: Array<{ path: string; body: string | null; credentials: RequestCredentials | undefined }> = [];

  globalThis.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    requests.push({
      path: String(input),
      body: init?.body ? String(init.body) : null,
      credentials: init?.credentials,
    });

    if (String(input).endsWith("/api/v1/auth/logout")) {
      return jsonResponse(null);
    }

    return jsonResponse({
      accessToken: "aquila-access.next",
    });
  };

  try {
    const refreshed = await refreshAuthToken();
    await logoutAuth();

    assert.deepEqual(refreshed, {
      accessToken: "aquila-access.next",
    });
    assert.deepEqual(requests, [
      {
        path: "http://localhost:8080/api/v1/auth/token/refresh",
        body: null,
        credentials: "include",
      },
      {
        path: "http://localhost:8080/api/v1/auth/logout",
        body: null,
        credentials: "include",
      },
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("loginWithGoogle rejects failed auth envelope with ApiError", async () => {
  const originalFetch = globalThis.fetch;

  globalThis.fetch = async () =>
    new Response(
      JSON.stringify({
        success: false,
        code: "UNAUTHORIZED",
        message: "Google token invalid",
        data: null,
      }),
      {
        status: 401,
        headers: { "Content-Type": "application/json" },
      },
    );

  try {
    await assert.rejects(
      () => loginWithGoogle("invalid-token"),
      (error) =>
        error instanceof ApiError &&
        error.status === 401 &&
        error.message === "Google token invalid",
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("auth token storage saves reads resolves and clears tokens", () => {
  const storage = new MemoryStorage();
  const authUser = {
    memberId: authTokens.memberId,
    name: authTokens.name,
    email: authTokens.email,
    role: authTokens.role,
  };

  assert.equal(readAuthTokens(storage), null);
  assert.equal(readAuthUser(storage), null);
  assert.equal(resolveAccessToken(storage), "");

  saveAuthTokens(authTokens, storage);

  assert.equal(storage.getItem(AUTH_ACCESS_TOKEN_KEY), "aquila-access.token");
  assert.equal(storage.getItem("aquila-office.auth.refreshToken"), null);
  assert.deepEqual(JSON.parse(storage.getItem(AUTH_USER_KEY) ?? "{}"), authUser);
  assert.deepEqual(readAuthTokens(storage), authTokens);
  assert.deepEqual(readAuthUser(storage), authUser);
  assert.equal(resolveAccessToken(storage), "aquila-access.token");

  clearAuthTokens(storage);

  assert.equal(readAuthTokens(storage), null);
  assert.equal(readAuthUser(storage), null);
  assert.equal(storage.getItem(AUTH_ACCESS_TOKEN_KEY), null);
  assert.equal(storage.getItem("aquila-office.auth.refreshToken"), null);
  assert.equal(storage.getItem(AUTH_USER_KEY), null);
});

test("recoverAccessTokenAfterUnauthorized refreshes and stores the next access token", async () => {
  const originalFetch = globalThis.fetch;
  const storage = new MemoryStorage();
  saveAuthTokens(authTokens, storage);

  globalThis.fetch = async () =>
    jsonResponse({
      accessToken: "aquila-access.next",
    });

  try {
    const recoveredAccessToken = await recoverAccessTokenAfterUnauthorized(storage);

    assert.equal(recoveredAccessToken, "aquila-access.next");
    assert.equal(storage.getItem(AUTH_ACCESS_TOKEN_KEY), "aquila-access.next");
    assert.deepEqual(readAuthTokens(storage), {
      ...authTokens,
      accessToken: "aquila-access.next",
    });
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("recoverAccessTokenAfterUnauthorized clears browser session when refresh fails", async () => {
  const originalFetch = globalThis.fetch;
  const storage = new MemoryStorage();
  saveAuthTokens(authTokens, storage);

  globalThis.fetch = async () =>
    new Response(
      JSON.stringify({
        success: false,
        code: "401",
        message: "Unauthorized",
        data: null,
      }),
      {
        status: 401,
        headers: { "Content-Type": "application/json" },
      },
    );

  try {
    const recoveredAccessToken = await recoverAccessTokenAfterUnauthorized(storage);

    assert.equal(recoveredAccessToken, null);
    assert.equal(readAuthTokens(storage), null);
    assert.equal(storage.getItem(AUTH_ACCESS_TOKEN_KEY), null);
    assert.equal(storage.getItem(AUTH_USER_KEY), null);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

function jsonResponse(data: unknown): Response {
  return new Response(
    JSON.stringify({
      success: true,
      code: "OK",
      message: "ok",
      data,
    }),
    {
      status: 200,
      headers: { "Content-Type": "application/json" },
    },
  );
}

class MemoryStorage implements Storage {
  private readonly values = new Map<string, string>();

  get length(): number {
    return this.values.size;
  }

  clear(): void {
    this.values.clear();
  }

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  key(index: number): string | null {
    return Array.from(this.values.keys())[index] ?? null;
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }
}
