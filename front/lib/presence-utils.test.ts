import assert from "node:assert/strict";
import test from "node:test";
import {
  MAX_PRESENCE_MEMBERS,
  MAX_SSE_EVENT_CHARS,
  createPresencePositionScheduler,
  issueWorkspacePresenceStreamTicket,
  openTicketedWorkspacePresenceStream,
  parseWorkspacePresenceMessage,
  sanitizeWorkspacePresencePosition,
  workspacePresenceStreamPath,
  workspacePresenceStreamTicketPath,
} from "./presence.ts";

test("presence parser enforces MAX_SSE_EVENT_CHARS, MAX_PRESENCE_MEMBERS, and 0..100 coordinates", () => {
  assert.equal(MAX_SSE_EVENT_CHARS, 65536);
  assert.equal(MAX_PRESENCE_MEMBERS, 500);

  // Oversized SSE payload fails closed
  const huge = JSON.stringify({
    type: "presence.position",
    workspaceId: 1,
    memberId: 2,
    position: { left: "50%", top: "50%" },
    status: "ACTIVE",
    members: [],
    message: "x".repeat(MAX_SSE_EVENT_CHARS),
  });
  assert.ok(huge.length > MAX_SSE_EVENT_CHARS);
  assert.equal(parseWorkspacePresenceMessage(huge), null);

  // Too many snapshot members fails closed
  const tooManyMembers = Array.from({ length: MAX_PRESENCE_MEMBERS + 1 }, (_, i) => ({
    memberId: i + 1,
    position: { left: "50%", top: "50%" },
    status: "ACTIVE",
  }));
  assert.equal(
    parseWorkspacePresenceMessage(
      JSON.stringify({
        type: "presence.snapshot",
        workspaceId: 1,
        memberId: null,
        position: null,
        status: null,
        members: tooManyMembers,
        message: null,
      }),
    ),
    null,
  );

  // Non-numeric / out-of-range coordinates rejected fail-closed
  assert.equal(
    sanitizeWorkspacePresencePosition({ left: "workspace-detail", top: "task-board" }),
    null,
  );
  assert.equal(sanitizeWorkspacePresencePosition({ left: "-0.1%", top: "50%" }), null);
  assert.equal(sanitizeWorkspacePresencePosition({ left: "50%", top: "100.5%" }), null);
  assert.equal(sanitizeWorkspacePresencePosition({ left: "NaN%", top: "50%" }), null);
  assert.deepEqual(sanitizeWorkspacePresencePosition({ left: "36.8%", top: "54%" }), {
    left: "36.8%",
    top: "54%",
  });

  // Malformed presence.position coordinates rejected cleanly
  assert.equal(
    parseWorkspacePresenceMessage(
      JSON.stringify({
        type: "presence.position",
        workspaceId: 1,
        memberId: 5,
        position: { left: "workspace-detail", top: "task-board" },
        status: "ACTIVE",
        members: [],
        message: null,
      }),
    ),
    null,
  );

  const validParsed = parseWorkspacePresenceMessage(
    JSON.stringify({
      type: "presence.position",
      workspaceId: 1,
      memberId: 5,
      position: { left: "36.8%", top: "54%" },
      status: "ACTIVE",
      members: [],
      message: null,
    }),
  );
  assert.notEqual(validParsed, null);
  assert.deepEqual(validParsed?.position, { left: "36.8%", top: "54%" });
});

test("createPresencePositionScheduler coalesces bursts and flushes trailing resting coordinate", async () => {
  const sent: Array<{ left: string; top: string }> = [];
  let releaseFirst!: () => void;
  let callIndex = 0;

  const scheduler = createPresencePositionScheduler(async (pos) => {
    callIndex += 1;
    sent.push(pos);
    if (callIndex === 1) {
      await new Promise<void>((resolve) => {
        releaseFirst = resolve;
      });
    }
  });

  const p1 = scheduler.schedule({ left: "10%", top: "10%" });
  const p2 = scheduler.schedule({ left: "20%", top: "20%" });
  const p3 = scheduler.schedule({ left: "35.5%", top: "48%" });

  assert.equal(sent.length, 1);
  assert.deepEqual(sent[0], { left: "10%", top: "10%" });

  releaseFirst();
  await Promise.all([p1, p2, p3]);

  assert.equal(sent.length, 2);
  assert.deepEqual(sent[1], { left: "35.5%", top: "48%" });
});

test("presence stream ticket issues short-lived ticket and falls back to accessToken only on 404/405", async () => {
  assert.equal(
    workspacePresenceStreamTicketPath(101),
    "/api/v1/workspaces/101/presence/stream-ticket",
  );
  assert.equal(
    workspacePresenceStreamPath(101, { ticket: "sse-ticket-1" }),
    "/api/v1/workspaces/101/presence/stream?ticket=sse-ticket-1",
  );
  assert.equal(
    workspacePresenceStreamPath(101, "tok-legacy"),
    "/api/v1/workspaces/101/presence/stream?accessToken=tok-legacy",
  );

  const originalFetch = globalThis.fetch;
  const originalEventSource = (globalThis as { EventSource?: unknown }).EventSource;
  const openedUrls: string[] = [];

  class FakeEventSource {
    url: string;
    constructor(url: string) {
      this.url = url;
      openedUrls.push(url);
    }
  }
  (globalThis as { EventSource?: unknown }).EventSource = FakeEventSource;

  try {
    // 1. Ticket success -> opens /stream?ticket=...
    globalThis.fetch = async () =>
      new Response(
        JSON.stringify({
          success: true,
          code: "OK",
          message: "ok",
          data: { ticket: "ticket-abc", expiresInSeconds: 60 },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );

    await openTicketedWorkspacePresenceStream(101, "secret-jwt");
    assert.ok(openedUrls[0]?.endsWith("/api/v1/workspaces/101/presence/stream?ticket=ticket-abc"));

    // 2. Empty ticket fails closed
    globalThis.fetch = async () =>
      new Response(
        JSON.stringify({
          success: true,
          code: "OK",
          message: "ok",
          data: { ticket: "   ", expiresInSeconds: 60 },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    await assert.rejects(() => issueWorkspacePresenceStreamTicket("secret-jwt", 101), RangeError);

    // 3. 404 legacy mock -> fallback to ?accessToken=
    globalThis.fetch = async () =>
      new Response(JSON.stringify({ message: "Not found" }), {
        status: 404,
        headers: { "Content-Type": "application/json" },
      });
    await openTicketedWorkspacePresenceStream(101, "secret-jwt");
    assert.ok(openedUrls[1]?.endsWith("/api/v1/workspaces/101/presence/stream?accessToken=secret-jwt"));

    // 4. 401/500 must fail closed (no accessToken URL fallback)
    globalThis.fetch = async () =>
      new Response(JSON.stringify({ message: "Unauthorized" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      });
    await assert.rejects(() => openTicketedWorkspacePresenceStream(101, "secret-jwt"));
    assert.equal(openedUrls.length, 2);
  } finally {
    globalThis.fetch = originalFetch;
    if (originalEventSource === undefined) {
      delete (globalThis as { EventSource?: unknown }).EventSource;
    } else {
      (globalThis as { EventSource?: unknown }).EventSource = originalEventSource;
    }
  }
});
