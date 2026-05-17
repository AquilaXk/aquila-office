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
