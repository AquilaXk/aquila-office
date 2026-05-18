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

export type WorkspaceSceneNode = {
  id: string;
  kind: WorkspaceSceneNodeKind;
  label: string;
  detail: string;
  status: string;
  x: number;
  y: number;
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
