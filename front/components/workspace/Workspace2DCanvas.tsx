"use client";

import { Crosshair, Maximize2, MousePointer2, Users } from "lucide-react";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  type WheelEvent as ReactWheelEvent,
} from "react";
import type { WorkspaceAgent } from "@/lib/agents";
import type { WorkspacePresenceMember, WorkspacePresencePosition } from "@/lib/presence";
import {
  createInitialWorkspaceViewport,
  createWorkspaceAvatarNodes,
  createWorkspaceContextMenuState,
  createWorkspaceSceneEventBus,
  panWorkspaceViewport,
  zoomWorkspaceViewport,
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
  onPresenceMove?: (position: WorkspacePresencePosition) => void | Promise<void>;
};

type DragState = {
  clientX: number;
  clientY: number;
};

type PhaserPointer = {
  x: number;
  y: number;
  rightButtonDown(): boolean;
};

const SCENE_WIDTH = 960;
const SCENE_HEIGHT = 520;
const WORLD_WIDTH = 1600;
const WORLD_HEIGHT = 960;

export default function Workspace2DCanvas({
  agents,
  members,
  presenceMembers,
  onPresenceMove,
}: Workspace2DCanvasProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const dragStateRef = useRef<DragState | null>(null);
  const eventBus = useMemo(() => createWorkspaceSceneEventBus(), []);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [contextMenu, setContextMenu] = useState<WorkspaceContextMenuState | null>(null);
  const [viewport, setViewport] = useState<WorkspaceViewport>(() =>
    createInitialWorkspaceViewport({
      width: SCENE_WIDTH,
      height: SCENE_HEIGHT,
      worldWidth: WORLD_WIDTH,
      worldHeight: WORLD_HEIGHT,
    }),
  );
  const nodes = useMemo(
    () => createWorkspaceAvatarNodes({ agents, members, presenceMembers }),
    [agents, members, presenceMembers],
  );
  const selectedNode = nodes.find((node) => node.id === selectedNodeId) ?? null;

  useEffect(() => {
    return eventBus.on("avatar.select", (event) => {
      setSelectedNodeId(`agent-${event.agentId}`);
      setContextMenu(null);
    });
  }, [eventBus]);

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
      // Phaser는 browser API에 의존하므로 client effect 내부에서만 로드한다.
      const Phaser = await import("phaser");
      if (disposed || !container) {
        return;
      }

      class WorkspaceScene extends Phaser.Scene {
        constructor() {
          super({ key: "Workspace2DScene" });
        }

        create() {
          this.cameras.main.setBounds(0, 0, viewport.worldWidth, viewport.worldHeight);
          this.cameras.main.setZoom(viewport.zoom);
          this.cameras.main.scrollX = viewport.x;
          this.cameras.main.scrollY = viewport.y;

          drawGrid(this, viewport);
          nodes.forEach((node) => {
            const fillColor = node.kind === "agent" ? 0xc2a35b : 0x6f9f79;
            const strokeColor = node.id === selectedNodeId ? 0xf1e6cc : 0x2b3035;
            const avatar = this.add.circle(node.x, node.y, 28, fillColor, 0.92);
            avatar.setStrokeStyle(3, strokeColor, 0.95);
            avatar.setInteractive({ useHandCursor: true });
            avatar.on("pointerdown", (pointer: PhaserPointer) => {
              if (pointer.rightButtonDown()) {
                setContextMenu(
                  createWorkspaceContextMenuState({
                    nodeId: node.id,
                    kind: node.kind,
                    screenX: pointer.x,
                    screenY: pointer.y,
                  }),
                );
                return;
              }

              setSelectedNodeId(node.id);
              setContextMenu(null);
              if (node.kind === "agent") {
                eventBus.emit({
                  type: "avatar.select",
                  agentId: Number(node.id.replace("agent-", "")),
                });
              }
            });

            this.add
              .text(node.x, node.y - 5, node.kind === "agent" ? "A" : "M", {
                color: "#101214",
                fontFamily: "Arial, Helvetica, sans-serif",
                fontSize: "18px",
                fontStyle: "700",
              })
              .setOrigin(0.5);
            this.add
              .text(node.x, node.y + 40, node.label, {
                color: "#f1e6cc",
                fontFamily: "Arial, Helvetica, sans-serif",
                fontSize: "13px",
                fontStyle: "700",
              })
              .setOrigin(0.5, 0);
          });
        }
      }

      game = new Phaser.Game({
        type: Phaser.CANVAS,
        parent: container,
        width: viewport.width,
        height: viewport.height,
        backgroundColor: "#101214",
        scene: WorkspaceScene,
      });
    }

    void mountScene();

    return () => {
      disposed = true;
      game?.destroy(true);
    };
  }, [eventBus, nodes, selectedNodeId, viewport]);

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
      setSelectedNodeId(node.id);
      setContextMenu(
        createWorkspaceContextMenuState({
          nodeId: node.id,
          kind: node.kind,
          screenX: bounds ? event.clientX - bounds.left : event.clientX,
          screenY: bounds ? event.clientY - bounds.top : event.clientY,
        }),
      );
    },
    [],
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

  return (
    <div className="workspace-2d-frame">
      <div className="workspace-2d-header">
        <div>
          <span className="workspace-detail-label">2D Workspace</span>
          <h2>작전 지도</h2>
        </div>
        <div className="workspace-2d-controls">
          <span>{Math.round(viewport.zoom * 100)}%</span>
          <button className="secondary-action workspace-task-refresh" type="button" onClick={resetViewport}>
            <Maximize2 aria-hidden="true" />
            <span>Reset</span>
          </button>
        </div>
      </div>

      <div className="workspace-2d-layout">
        <div className="workspace-2d-stage">
          <div
            ref={containerRef}
            className="workspace-2d-canvas"
            role="img"
            aria-label="Agent and member 2D workspace scene"
            onWheel={handleWheel}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerLeave={handlePointerUp}
            onDoubleClick={handleDoubleClick}
          />
          <div className="workspace-2d-status">
            <span>
              x {Math.round(viewport.x)} · y {Math.round(viewport.y)}
            </span>
            <span>{nodes.length} nodes</span>
          </div>
          {contextMenu ? (
            <WorkspaceContextMenu menu={contextMenu} />
          ) : null}
        </div>

        <div className="workspace-2d-roster" aria-label="2D workspace nodes">
          {nodes.length > 0 ? (
            nodes.map((node) => (
              <button
                className={`workspace-2d-node ${selectedNodeId === node.id ? "workspace-2d-node-active" : ""}`}
                type="button"
                key={node.id}
                onClick={() => {
                  setSelectedNodeId(node.id);
                  setContextMenu(null);
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
    </div>
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

function drawGrid(scene: Phaser.Scene, viewport: WorkspaceViewport) {
  const grid = scene.add.graphics();
  grid.lineStyle(1, 0xc2a35b, 0.16);
  for (let x = 0; x <= viewport.worldWidth; x += 80) {
    grid.moveTo(x, 0);
    grid.lineTo(x, viewport.worldHeight);
  }
  for (let y = 0; y <= viewport.worldHeight; y += 80) {
    grid.moveTo(0, y);
    grid.lineTo(viewport.worldWidth, y);
  }
  grid.strokePath();
  grid.lineStyle(2, 0xc2a35b, 0.34);
  grid.strokeRect(24, 24, viewport.worldWidth - 48, viewport.worldHeight - 48);
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
