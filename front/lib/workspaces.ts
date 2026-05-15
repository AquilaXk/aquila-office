import { apiFetch } from "@/lib/api-client";

export const WORKSPACE_MEMBER_HEADER = "X-Aquila-Creator-Google-Sub";

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

export async function fetchWorkspaces(memberGoogleSub: string): Promise<WorkspaceSummary[]> {
  return apiFetch<WorkspaceSummary[]>("/api/v1/workspaces", {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}

export async function fetchWorkspaceDetail(
  memberGoogleSub: string,
  workspaceId: number,
): Promise<WorkspaceDetail> {
  return apiFetch<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}

export async function fetchWorkspaceMembers(
  memberGoogleSub: string,
  workspaceId: number,
): Promise<WorkspaceMember[]> {
  return apiFetch<WorkspaceMember[]>(`/api/v1/workspaces/${workspaceId}/members`, {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}

export async function createWorkspace(
  memberGoogleSub: string,
  input: CreateWorkspaceInput,
): Promise<WorkspaceSummary> {
  return apiFetch<WorkspaceSummary>("/api/v1/workspaces", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
    body: JSON.stringify(input),
  });
}

export async function updateWorkspace(
  memberGoogleSub: string,
  workspaceId: number,
  input: UpdateWorkspaceInput,
): Promise<WorkspaceDetail> {
  return apiFetch<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
    body: JSON.stringify(input),
  });
}

export async function deleteWorkspace(
  memberGoogleSub: string,
  workspaceId: number,
): Promise<void> {
  return apiFetch<void>(`/api/v1/workspaces/${workspaceId}`, {
    method: "DELETE",
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}

export async function changeWorkspaceMemberRole(
  memberGoogleSub: string,
  workspaceId: number,
  memberId: number,
  role: WorkspaceMemberRole,
): Promise<void> {
  return apiFetch<void>(`/api/v1/workspaces/${workspaceId}/members/${memberId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
    body: JSON.stringify({ role }),
  });
}

export async function removeWorkspaceMember(
  memberGoogleSub: string,
  workspaceId: number,
  memberId: number,
): Promise<void> {
  return apiFetch<void>(`/api/v1/workspaces/${workspaceId}/members/${memberId}`, {
    method: "DELETE",
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}
