import {
  createOfficePathfindingContext,
  findOfficePath,
  rasterizeWallRectsToBitgrid,
  type OfficePathfindingContext,
  type WorkspaceWallRect,
} from "./office-pathfinding.ts";
import {
  createSpatialBitset,
  isPctPointOccupiedInBitset,
  rasterizeRectToSpatialBitset,
  type SpatialBitset,
} from "./office-spatial-bitmask.ts";

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

const MIN_ZOOM = 0.5;
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

export type WorkspaceStageRoom = StageRoomSpec;

export type WorkspaceStageLayout = {
  engineeringRows: number;
  worldSize: StageSize;
  rooms: WorkspaceStageRoom[];
  roomsByVariant?: ReadonlyMap<string, WorkspaceStageRoom>;
  walls: WallSegment[];
  wallBitset?: SpatialBitset;
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

export type RoomId = StageRoomSpec["variant"];

export type WorkspaceStageTaskInput = {
  taskId: number;
  title: string;
  type?: string;
  assignedAgentId?: number | string | null;
  status: string;
  updatedAt?: string;
};

export type StageTaskSummary = {
  taskId: number;
  title: string;
  status: string;
  roomId: RoomId;
  priorityWeight: number;
};

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
  agentCategory?: string;
  taskSummary?: string;
  roomId?: RoomId;
};

export type WorkspaceStageActorsInput = WorkspaceAvatarNodesInput & {
  tasks?: readonly WorkspaceStageTaskInput[];
  rooms?: StageRoomSpec[];
  roomsByVariant?: ReadonlyMap<string, WorkspaceStageRoom>;
  worldSize?: StageSize;
};

export type WorkspaceActorSpeech = {
  label: string;
  text: string;
  tone: WorkspaceStageActorStatus;
};

export type StageMiniMapRoom = {
  id: string;
  label: string;
  x: string;
  y: string;
  w: string;
  h: string;
  variant: StageRoomSpec["variant"];
};

export type WallSegment = {
  axis: "h" | "v";
  fixed: number;
  start: number;
  end: number;
};

const STAGE_ZOOM_MIN = 0.5;
const STAGE_ZOOM_MAX = 1.5;
const PLAYER_STAGE_BOUNDS = {
  minX: 3,
  maxX: 96,
  minY: 8,
  maxY: 91,
};
const QUEST_MARKER_POSITION: StageActorPosition = { left: "90.4%", top: "75.8%" };
const QUEST_INTERACTION_RADIUS_PCT = 6.5;
const ACTOR_INTERACTION_RADIUS_PCT = 7;
const WALL_DOOR_HALF_PCT = 5;
const PLAYER_WALL_PADDING_PCT = 0.6;
const AGENT_DOOR_OUT_OFFSET_PCT = 1.6;
const AGENT_DESK_COL_CENTERS = [14, 38, 62, 85];
const STAGE_WORLD_WIDTH = 1800;
const STAGE_WORLD_MIN_HEIGHT = 950;
const ENGINEERING_X = 40;
const ENGINEERING_Y = 90;
const ENGINEERING_W = 760;
const HUDDLE_X = 860;
const HUDDLE_Y = 90;
const HUDDLE_W = 890;
const HUDDLE_H = 320;
const LOWER_ZONE_GAP = 100;
const LOUNGE_X = 40;
const LOUNGE_W = 620;
const LOUNGE_H = 320;
const LIBRARY_X = 720;
const LIBRARY_W = 1030;
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

export function normalizeStageActorPosition(
  position: StageActorPosition | null | undefined,
  worldSize: StageSize,
  fallback: StageActorPosition,
): StageActorPosition {
  if (!position) {
    return fallback;
  }

  return {
    left: normalizeStageCoordinate(position.left, worldSize.width, fallback.left),
    top: normalizeStageCoordinate(position.top, worldSize.height, fallback.top),
  };
}

function normalizeStageCoordinate(value: string, size: number, fallback: string): string {
  const parsed = Number.parseFloat(value);
  if (!Number.isFinite(parsed) || size <= 0) {
    return fallback;
  }
  if (value.trim().endsWith("%")) {
    return pct(clampStagePercent(parsed));
  }
  return pct(clampStagePercent((parsed / size) * 100));
}

function clampStagePercent(value: number): number {
  return Math.max(0, Math.min(100, value));
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
      sub: "collaboration zone",
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

export function getStageMiniMapRooms(rooms: StageRoomSpec[]): StageMiniMapRoom[] {
  return rooms.map((room) => ({
    id: `room-${room.label.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "")}`,
    label: room.label,
    x: room.left,
    y: room.top,
    w: room.width,
    h: room.height,
    variant: room.variant,
  }));
}

export function focusWorkspaceViewportOnStagePosition(
  viewport: WorkspaceViewport,
  position: StageActorPosition,
): WorkspaceViewport {
  const worldX = (parseStagePct(position.left) / 100) * viewport.worldWidth;
  const worldY = (parseStagePct(position.top) / 100) * viewport.worldHeight;
  return clampViewport({
    ...viewport,
    x: worldX - viewport.width / (2 * viewport.zoom),
    y: worldY - viewport.height / (2 * viewport.zoom),
  });
}

export function getDeskRowCenters(rows: number): number[] {
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

export function getDeskHeightPct(rows: number): number {
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

export function pickWorkspaceAvatarKind(name: string, agent = false): WorkspaceAvatarKind {
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

function resolveRoomIdForTaskType(taskType: string | undefined): RoomId {
  switch (taskType) {
    case "CODE_REVIEW":
    case "PR_REVIEW":
      return "huddle";
    case "DOCUMENTATION":
      return "library";
    default:
      return "engineering";
  }
}

function getTaskStatusPriorityWeight(status: string): number {
  switch (status) {
    case "IN_PROGRESS":
      return 3;
    case "WAITING_USER":
      return 2;
    case "ASSIGNED":
      return 1;
    default:
      return 0;
  }
}

function truncateTaskSummary(title: string, maxChars = 22): string {
  const normalized = title.trim();
  if (normalized.length <= maxChars) {
    return normalized;
  }
  return `${normalized.slice(0, maxChars - 1)}…`;
}

/**
 * Single-pass index mapping agentId -> active StageTaskSummary without repeating filter/find scans.
 */
export function indexActiveAgentTasks(
  tasks: readonly WorkspaceStageTaskInput[] = [],
): Map<number, StageTaskSummary> {
  const byAgentId = new Map<number, StageTaskSummary>();
  for (let i = 0; i < tasks.length; i += 1) {
    const task = tasks[i];
    if (task.assignedAgentId == null) {
      continue;
    }
    const agentId =
      typeof task.assignedAgentId === "number"
        ? task.assignedAgentId
        : Number(task.assignedAgentId);
    if (!Number.isInteger(agentId) || agentId <= 0) {
      continue;
    }
    const weight = getTaskStatusPriorityWeight(task.status);
    if (weight <= 0) {
      continue;
    }
    const existing = byAgentId.get(agentId);
    if (!existing || weight > existing.priorityWeight) {
      byAgentId.set(agentId, {
        taskId: task.taskId,
        title: truncateTaskSummary(task.title),
        status: task.status,
        roomId: resolveRoomIdForTaskType(task.type),
        priorityWeight: weight,
      });
    }
  }
  return byAgentId;
}

const HUDDLE_SAFE_SEATS = [
  { x: 20, y: 58 },
  { x: 42, y: 58 },
  { x: 20, y: 74 },
  { x: 42, y: 74 },
];

const LIBRARY_SAFE_SEATS = [
  { x: 22, y: 48 },
  { x: 38, y: 48 },
  { x: 22, y: 62 },
  { x: 38, y: 62 },
];

function getAgentRoomSeatPosition(
  agentIndex: number,
  roomId: RoomId,
  engineeringRows: number,
  roomsByVariant: ReadonlyMap<string, WorkspaceStageRoom>,
  fallbackEngineeringRoom: StageRoomSpec,
): StageActorPosition {
  if (roomId === "huddle") {
    const room = roomsByVariant.get("huddle");
    if (room) {
      const slot = HUDDLE_SAFE_SEATS[agentIndex % HUDDLE_SAFE_SEATS.length];
      return {
        left: pct(parseStagePct(room.left) + (slot.x / 100) * parseStagePct(room.width)),
        top: pct(parseStagePct(room.top) + (slot.y / 100) * parseStagePct(room.height)),
      };
    }
  }
  if (roomId === "library") {
    const room = roomsByVariant.get("library");
    if (room) {
      const slot = LIBRARY_SAFE_SEATS[agentIndex % LIBRARY_SAFE_SEATS.length];
      return {
        left: pct(parseStagePct(room.left) + (slot.x / 100) * parseStagePct(room.width)),
        top: pct(parseStagePct(room.top) + (slot.y / 100) * parseStagePct(room.height)),
      };
    }
  }
  if (roomId === "rest") {
    const room = roomsByVariant.get("rest");
    if (room) {
      return getAgentLoungePosition(agentIndex, room);
    }
  }
  return getAgentPositionInStage(agentIndex, engineeringRows, fallbackEngineeringRoom);
}

export function getWorkspaceActorSpeech(actor: WorkspaceStageActor): WorkspaceActorSpeech | null {
  if (actor.kind !== "agent") {
    return null;
  }
  if (actor.status === "working") {
    return { label: "진행중", text: actor.taskSummary ?? "", tone: "working" };
  }
  if (actor.status === "review") {
    return { label: "리뷰", text: "", tone: "review" };
  }
  if (actor.status === "blocked") {
    return { label: "확인 필요", text: "", tone: "blocked" };
  }
  return null;
}

export function indexStageRoomsByVariant(
  rooms: readonly StageRoomSpec[],
): Map<string, WorkspaceStageRoom> {
  const map = new Map<string, WorkspaceStageRoom>();
  for (let i = 0; i < rooms.length; i += 1) {
    const room = rooms[i];
    map.set(room.variant, room);
  }
  return map;
}

export function createWorkspaceStageActors(input: WorkspaceStageActorsInput): WorkspaceStageActor[] {
  const rows = getEngineeringRows(input.agents.length);
  const worldSize = input.worldSize ?? getStageWorldSize(rows);
  const rooms = input.rooms?.length ? input.rooms : getStageRooms(rows, worldSize, input.agents.length);
  const roomsByVariant = input.roomsByVariant ?? indexStageRoomsByVariant(rooms);
  const engineeringRoom = roomsByVariant.get("engineering") ?? rooms[0];
  const restRoom = roomsByVariant.get("rest") ?? null;
  const presenceByMemberId = new Map(
    input.presenceMembers.map((member) => [member.memberId, member]),
  );
  const activeTaskByAgentId = indexActiveAgentTasks(input.tasks);

  const agentActors = input.agents.map<WorkspaceStageActor>((agent, index) => {
    const activeTask = activeTaskByAgentId.get(agent.agentId);
    const hasInProgressTask =
      agent.status === "READY" && activeTask !== undefined && activeTask.status === "IN_PROGRESS";
    const status: WorkspaceStageActorStatus = hasInProgressTask
      ? "working"
      : mapWorkspaceAgentStatusToActorStatus(agent.status);
    const targetRoomId: RoomId = hasInProgressTask
      ? activeTask.roomId
      : status === "review"
        ? "rest"
        : "engineering";

    const position =
      status === "review" && restRoom
        ? getAgentLoungePosition(index, restRoom)
        : getAgentRoomSeatPosition(index, targetRoomId, rows, roomsByVariant, engineeringRoom);

    return {
      id: `agent-${agent.agentId}`,
      kind: "agent",
      label: agent.name,
      detail: hasInProgressTask ? `${agent.category} · ${activeTask.title}` : `${agent.category} · ${agent.status}`,
      sourceStatus: agent.status,
      status,
      motion: getWorkspaceActorMotion(status),
      position,
      avatarKind: "agent",
      agentCategory: agent.category,
      taskSummary: hasInProgressTask ? activeTask.title : undefined,
      roomId: targetRoomId,
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
      position: normalizeStageActorPosition(
        presence?.position,
        worldSize,
        MEMBER_POSITIONS[index % MEMBER_POSITIONS.length],
      ),
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

export function buildStageWallBitset(
  walls: readonly WallSegment[],
  cols = 40,
  rows = 24,
  targetBitset?: SpatialBitset,
): SpatialBitset {
  const bitset = targetBitset ?? createSpatialBitset(cols, rows);
  for (let i = 0; i < walls.length; i += 1) {
    const wall = walls[i];
    if (wall.axis === "h") {
      rasterizeRectToSpatialBitset(
        bitset,
        wall.start,
        wall.fixed - PLAYER_WALL_PADDING_PCT,
        Math.max(0.5, wall.end - wall.start),
        PLAYER_WALL_PADDING_PCT * 2,
      );
    } else {
      rasterizeRectToSpatialBitset(
        bitset,
        wall.fixed - PLAYER_WALL_PADDING_PCT,
        wall.start,
        PLAYER_WALL_PADDING_PCT * 2,
        Math.max(0.5, wall.end - wall.start),
      );
    }
  }
  return bitset;
}

export function createWorkspaceStageLayout(agentCount: number): WorkspaceStageLayout {
  const engineeringRows = getEngineeringRows(agentCount);
  const worldSize = getStageWorldSize(engineeringRows);
  const rooms = getStageRooms(engineeringRows, worldSize, agentCount);
  const roomsByVariant = indexStageRoomsByVariant(rooms);
  const walls = buildStageWalls(rooms);
  const wallBitset = buildStageWallBitset(walls);
  return {
    engineeringRows,
    worldSize,
    rooms,
    roomsByVariant,
    walls,
    wallBitset,
  };
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
  wallBitset?: SpatialBitset,
): StageActorPosition {
  const fromX = parseStagePct(position.left);
  const fromY = parseStagePct(position.top);
  const targetX = clampNumber(fromX + deltaX, PLAYER_STAGE_BOUNDS.minX, PLAYER_STAGE_BOUNDS.maxX);
  const targetY = clampNumber(fromY + deltaY, PLAYER_STAGE_BOUNDS.minY, PLAYER_STAGE_BOUNDS.maxY);
  if (
    wallBitset &&
    Math.abs(targetX - fromX) <= 1.2 &&
    Math.abs(targetY - fromY) <= 1.2 &&
    !isPctPointOccupiedInBitset(wallBitset, fromX, fromY) &&
    !isPctPointOccupiedInBitset(wallBitset, targetX, fromY) &&
    !isPctPointOccupiedInBitset(wallBitset, targetX, targetY)
  ) {
    return { left: pct(targetX), top: pct(targetY) };
  }
  const resolvedX = resolveAxisMove(walls, "v", fromX, targetX, fromY);
  const resolvedY = resolveAxisMove(walls, "h", fromY, targetY, resolvedX);
  return { left: pct(resolvedX), top: pct(resolvedY) };
}

const AGENT_PRIMARY_ROOM_VARIANTS: readonly StageRoomSpec["variant"][] = [
  "engineering",
  "rest",
  "huddle",
  "library",
  "planning",
  "review",
];

function isPointInsideRoom(x: number, y: number, room: StageRoomSpec): boolean {
  const left = parseStagePct(room.left);
  const top = parseStagePct(room.top);
  const width = parseStagePct(room.width);
  const height = parseStagePct(room.height);
  return x >= left && x <= left + width && y >= top && y <= top + height;
}

function findRoomContaining(
  x: number,
  y: number,
  rooms: StageRoomSpec[],
  roomsByVariant?: ReadonlyMap<string, StageRoomSpec>,
): StageRoomSpec | null {
  if (roomsByVariant) {
    for (let i = 0; i < AGENT_PRIMARY_ROOM_VARIANTS.length; i += 1) {
      const candidate = roomsByVariant.get(AGENT_PRIMARY_ROOM_VARIANTS[i]);
      if (candidate && isPointInsideRoom(x, y, candidate)) {
        return candidate;
      }
    }
  }
  for (const room of rooms) {
    if (isPointInsideRoom(x, y, room)) {
      return room;
    }
  }
  return null;
}

function getRoomDoorPoint(room: StageRoomSpec): {
  inside: { x: number; y: number };
  outside: { x: number; y: number };
} {
  const left = parseStagePct(room.left);
  const top = parseStagePct(room.top);
  const width = parseStagePct(room.width);
  const height = parseStagePct(room.height);
  const right = left + width;
  const bottom = top + height;
  const offset = AGENT_DOOR_OUT_OFFSET_PCT;

  if (room.doorSide === "top") {
    const x = left + width * room.doorAt;
    return { inside: { x, y: top }, outside: { x, y: top - offset } };
  }
  if (room.doorSide === "bottom") {
    const x = left + width * room.doorAt;
    return { inside: { x, y: bottom }, outside: { x, y: bottom + offset } };
  }
  if (room.doorSide === "left") {
    const y = top + height * room.doorAt;
    return { inside: { x: left, y }, outside: { x: left - offset, y } };
  }

  const y = top + height * room.doorAt;
  return { inside: { x: right, y }, outside: { x: right + offset, y } };
}

function getHallwayCorner(
  sourceOutside: { x: number; y: number },
  sourceSide: StageRoomSpec["doorSide"],
  targetOutside: { x: number; y: number },
  targetSide: StageRoomSpec["doorSide"],
): { x: number; y: number } | null {
  const sourceHorizontal = sourceSide === "left" || sourceSide === "right";
  const targetHorizontal = targetSide === "left" || targetSide === "right";
  if (
    Math.abs(sourceOutside.x - targetOutside.x) < 0.05 ||
    Math.abs(sourceOutside.y - targetOutside.y) < 0.05
  ) {
    return null;
  }
  if (sourceHorizontal && !targetHorizontal) {
    return { x: sourceOutside.x, y: targetOutside.y };
  }
  if (!sourceHorizontal && targetHorizontal) {
    return { x: targetOutside.x, y: sourceOutside.y };
  }
  if (sourceHorizontal) {
    return { x: sourceOutside.x, y: targetOutside.y };
  }
  return { x: targetOutside.x, y: sourceOutside.y };
}

export function buildStageObstacleRects(
  rooms: readonly StageRoomSpec[],
  engineeringRows = 2,
): WorkspaceWallRect[] {
  const walls = buildStageWalls(rooms as StageRoomSpec[]);
  const rects: WorkspaceWallRect[] = [];
  const halfWall = PLAYER_WALL_PADDING_PCT;

  for (let i = 0; i < walls.length; i += 1) {
    const wall = walls[i];
    if (wall.axis === "h") {
      rects.push({
        leftPct: wall.start,
        topPct: wall.fixed - halfWall,
        widthPct: Math.max(0.5, wall.end - wall.start),
        heightPct: halfWall * 2,
      });
    } else {
      rects.push({
        leftPct: wall.fixed - halfWall,
        topPct: wall.start,
        widthPct: halfWall * 2,
        heightPct: Math.max(0.5, wall.end - wall.start),
      });
    }
  }

  const roomsByVariant = indexStageRoomsByVariant(rooms);
  const engineering = roomsByVariant.get("engineering");
  if (engineering) {
    const rLeft = parseStagePct(engineering.left);
    const rTop = parseStagePct(engineering.top);
    const rW = parseStagePct(engineering.width);
    const rH = parseStagePct(engineering.height);
    const rowCenters = getDeskRowCenters(engineeringRows);
    const deskHeightPct = getDeskHeightPct(engineeringRows) * 0.45;
    const deskWidthPct = 10;

    for (let r = 0; r < rowCenters.length; r += 1) {
      for (let c = 0; c < AGENT_DESK_COL_CENTERS.length; c += 1) {
        const cx = rLeft + (AGENT_DESK_COL_CENTERS[c] / 100) * rW;
        const cy = rTop + ((rowCenters[r] + 1.5) / 100) * rH;
        const w = (deskWidthPct / 100) * rW;
        const h = (deskHeightPct / 100) * rH;
        rects.push({ leftPct: cx - w / 2, topPct: cy - h / 2, widthPct: w, heightPct: h });
      }
    }
  }

  const huddle = roomsByVariant.get("huddle");
  if (huddle) {
    const hLeft = parseStagePct(huddle.left);
    const hTop = parseStagePct(huddle.top);
    const hW = parseStagePct(huddle.width);
    const hH = parseStagePct(huddle.height);
    rects.push({
      leftPct: hLeft + hW * 0.36,
      topPct: hTop + hH * 0.34,
      widthPct: hW * 0.28,
      heightPct: hH * 0.2,
    });
  }

  const rest = roomsByVariant.get("rest");
  if (rest) {
    const lLeft = parseStagePct(rest.left);
    const lTop = parseStagePct(rest.top);
    const lW = parseStagePct(rest.width);
    const lH = parseStagePct(rest.height);
    rects.push({
      leftPct: lLeft + lW * 0.34,
      topPct: lTop + lH * 0.46,
      widthPct: lW * 0.26,
      heightPct: lH * 0.14,
    });
  }

  const library = roomsByVariant.get("library");
  if (library) {
    const libLeft = parseStagePct(library.left);
    const libTop = parseStagePct(library.top);
    const libW = parseStagePct(library.width);
    const libH = parseStagePct(library.height);
    rects.push({
      leftPct: libLeft + libW * 0.46,
      topPct: libTop + libH * 0.38,
      widthPct: libW * 0.18,
      heightPct: libH * 0.24,
    });
  }

  return rects;
}

export function clampContextMenuPosition(
  screenX: number,
  screenY: number,
  bounds: { width: number; height: number },
  menuWidth = 148,
  menuHeight = 118,
): { x: number; y: number } {
  const padding = 8;
  const maxX = Math.max(padding, bounds.width - menuWidth - padding);
  const maxY = Math.max(padding, bounds.height - menuHeight - padding);
  return {
    x: Math.round(clampNumber(Number.isFinite(screenX) ? screenX : padding, padding, maxX)),
    y: Math.round(clampNumber(Number.isFinite(screenY) ? screenY : padding, padding, maxY)),
  };
}

let sharedPathfindingCtx: OfficePathfindingContext | null = null;

function ensurePathfindingContext(
  rooms: readonly StageRoomSpec[],
  provided?: OfficePathfindingContext,
): OfficePathfindingContext {
  if (provided) {
    return provided;
  }
  if (!sharedPathfindingCtx) {
    sharedPathfindingCtx = createOfficePathfindingContext();
  }
  rasterizeWallRectsToBitgrid(
    sharedPathfindingCtx.blockedBits,
    buildStageObstacleRects(rooms),
    true,
  );
  return sharedPathfindingCtx;
}

export function computeAgentPath(
  from: StageActorPosition,
  to: StageActorPosition,
  rooms: StageRoomSpec[],
  roomsByVariant?: ReadonlyMap<string, StageRoomSpec>,
  pathCtx?: OfficePathfindingContext,
): { x: number; y: number }[] {
  const fromX = parseStagePct(from.left);
  const fromY = parseStagePct(from.top);
  const toX = parseStagePct(to.left);
  const toY = parseStagePct(to.top);
  const indexedRooms = roomsByVariant ?? indexStageRoomsByVariant(rooms);
  const fromRoom = findRoomContaining(fromX, fromY, rooms, indexedRooms);
  const toRoom = findRoomContaining(toX, toY, rooms, indexedRooms);
  const ctx = ensurePathfindingContext(rooms, pathCtx);

  if (!fromRoom || !toRoom || fromRoom.label === toRoom.label) {
    const aStarWaypoints = findOfficePath(
      ctx,
      { x: fromX, y: fromY },
      { x: toX, y: toY },
      { snapRadiusCells: 4 },
    );
    if (aStarWaypoints.length >= 2) {
      return aStarWaypoints.slice(1);
    }
    return [{ x: toX, y: toY }];
  }

  const sourceDoor = getRoomDoorPoint(fromRoom);
  const targetDoor = getRoomDoorPoint(toRoom);
  const corridorAStar = findOfficePath(ctx, sourceDoor.outside, targetDoor.outside, {
    snapRadiusCells: 4,
  });
  const waypoints: { x: number; y: number }[] = [sourceDoor.inside, sourceDoor.outside];

  if (corridorAStar.length >= 3) {
    for (let i = 1; i < corridorAStar.length - 1; i += 1) {
      waypoints.push(corridorAStar[i]);
    }
  } else {
    const corner = getHallwayCorner(
      sourceDoor.outside,
      fromRoom.doorSide,
      targetDoor.outside,
      toRoom.doorSide,
    );
    if (corner) {
      waypoints.push(corner);
    }
  }

  waypoints.push(targetDoor.outside, targetDoor.inside, { x: toX, y: toY });
  return waypoints;
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

export function getStageDistancePct(a: StageActorPosition, b: StageActorPosition): number {
  const dx = parseStagePct(a.left) - parseStagePct(b.left);
  const dy = parseStagePct(a.top) - parseStagePct(b.top);
  return Math.hypot(dx, dy);
}

export function sameStagePosition(a: StageActorPosition, b: StageActorPosition): boolean {
  return a.left === b.left && a.top === b.top;
}

export function findNearestInteractableStageActor<T>(
  playerPosition: StageActorPosition,
  candidates: Array<{ actor: T; position: StageActorPosition }>,
  radius = ACTOR_INTERACTION_RADIUS_PCT,
): T | null {
  const playerX = parseStagePct(playerPosition.left);
  const playerY = parseStagePct(playerPosition.top);
  const maxDistanceSq = radius * radius;
  let nearestActor: T | null = null;
  let nearestDistanceSq = maxDistanceSq;

  for (let i = 0; i < candidates.length; i += 1) {
    const candidate = candidates[i];
    const dx = playerX - parseStagePct(candidate.position.left);
    const dy = playerY - parseStagePct(candidate.position.top);
    const distanceSq = dx * dx + dy * dy;
    if (distanceSq <= maxDistanceSq && (nearestActor === null || distanceSq < nearestDistanceSq)) {
      nearestActor = candidate.actor;
      nearestDistanceSq = distanceSq;
    }
  }
  return nearestActor;
}

export const findNearestStageActor = findNearestInteractableStageActor;

const QUEST_INTERACTION_RADIUS_SQ = QUEST_INTERACTION_RADIUS_PCT * QUEST_INTERACTION_RADIUS_PCT;
const QUEST_MARKER_X = parseStagePct(QUEST_MARKER_POSITION.left);
const QUEST_MARKER_Y = parseStagePct(QUEST_MARKER_POSITION.top);

export function isNearQuestMarker(position: StageActorPosition): boolean {
  const dx = parseStagePct(position.left) - QUEST_MARKER_X;
  const dy = parseStagePct(position.top) - QUEST_MARKER_Y;
  return dx * dx + dy * dy <= QUEST_INTERACTION_RADIUS_SQ;
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

export function clampNumber(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
