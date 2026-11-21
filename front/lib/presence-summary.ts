import { MAX_PRESENCE_MEMBERS } from "./presence.ts";

export type PresenceSummaryStreamStatus = "idle" | "connecting" | "open" | "error";

export type PresenceConnectionState = "idle" | "connecting" | "online" | "error";

export type PresencePosition = {
  left: string;
  top: string;
};

export type PresenceMember = {
  memberId: number;
  position: PresencePosition | null;
  status: string | null;
};

export type PresenceMessageType =
  | "presence.snapshot"
  | "presence.join"
  | "presence.leave"
  | "presence.position"
  | "presence.error";

export type PresenceMessage = {
  type: PresenceMessageType;
  memberId: number | null;
  position: PresencePosition | null;
  status: string | null;
  members: PresenceMember[];
};

export type PresenceLiveStateInput = {
  streamStatus: PresenceSummaryStreamStatus;
  workspaceMemberCount: number;
  events: PresenceMessage[];
  persistedMembers?: readonly PresenceMember[];
};

export type PresenceLiveState = {
  connectionState: PresenceConnectionState;
  onlineCount: number;
  workspaceMemberCount: number;
  lastEventType: PresenceMessageType | null;
  summary: string;
  members: PresenceMember[];
};

export function mergePresenceMembers(
  currentMembers: readonly PresenceMember[],
  event: PresenceMessage,
): PresenceMember[] {
  const membersById = new Map<number, PresenceMember>();
  if (event.type === "presence.snapshot") {
    if (event.members.length > MAX_PRESENCE_MEMBERS) {
      return [...currentMembers];
    }
    for (let i = 0; i < event.members.length; i += 1) {
      const member = event.members[i];
      membersById.set(member.memberId, {
        memberId: member.memberId,
        position: member.position,
        status: member.status,
      });
    }
  } else {
    for (let i = 0; i < currentMembers.length; i += 1) {
      const member = currentMembers[i];
      membersById.set(member.memberId, member);
    }
    if (
      event.memberId !== null &&
      event.type !== "presence.error" &&
      (event.type === "presence.leave" ||
        membersById.has(event.memberId) ||
        membersById.size < MAX_PRESENCE_MEMBERS)
    ) {
      applyPresenceEvent(membersById, event);
    }
  }
  return [...membersById.values()].sort((first, second) => first.memberId - second.memberId);
}

export function createPresenceLiveState(input: PresenceLiveStateInput): PresenceLiveState {
  const members = input.persistedMembers
    ? [...input.persistedMembers]
    : replayPresenceMembers(input.events);

  return {
    connectionState: connectionStateFor(input.streamStatus),
    onlineCount: members.length,
    workspaceMemberCount: input.workspaceMemberCount,
    lastEventType: input.events[0]?.type ?? null,
    summary: `${members.length}/${input.workspaceMemberCount} online`,
    members,
  };
}

function replayPresenceMembers(events: PresenceMessage[]): PresenceMember[] {
  const snapshotIndex = events.findIndex((event) => event.type === "presence.snapshot");
  const membersById = new Map<number, PresenceMember>();

  if (snapshotIndex >= 0) {
    for (const member of events[snapshotIndex]?.members ?? []) {
      membersById.set(member.memberId, {
        memberId: member.memberId,
        position: member.position,
        status: member.status,
      });
    }
  }

  const startIndex = snapshotIndex >= 0 ? snapshotIndex - 1 : events.length - 1;
  for (let i = startIndex; i >= 0; i--) {
    applyPresenceEvent(membersById, events[i]);
  }

  return [...membersById.values()].sort((first, second) => first.memberId - second.memberId);
}

function applyPresenceEvent(
  membersById: Map<number, PresenceMember>,
  event: PresenceMessage,
): void {
  if (event.memberId === null || event.type === "presence.snapshot" || event.type === "presence.error") {
    return;
  }

  if (event.type === "presence.leave") {
    membersById.delete(event.memberId);
    return;
  }

  const previous = membersById.get(event.memberId);
  membersById.set(event.memberId, {
    memberId: event.memberId,
    position: event.position ?? previous?.position ?? null,
    status: event.status ?? previous?.status ?? "ACTIVE",
  });
}

function connectionStateFor(status: PresenceSummaryStreamStatus): PresenceConnectionState {
  switch (status) {
    case "open":
      return "online";
    case "connecting":
      return "connecting";
    case "error":
      return "error";
    case "idle":
      return "idle";
  }
}
