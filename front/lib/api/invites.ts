import { apiFetch } from "./index.ts";
import { type WorkspaceMemberRole } from "../workspaces.ts";

export type WorkspaceInviteStatus = "PENDING" | "ACCEPTED" | "EXPIRED";
export type WorkspaceInviteManagementStatus = WorkspaceInviteStatus | "REVOKED";

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

export type WorkspaceInviteManagement = {
  inviteId: number;
  workspaceId: number;
  email: string;
  token: string;
  status: WorkspaceInviteManagementStatus;
  createdAt: string;
  expiresAt: string;
  acceptedAt: string | null;
  revokedAt: string | null;
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
  mode?: "email" | "link";
  role?: WorkspaceMemberRole;
  expiresInDays?: number;
};

export type ExtendWorkspaceInviteInput = {
  additionalDays?: number | null;
};

export async function fetchWorkspaceInvite(token: string): Promise<WorkspaceInvite> {
  return apiFetch<WorkspaceInvite>(`/api/v1/invites/${encodeURIComponent(token)}`);
}

export async function fetchWorkspaceInvites(
  accessToken: string,
  workspaceId: number,
  status?: WorkspaceInviteManagementStatus,
): Promise<WorkspaceInviteManagement[]> {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return apiFetch<WorkspaceInviteManagement[]>(`/api/v1/workspaces/${workspaceId}/invites${query}`, {
    accessToken,
  });
}

export async function createWorkspaceInvite(
  accessToken: string,
  workspaceId: number,
  input: CreateWorkspaceInviteInput,
): Promise<WorkspaceInvite> {
  return apiFetch<WorkspaceInvite>(`/api/v1/workspaces/${workspaceId}/invites`, {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function acceptWorkspaceInvite(
  token: string,
  accessToken: string,
): Promise<AcceptedWorkspaceInvite> {
  return apiFetch<AcceptedWorkspaceInvite>(`/api/v1/invites/${encodeURIComponent(token)}/accept`, {
    method: "POST",
    accessToken,
  });
}

export async function revokeWorkspaceInvite(
  accessToken: string,
  workspaceId: number,
  inviteId: number,
): Promise<void> {
  return deleteWorkspaceInvite(accessToken, workspaceId, inviteId);
}

export async function deleteWorkspaceInvite(
  accessToken: string,
  workspaceId: number,
  inviteId: number,
): Promise<void> {
  return apiFetch<void>(`/api/v1/workspaces/${workspaceId}/invites/${inviteId}`, {
    method: "DELETE",
    accessToken,
  });
}

export async function extendWorkspaceInvite(
  accessToken: string,
  workspaceId: number,
  inviteId: number,
  input: ExtendWorkspaceInviteInput = {},
): Promise<WorkspaceInviteManagement> {
  return apiFetch<WorkspaceInviteManagement>(
    `/api/v1/workspaces/${workspaceId}/invites/${inviteId}/extend`,
    {
      method: "PATCH",
      accessToken,
      json: input,
    },
  );
}
