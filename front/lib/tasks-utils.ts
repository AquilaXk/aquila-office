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

export type TaskExecutionStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED";

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

export type TaskExecutionLog = {
  logId: number;
  workspaceId?: number;
  taskId: number;
  taskExecutionId: number;
  agentId?: number | null;
  level: "INFO" | "WARN" | "ERROR" | string;
  message: string;
  createdAt?: string;
};

export type RunWorkspaceTaskResult = {
  workspaceId: number;
  taskId: number;
  executionId: number;
  agentId: number;
  taskStatus: TaskStatus;
  executionStatus: TaskExecutionStatus;
  finalText?: string | null;
  failureReason?: string | null;
};

export function workspaceTasksPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/tasks`;
}

export function workspaceTasksRunPath(workspaceId: number): string {
  return `${workspaceTasksPath(workspaceId)}/run`;
}

export function workspaceTaskPath(workspaceId: number, taskId: number): string {
  return `${workspaceTasksPath(workspaceId)}/${taskId}`;
}

export function workspaceTaskReportsPath(workspaceId: number, taskId: number): string {
  return `${workspaceTaskPath(workspaceId, taskId)}/reports`;
}

export function workspaceTaskLogsPath(workspaceId: number, taskId: number): string {
  return `${workspaceTaskPath(workspaceId, taskId)}/logs`;
}

export function workspaceTaskMessagesPath(workspaceId: number, taskId: number): string {
  return `${workspaceTaskPath(workspaceId, taskId)}/messages`;
}

export function workspaceTaskStatusPath(workspaceId: number, taskId: number): string {
  return `${workspaceTaskPath(workspaceId, taskId)}/status`;
}

export function workspaceTaskRunPath(workspaceId: number, taskId: number): string {
  return `${workspaceTaskPath(workspaceId, taskId)}/run`;
}

export function normalizeWorkspaceTasks(payload: unknown): WorkspaceTask[] {
  return normalizeList<WorkspaceTask>(payload);
}

export function normalizeTaskReports(payload: unknown): AgentReport[] {
  return normalizeList<AgentReport>(payload);
}

export function normalizeTaskExecutionLogs(payload: unknown): TaskExecutionLog[] {
  return normalizeList<TaskExecutionLog>(payload);
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
