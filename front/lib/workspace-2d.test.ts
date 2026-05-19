import assert from "node:assert/strict";
import test from "node:test";
import {
  WORKSPACE_SCENE_EVENT_TYPES,
  createWorkspaceAvatarNodes,
  createWorkspaceStageActors,
  createWorkspaceContextMenuState,
  createInitialWorkspaceViewport,
  getEngineeringRows,
  createWorkspaceSceneEventBus,
  buildStageWalls,
  clampStageZoom,
  getPlayerMoveDirection,
  getStageRooms,
  getStagePanForPosition,
  getStageWorldSize,
  getWorkspaceActorSpeech,
  isNearQuestMarker,
  mapWorkspaceAgentStatusToActorStatus,
  panWorkspaceViewport,
  resolveStageMove,
  smoothStagePan,
  zoomWorkspaceViewport,
  type StageRoomSpec,
} from "./workspace-2d.ts";

test("workspace scene event bus dispatches and unsubscribes typed events", () => {
  const bus = createWorkspaceSceneEventBus();
  const received: number[] = [];

  const unsubscribe = bus.on("avatar.select", (event) => {
    received.push(event.agentId);
  });

  bus.emit({ type: "avatar.select", agentId: 17 });
  unsubscribe();
  bus.emit({ type: "avatar.select", agentId: 23 });

  assert.deepEqual(received, [17]);
  assert.ok(WORKSPACE_SCENE_EVENT_TYPES.includes("viewport.change"));
});

test("workspace viewport helpers clamp pan and zoom inside the scene bounds", () => {
  const viewport = createInitialWorkspaceViewport({
    width: 960,
    height: 540,
    worldWidth: 1600,
    worldHeight: 960,
  });

  const panned = panWorkspaceViewport(viewport, { deltaX: 900, deltaY: 700 });
  const zoomed = zoomWorkspaceViewport(panned, { factor: 3, originX: 480, originY: 270 });
  const reset = zoomWorkspaceViewport(zoomed, { factor: 0.1, originX: 0, originY: 0 });

  assert.deepEqual(panned, {
    width: 960,
    height: 540,
    worldWidth: 1600,
    worldHeight: 960,
    x: 640,
    y: 420,
    zoom: 1,
  });
  assert.equal(zoomed.zoom, 2);
  assert.ok(zoomed.x <= 1120);
  assert.ok(zoomed.y <= 690);
  assert.equal(reset.zoom, 0.75);
  assert.ok(reset.x >= 0);
  assert.ok(reset.y >= 0);
});

test("workspace avatar nodes preserve agent and member identity for the scene", () => {
  const nodes = createWorkspaceAvatarNodes({
    agents: [
      {
        agentId: 7,
        name: "Backend Executor",
        category: "BACKEND",
        status: "READY",
      },
    ],
    members: [
      {
        memberId: 11,
        name: "Kim Admin",
        email: "kim@example.com",
        role: "ADMIN",
      },
    ],
    presenceMembers: [
      {
        memberId: 11,
        position: { left: "320", top: "160" },
        status: "ACTIVE",
      },
    ],
  });

  assert.deepEqual(nodes, [
    {
      id: "agent-7",
      kind: "agent",
      label: "Backend Executor",
      detail: "BACKEND · READY",
      status: "READY",
      x: 180,
      y: 150,
    },
    {
      id: "member-11",
      kind: "member",
      label: "Kim Admin",
      detail: "kim@example.com · ADMIN",
      status: "ACTIVE",
      x: 320,
      y: 160,
    },
  ]);
});

test("workspace context menu state exposes node actions at the requested screen point", () => {
  const menu = createWorkspaceContextMenuState({
    nodeId: "agent-7",
    kind: "agent",
    screenX: 48,
    screenY: 96,
  });

  assert.deepEqual(menu, {
    nodeId: "agent-7",
    kind: "agent",
    screenX: 48,
    screenY: 96,
    actions: ["open-profile", "focus-node", "start-chat"],
  });
});

test("workspace stage helpers clamp zoom and follow the player position", () => {
  assert.equal(clampStageZoom(0.1), 0.75);
  assert.equal(clampStageZoom(1.23456), 1.235);
  assert.equal(clampStageZoom(2), 1.5);

  assert.deepEqual(
    getStagePanForPosition({ left: "75%", top: "25%" }, { width: 1200, height: 800 }, 1.25),
    { x: -375, y: 250 },
  );
  assert.deepEqual(
    smoothStagePan({ x: 0, y: 0 }, { x: -100, y: 50 }, 0.2),
    { x: -20, y: 10 },
  );
});

test("workspace stage movement respects player bounds and room walls", () => {
  const rooms: StageRoomSpec[] = [
    {
      label: "Engineering",
      left: "20%",
      top: "20%",
      width: "30%",
      height: "30%",
      variant: "engineering",
      doorSide: "top",
      doorAt: 0.5,
    },
  ];
  const walls = buildStageWalls(rooms);

  assert.deepEqual(
    resolveStageMove({ left: "4%", top: "9%" }, -10, -10, walls),
    { left: "3%", top: "8%" },
  );
  assert.deepEqual(
    resolveStageMove({ left: "49%", top: "35%" }, 5, 0, walls),
    { left: "49.4%", top: "35%" },
  );
  assert.deepEqual(getPlayerMoveDirection("ArrowLeft"), { x: -1, y: 0 });
  assert.deepEqual(getPlayerMoveDirection("w"), { x: 0, y: -1 });
  assert.equal(getPlayerMoveDirection("Escape"), null);
});

test("workspace stage quest marker matches interaction radius", () => {
  assert.equal(isNearQuestMarker({ left: "90.4%", top: "75.8%" }), true);
  assert.equal(isNearQuestMarker({ left: "10%", top: "10%" }), false);
});

test("workspace stage actors match office placement, status, and speech contract", () => {
  const rows = getEngineeringRows(2);
  const worldSize = getStageWorldSize(rows);
  const rooms = getStageRooms(rows, worldSize, 2);
  const actors = createWorkspaceStageActors({
    agents: [
      {
        agentId: 7,
        name: "Backend Executor",
        category: "BACKEND",
        status: "READY",
      },
      {
        agentId: 8,
        name: "Review Bot",
        category: "QA",
        status: "ERROR",
      },
    ],
    members: [
      {
        memberId: 11,
        name: "Kim Admin",
        email: "kim@example.com",
        role: "ADMIN",
      },
    ],
    presenceMembers: [
      {
        memberId: 11,
        position: { left: "45.1%", top: "23.5%" },
        status: "ACTIVE",
      },
    ],
    rooms,
    worldSize,
  });

  assert.equal(mapWorkspaceAgentStatusToActorStatus("READY"), "idle");
  assert.equal(mapWorkspaceAgentStatusToActorStatus("CREATING"), "working");
  assert.equal(mapWorkspaceAgentStatusToActorStatus("ERROR"), "blocked");

  assert.deepEqual(actors.map((actor) => actor.id), ["agent-7", "agent-8", "member-11"]);
  assert.equal(actors[0].avatarKind, "agent");
  assert.equal(actors[0].status, "idle");
  assert.equal(actors[1].status, "blocked");
  assert.deepEqual(getWorkspaceActorSpeech(actors[1]), {
    label: "확인 필요",
    text: "",
    tone: "blocked",
  });
  assert.equal(actors[2].avatarKind, "mira");
  assert.deepEqual(actors[2].position, { left: "45.1%", top: "23.5%" });
});
