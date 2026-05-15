import { apiFetch } from "@/lib/api-client";

export const WORKSPACE_MEMBER_HEADER = "X-Aquila-Creator-Google-Sub";

export type WorkspaceSummary = {
  workspaceId: number;
  name: string;
  description: string | null;
  creatorGoogleSub: string;
  myRole: "ADMIN" | "MEMBER";
  joinedAt: string;
};

export type WorkspaceDetail = WorkspaceSummary;

export type CreateWorkspaceInput = {
  name: string;
  description?: string;
};

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
