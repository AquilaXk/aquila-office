import test from "node:test";
import assert from "node:assert/strict";
import { apiFetch } from "./api-client.ts";

test("apiFetch sends trimmed bearer access token and keeps custom headers", async () => {
  const originalFetch = globalThis.fetch;
  let capturedHeaders: Headers | null = null;

  globalThis.fetch = async (_input: RequestInfo | URL, init?: RequestInit) => {
    capturedHeaders = new Headers(init?.headers);
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
    assert.equal(capturedHeaders?.get("Authorization"), "Bearer aquila-access.token");
    assert.equal(capturedHeaders?.get("Content-Type"), "application/json");
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
