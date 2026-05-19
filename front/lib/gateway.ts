import { apiFetch } from "@/lib/api";
import {
  workspaceGatewayBindingPath,
  workspaceGatewayConnectionTestPath,
  workspaceGatewayStatusPath,
  type GatewayBindingStatus,
  type GatewayConnectionStatus,
} from "@/lib/gateway-utils";

export type GatewayMode = "MANAGED" | "EXTERNAL";

export type WorkspaceGatewayStatus = {
  status: GatewayBindingStatus;
  bound: boolean;
  bindingId: number | null;
  mode: GatewayMode | null;
  gatewayUrl: string | null;
  maskedToken: string | null;
  lastStatus: GatewayConnectionStatus | null;
  lastCheckedAt: string | null;
  lastError: string | null;
  updatedAt: string | null;
};

export type WorkspaceGatewayConnectionTest = {
  status: GatewayConnectionStatus;
  connected: boolean;
  gatewayUrl: string;
  message: string;
  agentCount: number;
};

export type WorkspaceGatewayBinding = {
  id: number;
  workspaceId: number;
  mode: GatewayMode;
  gatewayUrl: string;
  maskedToken: string;
};

export type CreateGatewayBindingInput = {
  gatewayUrl: string;
  token: string;
  validateConnection?: boolean;
};

export type TestGatewayConnectionInput = Pick<
  CreateGatewayBindingInput,
  "gatewayUrl" | "token"
>;

export type OpenClawIntegrationStorage = Pick<Storage, "getItem" | "setItem" | "removeItem">;

export function openClawIntegrationStorageKey(workspaceId: number): string {
  return `aio.workspace.${workspaceId}.integrations.openclaw`;
}

export function normalizeWorkspaceGatewayStatus(
  status: WorkspaceGatewayStatus,
): WorkspaceGatewayStatus {
  return {
    ...status,
    bound: Boolean(status.bound),
    status: status.status ?? (status.bound ? "BOUND" : "UNBOUND"),
    bindingId: status.bindingId ?? null,
    mode: status.mode ?? null,
    gatewayUrl: status.gatewayUrl ?? null,
    maskedToken: status.maskedToken ?? null,
    lastStatus: status.lastStatus ?? null,
    lastCheckedAt: status.lastCheckedAt ?? null,
    lastError: status.lastError ?? null,
    updatedAt: status.updatedAt ?? null,
  };
}

export function gatewayBindingToStatus(
  binding: WorkspaceGatewayBinding,
): WorkspaceGatewayStatus {
  return {
    status: "BOUND",
    bound: true,
    bindingId: binding.id,
    mode: binding.mode,
    gatewayUrl: binding.gatewayUrl,
    maskedToken: binding.maskedToken,
    lastStatus: null,
    lastCheckedAt: null,
    lastError: null,
    updatedAt: null,
  };
}

export function readStoredOpenClawGatewayStatus(
  storage: OpenClawIntegrationStorage | null | undefined,
  workspaceId: number,
): WorkspaceGatewayStatus | null {
  if (!storage) {
    return null;
  }

  const raw = storage.getItem(openClawIntegrationStorageKey(workspaceId));
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as WorkspaceGatewayStatus | WorkspaceGatewayBinding;
    if ("bound" in parsed) {
      return normalizeWorkspaceGatewayStatus(parsed);
    }
    return gatewayBindingToStatus(parsed);
  } catch {
    return null;
  }
}

export function writeStoredOpenClawGatewayStatus(
  storage: OpenClawIntegrationStorage | null | undefined,
  workspaceId: number,
  gateway: WorkspaceGatewayStatus,
): void {
  storage?.setItem(openClawIntegrationStorageKey(workspaceId), JSON.stringify(gateway));
}

export function deleteStoredOpenClawGatewayStatus(
  storage: OpenClawIntegrationStorage | null | undefined,
  workspaceId: number,
): void {
  storage?.removeItem(openClawIntegrationStorageKey(workspaceId));
}

export async function fetchWorkspaceGatewayStatus(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceGatewayStatus> {
  return apiFetch<WorkspaceGatewayStatus>(workspaceGatewayStatusPath(workspaceId), {
    accessToken,
  });
}

export async function testWorkspaceGatewayConnection(
  accessToken: string,
  workspaceId: number,
  input: TestGatewayConnectionInput,
): Promise<WorkspaceGatewayConnectionTest> {
  return apiFetch<WorkspaceGatewayConnectionTest>(
    workspaceGatewayConnectionTestPath(workspaceId),
    {
      method: "POST",
      accessToken,
      json: input,
    },
  );
}

export async function createWorkspaceGatewayBinding(
  accessToken: string,
  workspaceId: number,
  input: CreateGatewayBindingInput,
): Promise<WorkspaceGatewayBinding> {
  return apiFetch<WorkspaceGatewayBinding>(workspaceGatewayBindingPath(workspaceId), {
    method: "POST",
    accessToken,
    json: input,
  });
}
