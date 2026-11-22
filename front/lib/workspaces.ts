import { apiFetch } from "@/lib/api";
import { resolveAccessToken } from "@/lib/auth";

export type WorkspaceMemberRole = "ADMIN" | "MEMBER";
export type WorkspaceRole = WorkspaceMemberRole;

export type WorkspaceSummary = {
  workspaceId: number;
  name: string;
  description: string | null;
  creatorGoogleSub: string;
  myRole: WorkspaceMemberRole;
  agentCount?: number;
  runningTaskCount?: number;
  completedTaskCount?: number;
  dailyTokenLimit?: number | null;
  todayUsedTokens?: number;
  createdAt?: string;
  joinedAt: string;
};

export type WorkspaceDetail = WorkspaceSummary;

export type WorkspaceMember = {
  memberId: number;
  memberGoogleSub: string;
  email: string;
  name: string;
  role: WorkspaceMemberRole;
  joinedAt: string;
};

export type WorkspaceDashboardRecentReport = {
  executionId: number;
  taskId: number;
  agentId: number;
  status: string;
  summary: string | null;
  failureReason: string | null;
  createdAt: string;
};

export type WorkspaceDashboardRecentLog = {
  executionId: number;
  taskId: number;
  agentId: number;
  level: string;
  message: string;
  createdAt: string;
};

export type WorkspaceDashboardSummary = {
  agentCount: number;
  runningAgentCount: number;
  idleAgentCount: number;
  errorAgentCount: number;
  taskCount: number;
  runningTaskCount: number;
  completedTaskCount: number;
  failedTaskCount: number;
  dailyTokenLimit?: number | null;
  todayUsedTokens?: number;
  waitingApprovalTaskCount?: number;
  avgExecutionDurationMs?: number | null;
  recentReports: WorkspaceDashboardRecentReport[];
  recentLogs: WorkspaceDashboardRecentLog[];
};

export type WorkspaceMemberTaskStats = {
  memberId: number;
  memberName: string;
  rank: number;
  taskCount: number;
  completedTaskCount: number;
  runningTaskCount: number;
  failedTaskCount: number;
  waitingUserTaskCount: number;
  healthScore: number;
  flowScore: number;
  impactScore: number;
  totalScore: number;
};

export type CreateWorkspaceInput = {
  name: string;
  description?: string;
};

export type UpdateWorkspaceInput = {
  name: string;
  description?: string;
  dailyTokenLimit?: number | null;
};

export async function fetchWorkspaces(accessToken: string): Promise<WorkspaceSummary[]> {
  return apiFetch<WorkspaceSummary[]>("/api/v1/workspaces", {
    accessToken,
  });
}

export async function listWorkspaces(): Promise<WorkspaceSummary[]> {
  return fetchWorkspaces(resolveAccessToken());
}

export async function fetchWorkspaceDetail(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceDetail> {
  return apiFetch<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    accessToken,
  });
}

export async function getWorkspace(workspaceId: number): Promise<WorkspaceDetail> {
  return fetchWorkspaceDetail(resolveAccessToken(), workspaceId);
}

export async function fetchWorkspaceMembers(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceMember[]> {
  return apiFetch<WorkspaceMember[]>(`/api/v1/workspaces/${workspaceId}/members`, {
    accessToken,
  });
}

export async function listWorkspaceMembers(workspaceId: number): Promise<WorkspaceMember[]> {
  return fetchWorkspaceMembers(resolveAccessToken(), workspaceId);
}

export async function fetchWorkspaceDashboardSummary(
  accessToken: string,
  workspaceId: number,
  signal?: AbortSignal,
): Promise<WorkspaceDashboardSummary> {
  return apiFetch<WorkspaceDashboardSummary>(
    `/api/v1/workspaces/${workspaceId}/dashboard/summary`,
    {
      accessToken,
      signal,
    },
  );
}

export async function fetchWorkspaceMemberTaskStats(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceMemberTaskStats[]> {
  return apiFetch<WorkspaceMemberTaskStats[]>(
    `/api/v1/workspaces/${workspaceId}/members/task-stats`,
    {
      accessToken,
    },
  );
}

export async function createWorkspace(input: CreateWorkspaceInput): Promise<WorkspaceSummary>;
export async function createWorkspace(
  accessToken: string,
  input: CreateWorkspaceInput,
): Promise<WorkspaceSummary>;
export async function createWorkspace(
  accessTokenOrInput: string | CreateWorkspaceInput,
  maybeInput?: CreateWorkspaceInput,
): Promise<WorkspaceSummary> {
  const accessToken =
    typeof accessTokenOrInput === "string" ? accessTokenOrInput : resolveAccessToken();
  const input = typeof accessTokenOrInput === "string" ? maybeInput : accessTokenOrInput;

  return apiFetch<WorkspaceSummary>("/api/v1/workspaces", {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function updateWorkspace(
  workspaceId: number,
  input: UpdateWorkspaceInput,
): Promise<WorkspaceDetail>;
export async function updateWorkspace(
  accessToken: string,
  workspaceId: number,
  input: UpdateWorkspaceInput,
): Promise<WorkspaceDetail>;
export async function updateWorkspace(
  accessTokenOrWorkspaceId: string | number,
  workspaceIdOrInput: number | UpdateWorkspaceInput,
  maybeInput?: UpdateWorkspaceInput,
): Promise<WorkspaceDetail> {
  const accessToken =
    typeof accessTokenOrWorkspaceId === "string" ? accessTokenOrWorkspaceId : resolveAccessToken();
  const workspaceId =
    typeof accessTokenOrWorkspaceId === "string"
      ? (workspaceIdOrInput as number)
      : accessTokenOrWorkspaceId;
  const input =
    typeof accessTokenOrWorkspaceId === "string"
      ? maybeInput
      : (workspaceIdOrInput as UpdateWorkspaceInput);

  return apiFetch<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    method: "PATCH",
    accessToken,
    json: input,
  });
}

export async function deleteWorkspace(workspaceId: number): Promise<void>;
export async function deleteWorkspace(accessToken: string, workspaceId: number): Promise<void>;
export async function deleteWorkspace(
  accessTokenOrWorkspaceId: string | number,
  maybeWorkspaceId?: number,
): Promise<void> {
  const accessToken =
    typeof accessTokenOrWorkspaceId === "string" ? accessTokenOrWorkspaceId : resolveAccessToken();
  const workspaceId =
    typeof accessTokenOrWorkspaceId === "string" ? maybeWorkspaceId : accessTokenOrWorkspaceId;

  return apiFetch<void>(`/api/v1/workspaces/${workspaceId}`, {
    method: "DELETE",
    accessToken,
  });
}

export async function changeWorkspaceMemberRole(
  accessToken: string,
  workspaceId: number,
  memberId: number,
  role: WorkspaceMemberRole,
): Promise<void> {
  return apiFetch<void>(`/api/v1/workspaces/${workspaceId}/members/${memberId}`, {
    method: "PATCH",
    accessToken,
    json: { role },
  });
}

export async function changeMemberRole(
  workspaceId: number,
  memberId: number,
  role: WorkspaceMemberRole,
): Promise<void> {
  return changeWorkspaceMemberRole(resolveAccessToken(), workspaceId, memberId, role);
}

export async function removeWorkspaceMember(
  accessToken: string,
  workspaceId: number,
  memberId: number,
): Promise<void> {
  return apiFetch<void>(`/api/v1/workspaces/${workspaceId}/members/${memberId}`, {
    method: "DELETE",
    accessToken,
  });
}

export async function removeMember(workspaceId: number, memberId: number): Promise<void> {
  return removeWorkspaceMember(resolveAccessToken(), workspaceId, memberId);
}
