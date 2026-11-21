import assert from "node:assert/strict";
import test from "node:test";
import { createPresenceLiveState, mergePresenceMembers } from "./presence-summary.ts";
import type { WorkspacePresenceMessage } from "./presence.ts";

const snapshotEvent = {
  type: "presence.snapshot",
  workspaceId: 7,
  memberId: null,
  position: null,
  status: null,
  members: [
    {
      memberId: 1,
      position: { left: "dashboard", top: "agents" },
      status: "ACTIVE",
    },
  ],
  message: null,
} satisfies WorkspacePresenceMessage;

const positionEvent = {
  type: "presence.position",
  workspaceId: 7,
  memberId: 1,
  position: { left: "workspace-detail", top: "task-board" },
  status: "ACTIVE",
  members: [],
  message: null,
} satisfies WorkspacePresenceMessage;

test("presence live state replays events after the latest snapshot", () => {
  const state = createPresenceLiveState({
    streamStatus: "open",
    workspaceMemberCount: 3,
    events: [positionEvent, snapshotEvent],
  });

  assert.equal(state.connectionState, "online");
  assert.equal(state.onlineCount, 1);
  assert.equal(state.workspaceMemberCount, 3);
  assert.equal(state.lastEventType, "presence.position");
  assert.deepEqual(state.members[0], {
    memberId: 1,
    position: { left: "workspace-detail", top: "task-board" },
    status: "ACTIVE",
  });
});

test("presence live state applies join and leave events", () => {
  const joinEvent = {
    type: "presence.join",
    workspaceId: 7,
    memberId: 2,
    position: null,
    status: "ACTIVE",
    members: [],
    message: null,
  } satisfies WorkspacePresenceMessage;
  const leaveEvent = {
    type: "presence.leave",
    workspaceId: 7,
    memberId: 1,
    position: null,
    status: null,
    members: [],
    message: null,
  } satisfies WorkspacePresenceMessage;

  const state = createPresenceLiveState({
    streamStatus: "open",
    workspaceMemberCount: 3,
    events: [leaveEvent, joinEvent, snapshotEvent],
  });

  assert.equal(state.onlineCount, 1);
  assert.deepEqual(
    state.members.map((member) => member.memberId),
    [2],
  );
  assert.equal(state.summary, "1/3 online");
});

test("presence live state maps stream status to stable labels", () => {
  assert.equal(
    createPresenceLiveState({ streamStatus: "connecting", workspaceMemberCount: 0, events: [] })
      .connectionState,
    "connecting",
  );
  assert.equal(
    createPresenceLiveState({ streamStatus: "error", workspaceMemberCount: 0, events: [] })
      .connectionState,
    "error",
  );
  assert.equal(
    createPresenceLiveState({ streamStatus: "idle", workspaceMemberCount: 0, events: [] })
      .summary,
    "0/0 online",
  );
});

test("mergePresenceMembers preserves stationary snapshot members after >5 position updates", () => {
  const initialSnapshot: WorkspacePresenceMessage = {
    type: "presence.snapshot",
    workspaceId: 7,
    memberId: null,
    position: null,
    status: null,
    members: [
      { memberId: 1, position: { left: "20%", top: "40%" }, status: "ACTIVE" },
      { memberId: 2, position: { left: "60%", top: "30%" }, status: "ACTIVE" },
      { memberId: 3, position: { left: "75%", top: "70%" }, status: "ACTIVE" },
    ],
    message: null,
  };

  let merged = mergePresenceMembers([], initialSnapshot);
  const recentEvents: WorkspacePresenceMessage[] = [initialSnapshot];

  for (let i = 1; i <= 8; i += 1) {
    const move: WorkspacePresenceMessage = {
      type: "presence.position",
      workspaceId: 7,
      memberId: 1,
      position: { left: `${20 + i}%`, top: "40%" },
      status: "ACTIVE",
      members: [],
      message: null,
    };
    merged = mergePresenceMembers(merged, move);
    recentEvents.unshift(move);
  }

  const slicedDisplayEvents = recentEvents.slice(0, 5);
  const live = createPresenceLiveState({
    streamStatus: "open",
    workspaceMemberCount: 3,
    events: slicedDisplayEvents,
    persistedMembers: merged,
  });

  assert.equal(live.onlineCount, 3);
  assert.deepEqual(
    live.members.map((m) => m.memberId),
    [1, 2, 3],
  );
  assert.deepEqual(live.members[0].position, { left: "28%", top: "40%" });
  assert.deepEqual(live.members[1].position, { left: "60%", top: "30%" });
  assert.deepEqual(live.members[2].position, { left: "75%", top: "70%" });
});
