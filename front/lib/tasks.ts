import { apiFetch } from "@/lib/api-client";
import {
  normalizeTaskReports,
  normalizeWorkspaceTasks,
  workspaceTaskPath,
  workspaceTaskReportsPath,
  workspaceTasksPath,
  type AgentReport,
  type WorkspaceTask,
} from "@/lib/tasks-utils";

export {
  normalizeTaskReports,
  normalizeWorkspaceTasks,
  workspaceTaskPath,
  workspaceTaskReportsPath,
  workspaceTasksPath,
  type AgentReport,
  type TaskArtifact,
  type TaskArtifactType,
  type TaskPriority,
  type TaskStatus,
  type TaskType,
  type WorkspaceTask,
} from "@/lib/tasks-utils";

export async function fetchWorkspaceTasks(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceTask[]> {
  const payload = await apiFetch<unknown>(workspaceTasksPath(workspaceId), {
    accessToken,
  });

  return normalizeWorkspaceTasks(payload);
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
