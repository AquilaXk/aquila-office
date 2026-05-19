import { apiFetch } from "./index.ts";
import { resolveAccessToken } from "../auth.ts";
import { type WorkspaceMemberRole } from "../workspaces.ts";

export type WorkspaceInviteStatus = "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";
export type WorkspaceInviteManagementStatus = WorkspaceInviteStatus;

export type WorkspaceInvite = {
  inviteId: number;
  workspaceId: number;
  workspaceName?: string;
  targetEmail?: string | null;
  inviteUrl?: string | null;
  emailStatus?: string | null;
  role?: WorkspaceMemberRole;
  token: string;
  status: WorkspaceInviteStatus;
  createdAt: string;
  expiresAt: string;
  expired?: boolean;
  acceptedAt: string | null;
};

export type InviteInfo = WorkspaceInvite & {
  workspaceName: string;
  role: WorkspaceMemberRole;
  expired: boolean;
};

export type WorkspaceInviteManagement = {
  inviteId: number;
  workspaceId: number;
  targetEmail?: string | null;
  inviteUrl?: string | null;
  emailStatus?: string | null;
  role?: WorkspaceMemberRole;
  token: string;
  status: WorkspaceInviteManagementStatus;
  createdAt: string;
  expiresAt: string;
  acceptedAt: string | null;
  revokedAt: string | null;
};

export type InviteStatus = WorkspaceInviteStatus;
export type InviteManagement = WorkspaceInviteManagement & {
  role: WorkspaceMemberRole;
  emailStatus: string;
};

export type AcceptedWorkspaceInvite = {
  inviteId: number;
  workspaceId: number;
  memberGoogleSub: string;
  role: WorkspaceMemberRole;
  acceptedAt: string;
};

export type CreateWorkspaceInviteInput = {
  targetEmail?: string;
  role?: WorkspaceMemberRole;
  expiresInDays?: number;
};

export type ExtendWorkspaceInviteInput = {
  additionalDays?: number | null;
};

export async function fetchWorkspaceInvite(token: string): Promise<WorkspaceInvite> {
  return apiFetch<WorkspaceInvite>(`/api/v1/invites/${encodeURIComponent(token)}`);
}

export async function getInviteInfo(token: string): Promise<InviteInfo> {
  const invite = await fetchWorkspaceInvite(token);
  return {
    ...invite,
    workspaceName: invite.workspaceName ?? `Workspace #${invite.workspaceId}`,
    role: invite.role ?? "MEMBER",
    expired: invite.expired ?? isInviteExpired(invite.expiresAt),
  };
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

export async function listSentInvites(
  workspaceId: number,
  status?: WorkspaceInviteManagementStatus,
): Promise<InviteManagement[]> {
  const invites = await fetchWorkspaceInvites(resolveAccessToken(), workspaceId, status);
  return invites.map(normalizeInviteManagement);
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

export async function createInviteLink(
  workspaceId: number,
  input: CreateWorkspaceInviteInput,
): Promise<WorkspaceInvite> {
  return createWorkspaceInvite(resolveAccessToken(), workspaceId, input);
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

export async function acceptInvite(token: string): Promise<AcceptedWorkspaceInvite> {
  return acceptWorkspaceInvite(token, resolveAccessToken());
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

export async function deleteInvite(workspaceId: number, inviteId: number): Promise<void> {
  return deleteWorkspaceInvite(resolveAccessToken(), workspaceId, inviteId);
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

export async function extendInvite(
  workspaceId: number,
  inviteId: number,
  additionalDays: number,
): Promise<InviteManagement> {
  const invite = await extendWorkspaceInvite(resolveAccessToken(), workspaceId, inviteId, {
    additionalDays,
  });
  return normalizeInviteManagement(invite);
}

function normalizeInviteManagement(invite: WorkspaceInviteManagement): InviteManagement {
  return {
    ...invite,
    role: invite.role ?? "MEMBER",
    emailStatus: invite.emailStatus ?? "NOT_REQUESTED",
  };
}

function isInviteExpired(expiresAt: string): boolean {
  const date = new Date(expiresAt);
  return !Number.isNaN(date.getTime()) && date.getTime() <= Date.now();
}
