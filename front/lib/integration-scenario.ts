export type IntegrationScenarioStepId = "login" | "workspace" | "agent" | "task" | "report";

export type IntegrationScenarioReadiness = "empty" | "in-progress" | "ready";

export type IntegrationScenarioStep = {
  id: IntegrationScenarioStepId;
  label: string;
  required: boolean;
  route: string;
  apiPath: string;
};

export type IntegrationScenarioChecklistStep = IntegrationScenarioStep & {
  state: "pending" | "complete";
};

export type IntegrationScenarioChecklistInput = {
  hasAuthTokens: boolean;
  workspaceCount: number;
  agentCount: number;
  taskCount: number;
  reportCount: number;
};

export type IntegrationScenarioChecklist = {
  readiness: IntegrationScenarioReadiness;
  completedCount: number;
  totalCount: number;
  nextStepId: IntegrationScenarioStepId | null;
  steps: IntegrationScenarioChecklistStep[];
};

export const INTEGRATION_SCENARIO_STEPS = [
  {
    id: "login",
    label: "Login",
    required: true,
    route: "/auth",
    apiPath: "/api/v1/auth/google/login",
  },
  {
    id: "workspace",
    label: "Workspace",
    required: true,
    route: "/workspaces",
    apiPath: "/api/v1/workspaces",
  },
  {
    id: "agent",
    label: "Agent",
    required: true,
    route: "/workspaces/:workspaceId",
    apiPath: "/api/v1/workspaces/:workspaceId/agents",
  },
  {
    id: "task",
    label: "Task",
    required: true,
    route: "/workspaces/:workspaceId",
    apiPath: "/api/v1/workspaces/:workspaceId/tasks",
  },
  {
    id: "report",
    label: "Report",
    required: true,
    route: "/workspaces/:workspaceId",
    apiPath: "/api/v1/workspaces/:workspaceId/tasks/:taskId/reports",
  },
] as const satisfies readonly IntegrationScenarioStep[];

export function createIntegrationScenarioChecklist(
  input: IntegrationScenarioChecklistInput,
): IntegrationScenarioChecklist {
  const evidenceByStep: Record<IntegrationScenarioStepId, boolean> = {
    login: input.hasAuthTokens,
    workspace: input.workspaceCount > 0,
    agent: input.agentCount > 0,
    task: input.taskCount > 0,
    report: input.reportCount > 0,
  };
  const steps: IntegrationScenarioChecklistStep[] = INTEGRATION_SCENARIO_STEPS.map((step) => {
    const state: IntegrationScenarioChecklistStep["state"] = evidenceByStep[step.id]
      ? "complete"
      : "pending";

    return {
      ...step,
      state,
    };
  });
  const completedCount = steps.filter((step) => step.state === "complete").length;
  const nextStep = steps.find((step) => step.state === "pending");

  return {
    readiness: readinessFor(completedCount, steps.length),
    completedCount,
    totalCount: steps.length,
    nextStepId: nextStep?.id ?? null,
    steps,
  };
}

function readinessFor(completedCount: number, totalCount: number): IntegrationScenarioReadiness {
  if (completedCount === totalCount) {
    return "ready";
  }
  if (completedCount === 0) {
    return "empty";
  }
  return "in-progress";
}
