"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  use,
  type FormEvent,
} from "react";
import Toast from "@/components/ui/Toast";
import { WorkspaceChromeHeader } from "@/components/workspace/chrome/WorkspaceChromeHeader";
import {
  WorkspaceLeftSidebar,
  type OfficeZoneId,
} from "@/components/workspace/chrome/WorkspaceLeftSidebar";
import { WorkspaceStatusBar } from "@/components/workspace/chrome/WorkspaceStatusBar";
import {
  WorkspaceContextInspector,
  type InspectorTarget,
} from "@/components/workspace/inspector/WorkspaceContextInspector";
import { AgentHireModal } from "@/components/workspace/modals/AgentHireModal";
import { ArtifactFileModal } from "@/components/workspace/modals/ArtifactFileModal";
import { ConnectionSettingsModal } from "@/components/workspace/modals/ConnectionSettingsModal";
import { ShortcutCheatsheetModal } from "@/components/workspace/modals/ShortcutCheatsheetModal";
import { TaskCreateModal } from "@/components/workspace/modals/TaskCreateModal";
import {
  OrchestrationPlanModal,
  TaskChatModal,
  TaskReportsModal,
} from "@/components/workspace/modals/TaskDetailModals";
import { WorkspaceArtifactsAndDiffView } from "@/components/workspace/views/WorkspaceArtifactsAndDiffView";
import { WorkspaceApprovalInboxView } from "@/components/workspace/views/WorkspaceApprovalInboxView";
import { WorkspaceOrchestrationView } from "@/components/workspace/views/WorkspaceOrchestrationView";
import { WorkspaceTaskBoardView } from "@/components/workspace/views/WorkspaceTaskBoardView";
import Workspace2DCanvas from "@/components/workspace/Workspace2DCanvas";
import WorkspaceCommandPalette from "@/components/workspace/WorkspaceCommandPalette";
import {
  WorkspaceErrorState,
  WorkspaceLoadingState,
} from "@/components/workspace/WorkspaceShell";
import {
  actorMessageFromChatMessage,
  agentIdFromActorNode,
  createLocalDashboardSummary,
  createLocalId,
  defaultAgentIdForTask,
  defaultExpandedArtifactPaths,
  isValidWorkspaceId,
  lastChatMessageId,
  loadStoredCharacterProfile,
  mergeChatMessages,
  messageFromError,
  normalizeAgentSkillFiles,
  parseOptionalPositiveInteger,
  toFailureState,
  type ActorChatMessage,
  type AgentCreateFormState,
  type AgentMutationState,
  type ChatMutationState,
  type ConnectionModalKind,
  type ConnectionMutationState,
  type DashboardSummaryState,
  type GatewayConnectionState,
  type GatewayConnectionTestState,
  type GithubConnectionAction,
  type GithubConnectionState,
  type ModalLoadState,
  type PresenceMutationState,
  type PresenceStreamStatus,
  type SlackConnectionAction,
  type SlackConnectionState,
  type TaskChatPaginationState,
  type WorkspaceDetailState,
  type WorkspaceViewId,
} from "@/components/workspace/workspace-ui-helpers";
import {
  createWorkspaceAgent,
  deleteWorkspaceAgent,
  fetchWorkspaceAgents,
  updateWorkspaceAgent,
  type UpdateWorkspaceAgentInput,
  type WorkspaceAgent,
  type WorkspaceAgentCategory,
} from "@/lib/agents";
import {
  fetchArtifactFile,
  fetchArtifactTree,
  fetchOrchestrationPlanArtifact,
  type ArtifactFileContent,
  type ArtifactTree,
  type OrchestrationPlanArtifact,
} from "@/lib/artifacts";
import {
  hydrateDesktopAccessToken,
  readAuthUser,
  resolveAccessToken,
} from "@/lib/auth";
import {
  actorChatSessionKey,
  chatSessionMessagePageInput,
  fetchChatSessionMessages,
  fetchTaskChatMessages,
  readActorChatSessionIds,
  sendWorkspaceChatMessage,
  writeActorChatSessionIds,
  type ChatMessage,
} from "@/lib/chat";
import {
  createWorkspaceGatewayBinding,
  deleteStoredOpenClawGatewayStatus,
  fetchWorkspaceGatewayStatus,
  gatewayBindingToStatus,
  normalizeWorkspaceGatewayStatus,
  readStoredOpenClawGatewayStatus,
  testWorkspaceGatewayConnection,
  writeStoredOpenClawGatewayStatus,
} from "@/lib/gateway";
import {
  gatewayConnectionStatusLabel,
  normalizeGatewayForm,
} from "@/lib/gateway-utils";
import {
  createGithubCredential,
  deleteGithubCredential,
  deleteStoredGithubCredential,
  fetchGithubCredentials,
  readStoredGithubCredential,
  updateGithubCredential,
  writeStoredGithubCredential,
  type GithubCredential,
} from "@/lib/github";
import {
  createOrchestrationSession,
  dispatchOrchestrationSession,
  fetchOrchestrationSession,
  fetchOrchestrationSessions,
  type CreateOrchestrationSessionInput,
  type OrchestrationSession,
} from "@/lib/orchestrations";
import { computeBackoffDelayMs } from "@/lib/poll-bucket";
import {
  createPresencePositionScheduler,
  createWorkspacePresenceStream,
  parseWorkspacePresenceMessage,
  resolvePresenceStreamTarget,
  sanitizeWorkspacePresencePosition,
  updateWorkspacePresencePosition,
  type WorkspacePresenceMessage,
  type WorkspacePresencePosition,
} from "@/lib/presence";
import {
  createPresenceLiveState,
  mergePresenceMembers,
  type PresenceConnectionState,
  type PresenceMember,
} from "@/lib/presence-summary";
import {
  deleteSlackIntegration,
  deleteStoredSlackIntegration,
  fetchSlackInstallUrl,
  fetchSlackIntegrations,
  readStoredSlackIntegration,
  saveSlackPendingConnectionState,
  writeStoredSlackIntegration,
  type SlackIntegration,
} from "@/lib/slack";
import { isDesktop } from "@/lib/desktop/env";
import {
  cancelWorkspaceTask,
  createAndRunWorkspaceTask,
  createWorkspaceTask,
  fetchTaskReports,
  fetchWorkspaceTasks,
  recoverStaleWorkspaceTasks,
  requestTaskApproval,
  reviewTaskApproval,
  runWorkspaceTask,
  updateWorkspaceTaskStatus,
  type AgentReport,
  type CreateAndRunWorkspaceTaskInput,
  type TaskExecutionLog,
  type TaskStatus,
  type WorkspaceTask,
} from "@/lib/tasks";
import { readStoredTheme, toggleTheme, type ThemeMode } from "@/lib/theme";
import { useWorkspaceLayout } from "@/lib/use-workspace-layout";
import {
  fetchTaskInspectorSnapshot,
  useWorkspaceLiveSync,
} from "@/lib/use-workspace-live-sync";
import { useWorkspaceModelProviders } from "@/lib/use-workspace-model-providers";
import { hostDocumentHidden, onHostVisibilityChange } from "@/lib/visibility-poll";
import {
  getEngineeringRows,
  getStageRooms,
  getStageWorldSize,
  parseStagePct,
  pct as toStagePct,
  type StageActorPosition,
  type WorkspaceSceneNode,
} from "@/lib/workspace-2d";
import { useWorkspaceCommandItems } from "@/lib/workspace-command-items";
import {
  fetchWorkspaceDashboardSummary,
  fetchWorkspaceDetail,
  fetchWorkspaceMembers,
  type WorkspaceDashboardSummary,
  type WorkspaceMember,
} from "@/lib/workspaces";

const EMPTY_WORKSPACE_MEMBERS: WorkspaceMember[] = [];
const EMPTY_GITHUB_CREDENTIALS: GithubCredential[] = [];
const EMPTY_SLACK_INTEGRATIONS: SlackIntegration[] = [];
const VIEW_ORDER: WorkspaceViewId[] = [
  "office",
  "tasks",
  "approvals",
  "orchestrations",
  "artifacts",
];

export default function WorkspaceDetailPage({
  params,
}: {
  params: Promise<{ workspaceId: string }>;
}) {
  const { workspaceId } = use(params);
  const numericWorkspaceId = useMemo(() => Number(workspaceId), [workspaceId]);

  const {
    shortcutStyle,
    sidebarCollapsed,
    toggleSidebar,
    sidebarWidth,
    setSidebarWidth,
    resetSidebarWidth,
    inspectorWidth,
    setInspectorWidth,
    resetInspectorWidth,
  } = useWorkspaceLayout({ desktopShell: isDesktop() });

  const [state, setState] = useState<WorkspaceDetailState>({
    status: "loading",
    workspace: null,
    error: null,
  });
  const [tasks, setTasks] = useState<WorkspaceTask[]>([]);
  const [workspaceAgents, setWorkspaceAgents] = useState<WorkspaceAgent[]>([]);
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummaryState>({
    status: "idle",
    summary: null,
    error: null,
  });
  const [isRefreshingAll, setIsRefreshingAll] = useState(false);

  // Desktop 5-region navigation & chrome state
  const [activeView, setActiveView] = useState<WorkspaceViewId>("office");
  const [activeZone, setActiveZone] = useState<OfficeZoneId>("overview");
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const [mobileInspectorOpen, setMobileInspectorOpen] = useState(false);
  const [inspectorTarget, setInspectorTarget] = useState<InspectorTarget>(null);
  const [selectedActorNode, setSelectedActorNode] = useState<WorkspaceSceneNode | null>(null);
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => readStoredTheme());
  const [statusNotice, setStatusNotice] = useState<string | null>(null);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const [shortcutsOpen, setShortcutsOpen] = useState(false);
  const [cameraFocusTarget, setCameraFocusTarget] = useState<{
    position: StageActorPosition;
    nonce: number;
  } | null>(null);

  // Task board filters & mutations
  const [taskSearchQuery, setTaskSearchQuery] = useState("");
  const [taskStatusFilter, setTaskStatusFilter] = useState<TaskStatus | "ALL">("ALL");
  const [taskLayoutMode, setTaskLayoutMode] = useState<"kanban" | "list">("kanban");
  const [taskCreateOpen, setTaskCreateOpen] = useState(false);
  const [taskCreateDefaultAgentId, setTaskCreateDefaultAgentId] = useState<number | null>(null);
  const [taskCreateMutation, setTaskCreateMutation] = useState<AgentMutationState>({
    status: "idle",
    message: null,
    agentId: null,
  });
  const [mutatingTaskId, setMutatingTaskId] = useState<number | null>(null);
  const [recoveringStale, setRecoveringStale] = useState(false);
  const [staleRecoveryNotice, setStaleRecoveryNotice] = useState<{
    tone: "success" | "danger";
    message: string;
  } | null>(null);

  // Inspector task state
  const [inspectorTaskLogs, setInspectorTaskLogs] = useState<TaskExecutionLog[]>([]);
  const [inspectorTaskLogsState, setInspectorTaskLogsState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [inspectorTaskReports, setInspectorTaskReports] = useState<AgentReport[]>([]);
  const [inspectorTaskReportsState, setInspectorTaskReportsState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [inspectorTaskChatMessages, setInspectorTaskChatMessages] = useState<ChatMessage[]>([]);
  const [inspectorTaskChatState, setInspectorTaskChatState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [inspectorTaskChatInput, setInspectorTaskChatInput] = useState("");
  const [inspectorTaskChatMutation, setInspectorTaskChatMutation] = useState<ChatMutationState>({
    status: "idle",
    message: null,
  });

  // Agent hire & delete state
  const [agentHireOpen, setAgentHireOpen] = useState(false);
  const [agentCreateForm, setAgentCreateForm] = useState<AgentCreateFormState>({
    name: "",
    category: "CUSTOM",
    workspacePath: "",
    emoji: "",
    modelProviderId: null,
    modelId: "",
    fallbackModelId: "",
    skillFiles: [],
  });
  const [agentMutation, setAgentMutation] = useState<AgentMutationState>({
    status: "idle",
    message: null,
    agentId: null,
  });
  const [agentDeleteConfirmId, setAgentDeleteConfirmId] = useState<number | null>(null);

  // Actor chat session state
  const [actorChatInput, setActorChatInput] = useState("");
  const [actorChatMessages, setActorChatMessages] = useState<ActorChatMessage[]>([]);
  const [actorChatSessionId, setActorChatSessionId] = useState<number | null>(null);
  const [actorChatSessionIds, setActorChatSessionIds] = useState<Record<string, number>>(() => {
    if (typeof window === "undefined") {
      return {};
    }
    return readActorChatSessionIds(window.localStorage, numericWorkspaceId);
  });
  const [actorChatMutation, setActorChatMutation] = useState<ChatMutationState>({
    status: "idle",
    message: null,
  });
  const activeInspectorRef = useRef<{ target: InspectorTarget; actorSessionId: number | null }>({
    target: null,
    actorSessionId: null,
  });
  const lastFailedActorMsgRef = useRef<{
    nodeId: string;
    bubbleId: string;
    message: string;
  } | null>(null);
  const lastFailedTaskMsgRef = useRef<{ taskId: number; message: string } | null>(null);

  // Task reports/chat/orchestration plan modals
  const [selectedReportsTask, setSelectedReportsTask] = useState<WorkspaceTask | null>(null);
  const [modalTaskReports, setModalTaskReports] = useState<AgentReport[]>([]);
  const [modalReportState, setModalReportState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });

  const [selectedChatTask, setSelectedChatTask] = useState<WorkspaceTask | null>(null);
  const [modalTaskChatMessages, setModalTaskChatMessages] = useState<ChatMessage[]>([]);
  const [modalTaskChatState, setModalTaskChatState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [modalTaskChatInput, setModalTaskChatInput] = useState("");
  const [modalTaskChatAgentIdInput, setModalTaskChatAgentIdInput] = useState("");
  const [modalTaskChatMutation, setModalTaskChatMutation] = useState<ChatMutationState>({
    status: "idle",
    message: null,
  });
  const [modalTaskChatSessionId, setModalTaskChatSessionId] = useState<number | null>(null);
  const [modalTaskChatCursor, setModalTaskChatCursor] = useState<number | null>(null);
  const [modalTaskChatHasMore, setModalTaskChatHasMore] = useState(false);
  const [modalTaskChatPaginationState, setModalTaskChatPaginationState] =
    useState<TaskChatPaginationState>({ status: "idle", message: null });

  const [orchestrationPlan, setOrchestrationPlan] = useState<OrchestrationPlanArtifact | null>(null);
  const [orchestrationPlanState, setOrchestrationPlanState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });

  // Multi-agent orchestration view state
  const [orchestrationSessions, setOrchestrationSessions] = useState<OrchestrationSession[]>([]);
  const [orchestrationSessionsState, setOrchestrationSessionsState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [selectedOrchestrationSession, setSelectedOrchestrationSession] =
    useState<OrchestrationSession | null>(null);

  // Artifact view & modal state
  const [artifactTree, setArtifactTree] = useState<ArtifactTree | null>(null);
  const [artifactTreeState, setArtifactTreeState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [selectedArtifactPath, setSelectedArtifactPath] = useState<string | null>(null);
  const [selectedArtifactFile, setSelectedArtifactFile] = useState<ArtifactFileContent | null>(null);
  const [selectedArtifactFileState, setSelectedArtifactFileState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [modalArtifactFile, setModalArtifactFile] = useState<ArtifactFileContent | null>(null);
  const [modalArtifactPath, setModalArtifactPath] = useState<string | null>(null);
  const [modalArtifactNameHint, setModalArtifactNameHint] = useState<string | null>(null);
  const [modalArtifactState, setModalArtifactState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });

  // Presence state
  const [presenceStreamStatus, setPresenceStreamStatus] = useState<PresenceStreamStatus>("idle");
  const [presenceEvents, setPresenceEvents] = useState<WorkspacePresenceMessage[]>([]);
  const [presenceMembers, setPresenceMembers] = useState<PresenceMember[]>([]);
  const [playerPositionOverride, setPlayerPositionOverride] = useState<StageActorPosition | null>(
    null,
  );
  const [presenceMutation, setPresenceMutation] = useState<PresenceMutationState>({
    status: "idle",
    message: null,
  });

  // Connections modal state
  const [connectionModal, setConnectionModal] = useState<ConnectionModalKind>(null);
  const [slackConnectionState, setSlackConnectionState] = useState<SlackConnectionState>(() => {
    if (typeof window === "undefined") {
      return { status: "idle", integrations: [], error: null };
    }
    const stored = readStoredSlackIntegration(window.localStorage, numericWorkspaceId);
    return stored
      ? { status: "ready", integrations: [stored], error: null }
      : { status: "idle", integrations: [], error: null };
  });
  const [slackConnectionMutation, setSlackConnectionMutation] = useState<ConnectionMutationState>({
    status: "idle",
    message: null,
  });
  const [slackConnectionAction, setSlackConnectionAction] =
    useState<SlackConnectionAction | null>(null);

  const [githubConnectionState, setGithubConnectionState] = useState<GithubConnectionState>(() => {
    if (typeof window === "undefined") {
      return { status: "idle", credentials: [], error: null };
    }
    const stored = readStoredGithubCredential(window.localStorage, numericWorkspaceId);
    return stored
      ? { status: "ready", credentials: [stored], error: null }
      : { status: "idle", credentials: [], error: null };
  });
  const [githubDisplayNameInput, setGithubDisplayNameInput] = useState("");
  const [githubTokenInput, setGithubTokenInput] = useState("");
  const [selectedGithubCredentialId, setSelectedGithubCredentialId] = useState<number | null>(() => {
    if (typeof window === "undefined") {
      return null;
    }
    return readStoredGithubCredential(window.localStorage, numericWorkspaceId)?.credentialId ?? null;
  });
  const [editingGithubCredentialId, setEditingGithubCredentialId] = useState<number | null>(null);
  const [githubEditDisplayNameInput, setGithubEditDisplayNameInput] = useState("");
  const [githubEditTokenInput, setGithubEditTokenInput] = useState("");
  const [githubConnectionCreateMutation, setGithubConnectionCreateMutation] =
    useState<ConnectionMutationState>({ status: "idle", message: null });
  const [githubConnectionActionMutation, setGithubConnectionActionMutation] =
    useState<ConnectionMutationState>({ status: "idle", message: null });
  const [githubConnectionAction, setGithubConnectionAction] =
    useState<GithubConnectionAction | null>(null);

  const [gatewayConnectionState, setGatewayConnectionState] = useState<GatewayConnectionState>(
    () => {
      if (typeof window === "undefined") {
        return { status: "idle", gateway: null, error: null };
      }
      const stored = readStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId);
      return stored
        ? { status: "ready", gateway: stored, error: null }
        : { status: "idle", gateway: null, error: null };
    },
  );
  const [gatewayUrlInput, setGatewayUrlInput] = useState(() => {
    if (typeof window === "undefined") {
      return "";
    }
    return readStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId)?.gatewayUrl ?? "";
  });
  const [gatewayTokenInput, setGatewayTokenInput] = useState("");
  const [gatewayConnectionTestState, setGatewayConnectionTestState] =
    useState<GatewayConnectionTestState>({
      status: "idle",
      result: null,
      message: null,
    });
  const [gatewayBindingMutation, setGatewayBindingMutation] = useState<ConnectionMutationState>({
    status: "idle",
    message: null,
  });

  // ── Data Loaders ─────────────────────────────────────────────
  const reloadTasks = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setTasks([]);
      return;
    }
    try {
      const nextTasks = await fetchWorkspaceTasks(resolveAccessToken(), numericWorkspaceId);
      setTasks(nextTasks);
    } catch {
      setTasks([]);
    }
  }, [numericWorkspaceId]);

  const reloadAgents = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setWorkspaceAgents([]);
      return;
    }
    try {
      const agents = await fetchWorkspaceAgents(resolveAccessToken(), numericWorkspaceId);
      setWorkspaceAgents(agents);
    } catch {
      setWorkspaceAgents([]);
    }
  }, [numericWorkspaceId]);

  const reloadDashboardSummary = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setDashboardSummary({
        status: "error",
        summary: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }
    setDashboardSummary({ status: "loading", summary: null, error: null });
    try {
      const summary = await fetchWorkspaceDashboardSummary(resolveAccessToken(), numericWorkspaceId);
      setDashboardSummary({ status: "ready", summary, error: null });
    } catch (error) {
      setDashboardSummary({
        status: "error",
        summary: null,
        error: messageFromError(error, "대시보드 요약을 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const refreshTaskData = useCallback(async () => {
    await Promise.all([reloadTasks(), reloadDashboardSummary()]);
  }, [reloadDashboardSummary, reloadTasks]);

  const refreshAgentData = useCallback(async () => {
    await Promise.all([reloadAgents(), reloadDashboardSummary()]);
  }, [reloadAgents, reloadDashboardSummary]);

  const loadGatewayConnection = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setGatewayConnectionState({
        status: "error",
        gateway: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    setGatewayConnectionState({ status: "loading", gateway: null, error: null });
    setGatewayConnectionTestState({ status: "idle", result: null, message: null });
    setGatewayBindingMutation({ status: "idle", message: null });

    try {
      const gateway = await fetchWorkspaceGatewayStatus(resolveAccessToken(), numericWorkspaceId);
      const normalized = normalizeWorkspaceGatewayStatus(gateway);
      if (typeof window !== "undefined") {
        if (normalized.bound) {
          writeStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId, normalized);
        } else {
          deleteStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId);
        }
      }
      setGatewayConnectionState({ status: "ready", gateway: normalized, error: null });
      setGatewayUrlInput((current) => current || normalized.gatewayUrl || "");
    } catch (error) {
      setGatewayConnectionState({
        status: "error",
        gateway: null,
        error: messageFromError(error, "OpenClaw 게이트웨이 상태를 확인하지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const reloadOrchestrations = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      return;
    }
    setOrchestrationSessionsState({ status: "loading", error: null });
    try {
      const sessions = await fetchOrchestrationSessions(resolveAccessToken(), numericWorkspaceId);
      setOrchestrationSessions(sessions);
      setOrchestrationSessionsState({ status: "ready", error: null });
      setSelectedOrchestrationSession((current) => {
        if (current) {
          return sessions.find((s) => s.sessionId === current.sessionId) ?? sessions[0] ?? null;
        }
        return sessions[0] ?? null;
      });
    } catch (error) {
      setOrchestrationSessionsState({
        status: "error",
        error: messageFromError(error, "오케스트레이션 세션 목록을 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const refreshOrchestrationsSilent = useCallback(
    async (signal?: AbortSignal) => {
      if (!isValidWorkspaceId(numericWorkspaceId)) {
        return;
      }
      const token = resolveAccessToken();
      if (!token || signal?.aborted) {
        return;
      }
      try {
        const sessions = await fetchOrchestrationSessions(token, numericWorkspaceId, signal);
        if (signal?.aborted) {
          return;
        }
        setOrchestrationSessions(sessions);
        setSelectedOrchestrationSession((current) =>
          current ? sessions.find((s) => s.sessionId === current.sessionId) ?? sessions[0] ?? null : sessions[0] ?? null,
        );
      } catch {
        // Keep prior orchestration sessions visible when background sync fails
      }
    },
    [numericWorkspaceId],
  );

  const loadArtifactTreeData = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      return;
    }
    setArtifactTreeState({ status: "loading", error: null });
    try {
      const tree = await fetchArtifactTree(resolveAccessToken(), numericWorkspaceId);
      setArtifactTree(tree);
      setArtifactTreeState({ status: "ready", error: null });
      defaultExpandedArtifactPaths(tree);
    } catch (error) {
      setArtifactTree(null);
      setArtifactTreeState({
        status: "error",
        error: messageFromError(error, "산출물 파일 트리를 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const selectInlineArtifactFile = useCallback(
    async (path: string) => {
      setSelectedArtifactPath(path);
      setSelectedArtifactFile(null);
      setSelectedArtifactFileState({ status: "loading", error: null });
      try {
        const file = await fetchArtifactFile(resolveAccessToken(), numericWorkspaceId, path);
        setSelectedArtifactFile(file);
        setSelectedArtifactFileState({ status: "ready", error: null });
      } catch (error) {
        setSelectedArtifactFileState({
          status: "error",
          error: messageFromError(error, "산출물 파일을 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const openModalArtifactFile = useCallback(
    async (path: string, nameHint?: string) => {
      setModalArtifactFile(null);
      setModalArtifactPath(path);
      setModalArtifactNameHint(nameHint ?? path.split("/").pop() ?? path);
      setModalArtifactState({ status: "loading", error: null });
      try {
        const file = await fetchArtifactFile(resolveAccessToken(), numericWorkspaceId, path);
        setModalArtifactFile(file);
        setModalArtifactState({ status: "ready", error: null });
      } catch (error) {
        setModalArtifactState({
          status: "error",
          error: messageFromError(error, "산출물 파일을 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const reloadWorkspace = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setState({
        status: "error",
        workspace: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }
    setIsRefreshingAll(true);
    try {
      const [workspace, members] = await Promise.all([
        fetchWorkspaceDetail(resolveAccessToken(), numericWorkspaceId),
        fetchWorkspaceMembers(resolveAccessToken(), numericWorkspaceId),
      ]);
      setState({ status: "ready", workspace, members, error: null });
      await Promise.all([
        reloadTasks(),
        reloadAgents(),
        reloadDashboardSummary(),
        loadGatewayConnection(),
      ]);
    } catch (error) {
      setState(toFailureState(error, "워크스페이스 정보를 확인하지 못했습니다."));
    } finally {
      setIsRefreshingAll(false);
    }
  }, [loadGatewayConnection, numericWorkspaceId, reloadAgents, reloadDashboardSummary, reloadTasks]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialWorkspace() {
      await hydrateDesktopAccessToken();
      if (ignore) {
        return;
      }

      if (!isValidWorkspaceId(numericWorkspaceId)) {
        setState({
          status: "error",
          workspace: null,
          error: "올바르지 않은 워크스페이스입니다.",
        });
        setTasks([]);
        setWorkspaceAgents([]);
        return;
      }

      try {
        const token = resolveAccessToken();
        const [
          workspace,
          members,
          initialTasksResult,
          initialAgentsResult,
          initialSummaryResult,
          initialGatewayResult,
        ] = await Promise.all([
          fetchWorkspaceDetail(token, numericWorkspaceId),
          fetchWorkspaceMembers(token, numericWorkspaceId),
          fetchWorkspaceTasks(token, numericWorkspaceId)
            .then((nextTasks) => ({ ok: true as const, nextTasks }))
            .catch(() => ({ ok: false as const })),
          fetchWorkspaceAgents(token, numericWorkspaceId)
            .then((nextAgents) => ({ ok: true as const, nextAgents }))
            .catch(() => ({ ok: false as const })),
          fetchWorkspaceDashboardSummary(token, numericWorkspaceId)
            .then((summary) => ({ ok: true as const, summary }))
            .catch((error) => ({ ok: false as const, error })),
          fetchWorkspaceGatewayStatus(token, numericWorkspaceId)
            .then((gateway) => ({ ok: true as const, gateway }))
            .catch((error) => ({ ok: false as const, error })),
        ]);

        if (ignore) {
          return;
        }

        setState({ status: "ready", workspace, members, error: null });
        setTasks(initialTasksResult.ok ? initialTasksResult.nextTasks : []);
        setWorkspaceAgents(initialAgentsResult.ok ? initialAgentsResult.nextAgents : []);

        if (initialSummaryResult.ok) {
          setDashboardSummary({
            status: "ready",
            summary: initialSummaryResult.summary,
            error: null,
          });
        } else {
          setDashboardSummary({
            status: "error",
            summary: null,
            error: messageFromError(
              initialSummaryResult.error,
              "대시보드 요약을 불러오지 못했습니다.",
            ),
          });
        }

        if (initialGatewayResult.ok) {
          const normalized = normalizeWorkspaceGatewayStatus(initialGatewayResult.gateway);
          setGatewayConnectionState({ status: "ready", gateway: normalized, error: null });
          setGatewayUrlInput((current) => current || normalized.gatewayUrl || "");
        } else {
          setGatewayConnectionState({
            status: "error",
            gateway: null,
            error: messageFromError(
              initialGatewayResult.error,
              "OpenClaw 게이트웨이 상태를 확인하지 못했습니다.",
            ),
          });
        }
      } catch (error) {
        if (!ignore) {
          setState(toFailureState(error, "워크스페이스 정보를 확인하지 못했습니다."));
        }
      }
    }

    void loadInitialWorkspace();
    return () => {
      ignore = true;
    };
  }, [numericWorkspaceId]);

  useEffect(() => {
    if (!statusNotice) {
      return;
    }
    const timer = window.setTimeout(() => {
      setStatusNotice(null);
    }, 4500);
    return () => {
      window.clearTimeout(timer);
    };
  }, [statusNotice]);

  useEffect(() => {
    let ignore = false;
    let stream: EventSource | null = null;
    let ticketAbort: AbortController | null = null;
    let reconnectTimer: number | null = null;
    let reconnectFailures = 0;

    function clearReconnectTimer() {
      if (reconnectTimer !== null) {
        window.clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
    }

    function teardownStream() {
      clearReconnectTimer();
      ticketAbort?.abort();
      ticketAbort = null;
      stream?.close();
      stream = null;
    }

    function scheduleReconnect() {
      if (ignore || hostDocumentHidden()) {
        return;
      }
      clearReconnectTimer();
      const attempt = Math.max(0, reconnectFailures - 1);
      const delayMs = computeBackoffDelayMs(attempt, { baseMs: 1000, maxMs: 30_000 });
      reconnectTimer = window.setTimeout(() => {
        reconnectTimer = null;
        void connectPresenceStream();
      }, delayMs);
    }

    async function connectPresenceStream() {
      await Promise.resolve();
      if (ignore) {
        return;
      }

      if (state.status !== "ready" || !isValidWorkspaceId(numericWorkspaceId)) {
        setPresenceStreamStatus("idle");
        setPresenceEvents([]);
        setPresenceMembers([]);
        return;
      }

      if (hostDocumentHidden()) {
        return;
      }

      const accessToken = resolveAccessToken();
      if (!accessToken) {
        setPresenceStreamStatus("error");
        return;
      }

      teardownStream();
      setPresenceStreamStatus(reconnectFailures >= 2 ? "error" : "connecting");

      const controller = new AbortController();
      ticketAbort = controller;
      let target: Awaited<ReturnType<typeof resolvePresenceStreamTarget>>;
      try {
        target = await resolvePresenceStreamTarget(
          numericWorkspaceId,
          accessToken,
          controller.signal,
        );
      } catch {
        if (ignore || controller.signal.aborted) {
          return;
        }
        reconnectFailures += 1;
        setPresenceStreamStatus(reconnectFailures >= 2 ? "error" : "connecting");
        scheduleReconnect();
        return;
      }

      if (ignore || controller.signal.aborted) {
        return;
      }
      ticketAbort = null;

      stream = createWorkspacePresenceStream(numericWorkspaceId, target);

      stream.onopen = () => {
        reconnectFailures = 0;
        setPresenceStreamStatus("open");
      };
      stream.onmessage = (event) => {
        const message = parseWorkspacePresenceMessage(String(event.data));
        if (!message) {
          return;
        }
        setPresenceMembers((current) => mergePresenceMembers(current, message));
        setPresenceEvents((current) => [message, ...current].slice(0, 5));
        if (message.type === "presence.error") {
          setPresenceStreamStatus("error");
          return;
        }
        reconnectFailures = 0;
      };
      stream.onerror = () => {
        stream?.close();
        stream = null;
        if (ignore) {
          return;
        }
        reconnectFailures += 1;
        setPresenceStreamStatus(reconnectFailures >= 2 ? "error" : "connecting");
        scheduleReconnect();
      };
    }

    const unsubscribeVisibility = onHostVisibilityChange(() => {
      if (hostDocumentHidden()) {
        teardownStream();
        return;
      }
      if (!ignore && !stream && state.status === "ready" && isValidWorkspaceId(numericWorkspaceId)) {
        void connectPresenceStream();
      }
    });

    void connectPresenceStream();
    return () => {
      ignore = true;
      unsubscribeVisibility();
      teardownStream();
    };
  }, [numericWorkspaceId, state.status]);

  useEffect(() => {
    activeInspectorRef.current = {
      target: inspectorTarget,
      actorSessionId: actorChatSessionId,
    };
  }, [actorChatSessionId, inspectorTarget]);

  // ── Inspector Loaders ────────────────────────────────────────
  const isCurrentTaskInspector = (taskId: number): boolean =>
    activeInspectorRef.current.target?.type === "task" &&
    activeInspectorRef.current.target.taskId === taskId;

  const loadInspectorTaskDetails = useCallback(
    async (taskId: number) => {
      setInspectorTaskLogs([]);
      setInspectorTaskLogsState({ status: "loading", error: null });
      setInspectorTaskReports([]);
      setInspectorTaskReportsState({ status: "loading", error: null });
      setInspectorTaskChatMessages([]);
      setInspectorTaskChatState({ status: "loading", error: null });
      setInspectorTaskChatMutation({ status: "idle", message: null });

      const snapshot = await fetchTaskInspectorSnapshot(resolveAccessToken(), numericWorkspaceId, taskId);
      if (!isCurrentTaskInspector(taskId)) {
        return;
      }

      if (snapshot.logsRes.status === "fulfilled") {
        setInspectorTaskLogs(snapshot.logsRes.value);
        setInspectorTaskLogsState({ status: "ready", error: null });
      } else {
        setInspectorTaskLogsState({
          status: "error",
          error: messageFromError(snapshot.logsRes.reason, "실행 로그를 불러오지 못했습니다."),
        });
      }

      if (snapshot.reportsRes.status === "fulfilled") {
        setInspectorTaskReports(snapshot.reportsRes.value);
        setInspectorTaskReportsState({ status: "ready", error: null });
      } else {
        setInspectorTaskReportsState({
          status: "error",
          error: messageFromError(snapshot.reportsRes.reason, "리포트를 불러오지 못했습니다."),
        });
      }

      if (snapshot.resolvedChatMessages !== null) {
        setInspectorTaskChatMessages(snapshot.resolvedChatMessages);
        setInspectorTaskChatState({ status: "ready", error: null });
      } else if (snapshot.chatRes.status === "rejected") {
        setInspectorTaskChatState({
          status: "error",
          error: messageFromError(snapshot.chatRes.reason, "작업 대화를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const refreshInspectorTaskDetailsSilent = useCallback(
    async (taskId: number, signal?: AbortSignal) => {
      const token = resolveAccessToken();
      if (!token || signal?.aborted || !isCurrentTaskInspector(taskId)) {
        return;
      }
      const snapshot = await fetchTaskInspectorSnapshot(token, numericWorkspaceId, taskId, signal);
      if (signal?.aborted || !isCurrentTaskInspector(taskId)) {
        return;
      }
      if (snapshot.logsRes.status === "fulfilled") {
        setInspectorTaskLogs(snapshot.logsRes.value);
      }
      if (snapshot.reportsRes.status === "fulfilled") {
        setInspectorTaskReports(snapshot.reportsRes.value);
      }
      if (snapshot.resolvedChatMessages !== null) {
        const nextMessages = snapshot.resolvedChatMessages;
        setInspectorTaskChatMessages((current) => mergeChatMessages(current, nextMessages));
      }
    },
    [numericWorkspaceId],
  );

  const openTaskInspector = useCallback(
    (taskId: number) => {
      const nextTarget: InspectorTarget = { type: "task", taskId };
      activeInspectorRef.current = {
        target: nextTarget,
        actorSessionId: activeInspectorRef.current.actorSessionId,
      };
      setInspectorTarget(nextTarget);
      setMobileInspectorOpen(true);
      void loadInspectorTaskDetails(taskId);
    },
    [loadInspectorTaskDetails],
  );

  const isCurrentActorSession = (sessionId: number, nodeId?: string): boolean => {
    const cur = activeInspectorRef.current;
    if (cur.target?.type !== "actor" || cur.actorSessionId !== sessionId) {
      return false;
    }
    return nodeId === undefined || cur.target.nodeId === nodeId;
  };

  const pollActorChatMessages = useCallback(
    async (sessionId: number) => {
      try {
        const page = await fetchChatSessionMessages(
          resolveAccessToken(),
          numericWorkspaceId,
          sessionId,
          chatSessionMessagePageInput(),
        );
        if (!isCurrentActorSession(sessionId)) {
          return;
        }
        setActorChatMessages(page.messages.map(actorMessageFromChatMessage));
        setActorChatMutation({ status: "idle", message: null });
      } catch (error) {
        if (!isCurrentActorSession(sessionId)) {
          return;
        }
        setActorChatMutation({
          status: "error",
          message: messageFromError(error, "에이전트 대화 메시지를 갱신하지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const pollActorChatSilent = useCallback(
    async (sessionId: number, nodeId: string, signal: AbortSignal) => {
      const token = resolveAccessToken();
      if (!token || signal.aborted || !isCurrentActorSession(sessionId, nodeId)) {
        return;
      }
      try {
        const page = await fetchChatSessionMessages(
          token,
          numericWorkspaceId,
          sessionId,
          chatSessionMessagePageInput(),
          signal,
        );
        if (signal.aborted || !isCurrentActorSession(sessionId, nodeId)) {
          return;
        }
        setActorChatMessages(page.messages.map(actorMessageFromChatMessage));
      } catch {
        // Keep active chat state intact when background sync transiently fails
      }
    },
    [numericWorkspaceId],
  );

  const handleSelectActor = useCallback(
    (node: WorkspaceSceneNode) => {
      const sessionId =
        node.kind === "agent" ? actorChatSessionIds[actorChatSessionKey(node.id)] ?? null : null;
      if (selectedActorNode?.id !== node.id) {
        setActorChatInput("");
        setActorChatMessages([]);
        lastFailedActorMsgRef.current = null;
      }
      const nextTarget: InspectorTarget = { type: "actor", nodeId: node.id };
      activeInspectorRef.current = { target: nextTarget, actorSessionId: sessionId };
      setActorChatSessionId(sessionId);
      setSelectedActorNode(node);
      setInspectorTarget(nextTarget);
      setMobileInspectorOpen(true);
      setAgentDeleteConfirmId(null);
      setActorChatMutation({ status: "idle", message: null });
      if (sessionId) {
        void pollActorChatMessages(sessionId);
      }
    },
    [actorChatSessionIds, pollActorChatMessages, selectedActorNode?.id],
  );

  const handleSelectAgentFromSidebar = useCallback(
    (agentId: number) => {
      const agent = workspaceAgents.find((item) => item.agentId === agentId);
      const actorId = `agent-${agentId}`;
      const node: WorkspaceSceneNode = {
        id: actorId,
        kind: "agent",
        label: agent?.name ?? `에이전트 #${agentId}`,
        detail: agent?.category ?? "CUSTOM",
        status: agent?.status === "READY" ? "online" : "working",
        x: 32,
        y: 36,
      };
      handleSelectActor(node);
    },
    [handleSelectActor, workspaceAgents],
  );

  // ── Agent CRUD Handlers ──────────────────────────────────────
  const handleCreateAgent = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const name = agentCreateForm.name.trim();
    if (!name) {
      setAgentMutation({ status: "error", message: "에이전트 이름을 입력해 주세요.", agentId: null });
      return;
    }
    const skillFiles = normalizeAgentSkillFiles(agentCreateForm.skillFiles);
    if (skillFiles === null) {
      setAgentMutation({
        status: "error",
        message: "스킬 파일은 파일명과 내용을 모두 입력해 주세요.",
        agentId: null,
      });
      return;
    }

    setAgentMutation({ status: "submitting", message: "에이전트 등록 중", agentId: null });
    try {
      const created = await createWorkspaceAgent(resolveAccessToken(), numericWorkspaceId, {
        name,
        category: agentCreateForm.category,
        workspacePath: agentCreateForm.workspacePath.trim() || undefined,
        emoji: agentCreateForm.emoji.trim() || undefined,
        modelProviderId: agentCreateForm.modelProviderId ?? undefined,
        modelId: agentCreateForm.modelId?.trim() || undefined,
        fallbackModelId: agentCreateForm.fallbackModelId?.trim() || undefined,
        skillFiles: skillFiles.length > 0 ? skillFiles : undefined,
      });
      setAgentCreateForm({
        name: "",
        category: "CUSTOM",
        workspacePath: "",
        emoji: "",
        modelProviderId: null,
        modelId: "",
        fallbackModelId: "",
        skillFiles: [],
      });
      await refreshAgentData();
      setAgentMutation({
        status: "success",
        message: `에이전트 #${created.agentId} 등록 완료`,
        agentId: created.agentId,
      });
      setAgentHireOpen(false);
      setStatusNotice(`에이전트 '${created.name}' 등록됨`);
    } catch (error) {
      setAgentMutation({
        status: "error",
        message: messageFromError(error, "에이전트 등록에 실패했습니다."),
        agentId: null,
      });
    }
  };

  const handleUpdateAgent = useCallback(
    async (agentId: number, input: UpdateWorkspaceAgentInput) => {
      const updated = await updateWorkspaceAgent(
        resolveAccessToken(),
        numericWorkspaceId,
        agentId,
        input,
      );
      setWorkspaceAgents((current) =>
        current.map((item) => (item.agentId === updated.agentId ? updated : item)),
      );
      await reloadDashboardSummary();
      setStatusNotice(`에이전트 '${updated.name}' 설정 저장됨`);
    },
    [numericWorkspaceId, reloadDashboardSummary],
  );

  const handleConfirmDeleteAgent = useCallback(
    async (agent: WorkspaceAgent) => {
      setAgentMutation({
        status: "submitting",
        message: "에이전트 삭제 중",
        agentId: agent.agentId,
      });
      try {
        await deleteWorkspaceAgent(resolveAccessToken(), numericWorkspaceId, agent.agentId);
        setAgentDeleteConfirmId(null);
        setInspectorTarget(null);
        setSelectedActorNode(null);
        await refreshAgentData();
        setAgentMutation({
          status: "success",
          message: `에이전트 '${agent.name}' 삭제 완료`,
          agentId: agent.agentId,
        });
        setStatusNotice(`에이전트 '${agent.name}' 삭제됨`);
      } catch (error) {
        setAgentMutation({
          status: "error",
          message: messageFromError(error, "에이전트를 삭제하지 못했습니다."),
          agentId: agent.agentId,
        });
      }
    },
    [numericWorkspaceId, refreshAgentData],
  );

  // ── Task CRUD & Workflow Handlers ────────────────────────────
  const handleCreateTask = useCallback(
    async (input: CreateAndRunWorkspaceTaskInput, runImmediately: boolean) => {
      setTaskCreateMutation({ status: "submitting", message: "작업 생성 중", agentId: null });
      try {
        if (runImmediately) {
          const runResult = await createAndRunWorkspaceTask(
            resolveAccessToken(),
            numericWorkspaceId,
            input,
          );
          setStatusNotice(`작업 #${runResult.taskId} 생성 및 실행 시작`);
        } else {
          const created = await createWorkspaceTask(resolveAccessToken(), numericWorkspaceId, input);
          setStatusNotice(`작업 #${created.taskId} 생성 완료`);
        }
        setTaskCreateMutation({ status: "success", message: null, agentId: null });
        setTaskCreateOpen(false);
        setTaskCreateDefaultAgentId(null);
        await refreshTaskData();
      } catch (error) {
        setTaskCreateMutation({
          status: "error",
          message: messageFromError(error, "작업을 생성하지 못했습니다."),
          agentId: null,
        });
      }
    },
    [numericWorkspaceId, refreshTaskData],
  );

  const handleRunTask = useCallback(
    async (task: WorkspaceTask) => {
      setMutatingTaskId(task.taskId);
      try {
        await runWorkspaceTask(resolveAccessToken(), numericWorkspaceId, task.taskId, {});
        setStatusNotice(`작업 #${task.taskId} 실행 시작`);
        await refreshTaskData();
        if (inspectorTarget?.type === "task" && inspectorTarget.taskId === task.taskId) {
          void loadInspectorTaskDetails(task.taskId);
        }
      } catch (error) {
        setStatusNotice(messageFromError(error, "작업을 실행하지 못했습니다."));
      } finally {
        setMutatingTaskId(null);
      }
    },
    [inspectorTarget, loadInspectorTaskDetails, numericWorkspaceId, refreshTaskData],
  );

  const handleRequestTaskApproval = useCallback(
    async (task: WorkspaceTask) => {
      setMutatingTaskId(task.taskId);
      try {
        await requestTaskApproval(resolveAccessToken(), numericWorkspaceId, task.taskId, {});
        setStatusNotice(`작업 #${task.taskId} 승인 대기함으로 이동됨`);
        await refreshTaskData();
      } catch (error) {
        setStatusNotice(messageFromError(error, "승인 요청을 처리하지 못했습니다."));
      } finally {
        setMutatingTaskId(null);
      }
    },
    [numericWorkspaceId, refreshTaskData],
  );

  const handleReviewApproval = useCallback(
    async (
      taskId: number,
      input: { decision: "APPROVE" | "REJECT"; comment?: string; runAfterApprove?: boolean },
    ) => {
      await reviewTaskApproval(resolveAccessToken(), numericWorkspaceId, taskId, input);
      setStatusNotice(
        input.decision === "APPROVE" ? `작업 #${taskId} 승인 완료` : `작업 #${taskId} 반려 완료`,
      );
      await refreshTaskData();
      if (inspectorTarget?.type === "task" && inspectorTarget.taskId === taskId) {
        void loadInspectorTaskDetails(taskId);
      }
    },
    [inspectorTarget, loadInspectorTaskDetails, numericWorkspaceId, refreshTaskData],
  );

  const handleCancelTask = useCallback(
    async (task: WorkspaceTask) => {
      setMutatingTaskId(task.taskId);
      try {
        await cancelWorkspaceTask(resolveAccessToken(), numericWorkspaceId, task.taskId);
        setStatusNotice(`작업 #${task.taskId} 취소됨`);
        await refreshTaskData();
      } catch (error) {
        setStatusNotice(messageFromError(error, "작업을 취소하지 못했습니다."));
      } finally {
        setMutatingTaskId(null);
      }
    },
    [numericWorkspaceId, refreshTaskData],
  );

  const handleUpdateTaskStatus = useCallback(
    async (taskId: number, status: TaskStatus) => {
      setMutatingTaskId(taskId);
      try {
        await updateWorkspaceTaskStatus(resolveAccessToken(), numericWorkspaceId, taskId, {
          status,
        });
        setStatusNotice(`작업 #${taskId} 상태 변경됨`);
        await refreshTaskData();
      } catch (error) {
        setStatusNotice(messageFromError(error, "작업 상태를 변경하지 못했습니다."));
      } finally {
        setMutatingTaskId(null);
      }
    },
    [numericWorkspaceId, refreshTaskData],
  );

  const handleRecoverStaleTasks = useCallback(async () => {
    setRecoveringStale(true);
    setStaleRecoveryNotice(null);
    try {
      const result = await recoverStaleWorkspaceTasks(resolveAccessToken(), numericWorkspaceId, {
        staleMinutes: 15,
      });
      const message = `고착 복구 완료: 작업 ${result.recoveredTaskCount}건, 실행 ${result.recoveredExecutionCount}건`;
      setStaleRecoveryNotice({ tone: "success", message });
      setStatusNotice(message);
      await refreshTaskData();
    } catch (error) {
      setStaleRecoveryNotice({
        tone: "danger",
        message: messageFromError(error, "고착 작업 복구에 실패했습니다."),
      });
    } finally {
      setRecoveringStale(false);
    }
  }, [numericWorkspaceId, refreshTaskData]);

  // ── Orchestration Handlers ───────────────────────────────────
  const handleSelectOrchestrationSession = useCallback(
    async (sessionId: number) => {
      const local = orchestrationSessions.find((s) => s.sessionId === sessionId) ?? null;
      if (local) {
        setSelectedOrchestrationSession(local);
      }
      try {
        const fresh = await fetchOrchestrationSession(
          resolveAccessToken(),
          numericWorkspaceId,
          sessionId,
        );
        setSelectedOrchestrationSession(fresh);
      } catch {
        // Keep local snapshot when detail fetch transiently fails
      }
    },
    [numericWorkspaceId, orchestrationSessions],
  );

  const handleCreateOrchestrationSession = useCallback(
    async (input: CreateOrchestrationSessionInput) => {
      const created = await createOrchestrationSession(
        resolveAccessToken(),
        numericWorkspaceId,
        input,
      );
      setOrchestrationSessions((current) => [
        created,
        ...current.filter((item) => item.sessionId !== created.sessionId),
      ]);
      setSelectedOrchestrationSession(created);
      setStatusNotice(`오케스트레이션 세션 #${created.sessionId} 생성 완료`);
    },
    [numericWorkspaceId],
  );

  const handleDispatchOrchestrationSession = useCallback(
    async (sessionId: number, createPr: boolean) => {
      await dispatchOrchestrationSession(resolveAccessToken(), numericWorkspaceId, sessionId, {
        createPr,
      });
      await Promise.all([reloadOrchestrations(), refreshTaskData()]);
      setStatusNotice(`세션 #${sessionId} 실행 요청됨`);
    },
    [numericWorkspaceId, refreshTaskData, reloadOrchestrations],
  );

  // ── Chat & Plan Handlers ─────────────────────────────────────
  const openOrchestrationPlan = useCallback(
    async (planId: number) => {
      setOrchestrationPlan(null);
      setOrchestrationPlanState({ status: "loading", error: null });
      try {
        const plan = await fetchOrchestrationPlanArtifact(
          resolveAccessToken(),
          numericWorkspaceId,
          planId,
        );
        setOrchestrationPlan(plan);
        setOrchestrationPlanState({ status: "ready", error: null });
      } catch (error) {
        setOrchestrationPlanState({
          status: "error",
          error: messageFromError(error, "오케스트레이션 계획을 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const submitActorChatMessage = async (rawMessage: string, retryBubbleId?: string) => {
    if (!selectedActorNode) {
      return;
    }
    const message = rawMessage.trim();
    if (!message) {
      return;
    }
    if (selectedActorNode.kind !== "agent") {
      setActorChatMessages((current) => [
        ...current,
        { id: `member-${Date.now()}`, sender: "user", text: message, optimistic: false },
      ]);
      setActorChatInput("");
      return;
    }

    const agentId = agentIdFromActorNode(selectedActorNode);
    if (agentId === null) {
      return;
    }

    lastFailedActorMsgRef.current = null;
    const bubbleId = `optimistic-${Date.now()}`;
    const optimisticMessage: ActorChatMessage = {
      id: bubbleId,
      sender: "user",
      text: message,
      optimistic: true,
    };
    setActorChatMessages((current) => [
      ...(retryBubbleId ? current.filter((item) => item.id !== retryBubbleId) : current),
      optimisticMessage,
    ]);
    if (!retryBubbleId) {
      setActorChatInput("");
    }
    setActorChatMutation({ status: "submitting", message: "메시지 전송 중" });

    try {
      const result = await sendWorkspaceChatMessage(resolveAccessToken(), numericWorkspaceId, {
        message,
        agentId,
        taskType: "OTHER",
        priority: "MEDIUM",
        title: `에이전트 대화: ${selectedActorNode.label}`,
        chatSessionId: actorChatSessionId,
      });
      const sessionKey = actorChatSessionKey(selectedActorNode.id);
      if (activeInspectorRef.current.target?.type === "actor" && activeInspectorRef.current.target.nodeId === selectedActorNode.id) {
        activeInspectorRef.current = {
          target: activeInspectorRef.current.target,
          actorSessionId: result.chatSessionId,
        };
      }
      setActorChatSessionId(result.chatSessionId);
      setActorChatSessionIds((current) => {
        const next = { ...current, [sessionKey]: result.chatSessionId };
        if (typeof window !== "undefined") {
          writeActorChatSessionIds(window.localStorage, numericWorkspaceId, next);
        }
        return next;
      });
      setActorChatMessages((current) => [
        ...current.filter((item) => item.id !== bubbleId),
        ...result.messages.map(actorMessageFromChatMessage),
      ]);
      setActorChatMutation({ status: "success", message: `세션 #${result.chatSessionId} 저장됨` });
      await pollActorChatMessages(result.chatSessionId);
      await refreshTaskData();
    } catch (error) {
      lastFailedActorMsgRef.current = { nodeId: selectedActorNode.id, bubbleId, message };
      setActorChatMessages((current) =>
        current.map((item) =>
          item.id === bubbleId
            ? { ...item, optimistic: false, text: `${item.text} (전송 실패)` }
            : item,
        ),
      );
      setActorChatMutation({
        status: "error",
        message: messageFromError(error, "에이전트 대화를 전송하지 못했습니다."),
      });
    }
  };

  const handleSendActorChatMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await submitActorChatMessage(actorChatInput);
  };

  const handleRetryActorChat = () => {
    const failed = lastFailedActorMsgRef.current;
    if (
      !failed ||
      selectedActorNode?.id !== failed.nodeId ||
      actorChatMutation.status === "submitting"
    ) {
      return;
    }
    void submitActorChatMessage(failed.message, failed.bubbleId);
  };

  const submitInspectorTaskChat = async (task: WorkspaceTask, rawMessage: string, isRetry = false) => {
    const message = rawMessage.trim();
    const agentId = parseOptionalPositiveInteger(defaultAgentIdForTask(task, workspaceAgents));
    if (!message || agentId === null) {
      setInspectorTaskChatMutation({
        status: "error",
        message: "메시지 내용과 대상 에이전트를 확인해 주세요.",
      });
      return;
    }

    lastFailedTaskMsgRef.current = null;
    setInspectorTaskChatMutation({ status: "submitting", message: null });
    try {
      const currentSessionId =
        inspectorTaskChatMessages.find((item) => item.chatSessionId)?.chatSessionId ?? null;
      const result = await sendWorkspaceChatMessage(resolveAccessToken(), numericWorkspaceId, {
        message,
        agentId,
        taskType: task.type ?? "OTHER",
        priority: task.priority ?? "MEDIUM",
        title: task.title,
        createPr: false,
        chatSessionId: currentSessionId,
      });
      setInspectorTaskChatMessages((current) => mergeChatMessages(current, result.messages));
      if (!isRetry) {
        setInspectorTaskChatInput("");
      }
      setInspectorTaskChatMutation({ status: "success", message: "전송됨" });
      await refreshTaskData();
    } catch (error) {
      lastFailedTaskMsgRef.current = { taskId: task.taskId, message };
      setInspectorTaskChatMutation({
        status: "error",
        message: messageFromError(error, "작업 대화를 전송하지 못했습니다."),
      });
    }
  };

  const handleSendInspectorTaskChat = async (
    event: FormEvent<HTMLFormElement>,
    task: WorkspaceTask,
  ) => {
    event.preventDefault();
    await submitInspectorTaskChat(task, inspectorTaskChatInput);
  };

  const handleRetryTaskChat = (task: WorkspaceTask) => {
    const failed = lastFailedTaskMsgRef.current;
    if (!failed || failed.taskId !== task.taskId || inspectorTaskChatMutation.status === "submitting") {
      return;
    }
    void submitInspectorTaskChat(task, failed.message, true);
  };

  // ── Task Reports & Task Chat Modals ──────────────────────────
  const openTaskReportsModal = useCallback(
    async (task: WorkspaceTask) => {
      setSelectedReportsTask(task);
      setModalTaskReports([]);
      setModalReportState({ status: "loading", error: null });
      try {
        const reports = await fetchTaskReports(resolveAccessToken(), numericWorkspaceId, task.taskId);
        setModalTaskReports(reports);
        setModalReportState({ status: "ready", error: null });
      } catch (error) {
        setModalReportState({
          status: "error",
          error: messageFromError(error, "리포트를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const openTaskChatModal = useCallback(
    async (task: WorkspaceTask) => {
      setSelectedChatTask(task);
      setModalTaskChatMessages([]);
      setModalTaskChatAgentIdInput(defaultAgentIdForTask(task, workspaceAgents));
      setModalTaskChatInput("");
      setModalTaskChatMutation({ status: "idle", message: null });
      setModalTaskChatSessionId(null);
      setModalTaskChatCursor(null);
      setModalTaskChatHasMore(false);
      setModalTaskChatPaginationState({ status: "idle", message: null });
      setModalTaskChatState({ status: "loading", error: null });

      try {
        const messages = await fetchTaskChatMessages(
          resolveAccessToken(),
          numericWorkspaceId,
          task.taskId,
        );
        const sessionId = messages[0]?.chatSessionId ?? null;
        if (sessionId) {
          const page = await fetchChatSessionMessages(
            resolveAccessToken(),
            numericWorkspaceId,
            sessionId,
            chatSessionMessagePageInput(),
          );
          setModalTaskChatMessages(page.messages);
          setModalTaskChatSessionId(sessionId);
          setModalTaskChatCursor(page.nextCursor);
          setModalTaskChatHasMore(page.hasMore);
        } else {
          setModalTaskChatMessages(messages);
        }
        setModalTaskChatState({ status: "ready", error: null });
      } catch (error) {
        setModalTaskChatState({
          status: "error",
          error: messageFromError(error, "대화 메시지를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId, workspaceAgents],
  );

  const loadMoreModalTaskChatMessages = useCallback(async () => {
    if (!modalTaskChatSessionId || modalTaskChatCursor === null) {
      return;
    }
    setModalTaskChatPaginationState({ status: "loading", message: null });
    try {
      const page = await fetchChatSessionMessages(
        resolveAccessToken(),
        numericWorkspaceId,
        modalTaskChatSessionId,
        chatSessionMessagePageInput(modalTaskChatCursor),
      );
      setModalTaskChatMessages((current) => mergeChatMessages(current, page.messages));
      setModalTaskChatCursor(page.nextCursor);
      setModalTaskChatHasMore(page.hasMore);
      setModalTaskChatPaginationState({ status: "idle", message: null });
    } catch (error) {
      setModalTaskChatPaginationState({
        status: "error",
        message: messageFromError(error, "이전 메시지를 불러오지 못했습니다."),
      });
    }
  }, [modalTaskChatCursor, modalTaskChatSessionId, numericWorkspaceId]);

  const handleSendModalTaskChat = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedChatTask) {
      return;
    }
    const message = modalTaskChatInput.trim();
    const agentId = parseOptionalPositiveInteger(modalTaskChatAgentIdInput);
    if (!message || agentId === null) {
      setModalTaskChatMutation({
        status: "error",
        message: "에이전트와 메시지를 입력해 주세요.",
      });
      return;
    }

    setModalTaskChatMutation({ status: "submitting", message: null });
    try {
      const currentSessionId =
        modalTaskChatSessionId ??
        modalTaskChatMessages.find((item) => item.chatSessionId)?.chatSessionId ??
        null;
      const result = await sendWorkspaceChatMessage(resolveAccessToken(), numericWorkspaceId, {
        message,
        agentId,
        taskType: selectedChatTask.type,
        priority: selectedChatTask.priority,
        title: selectedChatTask.title,
        createPr: false,
        chatSessionId: currentSessionId,
      });
      const nextMessages = mergeChatMessages(modalTaskChatMessages, result.messages);
      setModalTaskChatMessages(nextMessages);
      setModalTaskChatSessionId(result.chatSessionId);
      setModalTaskChatCursor(lastChatMessageId(nextMessages));
      setModalTaskChatHasMore(false);
      setModalTaskChatInput("");
      setModalTaskChatState({ status: "ready", error: null });
      setModalTaskChatMutation({
        status: "success",
        message: `작업 #${result.taskId} 대화 메시지가 전송되었습니다.`,
      });
      if (result.orchestrationPlanId) {
        await openOrchestrationPlan(result.orchestrationPlanId);
      }
      await refreshTaskData();
    } catch (error) {
      setModalTaskChatMutation({
        status: "error",
        message: messageFromError(error, "대화 메시지를 전송하지 못했습니다."),
      });
    }
  };

  // ── Connections Modal Loaders & Handlers ─────────────────────
  const loadSlackConnections = useCallback(async () => {
    setSlackConnectionState({ status: "loading", integrations: [], error: null });
    setSlackConnectionMutation({ status: "idle", message: null });
    try {
      const integrations = await fetchSlackIntegrations(resolveAccessToken(), numericWorkspaceId);
      if (typeof window !== "undefined") {
        if (integrations[0]) {
          writeStoredSlackIntegration(window.localStorage, numericWorkspaceId, integrations[0]);
        } else {
          deleteStoredSlackIntegration(window.localStorage, numericWorkspaceId);
        }
      }
      setSlackConnectionState({ status: "ready", integrations, error: null });
    } catch (error) {
      setSlackConnectionState({
        status: "error",
        integrations: [],
        error: messageFromError(error, "Slack 연동 목록을 확인하지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const loadGithubCredentials = useCallback(async () => {
    setGithubConnectionState({ status: "loading", credentials: [], error: null });
    setGithubConnectionCreateMutation({ status: "idle", message: null });
    setGithubConnectionActionMutation({ status: "idle", message: null });
    try {
      const credentials = await fetchGithubCredentials(resolveAccessToken(), numericWorkspaceId);
      if (typeof window !== "undefined") {
        if (credentials[0]) {
          writeStoredGithubCredential(window.localStorage, numericWorkspaceId, credentials[0]);
        } else {
          deleteStoredGithubCredential(window.localStorage, numericWorkspaceId);
        }
      }
      setGithubConnectionState({ status: "ready", credentials, error: null });
      setSelectedGithubCredentialId((current) =>
        current && credentials.some((c) => c.credentialId === current)
          ? current
          : credentials[0]?.credentialId ?? null,
      );
    } catch (error) {
      setGithubConnectionState({
        status: "error",
        credentials: [],
        error: messageFromError(error, "GitHub 자격 증명 목록을 확인하지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const canManage = state.workspace?.myRole === "ADMIN";
  const modelProvidersController = useWorkspaceModelProviders({
    workspaceId: isValidWorkspaceId(numericWorkspaceId) ? numericWorkspaceId : null,
    canManage,
    enabled: state.status === "ready",
  });
  const reloadModelProviders = modelProvidersController.reload;

  const openConnectionModal = useCallback(
    (kind: Exclude<ConnectionModalKind, null>) => {
      setConnectionModal(kind);
      if (kind === "slack") void loadSlackConnections();
      if (kind === "github") void loadGithubCredentials();
      if (kind === "gateway") void loadGatewayConnection();
      if (kind === "models") void reloadModelProviders();
    },
    [loadGatewayConnection, loadGithubCredentials, loadSlackConnections, reloadModelProviders],
  );
  const githubConnectionCredentials =
    githubConnectionState.status === "ready" || githubConnectionState.status === "error"
      ? githubConnectionState.credentials
      : EMPTY_GITHUB_CREDENTIALS;
  const slackConnectionIntegrations =
    slackConnectionState.status === "ready" || slackConnectionState.status === "error"
      ? slackConnectionState.integrations
      : EMPTY_SLACK_INTEGRATIONS;
  const selectedGithubCredential =
    githubConnectionCredentials.find((c) => c.credentialId === selectedGithubCredentialId) ??
    githubConnectionCredentials[0] ??
    null;

  const normalizedConnectionGatewayForm = useMemo(
    () =>
      normalizeGatewayForm({
        gatewayUrl: gatewayUrlInput,
        token: gatewayTokenInput,
        validateConnection: true,
      }),
    [gatewayTokenInput, gatewayUrlInput],
  );

  // ── Presence Position Sync ───────────────────────────────────
  const defaultPlayerPosition = useMemo<StageActorPosition>(
    () => ({ left: "36.8%", top: "54%" }),
    [],
  );
  const lastValidPresencePositionRef = useRef<WorkspacePresencePosition>(defaultPlayerPosition);
  const presenceSuccessMessageRef = useRef("참여자 위치가 갱신되었습니다.");
  const presenceSchedulerRef = useRef<{
    workspaceId: number;
    scheduler: ReturnType<typeof createPresencePositionScheduler>;
  } | null>(null);

  const updatePresencePosition = useCallback(
    async (
      position: WorkspacePresencePosition,
      successMessage = "참여자 위치가 갱신되었습니다.",
    ) => {
      if (!isValidWorkspaceId(numericWorkspaceId)) {
        return;
      }
      const validPosition =
        sanitizeWorkspacePresencePosition(position) ?? lastValidPresencePositionRef.current;
      lastValidPresencePositionRef.current = validPosition;
      presenceSuccessMessageRef.current = successMessage;

      if (
        !presenceSchedulerRef.current ||
        presenceSchedulerRef.current.workspaceId !== numericWorkspaceId
      ) {
        const targetWorkspaceId = numericWorkspaceId;
        presenceSchedulerRef.current = {
          workspaceId: targetWorkspaceId,
          scheduler: createPresencePositionScheduler(async (nextPosition) => {
            setPresenceMutation({ status: "submitting", message: null });
            try {
              await updateWorkspacePresencePosition(resolveAccessToken(), targetWorkspaceId, {
                position: nextPosition,
                status: "ACTIVE",
              });
              setPresenceMutation({
                status: "success",
                message: presenceSuccessMessageRef.current,
              });
            } catch (error) {
              setPresenceMutation({
                status: "error",
                message: messageFromError(error, "참여자 위치를 갱신하지 못했습니다."),
              });
            }
          }),
        };
      }
      await presenceSchedulerRef.current.scheduler.schedule(validPosition);
    },
    [numericWorkspaceId],
  );

  const handlePlayerPositionChange = useCallback(
    (position: StageActorPosition) => {
      const validPosition =
        sanitizeWorkspacePresencePosition(position) ?? lastValidPresencePositionRef.current;
      lastValidPresencePositionRef.current = validPosition;
      setPlayerPositionOverride(validPosition);
      void updatePresencePosition(validPosition, "2D 오피스 위치가 갱신되었습니다.");
    },
    [updatePresencePosition],
  );

  // ── Derived View Props ───────────────────────────────────────
  const workspace = state.workspace;
  const members = state.status === "ready" ? state.members : EMPTY_WORKSPACE_MEMBERS;
  const currentMemberId = useMemo(() => readAuthUser()?.memberId ?? null, []);
  const storedCharacterProfile = useMemo(() => loadStoredCharacterProfile(), []);

  const presenceConnectionState: PresenceConnectionState =
    presenceStreamStatus === "open"
      ? "online"
      : presenceStreamStatus === "connecting"
        ? "connecting"
        : presenceStreamStatus === "error"
          ? "error"
          : "idle";

  const presenceLiveState = useMemo(
    () =>
      createPresenceLiveState({
        streamStatus: presenceStreamStatus,
        workspaceMemberCount: members.length,
        events: presenceEvents,
        persistedMembers: presenceMembers,
      }),
    [members.length, presenceEvents, presenceMembers, presenceStreamStatus],
  );

  const onlinePresenceMembers = useMemo(
    () => presenceLiveState.members.filter((m) => m.status === "ACTIVE" || !m.status),
    [presenceLiveState.members],
  );

  const localSummary = useMemo(
    () => createLocalDashboardSummary(workspaceAgents, tasks),
    [tasks, workspaceAgents],
  );
  const waitingApprovalCount = useMemo(() => {
    let count = 0;
    for (let i = 0; i < tasks.length; i += 1) {
      if (tasks[i].status === "WAITING_USER") {
        count += 1;
      }
    }
    return count;
  }, [tasks]);

  const rawSummary = dashboardSummary.summary ?? localSummary;
  const taskSummary = useMemo(
    () => ({
      ...rawSummary,
      waitingApprovalTaskCount: rawSummary.waitingApprovalTaskCount ?? waitingApprovalCount,
      todayUsedTokens: rawSummary.todayUsedTokens ?? workspace?.todayUsedTokens ?? 0,
      dailyTokenLimit: rawSummary.dailyTokenLimit ?? workspace?.dailyTokenLimit ?? null,
    }),
    [rawSummary, waitingApprovalCount, workspace?.dailyTokenLimit, workspace?.todayUsedTokens],
  );

  const selectedInspectorAgent = useMemo(() => {
    if (inspectorTarget?.type !== "actor" || !selectedActorNode) {
      return null;
    }
    const id = agentIdFromActorNode(selectedActorNode);
    return id ? workspaceAgents.find((a) => a.agentId === id) ?? null : null;
  }, [inspectorTarget, selectedActorNode, workspaceAgents]);

  const selectedInspectorTask = useMemo(() => {
    if (inspectorTarget?.type !== "task") {
      return null;
    }
    return tasks.find((t) => t.taskId === inspectorTarget.taskId) ?? null;
  }, [inspectorTarget, tasks]);

  const handleSelectView = useCallback(
    (nextView: WorkspaceViewId) => {
      setActiveView(nextView);
      setMobileSidebarOpen(false);
      if (nextView === "orchestrations") {
        void reloadOrchestrations();
      }
      if (nextView === "artifacts" && !artifactTree && artifactTreeState.status === "idle") {
        void loadArtifactTreeData();
      }
    },
    [artifactTree, artifactTreeState.status, loadArtifactTreeData, reloadOrchestrations],
  );

  const handleFocusZone = useCallback(
    (zone: OfficeZoneId) => {
      setActiveZone(zone);
      setActiveView("office");
      setMobileSidebarOpen(false);

      if (zone === "overview") {
        setCameraFocusTarget((prev) => ({
          position: defaultPlayerPosition,
          nonce: (prev?.nonce ?? 0) + 1,
        }));
        return;
      }

      const engRows = getEngineeringRows(workspaceAgents.length);
      const worldSize = getStageWorldSize(engRows);
      const rooms = getStageRooms(engRows, worldSize, workspaceAgents.length);
      const matched = rooms.find((room) => room.variant === zone) ?? rooms[0];
      if (matched) {
        setCameraFocusTarget((prev) => ({
          position: {
            left: toStagePct(parseStagePct(matched.left) + parseStagePct(matched.width) / 2),
            top: toStagePct(parseStagePct(matched.top) + parseStagePct(matched.height) / 2),
          },
          nonce: (prev?.nonce ?? 0) + 1,
        }));
      }
    },
    [defaultPlayerPosition, workspaceAgents.length],
  );

  const handleToggleTheme = useCallback(() => {
    setThemeMode((current) => toggleTheme(current));
  }, []);

  const workspaceCommandItems = useWorkspaceCommandItems({
    agents: workspaceAgents,
    members,
    presenceMembers: onlinePresenceMembers,
    tasks,
    sidebarCollapsed,
    themeMode,
    onSelectView: handleSelectView,
    onOpenCreateTask: () => {
      setTaskCreateDefaultAgentId(null);
      setTaskCreateMutation({ status: "idle", message: null, agentId: null });
      setTaskCreateOpen(true);
    },
    onOpenHireAgent: () => setAgentHireOpen(true),
    onOpenConnections: () => openConnectionModal("selector"),
    onToggleSidebar: toggleSidebar,
    onToggleTheme: handleToggleTheme,
    onOpenShortcuts: () => setShortcutsOpen(true),
    onSelectOfficeView: () => setActiveView("office"),
    onFocusCamera: (position) =>
      setCameraFocusTarget((prev) => ({
        position,
        nonce: (prev?.nonce ?? 0) + 1,
      })),
    onSelectActor: handleSelectActor,
  });

  const hasActiveExecution = useMemo(
    () =>
      tasks.some((task) => task.status === "IN_PROGRESS") ||
      orchestrationSessions.some((session) => session.status === "RUNNING"),
    [orchestrationSessions, tasks],
  );

  const hasOtherOverlay = Boolean(
    modalArtifactPath ||
      selectedChatTask ||
      selectedReportsTask ||
      orchestrationPlan ||
      taskCreateOpen ||
      agentHireOpen ||
      connectionModal,
  );

  const handleSummarySynced = useCallback((summary: WorkspaceDashboardSummary) => {
    setDashboardSummary({ status: "ready", summary, error: null });
  }, []);

  const handleCloseInspector = useCallback(() => {
    activeInspectorRef.current = { target: null, actorSessionId: null };
    setInspectorTarget(null);
    setMobileInspectorOpen(false);
  }, []);

  useWorkspaceLiveSync({
    enabled: state.status === "ready" && isValidWorkspaceId(numericWorkspaceId),
    workspaceId: numericWorkspaceId,
    hasActiveExecution,
    inspectorTarget,
    actorNodeId: selectedActorNode?.id ?? null,
    actorChatSessionId,
    shortcutStyle,
    viewOrder: VIEW_ORDER,
    commandPaletteOpen,
    shortcutsOpen,
    hasOtherOverlay,
    onSelectView: handleSelectView,
    onSetCommandPaletteOpen: setCommandPaletteOpen,
    onSetShortcutsOpen: setShortcutsOpen,
    onCloseInspector: handleCloseInspector,
    onTasksUpdated: setTasks,
    onSummaryUpdated: handleSummarySynced,
    onPollActorChatSilent: pollActorChatSilent,
    onRefreshInspectorTaskSilent: refreshInspectorTaskDetailsSilent,
    onRefreshOrchestrationsSilent: refreshOrchestrationsSilent,
  });

  // ── Loading / Error Fallback Routes ──────────────────────────
  if (state.status === "loading") {
    return (
      <WorkspaceLoadingState
        title="워크스페이스 불러오는 중"
        message="워크스페이스 구성과 실시간 연결 정보를 확인하고 있습니다."
        backHref="/workspaces"
        backLabel="목록으로"
      />
    );
  }

  if (state.status === "error" || state.status === "offline" || !workspace) {
    return (
      <WorkspaceErrorState
        title={state.status === "offline" ? "연결할 수 없음" : "워크스페이스 조회 실패"}
        message={state.error ?? "워크스페이스 정보를 확인할 수 없습니다."}
        backHref="/workspaces"
        backLabel="목록으로"
        onRetry={() => void reloadWorkspace()}
      />
    );
  }

  // ── Ready: 5-Region Desktop Chrome ───────────────────────────
  return (
    <div className="chrome-workspace-shell">
      <WorkspaceChromeHeader
        workspaceId={numericWorkspaceId}
        workspaceName={workspace.name}
        myRole={workspace.myRole}
        activeView={activeView}
        waitingApprovalCount={waitingApprovalCount}
        taskCount={tasks.length}
        theme={themeMode}
        canManage={canManage}
        isRefreshing={isRefreshingAll}
        isRecoveringStale={recoveringStale}
        hasInspector={Boolean(inspectorTarget)}
        mobileSidebarOpen={mobileSidebarOpen}
        sidebarCollapsed={sidebarCollapsed}
        shortcutStyle={shortcutStyle}
        onSelectView={handleSelectView}
        onOpenTaskCreate={() => {
          setTaskCreateDefaultAgentId(null);
          setTaskCreateMutation({ status: "idle", message: null, agentId: null });
          setTaskCreateOpen(true);
        }}
        onOpenHireAgent={() => {
          setAgentMutation({ status: "idle", message: null, agentId: null });
          setAgentHireOpen(true);
        }}
        onOpenConnections={() => openConnectionModal("selector")}
        onOpenCommandPalette={() => setCommandPaletteOpen(true)}
        onOpenShortcuts={() => setShortcutsOpen(true)}
        onRefreshWorkspace={() => void reloadWorkspace()}
        onRecoverStaleTasks={() => void handleRecoverStaleTasks()}
        onToggleTheme={handleToggleTheme}
        onToggleMobileSidebar={() => setMobileSidebarOpen((open) => !open)}
        onToggleMobileInspector={() => setMobileInspectorOpen((open) => !open)}
        onToggleSidebar={toggleSidebar}
      />

      <div className="chrome-body">
        <WorkspaceLeftSidebar
          agents={workspaceAgents}
          members={members}
          tasks={tasks}
          selectedAgentId={selectedInspectorAgent?.agentId ?? null}
          activeZone={activeZone}
          mobileOpen={mobileSidebarOpen}
          desktopCollapsed={sidebarCollapsed}
          widthPx={sidebarWidth}
          onWidthChange={setSidebarWidth}
          onWidthReset={resetSidebarWidth}
          currentMemberId={currentMemberId}
          playerAvatar={storedCharacterProfile}
          canManage={canManage}
          presenceLiveState={presenceLiveState}
          presenceEvents={presenceEvents}
          presenceMutation={presenceMutation}
          isUpdatingPresence={presenceMutation.status === "submitting"}
          onFocusZone={handleFocusZone}
          onSelectAgent={handleSelectAgentFromSidebar}
          onOpenHireAgent={() => {
            setAgentMutation({ status: "idle", message: null, agentId: null });
            setAgentHireOpen(true);
          }}
          onUpdatePresencePosition={() =>
            void updatePresencePosition(lastValidPresencePositionRef.current)
          }
        />

        <main className="chrome-viewport" aria-label="워크스페이스 작업 영역">
          {activeView === "office" ? (
            <section
              className="workspace-office-scene workspace-2d-section workspace-stage-surface"
              aria-label="워크스페이스 오피스 씬"
            >
              <Workspace2DCanvas
                agents={workspaceAgents}
                members={members}
                presenceMembers={onlinePresenceMembers}
                tasks={tasks}
                variant="fullscreen"
                playerPosition={playerPositionOverride ?? defaultPlayerPosition}
                playerAvatar={storedCharacterProfile}
                playerMemberId={currentMemberId}
                cameraFocusTarget={cameraFocusTarget}
                onPlayerPositionChange={handlePlayerPositionChange}
                onActorSelect={handleSelectActor}
                onOpenTasks={() => handleSelectView("tasks")}
                onOpenOrchestration={() => handleSelectView("orchestrations")}
                onOpenArtifacts={() => handleSelectView("artifacts")}
                onPresenceMove={(position) =>
                  updatePresencePosition(position, "2D 오피스 위치가 갱신되었습니다.")
                }
              />
            </section>
          ) : null}

          {activeView === "tasks" ? (
            <WorkspaceTaskBoardView
              tasks={tasks}
              agents={workspaceAgents}
              summary={taskSummary}
              selectedTaskId={selectedInspectorTask?.taskId ?? null}
              searchQuery={taskSearchQuery}
              statusFilter={taskStatusFilter}
              layoutMode={taskLayoutMode}
              canManage={canManage}
              mutatingTaskId={mutatingTaskId}
              recoveringStale={recoveringStale}
              staleRecoveryNotice={staleRecoveryNotice}
              onSearchQueryChange={setTaskSearchQuery}
              onStatusFilterChange={setTaskStatusFilter}
              onLayoutModeChange={setTaskLayoutMode}
              onOpenCreateModal={() => {
                setTaskCreateDefaultAgentId(null);
                setTaskCreateMutation({ status: "idle", message: null, agentId: null });
                setTaskCreateOpen(true);
              }}
              onRecoverStaleTasks={() => void handleRecoverStaleTasks()}
              onSelectTask={openTaskInspector}
              onRunTask={(task) => void handleRunTask(task)}
              onRequestApproval={(task) => void handleRequestTaskApproval(task)}
              onCancelTask={(task) => void handleCancelTask(task)}
              onUpdateTaskStatus={(taskId, status) => void handleUpdateTaskStatus(taskId, status)}
            />
          ) : null}

          {activeView === "approvals" ? (
            <WorkspaceApprovalInboxView
              tasks={tasks}
              agents={workspaceAgents}
              canManage={canManage}
              onSelectTask={openTaskInspector}
              onOpenReports={(task) => void openTaskReportsModal(task)}
              onReviewApproval={handleReviewApproval}
            />
          ) : null}

          {activeView === "orchestrations" ? (
            <WorkspaceOrchestrationView
              tasks={tasks}
              agents={workspaceAgents}
              sessions={orchestrationSessions}
              sessionsState={orchestrationSessionsState}
              selectedSession={selectedOrchestrationSession}
              canManage={Boolean(workspace)}
              onRefreshSessions={() => void reloadOrchestrations()}
              onSelectSession={(id) => void handleSelectOrchestrationSession(id)}
              onCreateSession={handleCreateOrchestrationSession}
              onDispatchSession={handleDispatchOrchestrationSession}
              onInspectTask={openTaskInspector}
            />
          ) : null}

          {activeView === "artifacts" ? (
            <WorkspaceArtifactsAndDiffView
              tree={artifactTree}
              treeState={artifactTreeState}
              selectedPath={selectedArtifactPath}
              selectedFile={selectedArtifactFile}
              fileState={selectedArtifactFileState}
              agents={workspaceAgents}
              onReloadTree={() => void loadArtifactTreeData()}
              onSelectFile={(path) => void selectInlineArtifactFile(path)}
            />
          ) : null}
        </main>

        <WorkspaceContextInspector
          target={inspectorTarget}
          selectedActorNode={selectedActorNode}
          selectedAgent={selectedInspectorAgent}
          selectedTask={selectedInspectorTask}
          agents={workspaceAgents}
          modelProviders={modelProvidersController.providers}
          tasks={tasks}
          canManage={canManage}
          mobileOpen={mobileInspectorOpen}
          widthPx={inspectorWidth}
          actorChatMessages={actorChatMessages}
          actorChatInput={actorChatInput}
          actorChatMutation={actorChatMutation}
          canSendActorChat={
            selectedActorNode !== null &&
            actorChatInput.trim().length > 0 &&
            actorChatMutation.status !== "submitting"
          }
          agentDeleteConfirmId={agentDeleteConfirmId}
          agentMutation={agentMutation}
          taskLogs={inspectorTaskLogs}
          taskLogsState={inspectorTaskLogsState}
          taskReports={inspectorTaskReports}
          taskReportsState={inspectorTaskReportsState}
          taskChatMessages={inspectorTaskChatMessages}
          taskChatState={inspectorTaskChatState}
          taskChatInput={inspectorTaskChatInput}
          taskChatMutation={inspectorTaskChatMutation}
          mutatingTaskId={mutatingTaskId}
          onClose={() => {
            setInspectorTarget(null);
            setMobileInspectorOpen(false);
          }}
          onWidthChange={setInspectorWidth}
          onWidthReset={resetInspectorWidth}
          onActorChatInputChange={setActorChatInput}
          onSendActorChat={(event) => void handleSendActorChatMessage(event)}
          onOpenPlanArtifact={(planId) => void openOrchestrationPlan(planId)}
          onInspectTask={openTaskInspector}
          onOpenCreateTaskForAgent={(agentId) => {
            setTaskCreateDefaultAgentId(agentId);
            setTaskCreateMutation({ status: "idle", message: null, agentId: null });
            setTaskCreateOpen(true);
          }}
          onOpenTaskReportsModal={(task) => void openTaskReportsModal(task)}
          onOpenTaskChatModal={(task) => void openTaskChatModal(task)}
          onUpdateAgent={handleUpdateAgent}
          onRequestDeleteAgent={setAgentDeleteConfirmId}
          onConfirmDeleteAgent={(agent) => void handleConfirmDeleteAgent(agent)}
          onReloadTaskLogs={(taskId) => void loadInspectorTaskDetails(taskId)}
          onReloadTaskReports={(taskId) => void loadInspectorTaskDetails(taskId)}
          onReloadTaskChat={(task) => void loadInspectorTaskDetails(task.taskId)}
          onTaskChatInputChange={setInspectorTaskChatInput}
          onSendTaskChat={(event, task) => void handleSendInspectorTaskChat(event, task)}
          onUpdateTaskStatus={(taskId, status) => void handleUpdateTaskStatus(taskId, status)}
          onRunTask={(task) => void handleRunTask(task)}
          onRequestTaskApproval={(task) => void handleRequestTaskApproval(task)}
          onCancelTask={(task) => void handleCancelTask(task)}
          onOpenArtifactPath={(path, name) => void openModalArtifactFile(path, name)}
          onRetryActorChat={handleRetryActorChat}
          onRetryTaskChat={handleRetryTaskChat}
        />
      </div>

      <WorkspaceStatusBar
        presenceConnectionState={presenceConnectionState}
        onlineCount={presenceLiveState.onlineCount}
        gatewayState={gatewayConnectionState}
        todayUsedTokens={taskSummary.todayUsedTokens}
        dailyTokenLimit={taskSummary.dailyTokenLimit ?? workspace.dailyTokenLimit}
        statusNotice={statusNotice}
      />

      <AgentHireModal
        open={agentHireOpen}
        form={agentCreateForm}
        modelProviders={modelProvidersController.providers}
        mutation={agentMutation}
        submitting={agentMutation.status === "submitting"}
        canSubmit={agentCreateForm.name.trim().length > 0 && agentMutation.status !== "submitting"}
        onClose={() => setAgentHireOpen(false)}
        onSubmit={handleCreateAgent}
        onNameChange={(name) => setAgentCreateForm((c) => ({ ...c, name }))}
        onCategoryChange={(category: WorkspaceAgentCategory) =>
          setAgentCreateForm((c) => ({ ...c, category }))
        }
        onWorkspacePathChange={(workspacePath) =>
          setAgentCreateForm((c) => ({ ...c, workspacePath }))
        }
        onEmojiChange={(emoji) => setAgentCreateForm((c) => ({ ...c, emoji }))}
        onModelProviderChange={(modelProviderId) =>
          setAgentCreateForm((c) => ({ ...c, modelProviderId }))
        }
        onModelIdChange={(modelId) => setAgentCreateForm((c) => ({ ...c, modelId }))}
        onFallbackModelIdChange={(fallbackModelId) =>
          setAgentCreateForm((c) => ({ ...c, fallbackModelId }))
        }
        onSkillFileAdd={() =>
          setAgentCreateForm((c) => ({
            ...c,
            skillFiles: [...c.skillFiles, { id: createLocalId(), fileName: "", content: "" }],
          }))
        }
        onSkillFileChange={(id, field, value) =>
          setAgentCreateForm((c) => ({
            ...c,
            skillFiles: c.skillFiles.map((f) => (f.id === id ? { ...f, [field]: value } : f)),
          }))
        }
        onSkillFileRemove={(id) =>
          setAgentCreateForm((c) => ({
            ...c,
            skillFiles: c.skillFiles.filter((f) => f.id !== id),
          }))
        }
      />

      <TaskCreateModal
        key={`task-create-${taskCreateOpen}-${taskCreateDefaultAgentId ?? "none"}`}
        open={taskCreateOpen}
        agents={workspaceAgents}
        initialAgentId={taskCreateDefaultAgentId}
        mutation={taskCreateMutation}
        submitting={taskCreateMutation.status === "submitting"}
        onClose={() => setTaskCreateOpen(false)}
        onCreate={handleCreateTask}
      />

      <ConnectionSettingsModal
        modal={connectionModal}
        canManage={canManage}
        slackState={slackConnectionState}
        slackIntegrations={slackConnectionIntegrations}
        slackMutation={slackConnectionMutation}
        slackAction={slackConnectionAction}
        githubState={githubConnectionState}
        githubCredentials={githubConnectionCredentials}
        selectedGithubCredential={selectedGithubCredential}
        selectedGithubCredentialId={selectedGithubCredentialId}
        editingGithubCredentialId={editingGithubCredentialId}
        githubDisplayNameInput={githubDisplayNameInput}
        githubTokenInput={githubTokenInput}
        githubEditDisplayNameInput={githubEditDisplayNameInput}
        githubEditTokenInput={githubEditTokenInput}
        githubCreateMutation={githubConnectionCreateMutation}
        githubActionMutation={githubConnectionActionMutation}
        githubAction={githubConnectionAction}
        canCreateGithubCredential={
          canManage &&
          githubDisplayNameInput.trim().length > 0 &&
          githubTokenInput.trim().length > 0 &&
          githubConnectionCreateMutation.status !== "submitting" &&
          githubConnectionAction === null
        }
        canUpdateGithubCredential={
          canManage &&
          selectedGithubCredential !== null &&
          githubEditDisplayNameInput.trim().length > 0 &&
          githubConnectionAction === null
        }
        gatewayState={gatewayConnectionState}
        gatewayUrlInput={gatewayUrlInput}
        gatewayTokenInput={gatewayTokenInput}
        gatewayTestState={gatewayConnectionTestState}
        gatewayBindingMutation={gatewayBindingMutation}
        canSubmitGateway={
          canManage &&
          normalizedConnectionGatewayForm.isValid &&
          gatewayConnectionTestState.status !== "submitting" &&
          gatewayBindingMutation.status !== "submitting"
        }
        modelProvidersController={modelProvidersController}
        onOpen={(kind) => openConnectionModal(kind)}
        onClose={() => setConnectionModal(null)}
        onBack={() => setConnectionModal("selector")}
        onReloadSlack={() => void loadSlackConnections()}
        onOpenSlackInstall={async () => {
          if (!canManage) return;
          setSlackConnectionMutation({ status: "submitting", message: "Slack 설치 URL 확인 중" });
          if (typeof window !== "undefined") {
            saveSlackPendingConnectionState(window.localStorage, numericWorkspaceId);
          }
          try {
            const installUrl = await fetchSlackInstallUrl(resolveAccessToken(), numericWorkspaceId);
            if (typeof window !== "undefined") {
              window.open(installUrl.url, "_blank", "noopener,noreferrer");
            }
            setSlackConnectionMutation({
              status: "success",
              message: "Slack 설치 페이지를 열었습니다.",
            });
          } catch (error) {
            setSlackConnectionMutation({
              status: "error",
              message: messageFromError(error, "Slack 설치 URL을 확인하지 못했습니다."),
            });
          }
        }}
        onDeleteSlack={async (integration) => {
          if (!canManage || slackConnectionAction) return;
          setSlackConnectionAction({ type: "delete", integrationId: integration.id });
          try {
            await deleteSlackIntegration(resolveAccessToken(), numericWorkspaceId, integration.id);
            const next = slackConnectionIntegrations.filter((item) => item.id !== integration.id);
            if (typeof window !== "undefined") {
              if (next[0]) {
                writeStoredSlackIntegration(window.localStorage, numericWorkspaceId, next[0]);
              } else {
                deleteStoredSlackIntegration(window.localStorage, numericWorkspaceId);
              }
            }
            setSlackConnectionState({ status: "ready", integrations: next, error: null });
            setSlackConnectionMutation({ status: "success", message: "Slack 연동이 삭제되었습니다." });
          } catch (error) {
            setSlackConnectionMutation({
              status: "error",
              message: messageFromError(error, "Slack 연동을 삭제하지 못했습니다."),
            });
          } finally {
            setSlackConnectionAction(null);
          }
        }}
        onReloadGithub={() => void loadGithubCredentials()}
        onCreateGithub={async (event) => {
          event.preventDefault();
          if (!canManage) return;
          setGithubConnectionCreateMutation({ status: "submitting", message: "등록 중" });
          try {
            const created = await createGithubCredential(resolveAccessToken(), numericWorkspaceId, {
              displayName: githubDisplayNameInput.trim(),
              token: githubTokenInput.trim(),
            });
            setGithubConnectionState((prev) =>
              prev.status === "ready"
                ? { ...prev, credentials: [created, ...prev.credentials] }
                : { status: "ready", credentials: [created], error: null },
            );
            setSelectedGithubCredentialId(created.credentialId);
            if (typeof window !== "undefined") {
              writeStoredGithubCredential(window.localStorage, numericWorkspaceId, created);
            }
            setGithubDisplayNameInput("");
            setGithubTokenInput("");
            setGithubConnectionCreateMutation({
              status: "success",
              message: "GitHub 자격 증명이 등록되었습니다.",
            });
          } catch (error) {
            setGithubConnectionCreateMutation({
              status: "error",
              message: messageFromError(error, "GitHub 자격 증명을 등록하지 못했습니다."),
            });
          }
        }}
        onSelectGithubCredential={setSelectedGithubCredentialId}
        onStartGithubEdit={(credential) => {
          setSelectedGithubCredentialId(credential.credentialId);
          setEditingGithubCredentialId(credential.credentialId);
          setGithubEditDisplayNameInput(credential.displayName);
          setGithubEditTokenInput("");
        }}
        onCancelGithubEdit={() => {
          setEditingGithubCredentialId(null);
          setGithubEditDisplayNameInput("");
          setGithubEditTokenInput("");
        }}
        onUpdateGithub={async (event, credential) => {
          event.preventDefault();
          if (!canManage) return;
          setGithubConnectionAction({ type: "update", credentialId: credential.credentialId });
          try {
            const updated = await updateGithubCredential(
              resolveAccessToken(),
              numericWorkspaceId,
              credential.credentialId,
              {
                displayName: githubEditDisplayNameInput.trim(),
                token: githubEditTokenInput.trim() || undefined,
              },
            );
            setGithubConnectionState((prev) =>
              prev.status === "ready"
                ? {
                    ...prev,
                    credentials: prev.credentials.map((c) =>
                      c.credentialId === updated.credentialId ? updated : c,
                    ),
                  }
                : prev,
            );
            setEditingGithubCredentialId(null);
            if (typeof window !== "undefined") {
              writeStoredGithubCredential(window.localStorage, numericWorkspaceId, updated);
            }
            setGithubConnectionActionMutation({
              status: "success",
              message: "GitHub 자격 증명이 수정되었습니다.",
            });
          } catch (error) {
            setGithubConnectionActionMutation({
              status: "error",
              message: messageFromError(error, "GitHub 자격 증명을 수정하지 못했습니다."),
            });
          } finally {
            setGithubConnectionAction(null);
          }
        }}
        onDeleteGithub={async (credential) => {
          if (!canManage) return;
          setGithubConnectionAction({ type: "delete", credentialId: credential.credentialId });
          try {
            await deleteGithubCredential(
              resolveAccessToken(),
              numericWorkspaceId,
              credential.credentialId,
            );
            const next = githubConnectionCredentials.filter(
              (c) => c.credentialId !== credential.credentialId,
            );
            if (typeof window !== "undefined") {
              if (next[0]) {
                writeStoredGithubCredential(window.localStorage, numericWorkspaceId, next[0]);
              } else {
                deleteStoredGithubCredential(window.localStorage, numericWorkspaceId);
              }
            }
            setGithubConnectionState({ status: "ready", credentials: next, error: null });
            setSelectedGithubCredentialId(next[0]?.credentialId ?? null);
            setGithubConnectionActionMutation({
              status: "success",
              message: "GitHub 자격 증명이 삭제되었습니다.",
            });
          } catch (error) {
            setGithubConnectionActionMutation({
              status: "error",
              message: messageFromError(error, "GitHub 자격 증명을 삭제하지 못했습니다."),
            });
          } finally {
            setGithubConnectionAction(null);
          }
        }}
        onGithubDisplayNameChange={setGithubDisplayNameInput}
        onGithubTokenChange={setGithubTokenInput}
        onGithubEditDisplayNameChange={setGithubEditDisplayNameInput}
        onGithubEditTokenChange={setGithubEditTokenInput}
        onReloadGateway={() => void loadGatewayConnection()}
        onGatewayUrlChange={setGatewayUrlInput}
        onGatewayTokenChange={setGatewayTokenInput}
        onTestGateway={async () => {
          if (!canManage || !normalizedConnectionGatewayForm.isValid) return;
          setGatewayConnectionTestState({ status: "submitting", result: null, message: null });
          try {
            const result = await testWorkspaceGatewayConnection(
              resolveAccessToken(),
              numericWorkspaceId,
              {
                gatewayUrl: normalizedConnectionGatewayForm.gatewayUrl,
                token: normalizedConnectionGatewayForm.token,
              },
            );
            setGatewayConnectionTestState({
              status: "success",
              result,
              message: result.message || gatewayConnectionStatusLabel(result.status),
            });
          } catch (error) {
            setGatewayConnectionTestState({
              status: "error",
              result: null,
              message: messageFromError(error, "게이트웨이 연결 테스트에 실패했습니다."),
            });
          }
        }}
        onBindGateway={async (event) => {
          event.preventDefault();
          if (!canManage || !normalizedConnectionGatewayForm.isValid) return;
          setGatewayBindingMutation({ status: "submitting", message: "연결 등록 중" });
          try {
            const binding = await createWorkspaceGatewayBinding(
              resolveAccessToken(),
              numericWorkspaceId,
              {
                gatewayUrl: normalizedConnectionGatewayForm.gatewayUrl,
                token: normalizedConnectionGatewayForm.token,
                validateConnection: normalizedConnectionGatewayForm.validateConnection,
              },
            );
            const gateway = gatewayBindingToStatus(binding);
            if (typeof window !== "undefined") {
              if (gateway.bound) {
                writeStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId, gateway);
              } else {
                deleteStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId);
              }
            }
            setGatewayConnectionState({ status: "ready", gateway, error: null });
            setGatewayTokenInput("");
            setGatewayBindingMutation({
              status: "success",
              message: "OpenClaw 게이트웨이 연결을 저장했습니다.",
            });
          } catch (error) {
            setGatewayBindingMutation({
              status: "error",
              message: messageFromError(error, "OpenClaw 게이트웨이 연결을 저장하지 못했습니다."),
            });
          }
        }}
      />

      <TaskReportsModal
        task={selectedReportsTask}
        reports={modalTaskReports}
        state={modalReportState}
        onClose={() => {
          setSelectedReportsTask(null);
          setModalTaskReports([]);
          setModalReportState({ status: "idle", error: null });
        }}
        onOpenArtifact={(path, name) => void openModalArtifactFile(path, name)}
      />

      <TaskChatModal
        task={selectedChatTask}
        messages={modalTaskChatMessages}
        state={modalTaskChatState}
        messageInput={modalTaskChatInput}
        agentIdInput={modalTaskChatAgentIdInput}
        agents={workspaceAgents}
        mutation={modalTaskChatMutation}
        canSend={
          Boolean(selectedChatTask) &&
          modalTaskChatInput.trim().length > 0 &&
          parseOptionalPositiveInteger(modalTaskChatAgentIdInput) !== null &&
          modalTaskChatMutation.status !== "submitting"
        }
        hasMoreMessages={modalTaskChatHasMore}
        paginationState={modalTaskChatPaginationState}
        onMessageChange={setModalTaskChatInput}
        onAgentChange={setModalTaskChatAgentIdInput}
        onSubmit={handleSendModalTaskChat}
        onLoadMoreMessages={() => void loadMoreModalTaskChatMessages()}
        onReload={(task) => void openTaskChatModal(task)}
        onClose={() => {
          setSelectedChatTask(null);
          setModalTaskChatMessages([]);
          setModalTaskChatState({ status: "idle", error: null });
        }}
      />

      <OrchestrationPlanModal
        plan={orchestrationPlan}
        state={orchestrationPlanState}
        onClose={() => {
          setOrchestrationPlan(null);
          setOrchestrationPlanState({ status: "idle", error: null });
        }}
        onOpenFile={(path, name) => void openModalArtifactFile(path, name)}
      />

      <ArtifactFileModal
        path={modalArtifactPath}
        nameHint={modalArtifactNameHint}
        file={modalArtifactFile}
        state={modalArtifactState}
        onClose={() => {
          setModalArtifactPath(null);
          setModalArtifactNameHint(null);
          setModalArtifactFile(null);
          setModalArtifactState({ status: "idle", error: null });
        }}
      />

      <WorkspaceCommandPalette
        open={commandPaletteOpen}
        items={workspaceCommandItems}
        onClose={() => setCommandPaletteOpen(false)}
      />

      <ShortcutCheatsheetModal
        open={shortcutsOpen}
        shortcutStyle={shortcutStyle}
        onClose={() => setShortcutsOpen(false)}
      />

      {statusNotice ? <Toast tone="info">{statusNotice}</Toast> : null}
    </div>
  );
}
