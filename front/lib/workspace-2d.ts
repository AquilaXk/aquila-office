export const WORKSPACE_SCENE_EVENT_TYPES = [
  "avatar.select",
  "avatar.context",
  "viewport.change",
] as const;

export type WorkspaceSceneEventType = (typeof WORKSPACE_SCENE_EVENT_TYPES)[number];

export type WorkspaceViewport = {
  width: number;
  height: number;
  worldWidth: number;
  worldHeight: number;
  x: number;
  y: number;
  zoom: number;
};

export type WorkspaceViewportInput = {
  width: number;
  height: number;
  worldWidth: number;
  worldHeight: number;
};

export type WorkspacePanInput = {
  deltaX: number;
  deltaY: number;
};

export type WorkspaceZoomInput = {
  factor: number;
  originX: number;
  originY: number;
};

export type WorkspaceSceneNodeKind = "agent" | "member";
export type WorkspaceSceneZoneKind = "room" | "desk";

export type WorkspaceSceneNode = {
  id: string;
  kind: WorkspaceSceneNodeKind;
  label: string;
  detail: string;
  status: string;
  x: number;
  y: number;
};

export type WorkspaceSceneZone = {
  id: string;
  kind: WorkspaceSceneZoneKind;
  label: string;
  x: number;
  y: number;
  width: number;
  height: number;
};

export type WorkspaceAvatarAgent = {
  agentId: number;
  name: string;
  category: string;
  status: string;
};

export type WorkspaceAvatarMember = {
  memberId: number;
  name: string;
  email: string;
  role: string;
};

export type WorkspaceAvatarPresenceMember = {
  memberId: number;
  position: { left: string; top: string } | null;
  status: string | null;
};

export type WorkspaceAvatarNodesInput = {
  agents: WorkspaceAvatarAgent[];
  members: WorkspaceAvatarMember[];
  presenceMembers: WorkspaceAvatarPresenceMember[];
};

export type WorkspaceContextMenuAction = "open-profile" | "focus-node" | "start-chat";

export type WorkspaceContextMenuInput = {
  nodeId: string;
  kind: WorkspaceSceneNodeKind;
  screenX: number;
  screenY: number;
};

export type WorkspaceContextMenuState = WorkspaceContextMenuInput & {
  actions: WorkspaceContextMenuAction[];
};

export type WorkspaceSceneEvent =
  | { type: "avatar.select"; agentId: number }
  | { type: "avatar.context"; agentId: number; x: number; y: number }
  | { type: "viewport.change"; viewport: WorkspaceViewport };

export type WorkspaceSceneEventOf<Type extends WorkspaceSceneEventType> = Extract<
  WorkspaceSceneEvent,
  { type: Type }
>;

export type WorkspaceSceneEventBus = {
  on<Type extends WorkspaceSceneEventType>(
    type: Type,
    listener: (event: WorkspaceSceneEventOf<Type>) => void,
  ): () => void;
  emit(event: WorkspaceSceneEvent): void;
  clear(): void;
};

const MIN_ZOOM = 0.75;
const MAX_ZOOM = 2;
const KEYBOARD_PAN_STEP = 96;

export type StageActorPosition = {
  left: string;
  top: string;
};

export type StagePan = {
  x: number;
  y: number;
};

export type StageSize = {
  width: number;
  height: number;
};

export type StageRoomSpec = {
  label: string;
  sub?: string;
  left: string;
  top: string;
  width: string;
  height: string;
  color?: string;
  variant: "engineering" | "huddle" | "rest" | "library" | "planning" | "review";
  doorSide: "top" | "right" | "bottom" | "left";
  doorAt: number;
};

export type WorkspaceAvatarKind =
  | "alex"
  | "mira"
  | "kenji"
  | "yuna"
  | "diego"
  | "iris"
  | "agent";

export type WorkspaceStageActorStatus = "working" | "idle" | "review" | "blocked";

export type WorkspaceStageActorMotion = "working" | "idle" | "talking" | "walking";

export type WorkspaceStageActor = {
  id: string;
  kind: WorkspaceSceneNodeKind;
  label: string;
  detail: string;
  sourceStatus: string;
  status: WorkspaceStageActorStatus;
  motion: WorkspaceStageActorMotion;
  position: StageActorPosition;
  avatarKind: WorkspaceAvatarKind;
};

export type WorkspaceStageActorsInput = WorkspaceAvatarNodesInput & {
  rooms?: StageRoomSpec[];
  worldSize?: StageSize;
};

export type WorkspaceActorSpeech = {
  label: string;
  text: string;
  tone: WorkspaceStageActorStatus;
};

export type WallSegment = {
  axis: "h" | "v";
  fixed: number;
  start: number;
  end: number;
};

const STAGE_ZOOM_MIN = 0.75;
const STAGE_ZOOM_MAX = 1.5;
const PLAYER_STAGE_BOUNDS = {
  minX: 3,
  maxX: 96,
  minY: 8,
  maxY: 91,
};
const QUEST_MARKER_POSITION: StageActorPosition = { left: "90.4%", top: "75.8%" };
const QUEST_INTERACTION_RADIUS_PCT = 6.5;
const WALL_DOOR_HALF_PCT = 5;
const PLAYER_WALL_PADDING_PCT = 0.6;
const AGENT_DESK_COL_CENTERS = [16, 33, 50, 67];
const STAGE_WORLD_WIDTH = 1800;
const STAGE_WORLD_MIN_HEIGHT = 950;
const ENGINEERING_X = 40;
const ENGINEERING_Y = 90;
const ENGINEERING_W = 620;
const HUDDLE_X = 710;
const HUDDLE_Y = 90;
const HUDDLE_W = 390;
const HUDDLE_H = 260;
const LOWER_ZONE_GAP = 100;
const LOUNGE_X = 40;
const LOUNGE_W = 620;
const LOUNGE_H = 320;
const LIBRARY_X = 710;
const LIBRARY_W = 1040;
const LIBRARY_H = 430;
const AGENT_LOUNGE_POSITIONS = [
  { x: 20, y: 70 },
  { x: 42, y: 58 },
  { x: 64, y: 68 },
  { x: 76, y: 42 },
  { x: 30, y: 36 },
  { x: 52, y: 80 },
];
const MEMBER_POSITIONS: StageActorPosition[] = [
  { left: "36.8%", top: "54%" },
  { left: "45.1%", top: "23.5%" },
  { left: "54.2%", top: "31.2%" },
  { left: "14.6%", top: "68.5%" },
  { left: "20.8%", top: "68.5%" },
  { left: "58.8%", top: "59.5%" },
  { left: "72.0%", top: "59.5%" },
  { left: "8.6%", top: "80%" },
];
const MEMBER_AVATAR_KINDS: WorkspaceAvatarKind[] = ["mira", "alex", "kenji", "yuna", "diego", "iris"];

export function clampStageZoom(value: number): number {
  return Math.min(STAGE_ZOOM_MAX, Math.max(STAGE_ZOOM_MIN, Number(value.toFixed(3))));
}

export function pct(value: number): string {
  return `${Number(value.toFixed(2))}%`;
}

export function parseStagePct(value: string): number {
  const parsed = Number.parseFloat(value);
  return Number.isFinite(parsed) ? parsed : 50;
}

export function getEngineeringRows(agentCount: number): number {
  return Math.max(2, Math.ceil(Math.max(agentCount, 1) / AGENT_DESK_COL_CENTERS.length));
}

function getEngineeringHeightPx(rows: number): number {
  return 320 + Math.max(0, rows - 2) * 130;
}

export function getStageWorldSize(engineeringRows: number): StageSize {
  const lowerTop = ENGINEERING_Y + getEngineeringHeightPx(engineeringRows) + LOWER_ZONE_GAP;
  return {
    width: STAGE_WORLD_WIDTH,
    height: Math.max(STAGE_WORLD_MIN_HEIGHT, lowerTop + Math.max(LOUNGE_H, LIBRARY_H) + 90),
  };
}

function pxToStagePct(value: number, total: number): string {
  return pct((value / total) * 100);
}

export function getStageRooms(
  engineeringRows: number,
  worldSize: StageSize,
  agentCount: number,
): StageRoomSpec[] {
  const engineeringHeight = getEngineeringHeightPx(engineeringRows);
  const lowerTop = ENGINEERING_Y + engineeringHeight + LOWER_ZONE_GAP;
  return [
    {
      label: "ENGINEERING",
      sub: `${agentCount} agents · ${engineeringRows} rows`,
      left: pxToStagePct(ENGINEERING_X, worldSize.width),
      top: pxToStagePct(ENGINEERING_Y, worldSize.height),
      width: pxToStagePct(ENGINEERING_W, worldSize.width),
      height: pxToStagePct(engineeringHeight, worldSize.height),
      variant: "engineering",
      doorSide: "right",
      doorAt: 0.72,
    },
    {
      label: "HUDDLE",
      sub: "meeting in progress",
      left: pxToStagePct(HUDDLE_X, worldSize.width),
      top: pxToStagePct(HUDDLE_Y, worldSize.height),
      width: pxToStagePct(HUDDLE_W, worldSize.width),
      height: pxToStagePct(HUDDLE_H, worldSize.height),
      variant: "huddle",
      doorSide: "bottom",
      doorAt: 0.5,
    },
    {
      label: "LOUNGE",
      sub: "rest zone",
      left: pxToStagePct(LOUNGE_X, worldSize.width),
      top: pxToStagePct(lowerTop, worldSize.height),
      width: pxToStagePct(LOUNGE_W, worldSize.width),
      height: pxToStagePct(LOUNGE_H, worldSize.height),
      variant: "rest",
      doorSide: "top",
      doorAt: 0.44,
    },
    {
      label: "LIBRARY",
      sub: "quiet zone",
      left: pxToStagePct(LIBRARY_X, worldSize.width),
      top: pxToStagePct(lowerTop, worldSize.height),
      width: pxToStagePct(LIBRARY_W, worldSize.width),
      height: pxToStagePct(LIBRARY_H, worldSize.height),
      variant: "library",
      doorSide: "left",
      doorAt: 0.25,
    },
  ];
}

function getDeskRowCenters(rows: number): number[] {
  if (rows <= 1) {
    return [55];
  }
  if (rows === 2) {
    return [38.5, 67.5];
  }
  const start = 28;
  const end = 82;
  const step = (end - start) / (rows - 1);
  return Array.from({ length: rows }, (_, index) => start + index * step);
}

function getDeskHeightPct(rows: number): number {
  if (rows <= 2) {
    return 15;
  }
  return Math.max(8, Math.min(15, 60 / rows));
}

export function getAgentPositionInStage(
  agentIndex: number,
  rows: number,
  room: StageRoomSpec,
): StageActorPosition {
  const columnIndex = agentIndex % AGENT_DESK_COL_CENTERS.length;
  const rowIndex = Math.floor(agentIndex / AGENT_DESK_COL_CENTERS.length);
  const roomLeft = parseStagePct(room.left);
  const roomTop = parseStagePct(room.top);
  const roomWidth = parseStagePct(room.width);
  const roomHeight = parseStagePct(room.height);
  const deskCenterX = AGENT_DESK_COL_CENTERS[columnIndex];
  const rowCenters = getDeskRowCenters(rows);
  const deskCenterY = rowCenters[rowIndex] ?? rowCenters[rowCenters.length - 1];
  const avatarFootY = deskCenterY - getDeskHeightPct(rows) * 0.3;

  return {
    left: pct(roomLeft + (deskCenterX / 100) * roomWidth),
    top: pct(roomTop + (avatarFootY / 100) * roomHeight),
  };
}

export function getAgentLoungePosition(agentIndex: number, room: StageRoomSpec): StageActorPosition {
  const slot = AGENT_LOUNGE_POSITIONS[agentIndex % AGENT_LOUNGE_POSITIONS.length];
  const round = Math.floor(agentIndex / AGENT_LOUNGE_POSITIONS.length);
  const roomLeft = parseStagePct(room.left);
  const roomTop = parseStagePct(room.top);
  const roomWidth = parseStagePct(room.width);
  const roomHeight = parseStagePct(room.height);
  const offset = (round % 2) * 4;

  return {
    left: pct(roomLeft + ((slot.x + offset) / 100) * roomWidth),
    top: pct(roomTop + ((slot.y + offset) / 100) * roomHeight),
  };
}

export function mapWorkspaceAgentStatusToActorStatus(status: string): WorkspaceStageActorStatus {
  switch (status) {
    case "CREATING":
      return "working";
    case "DISABLED":
      return "review";
    case "ERROR":
    case "SYNC_FAILED":
      return "blocked";
    case "READY":
    default:
      return "idle";
  }
}

function mapWorkspaceMemberStatusToActorStatus(status: string | null | undefined): WorkspaceStageActorStatus {
  return status === "ACTIVE" ? "idle" : "review";
}

function pickWorkspaceAvatarKind(name: string, agent = false): WorkspaceAvatarKind {
  if (agent) {
    return "agent";
  }
  let hash = 0;
  for (let index = 0; index < name.length; index += 1) {
    hash = (hash * 31 + name.charCodeAt(index)) >>> 0;
  }
  return MEMBER_AVATAR_KINDS[hash % MEMBER_AVATAR_KINDS.length];
}

function getWorkspaceActorMotion(status: WorkspaceStageActorStatus): WorkspaceStageActorMotion {
  if (status === "working") {
    return "working";
  }
  if (status === "review" || status === "blocked") {
    return "talking";
  }
  return "idle";
}

export function getWorkspaceActorSpeech(actor: WorkspaceStageActor): WorkspaceActorSpeech | null {
  if (actor.kind !== "agent") {
    return null;
  }
  if (actor.status === "working") {
    return { label: "진행중", text: "", tone: "working" };
  }
  if (actor.status === "review") {
    return { label: "리뷰", text: "", tone: "review" };
  }
  if (actor.status === "blocked") {
    return { label: "확인 필요", text: "", tone: "blocked" };
  }
  return null;
}

export function createWorkspaceStageActors(input: WorkspaceStageActorsInput): WorkspaceStageActor[] {
  const rows = getEngineeringRows(input.agents.length);
  const worldSize = input.worldSize ?? getStageWorldSize(rows);
  const rooms = input.rooms?.length ? input.rooms : getStageRooms(rows, worldSize, input.agents.length);
  const engineeringRoom = rooms.find((room) => room.variant === "engineering") ?? rooms[0];
  const restRoom = rooms.find((room) => room.variant === "rest") ?? null;
  const presenceByMemberId = new Map(
    input.presenceMembers.map((member) => [member.memberId, member]),
  );

  const agentActors = input.agents.map<WorkspaceStageActor>((agent, index) => {
    const status = mapWorkspaceAgentStatusToActorStatus(agent.status);
    const position =
      status === "review" && restRoom
        ? getAgentLoungePosition(index, restRoom)
        : getAgentPositionInStage(index, rows, engineeringRoom);
    return {
      id: `agent-${agent.agentId}`,
      kind: "agent",
      label: agent.name,
      detail: `${agent.category} · ${agent.status}`,
      sourceStatus: agent.status,
      status,
      motion: getWorkspaceActorMotion(status),
      position,
      avatarKind: "agent",
    };
  });

  const memberActors = input.members.map<WorkspaceStageActor>((member, index) => {
    const presence = presenceByMemberId.get(member.memberId);
    const status = mapWorkspaceMemberStatusToActorStatus(presence?.status);
    return {
      id: `member-${member.memberId}`,
      kind: "member",
      label: member.name,
      detail: `${member.email} · ${member.role}`,
      sourceStatus: presence?.status ?? "OFFLINE",
      status,
      motion: getWorkspaceActorMotion(status),
      position: presence?.position ?? MEMBER_POSITIONS[index % MEMBER_POSITIONS.length],
      avatarKind: pickWorkspaceAvatarKind(member.name),
    };
  });

  return [...agentActors, ...memberActors];
}

export function getWorkspaceActorScreenPosition(
  position: StageActorPosition,
  viewport: WorkspaceViewport,
): { left: number; top: number } {
  const worldX = (parseStagePct(position.left) / 100) * viewport.worldWidth;
  const worldY = (parseStagePct(position.top) / 100) * viewport.worldHeight;
  return {
    left: (worldX - viewport.x) * viewport.zoom,
    top: (worldY - viewport.y) * viewport.zoom,
  };
}

export function buildStageWalls(rooms: StageRoomSpec[]): WallSegment[] {
  const walls: WallSegment[] = [];
  for (const room of rooms) {
    const left = parseStagePct(room.left);
    const top = parseStagePct(room.top);
    const width = parseStagePct(room.width);
    const height = parseStagePct(room.height);
    const right = left + width;
    const bottom = top + height;

    pushWallSide(walls, "h", top, left, right, width, room.doorSide === "top" ? room.doorAt : null);
    pushWallSide(
      walls,
      "h",
      bottom,
      left,
      right,
      width,
      room.doorSide === "bottom" ? room.doorAt : null,
    );
    pushWallSide(walls, "v", left, top, bottom, height, room.doorSide === "left" ? room.doorAt : null);
    pushWallSide(
      walls,
      "v",
      right,
      top,
      bottom,
      height,
      room.doorSide === "right" ? room.doorAt : null,
    );
  }
  return walls;
}

function pushWallSide(
  walls: WallSegment[],
  axis: "h" | "v",
  fixed: number,
  start: number,
  end: number,
  span: number,
  doorAt: number | null,
) {
  if (doorAt == null) {
    walls.push({ axis, fixed, start, end });
    return;
  }
  const doorMid = start + span * doorAt;
  const half = (span * WALL_DOOR_HALF_PCT) / 100;
  const before = { axis, fixed, start, end: doorMid - half };
  const after = { axis, fixed, start: doorMid + half, end };
  if (before.end > before.start) {
    walls.push(before);
  }
  if (after.end > after.start) {
    walls.push(after);
  }
}

function findWallBlocking(
  walls: WallSegment[],
  axis: "h" | "v",
  from: number,
  to: number,
  perp: number,
): WallSegment | null {
  if (from === to) {
    return null;
  }
  for (const wall of walls) {
    if (wall.axis !== axis) {
      continue;
    }
    if (perp < wall.start || perp > wall.end) {
      continue;
    }
    if ((from < wall.fixed && to >= wall.fixed) || (from > wall.fixed && to <= wall.fixed)) {
      return wall;
    }
  }
  return null;
}

function resolveAxisMove(
  walls: WallSegment[],
  axis: "h" | "v",
  from: number,
  to: number,
  perp: number,
): number {
  const wall = findWallBlocking(walls, axis, from, to, perp);
  if (!wall) {
    return to;
  }
  if (from < wall.fixed) {
    const limit = wall.fixed - PLAYER_WALL_PADDING_PCT;
    return Math.max(from, Math.min(to, limit));
  }
  const limit = wall.fixed + PLAYER_WALL_PADDING_PCT;
  return Math.min(from, Math.max(to, limit));
}

export function resolveStageMove(
  position: StageActorPosition,
  deltaX: number,
  deltaY: number,
  walls: WallSegment[],
): StageActorPosition {
  const fromX = parseStagePct(position.left);
  const fromY = parseStagePct(position.top);
  const targetX = clampNumber(fromX + deltaX, PLAYER_STAGE_BOUNDS.minX, PLAYER_STAGE_BOUNDS.maxX);
  const targetY = clampNumber(fromY + deltaY, PLAYER_STAGE_BOUNDS.minY, PLAYER_STAGE_BOUNDS.maxY);
  const resolvedX = resolveAxisMove(walls, "v", fromX, targetX, fromY);
  const resolvedY = resolveAxisMove(walls, "h", fromY, targetY, resolvedX);
  return { left: pct(resolvedX), top: pct(resolvedY) };
}

export function getStagePanForPosition(
  position: StageActorPosition,
  stageSize: StageSize,
  zoom: number,
): StagePan {
  const playerLeftPct = parseStagePct(position.left);
  const playerTopPct = parseStagePct(position.top);
  return {
    x: -((playerLeftPct - 50) / 100) * stageSize.width * zoom,
    y: -((playerTopPct - 50) / 100) * stageSize.height * zoom,
  };
}

export function smoothStagePan(current: StagePan, target: StagePan, amount: number): StagePan {
  return {
    x: current.x + (target.x - current.x) * amount,
    y: current.y + (target.y - current.y) * amount,
  };
}

function getStageDistancePct(a: StageActorPosition, b: StageActorPosition): number {
  const dx = parseStagePct(a.left) - parseStagePct(b.left);
  const dy = parseStagePct(a.top) - parseStagePct(b.top);
  return Math.hypot(dx, dy);
}

export function isNearQuestMarker(position: StageActorPosition): boolean {
  return getStageDistancePct(position, QUEST_MARKER_POSITION) <= QUEST_INTERACTION_RADIUS_PCT;
}

export function getPlayerMoveDirection(key: string): { x: number; y: number } | null {
  switch (key.toLowerCase()) {
    case "arrowup":
    case "w":
      return { x: 0, y: -1 };
    case "arrowdown":
    case "s":
      return { x: 0, y: 1 };
    case "arrowleft":
    case "a":
      return { x: -1, y: 0 };
    case "arrowright":
    case "d":
      return { x: 1, y: 0 };
    default:
      return null;
  }
}

export function isPlayerMoveKey(key: string): boolean {
  return getPlayerMoveDirection(key) != null;
}

export function createWorkspaceSceneEventBus(): WorkspaceSceneEventBus {
  const listeners = new Map<WorkspaceSceneEventType, Set<(event: WorkspaceSceneEvent) => void>>();

  return {
    on(type, listener) {
      const currentListeners = listeners.get(type) ?? new Set();
      const wrappedListener = listener as (event: WorkspaceSceneEvent) => void;
      currentListeners.add(wrappedListener);
      listeners.set(type, currentListeners);

      return () => {
        currentListeners.delete(wrappedListener);
        if (currentListeners.size === 0) {
          listeners.delete(type);
        }
      };
    },
    emit(event) {
      listeners.get(event.type)?.forEach((listener) => listener(event));
    },
    clear() {
      listeners.clear();
    },
  };
}

export function createInitialWorkspaceViewport(input: WorkspaceViewportInput): WorkspaceViewport {
  const width = positiveOrFallback(input.width, 1);
  const height = positiveOrFallback(input.height, 1);
  const worldWidth = Math.max(width, positiveOrFallback(input.worldWidth, width));
  const worldHeight = Math.max(height, positiveOrFallback(input.worldHeight, height));

  return {
    width,
    height,
    worldWidth,
    worldHeight,
    x: 0,
    y: 0,
    zoom: 1,
  };
}

export function panWorkspaceViewport(
  viewport: WorkspaceViewport,
  input: WorkspacePanInput,
): WorkspaceViewport {
  return clampViewport({
    ...viewport,
    x: viewport.x + input.deltaX,
    y: viewport.y + input.deltaY,
  });
}

export function zoomWorkspaceViewport(
  viewport: WorkspaceViewport,
  input: WorkspaceZoomInput,
): WorkspaceViewport {
  const nextZoom = clampNumber(viewport.zoom * input.factor, MIN_ZOOM, MAX_ZOOM);
  const originX = clampNumber(input.originX, 0, viewport.width);
  const originY = clampNumber(input.originY, 0, viewport.height);
  const worldOriginX = viewport.x + originX / viewport.zoom;
  const worldOriginY = viewport.y + originY / viewport.zoom;

  return clampViewport({
    ...viewport,
    x: worldOriginX - originX / nextZoom,
    y: worldOriginY - originY / nextZoom,
    zoom: nextZoom,
  });
}

export function moveWorkspaceViewportByKey(
  viewport: WorkspaceViewport,
  key: string,
): WorkspaceViewport {
  switch (key) {
    case "ArrowUp":
    case "w":
    case "W":
      return panWorkspaceViewport(viewport, { deltaX: 0, deltaY: -KEYBOARD_PAN_STEP });
    case "ArrowDown":
    case "s":
    case "S":
      return panWorkspaceViewport(viewport, { deltaX: 0, deltaY: KEYBOARD_PAN_STEP });
    case "ArrowLeft":
    case "a":
    case "A":
      return panWorkspaceViewport(viewport, { deltaX: -KEYBOARD_PAN_STEP, deltaY: 0 });
    case "ArrowRight":
    case "d":
    case "D":
      return panWorkspaceViewport(viewport, { deltaX: KEYBOARD_PAN_STEP, deltaY: 0 });
    default:
      return viewport;
  }
}

export function createWorkspaceSceneZones(): WorkspaceSceneZone[] {
  return [
    {
      id: "room-command",
      kind: "room",
      label: "Command room",
      x: 80,
      y: 80,
      width: 640,
      height: 300,
    },
    {
      id: "room-archive",
      kind: "room",
      label: "Archive room",
      x: 790,
      y: 90,
      width: 520,
      height: 290,
    },
    {
      id: "desk-ops",
      kind: "desk",
      label: "Ops desk",
      x: 160,
      y: 140,
      width: 220,
      height: 90,
    },
    {
      id: "desk-agent",
      kind: "desk",
      label: "Agent desk",
      x: 860,
      y: 165,
      width: 240,
      height: 90,
    },
  ];
}

export function createWorkspaceAvatarNodes(
  input: WorkspaceAvatarNodesInput,
): WorkspaceSceneNode[] {
  const presenceByMemberId = new Map(
    input.presenceMembers.map((member) => [member.memberId, member]),
  );
  const agentNodes = input.agents.map<WorkspaceSceneNode>((agent, index) => ({
    id: `agent-${agent.agentId}`,
    kind: "agent",
    label: agent.name,
    detail: `${agent.category} · ${agent.status}`,
    status: agent.status,
    x: 180 + (index % 4) * 180,
    y: 150 + Math.floor(index / 4) * 130,
  }));
  const memberNodes = input.members.map<WorkspaceSceneNode>((member, index) => {
    const presence = presenceByMemberId.get(member.memberId);
    return {
      id: `member-${member.memberId}`,
      kind: "member",
      label: member.name,
      detail: `${member.email} · ${member.role}`,
      status: presence?.status ?? "OFFLINE",
      x: parseSceneCoordinate(presence?.position?.left, 220 + (index % 4) * 180),
      y: parseSceneCoordinate(presence?.position?.top, 420 + Math.floor(index / 4) * 130),
    };
  });

  return [...agentNodes, ...memberNodes];
}

export function createWorkspaceContextMenuState(
  input: WorkspaceContextMenuInput,
): WorkspaceContextMenuState {
  return {
    ...input,
    actions:
      input.kind === "agent"
        ? ["open-profile", "focus-node", "start-chat"]
        : ["open-profile", "focus-node"],
  };
}

function clampViewport(viewport: WorkspaceViewport): WorkspaceViewport {
  const zoom = clampNumber(viewport.zoom, MIN_ZOOM, MAX_ZOOM);
  const maxX = Math.max(0, viewport.worldWidth - viewport.width / zoom);
  const maxY = Math.max(0, viewport.worldHeight - viewport.height / zoom);

  return {
    ...viewport,
    zoom,
    x: clampNumber(viewport.x, 0, maxX),
    y: clampNumber(viewport.y, 0, maxY),
  };
}

function positiveOrFallback(value: number, fallback: number): number {
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

function parseSceneCoordinate(value: string | undefined, fallback: number): number {
  if (!value) {
    return fallback;
  }

  const parsed = Number.parseFloat(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function clampNumber(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
