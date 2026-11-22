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
import WorkspaceCommandPalette, {
  type WorkspaceCommandItem,
} from "@/components/workspace/WorkspaceCommandPalette";
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
import {
  createPresencePositionScheduler,
  createWorkspacePresenceStream,
  parseWorkspacePresenceMessage,
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
  fetchTaskExecutionLogs,
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
  createWorkspaceStageActors,
  getEngineeringRows,
  getStageRooms,
  getStageWorldSize,
  parseStagePct,
  pct as toStagePct,
  type StageActorPosition,
  type WorkspaceSceneNode,
} from "@/lib/workspace-2d";
import {
  isEditableEventTarget,
  matchViewSwitchShortcut,
} from "@/lib/workspace-shortcuts";
import {
  fetchWorkspaceDashboardSummary,
  fetchWorkspaceDetail,
  fetchWorkspaceMembers,
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
    let ignore = false;
    let stream: EventSource | null = null;

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

      const accessToken = resolveAccessToken();
      if (!accessToken) {
        setPresenceStreamStatus("error");
        return;
      }

      setPresenceStreamStatus("connecting");
      let reconnectFailures = 0;
      stream = createWorkspacePresenceStream(numericWorkspaceId, accessToken);

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
        if (!stream || stream.readyState === EventSource.CLOSED) {
          setPresenceStreamStatus("error");
          return;
        }
        reconnectFailures += 1;
        setPresenceStreamStatus(reconnectFailures >= 2 ? "error" : "connecting");
      };
    }

    void connectPresenceStream();
    return () => {
      ignore = true;
      stream?.close();
    };
  }, [numericWorkspaceId, state.status]);

  // ── Inspector Loaders ────────────────────────────────────────
  const loadInspectorTaskDetails = useCallback(
    async (taskId: number) => {
      setInspectorTaskLogs([]);
      setInspectorTaskLogsState({ status: "loading", error: null });
      setInspectorTaskReports([]);
      setInspectorTaskReportsState({ status: "loading", error: null });
      setInspectorTaskChatMessages([]);
      setInspectorTaskChatState({ status: "loading", error: null });
      setInspectorTaskChatMutation({ status: "idle", message: null });

      const token = resolveAccessToken();
      const [logsRes, reportsRes, chatRes] = await Promise.allSettled([
        fetchTaskExecutionLogs(token, numericWorkspaceId, taskId),
        fetchTaskReports(token, numericWorkspaceId, taskId),
        fetchTaskChatMessages(token, numericWorkspaceId, taskId),
      ]);

      if (logsRes.status === "fulfilled") {
        setInspectorTaskLogs(logsRes.value);
        setInspectorTaskLogsState({ status: "ready", error: null });
      } else {
        setInspectorTaskLogsState({
          status: "error",
          error: messageFromError(logsRes.reason, "실행 로그를 불러오지 못했습니다."),
        });
      }

      if (reportsRes.status === "fulfilled") {
        setInspectorTaskReports(reportsRes.value);
        setInspectorTaskReportsState({ status: "ready", error: null });
      } else {
        setInspectorTaskReportsState({
          status: "error",
          error: messageFromError(reportsRes.reason, "리포트를 불러오지 못했습니다."),
        });
      }

      if (chatRes.status === "fulfilled") {
        const sessionId = chatRes.value[0]?.chatSessionId ?? null;
        if (sessionId) {
          try {
            const page = await fetchChatSessionMessages(
              token,
              numericWorkspaceId,
              sessionId,
              chatSessionMessagePageInput(),
            );
            setInspectorTaskChatMessages(page.messages);
          } catch {
            setInspectorTaskChatMessages(chatRes.value);
          }
        } else {
          setInspectorTaskChatMessages(chatRes.value);
        }
        setInspectorTaskChatState({ status: "ready", error: null });
      } else {
        setInspectorTaskChatState({
          status: "error",
          error: messageFromError(chatRes.reason, "작업 대화를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const openTaskInspector = useCallback(
    (taskId: number) => {
      setInspectorTarget({ type: "task", taskId });
      setMobileInspectorOpen(true);
      void loadInspectorTaskDetails(taskId);
    },
    [loadInspectorTaskDetails],
  );

  const pollActorChatMessages = useCallback(
    async (sessionId: number) => {
      try {
        const page = await fetchChatSessionMessages(
          resolveAccessToken(),
          numericWorkspaceId,
          sessionId,
          chatSessionMessagePageInput(),
        );
        setActorChatMessages(page.messages.map(actorMessageFromChatMessage));
        setActorChatMutation({ status: "idle", message: null });
      } catch (error) {
        setActorChatMutation({
          status: "error",
          message: messageFromError(error, "에이전트 대화 메시지를 갱신하지 못했습니다."),
        });
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
      }
      setActorChatSessionId(sessionId);
      setSelectedActorNode(node);
      setInspectorTarget({ type: "actor", nodeId: node.id });
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
        skillFiles: skillFiles.length > 0 ? skillFiles : undefined,
      });
      setAgentCreateForm({
        name: "",
        category: "CUSTOM",
        workspacePath: "",
        emoji: "",
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

  const handleSendActorChatMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedActorNode) {
      return;
    }
    const message = actorChatInput.trim();
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

    const optimisticMessage: ActorChatMessage = {
      id: `optimistic-${Date.now()}`,
      sender: "user",
      text: message,
      optimistic: true,
    };
    setActorChatMessages((current) => [...current, optimisticMessage]);
    setActorChatInput("");
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
      setActorChatSessionId(result.chatSessionId);
      setActorChatSessionIds((current) => {
        const next = { ...current, [sessionKey]: result.chatSessionId };
        if (typeof window !== "undefined") {
          writeActorChatSessionIds(window.localStorage, numericWorkspaceId, next);
        }
        return next;
      });
      setActorChatMessages((current) => [
        ...current.filter((item) => item.id !== optimisticMessage.id),
        ...result.messages.map(actorMessageFromChatMessage),
      ]);
      setActorChatMutation({ status: "success", message: `세션 #${result.chatSessionId} 저장됨` });
      await pollActorChatMessages(result.chatSessionId);
      await refreshTaskData();
    } catch (error) {
      setActorChatMessages((current) =>
        current.map((item) =>
          item.id === optimisticMessage.id
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

  const handleSendInspectorTaskChat = async (
    event: FormEvent<HTMLFormElement>,
    task: WorkspaceTask,
  ) => {
    event.preventDefault();
    const message = inspectorTaskChatInput.trim();
    const agentId = parseOptionalPositiveInteger(defaultAgentIdForTask(task, workspaceAgents));
    if (!message || agentId === null) {
      setInspectorTaskChatMutation({
        status: "error",
        message: "메시지 내용과 대상 에이전트를 확인해 주세요.",
      });
      return;
    }

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
      setInspectorTaskChatInput("");
      setInspectorTaskChatMutation({ status: "success", message: "전송됨" });
      await refreshTaskData();
    } catch (error) {
      setInspectorTaskChatMutation({
        status: "error",
        message: messageFromError(error, "작업 대화를 전송하지 못했습니다."),
      });
    }
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

  const openConnectionModal = useCallback(
    (kind: Exclude<ConnectionModalKind, null>) => {
      setConnectionModal(kind);
      if (kind === "slack") void loadSlackConnections();
      if (kind === "github") void loadGithubCredentials();
      if (kind === "gateway") void loadGatewayConnection();
    },
    [loadGatewayConnection, loadGithubCredentials, loadSlackConnections],
  );

  const canManage = state.workspace?.myRole === "ADMIN";
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

  const workspaceCommandItems = useMemo<WorkspaceCommandItem[]>(() => {
    const items: WorkspaceCommandItem[] = [
      {
        id: "view-office",
        label: "오피스 스테이지 보기",
        description: "2D 가상 오피스 및 참여자 위치",
        keywords: "office stage 2d",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => handleSelectView("office"),
      },
      {
        id: "view-tasks",
        label: "작업 보드 열기",
        description: "칸반 보드 및 일일 토큰 예산 현황",
        keywords: "tasks board kanban",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => handleSelectView("tasks"),
      },
      {
        id: "view-approvals",
        label: "승인 대기함 열기",
        description: "에이전트 승인 요청 검토",
        keywords: "approvals inbox waiting",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => handleSelectView("approvals"),
      },
      {
        id: "view-orchestrations",
        label: "오케스트레이션 열기",
        description: "다중 에이전트 파이프라인 구성 및 실행",
        keywords: "orchestration dag pipeline",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => handleSelectView("orchestrations"),
      },
      {
        id: "view-artifacts",
        label: "산출물 및 Git 변경사항 열기",
        description: "파일 트리 미리보기 및 로컬 Diff 조회",
        keywords: "artifacts files git diff",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => handleSelectView("artifacts"),
      },
      {
        id: "action-create-task",
        label: "새 작업 생성",
        description: "에이전트 작업 등록 및 즉시 실행",
        keywords: "new task create run",
        category: "action",
        categoryLabel: "동작",
        onSelect: () => {
          setTaskCreateDefaultAgentId(null);
          setTaskCreateMutation({ status: "idle", message: null, agentId: null });
          setTaskCreateOpen(true);
        },
      },
      {
        id: "action-hire-agent",
        label: "에이전트 추가",
        description: "새 AI 에이전트 등록 및 스킬 파일 설정",
        keywords: "agent hire create skill",
        category: "action",
        categoryLabel: "동작",
        onSelect: () => setAgentHireOpen(true),
      },
      {
        id: "action-connections",
        label: "연결 설정 열기",
        description: "Slack · GitHub · OpenClaw 게이트웨이 설정",
        keywords: "connections slack github gateway",
        category: "action",
        categoryLabel: "동작",
        onSelect: () => openConnectionModal("selector"),
      },
      {
        id: "toggle-sidebar",
        label: sidebarCollapsed ? "탐색 사이드바 펼치기" : "탐색 사이드바 접기",
        description: "데스크톱 좌측 탐색 영역 전환",
        keywords: "sidebar collapse expand toggle",
        category: "navigation",
        categoryLabel: "화면",
        onSelect: toggleSidebar,
      },
      {
        id: "toggle-theme",
        label: `테마 전환 (현재: ${themeMode === "light" ? "라이트" : "다크"})`,
        description: "라이트/다크 시맨틱 테마 전환",
        keywords: "theme light dark",
        category: "theme",
        categoryLabel: "테마",
        onSelect: handleToggleTheme,
      },
    ];

    const stageActors = createWorkspaceStageActors({
      agents: workspaceAgents,
      members,
      presenceMembers: onlinePresenceMembers,
      tasks,
    });
    for (const actor of stageActors) {
      items.push({
        id: `actor-${actor.id}`,
        label: actor.label,
        description: actor.taskSummary ?? actor.detail,
        keywords: `${actor.kind} ${actor.status}`,
        category: actor.kind === "agent" ? "agent" : "member",
        categoryLabel: actor.kind === "agent" ? "에이전트" : "구성원",
        onSelect: () => {
          setActiveView("office");
          setCameraFocusTarget((prev) => ({
            position: actor.position,
            nonce: (prev?.nonce ?? 0) + 1,
          }));
          handleSelectActor({
            id: actor.id,
            kind: actor.kind,
            label: actor.label,
            detail: actor.detail,
            status: actor.sourceStatus,
            x: parseStagePct(actor.position.left),
            y: parseStagePct(actor.position.top),
          });
        },
      });
    }

    return items;
  }, [
    handleSelectActor,
    handleSelectView,
    handleToggleTheme,
    members,
    onlinePresenceMembers,
    openConnectionModal,
    sidebarCollapsed,
    tasks,
    themeMode,
    toggleSidebar,
    workspaceAgents,
  ]);

  useEffect(() => {
    const hasOpenOverlay = Boolean(
      commandPaletteOpen ||
        modalArtifactPath ||
        selectedChatTask ||
        selectedReportsTask ||
        orchestrationPlan ||
        taskCreateOpen ||
        agentHireOpen ||
        connectionModal,
    );

    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setCommandPaletteOpen((open) => !open);
        return;
      }
      if (event.key === "Escape") {
        if (commandPaletteOpen) {
          event.preventDefault();
          setCommandPaletteOpen(false);
          return;
        }
        if (!hasOpenOverlay && inspectorTarget) {
          event.preventDefault();
          setInspectorTarget(null);
          setMobileInspectorOpen(false);
        }
        return;
      }
      if (!hasOpenOverlay && !isEditableEventTarget(event.target)) {
        const viewIndex = matchViewSwitchShortcut(shortcutStyle, event, VIEW_ORDER.length);
        if (viewIndex !== null) {
          event.preventDefault();
          handleSelectView(VIEW_ORDER[viewIndex]);
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [
    agentHireOpen,
    commandPaletteOpen,
    connectionModal,
    handleSelectView,
    inspectorTarget,
    modalArtifactPath,
    orchestrationPlan,
    selectedChatTask,
    selectedReportsTask,
    shortcutStyle,
    taskCreateOpen,
  ]);

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
    </div>
  );
}
