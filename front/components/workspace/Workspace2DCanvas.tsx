"use client";

import { Crosshair, FolderTree, GitBranch, Maximize2, MousePointer2, Users } from "lucide-react";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
  type WheelEvent as ReactWheelEvent,
} from "react";
import {
  PixelAvatar,
  type PixelAvatarDirection,
  type PixelAvatarKind,
  type PixelAvatarPaletteOverride,
} from "@/components/arcade";
import { OfficeTilemapCanvas } from "@/components/workspace/OfficeTilemapCanvas";
import type { OfficeThemeId } from "@/lib/pixel-office/pixtuoid-sprites";
import type { WorkspaceAgent } from "@/lib/agents";
import type { WorkspacePresenceMember, WorkspacePresencePosition } from "@/lib/presence";
import type { WorkspaceTask } from "@/lib/tasks";
import {
  createOfficePathfindingContext,
  findOfficePath,
  rasterizeWallRectsToBitgrid,
  type OfficePathfindingContext,
  type PathPoint,
} from "../../lib/office-pathfinding";
import {
  buildOfficeSpatialKdTree,
  type SpatialPointInput,
} from "../../lib/office-spatial-kdtree";
import {
  buildStageObstacleRects,
  buildStageWallBitset,
  buildStageWalls,
  clampContextMenuPosition,
  clampNumber,
  clampStageZoom,
  computeAgentPath,
  createWorkspaceAvatarNodes,
  createWorkspaceContextMenuState,
  createWorkspaceStageActors,
  findNearestInteractableStageActor,
  getAgentLoungePosition,
  getAgentPositionInStage,
  getDeskHeightPct,
  getDeskRowCenters,
  getEngineeringRows,
  getStageMiniMapRooms,
  getStagePanForPosition,
  getStageRooms,
  getStageWorldSize,
  getWorkspaceActorSpeech,
  indexStageRoomsByVariant,
  isNearQuestMarker,
  isPlayerMoveKey,
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
  type WorkspaceStageActorStatus,
} from "@/lib/workspace-2d";
import type { WorkspaceMember } from "@/lib/workspaces";
import {
  ACTOR_FLAG,
  countOccupiedBits,
  createSpatialBitset,
  matchesActorBitmask,
  packActorBitfield,
  resetSpatialBitset,
  setSpatialBit,
  type SpatialBitset,
} from "../../lib/office-spatial-bitmask";

export type Workspace2DCanvasProps = {
  agents: WorkspaceAgent[];
  members: WorkspaceMember[];
  presenceMembers: WorkspacePresenceMember[];
  tasks?: WorkspaceTask[];
  variant?: "panel" | "fullscreen";
  playerPosition?: StageActorPosition | null;
  playerAvatar?: {
    kind: PixelAvatarKind;
    paletteOverride?: PixelAvatarPaletteOverride;
  } | null;
  playerMemberId?: number | null;
  rooms?: StageRoomSpec[];
  cameraFocusTarget?: { position: StageActorPosition; nonce: number } | null;
  onPresenceMove?: (position: WorkspacePresencePosition) => void | Promise<void>;
  onPlayerPositionChange?: (position: StageActorPosition) => void | Promise<void>;
  onActorSelect?: (node: WorkspaceSceneNode) => void;
  onOpenTasks?: () => void;
  onOpenOrchestration?: () => void;
  onOpenArtifacts?: () => void;
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
const OFFICE_THEME_STORAGE_KEY = "ao-office-theme";
const OFFICE_THEME_EVENT = "ao-office-theme-change";
const OFFICE_THEME_OPTIONS: ReadonlyArray<{ id: OfficeThemeId; label: string }> = [
  { id: "auto", label: "자동" },
  { id: "tokyo-night", label: "Tokyo Night" },
  { id: "catppuccin", label: "Catppuccin" },
  { id: "dracula", label: "Dracula" },
  { id: "gruvbox", label: "Gruvbox" },
  { id: "daylight", label: "Daylight" },
];
const VALID_OFFICE_THEMES = new Set<string>(OFFICE_THEME_OPTIONS.map((opt) => opt.id));

const noopSubscribe = () => () => {};

function subscribeOfficeTheme(callback: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  const onStorage = (event: StorageEvent) => {
    if (event.key === null || event.key === OFFICE_THEME_STORAGE_KEY) {
      callback();
    }
  };
  window.addEventListener("storage", onStorage);
  window.addEventListener(OFFICE_THEME_EVENT, callback);
  return () => {
    window.removeEventListener("storage", onStorage);
    window.removeEventListener(OFFICE_THEME_EVENT, callback);
  };
}

function getOfficeThemeSnapshot(): OfficeThemeId {
  if (typeof window === "undefined") return "auto";
  try {
    const saved = window.localStorage.getItem(OFFICE_THEME_STORAGE_KEY);
    if (saved && VALID_OFFICE_THEMES.has(saved)) {
      return saved as OfficeThemeId;
    }
  } catch {
    // Ignore storage read errors
  }
  return "auto";
}

function getServerOfficeThemeSnapshot(): OfficeThemeId {
  return "auto";
}

function getFocusgroupSupportSnapshot(): boolean {
  return typeof HTMLElement !== "undefined" && "focusGroup" in HTMLElement.prototype;
}

function getServerFocusgroupSupportSnapshot(): boolean {
  return false;
}

const ROOM_DISPLAY_LABELS: Record<StageRoomSpec["variant"], { label: string; sub: string }> = {
  engineering: { label: "엔지니어링실", sub: "에이전트 작업 구역" },
  planning: { label: "기획실", sub: "설계 · 요구사항" },
  review: { label: "검토실", sub: "코드 · 승인 리뷰" },
  huddle: { label: "회의실", sub: "협업 · 오케스트레이션" },
  rest: { label: "휴게실", sub: "대기 · 검토 라운지" },
  library: { label: "서재", sub: "산출물 · 문서 보관" },
};

function actorStatusKoreanLabel(status: WorkspaceStageActorStatus): string {
  switch (status) {
    case "working":
      return "실행 중";
    case "review":
      return "승인 대기";
    case "blocked":
      return "오류";
    case "idle":
      return "대기";
  }
}

export default function Workspace2DCanvas({
  agents,
  members,
  presenceMembers,
  tasks = [],
  variant = "panel",
  playerPosition = null,
  playerAvatar = null,
  playerMemberId = null,
  rooms = [],
  cameraFocusTarget = null,
  onPresenceMove,
  onPlayerPositionChange,
  onActorSelect,
  onOpenTasks,
  onOpenOrchestration,
  onOpenArtifacts,
}: Workspace2DCanvasProps) {
  const frameRef = useRef<HTMLDivElement | null>(null);
  const stageShellRef = useRef<HTMLDivElement | null>(null);
  const [stageViewportSize, setStageViewportSize] = useState<StageSize>({ width: 1280, height: 800 });
  const isFullscreenStage = variant === "fullscreen";
  const initialStageZoom = isFullscreenStage ? 0.75 : 1;
  const engineeringRows = useMemo(() => getEngineeringRows(agents.length), [agents.length]);
  const stageWorldSize = useMemo(() => getStageWorldSize(engineeringRows), [engineeringRows]);
  const stageRooms = useMemo(
    () => (rooms.length ? rooms : getStageRooms(engineeringRows, stageWorldSize, agents.length)),
    [agents.length, engineeringRows, rooms, stageWorldSize],
  );
  const roomsByVariant = useMemo(() => indexStageRoomsByVariant(stageRooms), [stageRooms]);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [contextMenu, setContextMenu] = useState<WorkspaceContextMenuState | null>(null);
  const [stageZoom, setStageZoom] = useState(initialStageZoom);
  const officeTheme = useSyncExternalStore(
    subscribeOfficeTheme,
    getOfficeThemeSnapshot,
    getServerOfficeThemeSnapshot,
  );
  const supportsFocusgroup = useSyncExternalStore(
    noopSubscribe,
    getFocusgroupSupportSnapshot,
    getServerFocusgroupSupportSnapshot,
  );
  const [showCollabLinks, setShowCollabLinks] = useState(true);
  const [motionPaused, setMotionPaused] = useState(false);
  const [toolbarFocusIndex, setToolbarFocusIndex] = useState(0);
  const toolbarRef = useRef<HTMLDivElement | null>(null);

  const handleSelectOfficeTheme = useCallback((nextTheme: OfficeThemeId) => {
    try {
      window.localStorage.setItem(OFFICE_THEME_STORAGE_KEY, nextTheme);
      window.dispatchEvent(new Event(OFFICE_THEME_EVENT));
    } catch {
      // Ignore storage write errors
    }
  }, []);

  const handleToolbarKeyDown = useCallback(
    (event: ReactKeyboardEvent<HTMLDivElement>) => {
      if (supportsFocusgroup) return;
      const root = toolbarRef.current;
      if (!root) return;
      const buttons = Array.from(
        root.querySelectorAll<HTMLButtonElement>("button:not([disabled])"),
      );
      if (buttons.length === 0) return;

      const currentIdx = buttons.findIndex((btn) => btn === document.activeElement);
      let nextIdx = -1;
      if (event.key === "ArrowRight") {
        nextIdx = (currentIdx + 1) % buttons.length;
      } else if (event.key === "ArrowLeft") {
        nextIdx = (currentIdx - 1 + buttons.length) % buttons.length;
      } else if (event.key === "Home") {
        nextIdx = 0;
      } else if (event.key === "End") {
        nextIdx = buttons.length - 1;
      }
      if (nextIdx >= 0) {
        event.preventDefault();
        setToolbarFocusIndex(nextIdx);
        buttons[nextIdx]?.focus();
      }
    },
    [supportsFocusgroup],
  );
  const [stagePanOverride, setStagePanOverride] = useState<{
    focusNonce: number | undefined;
    pan: StagePan;
  } | null>(null);
  const stagePan = useMemo(
    () =>
      cameraFocusTarget && stagePanOverride?.focusNonce !== cameraFocusTarget.nonce
        ? getStagePanForPosition(cameraFocusTarget.position, stageWorldSize, stageZoom)
        : (stagePanOverride?.pan ?? { x: 0, y: 0 }),
    [cameraFocusTarget, stagePanOverride, stageWorldSize, stageZoom],
  );
  const setStagePan = useCallback(
    (nextPan: StagePan) => {
      setStagePanOverride({ focusNonce: cameraFocusTarget?.nonce, pan: nextPan });
    },
    [cameraFocusTarget?.nonce],
  );
  const [localPlayerMove, setLocalPlayerMove] = useState<{
    propPosition: StageActorPosition | null;
    position: StageActorPosition;
  } | null>(null);
  const initialCameraRef = useRef(false);

  const effectivePlayerPosition =
    localPlayerMove && localPlayerMove.propPosition === playerPosition
      ? localPlayerMove.position
      : playerPosition;
  const handleLocalPlayerPositionChange = useCallback(
    (nextPosition: StageActorPosition) => {
      setLocalPlayerMove({ propPosition: playerPosition, position: nextPosition });
    },
    [playerPosition],
  );

  const nodes = useMemo(
    () => createWorkspaceAvatarNodes({ agents, members, presenceMembers }),
    [agents, members, presenceMembers],
  );
  const nodesById = useMemo(
    () => new Map<string, WorkspaceSceneNode>(nodes.map((node) => [node.id, node])),
    [nodes],
  );
  const stageActors = useMemo(
    () =>
      createWorkspaceStageActors({
        agents,
        members,
        presenceMembers,
        tasks,
        rooms: stageRooms,
        roomsByVariant,
        worldSize: stageWorldSize,
      }),
    [agents, members, presenceMembers, roomsByVariant, stageRooms, stageWorldSize, tasks],
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
        actor.id === playerActorId && effectivePlayerPosition
          ? {
              ...actor,
              position: effectivePlayerPosition,
              avatarKind: playerAvatar?.kind ?? actor.avatarKind,
            }
          : actor,
      ),
    [effectivePlayerPosition, playerActorId, playerAvatar?.kind, stageActors],
  );
  const actorsById = useMemo(
    () => new Map<string, WorkspaceStageActor>(logicalStageActors.map((actor) => [actor.id, actor])),
    [logicalStageActors],
  );
  const miniMapRooms = useMemo(() => getStageMiniMapRooms(stageRooms), [stageRooms]);
  const selectedNode = (selectedNodeId ? nodesById.get(selectedNodeId) : undefined) ?? null;
  const selectedStageActor =
    (selectedNodeId ? actorsById.get(selectedNodeId) : undefined) ??
    (playerActorId ? actorsById.get(playerActorId) : undefined) ??
    logicalStageActors[0] ??
    null;

  const [occupiedTileCount, onlineAgentCount, activeAgents, activeMembers] = useMemo(() => {
    const gridWidth = 40;
    const gridHeight = 24;
    const bitset = createSpatialBitset(gridWidth, gridHeight);

    let onlineAgents = 0;
    let readyAgents = 0;
    let onlineMembers = 0;
    const maxCount = Math.max(logicalStageActors.length, agents.length, presenceMembers.length);

    for (let index = 0; index < maxCount; index += 1) {
      if (index < agents.length && agents[index].status === "READY") {
        readyAgents += 1;
      }
      if (index < presenceMembers.length && presenceMembers[index].status === "ACTIVE") {
        onlineMembers += 1;
      }
      if (index >= logicalStageActors.length) {
        continue;
      }

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
        onlineAgents += 1;
      }
    }

    return [countOccupiedBits(bitset), onlineAgents, readyAgents, onlineMembers] as const;
  }, [agents, logicalStageActors, playerActorId, presenceMembers, selectedNodeId, selectedStageActor?.id]);

  useEffect(() => {
    const shell = stageShellRef.current;
    if (!shell || typeof ResizeObserver === "undefined") {
      return;
    }
    const updateSize = (width: number, height: number) => {
      if (width <= 0 || height <= 0) return;
      setStageViewportSize((current) =>
        Math.abs(current.width - width) < 1 && Math.abs(current.height - height) < 1
          ? current
          : { width: Math.round(width), height: Math.round(height) },
      );
    };
    const rect = shell.getBoundingClientRect();
    updateSize(rect.width, rect.height);
    const observer = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (!entry) return;
      updateSize(entry.contentRect.width, entry.contentRect.height);
    });
    observer.observe(shell);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!contextMenu) return;
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setContextMenu(null);
      }
    };
    const handlePointerDownOutside = (event: PointerEvent) => {
      if (event.target instanceof HTMLElement && event.target.closest(".workspace-2d-context-menu")) {
        return;
      }
      setContextMenu(null);
    };
    window.addEventListener("keydown", handleEscape);
    window.addEventListener("pointerdown", handlePointerDownOutside);
    return () => {
      window.removeEventListener("keydown", handleEscape);
      window.removeEventListener("pointerdown", handlePointerDownOutside);
    };
  }, [contextMenu]);

  useEffect(() => {
    if (!effectivePlayerPosition || initialCameraRef.current) return;
    initialCameraRef.current = true;
    setStagePan(getStagePanForPosition(effectivePlayerPosition, stageWorldSize, stageZoom));
  }, [effectivePlayerPosition, setStagePan, stageWorldSize, stageZoom]);

  const nodeForActor = useCallback(
    (actor: WorkspaceStageActor): WorkspaceSceneNode =>
      nodesById.get(actor.id) ?? {
        id: actor.id,
        kind: actor.kind,
        label: actor.label,
        detail: actor.detail,
        status: actor.sourceStatus,
        x: parseStagePct(actor.position.left),
        y: parseStagePct(actor.position.top),
      },
    [nodesById],
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
      const rawX = bounds ? payload.x - bounds.left : payload.x;
      const rawY = bounds ? payload.y - bounds.top : payload.y;
      const clamped = clampContextMenuPosition(rawX, rawY, {
        width: bounds?.width ?? (typeof window !== "undefined" ? window.innerWidth : 1280),
        height: bounds?.height ?? (typeof window !== "undefined" ? window.innerHeight : 800),
      });
      setSelectedNodeId(node.id);
      setContextMenu(
        createWorkspaceContextMenuState({
          nodeId: node.id,
          kind: node.kind,
          screenX: clamped.x,
          screenY: clamped.y,
        }),
      );
      onActorSelect?.(node);
    },
    [nodeForActor, onActorSelect],
  );

  const handleContextMenuAction = useCallback(
    (action: WorkspaceContextMenuAction) => {
      if (!contextMenu) return;
      const node = nodesById.get(contextMenu.nodeId);
      if (!node) {
        setContextMenu(null);
        return;
      }

      setSelectedNodeId(node.id);
      setContextMenu(null);
      if (action === "focus-node") {
        const actor = actorsById.get(node.id);
        if (actor) {
          setStagePan(getStagePanForPosition(actor.position, stageWorldSize, stageZoom));
        }
        return;
      }

      onActorSelect?.(node);
    },
    [actorsById, contextMenu, nodesById, onActorSelect, setStagePan, stageWorldSize, stageZoom],
  );

  const updateStageZoom = useCallback(
    (nextZoom: number) => {
      const zoom = clampStageZoom(nextZoom);
      setStageZoom(zoom);
      if (effectivePlayerPosition) {
        setStagePan(getStagePanForPosition(effectivePlayerPosition, stageWorldSize, zoom));
      }
    },
    [effectivePlayerPosition, setStagePan, stageWorldSize],
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
      const rLeft = parseStagePct(room.x);
      const rTop = parseStagePct(room.y);
      const rW = parseStagePct(room.w);
      const rH = parseStagePct(room.h);
      const firstActorInRoom = logicalStageActors.find((actor) => {
        const ax = parseStagePct(actor.position.left);
        const ay = parseStagePct(actor.position.top);
        return ax >= rLeft && ax <= rLeft + rW && ay >= rTop && ay <= rTop + rH;
      });
      const roomScaledW = (rW / 100) * stageWorldSize.width * stageZoom;
      const center =
        firstActorInRoom && roomScaledW > stageViewportSize.width * 0.78
          ? firstActorInRoom.position
          : {
              left: pct(rLeft + rW / 2),
              top: pct(rTop + rH / 2),
            };
      setStagePan(getStagePanForPosition(center, stageWorldSize, stageZoom));
    },
    [logicalStageActors, setStagePan, stageViewportSize.width, stageWorldSize, stageZoom],
  );

  return (
    <div
      className={`workspace-2d-frame ${isFullscreenStage ? "workspace-2d-frame-fullscreen" : ""}`}
      ref={frameRef}
    >
      <div className="workspace-2d-header">
        <div>
          <span className="workspace-detail-label">2D 오피스 스테이지</span>
          <h2>오피스 스테이지</h2>
        </div>
        <div className="workspace-2d-controls">
          <span>{Math.round(stageZoom * 100)}%</span>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => updateStageZoom(stageZoom - STAGE_ZOOM_STEP)}
            aria-label="축소"
            title="축소"
          >
            -
          </button>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => updateStageZoom(stageZoom + STAGE_ZOOM_STEP)}
            aria-label="확대"
            title="확대"
          >
            +
          </button>
          <button
            className="secondary-action workspace-task-refresh"
            type="button"
            onClick={() => {
              setStageZoom(initialStageZoom);
              const resetPan = effectivePlayerPosition
                ? getStagePanForPosition(effectivePlayerPosition, stageWorldSize, initialStageZoom)
                : { x: 0, y: 0 };
              setStagePan(resetPan);
            }}
            aria-label="화면 초기화"
            title="화면 초기화"
          >
            <Maximize2 aria-hidden="true" />
            <span>초기화</span>
          </button>
        </div>
      </div>

      <div className="workspace-2d-hero-hud" aria-label="선택된 참여자 정보">
        <div className="workspace-2d-hero-avatar">
          {selectedStageActor ? (
            <PixelAvatar
              kind={selectedStageActor.avatarKind}
              variant="mugshot"
              variantKey={selectedStageActor.label}
              agentCategory={selectedStageActor.agentCategory}
              size={2.5}
              activity={motionPaused ? "idle" : selectedStageActor.motion === "working" ? "typing" : "idle"}
              paletteOverride={selectedStageActor.id === playerActorId ? playerAvatar?.paletteOverride : undefined}
            />
          ) : (
            <Users aria-hidden="true" />
          )}
        </div>
        <div>
          <span>{selectedStageActor ? (selectedStageActor.kind === "agent" ? "에이전트" : "멤버") : "대상"}</span>
          <strong>{selectedNode?.label ?? selectedStageActor?.label ?? "없음"}</strong>
          <small>{selectedNode?.detail ?? selectedStageActor?.detail ?? "선택된 대상이 없습니다."}</small>
        </div>
      </div>

      <div className="workspace-2d-hud" aria-label="오피스 상태 지표">
        <div>
          <span>준비 에이전트</span>
          <strong>{activeAgents}</strong>
        </div>
        <div>
          <span>온라인 멤버</span>
          <strong>{activeMembers}</strong>
        </div>
        <div>
          <span>선택 대상</span>
          <strong>{selectedNode?.label ?? "없음"}</strong>
        </div>
        <div>
          <span>점유 타일</span>
          <strong>
            {occupiedTileCount} · {onlineAgentCount}
          </strong>
        </div>
        <div>
          <span>내 위치</span>
          <strong>
            {effectivePlayerPosition
              ? `${effectivePlayerPosition.left}, ${effectivePlayerPosition.top}`
              : "대기"}
          </strong>
        </div>
      </div>

      <div
        ref={toolbarRef}
        role="toolbar"
        aria-label="오피스 스테이지 표시 도구"
        className="ao-stage-toolbar"
        {...({ focusgroup: "toolbar wrap" } as Record<string, string>)}
        onKeyDown={handleToolbarKeyDown}
      >
        <div role="group" aria-label="오피스 테마" className="ao-toolbar-group">
          {OFFICE_THEME_OPTIONS.map((option, index) => (
            <button
              key={option.id}
              type="button"
              className="ao-toolbar-btn"
              aria-pressed={officeTheme === option.id}
              tabIndex={supportsFocusgroup ? 0 : toolbarFocusIndex === index ? 0 : -1}
              onClick={() => {
                setToolbarFocusIndex(index);
                handleSelectOfficeTheme(option.id);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
        <span role="separator" aria-orientation="vertical" className="ao-toolbar-sep" />
        <div role="group" aria-label="시각 효과" className="ao-toolbar-group">
          <button
            type="button"
            className="ao-toolbar-btn"
            aria-pressed={showCollabLinks}
            tabIndex={supportsFocusgroup ? 0 : toolbarFocusIndex === 6 ? 0 : -1}
            onClick={() => {
              setToolbarFocusIndex(6);
              setShowCollabLinks((prev) => !prev);
            }}
          >
            협업 연결선
          </button>
          <button
            type="button"
            className="ao-toolbar-btn"
            tabIndex={supportsFocusgroup ? 0 : toolbarFocusIndex === 7 ? 0 : -1}
            onClick={() => {
              setToolbarFocusIndex(7);
              setMotionPaused((prev) => !prev);
            }}
          >
            {motionPaused ? "모션 재생" : "모션 일시정지"}
          </button>
        </div>
      </div>

      <div className="workspace-2d-layout">
        <div className="workspace-2d-stage-shell" ref={stageShellRef} onWheel={handleStageShellWheel}>
          <ArcadeOfficeStage
            actors={logicalStageActors}
            rooms={stageRooms}
            roomsByVariant={roomsByVariant}
            worldSize={stageWorldSize}
            engineeringRows={engineeringRows}
            selectedActorId={selectedNodeId}
            playerActorId={playerActorId}
            playerAvatar={playerAvatar}
            playerPosition={effectivePlayerPosition}
            zoom={stageZoom}
            pan={stagePan}
            officeTheme={officeTheme}
            showCollabLinks={showCollabLinks}
            motionPaused={motionPaused}
            onPanChange={setStagePan}
            onZoomChange={updateStageZoom}
            onLocalPlayerPositionChange={handleLocalPlayerPositionChange}
            onPlayerPositionChange={onPlayerPositionChange}
            onPresenceMove={onPresenceMove}
            onOpenTasks={onOpenTasks}
            onOpenOrchestration={onOpenOrchestration}
            onOpenArtifacts={onOpenArtifacts}
            onActorContextMenu={openActorContextMenu}
            onActorSelect={selectStageActor}
          />
          <MapHud
            rooms={miniMapRooms}
            actors={logicalStageActors}
            playerActorId={playerActorId}
            myPosition={effectivePlayerPosition}
            totalActors={logicalStageActors.length}
            zoom={stageZoom}
            pan={stagePan}
            stageSize={stageWorldSize}
            viewportSize={stageViewportSize}
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

        <div className="workspace-2d-roster" aria-label="오피스 참여자 목록">
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
                  const bounds = frameRef.current?.getBoundingClientRect();
                  const rawX = bounds ? event.clientX - bounds.left : event.clientX;
                  const rawY = bounds ? event.clientY - bounds.top : event.clientY;
                  const clamped = clampContextMenuPosition(rawX, rawY, {
                    width: bounds?.width ?? (typeof window !== "undefined" ? window.innerWidth : 1280),
                    height: bounds?.height ?? (typeof window !== "undefined" ? window.innerHeight : 800),
                  });
                  setSelectedNodeId(node.id);
                  setContextMenu(
                    createWorkspaceContextMenuState({
                      nodeId: node.id,
                      kind: node.kind,
                      screenX: clamped.x,
                      screenY: clamped.y,
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
              <strong>표시할 참여자가 없습니다</strong>
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
  roomsByVariant,
  worldSize,
  engineeringRows,
  selectedActorId,
  playerActorId,
  playerAvatar,
  playerPosition,
  zoom,
  pan,
  officeTheme,
  showCollabLinks,
  motionPaused,
  onPanChange,
  onZoomChange,
  onLocalPlayerPositionChange,
  onPlayerPositionChange,
  onPresenceMove,
  onOpenTasks,
  onOpenOrchestration,
  onOpenArtifacts,
  onActorContextMenu,
  onActorSelect,
}: {
  actors: WorkspaceStageActor[];
  rooms: StageRoomSpec[];
  roomsByVariant: ReadonlyMap<string, StageRoomSpec>;
  worldSize: StageSize;
  engineeringRows: number;
  selectedActorId: string | null;
  playerActorId: string | null;
  playerAvatar: { kind: PixelAvatarKind; paletteOverride?: PixelAvatarPaletteOverride } | null;
  playerPosition: StageActorPosition | null;
  zoom: number;
  pan: StagePan;
  officeTheme: OfficeThemeId;
  showCollabLinks: boolean;
  motionPaused: boolean;
  onPanChange: (pan: StagePan) => void;
  onZoomChange: (zoom: number) => void;
  onLocalPlayerPositionChange: (position: StageActorPosition) => void;
  onPlayerPositionChange?: (position: StageActorPosition) => void | Promise<void>;
  onPresenceMove?: (position: WorkspacePresencePosition) => void | Promise<void>;
  onOpenTasks?: () => void;
  onOpenOrchestration?: () => void;
  onOpenArtifacts?: () => void;
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
  const onLocalPlayerPositionChangeRef = useRef(onLocalPlayerPositionChange);
  const onPlayerPositionChangeRef = useRef(onPlayerPositionChange);
  const lastParentNotifyAtRef = useRef(0);
  const pendingParentPositionRef = useRef<StageActorPosition | null>(null);
  const parentNotifyTimerRef = useRef<number | null>(null);
  const onActorSelectRef = useRef(onActorSelect);
  const onOpenTasksRef = useRef(onOpenTasks);
  const actorPositionRef = useRef<Map<string, StageActorPosition>>(new Map());
  const interactableActorsRef = useRef<Array<{ actor: WorkspaceStageActor; position: StageActorPosition }>>([]);
  const wallsRef = useRef<WallSegment[]>([]);
  const wallBitsetRef = useRef<SpatialBitset>(createSpatialBitset(40, 24));
  const pathfindingCtxRef = useRef<OfficePathfindingContext | null>(null);
  const activeWaypointsRef = useRef<PathPoint[]>([]);
  const playerWalkingRef = useRef(false);
  const startTickLoopRef = useRef<(() => void) | null>(null);
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
    onLocalPlayerPositionChangeRef.current = onLocalPlayerPositionChange;
    onPlayerPositionChangeRef.current = onPlayerPositionChange;
    onActorSelectRef.current = onActorSelect;
    onOpenTasksRef.current = onOpenTasks;
  }, [onActorSelect, onLocalPlayerPositionChange, onOpenTasks, onPanChange, onPlayerPositionChange]);

  useEffect(() => {
    playerPositionRef.current = playerPosition;
    fixedStageSizeRef.current = worldSize;
    zoomRef.current = zoom;
    panRef.current = pan;
  }, [pan, playerPosition, worldSize, zoom]);

  useEffect(() => {
    const walls = buildStageWalls(rooms);
    wallsRef.current = walls;
    resetSpatialBitset(wallBitsetRef.current);
    buildStageWallBitset(walls, 40, 24, wallBitsetRef.current);
    if (!pathfindingCtxRef.current) {
      pathfindingCtxRef.current = createOfficePathfindingContext();
    }
    rasterizeWallRectsToBitgrid(
      pathfindingCtxRef.current.blockedBits,
      buildStageObstacleRects(rooms, engineeringRows),
      true,
    );
  }, [engineeringRows, rooms]);

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
    const engineeringRoom = roomsByVariant.get("engineering") ?? rooms[0];
    const loungeRoom = roomsByVariant.get("rest") ?? null;
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
    roomsByVariant,
    travelingActorIds,
  ]);

  const interactableSpatialIndex = useMemo(() => {
    const actorsList: WorkspaceStageActor[] = [];
    const points: SpatialPointInput[] = [];
    for (let i = 0; i < renderedActors.length; i += 1) {
      const item = renderedActors[i];
      if (item.isPlayer) continue;
      const idx = actorsList.length;
      actorsList.push(item.actor);
      points.push({
        id: idx,
        x: parseStagePct(item.position.left),
        y: parseStagePct(item.position.top),
      });
    }
    return {
      actorsList,
      tree: points.length > 0 ? buildOfficeSpatialKdTree(points) : null,
    };
  }, [renderedActors]);

  const nearestInteractableActorId = useMemo(() => {
    if (!playerPosition || playerNearQuest || !interactableSpatialIndex.tree) {
      return null;
    }
    const nearest = interactableSpatialIndex.tree.findNearestWithin(
      parseStagePct(playerPosition.left),
      parseStagePct(playerPosition.top),
      7,
    );
    if (!nearest) {
      return null;
    }
    return interactableSpatialIndex.actorsList[nearest.id]?.id ?? null;
  }, [interactableSpatialIndex, playerNearQuest, playerPosition]);

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

      const path = computeAgentPath(
        previousPosition,
        logicalPosition,
        rooms,
        roomsByVariant,
        pathfindingCtxRef.current ?? undefined,
      );
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
  }, [agentVisualPositions, renderedActors, rooms, roomsByVariant]);

  useEffect(() => {
    if (!playerActorId) return;
    let rafId = 0;
    let lastFrame = 0;

    function flushPendingParentPosition() {
      if (parentNotifyTimerRef.current != null) {
        window.clearTimeout(parentNotifyTimerRef.current);
        parentNotifyTimerRef.current = null;
      }
      const pending = pendingParentPositionRef.current;
      if (pending) {
        pendingParentPositionRef.current = null;
        lastParentNotifyAtRef.current = performance.now();
        void onPlayerPositionChangeRef.current?.(pending);
      }
    }

    function notifyParentPlayerPositionThrottled(nextPosition: StageActorPosition) {
      const now = performance.now();
      const elapsed = now - lastParentNotifyAtRef.current;
      if (elapsed >= 100) {
        if (parentNotifyTimerRef.current != null) {
          window.clearTimeout(parentNotifyTimerRef.current);
          parentNotifyTimerRef.current = null;
        }
        pendingParentPositionRef.current = null;
        lastParentNotifyAtRef.current = now;
        void onPlayerPositionChangeRef.current?.(nextPosition);
        return;
      }
      pendingParentPositionRef.current = nextPosition;
      if (parentNotifyTimerRef.current == null) {
        parentNotifyTimerRef.current = window.setTimeout(
          flushPendingParentPosition,
          Math.max(16, 100 - elapsed),
        );
      }
    }

    function setWalkingIfChanged(nextWalking: boolean) {
      if (playerWalkingRef.current === nextWalking) {
        return;
      }
      playerWalkingRef.current = nextWalking;
      setPlayerWalking(nextWalking);
    }

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
      const nextPosition = resolveStageMove(
        currentPosition,
        deltaXPct,
        deltaYPct,
        wallsRef.current,
        wallBitsetRef.current,
      );
      const nextDirection: PixelAvatarDirection =
        Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "E" : "W") : dy < 0 ? "N" : "S";
      playerPositionRef.current = nextPosition;
      onLocalPlayerPositionChangeRef.current(nextPosition);
      setPlayerDirection(nextDirection);
      setWalkingIfChanged(true);
      const targetPan = getStagePanForPosition(nextPosition, stageSize, zoomRef.current);
      const nextPan = smoothStagePan(panRef.current, targetPan, CAMERA_FOLLOW_SMOOTHING);
      panRef.current = nextPan;
      onPanChangeRef.current(nextPan);
      notifyParentPlayerPositionThrottled(nextPosition);
      return true;
    }

    function stepAlongWaypoints(deltaSeconds: number): boolean {
      const queue = activeWaypointsRef.current;
      const currentPosition = playerPositionRef.current;
      const stageSize = fixedStageSizeRef.current;
      if (queue.length === 0 || !currentPosition || !stageSize || deltaSeconds <= 0) {
        return false;
      }

      const target = queue[0];
      const curX = parseStagePct(currentPosition.left);
      const curY = parseStagePct(currentPosition.top);
      const dx = target.x - curX;
      const dy = target.y - curY;
      const distPct = Math.hypot(dx, dy);

      if (distPct < 0.35) {
        queue.shift();
        if (queue.length === 0) {
          flushPendingParentPosition();
          return false;
        }
        return true;
      }

      const zoomScale = zoomRef.current;
      const widthPx = Math.max(1, stageSize.width * zoomScale);
      const heightPx = Math.max(1, stageSize.height * zoomScale);
      const distPx = Math.hypot((dx * widthPx) / 100, (dy * heightPx) / 100);
      const scale = distPx > 0 ? Math.min(1, (PLAYER_MOVE_SPEED_PX * deltaSeconds) / distPx) : 1;
      const nextPosition = resolveStageMove(
        currentPosition,
        dx * scale,
        dy * scale,
        wallsRef.current,
        wallBitsetRef.current,
      );

      if (sameStagePosition(nextPosition, currentPosition)) {
        queue.shift();
        if (queue.length === 0) {
          flushPendingParentPosition();
          return false;
        }
        return true;
      }

      const nextDirection: PixelAvatarDirection =
        Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "E" : "W") : dy < 0 ? "N" : "S";
      playerPositionRef.current = nextPosition;
      onLocalPlayerPositionChangeRef.current(nextPosition);
      setPlayerDirection(nextDirection);
      setWalkingIfChanged(true);
      const targetPan = getStagePanForPosition(nextPosition, stageSize, zoomRef.current);
      const nextPan = smoothStagePan(panRef.current, targetPan, CAMERA_FOLLOW_SMOOTHING);
      panRef.current = nextPan;
      onPanChangeRef.current(nextPan);
      notifyParentPlayerPositionThrottled(nextPosition);
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
        activeWaypointsRef.current = [];
        movePlayerByKeys(keysRef.current, 1 / 30);
        ensureTickLoop();
      }
    }

    function handleKeyUp(event: KeyboardEvent) {
      const key = event.key.toLowerCase();
      keysRef.current[key] = false;
      if (isPlayerMoveKey(key)) {
        flushPendingParentPosition();
      }
    }

    function tick(time: number) {
      const currentPosition = playerPositionRef.current;
      const stageSize = fixedStageSizeRef.current;
      let active = false;
      if (currentPosition && stageSize) {
        const deltaSeconds = lastFrame ? Math.min(0.05, (time - lastFrame) / 1000) : 1 / 60;
        if (movePlayerByKeys(keysRef.current, deltaSeconds)) {
          active = true;
        } else if (stepAlongWaypoints(deltaSeconds)) {
          active = true;
        }
      }
      if (active) {
        lastFrame = time;
        rafId = window.requestAnimationFrame(tick);
      } else {
        setWalkingIfChanged(false);
        lastFrame = 0;
        rafId = 0;
      }
    }

    function ensureTickLoop() {
      if (rafId !== 0) {
        return;
      }
      lastFrame = 0;
      rafId = window.requestAnimationFrame(tick);
    }

    startTickLoopRef.current = ensureTickLoop;
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    return () => {
      startTickLoopRef.current = null;
      activeWaypointsRef.current = [];
      keysRef.current = {};
      if (parentNotifyTimerRef.current != null) {
        window.clearTimeout(parentNotifyTimerRef.current);
        parentNotifyTimerRef.current = null;
      }
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      if (rafId !== 0) {
        window.cancelAnimationFrame(rafId);
      }
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
    const bounds = event.currentTarget.getBoundingClientRect();
    const localX = (event.clientX - bounds.left - bounds.width / 2 - pan.x) / zoom + worldSize.width / 2;
    const localY = (event.clientY - bounds.top - bounds.height / 2 - pan.y) / zoom + worldSize.height / 2;
    const targetXPct = clampNumber((localX / worldSize.width) * 100, 3, 96);
    const targetYPct = clampNumber((localY / worldSize.height) * 100, 8, 91);

    const startPosition = playerPositionRef.current;
    const pathCtx = pathfindingCtxRef.current;
    if (startPosition && pathCtx && startTickLoopRef.current) {
      const path = findOfficePath(
        pathCtx,
        { x: parseStagePct(startPosition.left), y: parseStagePct(startPosition.top) },
        { x: targetXPct, y: targetYPct },
        { snapRadiusCells: 4 },
      );
      if (path.length >= 2) {
        activeWaypointsRef.current = path.slice(1);
        startTickLoopRef.current();
        return;
      }
    }

    if (onPresenceMove) {
      void onPresenceMove({
        left: pct(targetXPct),
        top: pct(targetYPct),
      });
    }
  }

  function handleWheel(event: ReactWheelEvent<HTMLDivElement>) {
    event.preventDefault();
    event.stopPropagation();
    onZoomChange(zoom + (event.deltaY < 0 ? STAGE_ZOOM_STEP : -STAGE_ZOOM_STEP));
  }

  return (
    <div
      className="workspace-2d-stage is-tilemap-backed"
      ref={stageRef}
      role="img"
      aria-label="오피스 2D 스테이지"
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
          <OfficeTilemapCanvas
            rooms={rooms}
            engineeringRows={engineeringRows}
            actors={actors}
            worldSize={worldSize}
            themeId={officeTheme}
            showCollabLinks={showCollabLinks}
            motionPaused={motionPaused}
            playerActorId={playerActorId}
          />
          <StageCorridors roomsByVariant={roomsByVariant} />
          {rooms.map((room) => (
            <StageRoom
              key={room.label}
              room={room}
              engineeringRows={engineeringRows}
              onOpenOrchestration={onOpenOrchestration}
              onOpenArtifacts={onOpenArtifacts}
            />
          ))}
          <QuestMarker
            active={playerNearQuest}
            position={QUEST_MARKER_POSITION}
            onClick={onOpenTasks}
          />

          <div className="workspace-2d-actor-layer" aria-label="오피스 참여자 레이어">
            {renderedActors.map(({ actor, isPlayer, position, transitionMs, traveling }) => (
              <StageActor
                actor={actor}
                position={position}
                isPlayer={isPlayer}
                playerAvatar={playerAvatar}
                travel={actor.kind === "agent"}
                travelMs={transitionMs}
                walking={!motionPaused && ((isPlayer && playerWalking) || traveling)}
                motionPaused={motionPaused}
                direction={isPlayer ? playerDirection : "S"}
                selected={actor.id === selectedActorId}
                interactTarget={actor.id === nearestInteractableActorId}
                key={actor.id}
                onSelect={onActorSelect}
                onContextMenu={onActorContextMenu}
              />
            ))}
          </div>

          {actors.length === 0 ? (
            <div className="workspace-2d-empty-floor">
              <p>배치된 에이전트와 멤버가 없습니다.</p>
              <span>상단 툴바에서 에이전트를 추가하거나 설정에서 팀원을 초대하세요.</span>
            </div>
          ) : null}
        </div>
      </div>
      <div className="workspace-2d-status">
        <span>
          x {Math.round(pan.x)} · y {Math.round(pan.y)}
        </span>
        <span>참여자 {actors.length}명</span>
      </div>
    </div>
  );
}

function StageRoom({
  room,
  engineeringRows,
  onOpenOrchestration,
  onOpenArtifacts,
}: {
  room: StageRoomSpec;
  engineeringRows: number;
  onOpenOrchestration?: () => void;
  onOpenArtifacts?: () => void;
}) {
  const roomDisplay = ROOM_DISPLAY_LABELS[room.variant];

  return (
    <section
      className={`stage-room stage-room-${room.variant}`}
      style={{
        left: room.left,
        top: room.top,
        width: room.width,
        height: room.height,
        background: "var(--stage-room-bg)",
      }}
    >
      <div className="stage-room-label">
        <span>
          {roomDisplay?.label ?? room.label}
          {roomDisplay?.sub ? ` · ${roomDisplay.sub}` : ""}
        </span>
      </div>
      {room.variant === "engineering" ? <EngineeringPlatforms rows={engineeringRows} /> : null}
      {room.variant === "huddle" ? <HuddleTable onOpenOrchestration={onOpenOrchestration} /> : null}
      {room.variant === "rest" ? <RestLounge /> : null}
      {room.variant === "library" ? <LibraryRoom onOpenArtifacts={onOpenArtifacts} /> : null}
    </section>
  );
}

function StageCorridors({ roomsByVariant }: { roomsByVariant: ReadonlyMap<string, StageRoomSpec> }) {
  const engineering = roomsByVariant.get("engineering");
  const huddle = roomsByVariant.get("huddle");
  const lounge = roomsByVariant.get("rest");
  const library = roomsByVariant.get("library");
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
      <StagePlant left="92%" top="8%" color="var(--status-success)" />
      <StagePlant left="92%" top="82%" color="var(--status-success)" />
    </>
  );
}

function StageDesk({ left, top, width, height }: { left: string; top: string; width: string; height: string }) {
  return <div className="stage-desk" style={{ left, top, width, height }} aria-hidden="true" />;
}

function StageChair({ left, top }: { left: string; top: string; color?: string }) {
  return <div className="stage-chair" style={{ left, top }} aria-hidden="true" />;
}

function StagePlant({ left, top }: { left: string; top: string; color?: string }) {
  return <div className="stage-plant" style={{ left, top }} aria-hidden="true" />;
}

function CoffeeStation({ left, top }: { left: string; top: string }) {
  return <div className="stage-coffee" style={{ left, top }} aria-hidden="true" />;
}

function HuddleTable({ onOpenOrchestration }: { onOpenOrchestration?: () => void }) {
  return (
    <button
      type="button"
      className="stage-huddle-table stage-furniture-action"
      style={{ justifyContent: "center" }}
      onClick={onOpenOrchestration}
      aria-label="회의실 테이블 · 오케스트레이션 열기"
      title="회의실 테이블 · 오케스트레이션 열기"
    >
      <GitBranch size={13} aria-hidden="true" />
      <span>오케스트레이션</span>
    </button>
  );
}

function RestLounge() {
  return (
    <>
      <CoffeeStation left="8%" top="72%" />
      <StagePlant left="84%" top="10%" color="var(--status-success)" />
      <StageChair left="25%" top="35%" color="var(--accent-primary)" />
    </>
  );
}

function LibraryRoom({ onOpenArtifacts }: { onOpenArtifacts?: () => void }) {
  return (
    <button
      type="button"
      className="stage-library-shelf stage-library-shelf-a stage-furniture-action"
      style={{ justifyContent: "center" }}
      onClick={onOpenArtifacts}
      aria-label="서재 책장 · 산출물 및 변경사항 열기"
      title="서재 책장 · 산출물 및 변경사항 열기"
    >
      <FolderTree size={13} aria-hidden="true" />
      <span>산출물</span>
    </button>
  );
}

function QuestMarker({
  active,
  position,
  onClick,
}: {
  active: boolean;
  position: StageActorPosition;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`stage-quest-marker ${active ? "stage-quest-marker-active" : ""}`}
      style={{ left: position.left, top: position.top }}
      aria-label="작업 보드 열기"
      title="작업 보드 열기 (/)"
    >
      <span>작업 보드</span>
      {active ? <strong>/ 열기</strong> : null}
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
  motionPaused = false,
  direction = "S",
  selected,
  interactTarget = false,
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
  motionPaused?: boolean;
  direction?: PixelAvatarDirection;
  selected: boolean;
  interactTarget?: boolean;
  onContextMenu: (payload: { actor: WorkspaceStageActor; x: number; y: number }) => void;
  onSelect: (actor: WorkspaceStageActor) => void;
}) {
  const accent = actor.kind === "agent" ? "var(--accent-primary)" : "var(--accent-strong)";
  const speech = getWorkspaceActorSpeech(actor);
  const visibleSpeech = isPlayer ? null : speech;
  const motion = walking ? "walking" : "idle";
  const avatarKind = isPlayer && playerAvatar ? playerAvatar.kind : actor.avatarKind;
  const statusLabel = actorStatusKoreanLabel(actor.status);

  return (
    <button
      type="button"
      data-interact-target={interactTarget ? "true" : undefined}
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
      title={isPlayer ? `${actor.label} (나)` : `${actor.label} · ${statusLabel}`}
    >
      {isPlayer ? <span aria-hidden="true" className="stage-actor__proximity-ring" /> : null}
      {isPlayer ? <span aria-hidden="true" className="stage-actor__player-marker" /> : null}
      {visibleSpeech ? (
        <div
          className={`stage-actor__speech stage-actor__speech-${visibleSpeech.tone}`}
          title={visibleSpeech.text || actor.detail}
        >
          <span>{statusLabel}</span>
          {visibleSpeech.text ? <span>{visibleSpeech.text}</span> : null}
        </div>
      ) : null}
      <div className="stage-actor__badge" style={{ borderColor: selected ? accent : "var(--border-default)" }}>
        {!isPlayer ? <span className={`stage-actor__dot stage-actor__dot-${actor.status}`} title={statusLabel} /> : null}
        <span>{isPlayer ? "나" : actor.label}</span>
        {interactTarget ? (
          <span className="stage-actor__interact-hint" aria-hidden="true">
            /
          </span>
        ) : null}
      </div>
      <div className="stage-actor__avatar">
        <span className="stage-actor__shadow" aria-hidden="true" />
        <PixelAvatar
          kind={avatarKind}
          variant="full"
          variantKey={actor.label}
          agentCategory={actor.agentCategory}
          paletteOverride={isPlayer ? playerAvatar?.paletteOverride : undefined}
          direction={direction}
          size={3}
          walking={Boolean(walking)}
          activity={
            motionPaused
              ? "idle"
              : walking
                ? "walk"
                : actor.kind === "agent" && (actor.status === "working" || actor.status === "idle")
                  ? "typing"
                  : actor.status === "review"
                    ? "reading"
                    : "idle"
          }
        />
      </div>
    </button>
  );
}

function MapHud({
  rooms,
  actors,
  playerActorId,
  myPosition,
  totalActors,
  zoom,
  pan,
  stageSize,
  viewportSize,
  onRoomClick,
}: {
  rooms: StageMiniMapRoom[];
  actors: WorkspaceStageActor[];
  playerActorId: string | null;
  myPosition: StageActorPosition | null;
  totalActors: number;
  zoom: number;
  pan: StagePan;
  stageSize: StageSize;
  viewportSize: StageSize;
  onRoomClick: (room: StageMiniMapRoom) => void;
}) {
  const viewportWidth = clampNumber(
    (viewportSize.width / (Math.max(1, stageSize.width) * zoom)) * 100,
    12,
    100,
  );
  const viewportHeight = clampNumber(
    (viewportSize.height / (Math.max(1, stageSize.height) * zoom)) * 100,
    12,
    100,
  );
  const viewportLeft = clampNumber(
    50 - viewportWidth / 2 - (pan.x / (Math.max(1, stageSize.width) * zoom)) * 100,
    0,
    100 - viewportWidth,
  );
  const viewportTop = clampNumber(
    50 - viewportHeight / 2 - (pan.y / (Math.max(1, stageSize.height) * zoom)) * 100,
    0,
    100 - viewportHeight,
  );

  return (
    <section
      className="workspace-2d-map-hud"
      aria-label="미니맵 · 1층"
      style={{
        background: "var(--ui-surface)",
        border: "1px solid var(--ui-border)",
        borderRadius: 6,
        padding: 8,
      }}
    >
      <div
        style={{
          fontSize: 12,
          fontWeight: 600,
          color: "var(--ui-text-secondary)",
          paddingBottom: 6,
          marginBottom: 6,
          borderBottom: "1px solid var(--ui-border-subtle)",
        }}
      >
        미니맵 · 1층
      </div>
      <div className="workspace-2d-minimap" aria-label="오피스 구역 미니맵">
        {rooms.map((room) => (
          <button
            aria-label={`${ROOM_DISPLAY_LABELS[room.variant]?.label ?? room.label} 구역으로 이동`}
            title={ROOM_DISPLAY_LABELS[room.variant]?.label ?? room.label}
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
        {actors.map((actor) =>
          actor.id === playerActorId ? null : (
            <span
              key={actor.id}
              aria-hidden="true"
              className={`workspace-2d-minimap-actor ${
                actor.kind === "member"
                  ? "workspace-2d-minimap-actor-member"
                  : "workspace-2d-minimap-actor-agent"
              } workspace-2d-minimap-status-${actor.status}`}
              style={{ left: actor.position.left, top: actor.position.top }}
            />
          ),
        )}
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
        <span>참여자 {totalActors}명</span>
        <span>내 위치</span>
      </div>
    </section>
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
      <ZoomButton onClick={onZoomOut} disabled={zoom <= 0.75} label="축소">
        -
      </ZoomButton>
      <button type="button" onClick={onReset} aria-label="배율 초기화" title="배율 초기화">
        {Math.round(zoom * 100)}%
      </button>
      <ZoomButton onClick={onZoomIn} disabled={zoom >= 1.5} label="확대">
        +
      </ZoomButton>
    </div>
  );
}

function ZoomButton({
  children,
  disabled,
  onClick,
  label,
}: {
  children: ReactNode;
  disabled?: boolean;
  onClick: () => void;
  label: string;
}) {
  return (
    <button type="button" onClick={onClick} disabled={disabled} aria-label={label} title={label}>
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
  const speaker = actor?.label ?? "안내";
  const accent = actor ? (isAgent ? "var(--accent-primary)" : "var(--accent-strong)") : "var(--ui-text-secondary)";
  const line = actor
    ? isAgent
      ? `${actor.detail} · 대화를 열거나 작업을 할당할 수 있습니다.`
      : `${speaker} · ${actor.sourceStatus}`
    : "배치된 참여자가 없습니다. 에이전트를 추가하거나 팀원을 초대해 주세요.";

  return (
    <div className="workspace-2d-dialogue" aria-live="polite">
      <div className="workspace-2d-dialogue-avatar" style={{ borderColor: "var(--border-default)" }}>
        <PixelAvatar
          kind={actor?.avatarKind ?? "agent"}
          variant="mugshot"
          variantKey={actor?.label}
          agentCategory={actor?.agentCategory}
          size={2.5}
        />
      </div>
      <div>
        <strong style={{ color: accent }} className="truncate">
          {speaker}
        </strong>
        <p title={line}>{line}</p>
        <div className="workspace-2d-dialogue-actions">
          {actor ? (
            <DialogueChoice primary disabled={isSelf} onClick={() => onTalk(actor)}>
              대화하기
            </DialogueChoice>
          ) : null}
          <DialogueChoice onClick={onTasks}>작업 보드</DialogueChoice>
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
      return "위치 이동";
    case "open-profile":
      return "상세 정보";
    case "start-chat":
      return "대화하기";
  }
}

function shouldIgnoreStageMovementKey(target: EventTarget | null): boolean {
  if (typeof document !== "undefined" && document.querySelector('[role="dialog"]')) {
    return true;
  }
  if (!(target instanceof HTMLElement)) return false;
  if (target.closest('[role="dialog"]')) return true;
  const tagName = target.tagName.toLowerCase();
  return tagName === "input" || tagName === "textarea" || tagName === "select" || target.isContentEditable;
}
