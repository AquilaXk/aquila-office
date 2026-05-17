import { apiFetch } from "@/lib/api-client";

export type WorkspaceMemberRole = "ADMIN" | "MEMBER";

export type WorkspaceSummary = {
  workspaceId: number;
  name: string;
  description: string | null;
  creatorGoogleSub: string;
  myRole: WorkspaceMemberRole;
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

export type CreateWorkspaceInput = {
  name: string;
  description?: string;
};

export type UpdateWorkspaceInput = CreateWorkspaceInput;

export async function fetchWorkspaces(accessToken: string): Promise<WorkspaceSummary[]> {
  return apiFetch<WorkspaceSummary[]>("/api/v1/workspaces", {
    accessToken,
  });
}

export async function fetchWorkspaceDetail(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceDetail> {
  return apiFetch<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    accessToken,
  });
}

export async function fetchWorkspaceMembers(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceMember[]> {
  return apiFetch<WorkspaceMember[]>(`/api/v1/workspaces/${workspaceId}/members`, {
    accessToken,
  });
}

export async function createWorkspace(
  accessToken: string,
  input: CreateWorkspaceInput,
): Promise<WorkspaceSummary> {
  return apiFetch<WorkspaceSummary>("/api/v1/workspaces", {
    method: "POST",
    accessToken,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });
}

export async function updateWorkspace(
  accessToken: string,
  workspaceId: number,
  input: UpdateWorkspaceInput,
): Promise<WorkspaceDetail> {
  return apiFetch<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    method: "PATCH",
    accessToken,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });
}

export async function deleteWorkspace(
  accessToken: string,
  workspaceId: number,
): Promise<void> {
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
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ role }),
  });
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
