import { API_BASE_URL, apiFetch } from "@/lib/api-client";

export type WorkspacePresencePosition = {
  left: string;
  top: string;
};

export type WorkspacePresenceMember = {
  memberId: number;
  position: WorkspacePresencePosition | null;
  status: string | null;
};

export type WorkspacePresenceMessageType =
  | "presence.snapshot"
  | "presence.join"
  | "presence.leave"
  | "presence.position"
  | "presence.error";

export type WorkspacePresenceMessage = {
  type: WorkspacePresenceMessageType;
  workspaceId: number;
  memberId: number | null;
  position: WorkspacePresencePosition | null;
  status: string | null;
  members: WorkspacePresenceMember[];
  message: string | null;
};

export type UpdateWorkspacePresencePositionInput = {
  position: WorkspacePresencePosition;
  status?: string;
};

export function workspacePresenceStreamPath(workspaceId: number, accessToken?: string): string {
  const params = new URLSearchParams();
  const normalizedAccessToken = accessToken?.trim();
  if (normalizedAccessToken) {
    params.set("accessToken", normalizedAccessToken);
  }

  const query = params.toString();
  const path = `/api/v1/workspaces/${workspaceId}/presence/stream`;
  return query ? `${path}?${query}` : path;
}

export function workspacePresencePositionPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/presence/me/position`;
}

export function createWorkspacePresenceStream(
  workspaceId: number,
  accessToken: string,
): EventSource {
  return new EventSource(`${API_BASE_URL}${workspacePresenceStreamPath(workspaceId, accessToken)}`);
}

export async function updateWorkspacePresencePosition(
  accessToken: string,
  workspaceId: number,
  input: UpdateWorkspacePresencePositionInput,
): Promise<void> {
  await apiFetch<void>(workspacePresencePositionPath(workspaceId), {
    method: "PATCH",
    accessToken,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });
}

export function parseWorkspacePresenceMessage(data: string): WorkspacePresenceMessage | null {
  try {
    const parsed = JSON.parse(data) as WorkspacePresenceMessage;
    return typeof parsed.type === "string" && typeof parsed.workspaceId === "number"
      ? parsed
      : null;
  } catch {
    return null;
  }
}
