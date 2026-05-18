import assert from "node:assert/strict";
import test from "node:test";

async function loadScenarioModule(): Promise<{
  INTEGRATION_SCENARIO_STEPS: readonly { id: string; required: boolean }[];
  createIntegrationScenarioChecklist: (input: {
    hasAuthTokens: boolean;
    workspaceCount: number;
    agentCount: number;
    taskCount: number;
    reportCount: number;
  }) => {
    readiness: string;
    completedCount: number;
    totalCount: number;
    nextStepId: string | null;
    steps: readonly { id: string; state: string; route: string; apiPath: string }[];
  };
}> {
  try {
    return await import("./integration-scenario.ts");
  } catch (cause) {
    assert.fail(
      `integration scenario helper is not implemented: ${
        cause instanceof Error ? cause.message : String(cause)
      }`,
    );
  }
}

test("integration scenario exposes login to report journey in order", async () => {
  const scenario = await loadScenarioModule();

  assert.deepEqual(
    scenario.INTEGRATION_SCENARIO_STEPS.map((step) => [step.id, step.required]),
    [
      ["login", true],
      ["workspace", true],
      ["agent", true],
      ["task", true],
      ["report", true],
    ],
  );
});

test("integration scenario checklist marks completed steps and next action", async () => {
  const scenario = await loadScenarioModule();

  const checklist = scenario.createIntegrationScenarioChecklist({
    hasAuthTokens: true,
    workspaceCount: 1,
    agentCount: 1,
    taskCount: 1,
    reportCount: 0,
  });

  assert.equal(checklist.readiness, "in-progress");
  assert.equal(checklist.completedCount, 4);
  assert.equal(checklist.totalCount, 5);
  assert.equal(checklist.nextStepId, "report");
  assert.deepEqual(
    checklist.steps.map((step) => [step.id, step.state, step.route, step.apiPath]),
    [
      ["login", "complete", "/auth", "/api/v1/auth/google/login"],
      ["workspace", "complete", "/workspaces", "/api/v1/workspaces"],
      ["agent", "complete", "/workspaces/:workspaceId", "/api/v1/workspaces/:workspaceId/agents"],
      ["task", "complete", "/workspaces/:workspaceId", "/api/v1/workspaces/:workspaceId/tasks"],
      [
        "report",
        "pending",
        "/workspaces/:workspaceId",
        "/api/v1/workspaces/:workspaceId/tasks/:taskId/reports",
      ],
    ],
  );
});

test("integration scenario checklist reports ready only when the full journey has evidence", async () => {
  const scenario = await loadScenarioModule();

  const checklist = scenario.createIntegrationScenarioChecklist({
    hasAuthTokens: true,
    workspaceCount: 2,
    agentCount: 1,
    taskCount: 3,
    reportCount: 1,
  });

  assert.equal(checklist.readiness, "ready");
  assert.equal(checklist.completedCount, 5);
  assert.equal(checklist.nextStepId, null);
  assert(checklist.steps.every((step) => step.state === "complete"));
});
