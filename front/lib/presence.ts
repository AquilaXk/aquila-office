import { API_BASE_URL, apiFetch } from "./api/index.ts";

export const MAX_SSE_EVENT_CHARS = 65536;
export const MAX_PRESENCE_MEMBERS = 500;
const NUMERIC_PCT_PATTERN = /^[-+]?(?:\d+(?:\.\d+)?|\.\d+)%?$/;

const VALID_PRESENCE_MESSAGE_TYPES = new Set<WorkspacePresenceMessageType>([
  "presence.snapshot",
  "presence.join",
  "presence.leave",
  "presence.position",
  "presence.error",
]);

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

function normalizeCoordinateToken(value: unknown): string | null {
  if (typeof value === "number") {
    if (!Number.isFinite(value) || value < 0 || value > 100) {
      return null;
    }
    return `${value}%`;
  }
  if (typeof value !== "string") {
    return null;
  }
  const trimmed = value.trim();
  if (!NUMERIC_PCT_PATTERN.test(trimmed)) {
    return null;
  }
  const rawNumber = trimmed.endsWith("%") ? trimmed.slice(0, -1) : trimmed;
  const parsed = Number(rawNumber);
  if (!Number.isFinite(parsed) || parsed < 0 || parsed > 100) {
    return null;
  }
  return trimmed.endsWith("%") ? trimmed : `${parsed}%`;
}

export function sanitizeWorkspacePresencePosition(
  position: unknown,
): WorkspacePresencePosition | null {
  if (!position || typeof position !== "object") {
    return null;
  }
  const candidate = position as Partial<Record<keyof WorkspacePresencePosition, unknown>>;
  const left = normalizeCoordinateToken(candidate.left);
  const top = normalizeCoordinateToken(candidate.top);
  if (left === null || top === null) {
    return null;
  }
  return { left, top };
}

export async function updateWorkspacePresencePosition(
  accessToken: string,
  workspaceId: number,
  input: UpdateWorkspacePresencePositionInput,
): Promise<void> {
  const sanitizedPosition = sanitizeWorkspacePresencePosition(input.position);
  if (!sanitizedPosition) {
    throw new RangeError("Presence position coordinates must be numeric percentages within 0..100");
  }
  await apiFetch<void>(workspacePresencePositionPath(workspaceId), {
    method: "PATCH",
    accessToken,
    json: {
      ...input,
      position: sanitizedPosition,
    },
  });
}

export function parseWorkspacePresenceMessage(data: string): WorkspacePresenceMessage | null {
  if (typeof data !== "string" || data.length === 0 || data.length > MAX_SSE_EVENT_CHARS) {
    return null;
  }

  try {
    const raw = JSON.parse(data) as Partial<WorkspacePresenceMessage> | null;
    if (!raw || typeof raw !== "object") {
      return null;
    }
    if (
      typeof raw.type !== "string" ||
      !VALID_PRESENCE_MESSAGE_TYPES.has(raw.type as WorkspacePresenceMessageType) ||
      typeof raw.workspaceId !== "number" ||
      !Number.isFinite(raw.workspaceId)
    ) {
      return null;
    }

    const rawMembers = Array.isArray(raw.members) ? raw.members : [];
    if (rawMembers.length > MAX_PRESENCE_MEMBERS) {
      return null;
    }

    const sanitizedMembers: WorkspacePresenceMember[] = [];
    for (let i = 0; i < rawMembers.length; i += 1) {
      const m = rawMembers[i];
      if (!m || typeof m !== "object" || typeof m.memberId !== "number" || !Number.isFinite(m.memberId)) {
        continue;
      }
      sanitizedMembers.push({
        memberId: m.memberId,
        position: m.position === null ? null : sanitizeWorkspacePresencePosition(m.position),
        status: typeof m.status === "string" ? m.status : null,
      });
    }

    const sanitizedPosition =
      raw.position === null || raw.position === undefined
        ? null
        : sanitizeWorkspacePresencePosition(raw.position);

    if (raw.type === "presence.position" && raw.position !== null && sanitizedPosition === null) {
      return null;
    }

    return {
      type: raw.type as WorkspacePresenceMessageType,
      workspaceId: raw.workspaceId,
      memberId:
        typeof raw.memberId === "number" && Number.isFinite(raw.memberId) ? raw.memberId : null,
      position: sanitizedPosition,
      status: typeof raw.status === "string" ? raw.status : null,
      members: sanitizedMembers,
      message: typeof raw.message === "string" ? raw.message : null,
    };
  } catch {
    return null;
  }
}

export type PresencePositionScheduler = {
  schedule: (position: WorkspacePresencePosition) => Promise<void>;
  isInFlight: () => boolean;
};

export function createPresencePositionScheduler(
  sendFn: (position: WorkspacePresencePosition) => Promise<void>,
): PresencePositionScheduler {
  let inFlight = false;
  let pendingPosition: WorkspacePresencePosition | null = null;
  let activeChain: Promise<void> | null = null;

  async function runLoop(initialPosition: WorkspacePresencePosition): Promise<void> {
    inFlight = true;
    let current: WorkspacePresencePosition | null = initialPosition;
    try {
      while (current !== null) {
        const target = current;
        pendingPosition = null;
        await sendFn(target);
        current = pendingPosition;
      }
    } finally {
      inFlight = false;
      pendingPosition = null;
      activeChain = null;
    }
  }

  return {
    schedule(position: WorkspacePresencePosition): Promise<void> {
      const sanitized = sanitizeWorkspacePresencePosition(position);
      if (!sanitized) {
        return Promise.resolve();
      }
      if (inFlight && activeChain) {
        pendingPosition = sanitized;
        return activeChain;
      }
      activeChain = runLoop(sanitized);
      return activeChain;
    },
    isInFlight(): boolean {
      return inFlight;
    },
  };
}
