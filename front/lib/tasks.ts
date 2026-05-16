import { apiFetch } from "@/lib/api-client";
import { WORKSPACE_MEMBER_HEADER } from "@/lib/workspaces";
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
  memberGoogleSub: string,
  workspaceId: number,
): Promise<WorkspaceTask[]> {
  const payload = await apiFetch<unknown>(workspaceTasksPath(workspaceId), {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });

  return normalizeWorkspaceTasks(payload);
}

export async function fetchWorkspaceTask(
  memberGoogleSub: string,
  workspaceId: number,
  taskId: number,
): Promise<WorkspaceTask> {
  return apiFetch<WorkspaceTask>(workspaceTaskPath(workspaceId, taskId), {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}

export async function fetchTaskReports(
  memberGoogleSub: string,
  workspaceId: number,
  taskId: number,
): Promise<AgentReport[]> {
  const payload = await apiFetch<unknown>(workspaceTaskReportsPath(workspaceId, taskId), {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });

  return normalizeTaskReports(payload);
}
