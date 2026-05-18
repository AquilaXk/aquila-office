import { apiFetch } from "@/lib/api";
import {
  normalizeTaskReports,
  normalizeTaskExecutionLogs,
  normalizeWorkspaceTasks,
  workspaceTaskLogsPath,
  workspaceTaskMessagesPath,
  workspaceTaskPath,
  workspaceTaskReportsPath,
  workspaceTaskRunPath,
  workspaceTaskStatusPath,
  workspaceTasksRunPath,
  workspaceTasksPath,
  type AgentReport,
  type RunWorkspaceTaskResult,
  type TaskExecutionLog,
  type TaskPriority,
  type TaskStatus,
  type TaskType,
  type WorkspaceTask,
} from "@/lib/tasks-utils";
import type { ChatMessage } from "@/lib/chat";

export {
  normalizeTaskReports,
  normalizeTaskExecutionLogs,
  normalizeWorkspaceTasks,
  workspaceTaskLogsPath,
  workspaceTaskMessagesPath,
  workspaceTaskPath,
  workspaceTaskReportsPath,
  workspaceTaskRunPath,
  workspaceTaskStatusPath,
  workspaceTasksRunPath,
  workspaceTasksPath,
  type AgentReport,
  type RunWorkspaceTaskResult,
  type TaskArtifact,
  type TaskArtifactType,
  type TaskExecutionLog,
  type TaskExecutionStatus,
  type TaskPriority,
  type TaskStatus,
  type TaskType,
  type WorkspaceTask,
} from "@/lib/tasks-utils";

export type CreateWorkspaceTaskInput = {
  title: string;
  description?: string | null;
  type?: TaskType;
  priority?: TaskPriority;
  assignedAgentId?: number | null;
};

export type UpdateWorkspaceTaskStatusInput = {
  status: TaskStatus;
};

export type RunWorkspaceTaskInput = {
  createPr?: boolean;
};

export type CreateAndRunWorkspaceTaskInput = CreateWorkspaceTaskInput & RunWorkspaceTaskInput;

export async function fetchWorkspaceTasks(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceTask[]> {
  const payload = await apiFetch<unknown>(workspaceTasksPath(workspaceId), {
    accessToken,
  });

  return normalizeWorkspaceTasks(payload);
}

export async function createWorkspaceTask(
  accessToken: string,
  workspaceId: number,
  input: CreateWorkspaceTaskInput,
): Promise<WorkspaceTask> {
  return apiFetch<WorkspaceTask>(workspaceTasksPath(workspaceId), {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function createAndRunWorkspaceTask(
  accessToken: string,
  workspaceId: number,
  input: CreateAndRunWorkspaceTaskInput,
): Promise<RunWorkspaceTaskResult> {
  return apiFetch<RunWorkspaceTaskResult>(workspaceTasksRunPath(workspaceId), {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function fetchWorkspaceTask(
  accessToken: string,
  workspaceId: number,
  taskId: number,
): Promise<WorkspaceTask> {
  return apiFetch<WorkspaceTask>(workspaceTaskPath(workspaceId, taskId), {
    accessToken,
  });
}

export async function updateWorkspaceTaskStatus(
  accessToken: string,
  workspaceId: number,
  taskId: number,
  input: UpdateWorkspaceTaskStatusInput,
): Promise<WorkspaceTask> {
  return apiFetch<WorkspaceTask>(workspaceTaskStatusPath(workspaceId, taskId), {
    method: "PATCH",
    accessToken,
    json: input,
  });
}

export async function runWorkspaceTask(
  accessToken: string,
  workspaceId: number,
  taskId: number,
  input: RunWorkspaceTaskInput = {},
): Promise<RunWorkspaceTaskResult> {
  return apiFetch<RunWorkspaceTaskResult>(workspaceTaskRunPath(workspaceId, taskId), {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function fetchTaskReports(
  accessToken: string,
  workspaceId: number,
  taskId: number,
): Promise<AgentReport[]> {
  const payload = await apiFetch<unknown>(workspaceTaskReportsPath(workspaceId, taskId), {
    accessToken,
  });

  return normalizeTaskReports(payload);
}

export async function fetchTaskExecutionLogs(
  accessToken: string,
  workspaceId: number,
  taskId: number,
): Promise<TaskExecutionLog[]> {
  const payload = await apiFetch<unknown>(workspaceTaskLogsPath(workspaceId, taskId), {
    accessToken,
  });

  return normalizeTaskExecutionLogs(payload);
}

export async function fetchTaskMessages(
  accessToken: string,
  workspaceId: number,
  taskId: number,
): Promise<ChatMessage[]> {
  return apiFetch<ChatMessage[]>(workspaceTaskMessagesPath(workspaceId, taskId), {
    accessToken,
  });
}
