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

export const MAX_AGENT_SKILL_FILES = 20;
export const MAX_AGENT_SKILL_FILE_NAME_CHARS = 120;
export const MAX_AGENT_SKILL_CONTENT_CHARS = 65_536;

export type WorkspaceAgentSkillFile = {
  skillFileId: number;
  fileName: string;
  content?: string | null;
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
  modelProviderId?: number | null;
  modelId?: string | null;
  fallbackModelId?: string | null;
};

export type WorkspaceAgentSkillFileInput = {
  fileName: string;
  content: string;
};

export type CreateWorkspaceAgentInput = {
  name: string;
  emoji?: string;
  category?: WorkspaceAgentCategory;
  workspacePath?: string;
  skillFiles?: WorkspaceAgentSkillFileInput[];
  modelProviderId?: number | null;
  modelId?: string | null;
  fallbackModelId?: string | null;
};

export type UpdateWorkspaceAgentInput = {
  name: string;
  category?: WorkspaceAgentCategory;
  workspacePath?: string;
  skillFiles?: WorkspaceAgentSkillFileInput[];
  modelProviderId?: number | null;
  modelId?: string | null;
  fallbackModelId?: string | null;
  updateModelConfig?: boolean;
};

export function validateAgentSkillFiles(
  skillFiles: readonly WorkspaceAgentSkillFileInput[],
): WorkspaceAgentSkillFileInput[] {
  if (skillFiles.length > MAX_AGENT_SKILL_FILES) {
    throw new RangeError(
      `스킬 파일은 최대 ${MAX_AGENT_SKILL_FILES}개까지 등록할 수 있습니다.`,
    );
  }

  const normalized: WorkspaceAgentSkillFileInput[] = [];
  for (let i = 0; i < skillFiles.length; i += 1) {
    const fileName = skillFiles[i].fileName.trim();
    const content = skillFiles[i].content.trim();
    if (!fileName && !content) {
      continue;
    }
    if (!fileName || !content) {
      throw new RangeError("스킬 파일의 파일명과 본문을 모두 입력해 주세요.");
    }
    if (fileName.length > MAX_AGENT_SKILL_FILE_NAME_CHARS) {
      throw new RangeError(
        `스킬 파일명은 ${MAX_AGENT_SKILL_FILE_NAME_CHARS}자를 초과할 수 없습니다.`,
      );
    }
    if (fileName.includes("..") || fileName.startsWith("/") || fileName.includes("\\")) {
      throw new RangeError("스킬 파일명에 상위 경로(..)나 절대 경로를 사용할 수 없습니다.");
    }
    if (content.length > MAX_AGENT_SKILL_CONTENT_CHARS) {
      throw new RangeError(
        `스킬 본문은 ${MAX_AGENT_SKILL_CONTENT_CHARS}자를 초과할 수 없습니다.`,
      );
    }
    normalized.push({ fileName, content });
  }

  return normalized;
}

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
