import { apiFetch } from "@/lib/api-client";
import { WORKSPACE_MEMBER_HEADER } from "@/lib/workspaces";

export type WorkspaceInviteStatus = "PENDING" | "ACCEPTED" | "EXPIRED";
export type WorkspaceMemberRole = "ADMIN" | "MEMBER";

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

export async function fetchWorkspaceInvite(token: string): Promise<WorkspaceInvite> {
  return apiFetch<WorkspaceInvite>(`/api/v1/workspace-invites/${encodeURIComponent(token)}`);
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
