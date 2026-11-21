import test from "node:test";
import assert from "node:assert/strict";
import { ApiError, MAX_JSON_RESPONSE_CHARS, apiFetch, configureUnauthorizedRecovery } from "./api-client.ts";

test("apiFetch sends trimmed bearer access token and keeps custom headers", async () => {
  const originalFetch = globalThis.fetch;
  const captured: { headers: Headers | null } = { headers: null };

  globalThis.fetch = async (_input: RequestInfo | URL, init?: RequestInit) => {
    captured.headers = new Headers(init?.headers);
    return new Response(
      JSON.stringify({
        success: true,
        code: "OK",
        message: "ok",
        data: { ok: true },
      }),
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      },
    );
  };

  try {
    const data = await apiFetch<{ ok: boolean }>("/api/v1/workspaces", {
      accessToken: " aquila-access.token ",
      headers: { "Content-Type": "application/json" },
    });

    assert.deepEqual(data, { ok: true });
    assert.equal(captured.headers?.get("Authorization"), "Bearer aquila-access.token");
    assert.equal(captured.headers?.get("Content-Type"), "application/json");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("apiFetch serializes json payload and sets json content headers", async () => {
  const originalFetch = globalThis.fetch;
  let capturedInit: RequestInit | undefined;

  globalThis.fetch = async (_input: RequestInfo | URL, init?: RequestInit) => {
    capturedInit = init;
    return new Response(
      JSON.stringify({
        success: true,
        code: "OK",
        message: "ok",
        data: { workspaceId: 1 },
      }),
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      },
    );
  };

  try {
    const data = await apiFetch<{ workspaceId: number }>("/api/v1/workspaces", {
      method: "POST",
      accessToken: " aquila-access.token ",
      headers: { "X-Trace-Id": "front-api-client" },
      json: { name: "Aquila Office" },
    });

    const headers = new Headers(capturedInit?.headers);
    assert.deepEqual(data, { workspaceId: 1 });
    assert.equal(capturedInit?.body, JSON.stringify({ name: "Aquila Office" }));
    assert.equal(headers.get("Accept"), "application/json");
    assert.equal(headers.get("Content-Type"), "application/json");
    assert.equal(headers.get("Authorization"), "Bearer aquila-access.token");
    assert.equal(headers.get("X-Trace-Id"), "front-api-client");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("apiFetch refreshes access token once on unauthorized response and retries request", async () => {
  const originalFetch = globalThis.fetch;
  const authorizationHeaders: Array<string | null> = [];
  let recoveredAccessToken: string | null = null;

  configureUnauthorizedRecovery(async (failedAccessToken) => {
    recoveredAccessToken = failedAccessToken;
    return "aquila-access.next";
  });

  globalThis.fetch = async (_input: RequestInfo | URL, init?: RequestInit) => {
    authorizationHeaders.push(new Headers(init?.headers).get("Authorization"));
    if (authorizationHeaders.length === 1) {
      return new Response(
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
    }

    return new Response(
      JSON.stringify({
        success: true,
        code: "OK",
        message: "ok",
        data: { ok: true },
      }),
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      },
    );
  };

  try {
    const data = await apiFetch<{ ok: boolean }>("/api/v1/workspaces", {
      accessToken: "aquila-access.old",
    });

    assert.deepEqual(data, { ok: true });
    assert.equal(recoveredAccessToken, "aquila-access.old");
    assert.deepEqual(authorizationHeaders, [
      "Bearer aquila-access.old",
      "Bearer aquila-access.next",
    ]);
  } finally {
    configureUnauthorizedRecovery(null);
    globalThis.fetch = originalFetch;
  }
});

test("apiFetch fails closed when response exceeds MAX_JSON_RESPONSE_CHARS", async () => {
  assert.equal(MAX_JSON_RESPONSE_CHARS, 8 * 1024 * 1024);
  const originalFetch = globalThis.fetch;

  globalThis.fetch = async () => {
    const oversized = JSON.stringify({ data: "a".repeat(MAX_JSON_RESPONSE_CHARS + 64) });
    return new Response(oversized, {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  };

  try {
    await assert.rejects(
      () => apiFetch("/api/v1/workspaces/1/artifacts/file"),
      (err: unknown) => err instanceof ApiError,
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});
