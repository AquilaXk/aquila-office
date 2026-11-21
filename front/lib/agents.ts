import { apiFetch } from "@/lib/api";
import {
  MAX_AGENT_SKILL_CONTENT_CHARS,
  MAX_AGENT_SKILL_FILES,
  MAX_AGENT_SKILL_FILE_NAME_CHARS,
  normalizeWorkspaceAgents,
  validateAgentSkillFiles,
  workspaceAgentPath,
  workspaceAgentsPath,
  type CreateWorkspaceAgentInput,
  type UpdateWorkspaceAgentInput,
  type WorkspaceAgent,
  type WorkspaceAgentCategory,
  type WorkspaceAgentSkillFile,
  type WorkspaceAgentSkillFileInput,
  type WorkspaceAgentSkillFileSyncStatus,
  type WorkspaceAgentStatus,
} from "@/lib/agents-utils";

export {
  MAX_AGENT_SKILL_CONTENT_CHARS,
  MAX_AGENT_SKILL_FILES,
  MAX_AGENT_SKILL_FILE_NAME_CHARS,
  normalizeWorkspaceAgents,
  validateAgentSkillFiles,
  workspaceAgentPath,
  workspaceAgentsPath,
  type CreateWorkspaceAgentInput,
  type UpdateWorkspaceAgentInput,
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
    json: input,
  });
}

export async function updateWorkspaceAgent(
  accessToken: string,
  workspaceId: number,
  agentId: number,
  input: UpdateWorkspaceAgentInput,
): Promise<WorkspaceAgent> {
  const validatedSkills =
    input.skillFiles !== undefined ? validateAgentSkillFiles(input.skillFiles) : undefined;
  return apiFetch<WorkspaceAgent>(workspaceAgentPath(workspaceId, agentId), {
    method: "PATCH",
    accessToken,
    json: {
      ...input,
      ...(validatedSkills !== undefined ? { skillFiles: validatedSkills } : {}),
    },
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
