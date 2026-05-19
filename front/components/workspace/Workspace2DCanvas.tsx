"use client";

import { Crosshair, Maximize2, Minus, MousePointer2, Plus, Users } from "lucide-react";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  type WheelEvent as ReactWheelEvent,
} from "react";
import { PixelAvatar } from "@/components/arcade";
import type { WorkspaceAgent } from "@/lib/agents";
import type { WorkspacePresenceMember, WorkspacePresencePosition } from "@/lib/presence";
import {
  createInitialWorkspaceViewport,
  createWorkspaceSceneZones,
  createWorkspaceStageActors,
  createWorkspaceAvatarNodes,
  createWorkspaceContextMenuState,
  createWorkspaceSceneEventBus,
  buildStageWalls,
  getEngineeringRows,
  getPlayerMoveDirection,
  getStageRooms,
  getStageWorldSize,
  getWorkspaceActorScreenPosition,
  getWorkspaceActorSpeech,
  moveWorkspaceViewportByKey,
  panWorkspaceViewport,
  resolveStageMove,
  zoomWorkspaceViewport,
  type StageActorPosition,
  type StageRoomSpec,
  type WorkspaceStageActor,
  type WorkspaceContextMenuAction,
  type WorkspaceContextMenuState,
  type WorkspaceSceneNode,
  type WorkspaceViewport,
} from "@/lib/workspace-2d";
import type { WorkspaceMember } from "@/lib/workspaces";

export type Workspace2DCanvasProps = {
  agents: WorkspaceAgent[];
  members: WorkspaceMember[];
  presenceMembers: WorkspacePresenceMember[];
  playerPosition?: StageActorPosition | null;
  rooms?: StageRoomSpec[];
  onPresenceMove?: (position: WorkspacePresencePosition) => void | Promise<void>;
  onPlayerPositionChange?: (position: StageActorPosition) => void | Promise<void>;
  onActorSelect?: (node: WorkspaceSceneNode) => void;
};

type DragState = {
  clientX: number;
  clientY: number;
};

const SCENE_WIDTH = 960;
const SCENE_HEIGHT = 520;

export default function Workspace2DCanvas({
  agents,
  members,
  presenceMembers,
  playerPosition = null,
  rooms = [],
  onPresenceMove,
  onPlayerPositionChange,
  onActorSelect,
}: Workspace2DCanvasProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const dragStateRef = useRef<DragState | null>(null);
  const eventBus = useMemo(() => createWorkspaceSceneEventBus(), []);
  const zones = useMemo(() => createWorkspaceSceneZones(), []);
  const engineeringRows = useMemo(() => getEngineeringRows(agents.length), [agents.length]);
  const stageWorldSize = useMemo(() => getStageWorldSize(engineeringRows), [engineeringRows]);
  const stageRooms = useMemo(
    () => (rooms.length ? rooms : getStageRooms(engineeringRows, stageWorldSize, agents.length)),
    [agents.length, engineeringRows, rooms, stageWorldSize],
  );
  const stageWalls = useMemo(() => buildStageWalls(stageRooms), [stageRooms]);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [contextMenu, setContextMenu] = useState<WorkspaceContextMenuState | null>(null);
  const [viewport, setViewport] = useState<WorkspaceViewport>(() =>
    createInitialWorkspaceViewport({
      width: SCENE_WIDTH,
      height: SCENE_HEIGHT,
      worldWidth: stageWorldSize.width,
      worldHeight: stageWorldSize.height,
    }),
  );
  const nodes = useMemo(
    () => createWorkspaceAvatarNodes({ agents, members, presenceMembers }),
    [agents, members, presenceMembers],
  );
  const stageActors = useMemo(
    () =>
      createWorkspaceStageActors({
        agents,
        members,
        presenceMembers,
        rooms: stageRooms,
        worldSize: stageWorldSize,
      }),
    [agents, members, presenceMembers, stageRooms, stageWorldSize],
  );
  const selectedNode = nodes.find((node) => node.id === selectedNodeId) ?? null;

  const selectNode = useCallback(
    (node: WorkspaceSceneNode) => {
      setSelectedNodeId(node.id);
      setContextMenu(null);
      onActorSelect?.(node);
    },
    [onActorSelect],
  );

  const selectStageActor = useCallback(
    (actor: WorkspaceStageActor) => {
      const node =
        nodes.find((candidate) => candidate.id === actor.id) ??
        ({
          id: actor.id,
          kind: actor.kind,
          label: actor.label,
          detail: actor.detail,
          status: actor.sourceStatus,
          x: 0,
          y: 0,
        } satisfies WorkspaceSceneNode);
      selectNode(node);
    },
    [nodes, selectNode],
  );

  useEffect(() => {
    return eventBus.on("avatar.select", (event) => {
      const node = nodes.find((candidate) => candidate.id === `agent-${event.agentId}`);
      if (node) {
        selectNode(node);
      }
    });
  }, [eventBus, nodes, selectNode]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }

    const suppressContextMenu = (event: MouseEvent) => event.preventDefault();
    container.addEventListener("contextmenu", suppressContextMenu);
    return () => container.removeEventListener("contextmenu", suppressContextMenu);
  }, []);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }

    let disposed = false;
    let game: { destroy(removeCanvas?: boolean): void } | null = null;
    container.replaceChildren();

    async function mountScene() {
      // Phaser scene factory는 browser API에 의존하므로 client effect 내부에서만 로드한다.
      const { createWorkspace2DGame } = await import("@/game/office/main");
      if (disposed || !container) {
        return;
      }
      game = createWorkspace2DGame({
        parent: container,
        viewport,
        nodes: [],
        selectedNodeId: null,
        eventBus,
        onSelectNode: (node) => {
          selectNode(node);
        },
        onContextMenu: setContextMenu,
      });
    }

    void mountScene();

    return () => {
      disposed = true;
      game?.destroy(true);
    };
  }, [eventBus, selectNode, viewport]);

  const publishViewport = useCallback(
    (nextViewport: WorkspaceViewport): WorkspaceViewport => {
      eventBus.emit({ type: "viewport.change", viewport: nextViewport });
      return nextViewport;
    },
    [eventBus],
  );

  const handleWheel = useCallback(
    (event: ReactWheelEvent<HTMLDivElement>) => {
      const bounds = event.currentTarget.getBoundingClientRect();
      const factor = event.deltaY < 0 ? 1.12 : 0.88;
      setViewport((current) =>
        publishViewport(
          zoomWorkspaceViewport(current, {
            factor,
            originX: event.clientX - bounds.left,
            originY: event.clientY - bounds.top,
          }),
        ),
      );
    },
    [publishViewport],
  );

  const handlePointerDown = useCallback((event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.button !== 0) {
      return;
    }
    if ((event.target as HTMLElement).closest("button")) {
      return;
    }
    dragStateRef.current = {
      clientX: event.clientX,
      clientY: event.clientY,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  }, []);

  const handlePointerMove = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      const dragState = dragStateRef.current;
      if (!dragState) {
        return;
      }

      const deltaX = (dragState.clientX - event.clientX) / viewport.zoom;
      const deltaY = (dragState.clientY - event.clientY) / viewport.zoom;
      dragStateRef.current = {
        clientX: event.clientX,
        clientY: event.clientY,
      };
      setViewport((current) =>
        publishViewport(panWorkspaceViewport(current, { deltaX, deltaY })),
      );
    },
    [publishViewport, viewport.zoom],
  );

  const handlePointerUp = useCallback((event: ReactPointerEvent<HTMLDivElement>) => {
    dragStateRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  }, []);

  const handleDoubleClick = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      if (!onPresenceMove) {
        return;
      }
      const bounds = event.currentTarget.getBoundingClientRect();
      const x = Math.round(viewport.x + (event.clientX - bounds.left) / viewport.zoom);
      const y = Math.round(viewport.y + (event.clientY - bounds.top) / viewport.zoom);
      void onPresenceMove({ left: String(x), top: String(y) });
    },
    [onPresenceMove, viewport],
  );

  const openContextMenu = useCallback(
    (node: WorkspaceSceneNode, event: ReactMouseEvent<HTMLButtonElement>) => {
      event.preventDefault();
      const bounds = containerRef.current?.getBoundingClientRect();
      selectNode(node);
      setContextMenu(
        createWorkspaceContextMenuState({
          nodeId: node.id,
          kind: node.kind,
          screenX: bounds ? event.clientX - bounds.left : event.clientX,
          screenY: bounds ? event.clientY - bounds.top : event.clientY,
        }),
      );
    },
    [selectNode],
  );

  const resetViewport = useCallback(() => {
    setViewport((current) =>
      publishViewport(
        createInitialWorkspaceViewport({
          width: current.width,
          height: current.height,
          worldWidth: current.worldWidth,
          worldHeight: current.worldHeight,
        }),
      ),
    );
  }, [publishViewport]);

  const adjustZoom = useCallback(
    (factor: number) => {
      setViewport((current) =>
        publishViewport(
          zoomWorkspaceViewport(current, {
            factor,
            originX: current.width / 2,
            originY: current.height / 2,
          }),
        ),
      );
    },
    [publishViewport],
  );

  const handleKeyDown = useCallback(
    (event: ReactKeyboardEvent<HTMLDivElement>) => {
      const moveDirection = getPlayerMoveDirection(event.key);
      if (moveDirection && playerPosition && onPlayerPositionChange) {
        event.preventDefault();
        const nextPosition = resolveStageMove(
          playerPosition,
          moveDirection.x * 2,
          moveDirection.y * 2,
          stageWalls,
        );
        void onPlayerPositionChange(nextPosition);
        return;
      }

      const nextViewport = moveWorkspaceViewportByKey(viewport, event.key);
      if (nextViewport !== viewport) {
        event.preventDefault();
        setViewport(publishViewport(nextViewport));
      }
    },
    [onPlayerPositionChange, playerPosition, publishViewport, stageWalls, viewport],
  );

  const activeAgents = agents.filter((agent) => agent.status === "READY").length;
  const activeMembers = presenceMembers.filter((member) => member.status === "ACTIVE").length;

  return (
    <div className="workspace-2d-frame">
      <div className="workspace-2d-header">
        <div>
          <span className="workspace-detail-label">2D Workspace</span>
          <h2>작전 지도</h2>
        </div>
        <div className="workspace-2d-controls">
          <span>{Math.round(viewport.zoom * 100)}%</span>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => adjustZoom(0.88)}
            aria-label="Zoom out"
          >
            <Minus aria-hidden="true" />
          </button>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => adjustZoom(1.12)}
            aria-label="Zoom in"
          >
            <Plus aria-hidden="true" />
          </button>
          <button className="secondary-action workspace-task-refresh" type="button" onClick={resetViewport}>
            <Maximize2 aria-hidden="true" />
            <span>Reset</span>
          </button>
        </div>
      </div>

      <div className="workspace-2d-hud" aria-label="Office scene HUD">
        <div>
          <span>READY Agent</span>
          <strong>{activeAgents}</strong>
        </div>
        <div>
          <span>Active Member</span>
          <strong>{activeMembers}</strong>
        </div>
        <div>
          <span>Selected actor</span>
          <strong>{selectedNode?.label ?? "None"}</strong>
        </div>
        <div>
          <span>Viewport</span>
          <strong>{`${Math.round(viewport.x)}, ${Math.round(viewport.y)}`}</strong>
        </div>
        <div>
          <span>Player</span>
          <strong>{playerPosition ? `${playerPosition.left}, ${playerPosition.top}` : "No signal"}</strong>
        </div>
      </div>

      <div className="workspace-2d-layout">
        <div
          className="workspace-2d-stage"
          onWheel={handleWheel}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerLeave={handlePointerUp}
          onDoubleClick={handleDoubleClick}
        >
          <span aria-label="Stage keyboard controls" hidden />
          <div
            ref={containerRef}
            className="workspace-2d-canvas"
            role="img"
            aria-label="Agent and member 2D workspace scene"
            tabIndex={0}
            onKeyDown={handleKeyDown}
          />
          <div className="workspace-2d-actor-layer" aria-label="Workspace actors">
            {stageActors.map((actor) => (
              <StageActor
                actor={actor}
                key={actor.id}
                selected={selectedNodeId === actor.id}
                viewport={viewport}
                onSelect={selectStageActor}
                onContextMenu={(selectedActor, event) => {
                  const bounds = containerRef.current?.getBoundingClientRect();
                  selectStageActor(selectedActor);
                  setContextMenu(
                    createWorkspaceContextMenuState({
                      nodeId: selectedActor.id,
                      kind: selectedActor.kind,
                      screenX: bounds ? event.clientX - bounds.left : event.clientX,
                      screenY: bounds ? event.clientY - bounds.top : event.clientY,
                    }),
                  );
                }}
              />
            ))}
          </div>
          <div className="workspace-2d-status">
            <span>
              x {Math.round(viewport.x)} · y {Math.round(viewport.y)}
            </span>
            <span>{nodes.length} nodes</span>
          </div>
          {contextMenu ? (
            <WorkspaceContextMenu menu={contextMenu} />
          ) : null}
          <div className="workspace-2d-minimap" aria-label="Office scene minimap">
            {zones.map((zone) => (
              <span
                className={`workspace-2d-minimap-zone workspace-2d-minimap-${zone.kind}`}
                key={zone.id}
                title={zone.label}
                style={{
                  left: `${(zone.x / viewport.worldWidth) * 100}%`,
                  top: `${(zone.y / viewport.worldHeight) * 100}%`,
                  width: `${(zone.width / viewport.worldWidth) * 100}%`,
                  height: `${(zone.height / viewport.worldHeight) * 100}%`,
                }}
              />
            ))}
            <span
              className="workspace-2d-minimap-view"
              style={{
                left: `${(viewport.x / viewport.worldWidth) * 100}%`,
                top: `${(viewport.y / viewport.worldHeight) * 100}%`,
                width: `${(viewport.width / viewport.zoom / viewport.worldWidth) * 100}%`,
                height: `${(viewport.height / viewport.zoom / viewport.worldHeight) * 100}%`,
              }}
            />
          </div>
        </div>

        <div className="workspace-2d-roster" aria-label="2D workspace nodes">
          {nodes.length > 0 ? (
            nodes.map((node) => (
              <button
                className={`workspace-2d-node ${selectedNodeId === node.id ? "workspace-2d-node-active" : ""}`}
                type="button"
                key={node.id}
                onClick={() => {
                  selectNode(node);
                }}
                onContextMenu={(event) => openContextMenu(node, event)}
              >
                <span className={`workspace-2d-node-mark workspace-2d-node-${node.kind}`}>
                  {node.kind === "agent" ? <Crosshair aria-hidden="true" /> : <Users aria-hidden="true" />}
                </span>
                <span>
                  <strong>{node.label}</strong>
                  <small>{node.detail}</small>
                </span>
              </button>
            ))
          ) : (
            <div className="workspace-2d-empty">
              <MousePointer2 aria-hidden="true" />
              <strong>표시할 노드가 없습니다</strong>
            </div>
          )}
        </div>
      </div>

      {selectedNode ? (
        <div className="workspace-2d-selection" role="status">
          <MousePointer2 aria-hidden="true" />
          <span>
            {selectedNode.label} · {selectedNode.status}
          </span>
        </div>
      ) : null}

      <div className="workspace-2d-dialogue" aria-live="polite">
        <strong>{selectedNode ? selectedNode.label : "Office scene"}</strong>
        <p>
          {selectedNode
            ? `${selectedNode.detail} actor가 선택되었습니다. 우클릭 메뉴로 다음 행동을 고를 수 있습니다.`
            : "stage를 클릭해 actor를 선택하고, 방향키 또는 WASD로 화면을 이동합니다."}
        </p>
      </div>
  </div>
  );
}

function StageActor({
  actor,
  selected,
  viewport,
  onSelect,
  onContextMenu,
}: {
  actor: WorkspaceStageActor;
  selected: boolean;
  viewport: WorkspaceViewport;
  onSelect: (actor: WorkspaceStageActor) => void;
  onContextMenu: (
    actor: WorkspaceStageActor,
    event: ReactMouseEvent<HTMLButtonElement>,
  ) => void;
}) {
  const screenPosition = getWorkspaceActorScreenPosition(actor.position, viewport);
  const speech = getWorkspaceActorSpeech(actor);
  return (
    <button
      type="button"
      className={`stage-actor stage-actor--${actor.motion} stage-actor-${actor.kind} ${
        selected ? "stage-actor-selected" : ""
      }`}
      style={{
        left: `${screenPosition.left}px`,
        top: `${screenPosition.top}px`,
      }}
      aria-label={`${actor.label} ${actor.kind}`}
      onClick={() => onSelect(actor)}
      onContextMenu={(event) => {
        event.preventDefault();
        onContextMenu(actor, event);
      }}
    >
      {speech ? (
        <span className={`stage-actor__speech stage-actor__speech-${speech.tone}`}>
          {speech.label}
        </span>
      ) : null}
      <span className="stage-actor__badge">
        <span className={`stage-actor__dot stage-actor__dot-${actor.status}`} />
        <span>{actor.label}</span>
      </span>
      <span className="stage-actor__avatar">
        <PixelAvatar kind={actor.avatarKind} size={3} walking={actor.motion === "walking"} />
      </span>
    </button>
  );
}

function WorkspaceContextMenu({ menu }: { menu: WorkspaceContextMenuState }) {
  return (
    <div
      className="workspace-2d-context-menu"
      style={{
        left: `${menu.screenX}px`,
        top: `${menu.screenY}px`,
      }}
    >
      {menu.actions.map((action) => (
        <button type="button" key={action}>
          {labelForContextAction(action)}
        </button>
      ))}
    </div>
  );
}

function labelForContextAction(action: WorkspaceContextMenuAction): string {
  switch (action) {
    case "focus-node":
      return "Focus";
    case "open-profile":
      return "Profile";
    case "start-chat":
      return "Chat";
  }
}
