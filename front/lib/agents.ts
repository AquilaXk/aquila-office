import { apiFetch } from "@/lib/api-client";
import { WORKSPACE_MEMBER_HEADER } from "@/lib/workspaces";
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
  memberGoogleSub: string,
  workspaceId: number,
): Promise<WorkspaceAgent[]> {
  const payload = await apiFetch<unknown>(workspaceAgentsPath(workspaceId), {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });

  return normalizeWorkspaceAgents(payload);
}

export async function fetchWorkspaceAgent(
  memberGoogleSub: string,
  workspaceId: number,
  agentId: number,
): Promise<WorkspaceAgent> {
  return apiFetch<WorkspaceAgent>(workspaceAgentPath(workspaceId, agentId), {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}

export async function createWorkspaceAgent(
  memberGoogleSub: string,
  workspaceId: number,
  input: CreateWorkspaceAgentInput,
): Promise<WorkspaceAgent> {
  return apiFetch<WorkspaceAgent>(workspaceAgentsPath(workspaceId), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
    body: JSON.stringify(input),
  });
}

export async function deleteWorkspaceAgent(
  memberGoogleSub: string,
  workspaceId: number,
  agentId: number,
): Promise<void> {
  return apiFetch<void>(workspaceAgentPath(workspaceId, agentId), {
    method: "DELETE",
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}
