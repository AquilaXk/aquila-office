import { apiFetch } from "@/lib/api-client";
import {
  normalizeWorkspaceAgents,
  workspaceAgentPath,
  workspaceAgentsPath,
  type CreateWorkspaceAgentInput,
  type WorkspaceAgent,
  type WorkspaceAgentCategory,
  type WorkspaceAgentSkillFile,
  type WorkspaceAgentSkillFileInput,
  type WorkspaceAgentSkillFileSyncStatus,
  type WorkspaceAgentStatus,
} from "@/lib/agents-utils";

export {
  normalizeWorkspaceAgents,
  workspaceAgentPath,
  workspaceAgentsPath,
  type CreateWorkspaceAgentInput,
  type WorkspaceAgent,
  type WorkspaceAgentCategory,
  type WorkspaceAgentSkillFile,
  type WorkspaceAgentSkillFileInput,
  type WorkspaceAgentSkillFileSyncStatus,
  type WorkspaceAgentStatus,
};

export async function fetchWorkspaceAgents(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceAgent[]> {
  const payload = await apiFetch<unknown>(workspaceAgentsPath(workspaceId), {
    accessToken,
  });

  return normalizeWorkspaceAgents(payload);
}

export async function fetchWorkspaceAgent(
  accessToken: string,
  workspaceId: number,
  agentId: number,
): Promise<WorkspaceAgent> {
  return apiFetch<WorkspaceAgent>(workspaceAgentPath(workspaceId, agentId), {
    accessToken,
  });
}

export async function createWorkspaceAgent(
  accessToken: string,
  workspaceId: number,
  input: CreateWorkspaceAgentInput,
): Promise<WorkspaceAgent> {
  return apiFetch<WorkspaceAgent>(workspaceAgentsPath(workspaceId), {
    method: "POST",
    accessToken,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });
}

export async function deleteWorkspaceAgent(
  accessToken: string,
  workspaceId: number,
  agentId: number,
): Promise<void> {
  return apiFetch<void>(workspaceAgentPath(workspaceId, agentId), {
    method: "DELETE",
    accessToken,
  });
}
