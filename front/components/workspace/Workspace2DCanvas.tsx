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
  type ReactNode,
  type WheelEvent as ReactWheelEvent,
} from "react";
import {
  GlyphText,
  PixelAvatar,
  T4Panel,
  type PixelAvatarDirection,
  type PixelAvatarKind,
  type PixelAvatarPaletteOverride,
} from "@/components/arcade";
import { t4 } from "@/components/arcade/tokens";
import type { WorkspaceAgent } from "@/lib/agents";
import type { WorkspacePresenceMember, WorkspacePresencePosition } from "@/lib/presence";
import {
  buildStageWalls,
  clampStageZoom,
  computeAgentPath,
  createWorkspaceAvatarNodes,
  createWorkspaceContextMenuState,
  createWorkspaceStageActors,
  findNearestInteractableStageActor,
  getAgentLoungePosition,
  getAgentPositionInStage,
  getEngineeringRows,
  getStageMiniMapRooms,
  getStagePanForPosition,
  getStageRooms,
  getStageWorldSize,
  getWorkspaceActorSpeech,
  isNearQuestMarker,
  parseStagePct,
  pct,
  resolveStageMove,
  sameStagePosition,
  smoothStagePan,
  type StageActorPosition,
  type StageMiniMapRoom,
  type StagePan,
  type StageRoomSpec,
  type StageSize,
  type WallSegment,
  type WorkspaceContextMenuAction,
  type WorkspaceContextMenuState,
  type WorkspaceSceneNode,
  type WorkspaceStageActor,
} from "@/lib/workspace-2d";
import type { WorkspaceMember } from "@/lib/workspaces";
import {
  ACTOR_FLAG,
  countOccupiedBits,
  createSpatialBitset,
  matchesActorBitmask,
  packActorBitfield,
  setSpatialBit,
} from "../../lib/office-spatial-bitmask";

export type Workspace2DCanvasProps = {
  agents: WorkspaceAgent[];
  members: WorkspaceMember[];
  presenceMembers: WorkspacePresenceMember[];
  variant?: "panel" | "fullscreen";
  playerPosition?: StageActorPosition | null;
  playerAvatar?: {
    kind: PixelAvatarKind;
    paletteOverride?: PixelAvatarPaletteOverride;
  } | null;
  playerMemberId?: number | null;
  rooms?: StageRoomSpec[];
  onPresenceMove?: (position: WorkspacePresencePosition) => void | Promise<void>;
  onPlayerPositionChange?: (position: StageActorPosition) => void | Promise<void>;
  onActorSelect?: (node: WorkspaceSceneNode) => void;
  onOpenTasks?: () => void;
};

type StageRenderActor = {
  actor: WorkspaceStageActor;
  isPlayer: boolean;
  position: StageActorPosition;
  logicalPosition: StageActorPosition;
  transitionMs?: number;
  traveling: boolean;
};

type DragState = {
  pointerId: number;
  startX: number;
  startY: number;
  originX: number;
  originY: number;
};

const STAGE_ZOOM_STEP = 0.125;
const PLAYER_MOVE_SPEED_PX = 170;
const PLAYER_MOVE_FAST_SPEED_PX = 270;
const AGENT_TRAVEL_MIN_SEGMENT_MS = 90;
const CAMERA_FOLLOW_SMOOTHING = 0.18;
const QUEST_MARKER_POSITION: StageActorPosition = { left: "90.4%", top: "75.8%" };
const AGENT_DESK_COL_CENTERS = [14, 38, 62, 85];
const AGENT_DESK_W_PCT = 14;

export default function Workspace2DCanvas({
  agents,
  members,
  presenceMembers,
  variant = "panel",
  playerPosition = null,
  playerAvatar = null,
  playerMemberId = null,
  rooms = [],
  onPresenceMove,
  onPlayerPositionChange,
  onActorSelect,
  onOpenTasks,
}: Workspace2DCanvasProps) {
  const frameRef = useRef<HTMLDivElement | null>(null);
  const isFullscreenStage = variant === "fullscreen";
  const initialStageZoom = isFullscreenStage ? 0.75 : 1;
  const engineeringRows = useMemo(() => getEngineeringRows(agents.length), [agents.length]);
  const stageWorldSize = useMemo(() => getStageWorldSize(engineeringRows), [engineeringRows]);
  const stageRooms = useMemo(
    () => (rooms.length ? rooms : getStageRooms(engineeringRows, stageWorldSize, agents.length)),
    [agents.length, engineeringRows, rooms, stageWorldSize],
  );
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [contextMenu, setContextMenu] = useState<WorkspaceContextMenuState | null>(null);
  const [stageZoom, setStageZoom] = useState(initialStageZoom);
  const [stagePan, setStagePan] = useState<StagePan>({ x: 0, y: 0 });
  const initialCameraRef = useRef(false);

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
  const playerMember = useMemo(() => {
    const authenticatedMember =
      playerMemberId != null ? members.find((member) => member.memberId === playerMemberId) : null;
    return authenticatedMember ?? members[0] ?? null;
  }, [members, playerMemberId]);
  const playerActorId = playerMember ? `member-${playerMember.memberId}` : null;
  const logicalStageActors = useMemo(
    () =>
      stageActors.map((actor) =>
        actor.id === playerActorId && playerPosition
          ? {
              ...actor,
              position: playerPosition,
              avatarKind: playerAvatar?.kind ?? actor.avatarKind,
            }
          : actor,
      ),
    [playerActorId, playerAvatar?.kind, playerPosition, stageActors],
  );
  const miniMapRooms = useMemo(() => getStageMiniMapRooms(stageRooms), [stageRooms]);
  const selectedNode = nodes.find((node) => node.id === selectedNodeId) ?? null;
  const selectedStageActor =
    logicalStageActors.find((actor) => actor.id === selectedNodeId) ??
    logicalStageActors.find((actor) => actor.id === playerActorId) ??
    logicalStageActors[0] ??
    null;
  const stageSpatialMetrics = useMemo(() => {
    const gridWidth = 40;
    const gridHeight = 24;
    const bitset = createSpatialBitset(gridWidth, gridHeight);
    let onlineAgentCount = 0;

    for (let index = 0; index < logicalStageActors.length; index += 1) {
      const actor = logicalStageActors[index];
      const gx = Math.min(
        gridWidth - 1,
        Math.max(0, Math.floor((parseStagePct(actor.position.left) / 100) * gridWidth)),
      );
      const gy = Math.min(
        gridHeight - 1,
        Math.max(0, Math.floor((parseStagePct(actor.position.top) / 100) * gridHeight)),
      );
      setSpatialBit(bitset, gridWidth, gx, gy);

      let flags = 0;
      if (actor.status !== "blocked") flags |= ACTOR_FLAG.ONLINE;
      if (actor.kind === "agent") flags |= ACTOR_FLAG.AGENT;
      if (actor.id === selectedNodeId) flags |= ACTOR_FLAG.SELECTED;
      if (actor.id === selectedStageActor?.id) flags |= ACTOR_FLAG.FOCUSED;
      if (actor.motion === "working") flags |= ACTOR_FLAG.ACTIVE_TASK;
      if (actor.id === playerActorId) flags |= ACTOR_FLAG.SELF;

      const bitfield = packActorBitfield(flags, index & 0x0f, actor.kind === "agent" ? 1 : 0);
      if (matchesActorBitmask(bitfield, ACTOR_FLAG.AGENT | ACTOR_FLAG.ONLINE)) {
        onlineAgentCount += 1;
      }
    }

    return {
      occupiedTiles: countOccupiedBits(bitset),
      onlineAgentCount,
    };
  }, [logicalStageActors, playerActorId, selectedNodeId, selectedStageActor?.id]);
  const activeAgents = agents.filter((agent) => agent.status === "READY").length;
  const activeMembers = presenceMembers.filter((member) => member.status === "ACTIVE").length;

  useEffect(() => {
    if (!playerPosition || initialCameraRef.current) return;
    initialCameraRef.current = true;
    setStagePan(getStagePanForPosition(playerPosition, stageWorldSize, stageZoom));
  }, [playerPosition, stageWorldSize, stageZoom]);

  const nodeForActor = useCallback(
    (actor: WorkspaceStageActor): WorkspaceSceneNode =>
      nodes.find((candidate) => candidate.id === actor.id) ?? {
        id: actor.id,
        kind: actor.kind,
        label: actor.label,
        detail: actor.detail,
        status: actor.sourceStatus,
        x: parseStagePct(actor.position.left),
        y: parseStagePct(actor.position.top),
      },
    [nodes],
  );

  const selectStageActor = useCallback(
    (actor: WorkspaceStageActor) => {
      const node = nodeForActor(actor);
      setSelectedNodeId(node.id);
      setContextMenu(null);
      onActorSelect?.(node);
    },
    [nodeForActor, onActorSelect],
  );

  const openActorContextMenu = useCallback(
    (payload: { actor: WorkspaceStageActor; x: number; y: number }) => {
      const bounds = frameRef.current?.getBoundingClientRect();
      const node = nodeForActor(payload.actor);
      setSelectedNodeId(node.id);
      setContextMenu(
        createWorkspaceContextMenuState({
          nodeId: node.id,
          kind: node.kind,
          screenX: bounds ? payload.x - bounds.left : payload.x,
          screenY: bounds ? payload.y - bounds.top : payload.y,
        }),
      );
      onActorSelect?.(node);
    },
    [nodeForActor, onActorSelect],
  );

  const handleContextMenuAction = useCallback(
    (action: WorkspaceContextMenuAction) => {
      if (!contextMenu) return;
      const node = nodes.find((item) => item.id === contextMenu.nodeId);
      if (!node) {
        setContextMenu(null);
        return;
      }

      setSelectedNodeId(node.id);
      setContextMenu(null);
      if (action === "focus-node") {
        const actor = logicalStageActors.find((item) => item.id === node.id);
        if (actor) {
          setStagePan(getStagePanForPosition(actor.position, stageWorldSize, stageZoom));
        }
        return;
      }

      onActorSelect?.(node);
    },
    [contextMenu, logicalStageActors, nodes, onActorSelect, stageWorldSize, stageZoom],
  );

  const updateStageZoom = useCallback(
    (nextZoom: number) => {
      const zoom = clampStageZoom(nextZoom);
      setStageZoom(zoom);
      if (playerPosition) {
        setStagePan(getStagePanForPosition(playerPosition, stageWorldSize, zoom));
      }
    },
    [playerPosition, stageWorldSize],
  );

  const handleStageShellWheel = useCallback(
    (event: ReactWheelEvent<HTMLDivElement>) => {
      event.preventDefault();
      updateStageZoom(stageZoom + (event.deltaY < 0 ? STAGE_ZOOM_STEP : -STAGE_ZOOM_STEP));
    },
    [stageZoom, updateStageZoom],
  );

  const focusMiniMapRoom = useCallback(
    (room: StageMiniMapRoom) => {
      const center = {
        left: pct(parseStagePct(room.x) + parseStagePct(room.w) / 2),
        top: pct(parseStagePct(room.y) + parseStagePct(room.h) / 2),
      };
      setStagePan(getStagePanForPosition(center, stageWorldSize, stageZoom));
    },
    [stageWorldSize, stageZoom],
  );

  return (
    <div
      className={`workspace-2d-frame ${isFullscreenStage ? "workspace-2d-frame-fullscreen" : ""}`}
      ref={frameRef}
    >
      <div className="workspace-2d-header">
        <div>
          <span className="workspace-detail-label">2D Workspace</span>
          <h2>작전 지도</h2>
        </div>
        <div className="workspace-2d-controls">
          <span>{Math.round(stageZoom * 100)}%</span>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => updateStageZoom(stageZoom - STAGE_ZOOM_STEP)}
            aria-label="Zoom out"
          >
            -
          </button>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => updateStageZoom(stageZoom + STAGE_ZOOM_STEP)}
            aria-label="Zoom in"
          >
            +
          </button>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => {
              setStageZoom(initialStageZoom);
              const resetPan = playerPosition
                ? getStagePanForPosition(playerPosition, stageWorldSize, initialStageZoom)
                : { x: 0, y: 0 };
              setStagePan(resetPan);
            }}
          >
            <Maximize2 aria-hidden="true" />
            <span>Reset</span>
          </button>
        </div>
      </div>

      <div className="workspace-2d-hero-hud" aria-label="Selected actor HUD">
        <div className="workspace-2d-hero-avatar">
          {selectedStageActor ? (
            <PixelAvatar
              kind={selectedStageActor.avatarKind}
              size={2}
              walking={selectedStageActor.motion === "working"}
              paletteOverride={selectedStageActor.id === playerActorId ? playerAvatar?.paletteOverride : undefined}
            />
          ) : (
            <Users aria-hidden="true" />
          )}
        </div>
        <div>
          <span>{selectedStageActor ? (selectedStageActor.kind === "agent" ? "Agent" : "Member") : "Actor"}</span>
          <strong>{selectedNode?.label ?? selectedStageActor?.label ?? "None"}</strong>
          <small>{selectedNode?.detail ?? selectedStageActor?.detail ?? "No actor selected"}</small>
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
          <span>Grid</span>
          <strong>
            {stageSpatialMetrics.occupiedTiles} · {stageSpatialMetrics.onlineAgentCount}
          </strong>
        </div>
        <div>
          <span>Player</span>
          <strong>{playerPosition ? `${playerPosition.left}, ${playerPosition.top}` : "Standby"}</strong>
        </div>
      </div>

      <div className="workspace-2d-layout">
        <div className="workspace-2d-stage-shell" onWheel={handleStageShellWheel}>
          <ArcadeOfficeStage
            actors={logicalStageActors}
            rooms={stageRooms}
            worldSize={stageWorldSize}
            engineeringRows={engineeringRows}
            selectedActorId={selectedNodeId}
            playerActorId={playerActorId}
            playerAvatar={playerAvatar}
            playerPosition={playerPosition}
            zoom={stageZoom}
            pan={stagePan}
            onPanChange={setStagePan}
            onZoomChange={updateStageZoom}
            onPlayerPositionChange={onPlayerPositionChange}
            onPresenceMove={onPresenceMove}
            onOpenTasks={onOpenTasks}
            onActorContextMenu={openActorContextMenu}
            onActorSelect={selectStageActor}
          />
          <MapHud
            rooms={miniMapRooms}
            myPosition={playerPosition}
            totalActors={logicalStageActors.length}
            zoom={stageZoom}
            pan={stagePan}
            stageSize={stageWorldSize}
            onRoomClick={focusMiniMapRoom}
          />
          <MapZoomControls
            zoom={stageZoom}
            onZoomOut={() => updateStageZoom(stageZoom - STAGE_ZOOM_STEP)}
            onReset={() => updateStageZoom(initialStageZoom)}
            onZoomIn={() => updateStageZoom(stageZoom + STAGE_ZOOM_STEP)}
          />
          {contextMenu ? <WorkspaceContextMenu menu={contextMenu} onAction={handleContextMenuAction} /> : null}
          <DialogueBox
            actor={selectedStageActor}
            playerActorId={playerActorId}
            onTalk={selectStageActor}
            onTasks={onOpenTasks}
          />
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
                  onActorSelect?.(node);
                }}
                onContextMenu={(event) => {
                  event.preventDefault();
                  setSelectedNodeId(node.id);
                  setContextMenu(
                    createWorkspaceContextMenuState({
                      nodeId: node.id,
                      kind: node.kind,
                      screenX: event.clientX,
                      screenY: event.clientY,
                    }),
                  );
                }}
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
    </div>
  );
}

function ArcadeOfficeStage({
  actors,
  rooms,
  worldSize,
  engineeringRows,
  selectedActorId,
  playerActorId,
  playerAvatar,
  playerPosition,
  zoom,
  pan,
  onPanChange,
  onZoomChange,
  onPlayerPositionChange,
  onPresenceMove,
  onOpenTasks,
  onActorContextMenu,
  onActorSelect,
}: {
  actors: WorkspaceStageActor[];
  rooms: StageRoomSpec[];
  worldSize: StageSize;
  engineeringRows: number;
  selectedActorId: string | null;
  playerActorId: string | null;
  playerAvatar: { kind: PixelAvatarKind; paletteOverride?: PixelAvatarPaletteOverride } | null;
  playerPosition: StageActorPosition | null;
  zoom: number;
  pan: StagePan;
  onPanChange: (pan: StagePan) => void;
  onZoomChange: (zoom: number) => void;
  onPlayerPositionChange?: (position: StageActorPosition) => void | Promise<void>;
  onPresenceMove?: (position: WorkspacePresencePosition) => void | Promise<void>;
  onOpenTasks?: () => void;
  onActorContextMenu: (payload: { actor: WorkspaceStageActor; x: number; y: number }) => void;
  onActorSelect: (actor: WorkspaceStageActor) => void;
}) {
  const stageRef = useRef<HTMLDivElement | null>(null);
  const keysRef = useRef<Record<string, boolean>>({});
  const playerPositionRef = useRef<StageActorPosition | null>(playerPosition);
  const fixedStageSizeRef = useRef<StageSize | null>(worldSize);
  const zoomRef = useRef(zoom);
  const panRef = useRef(pan);
  const onPanChangeRef = useRef(onPanChange);
  const onPlayerPositionChangeRef = useRef(onPlayerPositionChange);
  const onActorSelectRef = useRef(onActorSelect);
  const onOpenTasksRef = useRef(onOpenTasks);
  const actorPositionRef = useRef<Map<string, StageActorPosition>>(new Map());
  const interactableActorsRef = useRef<Array<{ actor: WorkspaceStageActor; position: StageActorPosition }>>([]);
  const wallsRef = useRef<WallSegment[]>([]);
  const actorTravelTimerRef = useRef<Map<string, number>>(new Map());
  const agentMotionTimersRef = useRef<Map<string, number[]>>(new Map());
  const [drag, setDrag] = useState<DragState | null>(null);
  const [playerWalking, setPlayerWalking] = useState(false);
  const [playerDirection, setPlayerDirection] = useState<PixelAvatarDirection>("S");
  const [travelingActorIds, setTravelingActorIds] = useState<Set<string>>(() => new Set());
  const [agentVisualPositions, setAgentVisualPositions] = useState<Record<string, StageActorPosition>>({});
  const [agentTransitionMs, setAgentTransitionMs] = useState<Record<string, number>>({});
  const playerNearQuest = playerPosition ? isNearQuestMarker(playerPosition) : false;

  useEffect(() => {
    onPanChangeRef.current = onPanChange;
    onPlayerPositionChangeRef.current = onPlayerPositionChange;
    onActorSelectRef.current = onActorSelect;
    onOpenTasksRef.current = onOpenTasks;
  }, [onActorSelect, onOpenTasks, onPanChange, onPlayerPositionChange]);

  useEffect(() => {
    playerPositionRef.current = playerPosition;
    fixedStageSizeRef.current = worldSize;
    zoomRef.current = zoom;
    panRef.current = pan;
  }, [pan, playerPosition, worldSize, zoom]);

  useEffect(() => {
    wallsRef.current = buildStageWalls(rooms);
  }, [rooms]);

  useEffect(() => {
    const travelTimers = actorTravelTimerRef.current;
    const motionTimers = agentMotionTimersRef.current;
    return () => {
      travelTimers.forEach((timerId) => window.clearTimeout(timerId));
      travelTimers.clear();
      motionTimers.forEach((timerList) => timerList.forEach((timerId) => window.clearTimeout(timerId)));
      motionTimers.clear();
    };
  }, []);

  const renderedActors = useMemo<StageRenderActor[]>(() => {
    let agentSlot = 0;
    let loungeAgentSlot = 0;
    const engineeringRoom = rooms.find((room) => room.variant === "engineering") ?? rooms[0];
    const loungeRoom = rooms.find((room) => room.variant === "rest") ?? null;
    return actors.map((actor) => {
      const completedAgent = actor.kind === "agent" && actor.status === "review";
      const basePosition =
        actor.kind === "agent"
          ? getAgentPositionInStage(agentSlot++, engineeringRows, engineeringRoom)
          : actor.position;
      const loungePosition =
        completedAgent && loungeRoom ? getAgentLoungePosition(loungeAgentSlot++, loungeRoom) : null;
      const isPlayer = playerActorId != null && actor.id === playerActorId;
      const logicalPosition = isPlayer && playerPosition ? playerPosition : loungePosition ?? actor.position ?? basePosition;
      const visualPosition = actor.kind === "agent" ? agentVisualPositions[actor.id] : null;
      return {
        actor,
        isPlayer,
        position: visualPosition ?? logicalPosition,
        logicalPosition,
        transitionMs: actor.kind === "agent" ? agentTransitionMs[actor.id] : undefined,
        traveling: travelingActorIds.has(actor.id),
      };
    });
  }, [
    actors,
    agentTransitionMs,
    agentVisualPositions,
    engineeringRows,
    playerActorId,
    playerPosition,
    rooms,
    travelingActorIds,
  ]);

  useEffect(() => {
    interactableActorsRef.current = renderedActors
      .filter((item) => !item.isPlayer)
      .map((item) => ({ actor: item.actor, position: item.position }));
    const nextPositions = new Map<string, StageActorPosition>();
    const seenAgentIds = new Set<string>();

    for (const item of renderedActors) {
      const actorId = item.actor.id;
      const logicalPosition = item.logicalPosition;
      const previousPosition = actorPositionRef.current.get(actorId);
      nextPositions.set(actorId, logicalPosition);

      if (item.actor.kind !== "agent") continue;
      seenAgentIds.add(actorId);
      if (!previousPosition || sameStagePosition(previousPosition, logicalPosition)) continue;

      const oldTimers = agentMotionTimersRef.current.get(actorId);
      oldTimers?.forEach((timerId) => window.clearTimeout(timerId));

      const path = computeAgentPath(previousPosition, logicalPosition, rooms);
      const stage = fixedStageSizeRef.current;
      const stageWidth = stage?.width ?? 1200;
      const stageHeight = stage?.height ?? 800;
      const zoomFactor = zoomRef.current || 1;
      const motionTimers: number[] = [];
      let previousPoint = {
        x: parseStagePct(previousPosition.left),
        y: parseStagePct(previousPosition.top),
      };
      let cumulativeMs = 0;

      for (const waypoint of path) {
        const deltaX = ((waypoint.x - previousPoint.x) / 100) * stageWidth;
        const deltaY = ((waypoint.y - previousPoint.y) / 100) * stageHeight;
        const distancePx = Math.hypot(deltaX, deltaY);
        const segmentMs = Math.max(
          AGENT_TRAVEL_MIN_SEGMENT_MS,
          (distancePx * zoomFactor / PLAYER_MOVE_SPEED_PX) * 1000,
        );
        const startMs = cumulativeMs;
        const target = waypoint;
        const timerId = window.setTimeout(() => {
          setAgentTransitionMs((current) => ({ ...current, [actorId]: segmentMs }));
          setAgentVisualPositions((current) => ({
            ...current,
            [actorId]: { left: pct(target.x), top: pct(target.y) },
          }));
        }, Math.max(1, startMs));
        motionTimers.push(timerId);
        cumulativeMs += segmentMs;
        previousPoint = waypoint;
      }
      agentMotionTimersRef.current.set(actorId, motionTimers);

      setTravelingActorIds((current) => {
        if (current.has(actorId)) return current;
        const next = new Set(current);
        next.add(actorId);
        return next;
      });

      const existingTimer = actorTravelTimerRef.current.get(actorId);
      if (existingTimer != null) {
        window.clearTimeout(existingTimer);
      }
      const travelTimerId = window.setTimeout(() => {
        actorTravelTimerRef.current.delete(actorId);
        setTravelingActorIds((current) => {
          if (!current.has(actorId)) return current;
          const next = new Set(current);
          next.delete(actorId);
          return next;
        });
      }, cumulativeMs);
      actorTravelTimerRef.current.set(actorId, travelTimerId);
    }

    actorPositionRef.current = nextPositions;
    const staleAgentIds = Object.keys(agentVisualPositions).filter((id) => !seenAgentIds.has(id));
    if (staleAgentIds.length > 0) {
      queueMicrotask(() => {
        setAgentVisualPositions((current) => {
          const next = { ...current };
          for (const id of staleAgentIds) delete next[id];
          return next;
        });
        setAgentTransitionMs((current) => {
          const next = { ...current };
          for (const id of staleAgentIds) delete next[id];
          return next;
        });
      });
    }
  }, [agentVisualPositions, renderedActors, rooms]);

  useEffect(() => {
    if (!playerActorId) return;
    let rafId = 0;
    let lastFrame = 0;

    function movePlayerByKeys(keys: Record<string, boolean>, deltaSeconds: number): boolean {
      const currentPosition = playerPositionRef.current;
      const stageSize = fixedStageSizeRef.current;
      if (!currentPosition || !stageSize) return false;

      let dx = 0;
      let dy = 0;
      if (keys.w || keys.arrowup) dy -= 1;
      if (keys.s || keys.arrowdown) dy += 1;
      if (keys.a || keys.arrowleft) dx -= 1;
      if (keys.d || keys.arrowright) dx += 1;
      if (dx === 0 && dy === 0) return false;

      const length = Math.hypot(dx, dy) || 1;
      const speedPx = keys.shift ? PLAYER_MOVE_FAST_SPEED_PX : PLAYER_MOVE_SPEED_PX;
      const stepPx = speedPx * deltaSeconds;
      const direction = { x: dx / length, y: dy / length };
      const deltaXPct = (direction.x * stepPx * 100) / Math.max(1, stageSize.width * zoomRef.current);
      const deltaYPct = (direction.y * stepPx * 100) / Math.max(1, stageSize.height * zoomRef.current);
      const nextPosition = resolveStageMove(currentPosition, deltaXPct, deltaYPct, wallsRef.current);
      const nextDirection: PixelAvatarDirection =
        Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "E" : "W") : dy < 0 ? "N" : "S";
      playerPositionRef.current = nextPosition;
      setPlayerDirection(nextDirection);
      setPlayerWalking(true);
      const targetPan = getStagePanForPosition(nextPosition, stageSize, zoomRef.current);
      const nextPan = smoothStagePan(panRef.current, targetPan, CAMERA_FOLLOW_SMOOTHING);
      panRef.current = nextPan;
      onPanChangeRef.current(nextPan);
      void onPlayerPositionChangeRef.current?.(nextPosition);
      return true;
    }

    function handleKeyDown(event: KeyboardEvent) {
      const key = event.key.toLowerCase();
      if (key === "/") {
        if (event.metaKey || event.ctrlKey || event.altKey) return;
        if (shouldIgnoreStageMovementKey(event.target)) return;
        const keyboardPosition = playerPositionRef.current;
        if (!keyboardPosition) return;
        if (isNearQuestMarker(keyboardPosition)) {
          event.preventDefault();
          onOpenTasksRef.current?.();
          return;
        }
        const nearestActor = findNearestInteractableStageActor(
          keyboardPosition,
          interactableActorsRef.current,
        );
        if (nearestActor) {
          event.preventDefault();
          onActorSelectRef.current(nearestActor);
        }
        return;
      }

      if (!isPlayerMoveKey(key) && key !== "shift") return;
      if (event.metaKey || event.ctrlKey || event.altKey) return;
      if (shouldIgnoreStageMovementKey(event.target)) return;
      if (isPlayerMoveKey(key)) event.preventDefault();
      keysRef.current[key] = true;
      if (isPlayerMoveKey(key)) {
        movePlayerByKeys(keysRef.current, 1 / 30);
      }
    }

    function handleKeyUp(event: KeyboardEvent) {
      keysRef.current[event.key.toLowerCase()] = false;
    }

    function tick(time: number) {
      const currentPosition = playerPositionRef.current;
      const stageSize = fixedStageSizeRef.current;
      if (currentPosition && stageSize) {
        const deltaSeconds = lastFrame ? Math.min(0.05, (time - lastFrame) / 1000) : 0;
        if (!movePlayerByKeys(keysRef.current, deltaSeconds)) {
          setPlayerWalking(false);
        }
      }
      lastFrame = time;
      rafId = window.requestAnimationFrame(tick);
    }

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    rafId = window.requestAnimationFrame(tick);
    return () => {
      keysRef.current = {};
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      window.cancelAnimationFrame(rafId);
    };
  }, [playerActorId]);

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    if (event.button !== 0) return;
    if ((event.target as HTMLElement).closest("button")) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    setDrag({
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      originX: pan.x,
      originY: pan.y,
    });
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    if (!drag || drag.pointerId !== event.pointerId) return;
    onPanChange({
      x: drag.originX + event.clientX - drag.startX,
      y: drag.originY + event.clientY - drag.startY,
    });
  }

  function handlePointerEnd(event: ReactPointerEvent<HTMLDivElement>) {
    if (!drag || drag.pointerId !== event.pointerId) return;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    setDrag(null);
  }

  function handleDoubleClick(event: ReactMouseEvent<HTMLDivElement>) {
    if (!onPresenceMove) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    const localX = (event.clientX - bounds.left - bounds.width / 2 - pan.x) / zoom + worldSize.width / 2;
    const localY = (event.clientY - bounds.top - bounds.height / 2 - pan.y) / zoom + worldSize.height / 2;
    void onPresenceMove({
      left: pct((localX / worldSize.width) * 100),
      top: pct((localY / worldSize.height) * 100),
    });
  }

  function handleWheel(event: ReactWheelEvent<HTMLDivElement>) {
    event.preventDefault();
    event.stopPropagation();
    onZoomChange(zoom + (event.deltaY < 0 ? STAGE_ZOOM_STEP : -STAGE_ZOOM_STEP));
  }

  return (
    <div
      className="workspace-2d-stage"
      ref={stageRef}
      role="img"
      aria-label="Agent and member 2D workspace scene"
      tabIndex={0}
      style={{ cursor: drag ? "grabbing" : "grab" }}
      onWheel={handleWheel}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      onDoubleClick={handleDoubleClick}
    >
      <div
        className="workspace-2d-world"
        style={{
          width: worldSize.width,
          height: worldSize.height,
          marginLeft: -(worldSize.width / 2),
          marginTop: -(worldSize.height / 2),
          transform: `translate3d(${pan.x}px, ${pan.y}px, 0)`,
          transition: drag || playerWalking ? "none" : "transform 160ms ease-out",
        }}
      >
        <div
          className="workspace-2d-world-scale"
          style={{
            transform: `scale(${zoom})`,
            transition: drag ? "none" : "transform 160ms ease-out",
          }}
        >
          <div className="workspace-2d-grid" />
          <StageCorridors rooms={rooms} />
          {rooms.map((room) => (
            <StageRoom key={room.label} room={room} engineeringRows={engineeringRows} />
          ))}
          <QuestMarker active={playerNearQuest} position={QUEST_MARKER_POSITION} />

          <div className="workspace-2d-actor-layer" aria-label="Workspace actors">
            {renderedActors.map(({ actor, isPlayer, position, transitionMs, traveling }) => (
              <StageActor
                actor={actor}
                position={position}
                isPlayer={isPlayer}
                playerAvatar={playerAvatar}
                travel={actor.kind === "agent"}
                travelMs={transitionMs}
                walking={(isPlayer && playerWalking) || traveling}
                direction={isPlayer ? playerDirection : "S"}
                selected={actor.id === selectedActorId}
                key={actor.id}
                onSelect={onActorSelect}
                onContextMenu={onActorContextMenu}
              />
            ))}
          </div>

          {actors.length === 0 ? (
            <div className="workspace-2d-empty-floor">
              <p>NO WORKSPACE MEMBERS ON FLOOR</p>
              <span>AGENT 버튼에서 Agent를 고용하거나 WORKSPACE에서 멤버를 초대하세요.</span>
            </div>
          ) : null}
        </div>
      </div>
      <div className="workspace-2d-status">
        <span>
          x {Math.round(pan.x)} · y {Math.round(pan.y)}
        </span>
        <span>{actors.length} actors</span>
      </div>
    </div>
  );
}

function StageRoom({ room, engineeringRows }: { room: StageRoomSpec; engineeringRows: number }) {
  const doorPct = room.doorAt * 100;
  const doorHalfPct = 5;
  const wallStyle = {
    position: "absolute" as const,
    background: "rgba(230, 234, 240, 0.16)",
  };

  return (
    <section
      className={`stage-room stage-room-${room.variant}`}
      style={{
        left: room.left,
        top: room.top,
        width: room.width,
        height: room.height,
        borderColor: "rgba(230, 234, 240, 0.12)",
        background: "rgba(24, 25, 29, 0.72)",
      }}
    >
      {room.doorSide === "top" ? (
        <>
          <div style={{ ...wallStyle, left: 0, top: 0, height: 2, width: `${Math.max(0, doorPct - doorHalfPct)}%` }} />
          <div style={{ ...wallStyle, left: `${Math.min(100, doorPct + doorHalfPct)}%`, top: 0, height: 2, right: 0 }} />
        </>
      ) : (
        <div style={{ ...wallStyle, left: 0, top: 0, right: 0, height: 2 }} />
      )}
      {room.doorSide === "bottom" ? (
        <>
          <div style={{ ...wallStyle, left: 0, bottom: 0, height: 2, width: `${Math.max(0, doorPct - doorHalfPct)}%` }} />
          <div style={{ ...wallStyle, left: `${Math.min(100, doorPct + doorHalfPct)}%`, bottom: 0, height: 2, right: 0 }} />
        </>
      ) : (
        <div style={{ ...wallStyle, left: 0, bottom: 0, right: 0, height: 2 }} />
      )}
      {room.doorSide === "left" ? (
        <>
          <div style={{ ...wallStyle, left: 0, top: 0, width: 2, height: `${Math.max(0, doorPct - doorHalfPct)}%` }} />
          <div style={{ ...wallStyle, left: 0, top: `${Math.min(100, doorPct + doorHalfPct)}%`, width: 2, bottom: 0 }} />
        </>
      ) : (
        <div style={{ ...wallStyle, left: 0, top: 0, bottom: 0, width: 2 }} />
      )}
      {room.doorSide === "right" ? (
        <>
          <div style={{ ...wallStyle, right: 0, top: 0, width: 2, height: `${Math.max(0, doorPct - doorHalfPct)}%` }} />
          <div style={{ ...wallStyle, right: 0, top: `${Math.min(100, doorPct + doorHalfPct)}%`, width: 2, bottom: 0 }} />
        </>
      ) : (
        <div style={{ ...wallStyle, right: 0, top: 0, bottom: 0, width: 2 }} />
      )}
      <div className="stage-room-label" style={{ color: t4.dim }}>
        <GlyphText glyph="♦">
          {room.label}
          {room.sub ? ` · ${room.sub}` : ""}
        </GlyphText>
      </div>
      {room.variant === "engineering" ? <EngineeringPlatforms rows={engineeringRows} /> : null}
      {room.variant === "huddle" ? <HuddleTable /> : null}
      {room.variant === "rest" ? <RestLounge /> : null}
      {room.variant === "library" ? <LibraryRoom /> : null}
    </section>
  );
}

function StageCorridors({ rooms }: { rooms: StageRoomSpec[] }) {
  const engineering = rooms.find((room) => room.variant === "engineering");
  const huddle = rooms.find((room) => room.variant === "huddle");
  const lounge = rooms.find((room) => room.variant === "rest");
  const library = rooms.find((room) => room.variant === "library");
  if (!engineering || !huddle || !lounge || !library) return null;

  const engineeringRight = parseStagePct(engineering.left) + parseStagePct(engineering.width);
  const huddleLeft = parseStagePct(huddle.left);
  const topY = Math.min(parseStagePct(engineering.top), parseStagePct(huddle.top));
  const bottomY = Math.max(
    parseStagePct(lounge.top) + parseStagePct(lounge.height),
    parseStagePct(library.top) + parseStagePct(library.height),
  );
  const lowerTop = Math.min(parseStagePct(lounge.top), parseStagePct(library.top));
  const engineeringBottom = parseStagePct(engineering.top) + parseStagePct(engineering.height);
  const verticalWidth = Math.max(1.3, Math.min(2.2, huddleLeft - engineeringRight));
  const verticalCenter = engineeringRight + (huddleLeft - engineeringRight) / 2;
  const horizontalHeight = 3.4;
  const horizontalTop = engineeringBottom + (lowerTop - engineeringBottom) / 2 - horizontalHeight / 2;

  return (
    <>
      <div
        className="stage-corridor stage-corridor-vertical"
        style={{
          left: pct(verticalCenter - verticalWidth / 2),
          top: pct(topY),
          width: pct(verticalWidth),
          height: pct(bottomY - topY),
        }}
      />
      <div
        className="stage-corridor stage-corridor-horizontal"
        style={{
          top: pct(horizontalTop),
          height: pct(horizontalHeight),
        }}
      />
    </>
  );
}

function EngineeringPlatforms({ rows }: { rows: number }) {
  const rowCenters = getDeskRowCenters(rows);
  const deskHeight = getDeskHeightPct(rows);
  return (
    <>
      {rowCenters.flatMap((centerY, rowIndex) =>
        AGENT_DESK_COL_CENTERS.map((centerX, columnIndex) => (
          <StageDesk
            key={`${rowIndex}-${columnIndex}`}
            left={pct(centerX - AGENT_DESK_W_PCT / 2)}
            top={pct(centerY - deskHeight / 2)}
            width={pct(AGENT_DESK_W_PCT)}
            height={pct(deskHeight)}
          />
        )),
      )}
      <StagePlant left="92%" top="8%" color={t4.ok} />
      <StagePlant left="92%" top="82%" color={t4.ok} />
    </>
  );
}

function StageDesk({ left, top, width, height }: { left: string; top: string; width: string; height: string }) {
  return (
    <div className="stage-desk" style={{ left, top, width, height }}>
      <div className="stage-desk-table" />
      <div className="stage-desk-monitor" />
    </div>
  );
}

function StageChair({ left, top, color = t4.line }: { left: string; top: string; color?: string }) {
  return (
    <div className="stage-chair" style={{ left, top }}>
      <div style={{ borderColor: color }} />
      <span style={{ background: color }} />
    </div>
  );
}

function StagePlant({ left, top, color = t4.ok }: { left: string; top: string; color?: string }) {
  return (
    <div className="stage-plant" style={{ left, top }}>
      <div style={{ borderColor: `${color}55` }} />
      <span style={{ background: color }} />
      <span style={{ background: color }} />
      <span style={{ background: color }} />
    </div>
  );
}

function CoffeeStation({ left, top }: { left: string; top: string }) {
  return (
    <div className="stage-coffee" style={{ left, top }}>
      <div />
      <span />
      <strong />
    </div>
  );
}

function HuddleTable() {
  return (
    <>
      <div className="stage-huddle-table" />
      <StageChair left="25%" top="35%" color={t4.mp} />
      <StageChair left="72%" top="35%" color={t4.mp} />
      <StageChair left="25%" top="68%" color={t4.mp} />
      <StageChair left="72%" top="68%" color={t4.mp} />
    </>
  );
}

function RestLounge() {
  return (
    <>
      <div className="stage-lounge-bench" />
      <div className="stage-lounge-table" />
      <CoffeeStation left="8%" top="72%" />
      <StagePlant left="84%" top="10%" color={t4.ok} />
      <StagePlant left="5%" top="10%" color={t4.ok} />
    </>
  );
}

function LibraryRoom() {
  return (
    <>
      <div className="stage-library-shelf stage-library-shelf-a" />
      <div className="stage-library-shelf stage-library-shelf-b" />
      <div className="stage-library-shelf stage-library-shelf-c" />
      <div className="stage-library-shelf stage-library-shelf-d" />
      <div className="stage-library-table" />
      <StagePlant left="4%" top="45%" color={t4.xp} />
      <StagePlant left="88%" top="45%" color={t4.xp} />
    </>
  );
}

function QuestMarker({ active, position }: { active: boolean; position: StageActorPosition }) {
  return (
    <button
      type="button"
      className={`stage-quest-marker ${active ? "stage-quest-marker-active" : ""}`}
      style={{ left: position.left, top: position.top }}
      aria-label="Task marker"
    >
      <span>TASK</span>
      {active ? <strong>/ 태스크 보드</strong> : null}
    </button>
  );
}

function StageActor({
  actor,
  position,
  isPlayer = false,
  playerAvatar,
  travel,
  travelMs,
  walking,
  direction = "S",
  selected,
  onContextMenu,
  onSelect,
}: {
  actor: WorkspaceStageActor;
  position: StageActorPosition;
  isPlayer?: boolean;
  playerAvatar: { kind: PixelAvatarKind; paletteOverride?: PixelAvatarPaletteOverride } | null;
  travel?: boolean;
  travelMs?: number;
  walking?: boolean;
  direction?: PixelAvatarDirection;
  selected: boolean;
  onContextMenu: (payload: { actor: WorkspaceStageActor; x: number; y: number }) => void;
  onSelect: (actor: WorkspaceStageActor) => void;
}) {
  const accent = actor.kind === "agent" ? t4.agent : t4.mp;
  const speech = getWorkspaceActorSpeech(actor);
  const visibleSpeech = isPlayer ? null : speech;
  const motion = walking ? "walking" : "idle";
  const avatarKind = isPlayer && playerAvatar ? playerAvatar.kind : actor.avatarKind;
  return (
    <button
      type="button"
      className={`stage-actor stage-actor--${motion} ${actor.kind === "member" ? "stage-actor-member" : "stage-actor-agent"} ${
        isPlayer ? "stage-actor-player" : ""
      } ${selected ? "stage-actor-selected" : ""}`}
      style={{
        left: position.left,
        top: position.top,
        transition: isPlayer
          ? "none"
          : travel
            ? `left ${travelMs ?? 320}ms linear, top ${travelMs ?? 320}ms linear`
            : actor.kind === "member"
              ? "left 30ms linear, top 30ms linear"
              : "left 90ms linear, top 90ms linear",
      }}
      onClick={() => onSelect(actor)}
      onContextMenu={(event) => {
        event.preventDefault();
        if (isPlayer) return;
        onContextMenu({ actor, x: event.clientX, y: event.clientY });
      }}
      aria-label={isPlayer ? `${actor.label} 플레이어` : `${actor.label} 채팅`}
    >
      {isPlayer ? <span aria-hidden="true" className="stage-actor__player-marker" /> : null}
      {visibleSpeech ? (
        <div
          className={`stage-actor__speech stage-actor__speech-${visibleSpeech.tone}`}
          title={visibleSpeech.text || actor.detail}
        >
          <span>{visibleSpeech.label}</span>
          {visibleSpeech.text ? <span>{visibleSpeech.text}</span> : null}
        </div>
      ) : null}
      <div className="stage-actor__badge" style={{ borderColor: selected ? accent : "rgba(230, 234, 240, 0.18)" }}>
        {!isPlayer ? <span className={`stage-actor__dot stage-actor__dot-${actor.status}`} /> : null}
        <span>{isPlayer ? "YOU" : actor.label}</span>
      </div>
      <div className="stage-actor__avatar">
        <PixelAvatar
          kind={avatarKind}
          paletteOverride={isPlayer ? playerAvatar?.paletteOverride : undefined}
          direction={direction}
          size={3}
          walking={Boolean(walking)}
        />
      </div>
    </button>
  );
}

function MapHud({
  rooms,
  myPosition,
  totalActors,
  zoom,
  pan,
  stageSize,
  onRoomClick,
}: {
  rooms: StageMiniMapRoom[];
  myPosition: StageActorPosition | null;
  totalActors: number;
  zoom: number;
  pan: StagePan;
  stageSize: StageSize;
  onRoomClick: (room: StageMiniMapRoom) => void;
}) {
  const viewportWidth = clampMiniMapValue(100 / zoom, 24, 100);
  const viewportHeight = clampMiniMapValue(100 / zoom, 24, 100);
  const viewportLeft = clampMiniMapValue(
    50 - viewportWidth / 2 - (pan.x / (Math.max(1, stageSize.width) * zoom)) * 100,
    0,
    100 - viewportWidth,
  );
  const viewportTop = clampMiniMapValue(
    50 - viewportHeight / 2 - (pan.y / (Math.max(1, stageSize.height) * zoom)) * 100,
    0,
    100 - viewportHeight,
  );

  return (
    <T4Panel label="MAP · FLOOR 01" accent={t4.mp} className="workspace-2d-map-hud" style={{ padding: 8 }}>
      <div className="workspace-2d-minimap" aria-label="Office scene minimap">
        {rooms.map((room) => (
          <button
            aria-label={`Move view to ${room.label}`}
            className={`workspace-2d-minimap-zone workspace-2d-minimap-room-button workspace-2d-minimap-${room.variant}`}
            key={room.id}
            onClick={() => onRoomClick(room)}
            type="button"
            style={{
              left: room.x,
              top: room.y,
              width: room.w,
              height: room.h,
            }}
          />
        ))}
        {myPosition ? (
          <span
            aria-label="내 위치"
            className="workspace-2d-minimap-actor workspace-2d-minimap-actor-player"
            style={{ left: myPosition.left, top: myPosition.top }}
          />
        ) : null}
        <span
          className="workspace-2d-minimap-view"
          style={{
            left: `${viewportLeft}%`,
            top: `${viewportTop}%`,
            width: `${viewportWidth}%`,
            height: `${viewportHeight}%`,
          }}
        />
      </div>
      <div className="workspace-2d-map-caption">
        <span>
          <GlyphText glyph="◆">{String(totalActors).padStart(2, "0")} ACTORS</GlyphText>
        </span>
        <span>
          <GlyphText glyph="●">YOU</GlyphText>
        </span>
      </div>
    </T4Panel>
  );
}

function MapZoomControls({
  zoom,
  onZoomOut,
  onReset,
  onZoomIn,
}: {
  zoom: number;
  onZoomOut: () => void;
  onReset: () => void;
  onZoomIn: () => void;
}) {
  return (
    <div className="workspace-2d-map-controls">
      <ZoomButton onClick={onZoomOut} disabled={zoom <= 0.75}>
        -
      </ZoomButton>
      <button type="button" onClick={onReset}>
        {Math.round(zoom * 100)}%
      </button>
      <ZoomButton onClick={onZoomIn} disabled={zoom >= 1.5}>
        +
      </ZoomButton>
    </div>
  );
}

function ZoomButton({ children, disabled, onClick }: { children: ReactNode; disabled?: boolean; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}

function DialogueBox({
  actor,
  playerActorId,
  onTalk,
  onTasks,
}: {
  actor: WorkspaceStageActor | null;
  playerActorId: string | null;
  onTalk: (actor: WorkspaceStageActor) => void;
  onTasks?: () => void;
}) {
  const isAgent = actor?.kind === "agent";
  const isSelf = Boolean(actor && playerActorId === actor.id);
  const speaker = actor?.label ?? "GUIDE";
  const accent = actor ? (isAgent ? t4.agent : t4.mp) : t4.dim;
  const line = actor
    ? isAgent
      ? `${actor.detail} · 다음 작업을 할당할 수 있습니다.`
      : `${speaker} · ${actor.sourceStatus}`
    : "No members or agents on the floor yet. Hire an agent or invite teammates from WORKSPACE.";

  return (
    <div className="workspace-2d-dialogue" aria-live="polite">
      <div className="workspace-2d-dialogue-avatar" style={{ borderColor: "rgba(230, 234, 240, 0.14)" }}>
        <PixelAvatar kind={actor?.avatarKind ?? "agent"} size={3} />
      </div>
      <div>
        <strong style={{ color: accent }}>
          <GlyphText glyph={isAgent ? "◇" : actor ? "●" : "★"} truncate>
            {speaker.toUpperCase()}
          </GlyphText>
        </strong>
        <p title={line}>{line}</p>
        <div className="workspace-2d-dialogue-actions">
          {actor ? (
            <DialogueChoice primary disabled={isSelf} onClick={() => onTalk(actor)}>
              <GlyphText glyph="▶">TALK</GlyphText>
            </DialogueChoice>
          ) : null}
          <DialogueChoice onClick={onTasks}>TASKS</DialogueChoice>
        </div>
      </div>
    </div>
  );
}

function DialogueChoice({
  children,
  primary,
  disabled,
  onClick,
}: {
  children: ReactNode;
  primary?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      className={primary ? "workspace-2d-dialogue-primary" : undefined}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}

function WorkspaceContextMenu({
  menu,
  onAction,
}: {
  menu: WorkspaceContextMenuState;
  onAction: (action: WorkspaceContextMenuAction) => void;
}) {
  return (
    <div className="workspace-2d-context-menu" style={{ left: `${menu.screenX}px`, top: `${menu.screenY}px` }}>
      {menu.actions.map((action) => (
        <button type="button" key={action} onClick={() => onAction(action)}>
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

function getDeskRowCenters(rows: number): number[] {
  if (rows <= 1) return [55];
  if (rows === 2) return [38.5, 67.5];
  const start = 28;
  const end = 82;
  const step = (end - start) / (rows - 1);
  return Array.from({ length: rows }, (_, index) => start + index * step);
}

function getDeskHeightPct(rows: number): number {
  if (rows <= 2) return 15;
  return Math.max(8, Math.min(15, 60 / rows));
}

function clampMiniMapValue(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function isPlayerMoveKey(key: string): boolean {
  switch (key) {
    case "arrowup":
    case "arrowdown":
    case "arrowleft":
    case "arrowright":
    case "w":
    case "a":
    case "s":
    case "d":
      return true;
    default:
      return false;
  }
}

function shouldIgnoreStageMovementKey(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  const tagName = target.tagName.toLowerCase();
  return tagName === "input" || tagName === "textarea" || tagName === "select" || target.isContentEditable;
}
