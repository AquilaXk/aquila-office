import assert from "node:assert/strict";
import test from "node:test";
import {
  MAX_ORCHESTRATION_DEPENDS_ON_SERIALIZED_CHARS,
  MAX_ORCHESTRATION_INSTRUCTION_CHARS,
  MAX_ORCHESTRATION_STEPS,
  MAX_ORCHESTRATION_STEP_KEY_CHARS,
  createOrchestrationSession,
  dispatchOrchestrationSession,
  fetchOrchestrationSession,
  fetchOrchestrationSessions,
  normalizeOrchestrationSessions,
  validateCreateOrchestrationInput,
  workspaceOrchestrationDispatchPath,
  workspaceOrchestrationPath,
  workspaceOrchestrationsPath,
  type OrchestrationSession,
} from "./orchestrations.ts";

const sampleSession: OrchestrationSession = {
  sessionId: 77,
  workspaceId: 101,
  sourceTaskId: 301,
  status: "PLANNED",
  createdAt: "2026-11-21T10:00:00Z",
  updatedAt: "2026-11-21T10:00:00Z",
  steps: [
    {
      stepKey: "plan",
      instruction: "설계 문서를 작성하라",
      agentId: 501,
      taskId: null,
      dependsOn: [],
      status: "PENDING",
    },
    {
      stepKey: "verify",
      instruction: "검증 테스트를 실행하라",
      agentId: 502,
      taskId: null,
      dependsOn: ["plan"],
      status: "PENDING",
    },
  ],
};

test("orchestration path helpers target workspace orchestration endpoints", () => {
  assert.equal(workspaceOrchestrationsPath(101), "/api/v1/workspaces/101/orchestrations");
  assert.equal(workspaceOrchestrationPath(101, 77), "/api/v1/workspaces/101/orchestrations/77");
  assert.equal(
    workspaceOrchestrationDispatchPath(101, 77),
    "/api/v1/workspaces/101/orchestrations/77/dispatch",
  );
});

test("validateCreateOrchestrationInput enforces Fail-Closed bounds and detects DAG cycles", () => {
  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 0,
        steps: [{ stepKey: "s1", instruction: "run", agentId: 1 }],
      }),
    RangeError,
  );

  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 301,
        steps: [],
      }),
    RangeError,
  );

  const tooManySteps = Array.from({ length: MAX_ORCHESTRATION_STEPS + 1 }, (_, i) => ({
    stepKey: `step_${i}`,
    instruction: "work",
    agentId: 1,
  }));
  assert.throws(
    () => validateCreateOrchestrationInput({ sourceTaskId: 301, steps: tooManySteps }),
    RangeError,
  );

  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 301,
        steps: [
          {
            stepKey: "k".repeat(MAX_ORCHESTRATION_STEP_KEY_CHARS + 1),
            instruction: "work",
            agentId: 1,
          },
        ],
      }),
    RangeError,
  );

  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 301,
        steps: [
          {
            stepKey: "step-1",
            instruction: "x".repeat(MAX_ORCHESTRATION_INSTRUCTION_CHARS + 1),
            agentId: 1,
          },
        ],
      }),
    RangeError,
  );

  assert.equal(MAX_ORCHESTRATION_INSTRUCTION_CHARS, 1000);
  assert.equal(MAX_ORCHESTRATION_DEPENDS_ON_SERIALIZED_CHARS, 1000);

  const thirteenDepsSteps = Array.from({ length: 13 }, (_, i) => {
    const prefix = `s${String(i).padStart(2, "0")}_`;
    return {
      stepKey: `${prefix}${"k".repeat(MAX_ORCHESTRATION_STEP_KEY_CHARS - prefix.length)}`,
      instruction: "work",
      agentId: 1,
    };
  });
  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 301,
        steps: [
          ...thirteenDepsSteps,
          {
            stepKey: "join",
            instruction: "combine",
            agentId: 1,
            dependsOn: thirteenDepsSteps.map((s) => s.stepKey),
          },
        ],
      }),
    RangeError,
  );

  // Unknown dependency
  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 301,
        steps: [{ stepKey: "a", instruction: "work", agentId: 1, dependsOn: ["missing"] }],
      }),
    RangeError,
  );

  // Self dependency
  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 301,
        steps: [{ stepKey: "a", instruction: "work", agentId: 1, dependsOn: ["a"] }],
      }),
    RangeError,
  );

  // Cycle a -> b -> a
  assert.throws(
    () =>
      validateCreateOrchestrationInput({
        sourceTaskId: 301,
        steps: [
          { stepKey: "a", instruction: "work a", agentId: 1, dependsOn: ["b"] },
          { stepKey: "b", instruction: "work b", agentId: 2, dependsOn: ["a"] },
        ],
      }),
    RangeError,
  );

  const valid = validateCreateOrchestrationInput({
    sourceTaskId: 301,
    steps: [
      { stepKey: " plan ", instruction: " 설계 ", agentId: 501, dependsOn: [] },
      { stepKey: " verify ", instruction: " 검증 ", agentId: 502, dependsOn: [" plan "] },
    ],
  });
  assert.deepEqual(valid.steps[1].dependsOn, ["plan"]);
});

test("orchestration client calls fetch/create/dispatch endpoints with bearer token", async () => {
  const originalFetch = globalThis.fetch;
  const requests: Array<{ url: string; method: string; body: string | null; auth: string | null }> = [];

  globalThis.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    requests.push({
      url,
      method,
      body: init?.body ? String(init.body) : null,
      auth: new Headers(init?.headers).get("Authorization"),
    });

    if (url.endsWith("/orchestrations") && method === "GET") {
      return jsonResponse([sampleSession]);
    }
    if (url.endsWith("/orchestrations/77") && method === "GET") {
      return jsonResponse(sampleSession);
    }
    if (url.endsWith("/orchestrations") && method === "POST") {
      return jsonResponse(sampleSession);
    }
    return jsonResponse({
      sessionId: 77,
      workspaceId: 101,
      sourceTaskId: 301,
      status: "RUNNING",
      dispatchedCount: 1,
      steps: sampleSession.steps,
    });
  };

  try {
    const listed = await fetchOrchestrationSessions("tok", 101);
    assert.equal(listed.length, 1);

    const detail = await fetchOrchestrationSession("tok", 101, 77);
    assert.equal(detail.sessionId, 77);

    const created = await createOrchestrationSession("tok", 101, {
      sourceTaskId: 301,
      steps: [{ stepKey: "plan", instruction: "설계 문서를 작성하라", agentId: 501 }],
    });
    assert.equal(created.status, "PLANNED");

    const dispatched = await dispatchOrchestrationSession("tok", 101, 77, { createPr: false });
    assert.equal(dispatched.dispatchedCount, 1);
    assert.equal(requests[3].method, "POST");
    assert.equal(requests[3].auth, "Bearer tok");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("normalizeOrchestrationSessions handles direct arrays, content wrappers, and invalid payloads", () => {
  assert.deepEqual(normalizeOrchestrationSessions([sampleSession]), [sampleSession]);
  assert.deepEqual(normalizeOrchestrationSessions({ content: [sampleSession] }), [sampleSession]);
  assert.deepEqual(normalizeOrchestrationSessions({ sessions: [sampleSession] }), [sampleSession]);
  assert.deepEqual(normalizeOrchestrationSessions(null), []);
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
