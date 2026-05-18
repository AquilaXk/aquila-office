"use client";

import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarDays,
  Copy,
  FileText,
  FolderTree,
  Hash,
  ListChecks,
  Loader2,
  Mail,
  RefreshCw,
  Send,
  Settings,
  ShieldCheck,
  UserRound,
  Users,
  WifiOff,
  X,
} from "lucide-react";
import Link from "next/link";
import { use, useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import Workspace2DCanvas from "@/components/workspace/Workspace2DCanvas";
import { MarkdownContent } from "@/components/ui";
import { ApiError } from "@/lib/api";
import {
  createWorkspaceAgent,
  deleteWorkspaceAgent,
  fetchWorkspaceAgents,
  type WorkspaceAgent,
  type WorkspaceAgentCategory,
} from "@/lib/agents";
import { resolveAccessToken } from "@/lib/auth";
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
  chatSessionMessagePageInput,
  fetchChatSessionMessages,
  fetchTaskChatMessages,
  sendWorkspaceChatMessage,
  type ChatMessage,
} from "@/lib/chat";
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
  });
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
  const [presenceStreamStatus, setPresenceStreamStatus] =
    useState<PresenceStreamStatus>("idle");
  const [presenceEvents, setPresenceEvents] = useState<WorkspacePresenceMessage[]>([]);
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
  const [artifactFile, setArtifactFile] = useState<ArtifactFileContent | null>(null);
  const [artifactFilePath, setArtifactFilePath] = useState<string | null>(null);
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

    setAgentMutation({ status: "submitting", message: "Agent 등록 중", agentId: null });

    try {
      const created = await createWorkspaceAgent(resolveAccessToken(), numericWorkspaceId, {
        name,
        category: agentCreateForm.category,
        workspacePath: agentCreateForm.workspacePath.trim() || undefined,
      });
      setAgentCreateForm((current) => ({
        ...current,
        name: "",
        workspacePath: "",
      }));
      await refreshAgentData();
      setAgentMutation({
        status: "success",
        message: `Agent #${created.agentId} 등록 완료`,
        agentId: created.agentId,
      });
    } catch (error) {
      setAgentMutation({
        status: "error",
        message: messageFromError(error, "Agent 등록을 처리하지 못했습니다."),
        agentId: null,
      });
    }
  };

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
        setOrchestrationPlan(null);
        setOrchestrationPlanState({ status: "loading", error: null });
        try {
          const plan = await fetchOrchestrationPlanArtifact(
            resolveAccessToken(),
            numericWorkspaceId,
            result.orchestrationPlanId,
          );
          setOrchestrationPlan(plan);
          setOrchestrationPlanState({ status: "ready", error: null });
        } catch (planError) {
          setOrchestrationPlanState({
            status: "error",
            error: messageFromError(planError, "Orchestration plan artifact를 불러오지 못했습니다."),
          });
        }
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

  const openWorkspaceFiles = useCallback(async () => {
    setFilesOpen(true);
    setArtifactTree(null);
    setArtifactTreeState({ status: "loading", error: null });

    try {
      const tree = await fetchArtifactTree(resolveAccessToken(), numericWorkspaceId);
      setArtifactTree(tree);
      setArtifactTreeState({ status: "ready", error: null });
    } catch (error) {
      setArtifactTreeState({
        status: "error",
        error: messageFromError(error, "파일 트리를 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const openArtifactFile = useCallback(
    async (path: string) => {
      setArtifactFile(null);
      setArtifactFilePath(path);
      setArtifactFileState({ status: "loading", error: null });

      try {
        const file = await fetchArtifactFile(resolveAccessToken(), numericWorkspaceId, path);
        setArtifactFile(file);
        setArtifactFileState({ status: "ready", error: null });
      } catch (error) {
        setArtifactFileState({
          status: "error",
          error: messageFromError(error, "파일 내용을 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const scrollToWorkspaceSection = useCallback((sectionId: string) => {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, []);

  const handleOfficeSceneNavAction = useCallback(
    (itemId: OfficeSceneNavItemId) => {
      if (itemId === "tasks") {
        scrollToWorkspaceSection("workspace-tasks");
      }
      if (itemId === "hire-agent") {
        scrollToWorkspaceSection("workspace-hire-agent");
      }
      if (itemId === "files") {
        void openWorkspaceFiles();
      }
    },
    [openWorkspaceFiles, scrollToWorkspaceSection],
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
        { email },
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
          <div className="workspace-detail-actions">
            <Link className="secondary-action workspace-detail-back" href="/">
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
          </div>
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
                  if (item.id === "connections") {
                    return (
                      <Link
                        className="workspace-office-nav-item"
                        href={`/workspaces/${numericWorkspaceId}/settings`}
                        key={item.id}
                      >
                        <Icon aria-hidden="true" />
                        <span>{item.label}</span>
                      </Link>
                    );
                  }
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
                onPresenceMove={(position) =>
                  updatePresencePosition(position, "2D workspace 위치가 갱신되었습니다.")
                }
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
                        <UserRound aria-hidden="true" />
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
                        <UserRound aria-hidden="true" />
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
                  {members.map((member) => (
                    <article className="workspace-member-row" key={member.memberId}>
                      <div className="workspace-member-avatar">
                        <UserRound aria-hidden="true" />
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
                  ))}
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
                      <strong>{inviteState.invite.email}</strong>
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

      <TaskReportsModal
        task={selectedTask}
        reports={taskReports}
        state={reportState}
        onClose={() => {
          setSelectedTask(null);
          setTaskReports([]);
          setReportState({ status: "idle", error: null });
        }}
        onOpenArtifact={(path) => void openArtifactFile(path)}
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
        onOpenFile={(path) => void openArtifactFile(path)}
      />
      <ArtifactTreeModal
        open={filesOpen}
        tree={artifactTree}
        state={artifactTreeState}
        onClose={() => {
          setFilesOpen(false);
          setArtifactTree(null);
          setArtifactTreeState({ status: "idle", error: null });
        }}
        onReload={() => void openWorkspaceFiles()}
        onOpenFile={(path) => void openArtifactFile(path)}
      />
      <ArtifactFileModal
        path={artifactFilePath}
        file={artifactFile}
        state={artifactFileState}
        onClose={() => {
          setArtifactFile(null);
          setArtifactFilePath(null);
          setArtifactFileState({ status: "idle", error: null });
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
  onOpenArtifact: (path: string) => void;
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
                            onClick={() => path && onOpenArtifact(path)}
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
  onOpenFile: (path: string) => void;
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
              text="Orchestration plan artifact를 불러오고 있습니다."
            />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && plan && plan.steps.length === 0 ? (
            <ModalState icon={<FileText aria-hidden="true" />} text="표시할 plan step이 없습니다." />
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
                          onClick={() => onOpenFile(file.path)}
                          disabled={!file.exists}
                        >
                          <FileText aria-hidden="true" />
                          <span>{file.name}</span>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <p>연결된 파일이 없습니다.</p>
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
  onClose,
  onReload,
  onOpenFile,
}: {
  open: boolean;
  tree: ArtifactTree | null;
  state: ModalLoadState;
  onClose: () => void;
  onReload: () => void;
  onOpenFile: (path: string) => void;
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
            <h2>Files</h2>
          </div>
          <div className="workspace-modal-actions">
            <button
              className="secondary-action workspace-task-refresh"
              type="button"
              onClick={onReload}
              disabled={state.status === "loading"}
            >
              <RefreshCw aria-hidden="true" />
              <span>새로고침</span>
            </button>
            <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
              <X aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="workspace-modal-body">
          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="파일 트리를 불러오고 있습니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && nodes.length === 0 ? (
            <ModalState icon={<FolderTree aria-hidden="true" />} text="아직 표시할 파일이 없습니다." />
          ) : null}
          {nodes.length > 0 ? <ArtifactTreeNodes nodes={nodes} depth={0} onOpenFile={onOpenFile} /> : null}
        </div>
      </section>
    </div>
  );
}

function ArtifactTreeNodes({
  nodes,
  depth,
  onOpenFile,
}: {
  nodes: ArtifactNode[];
  depth: number;
  onOpenFile: (path: string) => void;
}) {
  return (
    <ul className="workspace-artifact-tree">
      {nodes.map((node) => (
        <li key={node.path}>
          <button
            className="workspace-artifact-row"
            type="button"
            onClick={() => {
              if (node.type === "FILE") {
                onOpenFile(node.path);
              }
            }}
            style={{ paddingLeft: 10 + depth * 16 }}
          >
            <span aria-hidden="true">{node.type === "DIRECTORY" ? "▾" : "·"}</span>
            <strong>{node.name}</strong>
            {typeof node.sizeBytes === "number" ? <small>{formatFileSize(node.sizeBytes)}</small> : null}
          </button>
          {node.children && node.children.length > 0 ? (
            <ArtifactTreeNodes nodes={node.children} depth={depth + 1} onOpenFile={onOpenFile} />
          ) : null}
        </li>
      ))}
    </ul>
  );
}

function ArtifactFileModal({
  path,
  file,
  state,
  onClose,
}: {
  path: string | null;
  file: ArtifactFileContent | null;
  state: ModalLoadState;
  onClose: () => void;
}) {
  if (!path && state.status === "idle") {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal workspace-file-modal" role="dialog" aria-modal="true" aria-label="Artifact file">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">{path ?? ""}</span>
            <h2>{file?.name ?? "File"}</h2>
          </div>
          <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="파일 내용을 불러오고 있습니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
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
