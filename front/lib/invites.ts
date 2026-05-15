import { apiFetch } from "@/lib/api-client";
import { WORKSPACE_MEMBER_HEADER, type WorkspaceMemberRole } from "@/lib/workspaces";

export type WorkspaceInviteStatus = "PENDING" | "ACCEPTED" | "EXPIRED";

export type WorkspaceInvite = {
  inviteId: number;
  workspaceId: number;
  email: string;
  token: string;
  status: WorkspaceInviteStatus;
  createdAt: string;
  expiresAt: string;
  acceptedAt: string | null;
};

export type AcceptedWorkspaceInvite = {
  inviteId: number;
  workspaceId: number;
  memberGoogleSub: string;
  role: WorkspaceMemberRole;
  acceptedAt: string;
};

export type CreateWorkspaceInviteInput = {
  email: string;
};

export async function fetchWorkspaceInvite(token: string): Promise<WorkspaceInvite> {
  return apiFetch<WorkspaceInvite>(`/api/v1/workspace-invites/${encodeURIComponent(token)}`);
}

export async function createWorkspaceInvite(
  memberGoogleSub: string,
  workspaceId: number,
  input: CreateWorkspaceInviteInput,
): Promise<WorkspaceInvite> {
  return apiFetch<WorkspaceInvite>(`/api/v1/workspaces/${workspaceId}/invites`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
    body: JSON.stringify(input),
  });
}

export async function acceptWorkspaceInvite(
  token: string,
  memberGoogleSub: string,
): Promise<AcceptedWorkspaceInvite> {
  return apiFetch<AcceptedWorkspaceInvite>(
    `/api/v1/workspace-invites/${encodeURIComponent(token)}/accept`,
    {
      method: "POST",
      headers: {
        [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
      },
    },
  );
}
