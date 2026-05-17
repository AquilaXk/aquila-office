import { apiFetch } from "@/lib/api-client";
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
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(input),
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
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });
}
