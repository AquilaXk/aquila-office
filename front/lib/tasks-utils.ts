export type TaskStatus =
  | "REQUESTED"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "WAITING_USER"
  | "COMPLETED"
  | "FAILED"
  | "CANCELED";

export type TaskType =
  | "CODE_REVIEW"
  | "PR_REVIEW"
  | "BUG_FIX"
  | "FEATURE_IMPLEMENTATION"
  | "REFACTORING"
  | "TEST_CREATION"
  | "DOCUMENTATION"
  | "PR_CREATION"
  | "OTHER";

export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export type TaskArtifactType =
  | "PR_URL"
  | "COMMIT_HASH"
  | "FILE_PATH"
  | "DIFF"
  | "LOG_FILE"
  | "RESULT_FILE"
  | "OTHER";

export type WorkspaceTask = {
  taskId: number;
  workspaceId?: number;
  createdByGoogleSub?: string;
  title: string;
  description?: string | null;
  type?: TaskType;
  priority?: TaskPriority;
  assignedAgentId?: number | string | null;
  status: TaskStatus;
  createdAt?: string;
  updatedAt?: string;
};

export type TaskArtifact = {
  artifactId: number;
  artifactType: TaskArtifactType;
  name: string;
  url?: string | null;
};

export type AgentReport = {
  reportId: number;
  taskId: number;
  status: TaskStatus;
  summary?: string | null;
  detail?: string | null;
  recommendedAction?: string | null;
  artifacts?: TaskArtifact[];
  createdAt?: string;
};

export function workspaceTasksPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/tasks`;
}

export function workspaceTaskPath(workspaceId: number, taskId: number): string {
  return `${workspaceTasksPath(workspaceId)}/${taskId}`;
}

export function workspaceTaskReportsPath(workspaceId: number, taskId: number): string {
  return `${workspaceTaskPath(workspaceId, taskId)}/reports`;
}

export function normalizeWorkspaceTasks(payload: unknown): WorkspaceTask[] {
  return normalizeList<WorkspaceTask>(payload);
}

export function normalizeTaskReports(payload: unknown): AgentReport[] {
  return normalizeList<AgentReport>(payload);
}

function normalizeList<T>(payload: unknown): T[] {
  if (Array.isArray(payload)) {
    return payload as T[];
  }

  if (payload && typeof payload === "object") {
    const record = payload as Record<string, unknown>;
    if (Array.isArray(record.content)) {
      return record.content as T[];
    }
  }

  return [];
}
