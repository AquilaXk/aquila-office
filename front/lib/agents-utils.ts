export type WorkspaceAgentCategory =
  | "ORCHESTRATOR"
  | "BACKEND"
  | "FRONTEND"
  | "QA"
  | "CUSTOM";

export type WorkspaceAgentStatus =
  | "CREATING"
  | "READY"
  | "SYNC_FAILED"
  | "ERROR"
  | "DISABLED";

export type WorkspaceAgentSkillFileSyncStatus = "PENDING" | "SYNCED" | "FAILED";

export type WorkspaceAgentSkillFile = {
  skillFileId: number;
  fileName: string;
  syncStatus: WorkspaceAgentSkillFileSyncStatus;
  syncError: string | null;
};

export type WorkspaceAgent = {
  agentId: number;
  workspaceId: number;
  name: string;
  category: WorkspaceAgentCategory;
  openClawAgentId: string | null;
  workspacePath: string;
  status: WorkspaceAgentStatus;
  syncError: string | null;
  createdByGoogleSub: string;
  createdAt: string | null;
  updatedAt: string | null;
  skillFiles: WorkspaceAgentSkillFile[];
};

export type WorkspaceAgentSkillFileInput = {
  fileName: string;
  content: string;
};

export type CreateWorkspaceAgentInput = {
  name: string;
  category?: WorkspaceAgentCategory;
  workspacePath?: string;
  skillFiles?: WorkspaceAgentSkillFileInput[];
};

export function workspaceAgentsPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/agents`;
}

export function workspaceAgentPath(workspaceId: number, agentId: number): string {
  return `${workspaceAgentsPath(workspaceId)}/${agentId}`;
}

export function normalizeWorkspaceAgents(payload: unknown): WorkspaceAgent[] {
  if (Array.isArray(payload)) {
    return payload as WorkspaceAgent[];
  }
  if (!payload || typeof payload !== "object") {
    return [];
  }

  const record = payload as Record<string, unknown>;
  if (Array.isArray(record.content)) {
    return record.content as WorkspaceAgent[];
  }
  if (Array.isArray(record.agents)) {
    return record.agents as WorkspaceAgent[];
  }
  if (Array.isArray(record.data)) {
    return record.data as WorkspaceAgent[];
  }

  return [];
}
