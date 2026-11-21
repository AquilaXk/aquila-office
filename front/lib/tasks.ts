import { apiFetch } from "@/lib/api";
import {
  MAX_STALE_MINUTES,
  MAX_TASK_APPROVAL_TEXT_CHARS,
  MIN_STALE_MINUTES,
  normalizeTaskExecutionLogs,
  normalizeTaskReports,
  normalizeWorkspaceTasks,
  workspaceTaskCancelPath,
  workspaceTaskLogsPath,
  workspaceTaskMessagesPath,
  workspaceTaskPath,
  workspaceTaskReportsPath,
  workspaceTaskRequestApprovalPath,
  workspaceTaskReviewApprovalPath,
  workspaceTaskRunPath,
  workspaceTaskStatusPath,
  workspaceTasksPath,
  workspaceTasksRecoverStalePath,
  workspaceTasksRunPath,
  type AgentReport,
  type RecoverStaleTasksInput,
  type RecoverStaleTasksResult,
  type RequestTaskApprovalInput,
  type ReviewTaskApprovalInput,
  type RunWorkspaceTaskResult,
  type TaskExecutionLog,
  type TaskPriority,
  type TaskStatus,
  type TaskType,
  type WorkspaceTask,
} from "@/lib/tasks-utils";
import type { ChatMessage } from "@/lib/chat";

export {
  MAX_STALE_MINUTES,
  MAX_TASK_APPROVAL_TEXT_CHARS,
  MIN_STALE_MINUTES,
  normalizeTaskReports,
  normalizeTaskExecutionLogs,
  normalizeWorkspaceTasks,
  validateTaskApprovalText,
  workspaceTaskCancelPath,
  workspaceTaskLogsPath,
  workspaceTaskMessagesPath,
  workspaceTaskPath,
  workspaceTaskReportsPath,
  workspaceTaskRequestApprovalPath,
  workspaceTaskReviewApprovalPath,
  workspaceTaskRunPath,
  workspaceTaskStatusPath,
  workspaceTasksPath,
  workspaceTasksRecoverStalePath,
  workspaceTasksRunPath,
  type AgentReport,
  type RecoverStaleTasksInput,
  type RecoverStaleTasksResult,
  type RequestTaskApprovalInput,
  type ReviewTaskApprovalInput,
  type RunWorkspaceTaskResult,
  type TaskApprovalDecision,
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

export async function cancelWorkspaceTask(
  accessToken: string,
  workspaceId: number,
  taskId: number,
): Promise<WorkspaceTask> {
  return apiFetch<WorkspaceTask>(workspaceTaskCancelPath(workspaceId, taskId), {
    method: "POST",
    accessToken,
  });
}

export async function requestTaskApproval(
  accessToken: string,
  workspaceId: number,
  taskId: number,
  input: RequestTaskApprovalInput = {},
): Promise<WorkspaceTask> {
  const reason = input.reason?.trim() ?? null;
  if (reason && reason.length > MAX_TASK_APPROVAL_TEXT_CHARS) {
    throw new RangeError(
      `승인 요청 사유는 ${MAX_TASK_APPROVAL_TEXT_CHARS}자를 초과할 수 없습니다.`,
    );
  }
  return apiFetch<WorkspaceTask>(workspaceTaskRequestApprovalPath(workspaceId, taskId), {
    method: "POST",
    accessToken,
    json: reason ? { reason } : {},
  });
}

export async function reviewTaskApproval(
  accessToken: string,
  workspaceId: number,
  taskId: number,
  input: ReviewTaskApprovalInput,
): Promise<WorkspaceTask> {
  const comment = input.comment?.trim() ?? null;
  if (comment && comment.length > MAX_TASK_APPROVAL_TEXT_CHARS) {
    throw new RangeError(
      `검토 의견은 ${MAX_TASK_APPROVAL_TEXT_CHARS}자를 초과할 수 없습니다.`,
    );
  }
  return apiFetch<WorkspaceTask>(workspaceTaskReviewApprovalPath(workspaceId, taskId), {
    method: "POST",
    accessToken,
    json: {
      decision: input.decision,
      ...(comment ? { comment } : {}),
      ...(input.runAfterApprove !== undefined ? { runAfterApprove: input.runAfterApprove } : {}),
    },
  });
}

export async function recoverStaleWorkspaceTasks(
  accessToken: string,
  workspaceId: number,
  input: RecoverStaleTasksInput = {},
): Promise<RecoverStaleTasksResult> {
  const staleMinutes = input.staleMinutes ?? 15;
  if (
    !Number.isInteger(staleMinutes) ||
    staleMinutes < MIN_STALE_MINUTES ||
    staleMinutes > MAX_STALE_MINUTES
  ) {
    throw new RangeError(
      `고착 복구 기준 시간은 ${MIN_STALE_MINUTES}~${MAX_STALE_MINUTES}분 사이 정수여야 합니다.`,
    );
  }
  return apiFetch<RecoverStaleTasksResult>(workspaceTasksRecoverStalePath(workspaceId), {
    method: "POST",
    accessToken,
    json: { staleMinutes },
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
