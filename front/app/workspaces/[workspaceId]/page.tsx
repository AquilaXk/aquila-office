"use client";

import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarDays,
  Copy,
  FileText,
  FolderTree,
  GitBranch,
  Hash,
  KeyRound,
  ListChecks,
  Loader2,
  Mail,
  RefreshCw,
  Send,
  Settings,
  ShieldCheck,
  Trash2,
  UserRound,
  Users,
  WifiOff,
  X,
} from "lucide-react";
import Link from "next/link";
import { use, useCallback, useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from "react";
import {
  PixelAvatar,
  type PixelAvatarKind,
  type PixelAvatarPaletteOverride,
} from "@/components/arcade";
import Workspace2DCanvas from "@/components/workspace/Workspace2DCanvas";
import { MarkdownContent } from "@/components/ui";
import { ApiError } from "@/lib/api";
import {
  createWorkspaceAgent,
  deleteWorkspaceAgent,
  fetchWorkspaceAgents,
  type WorkspaceAgent,
  type WorkspaceAgentCategory,
  type WorkspaceAgentSkillFileInput,
} from "@/lib/agents";
import { readAuthUser, resolveAccessToken } from "@/lib/auth";
import {
  fetchArtifactFile,
  fetchArtifactTree,
  fetchOrchestrationPlanArtifact,
  type ArtifactFileContent,
  type ArtifactNode,
  type ArtifactTree,
  type OrchestrationPlanArtifact,
} from "@/lib/artifacts";
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
  type WorkspaceGatewayConnectionTest,
  type WorkspaceGatewayStatus,
} from "@/lib/gateway";
import {
  gatewayConnectionStatusLabel,
  gatewayStatusLabel,
  normalizeGatewayForm,
} from "@/lib/gateway-utils";
import {
  createGithubCredential,
  deleteStoredGithubCredential,
  deleteGithubCredential,
  fetchGithubCredentials,
  readStoredGithubCredential,
  updateGithubCredential,
  writeStoredGithubCredential,
  type GithubCredential,
} from "@/lib/github";
import { createWorkspaceInvite, type WorkspaceInvite } from "@/lib/invites";
import {
  createWorkspacePresenceStream,
  parseWorkspacePresenceMessage,
  updateWorkspacePresencePosition,
  type WorkspacePresenceMessage,
  type WorkspacePresencePosition,
} from "@/lib/presence";
import {
  createPresenceLiveState,
  type PresenceConnectionState,
  type PresenceLiveState,
} from "@/lib/presence-summary";
import {
  createWorkspaceTask,
  fetchTaskReports,
  fetchWorkspaceTasks,
  runWorkspaceTask,
  updateWorkspaceTaskStatus,
  type AgentReport,
  type TaskArtifact,
  type TaskPriority,
  type TaskStatus,
  type TaskType,
  type WorkspaceTask,
} from "@/lib/tasks";
import {
  createWorkspaceDetailActionItems,
  createWorkspaceDetailOverview,
  type WorkspaceDetailActionItem,
} from "@/lib/workspace-detail";
import {
  deleteStoredSlackIntegration,
  deleteSlackIntegration,
  fetchSlackInstallUrl,
  fetchSlackIntegrations,
  readStoredSlackIntegration,
  saveSlackPendingConnectionState,
  type SlackIntegration,
  writeStoredSlackIntegration,
} from "@/lib/slack";
import type { StageActorPosition, WorkspaceSceneNode } from "@/lib/workspace-2d";
import {
  fetchWorkspaceDashboardSummary,
  fetchWorkspaceDetail,
  fetchWorkspaceMemberTaskStats,
  fetchWorkspaceMembers,
  type WorkspaceDashboardSummary,
  type WorkspaceDetail,
  type WorkspaceMemberTaskStats,
  type WorkspaceMember,
  type WorkspaceMemberRole,
} from "@/lib/workspaces";

type WorkspaceDetailState =
  | { status: "loading"; workspace: null; error: null }
  | { status: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[]; error: null }
  | { status: "error" | "offline"; workspace: null; error: string };

type InviteCreateState =
  | { status: "idle"; invite: null; invitePath: null; inviteUrl: null; error: null }
  | { status: "submitting"; invite: null; invitePath: null; inviteUrl: null; error: null }
  | { status: "created"; invite: WorkspaceInvite; invitePath: string; inviteUrl: string; error: null }
  | { status: "error"; invite: null; invitePath: null; inviteUrl: null; error: string };

type TaskLoadState =
  | { status: "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

type AgentLoadState =
  | { status: "idle" | "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

type AgentCreateFormState = {
  name: string;
  category: WorkspaceAgentCategory;
  workspacePath: string;
  emoji: string;
  skillFiles: AgentSkillFileDraft[];
};

type AgentSkillFileDraft = WorkspaceAgentSkillFileInput & {
  id: string;
};

type AgentMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
  agentId: number | null;
};

type TaskCreateFormState = {
  title: string;
  description: string;
  type: TaskType;
  priority: TaskPriority;
  assignedAgentId: string;
};

type TaskMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
  taskId: number | null;
};

type ChatMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

type TaskChatPaginationState = {
  status: "idle" | "loading" | "error";
  message: string | null;
};

type PresenceMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

type PresenceStreamStatus = "idle" | "connecting" | "open" | "error";
type ActorPanelTab = "chat" | "tasks";
type TaskDashboardTab = "summary" | "board";
type ConnectionModalKind = null | "selector" | "slack" | "discord" | "github" | "gateway";

type ConnectionMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

type SlackConnectionState =
  | { status: "idle" | "loading"; integrations: []; error: null }
  | { status: "ready"; integrations: SlackIntegration[]; error: null }
  | { status: "error"; integrations: SlackIntegration[]; error: string };

type GithubConnectionState =
  | { status: "idle" | "loading"; credentials: []; error: null }
  | { status: "ready"; credentials: GithubCredential[]; error: null }
  | { status: "error"; credentials: GithubCredential[]; error: string };

type GatewayConnectionState =
  | { status: "idle" | "loading"; gateway: null; error: null }
  | { status: "ready"; gateway: WorkspaceGatewayStatus; error: null }
  | { status: "error"; gateway: WorkspaceGatewayStatus | null; error: string };

type GatewayConnectionTestState =
  | { status: "idle"; result: null; message: null }
  | { status: "submitting"; result: null; message: null }
  | { status: "success"; result: WorkspaceGatewayConnectionTest; message: string }
  | { status: "error"; result: null; message: string };

type GithubConnectionAction =
  | { type: "update"; credentialId: number }
  | { type: "delete"; credentialId: number };

type SlackConnectionAction = { type: "delete"; integrationId: number };

type ActorChatMessage = {
  id: string;
  sender: "user" | "agent" | "system";
  text: string;
  optimistic: boolean;
  taskId?: number | null;
  taskExecutionId?: number | null;
  orchestrationPlanId?: number | null;
};

type DashboardSummaryState =
  | { status: "idle" | "loading"; summary: null; error: null }
  | { status: "ready"; summary: WorkspaceDashboardSummary; error: null }
  | { status: "error"; summary: null; error: string };

type MemberTaskStatsState =
  | { status: "idle" | "loading"; stats: WorkspaceMemberTaskStats[]; error: null }
  | { status: "ready"; stats: WorkspaceMemberTaskStats[]; error: null }
  | { status: "error"; stats: WorkspaceMemberTaskStats[]; error: string };

type ModalLoadState =
  | { status: "idle"; error: null }
  | { status: "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

type AvatarColorKey = "skin" | "hair" | "shirt";

interface StoredCharacterProfile {
  kind: PixelAvatarKind;
  paletteOverride?: PixelAvatarPaletteOverride;
}

type TaskGroup = {
  key: string;
  label: string;
  statuses: TaskStatus[];
  tone: "muted" | "blue" | "green" | "red";
};

const TASK_GROUPS: TaskGroup[] = [
  { key: "waiting", label: "대기", statuses: ["REQUESTED", "ASSIGNED"], tone: "muted" },
  { key: "progress", label: "진행", statuses: ["IN_PROGRESS", "WAITING_USER"], tone: "blue" },
  { key: "done", label: "완료", statuses: ["COMPLETED"], tone: "green" },
  { key: "stopped", label: "중단", statuses: ["FAILED", "CANCELED"], tone: "red" },
];

const TASK_TYPE_OPTIONS: TaskType[] = [
  "FEATURE_IMPLEMENTATION",
  "BUG_FIX",
  "CODE_REVIEW",
  "PR_REVIEW",
  "REFACTORING",
  "TEST_CREATION",
  "DOCUMENTATION",
  "PR_CREATION",
  "OTHER",
];

const TASK_PRIORITY_OPTIONS: TaskPriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const EMPTY_GITHUB_CREDENTIALS: GithubCredential[] = [];
const EMPTY_SLACK_INTEGRATIONS: SlackIntegration[] = [];

const TASK_STATUS_OPTIONS: TaskStatus[] = [
  "REQUESTED",
  "ASSIGNED",
  "IN_PROGRESS",
  "WAITING_USER",
  "COMPLETED",
  "FAILED",
  "CANCELED",
];

const AGENT_CATEGORY_OPTIONS: WorkspaceAgentCategory[] = [
  "ORCHESTRATOR",
  "BACKEND",
  "FRONTEND",
  "QA",
  "CUSTOM",
];

const officeSceneNavItems = [
  { id: "connections", label: "Connections", icon: Settings },
  { id: "tasks", label: "Tasks", icon: ListChecks },
  { id: "hire-agent", label: "Hire agent", icon: UserRound },
  { id: "files", label: "Files", icon: FolderTree },
  { id: "leave-workspace", label: "Leave workspace", icon: ArrowLeft },
] as const;

type OfficeSceneNavItemId = (typeof officeSceneNavItems)[number]["id"];

const EMPTY_WORKSPACE_MEMBERS: WorkspaceMember[] = [];
const EMPTY_PRESENCE_MEMBERS: WorkspacePresenceMessage["members"] = [];
const CHARACTER_STORAGE_KEY = "ai-office.character";

export default function WorkspaceDetailPage({
  params,
}: {
  params: Promise<{ workspaceId: string }>;
}) {
  const { workspaceId } = use(params);
  const numericWorkspaceId = useMemo(() => Number(workspaceId), [workspaceId]);
  const [state, setState] = useState<WorkspaceDetailState>({
    status: "loading",
    workspace: null,
    error: null,
  });
  const [inviteEmail, setInviteEmail] = useState("");
  const [copiedInvite, setCopiedInvite] = useState(false);
  const [inviteState, setInviteState] = useState<InviteCreateState>({
    status: "idle",
    invite: null,
    invitePath: null,
    inviteUrl: null,
    error: null,
  });
  const [tasks, setTasks] = useState<WorkspaceTask[]>([]);
  const [taskState, setTaskState] = useState<TaskLoadState>({
    status: "loading",
    error: null,
  });
  const [workspaceAgents, setWorkspaceAgents] = useState<WorkspaceAgent[]>([]);
  const [agentState, setAgentState] = useState<AgentLoadState>({
    status: "idle",
    error: null,
  });
  const [agentCreateForm, setAgentCreateForm] = useState<AgentCreateFormState>({
    name: "",
    category: "CUSTOM",
    workspacePath: "",
    emoji: "",
    skillFiles: [],
  });
  const [agentHireOpen, setAgentHireOpen] = useState(false);
  const [agentMutation, setAgentMutation] = useState<AgentMutationState>({
    status: "idle",
    message: null,
    agentId: null,
  });
  const [taskCreateForm, setTaskCreateForm] = useState<TaskCreateFormState>({
    title: "",
    description: "",
    type: "FEATURE_IMPLEMENTATION",
    priority: "MEDIUM",
    assignedAgentId: "",
  });
  const [createPr, setCreatePr] = useState(false);
  const [taskMutation, setTaskMutation] = useState<TaskMutationState>({
    status: "idle",
    message: null,
    taskId: null,
  });
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummaryState>({
    status: "idle",
    summary: null,
    error: null,
  });
  const [memberTaskStats, setMemberTaskStats] = useState<MemberTaskStatsState>({
    status: "idle",
    stats: [],
    error: null,
  });
  const [selectedTask, setSelectedTask] = useState<WorkspaceTask | null>(null);
  const [taskReports, setTaskReports] = useState<AgentReport[]>([]);
  const [reportState, setReportState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [selectedChatTask, setSelectedChatTask] = useState<WorkspaceTask | null>(null);
  const [taskChatMessages, setTaskChatMessages] = useState<ChatMessage[]>([]);
  const [taskChatState, setTaskChatState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [taskChatMessageInput, setTaskChatMessageInput] = useState("");
  const [taskChatAgentIdInput, setTaskChatAgentIdInput] = useState("");
  const [taskChatMutation, setTaskChatMutation] = useState<ChatMutationState>({
    status: "idle",
    message: null,
  });
  const [taskChatSessionId, setTaskChatSessionId] = useState<number | null>(null);
  const [taskChatCursor, setTaskChatCursor] = useState<number | null>(null);
  const [taskChatHasMore, setTaskChatHasMore] = useState(false);
  const [taskChatPaginationState, setTaskChatPaginationState] =
    useState<TaskChatPaginationState>({
      status: "idle",
      message: null,
    });
  const [selectedActor, setSelectedActor] = useState<WorkspaceSceneNode | null>(null);
  const [actorPanelOpen, setActorPanelOpen] = useState(false);
  const [actorPanelTab, setActorPanelTab] = useState<ActorPanelTab>("chat");
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
  const [taskDashboardOpen, setTaskDashboardOpen] = useState(false);
  const [taskDashboardTab, setTaskDashboardTab] = useState<TaskDashboardTab>("summary");
  const [selectedDashboardTask, setSelectedDashboardTask] = useState<WorkspaceTask | null>(null);
  const [presenceStreamStatus, setPresenceStreamStatus] =
    useState<PresenceStreamStatus>("idle");
  const [presenceEvents, setPresenceEvents] = useState<WorkspacePresenceMessage[]>([]);
  const [playerPositionOverride, setPlayerPositionOverride] =
    useState<StageActorPosition | null>(null);
  const [presenceMutation, setPresenceMutation] = useState<PresenceMutationState>({
    status: "idle",
    message: null,
  });
  const [filesOpen, setFilesOpen] = useState(false);
  const [artifactTree, setArtifactTree] = useState<ArtifactTree | null>(null);
  const [artifactTreeState, setArtifactTreeState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [expandedArtifactPaths, setExpandedArtifactPaths] = useState<Set<string>>(
    () => new Set(),
  );
  const [artifactNotice, setArtifactNotice] = useState<string | null>(null);
  const [artifactFile, setArtifactFile] = useState<ArtifactFileContent | null>(null);
  const [artifactFilePath, setArtifactFilePath] = useState<string | null>(null);
  const [artifactFileNameHint, setArtifactFileNameHint] = useState<string | null>(null);
  const [artifactFileState, setArtifactFileState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [orchestrationPlan, setOrchestrationPlan] =
    useState<OrchestrationPlanArtifact | null>(null);
  const [orchestrationPlanState, setOrchestrationPlanState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
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
  const [slackConnectionMutation, setSlackConnectionMutation] =
    useState<ConnectionMutationState>({
      status: "idle",
      message: null,
    });
  const [slackConnectionAction, setSlackConnectionAction] =
    useState<SlackConnectionAction | null>(null);
  const [githubConnectionState, setGithubConnectionState] =
    useState<GithubConnectionState>(() => {
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
  const [selectedGithubCredentialId, setSelectedGithubCredentialId] =
    useState<number | null>(() => {
      if (typeof window === "undefined") {
        return null;
      }
      return readStoredGithubCredential(window.localStorage, numericWorkspaceId)?.credentialId ?? null;
    });
  const [editingGithubCredentialId, setEditingGithubCredentialId] =
    useState<number | null>(null);
  const [githubEditDisplayNameInput, setGithubEditDisplayNameInput] = useState("");
  const [githubEditTokenInput, setGithubEditTokenInput] = useState("");
  const [githubConnectionCreateMutation, setGithubConnectionCreateMutation] =
    useState<ConnectionMutationState>({
      status: "idle",
      message: null,
    });
  const [githubConnectionActionMutation, setGithubConnectionActionMutation] =
    useState<ConnectionMutationState>({
      status: "idle",
      message: null,
    });
  const [githubConnectionAction, setGithubConnectionAction] =
    useState<GithubConnectionAction | null>(null);
  const [gatewayConnectionState, setGatewayConnectionState] =
    useState<GatewayConnectionState>(() => {
      if (typeof window === "undefined") {
        return { status: "idle", gateway: null, error: null };
      }
      const stored = readStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId);
      return stored
        ? { status: "ready", gateway: stored, error: null }
        : { status: "idle", gateway: null, error: null };
    });
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
  const [gatewayBindingMutation, setGatewayBindingMutation] =
    useState<ConnectionMutationState>({
      status: "idle",
      message: null,
    });

  const reloadTasks = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setTasks([]);
      setTaskState({
        status: "error",
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    setTaskState({ status: "loading", error: null });

    try {
      const nextTasks = await fetchWorkspaceTasks(resolveAccessToken(), numericWorkspaceId);
      setTasks(nextTasks);
      setTaskState({ status: "ready", error: null });
    } catch (error) {
      setTasks([]);
      setTaskState({
        status: "error",
        error: messageFromError(error, "Task 목록을 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const reloadAgents = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setWorkspaceAgents([]);
      setAgentState({
        status: "error",
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    setAgentState({ status: "loading", error: null });

    try {
      const agents = await fetchWorkspaceAgents(resolveAccessToken(), numericWorkspaceId);
      setWorkspaceAgents(agents);
      setAgentState({ status: "ready", error: null });
    } catch (error) {
      setWorkspaceAgents([]);
      setAgentState({
        status: "error",
        error: messageFromError(error, "Agent 목록을 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const reloadDashboardSummary = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setDashboardSummary({
        status: "error",
        summary: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      setMemberTaskStats({
        status: "error",
        stats: [],
        error: "올바르지 않은 워크스페이스입니다.",
      });
      setWorkspaceAgents([]);
      setAgentState({
        status: "error",
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    setDashboardSummary({ status: "loading", summary: null, error: null });
    setMemberTaskStats({ status: "loading", stats: [], error: null });

    const [summaryResult, statsResult] = await Promise.all([
      fetchWorkspaceDashboardSummary(resolveAccessToken(), numericWorkspaceId)
        .then((summary) => ({ ok: true as const, summary }))
        .catch((error) => ({ ok: false as const, error })),
      fetchWorkspaceMemberTaskStats(resolveAccessToken(), numericWorkspaceId)
        .then((stats) => ({ ok: true as const, stats }))
        .catch((error) => ({ ok: false as const, error })),
    ]);

    if (summaryResult.ok) {
      setDashboardSummary({ status: "ready", summary: summaryResult.summary, error: null });
    } else {
      setDashboardSummary({
        status: "error",
        summary: null,
        error: messageFromError(summaryResult.error, "Dashboard 요약을 불러오지 못했습니다."),
      });
    }

    if (statsResult.ok) {
      setMemberTaskStats({ status: "ready", stats: statsResult.stats, error: null });
    } else {
      setMemberTaskStats({
        status: "error",
        stats: [],
        error: messageFromError(statsResult.error, "멤버 작업 통계를 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const refreshTaskData = useCallback(async () => {
    await Promise.all([reloadTasks(), reloadDashboardSummary()]);
  }, [reloadDashboardSummary, reloadTasks]);

  const refreshAgentData = useCallback(async () => {
    await Promise.all([reloadAgents(), reloadDashboardSummary()]);
  }, [reloadAgents, reloadDashboardSummary]);

  const handleCreateAgent = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const name = agentCreateForm.name.trim();
    if (!name) {
      setAgentMutation({
        status: "error",
        message: "Agent 이름을 입력해 주세요.",
        agentId: null,
      });
      return;
    }

    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setAgentMutation({
        status: "error",
        message: "올바르지 않은 워크스페이스입니다.",
        agentId: null,
      });
      return;
    }

    const skillFiles = normalizeAgentSkillFiles(agentCreateForm.skillFiles);
    if (skillFiles === null) {
      setAgentMutation({
        status: "error",
        message: "Skill 파일은 파일명과 내용을 모두 입력해 주세요.",
        agentId: null,
      });
      return;
    }

    setAgentMutation({ status: "submitting", message: "Agent 등록 중", agentId: null });

    try {
      const created = await createWorkspaceAgent(resolveAccessToken(), numericWorkspaceId, {
        name,
        category: agentCreateForm.category,
        workspacePath: agentCreateForm.workspacePath.trim() || undefined,
        emoji: agentCreateForm.emoji.trim() || undefined,
        skillFiles: skillFiles.length > 0 ? skillFiles : undefined,
      });
      setAgentCreateForm((current) => ({
        ...current,
        name: "",
        workspacePath: "",
        emoji: "",
        skillFiles: [],
      }));
      await refreshAgentData();
      setAgentMutation({
        status: "success",
        message: `Agent #${created.agentId} 등록 완료`,
        agentId: created.agentId,
      });
      setAgentHireOpen(false);
    } catch (error) {
      setAgentMutation({
        status: "error",
        message: messageFromError(error, "Agent 등록을 처리하지 못했습니다."),
        agentId: null,
      });
    }
  };

  const addAgentSkillFile = useCallback(() => {
    setAgentCreateForm((current) => ({
      ...current,
      skillFiles: [
        ...current.skillFiles,
        { id: createLocalId(), fileName: "", content: "" },
      ],
    }));
    setAgentMutation({ status: "idle", message: null, agentId: null });
  }, []);

  const updateAgentSkillFile = useCallback(
    (id: string, field: "fileName" | "content", value: string) => {
      setAgentCreateForm((current) => ({
        ...current,
        skillFiles: current.skillFiles.map((skillFile) =>
          skillFile.id === id ? { ...skillFile, [field]: value } : skillFile,
        ),
      }));
      setAgentMutation({ status: "idle", message: null, agentId: null });
    },
    [],
  );

  const removeAgentSkillFile = useCallback((id: string) => {
    setAgentCreateForm((current) => ({
      ...current,
      skillFiles: current.skillFiles.filter((skillFile) => skillFile.id !== id),
    }));
    setAgentMutation({ status: "idle", message: null, agentId: null });
  }, []);

  const handleDeleteAgent = useCallback(
    async (agent: WorkspaceAgent) => {
      setAgentMutation({ status: "submitting", message: "Agent 삭제 중", agentId: agent.agentId });

      try {
        await deleteWorkspaceAgent(resolveAccessToken(), numericWorkspaceId, agent.agentId);
        await refreshAgentData();
        setAgentMutation({
          status: "success",
          message: `Agent #${agent.agentId} 삭제 완료`,
          agentId: agent.agentId,
        });
      } catch (error) {
        setAgentMutation({
          status: "error",
          message: messageFromError(error, "Agent 삭제를 처리하지 못했습니다."),
          agentId: agent.agentId,
        });
      }
    },
    [numericWorkspaceId, refreshAgentData],
  );

  const handleCreateTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const title = taskCreateForm.title.trim();
    if (!title) {
      setTaskMutation({
        status: "error",
        message: "Task 제목을 입력해 주세요.",
        taskId: null,
      });
      return;
    }

    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setTaskMutation({
        status: "error",
        message: "올바르지 않은 워크스페이스입니다.",
        taskId: null,
      });
      return;
    }

    const assignedAgentId = parseOptionalPositiveInteger(taskCreateForm.assignedAgentId);
    if (taskCreateForm.assignedAgentId.trim() && assignedAgentId === null) {
      setTaskMutation({
        status: "error",
        message: "Agent ID는 1 이상의 숫자로 입력해 주세요.",
        taskId: null,
      });
      return;
    }

    setTaskMutation({ status: "submitting", message: "Task 생성 중", taskId: null });

    try {
      const created = await createWorkspaceTask(resolveAccessToken(), numericWorkspaceId, {
        title,
        description: taskCreateForm.description.trim() || null,
        type: taskCreateForm.type,
        priority: taskCreateForm.priority,
        assignedAgentId,
      });
      setTaskCreateForm((current) => ({
        ...current,
        title: "",
        description: "",
        assignedAgentId: "",
      }));
      await refreshTaskData();
      setTaskMutation({
        status: "success",
        message: `Task #${created.taskId} 생성 완료`,
        taskId: created.taskId,
      });
    } catch (error) {
      setTaskMutation({
        status: "error",
        message: messageFromError(error, "Task 생성을 처리하지 못했습니다."),
        taskId: null,
      });
    }
  };

  const handleRunTask = useCallback(
    async (task: WorkspaceTask) => {
      setTaskMutation({ status: "submitting", message: "Task 실행 중", taskId: task.taskId });

      try {
        const result = await runWorkspaceTask(resolveAccessToken(), numericWorkspaceId, task.taskId, {
          createPr,
        });
        await refreshTaskData();
        setTaskMutation({
          status: "success",
          message: `실행 #${result.executionId} 요청 완료`,
          taskId: task.taskId,
        });
      } catch (error) {
        setTaskMutation({
          status: "error",
          message: messageFromError(error, "Task 실행을 처리하지 못했습니다."),
          taskId: task.taskId,
        });
      }
    },
    [createPr, numericWorkspaceId, refreshTaskData],
  );

  const handleUpdateTaskStatus = useCallback(
    async (task: WorkspaceTask, status: TaskStatus) => {
      if (task.status === status) {
        return;
      }

      setTaskMutation({ status: "submitting", message: "Task 상태 변경 중", taskId: task.taskId });

      try {
        const updated = await updateWorkspaceTaskStatus(resolveAccessToken(), numericWorkspaceId, task.taskId, {
          status,
        });
        await refreshTaskData();
        setTaskMutation({
          status: "success",
          message: `Task #${updated.taskId} 상태 변경 완료`,
          taskId: updated.taskId,
        });
      } catch (error) {
        setTaskMutation({
          status: "error",
          message: messageFromError(error, "Task 상태 변경을 처리하지 못했습니다."),
          taskId: task.taskId,
        });
      }
    },
    [numericWorkspaceId, refreshTaskData],
  );

  const openTaskReports = useCallback(
    async (task: WorkspaceTask) => {
      setSelectedTask(task);
      setTaskReports([]);
      setReportState({ status: "loading", error: null });

      try {
        const reports = await fetchTaskReports(resolveAccessToken(), numericWorkspaceId, task.taskId);
        setTaskReports(reports);
        setReportState({ status: "ready", error: null });
      } catch (error) {
        setReportState({
          status: "error",
          error: messageFromError(error, "Task report를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const openTaskChat = useCallback(
    async (task: WorkspaceTask) => {
      setSelectedChatTask(task);
      setTaskChatMessages([]);
      setTaskChatAgentIdInput(defaultAgentIdForTask(task, workspaceAgents));
      setTaskChatMessageInput("");
      setTaskChatMutation({ status: "idle", message: null });
      setTaskChatSessionId(null);
      setTaskChatCursor(null);
      setTaskChatHasMore(false);
      setTaskChatPaginationState({ status: "idle", message: null });
      setTaskChatState({ status: "loading", error: null });

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
          setTaskChatMessages(page.messages);
          setTaskChatSessionId(sessionId);
          setTaskChatCursor(page.nextCursor);
          setTaskChatHasMore(page.hasMore);
        } else {
          setTaskChatMessages(messages);
          setTaskChatSessionId(null);
          setTaskChatCursor(null);
          setTaskChatHasMore(false);
        }
        setTaskChatState({ status: "ready", error: null });
      } catch (error) {
        setTaskChatState({
          status: "error",
          error: messageFromError(error, "Task chat 메시지를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId, workspaceAgents],
  );

  const loadMoreTaskChatMessages = useCallback(async () => {
    if (!taskChatSessionId || taskChatCursor === null) {
      return;
    }

    setTaskChatPaginationState({ status: "loading", message: null });
    try {
      const page = await fetchChatSessionMessages(
        resolveAccessToken(),
        numericWorkspaceId,
        taskChatSessionId,
        chatSessionMessagePageInput(taskChatCursor),
      );
      setTaskChatMessages((current) => mergeChatMessages(current, page.messages));
      setTaskChatCursor(page.nextCursor);
      setTaskChatHasMore(page.hasMore);
      setTaskChatPaginationState({ status: "idle", message: null });
    } catch (error) {
      setTaskChatPaginationState({
        status: "error",
        message: messageFromError(error, "Chat 메시지를 더 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId, taskChatCursor, taskChatSessionId]);

  const openOrchestrationPlan = useCallback(
    async (planId: number) => {
      setOrchestrationPlan(null);
      setOrchestrationPlanState({ status: "loading", error: null });
      try {
        const plan = await fetchOrchestrationPlanArtifact(resolveAccessToken(), numericWorkspaceId, planId);
        setOrchestrationPlan(plan);
        setOrchestrationPlanState({ status: "ready", error: null });
      } catch (planError) {
        setOrchestrationPlanState({
          status: "error",
          error: messageFromError(planError, "Orchestration plan artifact를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const handleSendTaskChatMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedChatTask) {
      return;
    }

    const message = taskChatMessageInput.trim();
    if (!message) {
      setTaskChatMutation({
        status: "error",
        message: "전송할 메시지를 입력해 주세요.",
      });
      return;
    }

    const agentId = parseOptionalPositiveInteger(taskChatAgentIdInput);
    if (agentId === null) {
      setTaskChatMutation({
        status: "error",
        message: "Agent를 선택해 주세요.",
      });
      return;
    }

    setTaskChatMutation({ status: "submitting", message: null });
    try {
      const currentSessionId =
        taskChatSessionId ?? taskChatMessages.find((chatMessage) => chatMessage.chatSessionId)?.chatSessionId;
      const result = await sendWorkspaceChatMessage(resolveAccessToken(), numericWorkspaceId, {
        message,
        agentId,
        taskType: selectedChatTask.type,
        priority: selectedChatTask.priority,
        title: selectedChatTask.title,
        createPr,
        chatSessionId: currentSessionId ?? null,
      });
      const nextMessages = mergeChatMessages(taskChatMessages, result.messages);
      setTaskChatMessages(nextMessages);
      setTaskChatSessionId(result.chatSessionId);
      setTaskChatCursor(lastChatMessageId(nextMessages));
      setTaskChatHasMore(false);
      setTaskChatPaginationState({ status: "idle", message: null });
      setTaskChatMessageInput("");
      setTaskChatState({ status: "ready", error: null });
      setTaskChatMutation({
        status: "success",
        message: `Task #${result.taskId} chat 메시지가 전송되었습니다.`,
      });
      if (result.orchestrationPlanId) {
        await openOrchestrationPlan(result.orchestrationPlanId);
      } else {
        setOrchestrationPlan(null);
        setOrchestrationPlanState({ status: "idle", error: null });
      }
      await refreshTaskData();
    } catch (error) {
      setTaskChatMutation({
        status: "error",
        message: messageFromError(error, "Task chat 메시지를 전송하지 못했습니다."),
      });
    }
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
        setActorChatMessages(page.messages.map(actorMessageFromChatMessage));
        setActorChatMutation({ status: "idle", message: null });
      } catch (error) {
        setActorChatMutation({
          status: "error",
          message: messageFromError(error, "Agent 대화 메시지를 갱신하지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const handleSelectActor = useCallback(
    (node: WorkspaceSceneNode) => {
      const sessionId =
        node.kind === "agent" ? actorChatSessionIds[actorChatSessionKey(node.id)] ?? null : null;
      if (selectedActor?.id !== node.id) {
        setActorChatInput("");
        setActorChatMessages([]);
      }
      setActorChatSessionId(sessionId);
      setSelectedActor(node);
      setActorPanelOpen(true);
      setActorPanelTab("chat");
      setActorChatMutation({ status: "idle", message: null });
      if (sessionId) {
        void pollActorChatMessages(sessionId);
      }
    },
    [actorChatSessionIds, pollActorChatMessages, selectedActor?.id],
  );

  const handleCloseActorPanel = useCallback(() => {
    setActorPanelOpen(false);
    setSelectedActor(null);
    setActorPanelTab("chat");
    setActorChatInput("");
    setActorChatSessionId(null);
    setActorChatMutation({ status: "idle", message: null });
  }, []);

  const handleSendActorChatMessage = async () => {
    if (!selectedActor) {
      setActorChatMutation({ status: "error", message: "actor를 선택해 주세요." });
      return;
    }

    const message = actorChatInput.trim();
    if (!message) {
      setActorChatMutation({ status: "error", message: "전송할 메시지를 입력해 주세요." });
      return;
    }

    if (selectedActor.kind !== "agent") {
      setActorChatMessages((current) => [
        ...current,
        {
          id: `member-${Date.now()}`,
          sender: "user",
          text: message,
          optimistic: false,
        },
        {
          id: `member-system-${Date.now()}`,
          sender: "system",
          text: "현재 멤버 대화는 화면 내 대화로 표시됩니다.",
          optimistic: false,
        },
      ]);
      setActorChatInput("");
      setActorChatMutation({ status: "success", message: "멤버 actor 대화가 표시되었습니다." });
      return;
    }

    const agentId = agentIdFromActorNode(selectedActor);
    if (agentId === null) {
      setActorChatMutation({ status: "error", message: "전송할 Agent를 확인해 주세요." });
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
    setActorChatMutation({ status: "submitting", message: "Agent에게 메시지 전송 중" });

    try {
      const result = await sendWorkspaceChatMessage(resolveAccessToken(), numericWorkspaceId, {
        message,
        agentId,
        taskType: "OTHER",
        priority: "MEDIUM",
        title: `Actor chat: ${selectedActor.label}`,
        chatSessionId: actorChatSessionId,
      });
      const sessionKey = actorChatSessionKey(selectedActor.id);
      setActorChatSessionId(result.chatSessionId);
      setActorChatSessionIds((current) => {
        const next = { ...current, [sessionKey]: result.chatSessionId };
        if (typeof window !== "undefined") {
          writeActorChatSessionIds(window.localStorage, numericWorkspaceId, next);
        }
        return next;
      });
      setActorChatMessages((current) => [
        ...current.filter((chatMessage) => chatMessage.id !== optimisticMessage.id),
        ...result.messages.map(actorMessageFromChatMessage),
      ]);
      setActorChatMutation({ status: "success", message: `Session #${result.chatSessionId} 저장됨` });
      await pollActorChatMessages(result.chatSessionId);
      await refreshTaskData();
    } catch (error) {
      setActorChatMessages((current) =>
        current.map((chatMessage) =>
          chatMessage.id === optimisticMessage.id
            ? { ...chatMessage, optimistic: false, text: `${chatMessage.text} (전송 실패)` }
            : chatMessage,
        ),
      );
      setActorChatMutation({
        status: "error",
        message: messageFromError(error, "Agent 대화를 전송하지 못했습니다."),
      });
    }
  };

  const openWorkspaceFiles = useCallback(async () => {
    setFilesOpen(true);
    setArtifactTree(null);
    setArtifactTreeState({ status: "loading", error: null });
    setArtifactNotice(null);

    try {
      const tree = await fetchArtifactTree(resolveAccessToken(), numericWorkspaceId);
      setArtifactTree(tree);
      setExpandedArtifactPaths(defaultExpandedArtifactPaths(tree));
      setArtifactTreeState({ status: "ready", error: null });
    } catch {
      setArtifactTree(demoWorkspaceArtifactTree(numericWorkspaceId));
      setExpandedArtifactPaths(defaultExpandedArtifactPaths(demoWorkspaceArtifactTree(numericWorkspaceId)));
      setArtifactNotice("파일 API 응답 대신 demo workspace 파일을 표시합니다.");
      setArtifactTreeState({ status: "ready", error: null });
    }
  }, [numericWorkspaceId]);

  const openArtifactFile = useCallback(
    async (path: string, nameHint?: string) => {
      setArtifactFile(null);
      setArtifactFilePath(path);
      setArtifactFileNameHint(nameHint ?? path.split("/").pop() ?? path);
      setArtifactFileState({ status: "loading", error: null });
      setArtifactNotice(null);

      try {
        const file = await fetchArtifactFile(resolveAccessToken(), numericWorkspaceId, path);
        setArtifactFile(file);
        setArtifactFileState({ status: "ready", error: null });
      } catch {
        const fallbackFile = demoWorkspaceArtifactFile(numericWorkspaceId, path);
        if (!fallbackFile) {
          setArtifactFileState({ status: "error", error: "파일을 찾을 수 없습니다." });
          return;
        }
        setArtifactFile(fallbackFile);
        setArtifactNotice("파일 API 응답 대신 demo workspace 파일을 표시합니다.");
        setArtifactFileState({ status: "ready", error: null });
      }
    },
    [numericWorkspaceId],
  );

  const toggleArtifactDirectory = useCallback((path: string) => {
    setExpandedArtifactPaths((current) => toggleExpandedArtifactPath(current, path));
  }, []);

  const loadSlackConnections = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setSlackConnectionState({
        status: "error",
        integrations: [],
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    setSlackConnectionState({ status: "loading", integrations: [], error: null });
    setSlackConnectionMutation({ status: "idle", message: null });

    try {
      const integrations = await fetchSlackIntegrations(
        resolveAccessToken(),
        numericWorkspaceId,
      );
      if (typeof window !== "undefined") {
        if (integrations[0]) {
          writeStoredSlackIntegration(window.localStorage, numericWorkspaceId, integrations[0]);
        } else {
          deleteStoredSlackIntegration(window.localStorage, numericWorkspaceId);
        }
      }
      setSlackConnectionState({ status: "ready", integrations, error: null });
    } catch (error) {
      const stored =
        typeof window === "undefined"
          ? null
          : readStoredSlackIntegration(window.localStorage, numericWorkspaceId);
      setSlackConnectionState({
        status: "error",
        integrations: stored ? [stored] : [],
        error: messageFromError(error, "Slack 연동 목록을 확인하지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const loadGithubCredentials = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setGithubConnectionState({
        status: "error",
        credentials: [],
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    setGithubConnectionState({ status: "loading", credentials: [], error: null });
    setGithubConnectionCreateMutation({ status: "idle", message: null });
    setGithubConnectionActionMutation({ status: "idle", message: null });

    try {
      const credentials = await fetchGithubCredentials(
        resolveAccessToken(),
        numericWorkspaceId,
      );
      if (typeof window !== "undefined") {
        if (credentials[0]) {
          writeStoredGithubCredential(window.localStorage, numericWorkspaceId, credentials[0]);
        } else {
          deleteStoredGithubCredential(window.localStorage, numericWorkspaceId);
        }
      }
      setGithubConnectionState({ status: "ready", credentials, error: null });
      setSelectedGithubCredentialId((current) =>
        current && credentials.some((credential) => credential.credentialId === current)
          ? current
          : credentials[0]?.credentialId ?? null,
      );
    } catch (error) {
      const stored =
        typeof window === "undefined"
          ? null
          : readStoredGithubCredential(window.localStorage, numericWorkspaceId);
      setGithubConnectionState({
        status: "error",
        credentials: stored ? [stored] : [],
        error: messageFromError(error, "GitHub 자격 증명 목록을 확인하지 못했습니다."),
      });
      setSelectedGithubCredentialId((current) => current ?? stored?.credentialId ?? null);
    }
  }, [numericWorkspaceId]);

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
      const gateway = await fetchWorkspaceGatewayStatus(
        resolveAccessToken(),
        numericWorkspaceId,
      );
      const normalizedGateway = normalizeWorkspaceGatewayStatus(gateway);
      if (typeof window !== "undefined") {
        if (normalizedGateway.bound) {
          writeStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId, normalizedGateway);
        } else {
          deleteStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId);
        }
      }
      setGatewayConnectionState({ status: "ready", gateway: normalizedGateway, error: null });
      setGatewayUrlInput((current) =>
        current || normalizedGateway.gatewayUrl || "",
      );
    } catch (error) {
      const stored =
        typeof window === "undefined"
          ? null
          : readStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId);
      setGatewayConnectionState({
        status: "error",
        gateway: stored,
        error: messageFromError(error, "OpenClaw Gateway 상태를 확인하지 못했습니다."),
      });
      setGatewayUrlInput((current) => current || stored?.gatewayUrl || "");
    }
  }, [numericWorkspaceId]);

  const openConnectionModal = useCallback(
    (kind: Exclude<ConnectionModalKind, null>) => {
      setConnectionModal(kind);

      if (kind === "slack") {
        void loadSlackConnections();
      }
      if (kind === "github") {
        void loadGithubCredentials();
      }
      if (kind === "gateway") {
        void loadGatewayConnection();
      }
    },
    [loadGatewayConnection, loadGithubCredentials, loadSlackConnections],
  );

  const handleOfficeSceneNavAction = useCallback(
    (itemId: OfficeSceneNavItemId) => {
      if (itemId === "connections") {
        openConnectionModal("selector");
      }
      if (itemId === "tasks") {
        setTaskDashboardOpen(true);
        setTaskDashboardTab("summary");
      }
      if (itemId === "hire-agent") {
        setAgentHireOpen(true);
      }
      if (itemId === "files") {
        void openWorkspaceFiles();
      }
    },
    [openConnectionModal, openWorkspaceFiles],
  );

  const reloadWorkspace = useCallback(async () => {
    setState({ status: "loading", workspace: null, error: null });

    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setState({
        status: "error",
        workspace: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      setDashboardSummary({
        status: "error",
        summary: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      setMemberTaskStats({
        status: "error",
        stats: [],
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    try {
      const [workspace, members] = await Promise.all([
        fetchWorkspaceDetail(resolveAccessToken(), numericWorkspaceId),
        fetchWorkspaceMembers(resolveAccessToken(), numericWorkspaceId),
      ]);
      setState({ status: "ready", workspace, members, error: null });
      void reloadTasks();
      void reloadAgents();
      void reloadDashboardSummary();
    } catch (error) {
      setState(toFailureState(error, "워크스페이스 정보를 확인하지 못했습니다."));
    }
  }, [numericWorkspaceId, reloadAgents, reloadDashboardSummary, reloadTasks]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialWorkspace() {
      if (!isValidWorkspaceId(numericWorkspaceId)) {
        if (!ignore) {
          setState({
            status: "error",
            workspace: null,
            error: "올바르지 않은 워크스페이스입니다.",
          });
          setTasks([]);
          setTaskState({
            status: "error",
            error: "올바르지 않은 워크스페이스입니다.",
          });
          setDashboardSummary({
            status: "error",
            summary: null,
            error: "올바르지 않은 워크스페이스입니다.",
          });
          setMemberTaskStats({
            status: "error",
            stats: [],
            error: "올바르지 않은 워크스페이스입니다.",
          });
          setWorkspaceAgents([]);
          setAgentState({
            status: "error",
            error: "올바르지 않은 워크스페이스입니다.",
          });
        }
        return;
      }

      try {
        const [
          workspace,
          members,
          initialTasksResult,
          initialAgentsResult,
          initialSummaryResult,
          initialMemberTaskStatsResult,
        ] = await Promise.all([
          fetchWorkspaceDetail(resolveAccessToken(), numericWorkspaceId),
          fetchWorkspaceMembers(resolveAccessToken(), numericWorkspaceId),
          fetchWorkspaceTasks(resolveAccessToken(), numericWorkspaceId)
            .then((nextTasks) => ({ ok: true as const, nextTasks }))
            .catch((error) => ({ ok: false as const, error })),
          fetchWorkspaceAgents(resolveAccessToken(), numericWorkspaceId)
            .then((agents) => ({ ok: true as const, agents }))
            .catch((error) => ({ ok: false as const, error })),
          fetchWorkspaceDashboardSummary(resolveAccessToken(), numericWorkspaceId)
            .then((summary) => ({ ok: true as const, summary }))
            .catch((error) => ({ ok: false as const, error })),
          fetchWorkspaceMemberTaskStats(resolveAccessToken(), numericWorkspaceId)
            .then((stats) => ({ ok: true as const, stats }))
            .catch((error) => ({ ok: false as const, error })),
        ]);
        if (!ignore) {
          setState({ status: "ready", workspace, members, error: null });
          if (initialTasksResult.ok) {
            setTasks(initialTasksResult.nextTasks);
            setTaskState({ status: "ready", error: null });
          } else {
            setTasks([]);
            setTaskState({
              status: "error",
              error: messageFromError(initialTasksResult.error, "Task 목록을 불러오지 못했습니다."),
            });
          }
          if (initialAgentsResult.ok) {
            setWorkspaceAgents(initialAgentsResult.agents);
            setAgentState({ status: "ready", error: null });
          } else {
            setWorkspaceAgents([]);
            setAgentState({
              status: "error",
              error: messageFromError(initialAgentsResult.error, "Agent 목록을 불러오지 못했습니다."),
            });
          }
          if (initialSummaryResult.ok) {
            setDashboardSummary({ status: "ready", summary: initialSummaryResult.summary, error: null });
          } else {
            setDashboardSummary({
              status: "error",
              summary: null,
              error: messageFromError(initialSummaryResult.error, "Dashboard 요약을 불러오지 못했습니다."),
            });
          }
          if (initialMemberTaskStatsResult.ok) {
            setMemberTaskStats({
              status: "ready",
              stats: initialMemberTaskStatsResult.stats,
              error: null,
            });
          } else {
            setMemberTaskStats({
              status: "error",
              stats: [],
              error: messageFromError(initialMemberTaskStatsResult.error, "멤버 작업 통계를 불러오지 못했습니다."),
            });
          }
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
        return;
      }

      const accessToken = resolveAccessToken();
      if (!accessToken) {
        setPresenceStreamStatus("error");
        return;
      }

      setPresenceStreamStatus("connecting");
      stream = createWorkspacePresenceStream(numericWorkspaceId, accessToken);

      stream.onopen = () => {
        setPresenceStreamStatus("open");
      };
      stream.onmessage = (event) => {
        const message = parseWorkspacePresenceMessage(String(event.data));
        if (!message) {
          return;
        }
        setPresenceEvents((current) => [message, ...current].slice(0, 5));
        if (message.type === "presence.error") {
          setPresenceStreamStatus("error");
        }
      };
      stream.onerror = () => {
        setPresenceStreamStatus("error");
      };
    }

    void connectPresenceStream();

    return () => {
      ignore = true;
      stream?.close();
    };
  }, [numericWorkspaceId, state.status]);

  const workspace = state.workspace;
  const members = state.status === "ready" ? state.members : EMPTY_WORKSPACE_MEMBERS;
  const adminCount = members.filter((member) => member.role === "ADMIN").length;
  const groupedTasks = useMemo(
    () =>
      TASK_GROUPS.map((group) => ({
        ...group,
        tasks: tasks.filter((task) => group.statuses.includes(task.status)),
      })),
    [tasks],
  );
  const summary = dashboardSummary.status === "ready" ? dashboardSummary.summary : null;
  const recentReports = summary?.recentReports ?? [];
  const recentLogs = summary?.recentLogs ?? [];
  const memberStats = memberTaskStats.status === "ready" ? memberTaskStats.stats : [];
  const readyAgentCount = workspaceAgents.filter((agent) => agent.status === "READY").length;
  const isDashboardLoading = dashboardSummary.status === "loading" || memberTaskStats.status === "loading";
  const isAgentMutating = agentMutation.status === "submitting";
  const canCreateAgent = agentCreateForm.name.trim().length > 0 && !isAgentMutating;
  const isTaskMutating = taskMutation.status === "submitting";
  const canCreateTask = taskCreateForm.title.trim().length > 0 && !isTaskMutating;
  const isSendingTaskChat = taskChatMutation.status === "submitting";
  const canSendTaskChatMessage =
    selectedChatTask !== null &&
    taskChatMessageInput.trim().length > 0 &&
    taskChatAgentIdInput.trim().length > 0 &&
    !isSendingTaskChat;
  const presenceLiveState = useMemo(
    () =>
      createPresenceLiveState({
        streamStatus: presenceStreamStatus,
        workspaceMemberCount: members.length,
        events: presenceEvents,
      }),
    [members.length, presenceEvents, presenceStreamStatus],
  );
  const onlinePresenceMembers = presenceLiveState.members ?? EMPTY_PRESENCE_MEMBERS;
  const isUpdatingPresence = presenceMutation.status === "submitting";
  const storedCharacterProfile = useMemo(() => loadStoredCharacterProfile(), []);
  const currentMemberId = useMemo(() => readAuthUser()?.memberId ?? null, []);
  const canCreateInvite = workspace?.myRole === "ADMIN";
  const isCreatingInvite = inviteState.status === "submitting";
  const canSubmitInvite = canCreateInvite && inviteEmail.trim().length > 0 && !isCreatingInvite;
  const detailOverview = useMemo(
    () =>
      createWorkspaceDetailOverview({
        dashboardSummary: summary,
        agents: workspaceAgents,
        tasks,
        members,
        presenceMembers: onlinePresenceMembers,
      }),
    [members, onlinePresenceMembers, summary, tasks, workspaceAgents],
  );
  const detailActionItems = useMemo(
    () =>
      createWorkspaceDetailActionItems({
        agentCount: detailOverview.agentSummary.total,
        taskCount: detailOverview.taskSummary.total,
        memberCount: detailOverview.memberSummary.total,
        canCreateInvite,
        hasArtifactTree: artifactTree !== null,
        hasOrchestrationPlan: orchestrationPlan !== null,
      }),
    [artifactTree, canCreateInvite, detailOverview, orchestrationPlan],
  );
  const selectedActorAgentId = selectedActor ? agentIdFromActorNode(selectedActor) : null;
  const selectedActorTasks = useMemo(
    () =>
      selectedActorAgentId
        ? tasks.filter((task) => isTaskAssignedToAgent(task, selectedActorAgentId))
        : tasks,
    [selectedActorAgentId, tasks],
  );
  const localDashboardSummary = useMemo(
    () => createLocalDashboardSummary(workspaceAgents, tasks),
    [tasks, workspaceAgents],
  );
  const taskDashboardSummary = summary ?? localDashboardSummary;
  const canSendActorChatMessage =
    selectedActor !== null &&
    actorChatInput.trim().length > 0 &&
    actorChatMutation.status !== "submitting";
  const canManageConnections = workspace?.myRole === "ADMIN";
  const slackConnectionIntegrations =
    slackConnectionState.status === "ready" || slackConnectionState.status === "error"
      ? slackConnectionState.integrations
      : EMPTY_SLACK_INTEGRATIONS;
  const githubConnectionCredentials =
    githubConnectionState.status === "ready" || githubConnectionState.status === "error"
      ? githubConnectionState.credentials
      : EMPTY_GITHUB_CREDENTIALS;
  const selectedGithubCredential =
    githubConnectionCredentials.find(
      (credential) => credential.credentialId === selectedGithubCredentialId,
    ) ??
    githubConnectionCredentials[0] ??
    null;
  const trimmedGithubDisplayName = githubDisplayNameInput.trim();
  const trimmedGithubToken = githubTokenInput.trim();
  const isCreatingGithubCredential =
    githubConnectionCreateMutation.status === "submitting";
  const hasGithubCredentialAction = githubConnectionAction !== null;
  const canCreateGithubCredential =
    canManageConnections &&
    trimmedGithubDisplayName.length > 0 &&
    trimmedGithubToken.length > 0 &&
    !isCreatingGithubCredential &&
    !hasGithubCredentialAction;
  const canUpdateGithubCredential =
    canManageConnections &&
    selectedGithubCredential !== null &&
    githubEditDisplayNameInput.trim().length > 0 &&
    !hasGithubCredentialAction;
  const normalizedConnectionGatewayForm = useMemo(
    () =>
      normalizeGatewayForm({
        gatewayUrl: gatewayUrlInput,
        token: gatewayTokenInput,
        validateConnection: true,
      }),
    [gatewayTokenInput, gatewayUrlInput],
  );
  const isTestingConnectionGateway = gatewayConnectionTestState.status === "submitting";
  const isBindingConnectionGateway = gatewayBindingMutation.status === "submitting";
  const canSubmitConnectionGateway =
    canManageConnections &&
    normalizedConnectionGatewayForm.isValid &&
    !isTestingConnectionGateway &&
    !isBindingConnectionGateway;

  const handleOpenSlackInstallUrl = async () => {
    if (!canManageConnections || !isValidWorkspaceId(numericWorkspaceId)) {
      return;
    }

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
  };

  const handleDeleteSlackConnection = async (integration: SlackIntegration) => {
    if (!canManageConnections || slackConnectionAction) {
      return;
    }

    setSlackConnectionAction({ type: "delete", integrationId: integration.id });
    setSlackConnectionMutation({ status: "submitting", message: "Slack 연동 삭제 중" });

    try {
      await deleteSlackIntegration(resolveAccessToken(), numericWorkspaceId, integration.id);
      const nextIntegrations = slackConnectionIntegrations.filter(
        (currentIntegration) => currentIntegration.id !== integration.id,
      );
      if (typeof window !== "undefined") {
        if (nextIntegrations[0]) {
          writeStoredSlackIntegration(window.localStorage, numericWorkspaceId, nextIntegrations[0]);
        } else {
          deleteStoredSlackIntegration(window.localStorage, numericWorkspaceId);
        }
      }
      setSlackConnectionState({ status: "ready", integrations: nextIntegrations, error: null });
      setSlackConnectionMutation({
        status: "success",
        message: "Slack 연동이 삭제되었습니다.",
      });
    } catch (error) {
      setSlackConnectionMutation({
        status: "error",
        message: messageFromError(error, "Slack 연동을 삭제하지 못했습니다."),
      });
    } finally {
      setSlackConnectionAction(null);
    }
  };

  const handleCreateGithubConnection = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!canManageConnections) {
      return;
    }

    if (!trimmedGithubDisplayName || !trimmedGithubToken) {
      setGithubConnectionCreateMutation({
        status: "error",
        message: "이름과 token을 모두 입력해 주세요.",
      });
      return;
    }

    setGithubConnectionCreateMutation({
      status: "submitting",
      message: "GitHub 자격 증명 등록 중",
    });

    try {
      const createdCredential = await createGithubCredential(
        resolveAccessToken(),
        numericWorkspaceId,
        {
          displayName: trimmedGithubDisplayName,
          token: trimmedGithubToken,
        },
      );
      setGithubConnectionState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              credentials: [createdCredential, ...previous.credentials],
            }
          : { status: "ready", credentials: [createdCredential], error: null },
      );
      setSelectedGithubCredentialId(createdCredential.credentialId);
      if (typeof window !== "undefined") {
        writeStoredGithubCredential(window.localStorage, numericWorkspaceId, createdCredential);
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
  };

  const startGithubCredentialEdit = (credential: GithubCredential) => {
    if (!canManageConnections || hasGithubCredentialAction) {
      return;
    }

    setSelectedGithubCredentialId(credential.credentialId);
    setEditingGithubCredentialId(credential.credentialId);
    setGithubEditDisplayNameInput(credential.displayName);
    setGithubEditTokenInput("");
    setGithubConnectionActionMutation({ status: "idle", message: null });
  };

  const cancelGithubCredentialEdit = () => {
    if (hasGithubCredentialAction) {
      return;
    }

    setEditingGithubCredentialId(null);
    setGithubEditDisplayNameInput("");
    setGithubEditTokenInput("");
  };

  const handleUpdateGithubConnection = async (
    event: FormEvent<HTMLFormElement>,
    credential: GithubCredential,
  ) => {
    event.preventDefault();

    if (!canManageConnections || hasGithubCredentialAction) {
      return;
    }

    const nextDisplayName = githubEditDisplayNameInput.trim();
    const nextToken = githubEditTokenInput.trim();
    if (!nextDisplayName) {
      setGithubConnectionActionMutation({
        status: "error",
        message: "표시 이름을 입력해 주세요.",
      });
      return;
    }

    setGithubConnectionAction({ type: "update", credentialId: credential.credentialId });
    setGithubConnectionActionMutation({
      status: "submitting",
      message: "GitHub 자격 증명 수정 중",
    });

    try {
      const updatedCredential = await updateGithubCredential(
        resolveAccessToken(),
        numericWorkspaceId,
        credential.credentialId,
        {
          displayName: nextDisplayName,
          token: nextToken || undefined,
        },
      );
      setGithubConnectionState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              credentials: previous.credentials.map((currentCredential) =>
                currentCredential.credentialId === updatedCredential.credentialId
                  ? updatedCredential
                  : currentCredential,
              ),
            }
          : previous,
      );
      setEditingGithubCredentialId(null);
      setGithubEditDisplayNameInput("");
      setGithubEditTokenInput("");
      if (typeof window !== "undefined") {
        writeStoredGithubCredential(window.localStorage, numericWorkspaceId, updatedCredential);
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
  };

  const handleDeleteGithubConnection = async (credential: GithubCredential) => {
    if (!canManageConnections || hasGithubCredentialAction) {
      return;
    }

    setGithubConnectionAction({ type: "delete", credentialId: credential.credentialId });
    setGithubConnectionActionMutation({
      status: "submitting",
      message: "GitHub 자격 증명 삭제 중",
    });

    try {
      await deleteGithubCredential(
        resolveAccessToken(),
        numericWorkspaceId,
        credential.credentialId,
      );
      const nextCredentials = githubConnectionCredentials.filter(
        (currentCredential) => currentCredential.credentialId !== credential.credentialId,
      );
      setGithubConnectionState((previous) =>
        previous.status === "ready"
          ? { ...previous, credentials: nextCredentials }
          : previous,
      );
      setSelectedGithubCredentialId(nextCredentials[0]?.credentialId ?? null);
      if (typeof window !== "undefined") {
        if (nextCredentials[0]) {
          writeStoredGithubCredential(window.localStorage, numericWorkspaceId, nextCredentials[0]);
        } else {
          deleteStoredGithubCredential(window.localStorage, numericWorkspaceId);
        }
      }
      if (editingGithubCredentialId === credential.credentialId) {
        setEditingGithubCredentialId(null);
        setGithubEditDisplayNameInput("");
        setGithubEditTokenInput("");
      }
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
  };

  const handleTestConnectionGateway = async () => {
    if (!canManageConnections) {
      return;
    }

    if (!normalizedConnectionGatewayForm.isValid) {
      setGatewayConnectionTestState({
        status: "error",
        result: null,
        message:
          normalizedConnectionGatewayForm.urlError ??
          normalizedConnectionGatewayForm.tokenError ??
          "OpenClaw Gateway 연결 정보를 확인해 주세요.",
      });
      return;
    }

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
        message: messageFromError(error, "OpenClaw Gateway 연결 테스트를 처리하지 못했습니다."),
      });
    }
  };

  const handleCreateConnectionGatewayBinding = async (
    event: FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();

    if (!canManageConnections) {
      return;
    }

    if (!normalizedConnectionGatewayForm.isValid) {
      setGatewayBindingMutation({
        status: "error",
        message:
          normalizedConnectionGatewayForm.urlError ??
          normalizedConnectionGatewayForm.tokenError ??
          "OpenClaw Gateway 연결 정보를 확인해 주세요.",
      });
      return;
    }

    setGatewayBindingMutation({
      status: "submitting",
      message: "OpenClaw Gateway binding 등록 중",
    });

    try {
      const binding = await createWorkspaceGatewayBinding(resolveAccessToken(), numericWorkspaceId, {
        gatewayUrl: normalizedConnectionGatewayForm.gatewayUrl,
        token: normalizedConnectionGatewayForm.token,
        validateConnection: normalizedConnectionGatewayForm.validateConnection,
      });
      let gateway = gatewayBindingToStatus(binding);
      try {
        gateway = normalizeWorkspaceGatewayStatus(
          await fetchWorkspaceGatewayStatus(resolveAccessToken(), numericWorkspaceId),
        );
      } catch {
        // 상태 재조회가 실패해도 방금 저장한 binding 응답으로 화면과 캐시를 갱신한다.
      }
      if (typeof window !== "undefined") {
        if (gateway.bound) {
          writeStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId, gateway);
        } else {
          deleteStoredOpenClawGatewayStatus(window.localStorage, numericWorkspaceId);
        }
      }
      setGatewayConnectionState({ status: "ready", gateway, error: null });
      setGatewayUrlInput(normalizedConnectionGatewayForm.gatewayUrl);
      setGatewayTokenInput("");
      setGatewayBindingMutation({
        status: "success",
        message: "OpenClaw Gateway 정보를 저장했습니다.",
      });
    } catch (error) {
      setGatewayBindingMutation({
        status: "error",
        message: messageFromError(error, "OpenClaw Gateway 정보를 저장하지 못했습니다."),
      });
    }
  };

  const updatePresencePosition = useCallback(
    async (
      position: WorkspacePresencePosition,
      successMessage = "Presence 위치가 갱신되었습니다.",
    ) => {
      if (!isValidWorkspaceId(numericWorkspaceId) || isUpdatingPresence) {
        return;
      }

      setPresenceMutation({ status: "submitting", message: null });
      try {
        await updateWorkspacePresencePosition(resolveAccessToken(), numericWorkspaceId, {
          position,
          status: "ACTIVE",
        });
        setPresenceMutation({
          status: "success",
          message: successMessage,
        });
      } catch (error) {
        setPresenceMutation({
          status: "error",
          message: messageFromError(error, "Presence 위치를 갱신하지 못했습니다."),
        });
      }
    },
    [isUpdatingPresence, numericWorkspaceId],
  );
  const defaultPlayerPosition = useMemo<StageActorPosition>(
    () => ({ left: "36.8%", top: "54%" }),
    [],
  );
  const playerPosition = playerPositionOverride ?? defaultPlayerPosition;
  const handlePlayerPositionChange = useCallback(
    (position: StageActorPosition) => {
      setPlayerPositionOverride(position);
      void updatePresencePosition(position, "2D workspace 위치가 갱신되었습니다.");
    },
    [updatePresencePosition],
  );

  const handleUpdatePresencePosition = async () => {
    if (!isValidWorkspaceId(numericWorkspaceId) || isUpdatingPresence) {
      return;
    }

    await updatePresencePosition({
      left: "workspace-detail",
      top: "task-board",
    });
  };

  const handleCreateInvite = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setInviteState({
        status: "error",
        invite: null,
        invitePath: null,
        inviteUrl: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    const email = inviteEmail.trim();
    if (!email) {
      setInviteState({
        status: "error",
        invite: null,
        invitePath: null,
        inviteUrl: null,
        error: "초대할 이메일을 입력해 주세요.",
      });
      return;
    }

    setInviteState({
      status: "submitting",
      invite: null,
      invitePath: null,
      inviteUrl: null,
      error: null,
    });
    setCopiedInvite(false);

    try {
      const invite = await createWorkspaceInvite(
        resolveAccessToken(),
        numericWorkspaceId,
        { targetEmail: email },
      );
      const invitePath = `/invites/${invite.token}`;
      const inviteUrl = `${window.location.origin}${invitePath}`;
      setInviteState({ status: "created", invite, invitePath, inviteUrl, error: null });
      setCopiedInvite(false);
      setInviteEmail("");
    } catch (error) {
      setInviteState({
        status: "error",
        invite: null,
        invitePath: null,
        inviteUrl: null,
        error: messageFromError(error, "초대 생성을 처리하지 못했습니다."),
      });
    }
  };

  const copyInviteUrl = async () => {
    if (inviteState.status !== "created") {
      return;
    }

    try {
      await window.navigator.clipboard?.writeText(inviteState.inviteUrl);
      setCopiedInvite(true);
    } catch {
      setCopiedInvite(false);
    }
  };

  return (
    <main className="workspace-detail-shell">
      <section className="workspace-detail-panel" aria-label="워크스페이스 상세">
        <header className="workspace-detail-header">
          <nav className="workspace-detail-actions" aria-label="워크스페이스 내비게이션">
            <Link className="secondary-action workspace-detail-back" href="/workspaces">
              <ArrowLeft aria-hidden="true" />
              <span>목록으로</span>
            </Link>
            {isValidWorkspaceId(numericWorkspaceId) ? (
              <Link
                className="secondary-action workspace-detail-back"
                href={`/workspaces/${numericWorkspaceId}/settings`}
              >
                <Settings aria-hidden="true" />
                <span>설정</span>
              </Link>
            ) : null}
            <button
              className="secondary-action workspace-detail-back"
              type="button"
              onClick={() => openConnectionModal("selector")}
              disabled={state.status !== "ready"}
            >
              <Settings aria-hidden="true" />
              <span>Connection settings</span>
            </button>
          </nav>
          <div className="workspace-detail-title">
            <div className="workspace-detail-icon">
              <Building2 aria-hidden="true" />
            </div>
            <div>
              <p className="eyebrow">Workspace</p>
              <h1>{workspace?.name ?? "워크스페이스 상세"}</h1>
            </div>
          </div>
        </header>

        {state.status === "loading" ? (
          <WorkspaceDetailStateMessage
            tone="neutral"
            icon={<Loader2 className="spin" aria-hidden="true" />}
            title="상세 확인 중"
            message="워크스페이스 정보를 불러오고 있습니다."
          />
        ) : null}

        {state.status === "error" || state.status === "offline" ? (
          <WorkspaceDetailStateMessage
            tone={state.status === "offline" ? "offline" : "danger"}
            icon={
              state.status === "offline" ? (
                <WifiOff aria-hidden="true" />
              ) : (
                <AlertCircle aria-hidden="true" />
              )
            }
            title={state.status === "offline" ? "연결 실패" : "상세 확인 실패"}
            message={state.error}
            action={
              <button
                className="secondary-action workspace-detail-retry"
                type="button"
                onClick={() => void reloadWorkspace()}
              >
                <RefreshCw aria-hidden="true" />
                <span>다시 시도</span>
              </button>
            }
          />
        ) : null}

        {workspace ? (
          <section className="workspace-detail-content" aria-label="워크스페이스 정보">
            <section className="workspace-office-scene workspace-2d-section" aria-label="워크스페이스 오피스 씬">
              <nav className="workspace-office-nav" aria-label="워크스페이스 오피스 씬 메뉴">
                {officeSceneNavItems.map((item) => {
                  const Icon = item.icon;
                  if (item.id === "leave-workspace") {
                    return (
                      <Link className="workspace-office-nav-item" href="/workspaces" key={item.id}>
                        <Icon aria-hidden="true" />
                        <span>{item.label}</span>
                      </Link>
                    );
                  }
                  return (
                    <button
                      className="workspace-office-nav-item"
                      type="button"
                      key={item.id}
                      onClick={() => handleOfficeSceneNavAction(item.id)}
                    >
                      <Icon aria-hidden="true" />
                      <span>{item.label}</span>
                    </button>
                  );
                })}
              </nav>

              <div className="workspace-office-hero">
                <div className="workspace-detail-summary">
                  <span className="workspace-detail-label">Workspace office</span>
                  <strong>{workspace.name}</strong>
                  <p>{workspace.description ?? "설명 없음"}</p>
                </div>
                <div className={`workspace-detail-readiness workspace-detail-readiness-${detailOverview.readiness}`}>
                  <ShieldCheck aria-hidden="true" />
                  <span>{formatWorkspaceDetailReadiness(detailOverview.readiness)}</span>
                </div>
              </div>

              <Workspace2DCanvas
                agents={workspaceAgents}
                members={members}
                presenceMembers={onlinePresenceMembers}
                playerPosition={playerPosition}
                playerAvatar={storedCharacterProfile}
                playerMemberId={currentMemberId}
                onPlayerPositionChange={handlePlayerPositionChange}
                onActorSelect={handleSelectActor}
                onOpenTasks={() => {
                  setTaskDashboardOpen(true);
                  setTaskDashboardTab("summary");
                }}
                onPresenceMove={(position) =>
                  updatePresencePosition(position, "2D workspace 위치가 갱신되었습니다.")
                }
              />

              <ActorTaskPanel
                open={actorPanelOpen}
                actor={selectedActor}
                tab={actorPanelTab}
                messages={actorChatMessages}
                input={actorChatInput}
                mutation={actorChatMutation}
                tasks={selectedActorTasks}
                canSend={canSendActorChatMessage}
                onTabChange={setActorPanelTab}
                onInputChange={(value) => {
                  setActorChatInput(value);
                  setActorChatMutation({ status: "idle", message: null });
                }}
                onSend={() => void handleSendActorChatMessage()}
                onClose={handleCloseActorPanel}
                onOpenDashboard={(task) => {
                  setSelectedDashboardTask(task);
                  setTaskDashboardTab("board");
                  setTaskDashboardOpen(true);
                }}
                onOpenReports={openTaskReports}
                onOpenChat={openTaskChat}
                onOpenPlan={(planId) => void openOrchestrationPlan(planId)}
              />

              <section className="workspace-detail-action-rail" aria-label="워크스페이스 상세 작업">
                {detailActionItems.map((item) => (
                  <WorkspaceDetailActionCard
                    item={item}
                    key={item.id}
                    onClick={() => {
                      if (item.id === "files") {
                        void openWorkspaceFiles();
                      }
                      if (item.id === "chat" && tasks[0]) {
                        void openTaskChat(tasks[0]);
                      }
                    }}
                  />
                ))}
              </section>
            </section>

            <section className="workspace-task-section" aria-label="워크스페이스 대시보드 요약">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Dashboard</span>
                  <h2>운영 요약</h2>
                </div>
                <button
                  className="secondary-action workspace-task-refresh"
                  type="button"
                  onClick={() => void reloadDashboardSummary()}
                  disabled={isDashboardLoading}
                >
                  {isDashboardLoading ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <RefreshCw aria-hidden="true" />
                  )}
                  <span>새로고침</span>
                </button>
              </div>

              {dashboardSummary.status === "error" ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{dashboardSummary.error}</span>
                </div>
              ) : null}

              {memberTaskStats.status === "error" ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{memberTaskStats.error}</span>
                </div>
              ) : null}

              {isDashboardLoading ? (
                <div className="workspace-task-notice">
                  <Loader2 className="spin" aria-hidden="true" />
                  <span>Dashboard 요약을 불러오고 있습니다.</span>
                </div>
              ) : null}

              {summary ? (
                <dl className="workspace-detail-meta">
                  <DetailMetaItem
                    icon={<Users aria-hidden="true" />}
                    label="Agent"
                    value={`${summary.agentCount}개 · 실행 ${summary.runningAgentCount}개`}
                  />
                  <DetailMetaItem
                    icon={<ShieldCheck aria-hidden="true" />}
                    label="Agent 상태"
                    value={`대기 ${summary.idleAgentCount}개 · 오류 ${summary.errorAgentCount}개`}
                  />
                  <DetailMetaItem
                    icon={<ListChecks aria-hidden="true" />}
                    label="Task"
                    value={`${summary.taskCount}개 · 진행 ${summary.runningTaskCount}개`}
                  />
                  <DetailMetaItem
                    icon={<AlertCircle aria-hidden="true" />}
                    label="Task 결과"
                    value={`완료 ${summary.completedTaskCount}개 · 실패 ${summary.failedTaskCount}개`}
                  />
                </dl>
              ) : !isDashboardLoading ? (
                <div className="workspace-member-empty">
                  <ListChecks aria-hidden="true" />
                  <strong>요약 데이터가 없습니다</strong>
                  <p>워크스페이스 작업이 집계되면 운영 요약이 표시됩니다.</p>
                </div>
              ) : null}

              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Recent</span>
                  <h2>최근 report와 log</h2>
                </div>
                <span className="workspace-count-badge">{recentReports.length + recentLogs.length}개</span>
              </div>

              {recentReports.length > 0 || recentLogs.length > 0 ? (
                <div className="workspace-member-list">
                  {recentReports.slice(0, 3).map((report) => (
                    <article className="workspace-member-row" key={`report-${report.executionId}-${report.createdAt}`}>
                      <div className="workspace-member-avatar">
                        <FileText aria-hidden="true" />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{report.summary ?? report.failureReason ?? "요약 없음"}</strong>
                        <p>
                          Task #{report.taskId} · Agent #{report.agentId} · {formatDateTime(report.createdAt)}
                        </p>
                      </div>
                      <div className="workspace-member-meta">
                        <span className="role-badge">{report.status}</span>
                        <span>Report</span>
                      </div>
                    </article>
                  ))}
                  {recentLogs.slice(0, 3).map((log) => (
                    <article className="workspace-member-row" key={`log-${log.executionId}-${log.createdAt}-${log.message}`}>
                      <div className="workspace-member-avatar">
                        <AlertCircle aria-hidden="true" />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{log.message}</strong>
                        <p>
                          Task #{log.taskId} · Agent #{log.agentId} · {formatDateTime(log.createdAt)}
                        </p>
                      </div>
                      <div className="workspace-member-meta">
                        <span className="role-badge">{log.level}</span>
                        <span>Log</span>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="workspace-member-empty">
                  <FileText aria-hidden="true" />
                  <strong>최근 기록이 없습니다</strong>
                  <p>Agent 실행 report나 log가 생성되면 이곳에 표시됩니다.</p>
                </div>
              )}

              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Score</span>
                  <h2>멤버 작업 점수</h2>
                </div>
                <span className="workspace-count-badge">{memberStats.length}명</span>
              </div>

              {memberStats.length > 0 ? (
                <div className="workspace-member-list">
                  {memberStats.map((stat) => (
                    <article className="workspace-member-row" key={stat.memberId}>
                      <div className="workspace-member-avatar">
                        <PixelAvatar kind={pickAvatarKind(stat.memberName)} size={2} />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{stat.memberName}</strong>
                        <p>
                          Task {stat.taskCount} · 완료 {stat.completedTaskCount} · 진행 {stat.runningTaskCount} · 실패{" "}
                          {stat.failedTaskCount}
                        </p>
                      </div>
                      <div className="workspace-member-meta">
                        <span className="role-badge">#{stat.rank}</span>
                        <span>
                          총점 {formatScore(stat.totalScore)} · health {formatScore(stat.healthScore)}
                        </span>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="workspace-member-empty">
                  <Users aria-hidden="true" />
                  <strong>멤버 작업 통계가 없습니다</strong>
                  <p>멤버별 Task 기록이 집계되면 순위와 점수가 표시됩니다.</p>
                </div>
              )}
            </section>

            <section id="workspace-hire-agent" className="workspace-member-section" aria-label="워크스페이스 Agent">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Agents</span>
                  <h2>Agent 목록</h2>
                </div>
                <div className="workspace-task-actions">
                  <span className="workspace-count-badge">{workspaceAgents.length}개</span>
                  <button
                    className="secondary-action workspace-task-refresh"
                    type="button"
                    onClick={() => void reloadAgents()}
                    disabled={agentState.status === "loading"}
                  >
                    {agentState.status === "loading" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <RefreshCw aria-hidden="true" />
                    )}
                    <span>새로고침</span>
                  </button>
                </div>
              </div>

              {agentState.status === "error" ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{agentState.error}</span>
                </div>
              ) : null}

              {agentState.status === "loading" ? (
                <div className="workspace-task-notice">
                  <Loader2 className="spin" aria-hidden="true" />
                  <span>Agent 목록을 불러오고 있습니다.</span>
                </div>
              ) : null}

              <form className="workspace-invite-form" aria-label="Agent 등록" onSubmit={handleCreateAgent}>
                <label className="form-field">
                  <span>
                    <strong>Agent 이름</strong>
                    <small>필수</small>
                  </span>
                  <input
                    type="text"
                    value={agentCreateForm.name}
                    onChange={(event) =>
                      setAgentCreateForm((current) => ({ ...current, name: event.target.value }))
                    }
                    placeholder="Agent 이름"
                    disabled={isAgentMutating}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>카테고리</strong>
                    <small>{formatAgentCategory(agentCreateForm.category)}</small>
                  </span>
                  <select
                    value={agentCreateForm.category}
                    onChange={(event) =>
                      setAgentCreateForm((current) => ({
                        ...current,
                        category: event.target.value as WorkspaceAgentCategory,
                      }))
                    }
                    disabled={isAgentMutating}
                  >
                    {AGENT_CATEGORY_OPTIONS.map((category) => (
                      <option value={category} key={category}>
                        {formatAgentCategory(category)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="form-field">
                  <span>
                    <strong>Workspace path</strong>
                    <small>선택</small>
                  </span>
                  <input
                    type="text"
                    value={agentCreateForm.workspacePath}
                    onChange={(event) =>
                      setAgentCreateForm((current) => ({ ...current, workspacePath: event.target.value }))
                    }
                    placeholder="/workspace"
                    disabled={isAgentMutating}
                  />
                </label>

                {agentMutation.status === "error" ? (
                  <div className="form-notice form-notice-error" role="alert">
                    <AlertCircle aria-hidden="true" />
                    <span>{agentMutation.message}</span>
                  </div>
                ) : null}

                {agentMutation.status === "success" ? (
                  <div className="form-notice form-notice-success" role="status">
                    <ShieldCheck aria-hidden="true" />
                    <span>{agentMutation.message}</span>
                  </div>
                ) : null}

                <button
                  className="primary-action primary-action-active workspace-invite-submit"
                  type="submit"
                  disabled={!canCreateAgent}
                >
                  {isAgentMutating && agentMutation.agentId === null ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <Send aria-hidden="true" />
                  )}
                  <span>{isAgentMutating && agentMutation.agentId === null ? "등록 중" : "Agent 등록"}</span>
                </button>
              </form>

              {workspaceAgents.length > 0 ? (
                <div className="workspace-member-list">
                  {workspaceAgents.map((agent) => (
                    <article className="workspace-member-row" key={agent.agentId}>
                      <div className="workspace-member-avatar">
                        <PixelAvatar kind="agent" size={2} walking={agent.status === "READY"} />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{agent.name}</strong>
                        <p>
                          Agent #{agent.agentId} · {formatAgentCategory(agent.category)} · {agent.workspacePath}
                        </p>
                      </div>
                      <div className="workspace-member-meta">
                        <span className="role-badge">{formatAgentStatus(agent.status)}</span>
                        <span>{agent.skillFiles.length} skill</span>
                        <button
                          className="secondary-action workspace-task-refresh"
                          type="button"
                          onClick={() => void handleDeleteAgent(agent)}
                          disabled={isAgentMutating}
                        >
                          {isAgentMutating && agentMutation.agentId === agent.agentId ? (
                            <Loader2 className="spin" aria-hidden="true" />
                          ) : (
                            <X aria-hidden="true" />
                          )}
                          <span>삭제</span>
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="workspace-member-empty">
                  <Users aria-hidden="true" />
                  <strong>표시할 Agent가 없습니다</strong>
                  <p>워크스페이스에 Agent가 등록되면 이곳에 표시됩니다.</p>
                </div>
              )}
            </section>

            <section className="workspace-invite-section" aria-label="Task 생성">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Task</span>
                  <h2>Task 생성</h2>
                </div>
                <span className="workspace-count-badge">{tasks.length}개</span>
              </div>

              <form className="workspace-invite-form" onSubmit={handleCreateTask}>
                <label className="form-field">
                  <span>
                    <strong>제목</strong>
                    <small>필수</small>
                  </span>
                  <input
                    type="text"
                    value={taskCreateForm.title}
                    onChange={(event) =>
                      setTaskCreateForm((current) => ({ ...current, title: event.target.value }))
                    }
                    placeholder="작업 제목"
                    disabled={isTaskMutating}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>설명</strong>
                    <small>선택</small>
                  </span>
                  <textarea
                    value={taskCreateForm.description}
                    onChange={(event) =>
                      setTaskCreateForm((current) => ({ ...current, description: event.target.value }))
                    }
                    placeholder="Agent에게 전달할 작업 내용을 입력하세요."
                    disabled={isTaskMutating}
                    rows={3}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>유형</strong>
                    <small>{formatTaskType(taskCreateForm.type)}</small>
                  </span>
                  <select
                    value={taskCreateForm.type}
                    onChange={(event) =>
                      setTaskCreateForm((current) => ({
                        ...current,
                        type: event.target.value as TaskType,
                      }))
                    }
                    disabled={isTaskMutating}
                  >
                    {TASK_TYPE_OPTIONS.map((type) => (
                      <option value={type} key={type}>
                        {formatTaskType(type)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="form-field">
                  <span>
                    <strong>우선순위</strong>
                    <small>{formatTaskPriority(taskCreateForm.priority)}</small>
                  </span>
                  <select
                    value={taskCreateForm.priority}
                    onChange={(event) =>
                      setTaskCreateForm((current) => ({
                        ...current,
                        priority: event.target.value as TaskPriority,
                      }))
                    }
                    disabled={isTaskMutating}
                  >
                    {TASK_PRIORITY_OPTIONS.map((priority) => (
                      <option value={priority} key={priority}>
                        {formatTaskPriority(priority)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="form-field">
                  <span>
                    <strong>Agent ID</strong>
                    <small>선택</small>
                  </span>
                  <input
                    type="number"
                    min="1"
                    value={taskCreateForm.assignedAgentId}
                    onChange={(event) =>
                      setTaskCreateForm((current) => ({ ...current, assignedAgentId: event.target.value }))
                    }
                    placeholder="자동 배정"
                    disabled={isTaskMutating}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>실행 옵션</strong>
                    <small>Task 실행 버튼에 적용</small>
                  </span>
                  <span className="workspace-invite-help">
                    <input
                      type="checkbox"
                      checked={createPr}
                      onChange={(event) => setCreatePr(event.target.checked)}
                      disabled={isTaskMutating}
                    />
                    실행 시 PR 생성을 요청합니다.
                  </span>
                </label>

                {taskMutation.status === "error" ? (
                  <div className="form-notice form-notice-error" role="alert">
                    <AlertCircle aria-hidden="true" />
                    <span>{taskMutation.message}</span>
                  </div>
                ) : null}

                {taskMutation.status === "success" ? (
                  <div className="form-notice form-notice-success" role="status">
                    <ShieldCheck aria-hidden="true" />
                    <span>{taskMutation.message}</span>
                  </div>
                ) : null}

                <button
                  className="primary-action primary-action-active workspace-invite-submit"
                  type="submit"
                  disabled={!canCreateTask}
                >
                  {isTaskMutating && taskMutation.taskId === null ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <Send aria-hidden="true" />
                  )}
                  <span>{isTaskMutating && taskMutation.taskId === null ? "생성 중" : "Task 생성"}</span>
                </button>
              </form>
            </section>

            <section id="workspace-tasks" className="workspace-task-section" aria-label="워크스페이스 Task">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Tasks</span>
                  <h2>작업 보드</h2>
                </div>
                <div className="workspace-task-actions">
                  <button
                    className="secondary-action workspace-task-refresh"
                    type="button"
                    onClick={() => void openWorkspaceFiles()}
                    disabled={artifactTreeState.status === "loading"}
                  >
                    <FolderTree aria-hidden="true" />
                    <span>파일</span>
                  </button>
                  <button
                    className="secondary-action workspace-task-refresh"
                    type="button"
                    onClick={() => void reloadTasks()}
                    disabled={taskState.status === "loading"}
                  >
                    {taskState.status === "loading" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <RefreshCw aria-hidden="true" />
                    )}
                    <span>새로고침</span>
                  </button>
                </div>
              </div>

              {taskState.status === "error" ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{taskState.error}</span>
                </div>
              ) : null}

              {taskState.status === "loading" ? (
                <div className="workspace-task-notice">
                  <Loader2 className="spin" aria-hidden="true" />
                  <span>Task 목록을 불러오고 있습니다.</span>
                </div>
              ) : null}

              <div className="workspace-task-board">
                {groupedTasks.map((group) => (
                  <article className={`workspace-task-column workspace-task-column-${group.tone}`} key={group.key}>
                    <div className="workspace-task-column-header">
                      <strong>{group.label}</strong>
                      <span>{group.tasks.length}</span>
                    </div>
                    {group.tasks.length > 0 ? (
                      <div className="workspace-task-list">
                        {group.tasks.map((task) => (
                          <TaskCard
                            task={task}
                            key={task.taskId}
                            busy={isTaskMutating && taskMutation.taskId === task.taskId}
                            onOpenChat={openTaskChat}
                            onOpenReports={openTaskReports}
                            onRunTask={handleRunTask}
                            onUpdateStatus={handleUpdateTaskStatus}
                          />
                        ))}
                      </div>
                    ) : (
                      <div className="workspace-task-empty">
                        <ListChecks aria-hidden="true" />
                        <span>표시할 Task가 없습니다.</span>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            </section>

            <dl className="workspace-detail-meta">
              <DetailMetaItem
                icon={<ShieldCheck aria-hidden="true" />}
                label="내 역할"
                value={<span className="role-badge">{formatRole(workspace.myRole)}</span>}
              />
              <DetailMetaItem
                icon={<UserRound aria-hidden="true" />}
                label="생성자"
                value={workspace.creatorGoogleSub}
              />
              <DetailMetaItem
                icon={<CalendarDays aria-hidden="true" />}
                label="참여 일시"
                value={formatDateTime(workspace.joinedAt)}
              />
              <DetailMetaItem
                icon={<Hash aria-hidden="true" />}
                label="Workspace ID"
                value={workspace.workspaceId}
              />
            </dl>

            <section className="workspace-member-section" aria-label="워크스페이스 멤버">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Members</span>
                  <h2>멤버 목록</h2>
                </div>
                <span className="workspace-count-badge">{members.length}명</span>
              </div>

              {members.length > 0 ? (
                <div className="workspace-member-list">
                  {members.map((member) => {
                    const isCurrentMember = currentMemberId != null && member.memberId === currentMemberId;
                    const memberAvatar =
                      isCurrentMember && storedCharacterProfile
                        ? storedCharacterProfile
                        : memberProfileToStoredCharacter({ avatarKind: pickAvatarKind(member.name) });
                    return (
                      <article className="workspace-member-row" key={member.memberId}>
                        <div className="workspace-member-avatar">
                          <PixelAvatar
                            kind={memberAvatar?.kind ?? pickAvatarKind(member.name)}
                            size={2}
                            paletteOverride={memberAvatar?.paletteOverride}
                          />
                        </div>
                        <div className="workspace-member-main">
                          <strong>{member.name}</strong>
                          <p>{member.email}</p>
                        </div>
                        <div className="workspace-member-meta">
                          <span className="role-badge">{formatRole(member.role)}</span>
                          <span>{formatDateTime(member.joinedAt)}</span>
                        </div>
                      </article>
                    );
                  })}
                </div>
              ) : (
                <div className="workspace-member-empty">
                  <Users aria-hidden="true" />
                  <strong>표시할 멤버가 없습니다</strong>
                  <p>워크스페이스에 참여한 멤버가 생기면 이곳에 표시됩니다.</p>
                </div>
              )}
            </section>

            <section className="workspace-invite-section" aria-label="워크스페이스 초대 생성">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Invite</span>
                  <h2>멤버 초대</h2>
                </div>
                <span className={canCreateInvite ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {canCreateInvite ? "ADMIN" : "MEMBER"}
                </span>
              </div>

              <form className="workspace-invite-form" onSubmit={handleCreateInvite}>
                <label className="form-field">
                  <span>
                    <strong>초대 이메일</strong>
                    <small>{canCreateInvite ? "7일 유효" : "관리자 전용"}</small>
                  </span>
                  <input
                    type="email"
                    value={inviteEmail}
                    onChange={(event) => setInviteEmail(event.target.value)}
                    placeholder="member@example.com"
                    disabled={!canCreateInvite || isCreatingInvite}
                  />
                </label>

                {inviteState.status === "error" ? (
                  <div className="form-notice form-notice-error" role="alert">
                    <AlertCircle aria-hidden="true" />
                    <span>{inviteState.error}</span>
                  </div>
                ) : null}

                {inviteState.status === "created" ? (
                  <div className="workspace-invite-result" role="status">
                    <Mail aria-hidden="true" />
                    <div>
                      <strong>{formatInviteTarget(inviteState.invite)}</strong>
                      <p>{inviteState.inviteUrl}</p>
                      <div className="workspace-invite-actions">
                        <Link className="secondary-action workspace-invite-open" href={inviteState.invitePath}>
                          초대 열기
                        </Link>
                        <button
                          className="secondary-action workspace-invite-copy"
                          type="button"
                          onClick={() => void copyInviteUrl()}
                        >
                          <Copy aria-hidden="true" />
                          <span>{copiedInvite ? "복사됨" : "복사"}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ) : null}

                {!canCreateInvite ? (
                  <p className="workspace-invite-help">
                    관리자 역할만 새 초대를 생성할 수 있습니다.
                  </p>
                ) : null}

                <button
                  className="primary-action primary-action-active workspace-invite-submit"
                  type="submit"
                  disabled={!canSubmitInvite}
                >
                  {isCreatingInvite ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <Send aria-hidden="true" />
                  )}
                  <span>{isCreatingInvite ? "생성 중" : "초대 생성"}</span>
                </button>
              </form>
            </section>
          </section>
        ) : null}
      </section>

      <aside className="workspace-detail-side" aria-label="조회 정보">
        <div>
          <UserRound aria-hidden="true" />
          <strong>{resolveAccessToken() ? "설정됨" : "미설정"}</strong>
          <span>개발 access token</span>
        </div>
        <div>
          <Hash aria-hidden="true" />
          <strong>{isValidWorkspaceId(numericWorkspaceId) ? numericWorkspaceId : "-"}</strong>
          <span>요청 워크스페이스 ID</span>
        </div>
        <div>
          <ShieldCheck aria-hidden="true" />
          <strong>{members.length}명</strong>
          <span>멤버 {members.length}명 · 관리자 {adminCount}명</span>
        </div>
        <div>
          <UserRound aria-hidden="true" />
          <strong>{workspaceAgents.length}개</strong>
          <span>READY {readyAgentCount}개 · 전체 Agent</span>
        </div>
        <div>
          <ListChecks aria-hidden="true" />
          <strong>{tasks.length}개</strong>
          <span>Task board 표시 항목</span>
        </div>
        <WorkspacePresencePanel
          liveState={presenceLiveState}
          events={presenceEvents}
          mutation={presenceMutation}
          isUpdating={isUpdatingPresence}
          disabled={state.status !== "ready"}
          onUpdate={() => void handleUpdatePresencePosition()}
        />
      </aside>

      <AgentHireModal
        open={agentHireOpen}
        form={agentCreateForm}
        mutation={agentMutation}
        submitting={isAgentMutating}
        canSubmit={canCreateAgent}
        onClose={() => setAgentHireOpen(false)}
        onSubmit={handleCreateAgent}
        onNameChange={(value) => {
          setAgentCreateForm((current) => ({ ...current, name: value }));
          setAgentMutation({ status: "idle", message: null, agentId: null });
        }}
        onCategoryChange={(value) => {
          setAgentCreateForm((current) => ({ ...current, category: value }));
          setAgentMutation({ status: "idle", message: null, agentId: null });
        }}
        onWorkspacePathChange={(value) => {
          setAgentCreateForm((current) => ({ ...current, workspacePath: value }));
          setAgentMutation({ status: "idle", message: null, agentId: null });
        }}
        onEmojiChange={(value) => {
          setAgentCreateForm((current) => ({ ...current, emoji: value }));
          setAgentMutation({ status: "idle", message: null, agentId: null });
        }}
        onSkillFileAdd={addAgentSkillFile}
        onSkillFileChange={updateAgentSkillFile}
        onSkillFileRemove={removeAgentSkillFile}
      />
      <ConnectionSettingsModal
        modal={connectionModal}
        canManage={canManageConnections}
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
        canCreateGithubCredential={canCreateGithubCredential}
        canUpdateGithubCredential={canUpdateGithubCredential}
        gatewayState={gatewayConnectionState}
        gatewayUrlInput={gatewayUrlInput}
        gatewayTokenInput={gatewayTokenInput}
        gatewayTestState={gatewayConnectionTestState}
        gatewayBindingMutation={gatewayBindingMutation}
        canSubmitGateway={canSubmitConnectionGateway}
        onOpen={openConnectionModal}
        onClose={() => setConnectionModal(null)}
        onBack={() => setConnectionModal("selector")}
        onReloadSlack={() => void loadSlackConnections()}
        onOpenSlackInstall={() => void handleOpenSlackInstallUrl()}
        onDeleteSlack={(integration) => void handleDeleteSlackConnection(integration)}
        onReloadGithub={() => void loadGithubCredentials()}
        onCreateGithub={handleCreateGithubConnection}
        onSelectGithubCredential={setSelectedGithubCredentialId}
        onStartGithubEdit={startGithubCredentialEdit}
        onCancelGithubEdit={cancelGithubCredentialEdit}
        onUpdateGithub={handleUpdateGithubConnection}
        onDeleteGithub={(credential) => void handleDeleteGithubConnection(credential)}
        onGithubDisplayNameChange={(value) => {
          setGithubDisplayNameInput(value);
          setGithubConnectionCreateMutation({ status: "idle", message: null });
        }}
        onGithubTokenChange={(value) => {
          setGithubTokenInput(value);
          setGithubConnectionCreateMutation({ status: "idle", message: null });
        }}
        onGithubEditDisplayNameChange={(value) => {
          setGithubEditDisplayNameInput(value);
          setGithubConnectionActionMutation({ status: "idle", message: null });
        }}
        onGithubEditTokenChange={(value) => {
          setGithubEditTokenInput(value);
          setGithubConnectionActionMutation({ status: "idle", message: null });
        }}
        onReloadGateway={() => void loadGatewayConnection()}
        onGatewayUrlChange={(value) => {
          setGatewayUrlInput(value);
          setGatewayBindingMutation({ status: "idle", message: null });
          setGatewayConnectionTestState({ status: "idle", result: null, message: null });
        }}
        onGatewayTokenChange={(value) => {
          setGatewayTokenInput(value);
          setGatewayBindingMutation({ status: "idle", message: null });
          setGatewayConnectionTestState({ status: "idle", result: null, message: null });
        }}
        onTestGateway={() => void handleTestConnectionGateway()}
        onBindGateway={handleCreateConnectionGatewayBinding}
      />
      <TaskDashboardModal
        open={taskDashboardOpen}
        tab={taskDashboardTab}
        summary={taskDashboardSummary}
        apiSummary={summary}
        localSummary={localDashboardSummary}
        groupedTasks={groupedTasks}
        selectedTask={selectedDashboardTask}
        onTabChange={setTaskDashboardTab}
        onSelectTask={setSelectedDashboardTask}
        onOpenReports={openTaskReports}
        onOpenChat={openTaskChat}
        onClose={() => {
          setTaskDashboardOpen(false);
          setSelectedDashboardTask(null);
        }}
      />
      <TaskReportsModal
        task={selectedTask}
        reports={taskReports}
        state={reportState}
        onClose={() => {
          setSelectedTask(null);
          setTaskReports([]);
          setReportState({ status: "idle", error: null });
        }}
        onOpenArtifact={(path, name) => void openArtifactFile(path, name)}
      />
      <TaskChatModal
        task={selectedChatTask}
        messages={taskChatMessages}
        state={taskChatState}
        messageInput={taskChatMessageInput}
        agentIdInput={taskChatAgentIdInput}
        agents={workspaceAgents}
        mutation={taskChatMutation}
        canSend={canSendTaskChatMessage}
        hasMoreMessages={taskChatHasMore}
        paginationState={taskChatPaginationState}
        onMessageChange={(value) => {
          setTaskChatMessageInput(value);
          setTaskChatMutation({ status: "idle", message: null });
        }}
        onAgentChange={(value) => {
          setTaskChatAgentIdInput(value);
          setTaskChatMutation({ status: "idle", message: null });
        }}
        onSubmit={handleSendTaskChatMessage}
        onLoadMoreMessages={() => void loadMoreTaskChatMessages()}
        onReload={(task) => void openTaskChat(task)}
        onClose={() => {
          setSelectedChatTask(null);
          setTaskChatMessages([]);
          setTaskChatState({ status: "idle", error: null });
          setTaskChatMessageInput("");
          setTaskChatAgentIdInput("");
          setTaskChatMutation({ status: "idle", message: null });
          setTaskChatSessionId(null);
          setTaskChatCursor(null);
          setTaskChatHasMore(false);
          setTaskChatPaginationState({ status: "idle", message: null });
        }}
      />
      <OrchestrationPlanModal
        plan={orchestrationPlan}
        state={orchestrationPlanState}
        onClose={() => {
          setOrchestrationPlan(null);
          setOrchestrationPlanState({ status: "idle", error: null });
        }}
        onOpenFile={(path, name) => void openArtifactFile(path, name)}
      />
      <ArtifactTreeModal
        open={filesOpen}
        tree={artifactTree}
        state={artifactTreeState}
        expandedArtifactPaths={expandedArtifactPaths}
        notice={artifactNotice}
        onClose={() => {
          setFilesOpen(false);
          setArtifactTree(null);
          setExpandedArtifactPaths(new Set());
          setArtifactTreeState({ status: "idle", error: null });
          setArtifactNotice(null);
        }}
        onReload={() => void openWorkspaceFiles()}
        onOpenFile={(path) => void openArtifactFile(path)}
        onToggleDirectory={toggleArtifactDirectory}
      />
      <ArtifactFileModal
        path={artifactFilePath}
        nameHint={artifactFileNameHint}
        file={artifactFile}
        state={artifactFileState}
        notice={artifactNotice}
        onClose={() => {
          setArtifactFile(null);
          setArtifactFilePath(null);
          setArtifactFileNameHint(null);
          setArtifactFileState({ status: "idle", error: null });
          setArtifactNotice(null);
        }}
      />
    </main>
  );
}

function WorkspaceDetailActionCard({
  item,
  onClick,
}: {
  item: WorkspaceDetailActionItem;
  onClick: () => void;
}) {
  const clickable = item.id === "files" || (item.id === "chat" && item.state === "ready");

  return (
    <button
      className={`workspace-detail-action-card workspace-detail-action-card-${item.tone}`}
      type="button"
      onClick={onClick}
      disabled={!clickable || item.state === "locked"}
    >
      <span>{item.label}</span>
      <strong>{item.value}</strong>
      <small>{formatWorkspaceDetailActionState(item.state)}</small>
    </button>
  );
}

function ConnectionSettingsModal({
  modal,
  canManage,
  slackState,
  slackIntegrations,
  slackMutation,
  slackAction,
  githubState,
  githubCredentials,
  selectedGithubCredential,
  selectedGithubCredentialId,
  editingGithubCredentialId,
  githubDisplayNameInput,
  githubTokenInput,
  githubEditDisplayNameInput,
  githubEditTokenInput,
  githubCreateMutation,
  githubActionMutation,
  githubAction,
  canCreateGithubCredential,
  canUpdateGithubCredential,
  gatewayState,
  gatewayUrlInput,
  gatewayTokenInput,
  gatewayTestState,
  gatewayBindingMutation,
  canSubmitGateway,
  onOpen,
  onClose,
  onBack,
  onReloadSlack,
  onOpenSlackInstall,
  onDeleteSlack,
  onReloadGithub,
  onCreateGithub,
  onSelectGithubCredential,
  onStartGithubEdit,
  onCancelGithubEdit,
  onUpdateGithub,
  onDeleteGithub,
  onGithubDisplayNameChange,
  onGithubTokenChange,
  onGithubEditDisplayNameChange,
  onGithubEditTokenChange,
  onReloadGateway,
  onGatewayUrlChange,
  onGatewayTokenChange,
  onTestGateway,
  onBindGateway,
}: {
  modal: ConnectionModalKind;
  canManage: boolean;
  slackState: SlackConnectionState;
  slackIntegrations: SlackIntegration[];
  slackMutation: ConnectionMutationState;
  slackAction: SlackConnectionAction | null;
  githubState: GithubConnectionState;
  githubCredentials: GithubCredential[];
  selectedGithubCredential: GithubCredential | null;
  selectedGithubCredentialId: number | null;
  editingGithubCredentialId: number | null;
  githubDisplayNameInput: string;
  githubTokenInput: string;
  githubEditDisplayNameInput: string;
  githubEditTokenInput: string;
  githubCreateMutation: ConnectionMutationState;
  githubActionMutation: ConnectionMutationState;
  githubAction: GithubConnectionAction | null;
  canCreateGithubCredential: boolean;
  canUpdateGithubCredential: boolean;
  gatewayState: GatewayConnectionState;
  gatewayUrlInput: string;
  gatewayTokenInput: string;
  gatewayTestState: GatewayConnectionTestState;
  gatewayBindingMutation: ConnectionMutationState;
  canSubmitGateway: boolean;
  onOpen: (kind: Exclude<ConnectionModalKind, null>) => void;
  onClose: () => void;
  onBack: () => void;
  onReloadSlack: () => void;
  onOpenSlackInstall: () => void;
  onDeleteSlack: (integration: SlackIntegration) => void;
  onReloadGithub: () => void;
  onCreateGithub: (event: FormEvent<HTMLFormElement>) => void;
  onSelectGithubCredential: (credentialId: number) => void;
  onStartGithubEdit: (credential: GithubCredential) => void;
  onCancelGithubEdit: () => void;
  onUpdateGithub: (event: FormEvent<HTMLFormElement>, credential: GithubCredential) => void;
  onDeleteGithub: (credential: GithubCredential) => void;
  onGithubDisplayNameChange: (value: string) => void;
  onGithubTokenChange: (value: string) => void;
  onGithubEditDisplayNameChange: (value: string) => void;
  onGithubEditTokenChange: (value: string) => void;
  onReloadGateway: () => void;
  onGatewayUrlChange: (value: string) => void;
  onGatewayTokenChange: (value: string) => void;
  onTestGateway: () => void;
  onBindGateway: (event: FormEvent<HTMLFormElement>) => void;
}) {
  if (!modal) {
    return null;
  }

  const title = connectionModalTitle(modal);
  const isSlackLoading = slackState.status === "loading";
  const isGithubLoading = githubState.status === "loading";
  const isGatewayLoading = gatewayState.status === "loading";
  const currentGateway =
    gatewayState.status === "ready" || gatewayState.status === "error"
      ? gatewayState.gateway
      : null;

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section
        className={`workspace-modal workspace-connection-modal workspace-connection-modal-${modal}${
          modal === "selector" ? " workspace-connection-selector-modal" : ""
        }`}
        role="dialog"
        aria-modal="true"
        aria-label="Connection settings"
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">Connections</span>
            <h2>{title}</h2>
          </div>
          <div className="workspace-modal-actions">
            {modal !== "selector" ? (
              <button
                className="secondary-action workspace-task-refresh"
                type="button"
                onClick={onBack}
              >
                <ArrowLeft aria-hidden="true" />
                <span>뒤로</span>
              </button>
            ) : null}
            <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
              <X aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="workspace-modal-body">
          {modal === "selector" ? (
            <div className="workspace-connection-grid" aria-label="Connection targets">
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="Slack"
                onClick={() => onOpen("slack")}
              >
                <KeyRound aria-hidden="true" />
                <span>Slack</span>
                <strong>설치와 연동 목록</strong>
              </button>
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="Discord"
                onClick={() => onOpen("discord")}
              >
                <Hash aria-hidden="true" />
                <span>Discord</span>
                <strong>준비 중</strong>
              </button>
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="GitHub"
                onClick={() => onOpen("github")}
              >
                <GitBranch aria-hidden="true" />
                <span>GitHub</span>
                <strong>Token 자격 증명</strong>
              </button>
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="AI / OpenClaw"
                onClick={() => onOpen("gateway")}
              >
                <ShieldCheck aria-hidden="true" />
                <span>AI / OpenClaw</span>
                <strong>OpenClaw Gateway와 Agent 실행 환경</strong>
              </button>
            </div>
          ) : null}

          {modal !== "selector" && !canManage ? (
            <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
              관리자 역할만 연결 설정을 변경할 수 있습니다.
            </ConnectionNotice>
          ) : null}

          {modal === "slack" ? (
            <>
              <div className="workspace-modal-summary">
                <strong>Slack 설치</strong>
                <p>설치 진입 시 workspace와 return 위치를 저장하고 연동 목록을 갱신합니다.</p>
              </div>
              <div className="workspace-invite-management-toolbar">
                <button
                  className="secondary-action workspace-invite-refresh"
                  type="button"
                  onClick={onOpenSlackInstall}
                  disabled={!canManage || slackMutation.status === "submitting"}
                >
                  {slackMutation.status === "submitting" ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <KeyRound aria-hidden="true" />
                  )}
                  <span>{slackMutation.status === "submitting" ? "확인 중" : "Slack 설치 열기"}</span>
                </button>
                <button
                  className="secondary-action workspace-invite-refresh"
                  type="button"
                  onClick={onReloadSlack}
                  disabled={isSlackLoading}
                >
                  <RefreshCw className={isSlackLoading ? "spin" : undefined} aria-hidden="true" />
                  <span>목록 새로고침</span>
                </button>
              </div>
              {slackMutation.status === "error" && slackMutation.message ? (
                <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {slackMutation.message}
                </ConnectionNotice>
              ) : null}
              {slackMutation.status === "success" && slackMutation.message ? (
                <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                  {slackMutation.message}
                </ConnectionNotice>
              ) : null}
              {slackState.status === "loading" ? (
                <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="Slack 연동 목록 확인 중" />
              ) : null}
              {slackState.status === "error" ? (
                <ModalState icon={<AlertCircle aria-hidden="true" />} text={slackState.error} tone="danger" />
              ) : null}
              {slackIntegrations.length > 0 ? (
                <div className="workspace-connection-list">
                  {slackIntegrations.map((integration) => {
                    const busy = slackAction?.integrationId === integration.id;
                    return (
                      <article className="workspace-connection-row" key={integration.id}>
                        <div className="workspace-member-main">
                          <strong>{integration.channelName ?? integration.slackChannelId}</strong>
                          <p>
                            Team {integration.slackTeamId} · Channel {integration.slackChannelId} ·{" "}
                            {integration.maskedBotToken}
                          </p>
                        </div>
                        <button
                          className="danger-action workspace-github-row-button"
                          type="button"
                          onClick={() => onDeleteSlack(integration)}
                          disabled={!canManage || Boolean(slackAction)}
                        >
                          {busy ? <Loader2 className="spin" aria-hidden="true" /> : <Trash2 aria-hidden="true" />}
                          <span>{busy ? "삭제 중" : "삭제"}</span>
                        </button>
                      </article>
                    );
                  })}
                </div>
              ) : slackState.status === "ready" ? (
                <div className="workspace-member-empty">
                  <KeyRound aria-hidden="true" />
                  <strong>Slack 연동이 없습니다</strong>
                  <p>설치 완료 후 연결 목록이 표시됩니다.</p>
                </div>
              ) : null}
            </>
          ) : null}

          {modal === "discord" ? (
            <div className="workspace-member-empty">
              <Hash aria-hidden="true" />
              <strong>Discord 준비 중</strong>
              <p>연결 흐름이 열리면 같은 back/close modal 패턴으로 제공됩니다.</p>
            </div>
          ) : null}

          {modal === "github" ? (
            <>
              <form className="workspace-connection-form" onSubmit={onCreateGithub}>
                <div className="workspace-gateway-form-grid">
                  <label className="form-field">
                    <span>
                      <strong>표시 이름</strong>
                      <small>{githubDisplayNameInput.length}/100</small>
                    </span>
                    <input
                      value={githubDisplayNameInput}
                      onChange={(event) => onGithubDisplayNameChange(event.target.value)}
                      disabled={!canManage || githubCreateMutation.status === "submitting"}
                    />
                  </label>
                  <label className="form-field">
                    <span>
                      <strong>Token</strong>
                      <small>필수</small>
                    </span>
                    <input
                      type="password"
                      value={githubTokenInput}
                      onChange={(event) => onGithubTokenChange(event.target.value)}
                      disabled={!canManage || githubCreateMutation.status === "submitting"}
                    />
                  </label>
                </div>
                {githubCreateMutation.status === "error" && githubCreateMutation.message ? (
                  <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {githubCreateMutation.message}
                  </ConnectionNotice>
                ) : null}
                {githubCreateMutation.status === "success" && githubCreateMutation.message ? (
                  <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                    {githubCreateMutation.message}
                  </ConnectionNotice>
                ) : null}
                <div className="workspace-settings-actions">
                  <button
                    className="primary-action primary-action-active"
                    type="submit"
                    disabled={!canCreateGithubCredential}
                  >
                    {githubCreateMutation.status === "submitting" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <KeyRound aria-hidden="true" />
                    )}
                    <span>{githubCreateMutation.status === "submitting" ? "등록 중" : "등록"}</span>
                  </button>
                </div>
              </form>

              <div className="workspace-invite-management-toolbar">
                <strong>GitHub 자격 증명</strong>
                <button
                  className="secondary-action workspace-invite-refresh"
                  type="button"
                  onClick={onReloadGithub}
                  disabled={isGithubLoading}
                >
                  <RefreshCw className={isGithubLoading ? "spin" : undefined} aria-hidden="true" />
                  <span>목록 새로고침</span>
                </button>
              </div>
              {githubActionMutation.status === "error" && githubActionMutation.message ? (
                <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {githubActionMutation.message}
                </ConnectionNotice>
              ) : null}
              {githubActionMutation.status === "success" && githubActionMutation.message ? (
                <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                  {githubActionMutation.message}
                </ConnectionNotice>
              ) : null}
              {githubState.status === "loading" ? (
                <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="GitHub 자격 증명 확인 중" />
              ) : null}
              {githubState.status === "error" ? (
                <ModalState icon={<AlertCircle aria-hidden="true" />} text={githubState.error} tone="danger" />
              ) : null}
              {selectedGithubCredential ? (
                <section className="workspace-modal-summary" aria-label="selected credential">
                  <strong>selected credential</strong>
                  <p>
                    {selectedGithubCredential.displayName} ·{" "}
                    <span className="workspace-github-token">{selectedGithubCredential.maskedToken}</span>
                  </p>
                </section>
              ) : null}
              {githubCredentials.length > 0 ? (
                <div className="workspace-connection-list">
                  {githubCredentials.map((credential) => {
                    const busy = githubAction?.credentialId === credential.credentialId;
                    const selected = selectedGithubCredentialId === credential.credentialId;
                    const editing = editingGithubCredentialId === credential.credentialId;

                    return (
                      <article className="workspace-connection-row" key={credential.credentialId}>
                        <button
                          className={`workspace-connection-select${selected ? " workspace-connection-select-active" : ""}`}
                          type="button"
                          aria-pressed={selected}
                          onClick={() => onSelectGithubCredential(credential.credentialId)}
                        >
                          <strong>{credential.displayName}</strong>
                          <span>{credential.maskedToken}</span>
                        </button>
                        <div className="workspace-github-row-actions">
                          <button
                            className="secondary-action workspace-github-row-button"
                            type="button"
                            onClick={() => onStartGithubEdit(credential)}
                            disabled={!canManage || Boolean(githubAction)}
                          >
                            <KeyRound aria-hidden="true" />
                            <span>수정</span>
                          </button>
                          <button
                            className="danger-action workspace-github-row-button"
                            type="button"
                            onClick={() => onDeleteGithub(credential)}
                            disabled={!canManage || Boolean(githubAction)}
                          >
                            {busy && githubAction?.type === "delete" ? (
                              <Loader2 className="spin" aria-hidden="true" />
                            ) : (
                              <Trash2 aria-hidden="true" />
                            )}
                            <span>{busy && githubAction?.type === "delete" ? "삭제 중" : "삭제"}</span>
                          </button>
                        </div>
                        {editing ? (
                          <form
                            className="workspace-github-edit-form"
                            onSubmit={(event) => onUpdateGithub(event, credential)}
                          >
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>표시 이름</strong>
                                <small>필수</small>
                              </span>
                              <input
                                value={githubEditDisplayNameInput}
                                onChange={(event) => onGithubEditDisplayNameChange(event.target.value)}
                                disabled={Boolean(githubAction)}
                              />
                            </label>
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>Token</strong>
                                <small>변경 시 입력</small>
                              </span>
                              <input
                                type="password"
                                value={githubEditTokenInput}
                                onChange={(event) => onGithubEditTokenChange(event.target.value)}
                                disabled={Boolean(githubAction)}
                              />
                            </label>
                            <div className="workspace-github-row-actions">
                              <button
                                className="primary-action primary-action-active workspace-github-row-button"
                                type="submit"
                                disabled={!canUpdateGithubCredential}
                              >
                                {busy && githubAction?.type === "update" ? (
                                  <Loader2 className="spin" aria-hidden="true" />
                                ) : (
                                  <ShieldCheck aria-hidden="true" />
                                )}
                                <span>{busy && githubAction?.type === "update" ? "저장 중" : "저장"}</span>
                              </button>
                              <button
                                className="secondary-action workspace-github-row-button"
                                type="button"
                                onClick={onCancelGithubEdit}
                                disabled={Boolean(githubAction)}
                              >
                                <X aria-hidden="true" />
                                <span>취소</span>
                              </button>
                            </div>
                          </form>
                        ) : null}
                      </article>
                    );
                  })}
                </div>
              ) : githubState.status === "ready" ? (
                <div className="workspace-member-empty">
                  <GitBranch aria-hidden="true" />
                  <strong>GitHub 자격 증명이 없습니다</strong>
                  <p>Agent가 저장소 작업을 수행할 token을 등록해 주세요.</p>
                </div>
              ) : null}
            </>
          ) : null}

          {modal === "gateway" ? (
            <>
              <form className="workspace-connection-form workspace-gateway-form" onSubmit={onBindGateway}>
                <div className="workspace-gateway-form-grid">
                  <label className="form-field">
                    <span>
                      <strong>Gateway URL</strong>
                      <small>http/https</small>
                    </span>
                    <input
                      aria-label="Gateway URL"
                      value={gatewayUrlInput}
                      onChange={(event) => onGatewayUrlChange(event.target.value)}
                      disabled={!canManage || gatewayBindingMutation.status === "submitting"}
                    />
                  </label>
                  <label className="form-field">
                    <span>
                      <strong>Token</strong>
                      <small>필수</small>
                    </span>
                    <input
                      aria-label="Token"
                      type="password"
                      value={gatewayTokenInput}
                      onChange={(event) => onGatewayTokenChange(event.target.value)}
                      disabled={!canManage || gatewayBindingMutation.status === "submitting"}
                    />
                  </label>
                </div>
                <div className="workspace-settings-actions workspace-gateway-actions">
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={onReloadGateway}
                    disabled={isGatewayLoading}
                  >
                    <RefreshCw className={isGatewayLoading ? "spin" : undefined} aria-hidden="true" />
                    <span>상태 새로고침</span>
                  </button>
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={onTestGateway}
                    disabled={!canSubmitGateway}
                  >
                    {gatewayTestState.status === "submitting" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <ShieldCheck aria-hidden="true" />
                    )}
                    <span>{gatewayTestState.status === "submitting" ? "테스트 중" : "연결 테스트"}</span>
                  </button>
                  <button
                    className="primary-action primary-action-active"
                    type="submit"
                    disabled={!canSubmitGateway}
                  >
                    {gatewayBindingMutation.status === "submitting" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <KeyRound aria-hidden="true" />
                    )}
                    <span>{gatewayBindingMutation.status === "submitting" ? "등록 중" : "OpenClaw binding 등록"}</span>
                  </button>
                </div>
                {gatewayTestState.status === "error" ? (
                  <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {gatewayTestState.message}
                  </ConnectionNotice>
                ) : null}
                {gatewayTestState.status === "success" ? (
                  <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                    {gatewayTestState.message}
                  </ConnectionNotice>
                ) : null}
                {gatewayBindingMutation.status === "error" && gatewayBindingMutation.message ? (
                  <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {gatewayBindingMutation.message}
                  </ConnectionNotice>
                ) : null}
                {gatewayBindingMutation.status === "success" && gatewayBindingMutation.message ? (
                  <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                    {gatewayBindingMutation.message}
                  </ConnectionNotice>
                ) : null}
              </form>

              {gatewayState.status === "error" ? (
                <ModalState icon={<AlertCircle aria-hidden="true" />} text={gatewayState.error} tone="danger" />
              ) : null}
              <div className="workspace-gateway-status-list" aria-label="Gateway status list">
                <div>
                  <span>Binding</span>
                  <strong>
                    {currentGateway ? gatewayStatusLabel(currentGateway.status) : "상태 없음"}
                  </strong>
                </div>
                <div>
                  <span>URL</span>
                  <strong>{(currentGateway?.gatewayUrl ?? gatewayUrlInput) || "-"}</strong>
                </div>
                <div>
                  <span>Last status</span>
                  <strong>
                    {currentGateway?.lastStatus
                      ? gatewayConnectionStatusLabel(currentGateway.lastStatus)
                      : "-"}
                  </strong>
                </div>
                <div>
                  <span>Connection test</span>
                  <strong>
                    {gatewayTestState.status === "success"
                      ? gatewayConnectionStatusLabel(gatewayTestState.result.status)
                      : "-"}
                  </strong>
                </div>
              </div>
            </>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function ConnectionNotice({
  tone,
  icon,
  children,
}: {
  tone: "success" | "danger";
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className={`workspace-settings-notice workspace-settings-notice-${tone}`} role={tone === "danger" ? "alert" : "status"}>
      {icon}
      <span>{children}</span>
    </div>
  );
}

function AgentHireModal({
  open,
  form,
  mutation,
  submitting,
  canSubmit,
  onClose,
  onSubmit,
  onNameChange,
  onCategoryChange,
  onWorkspacePathChange,
  onEmojiChange,
  onSkillFileAdd,
  onSkillFileChange,
  onSkillFileRemove,
}: {
  open: boolean;
  form: AgentCreateFormState;
  mutation: AgentMutationState;
  submitting: boolean;
  canSubmit: boolean;
  onClose: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onNameChange: (value: string) => void;
  onCategoryChange: (value: WorkspaceAgentCategory) => void;
  onWorkspacePathChange: (value: string) => void;
  onEmojiChange: (value: string) => void;
  onSkillFileAdd: () => void;
  onSkillFileChange: (id: string, field: "fileName" | "content", value: string) => void;
  onSkillFileRemove: (id: string) => void;
}) {
  if (!open) {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section
        className="workspace-modal workspace-agent-hire-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Agent hire"
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">Hire agent</span>
            <h2>Agent hire</h2>
          </div>
          <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
            <X aria-hidden="true" />
          </button>
        </header>

        <form className="workspace-modal-body workspace-agent-hire-form" onSubmit={onSubmit}>
          <div className="workspace-agent-hire-fields">
            <label className="form-field">
              <span>
                <strong>Agent 이름</strong>
              </span>
              <input
                autoFocus
                disabled={submitting}
                onChange={(event) => onNameChange(event.target.value)}
                placeholder="예: 백엔드 에이전트"
                value={form.name}
              />
            </label>

            <label className="form-field">
              <span>
                <strong>역할</strong>
                <small>{formatAgentCategory(form.category)}</small>
              </span>
              <select
                disabled={submitting}
                onChange={(event) => onCategoryChange(event.target.value as WorkspaceAgentCategory)}
                value={form.category}
              >
                {AGENT_CATEGORY_OPTIONS.map((category) => (
                  <option value={category} key={category}>
                    {formatAgentCategory(category)}
                  </option>
                ))}
              </select>
            </label>

            <label className="form-field">
              <span>
                <strong>Workspace Path</strong>
                <small>선택</small>
              </span>
              <input
                disabled={submitting}
                onChange={(event) => onWorkspacePathChange(event.target.value)}
                placeholder="/workspace/backend"
                value={form.workspacePath}
              />
            </label>

            <label className="form-field">
              <span>
                <strong>Emoji</strong>
                <small>선택</small>
              </span>
              <input
                disabled={submitting}
                onChange={(event) => onEmojiChange(event.target.value)}
                placeholder="🤖"
                value={form.emoji}
              />
            </label>

            <section className="workspace-agent-skill-files" aria-label="Skill 파일">
              <div className="workspace-section-heading">
                <div>
                  <span className="workspace-detail-label">Skill 파일</span>
                  <p>추가하지 않으면 서버 기본 템플릿만 사용합니다.</p>
                </div>
                <button
                  className="secondary-action workspace-task-refresh"
                  type="button"
                  onClick={onSkillFileAdd}
                  disabled={submitting}
                >
                  Skill 파일 추가
                </button>
              </div>

              {form.skillFiles.map((skillFile, index) => (
                <div className="workspace-agent-skill-file" key={skillFile.id}>
                  <div className="workspace-section-heading">
                    <span className="workspace-detail-label">Skill {index + 1}</span>
                    <button
                      className="secondary-action workspace-task-refresh"
                      type="button"
                      onClick={() => onSkillFileRemove(skillFile.id)}
                      disabled={submitting}
                    >
                      삭제
                    </button>
                  </div>
                  <input
                    disabled={submitting}
                    onChange={(event) => onSkillFileChange(skillFile.id, "fileName", event.target.value)}
                    placeholder="backend-agent.md"
                    value={skillFile.fileName}
                  />
                  <textarea
                    disabled={submitting}
                    onChange={(event) => onSkillFileChange(skillFile.id, "content", event.target.value)}
                    placeholder="# Role&#10;- 이 Agent가 따라야 할 작업 규칙을 작성하세요."
                    rows={5}
                    value={skillFile.content}
                  />
                </div>
              ))}
            </section>

            {mutation.status === "error" ? (
              <div className="form-notice form-notice-error" role="alert">
                <AlertCircle aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}
            {mutation.status === "success" ? (
              <div className="form-notice form-notice-success" role="status">
                <ShieldCheck aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}
          </div>

          <aside className="workspace-agent-hire-preview" aria-label="Agent hire preview">
            <div className="workspace-row-icon">
              <UserRound aria-hidden="true" />
            </div>
            {form.emoji.trim() ? <strong>{form.emoji.trim()}</strong> : null}
            <h3>{formatAgentCategory(form.category)}</h3>
            <p>OpenClaw 연동 후 workspace 안에 자동 배치됩니다.</p>
          </aside>

          <div className="workspace-modal-footer">
            <button className="primary-action primary-action-active" type="submit" disabled={!canSubmit}>
              {submitting ? <Loader2 className="spin" aria-hidden="true" /> : <UserRound aria-hidden="true" />}
              <span>고용</span>
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function ActorTaskPanel({
  open,
  actor,
  tab,
  messages,
  input,
  mutation,
  tasks,
  canSend,
  onTabChange,
  onInputChange,
  onSend,
  onClose,
  onOpenDashboard,
  onOpenReports,
  onOpenChat,
  onOpenPlan,
}: {
  open: boolean;
  actor: WorkspaceSceneNode | null;
  tab: ActorPanelTab;
  messages: ActorChatMessage[];
  input: string;
  mutation: ChatMutationState;
  tasks: WorkspaceTask[];
  canSend: boolean;
  onTabChange: (tab: ActorPanelTab) => void;
  onInputChange: (value: string) => void;
  onSend: () => void;
  onClose: () => void;
  onOpenDashboard: (task: WorkspaceTask) => void;
  onOpenReports: (task: WorkspaceTask) => void;
  onOpenChat: (task: WorkspaceTask) => void;
  onOpenPlan: (planId: number) => void;
}) {
  const isAgent = actor?.kind === "agent";
  const activeTab = isAgent ? tab : "chat";
  const actorMessagesEndRef = useRef<HTMLDivElement | null>(null);
  const sortedTasks = useMemo(() => [...tasks].sort(compareActorPanelTasks), [tasks]);

  useEffect(() => {
    if (!open || activeTab !== "chat") {
      return;
    }
    actorMessagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [activeTab, messages.length, mutation.status, open]);

  useEffect(() => {
    if (!open) {
      return;
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [onClose, open]);

  if (!open || !actor) {
    return null;
  }

  const actorAvatarKind = pickAvatarKind(actor.label, isAgent);

  return (
    <aside className="workspace-actor-panel" aria-label="Agent 대화와 Task 대시보드">
      <header className="workspace-actor-header">
        <div className="workspace-member-avatar">
          <PixelAvatar kind={actorAvatarKind} size={2} walking={isAgent} />
        </div>
        <div>
          <span className="workspace-detail-label">{isAgent ? "Agent actor" : "Member actor"}</span>
          <h2>{actor.label}</h2>
          <p>{actor.detail}</p>
        </div>
        <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="패널 닫기">
          <X aria-hidden="true" />
        </button>
      </header>

      <div className="workspace-actor-tabs" role="tablist" aria-label="Actor panel tabs">
        <button
          className={activeTab === "chat" ? "workspace-actor-tab-active" : ""}
          type="button"
          role="tab"
          aria-selected={activeTab === "chat"}
          onClick={() => onTabChange("chat")}
        >
          Chat
        </button>
        {isAgent ? (
          <button
            className={activeTab === "tasks" ? "workspace-actor-tab-active" : ""}
            type="button"
            role="tab"
            aria-selected={activeTab === "tasks"}
            onClick={() => onTabChange("tasks")}
          >
            Tasks
          </button>
        ) : null}
      </div>

      {activeTab === "chat" ? (
        <div className="workspace-actor-content" role="tabpanel">
          <div className="workspace-actor-chat-list" aria-label="Agent chat messages">
            {messages.length > 0 ? (
              messages.map((message) => (
                <article
                  className={`workspace-actor-message workspace-actor-message-${message.sender}`}
                  key={message.id}
                >
                  <strong>{actorChatSenderLabel(message.sender)}</strong>
                  <p>{message.text}</p>
                  {message.taskId || message.taskExecutionId || message.orchestrationPlanId ? (
                    <div className="workspace-actor-message-links">
                      {message.taskId ? <span className="role-badge">Task #{message.taskId}</span> : null}
                      {message.taskExecutionId ? (
                        <span className="role-badge">Execution #{message.taskExecutionId}</span>
                      ) : null}
                      {message.orchestrationPlanId ? (
                        <button
                          className="secondary-action workspace-task-refresh actor-message-plan"
                          type="button"
                          onClick={() => onOpenPlan(message.orchestrationPlanId ?? 0)}
                        >
                          <FileText aria-hidden="true" />
                          <span>Plan #{message.orchestrationPlanId}</span>
                        </button>
                      ) : null}
                    </div>
                  ) : null}
                  {message.optimistic ? <span>optimistic</span> : null}
                </article>
              ))
            ) : (
              <div className="workspace-actor-empty">
                <Send aria-hidden="true" />
                <strong>Agent 메시지가 없습니다</strong>
                <p>선택한 Agent에게 바로 요청을 보낼 수 있습니다.</p>
              </div>
            )}
            {mutation.status === "submitting" && isAgent ? (
              <article className="workspace-actor-message workspace-actor-message-agent" aria-label={`${actor.label} 응답 대기 중`}>
                <strong>Agent</strong>
                <p>응답을 기다리는 중...</p>
              </article>
            ) : null}
            <div ref={actorMessagesEndRef} />
          </div>

          <form
            className="workspace-actor-chat-form"
            onSubmit={(event) => {
              event.preventDefault();
              onSend();
            }}
          >
            <label className="form-field">
              <span>
                <strong>Agent 메시지</strong>
                <small>{input.length}/1000</small>
              </span>
              <textarea
                value={input}
                onChange={(event) => onInputChange(event.target.value)}
                maxLength={1000}
                rows={3}
                disabled={mutation.status === "submitting"}
              />
            </label>

            {mutation.status === "error" && mutation.message ? (
              <div className="workspace-task-notice" role="alert">
                <AlertCircle aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}
            {mutation.status === "success" && mutation.message ? (
              <div className="workspace-task-notice">
                <ShieldCheck aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}

            <button
              className="primary-action primary-action-active workspace-invite-submit"
              type="submit"
              disabled={!canSend}
            >
              {mutation.status === "submitting" ? (
                <Loader2 className="spin" aria-hidden="true" />
              ) : (
                <Send aria-hidden="true" />
              )}
              <span>{mutation.status === "submitting" ? "전송 중" : "Agent 메시지 전송"}</span>
            </button>
          </form>
        </div>
      ) : (
        <div className="workspace-actor-content" role="tabpanel">
          {sortedTasks.length > 0 ? (
            <div className="workspace-actor-task-list">
              {sortedTasks.map((task) => (
                <article className="workspace-actor-task-card" key={task.taskId}>
                  <div>
                    <strong>{task.title}</strong>
                    <p>{task.description ?? "설명 없음"}</p>
                  </div>
                  <div className="workspace-task-card-meta">
                    <span>{formatTaskStatus(task.status)}</span>
                    <span>{formatTaskPriority(task.priority)}</span>
                  </div>
                  <div className="workspace-task-actions">
                    <button
                      className="secondary-action workspace-task-refresh"
                      type="button"
                      onClick={() => onOpenDashboard(task)}
                    >
                      <ListChecks aria-hidden="true" />
                      <span>Dashboard</span>
                    </button>
                    <button
                      className="secondary-action workspace-task-refresh"
                      type="button"
                      onClick={() => onOpenReports(task)}
                    >
                      <FileText aria-hidden="true" />
                      <span>Report</span>
                    </button>
                    <button
                      className="secondary-action workspace-task-refresh"
                      type="button"
                      onClick={() => onOpenChat(task)}
                    >
                      <Send aria-hidden="true" />
                      <span>Chat</span>
                    </button>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="workspace-actor-empty">
              <ListChecks aria-hidden="true" />
              <strong>연결된 Task가 없습니다</strong>
              <p>Agent 배정 작업이 생성되면 이 패널에 표시됩니다.</p>
            </div>
          )}
        </div>
      )}
    </aside>
  );
}

function TaskDashboardModal({
  open,
  tab,
  summary,
  apiSummary,
  localSummary,
  groupedTasks,
  selectedTask,
  onTabChange,
  onSelectTask,
  onOpenReports,
  onOpenChat,
  onClose,
}: {
  open: boolean;
  tab: TaskDashboardTab;
  summary: WorkspaceDashboardSummary;
  apiSummary: WorkspaceDashboardSummary | null;
  localSummary: WorkspaceDashboardSummary;
  groupedTasks: Array<TaskGroup & { tasks: WorkspaceTask[] }>;
  selectedTask: WorkspaceTask | null;
  onTabChange: (tab: TaskDashboardTab) => void;
  onSelectTask: (task: WorkspaceTask) => void;
  onOpenReports: (task: WorkspaceTask) => void;
  onOpenChat: (task: WorkspaceTask) => void;
  onClose: () => void;
}) {
  if (!open) {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section
        className="workspace-modal workspace-task-dashboard-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Task dashboard"
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">Tasks</span>
            <h2>Task dashboard</h2>
          </div>
          <div className="workspace-modal-actions">
            <div className="workspace-actor-tabs" role="tablist" aria-label="Task dashboard tabs">
              <button
                className={tab === "summary" ? "workspace-actor-tab-active" : ""}
                type="button"
                role="tab"
                aria-selected={tab === "summary"}
                onClick={() => onTabChange("summary")}
              >
                Summary
              </button>
              <button
                className={tab === "board" ? "workspace-actor-tab-active" : ""}
                type="button"
                role="tab"
                aria-selected={tab === "board"}
                onClick={() => onTabChange("board")}
              >
                Board
              </button>
            </div>
            <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
              <X aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="workspace-modal-body">
          {tab === "summary" ? (
            <>
              <div className="workspace-dashboard-source-grid">
                <div>
                  <span>API summary</span>
                  <strong>{apiSummary ? `${apiSummary.taskCount} Tasks` : "없음"}</strong>
                </div>
                <div>
                  <span>Local fallback</span>
                  <strong>{`${localSummary.taskCount} Tasks`}</strong>
                </div>
              </div>
              <dl className="workspace-detail-meta">
                <DetailMetaItem
                  icon={<Users aria-hidden="true" />}
                  label="Agent"
                  value={`${summary.agentCount}개 · 실행 ${summary.runningAgentCount}개`}
                />
                <DetailMetaItem
                  icon={<ListChecks aria-hidden="true" />}
                  label="Task"
                  value={`${summary.taskCount}개 · 진행 ${summary.runningTaskCount}개`}
                />
                <DetailMetaItem
                  icon={<ShieldCheck aria-hidden="true" />}
                  label="완료"
                  value={`${summary.completedTaskCount}개`}
                />
                <DetailMetaItem
                  icon={<AlertCircle aria-hidden="true" />}
                  label="실패"
                  value={`${summary.failedTaskCount}개`}
                />
              </dl>
            </>
          ) : (
            <>
              <div className="workspace-task-dashboard-board">
                {groupedTasks.map((group) => (
                  <article
                    className={`workspace-task-column workspace-task-column-${group.tone}`}
                    key={group.key}
                  >
                    <div className="workspace-task-column-header">
                      <strong>{group.label}</strong>
                      <span>{group.tasks.length}</span>
                    </div>
                    {group.tasks.length > 0 ? (
                      <div className="workspace-task-list">
                        {group.tasks.map((task) => (
                          <button
                            className="workspace-task-card"
                            type="button"
                            key={task.taskId}
                            onClick={() => onSelectTask(task)}
                          >
                            <div className="workspace-task-card-title">
                              <strong>{task.title}</strong>
                              <span className={`task-status-badge task-status-${task.status.toLowerCase().replaceAll("_", "-")}`}>
                                {formatTaskStatus(task.status)}
                              </span>
                            </div>
                            {task.description ? <p>{task.description}</p> : null}
                          </button>
                        ))}
                      </div>
                    ) : (
                      <div className="workspace-task-empty">
                        <ListChecks aria-hidden="true" />
                        <span>Task 없음</span>
                      </div>
                    )}
                  </article>
                ))}
              </div>

              <section className="workspace-task-dashboard-detail" aria-label="selectedDashboardTask detail">
                {selectedTask ? (
                  <>
                    <div>
                      <span className="workspace-detail-label">Task #{selectedTask.taskId}</span>
                      <h3>{selectedTask.title}</h3>
                      <p>{selectedTask.description ?? "설명 없음"}</p>
                    </div>
                    <div className="workspace-task-actions">
                      <button
                        className="secondary-action workspace-task-refresh"
                        type="button"
                        onClick={() => onOpenReports(selectedTask)}
                      >
                        <FileText aria-hidden="true" />
                        <span>Report</span>
                      </button>
                      <button
                        className="secondary-action workspace-task-refresh"
                        type="button"
                        onClick={() => onOpenChat(selectedTask)}
                      >
                        <Send aria-hidden="true" />
                        <span>Chat</span>
                      </button>
                    </div>
                  </>
                ) : (
                  <div className="workspace-actor-empty">
                    <ListChecks aria-hidden="true" />
                    <strong>Task를 선택해 주세요</strong>
                    <p>Board에서 항목을 선택하면 상세 작업이 표시됩니다.</p>
                  </div>
                )}
              </section>
            </>
          )}
        </div>
      </section>
    </div>
  );
}

function WorkspacePresencePanel({
  liveState,
  events,
  mutation,
  isUpdating,
  disabled,
  onUpdate,
}: {
  liveState: PresenceLiveState;
  events: WorkspacePresenceMessage[];
  mutation: PresenceMutationState;
  isUpdating: boolean;
  disabled: boolean;
  onUpdate: () => void;
}) {
  const recentEvents = events.slice(0, 4);

  return (
    <div className="workspace-presence-panel" aria-label="실시간 Presence">
      <span aria-label="Workspace presence" hidden />
      <div className="workspace-presence-header">
        <Users aria-hidden="true" />
        <div>
          <strong>{formatPresenceConnectionState(liveState.connectionState)}</strong>
          <span>{liveState.summary}</span>
        </div>
      </div>

      <div className="workspace-presence-status-grid">
        <PresenceMetric label="Stream" value={formatPresenceConnectionState(liveState.connectionState)} />
        <PresenceMetric label="Online" value={`${liveState.onlineCount}명`} />
        <PresenceMetric
          label="Latest"
          value={liveState.lastEventType ? formatPresenceEventType(liveState.lastEventType) : "없음"}
        />
      </div>

      <div className="workspace-presence-member-grid" aria-label="Presence member positions">
        {liveState.members.length > 0 ? (
          liveState.members.slice(0, 4).map((member) => (
            <div className="workspace-presence-member-card" key={member.memberId}>
              <strong>{`Member #${member.memberId}`}</strong>
              <span>{member.status ?? "UNKNOWN"}</span>
              <p>{formatPresencePosition(member.position)}</p>
            </div>
          ))
        ) : (
          <div className="workspace-presence-empty">
            <span>online member 없음</span>
          </div>
        )}
      </div>

      <div className="workspace-presence-event-list" aria-label="Presence event log">
        {recentEvents.length > 0 ? (
          recentEvents.map((event, index) => (
            <span key={`${event.type}-${event.memberId ?? "workspace"}-${index}`}>
              {formatPresenceEventType(event.type)}
              {event.memberId ? ` · #${event.memberId}` : ""}
            </span>
          ))
        ) : (
          <span>event 없음</span>
        )}
      </div>

      <button
        className="secondary-action workspace-task-refresh"
        type="button"
        onClick={onUpdate}
        disabled={isUpdating || disabled}
      >
        {isUpdating ? (
          <Loader2 className="spin" aria-hidden="true" />
        ) : (
          <RefreshCw aria-hidden="true" />
        )}
        <span>{isUpdating ? "갱신 중" : "위치 갱신"}</span>
      </button>

      {mutation.status === "error" && mutation.message ? (
        <span className="workspace-presence-message workspace-presence-message-error">
          {mutation.message}
        </span>
      ) : null}
      {mutation.status === "success" && mutation.message ? (
        <span className="workspace-presence-message workspace-presence-message-success">
          {mutation.message}
        </span>
      ) : null}
    </div>
  );
}

function PresenceMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="workspace-presence-metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function TaskCard({
  task,
  busy,
  onOpenChat,
  onOpenReports,
  onRunTask,
  onUpdateStatus,
}: {
  task: WorkspaceTask;
  busy: boolean;
  onOpenChat: (task: WorkspaceTask) => void;
  onOpenReports: (task: WorkspaceTask) => void;
  onRunTask: (task: WorkspaceTask) => void;
  onUpdateStatus: (task: WorkspaceTask, status: TaskStatus) => void;
}) {
  return (
    <article className="workspace-task-card">
      <div className="workspace-task-card-title">
        <strong>{task.title}</strong>
        <span className={`task-status-badge task-status-${task.status.toLowerCase().replaceAll("_", "-")}`}>
          {formatTaskStatus(task.status)}
        </span>
      </div>
      {task.description ? <p>{task.description}</p> : null}
      <div className="workspace-task-card-meta">
        <span>{formatTaskType(task.type)}</span>
        <span>{formatTaskPriority(task.priority)}</span>
        <span>{formatDateTime(task.updatedAt ?? task.createdAt ?? "")}</span>
      </div>
      <div className="workspace-task-actions">
        <button
          className="secondary-action workspace-task-refresh"
          type="button"
          onClick={() => onOpenReports(task)}
          disabled={busy}
        >
          <FileText aria-hidden="true" />
          <span>Report</span>
        </button>
        <button
          className="secondary-action workspace-task-refresh"
          type="button"
          onClick={() => onOpenChat(task)}
          disabled={busy}
        >
          <Send aria-hidden="true" />
          <span>Chat</span>
        </button>
        <button
          className="secondary-action workspace-task-refresh"
          type="button"
          onClick={() => onRunTask(task)}
          disabled={busy || task.status === "COMPLETED" || task.status === "CANCELED"}
        >
          {busy ? <Loader2 className="spin" aria-hidden="true" /> : <Send aria-hidden="true" />}
          <span>{busy ? "처리 중" : "실행"}</span>
        </button>
        <label className="form-field">
          <span>
            <strong>상태</strong>
            <small>{formatTaskStatus(task.status)}</small>
          </span>
          <select
            value={task.status}
            onChange={(event) => onUpdateStatus(task, event.target.value as TaskStatus)}
            disabled={busy}
            aria-label="Task 상태 변경"
          >
            {TASK_STATUS_OPTIONS.map((status) => (
              <option value={status} key={status}>
                {formatTaskStatus(status)}
              </option>
            ))}
          </select>
        </label>
      </div>
    </article>
  );
}

function TaskReportsModal({
  task,
  reports,
  state,
  onClose,
  onOpenArtifact,
}: {
  task: WorkspaceTask | null;
  reports: AgentReport[];
  state: ModalLoadState;
  onClose: () => void;
  onOpenArtifact: (path: string, name?: string) => void;
}) {
  if (!task) {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal" role="dialog" aria-modal="true" aria-label="Task report">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">Task #{task.taskId}</span>
            <h2>Report</h2>
          </div>
          <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          <div className="workspace-modal-summary">
            <strong>{task.title}</strong>
            {task.description ? <p>{task.description}</p> : null}
          </div>

          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="Report를 불러오고 있습니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && reports.length === 0 ? (
            <ModalState icon={<FileText aria-hidden="true" />} text="아직 표시할 report가 없습니다." />
          ) : null}

          {reports.length > 0 ? (
            <div className="workspace-report-list">
              {reports.map((report) => (
                <article className="workspace-report-card" key={report.reportId}>
                  <div className="workspace-report-card-header">
                    <strong>Report #{report.reportId}</strong>
                    <span className={`task-status-badge task-status-${report.status.toLowerCase().replaceAll("_", "-")}`}>
                      {formatTaskStatus(report.status)}
                    </span>
                  </div>
                  {report.summary ? <p>{report.summary}</p> : null}
                  {report.recommendedAction ? (
                    <p className="workspace-report-action">{report.recommendedAction}</p>
                  ) : null}
                  {report.artifacts && report.artifacts.length > 0 ? (
                    <div className="workspace-report-artifacts">
                      {report.artifacts.map((artifact) => {
                        const path = artifactPath(artifact);
                        return (
                          <button
                            className="secondary-action workspace-report-artifact"
                            type="button"
                            key={artifact.artifactId}
                            onClick={() => path && onOpenArtifact(path, artifact.name)}
                            disabled={!path}
                          >
                            <FileText aria-hidden="true" />
                            <span>{artifact.name}</span>
                          </button>
                        );
                      })}
                    </div>
                  ) : null}
                </article>
              ))}
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function TaskChatModal({
  task,
  messages,
  state,
  messageInput,
  agentIdInput,
  agents,
  mutation,
  canSend,
  hasMoreMessages,
  paginationState,
  onMessageChange,
  onAgentChange,
  onSubmit,
  onLoadMoreMessages,
  onReload,
  onClose,
}: {
  task: WorkspaceTask | null;
  messages: ChatMessage[];
  state: ModalLoadState;
  messageInput: string;
  agentIdInput: string;
  agents: WorkspaceAgent[];
  mutation: ChatMutationState;
  canSend: boolean;
  hasMoreMessages: boolean;
  paginationState: TaskChatPaginationState;
  onMessageChange: (value: string) => void;
  onAgentChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onLoadMoreMessages: () => void;
  onReload: (task: WorkspaceTask) => void;
  onClose: () => void;
}) {
  if (!task) {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal" role="dialog" aria-modal="true" aria-label="Task chat 메시지">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">Task #{task.taskId}</span>
            <h2>Chat</h2>
          </div>
          <div className="workspace-modal-actions">
            <button
              className="secondary-action workspace-task-refresh"
              type="button"
              onClick={() => onReload(task)}
              disabled={state.status === "loading"}
            >
              {state.status === "loading" ? (
                <Loader2 className="spin" aria-hidden="true" />
              ) : (
                <RefreshCw aria-hidden="true" />
              )}
              <span>새로고침</span>
            </button>
            <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
              <X aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="workspace-modal-body">
          <div className="workspace-modal-summary">
            <strong>{task.title}</strong>
            {task.description ? <p>{task.description}</p> : null}
          </div>

          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="Chat 메시지를 불러오고 있습니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && messages.length === 0 ? (
            <ModalState icon={<Send aria-hidden="true" />} text="아직 표시할 chat 메시지가 없습니다." />
          ) : null}

          {messages.length > 0 ? (
            <div className="workspace-report-list">
              {messages.map((message) => (
                <article className="workspace-report-card" key={message.messageId}>
                  <div className="workspace-report-card-header">
                    <strong>{formatChatRole(message.role)}</strong>
                    <span>{formatDateTime(message.createdAt)}</span>
                  </div>
                  <p>{message.content}</p>
                  <div className="workspace-report-artifacts">
                    <span className="role-badge">Session #{message.chatSessionId}</span>
                    {message.taskExecutionId ? <span>Execution #{message.taskExecutionId}</span> : null}
                  </div>
                </article>
              ))}
            </div>
          ) : null}

          {hasMoreMessages ? (
            <button
              className="secondary-action workspace-task-refresh"
              type="button"
              onClick={onLoadMoreMessages}
              disabled={paginationState.status === "loading"}
            >
              {paginationState.status === "loading" ? (
                <Loader2 className="spin" aria-hidden="true" />
              ) : (
                <RefreshCw aria-hidden="true" />
              )}
              <span>{paginationState.status === "loading" ? "불러오는 중" : "다음 메시지 더 보기"}</span>
            </button>
          ) : null}
          {paginationState.status === "error" && paginationState.message ? (
            <div className="workspace-task-notice" role="alert">
              <AlertCircle aria-hidden="true" />
              <span>{paginationState.message}</span>
            </div>
          ) : null}

          <form className="workspace-invite-form" onSubmit={onSubmit}>
            <label className="form-field">
              <span>
                <strong>Agent</strong>
                <small>{agents.length}개</small>
              </span>
              <select
                value={agentIdInput}
                onChange={(event) => onAgentChange(event.target.value)}
                disabled={mutation.status === "submitting" || agents.length === 0}
              >
                <option value="">Agent 선택</option>
                {agents.map((agent) => (
                  <option value={agent.agentId} key={agent.agentId}>
                    {agent.name} · {formatAgentStatus(agent.status)}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-field">
              <span>
                <strong>Message</strong>
                <small>{messageInput.length}/1000</small>
              </span>
              <textarea
                value={messageInput}
                onChange={(event) => onMessageChange(event.target.value)}
                maxLength={1000}
                disabled={mutation.status === "submitting"}
              />
            </label>

            {mutation.status === "error" && mutation.message ? (
              <div className="workspace-task-notice" role="alert">
                <AlertCircle aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}
            {mutation.status === "success" && mutation.message ? (
              <div className="workspace-task-notice">
                <Send aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}

            <button
              className="primary-action primary-action-active workspace-invite-submit"
              type="submit"
              disabled={!canSend}
            >
              {mutation.status === "submitting" ? (
                <Loader2 className="spin" aria-hidden="true" />
              ) : (
                <Send aria-hidden="true" />
              )}
              <span>{mutation.status === "submitting" ? "전송 중" : "메시지 전송"}</span>
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}

function OrchestrationPlanModal({
  plan,
  state,
  onClose,
  onOpenFile,
}: {
  plan: OrchestrationPlanArtifact | null;
  state: ModalLoadState;
  onClose: () => void;
  onOpenFile: (path: string, name: string) => void;
}) {
  if (!plan && state.status === "idle") {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section
        className="workspace-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Orchestration plan artifact"
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">
              Plan {plan ? `#${plan.planId}` : ""}
            </span>
            <h2>Orchestration Plan</h2>
          </div>
          <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          {state.status === "loading" ? (
            <ModalState
              icon={<Loader2 className="spin" aria-hidden="true" />}
              text="계획을 불러오는 중입니다."
            />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && plan && plan.steps.length === 0 ? (
            <ModalState icon={<FileText aria-hidden="true" />} text="아직 등록된 step이 없습니다." />
          ) : null}

          {plan && plan.steps.length > 0 ? (
            <div className="workspace-report-list">
              {plan.steps.map((step) => (
                <article className="workspace-report-card" key={step.stepId}>
                  <div className="workspace-report-card-header">
                    <strong>
                      {step.sequenceNo}. {step.title}
                    </strong>
                    <span>{step.stepKey}</span>
                  </div>
                  {step.files.length > 0 ? (
                    <div className="workspace-report-artifacts">
                      {step.files.map((file) => (
                        <button
                          className="secondary-action workspace-report-artifact"
                          type="button"
                          key={file.path}
                          onClick={() => onOpenFile(file.path, file.name)}
                          disabled={!file.exists}
                        >
                          <FileText aria-hidden="true" />
                          <span>{file.name}</span>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <p>생성된 파일이 없습니다.</p>
                  )}
                </article>
              ))}
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function ArtifactTreeModal({
  open,
  tree,
  state,
  expandedArtifactPaths,
  notice,
  onClose,
  onReload,
  onOpenFile,
  onToggleDirectory,
}: {
  open: boolean;
  tree: ArtifactTree | null;
  state: ModalLoadState;
  expandedArtifactPaths: Set<string>;
  notice: string | null;
  onClose: () => void;
  onReload: () => void;
  onOpenFile: (path: string) => void;
  onToggleDirectory: (path: string) => void;
}) {
  if (!open) {
    return null;
  }

  const nodes = tree?.children ?? [];

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal" role="dialog" aria-modal="true" aria-label="Workspace files">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">{tree?.rootPath ?? "/"}</span>
            <h2>WORKSPACE FILES</h2>
          </div>
          <div className="workspace-modal-actions">
            <button
              className="secondary-action workspace-task-refresh"
              type="button"
              onClick={onReload}
              disabled={state.status === "loading"}
            >
              <RefreshCw aria-hidden="true" />
              <span>RELOAD</span>
            </button>
            <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
              <X aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="workspace-modal-body">
          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="파일 트리를 불러오는 중입니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {notice ? (
            <ModalState icon={<ShieldCheck aria-hidden="true" />} text={notice} />
          ) : null}
          {state.status === "ready" && nodes.length === 0 ? (
            <ModalState icon={<FolderTree aria-hidden="true" />} text="아직 생성된 파일이 없습니다." />
          ) : null}
          {nodes.length > 0 ? (
            <ArtifactTreeNodes
              nodes={nodes}
              depth={0}
              expandedArtifactPaths={expandedArtifactPaths}
              onOpenFile={onOpenFile}
              onToggleDirectory={onToggleDirectory}
            />
          ) : null}
        </div>
      </section>
    </div>
  );
}

function ArtifactTreeNodes({
  nodes,
  depth,
  expandedArtifactPaths,
  onOpenFile,
  onToggleDirectory,
}: {
  nodes: ArtifactNode[];
  depth: number;
  expandedArtifactPaths: Set<string>;
  onOpenFile: (path: string) => void;
  onToggleDirectory: (path: string) => void;
}) {
  return (
    <ul className="workspace-artifact-tree">
      {nodes.map((node) => {
        const directory = node.type === "DIRECTORY";
        const expanded = directory && expandedArtifactPaths.has(node.path);

        return (
          <li key={node.path}>
            <button
              className={[
                "workspace-artifact-row",
                directory ? "workspace-artifact-row-directory" : "",
                expanded ? "workspace-artifact-row-expanded" : "",
              ].filter(Boolean).join(" ")}
              type="button"
              aria-expanded={directory ? expanded : undefined}
              onClick={() => {
                if (directory) {
                  onToggleDirectory(node.path);
                  return;
                }
                onOpenFile(node.path);
              }}
              style={{ paddingLeft: 10 + depth * 16 }}
            >
              <span aria-hidden="true">{directory ? (expanded ? "▾" : "▸") : "·"}</span>
              <strong>{node.name}</strong>
              {typeof node.sizeBytes === "number" ? <small>{formatFileSize(node.sizeBytes)}</small> : null}
            </button>
            {directory && expanded && node.children && node.children.length > 0 ? (
              <ArtifactTreeNodes
                nodes={node.children}
                depth={depth + 1}
                expandedArtifactPaths={expandedArtifactPaths}
                onOpenFile={onOpenFile}
                onToggleDirectory={onToggleDirectory}
              />
            ) : null}
          </li>
        );
      })}
    </ul>
  );
}

function ArtifactFileModal({
  path,
  nameHint,
  file,
  state,
  notice,
  onClose,
}: {
  path: string | null;
  nameHint: string | null;
  file: ArtifactFileContent | null;
  state: ModalLoadState;
  notice: string | null;
  onClose: () => void;
}) {
  if (!path && state.status === "idle") {
    return null;
  }

  const modalTitle = file?.name ?? nameHint ?? path ?? "ARTIFACT";

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal workspace-file-modal" role="dialog" aria-modal="true" aria-label="Artifact file">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">{path ?? ""}</span>
            <h2>{modalTitle}</h2>
          </div>
          <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="파일을 불러오는 중입니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {notice ? (
            <ModalState icon={<ShieldCheck aria-hidden="true" />} text={notice} />
          ) : null}
          {file && isMarkdownArtifactFile(path, file) ? (
            <div className="workspace-file-markdown">
              <MarkdownContent content={file.content} />
            </div>
          ) : null}
          {file && !isMarkdownArtifactFile(path, file) ? (
            <pre className="workspace-file-content">{file.content}</pre>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function ModalState({
  icon,
  text,
  tone = "neutral",
}: {
  icon: ReactNode;
  text: string;
  tone?: "neutral" | "danger";
}) {
  return (
    <div className={`workspace-modal-state workspace-modal-state-${tone}`}>
      {icon}
      <span>{text}</span>
    </div>
  );
}

function WorkspaceDetailStateMessage({
  tone,
  icon,
  title,
  message,
  action,
}: {
  tone: "neutral" | "danger" | "offline";
  icon: ReactNode;
  title: string;
  message: string;
  action?: ReactNode;
}) {
  return (
    <div className={`workspace-detail-state workspace-detail-state-${tone}`}>
      {icon}
      <strong>{title}</strong>
      <p>{message}</p>
      {action}
    </div>
  );
}

function DetailMetaItem({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: ReactNode;
}) {
  return (
    <div>
      <dt>
        {icon}
        <span>{label}</span>
      </dt>
      <dd>{value}</dd>
    </div>
  );
}

function connectionModalTitle(modal: Exclude<ConnectionModalKind, null>): string {
  switch (modal) {
    case "selector":
      return "Connection settings";
    case "slack":
      return "Slack";
    case "discord":
      return "Discord";
    case "github":
      return "GitHub";
    case "gateway":
      return "AI / OpenClaw";
  }
}

function toFailureState(error: unknown, fallback: string): WorkspaceDetailState {
  if (error instanceof ApiError) {
    return {
      status: "error",
      workspace: null,
      error: error.message,
    };
  }

  return {
    status: "offline",
    workspace: null,
    error: fallback,
  };
}

function messageFromError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return fallback;
}

function pickAvatarKind(name: string, agent = false): PixelAvatarKind {
  if (agent) {
    return "agent";
  }
  const kinds: PixelAvatarKind[] = ["mira", "alex", "kenji", "yuna", "diego", "iris"];
  const hash = [...name].reduce((sum, char) => sum + char.charCodeAt(0), 0);
  return kinds[hash % kinds.length];
}

function memberProfileToStoredCharacter(profile?: {
  avatarKind?: string | null;
  avatarColors?: Partial<Record<AvatarColorKey, string>> | null;
}): StoredCharacterProfile | undefined {
  if (!profile?.avatarKind || !isPixelAvatarKind(profile.avatarKind)) {
    return undefined;
  }
  const { skin, hair, shirt } = profile.avatarColors ?? {};
  if (!skin && !hair && !shirt) {
    return { kind: profile.avatarKind };
  }
  return {
    kind: profile.avatarKind,
    paletteOverride: {
      ...(skin ? { skin } : {}),
      ...(hair ? { hair } : {}),
      ...(shirt ? { shirt } : {}),
    },
  };
}

function loadStoredCharacterProfile(): StoredCharacterProfile | null {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    const raw = window.localStorage.getItem(CHARACTER_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const saved = JSON.parse(raw) as {
      kind?: string;
      paletteOverride?: PixelAvatarPaletteOverride;
    };
    if (!saved.kind || !isPixelAvatarKind(saved.kind)) {
      return null;
    }
    return {
      kind: saved.kind,
      paletteOverride: saved.paletteOverride,
    };
  } catch {
    return null;
  }
}

function isPixelAvatarKind(value: string): value is PixelAvatarKind {
  return ["mira", "alex", "kenji", "yuna", "diego", "iris", "agent"].includes(value);
}

function defaultAgentIdForTask(task: WorkspaceTask, agents: WorkspaceAgent[]): string {
  if (typeof task.assignedAgentId === "number" && task.assignedAgentId > 0) {
    return String(task.assignedAgentId);
  }
  if (typeof task.assignedAgentId === "string" && task.assignedAgentId.trim()) {
    return task.assignedAgentId.trim();
  }

  const readyAgent = agents.find((agent) => agent.status === "READY");
  return String(readyAgent?.agentId ?? agents[0]?.agentId ?? "");
}

function mergeChatMessages(current: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const byId = new Map<number, ChatMessage>();
  for (const message of current) {
    byId.set(message.messageId, message);
  }
  for (const message of incoming) {
    byId.set(message.messageId, message);
  }
  return [...byId.values()].sort((left, right) => left.messageId - right.messageId);
}

function lastChatMessageId(messages: ChatMessage[]): number | null {
  return messages.length > 0 ? messages[messages.length - 1].messageId : null;
}

function isValidWorkspaceId(workspaceId: number): boolean {
  return Number.isInteger(workspaceId) && workspaceId > 0;
}

function parseOptionalPositiveInteger(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }

  const parsed = Number(trimmed);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

function agentIdFromActorNode(node: WorkspaceSceneNode): number | null {
  if (node.kind !== "agent" || !node.id.startsWith("agent-")) {
    return null;
  }

  const parsed = Number.parseInt(node.id.replace("agent-", ""), 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

function actorMessageFromChatMessage(message: ChatMessage): ActorChatMessage {
  return {
    id: `message-${message.messageId}`,
    sender: message.role === "USER" ? "user" : message.role === "SYSTEM" ? "system" : "agent",
    text: message.content,
    optimistic: false,
    taskId: message.taskId,
    taskExecutionId: message.taskExecutionId,
    orchestrationPlanId: message.orchestrationPlanId,
  };
}

function actorChatSenderLabel(sender: ActorChatMessage["sender"]): string {
  if (sender === "user") {
    return "Me";
  }
  if (sender === "system") {
    return "System";
  }
  return "Agent";
}

function isTaskAssignedToAgent(task: WorkspaceTask, agentId: number): boolean {
  const assignedAgentId =
    typeof task.assignedAgentId === "string"
      ? Number.parseInt(task.assignedAgentId, 10)
      : task.assignedAgentId;
  return assignedAgentId === agentId;
}

const ACTOR_PANEL_STATUS_ORDER: TaskStatus[] = [
  "IN_PROGRESS",
  "WAITING_USER",
  "ASSIGNED",
  "REQUESTED",
  "FAILED",
  "CANCELED",
  "COMPLETED",
];

const ACTOR_PANEL_PRIORITY_ORDER: TaskPriority[] = ["URGENT", "HIGH", "MEDIUM", "LOW"];

function compareActorPanelTasks(a: WorkspaceTask, b: WorkspaceTask): number {
  const statusDelta = ACTOR_PANEL_STATUS_ORDER.indexOf(a.status) - ACTOR_PANEL_STATUS_ORDER.indexOf(b.status);
  if (statusDelta !== 0) {
    return statusDelta;
  }

  const priorityDelta = actorPanelPriorityRank(a.priority) - actorPanelPriorityRank(b.priority);
  if (priorityDelta !== 0) {
    return priorityDelta;
  }

  const bTime = Date.parse(b.updatedAt ?? b.createdAt ?? "");
  const aTime = Date.parse(a.updatedAt ?? a.createdAt ?? "");
  return (Number.isFinite(bTime) ? bTime : 0) - (Number.isFinite(aTime) ? aTime : 0);
}

function actorPanelPriorityRank(priority: TaskPriority | undefined): number {
  const index = priority ? ACTOR_PANEL_PRIORITY_ORDER.indexOf(priority) : -1;
  return index >= 0 ? index : ACTOR_PANEL_PRIORITY_ORDER.length;
}

function createLocalDashboardSummary(
  agents: WorkspaceAgent[],
  tasks: WorkspaceTask[],
): WorkspaceDashboardSummary {
  const errorAgentCount = agents.filter((agent) =>
    agent.status === "ERROR" || agent.status === "SYNC_FAILED",
  ).length;
  const runningAgentCount = agents.filter((agent) => agent.status === "READY").length;
  const runningTaskStatuses: TaskStatus[] = ["ASSIGNED", "IN_PROGRESS", "WAITING_USER"];

  return {
    agentCount: agents.length,
    runningAgentCount,
    idleAgentCount: Math.max(0, agents.length - runningAgentCount - errorAgentCount),
    errorAgentCount,
    taskCount: tasks.length,
    runningTaskCount: tasks.filter((task) => runningTaskStatuses.includes(task.status)).length,
    completedTaskCount: tasks.filter((task) => task.status === "COMPLETED").length,
    failedTaskCount: tasks.filter((task) => task.status === "FAILED").length,
    recentReports: [],
    recentLogs: [],
  };
}

function formatRole(role: WorkspaceMemberRole): string {
  return role === "ADMIN" ? "관리자" : "멤버";
}

function formatAgentCategory(category: WorkspaceAgent["category"]): string {
  switch (category) {
    case "ORCHESTRATOR":
      return "오케스트레이터";
    case "BACKEND":
      return "백엔드";
    case "FRONTEND":
      return "프론트엔드";
    case "QA":
      return "QA";
    case "CUSTOM":
      return "커스텀";
  }
}

function formatAgentStatus(status: WorkspaceAgent["status"]): string {
  switch (status) {
    case "CREATING":
      return "생성 중";
    case "READY":
      return "준비됨";
    case "SYNC_FAILED":
      return "동기화 실패";
    case "ERROR":
      return "오류";
    case "DISABLED":
      return "비활성";
  }
}

function formatTaskStatus(status: TaskStatus): string {
  switch (status) {
    case "REQUESTED":
      return "요청됨";
    case "ASSIGNED":
      return "배정됨";
    case "IN_PROGRESS":
      return "진행 중";
    case "WAITING_USER":
      return "대기";
    case "COMPLETED":
      return "완료";
    case "FAILED":
      return "실패";
    case "CANCELED":
      return "취소";
  }
}

function formatTaskType(type?: TaskType): string {
  if (!type) {
    return "유형 없음";
  }
  return type.replaceAll("_", " ");
}

function formatTaskPriority(priority?: TaskPriority): string {
  if (!priority) {
    return "우선순위 없음";
  }
  if (priority === "URGENT") {
    return "긴급";
  }
  if (priority === "HIGH") {
    return "높음";
  }
  if (priority === "MEDIUM") {
    return "보통";
  }
  return "낮음";
}

function formatChatRole(role: ChatMessage["role"]): string {
  if (role === "USER") {
    return "사용자";
  }
  if (role === "ASSISTANT") {
    return "Agent";
  }
  return "System";
}

function formatPresenceConnectionState(state: PresenceConnectionState): string {
  if (state === "online") {
    return "연결됨";
  }
  if (state === "connecting") {
    return "연결 중";
  }
  if (state === "error") {
    return "오류";
  }
  return "대기";
}

function formatPresencePosition(position: WorkspacePresencePosition | null): string {
  if (!position) {
    return "좌표 없음";
  }
  return `${position.left} · ${position.top}`;
}

function formatPresenceEventType(type: WorkspacePresenceMessage["type"]): string {
  switch (type) {
    case "presence.snapshot":
      return "snapshot";
    case "presence.join":
      return "join";
    case "presence.leave":
      return "leave";
    case "presence.position":
      return "position";
    case "presence.error":
      return "error";
  }
}

function formatWorkspaceDetailReadiness(readiness: "empty" | "ready" | "attention"): string {
  switch (readiness) {
    case "attention":
      return "주의 필요";
    case "empty":
      return "준비 중";
    case "ready":
      return "운영 가능";
  }
}

function formatWorkspaceDetailActionState(state: "ready" | "empty" | "locked"): string {
  switch (state) {
    case "empty":
      return "대기";
    case "locked":
      return "잠김";
    case "ready":
      return "준비";
  }
}

function formatScore(value: number): string {
  if (!Number.isFinite(value)) {
    return "-";
  }
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function artifactPath(artifact: TaskArtifact): string | null {
  if (artifact.url) {
    try {
      const url = new URL(artifact.url, "http://local");
      return url.searchParams.get("path");
    } catch {
      return null;
    }
  }

  if (artifact.artifactType === "FILE_PATH" || artifact.artifactType === "RESULT_FILE") {
    return artifact.name;
  }

  return null;
}

function defaultExpandedArtifactPaths(tree: ArtifactTree): Set<string> {
  return new Set(tree.children.filter((node) => node.type === "DIRECTORY").map((node) => node.path));
}

function toggleExpandedArtifactPath(current: Set<string>, path: string): Set<string> {
  const next = new Set(current);
  if (next.has(path)) {
    next.delete(path);
  } else {
    next.add(path);
  }
  return next;
}

function demoWorkspaceArtifactTree(workspaceId: number): ArtifactTree {
  return {
    workspaceId,
    rootPath: "/",
    children: [
      {
        name: "reports",
        path: "reports",
        type: "DIRECTORY",
        children: [
          {
            name: "fallback.md",
            path: "reports/fallback.md",
            type: "FILE",
            contentType: "text/markdown",
            sizeBytes: 168,
          },
          {
            name: "daily",
            path: "reports/daily",
            type: "DIRECTORY",
            children: [
              {
                name: "summary.md",
                path: "reports/daily/summary.md",
                type: "FILE",
                contentType: "text/markdown",
                sizeBytes: 144,
              },
            ],
          },
          {
            name: "logs",
            path: "reports/logs",
            type: "DIRECTORY",
            children: [
              {
                name: "plain-output.log",
                path: "reports/logs/plain-output.log",
                type: "FILE",
                contentType: "text/plain",
                sizeBytes: 96,
              },
              {
                name: "missing-file.log",
                path: "reports/logs/missing-file.log",
                type: "FILE",
                contentType: "text/plain",
                sizeBytes: 0,
              },
            ],
          },
        ],
      },
    ],
  };
}

function demoWorkspaceArtifactFile(
  workspaceId: number,
  path: string,
): ArtifactFileContent | null {
  const name = path.split("/").pop() || "fallback.md";
  const fallbackFiles: Record<string, Pick<ArtifactFileContent, "contentType" | "sizeBytes" | "content">> = {
    "reports/fallback.md": {
      contentType: "text/markdown",
      sizeBytes: 168,
      content: "# Demo workspace artifact\n\n파일 API 응답 대신 demo workspace 파일을 표시합니다.",
    },
    "reports/daily/summary.md": {
      contentType: "text/markdown",
      sizeBytes: 144,
      content: "# Daily summary\n\nsample workspace nested directory markdown preview입니다.",
    },
    "reports/logs/plain-output.log": {
      contentType: "text/plain",
      sizeBytes: 96,
      content: "fallback artifact\nAPI 응답이 없어 demo workspace plain text를 표시합니다.",
    },
  };
  const fallbackFile = fallbackFiles[path];

  if (!fallbackFile) {
    return null;
  }

  return {
    workspaceId,
    path,
    name,
    ...fallbackFile,
  };
}

function isMarkdownArtifactFile(
  path: string | null,
  file: ArtifactFileContent | null,
): boolean {
  const contentType = file?.contentType?.toLowerCase() ?? "";
  const filePath = (path ?? file?.path ?? file?.name ?? "").toLowerCase();

  return (
    contentType.includes("markdown") ||
    filePath.endsWith(".md") ||
    filePath.endsWith(".markdown")
  );
}

function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return "";
  }
  if (bytes < 1024) {
    return `${bytes}B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)}KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
}

function normalizeAgentSkillFiles(
  agentSkillFiles: AgentSkillFileDraft[],
): WorkspaceAgentSkillFileInput[] | null {
  const normalized = agentSkillFiles.map((skillFile) => ({
    fileName: skillFile.fileName.trim(),
    content: skillFile.content.trim(),
  }));

  if (
    normalized.some(
      (skillFile) =>
        (skillFile.fileName.length === 0 && skillFile.content.length > 0) ||
        (skillFile.fileName.length > 0 && skillFile.content.length === 0),
    )
  ) {
    return null;
  }

  return normalized.filter(
    (skillFile) => skillFile.fileName.length > 0 && skillFile.content.length > 0,
  );
}

function createLocalId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function formatInviteTarget(invite: WorkspaceInvite): string {
  return invite.targetEmail?.trim() || "SHARE LINK";
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}
