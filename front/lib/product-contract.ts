export type ProductScenarioKey =
  | "auth.google-login"
  | "workspace.create"
  | "invite.accept"
  | "agent.create-sync"
  | "chat.send"
  | "artifact.step-read"
  | "gateway.failure"
  | "auth.expired"
  | "ui.empty-state";

export type ProductRecoverySignal = "GATEWAY_FAILURE" | "TOKEN_EXPIRED" | "EMPTY_STATE";

export type CriticalProductScenario = {
  key: ProductScenarioKey;
  route: string;
  apiPath?: string;
  method?: "GET" | "POST";
  recoverySignal?: ProductRecoverySignal;
};

export const CRITICAL_PRODUCT_SCENARIOS = [
  {
    key: "auth.google-login",
    route: "/auth",
    apiPath: "/api/v1/auth/google/login",
    method: "POST",
  },
  {
    key: "workspace.create",
    route: "/workspaces",
    apiPath: "/api/v1/workspaces",
    method: "POST",
  },
  {
    key: "invite.accept",
    route: "/invite/:token",
    apiPath: "/api/v1/invites/:token/accept",
    method: "POST",
  },
  {
    key: "agent.create-sync",
    route: "/workspaces/:workspaceId",
    apiPath: "/api/v1/workspaces/:workspaceId/agents",
    method: "POST",
  },
  {
    key: "chat.send",
    route: "/workspaces/:workspaceId",
    apiPath: "/api/v1/workspaces/:workspaceId/chat/messages",
    method: "POST",
  },
  {
    key: "artifact.step-read",
    route: "/workspaces/:workspaceId",
    apiPath: "/api/v1/workspaces/:workspaceId/artifacts/orchestration-plans/:planId/steps/:stepId",
    method: "GET",
  },
  {
    key: "gateway.failure",
    route: "/workspaces/:workspaceId/settings",
    recoverySignal: "GATEWAY_FAILURE",
  },
  {
    key: "auth.expired",
    route: "/auth",
    recoverySignal: "TOKEN_EXPIRED",
  },
  {
    key: "ui.empty-state",
    route: "/workspaces",
    recoverySignal: "EMPTY_STATE",
  },
] as const satisfies readonly CriticalProductScenario[];

export function productContractReady(
  scenarios: readonly CriticalProductScenario[],
): boolean {
  return scenarios.every((scenario) => {
    const hasRoutableSurface = scenario.route.startsWith("/");
    const hasApiContract = scenario.apiPath !== undefined && scenario.method !== undefined;
    const hasRecoveryContract = scenario.recoverySignal !== undefined;

    return hasRoutableSurface && (hasApiContract || hasRecoveryContract);
  });
}
