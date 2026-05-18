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

function clampNumber(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
