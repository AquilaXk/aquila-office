import type { PixelAvatarKind, PixelAvatarPaletteOverride } from "@/components/arcade";
import { ApiError } from "@/lib/api-client";
import type {
  WorkspaceAgent,
  WorkspaceAgentCategory,
  WorkspaceAgentSkillFileInput,
} from "@/lib/agents";
import type {
  ArtifactFileContent,
  ArtifactTree,
} from "@/lib/artifacts";
import type { ChatMessage } from "@/lib/chat";
import type {
  WorkspaceGatewayConnectionTest,
  WorkspaceGatewayStatus,
} from "@/lib/gateway";
import type { GithubCredential } from "@/lib/github";
import type { SlackIntegration } from "@/lib/slack";
import type {
  TaskArtifact,
  TaskPriority,
  TaskStatus,
  WorkspaceTask,
} from "@/lib/tasks";
import {
  pickWorkspaceAvatarKind,
  type WorkspaceSceneNode,
} from "@/lib/workspace-2d";
import type {
  WorkspaceDashboardSummary,
  WorkspaceDetail,
  WorkspaceMember,
} from "@/lib/workspaces";

export {
  buildAgentQuickPrompts,
  formatAgentCategory,
  formatAgentStatus,
  formatChatRole,
  formatDateTime,
  formatDurationMs,
  formatFileSize,
  formatOrchestrationStatus,
  formatOrchestrationStepStatus,
  formatPresenceConnectionState,
  formatPresenceEventType,
  formatPresencePosition,
  formatTaskPriority,
  formatTaskStatus,
  formatTaskType,
  formatTokenUsageSummary,
  formatWorkspaceDetailActionState,
  formatWorkspaceDetailReadiness,
  type AgentQuickPrompt,
} from "@/lib/workspace-formatters";

export type WorkspaceViewId =
  | "office"
  | "tasks"
  | "approvals"
  | "orchestrations"
  | "artifacts";

export type WorkspaceDetailState =
  | { status: "loading"; workspace: null; error: null }
  | { status: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[]; error: null }
  | { status: "error" | "offline"; workspace: null; error: string };

export type AgentSkillFileDraft = WorkspaceAgentSkillFileInput & {
  id: string;
};

export type AgentCreateFormState = {
  name: string;
  category: WorkspaceAgentCategory;
  workspacePath: string;
  emoji: string;
  skillFiles: AgentSkillFileDraft[];
};

export type AgentMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
  agentId: number | null;
};

export type ChatMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

export type TaskChatPaginationState = {
  status: "idle" | "loading" | "error";
  message: string | null;
};

export type PresenceMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

export type PresenceStreamStatus = "idle" | "connecting" | "open" | "error";
export type ConnectionModalKind = null | "selector" | "slack" | "discord" | "github" | "gateway";

export type ConnectionMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

export type SlackConnectionState =
  | { status: "idle" | "loading"; integrations: []; error: null }
  | { status: "ready"; integrations: SlackIntegration[]; error: null }
  | { status: "error"; integrations: SlackIntegration[]; error: string };

export type GithubConnectionState =
  | { status: "idle" | "loading"; credentials: []; error: null }
  | { status: "ready"; credentials: GithubCredential[]; error: null }
  | { status: "error"; credentials: GithubCredential[]; error: string };

export type GatewayConnectionState =
  | { status: "idle" | "loading"; gateway: null; error: null }
  | { status: "ready"; gateway: WorkspaceGatewayStatus; error: null }
  | { status: "error"; gateway: WorkspaceGatewayStatus | null; error: string };

export type GatewayConnectionTestState =
  | { status: "idle"; result: null; message: null }
  | { status: "submitting"; result: null; message: null }
  | { status: "success"; result: WorkspaceGatewayConnectionTest; message: string }
  | { status: "error"; result: null; message: string };

export type GithubConnectionAction =
  | { type: "update"; credentialId: number }
  | { type: "delete"; credentialId: number };

export type SlackConnectionAction = { type: "delete"; integrationId: number };

export type ActorChatMessage = {
  id: string;
  sender: "user" | "agent" | "system";
  text: string;
  optimistic: boolean;
  taskId?: number | null;
  taskExecutionId?: number | null;
  orchestrationPlanId?: number | null;
};

export type DashboardSummaryState =
  | { status: "idle" | "loading"; summary: null; error: null }
  | { status: "ready"; summary: WorkspaceDashboardSummary; error: null }
  | { status: "error"; summary: null; error: string };

export type ModalLoadState =
  | { status: "idle"; error: null }
  | { status: "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

export interface StoredCharacterProfile {
  kind: PixelAvatarKind;
  paletteOverride?: PixelAvatarPaletteOverride;
}

export type TaskGroup = {
  key: string;
  label: string;
  statuses: TaskStatus[];
  tone: "muted" | "blue" | "green" | "red";
};

export const TASK_GROUPS: TaskGroup[] = [
  { key: "waiting", label: "대기", statuses: ["REQUESTED", "ASSIGNED"], tone: "muted" },
  { key: "progress", label: "진행", statuses: ["IN_PROGRESS", "WAITING_USER"], tone: "blue" },
  { key: "done", label: "완료", statuses: ["COMPLETED"], tone: "green" },
  { key: "stopped", label: "중단", statuses: ["FAILED", "CANCELED"], tone: "red" },
];

export const AGENT_CATEGORY_OPTIONS: WorkspaceAgentCategory[] = [
  "ORCHESTRATOR",
  "BACKEND",
  "FRONTEND",
  "QA",
  "CUSTOM",
];

export const CHARACTER_STORAGE_KEY = "ai-office.character";
export const MAX_TASK_TITLE_CHARS = 200;
export const MAX_TASK_DESCRIPTION_CHARS = 4000;
export const MAX_TASK_TYPE_CHARS = 64;
export const MAX_LOCAL_DIFF_CHARS = 1_048_576;
export const MAX_WATCH_EVENTS = 25;

export function connectionModalTitle(modal: Exclude<ConnectionModalKind, null>): string {
  switch (modal) {
    case "selector":
      return "연결 설정";
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

export function toFailureState(error: unknown, fallback: string): WorkspaceDetailState {
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

export function messageFromError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallback;
}

export function pickAvatarKind(name: string, agent = false): PixelAvatarKind {
  return pickWorkspaceAvatarKind(name, agent);
}

export function isPixelAvatarKind(value: string): value is PixelAvatarKind {
  return ["mira", "alex", "kenji", "yuna", "diego", "iris", "agent"].includes(value);
}

export function loadStoredCharacterProfile(): StoredCharacterProfile | null {
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

export function defaultAgentIdForTask(task: WorkspaceTask, agents: WorkspaceAgent[]): string {
  if (typeof task.assignedAgentId === "number" && task.assignedAgentId > 0) {
    return String(task.assignedAgentId);
  }
  if (typeof task.assignedAgentId === "string" && task.assignedAgentId.trim()) {
    return task.assignedAgentId.trim();
  }

  const readyAgent = agents.find((agent) => agent.status === "READY");
  return String(readyAgent?.agentId ?? agents[0]?.agentId ?? "");
}

export function mergeChatMessages(current: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const byId = new Map<number, ChatMessage>();
  for (const message of current) {
    byId.set(message.messageId, message);
  }
  for (const message of incoming) {
    byId.set(message.messageId, message);
  }
  return [...byId.values()].sort((left, right) => left.messageId - right.messageId);
}

export function lastChatMessageId(messages: ChatMessage[]): number | null {
  return messages.length > 0 ? messages[messages.length - 1].messageId : null;
}

export function isValidWorkspaceId(workspaceId: number): boolean {
  return Number.isInteger(workspaceId) && workspaceId > 0;
}

export function parseOptionalPositiveInteger(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }

  const parsed = Number(trimmed);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export function agentIdFromActorNode(node: WorkspaceSceneNode): number | null {
  if (node.kind !== "agent" || !node.id.startsWith("agent-")) {
    return null;
  }

  const parsed = Number.parseInt(node.id.replace("agent-", ""), 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export function actorMessageFromChatMessage(message: ChatMessage): ActorChatMessage {
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

export function actorChatSenderLabel(sender: ActorChatMessage["sender"]): string {
  if (sender === "user") {
    return "나";
  }
  if (sender === "system") {
    return "시스템";
  }
  return "에이전트";
}

export function isTaskAssignedToAgent(task: WorkspaceTask, agentId: number): boolean {
  const assignedAgentId =
    typeof task.assignedAgentId === "string"
      ? Number.parseInt(task.assignedAgentId, 10)
      : task.assignedAgentId;
  return assignedAgentId === agentId;
}

const ACTOR_PANEL_STATUS_RANK: Record<TaskStatus, number> = {
  IN_PROGRESS: 0,
  WAITING_USER: 1,
  ASSIGNED: 2,
  REQUESTED: 3,
  FAILED: 4,
  CANCELED: 5,
  COMPLETED: 6,
};

const ACTOR_PANEL_PRIORITY_RANK: Record<TaskPriority, number> = {
  URGENT: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
};

function actorPanelPriorityRank(priority: TaskPriority | undefined): number {
  return priority !== undefined ? (ACTOR_PANEL_PRIORITY_RANK[priority] ?? 4) : 4;
}

export function compareActorPanelTasks(
  a: WorkspaceTask,
  b: WorkspaceTask,
  timestampByTaskId?: ReadonlyMap<number, number>,
): number {
  const statusDelta =
    (ACTOR_PANEL_STATUS_RANK[a.status] ?? 99) -
    (ACTOR_PANEL_STATUS_RANK[b.status] ?? 99);
  if (statusDelta !== 0) {
    return statusDelta;
  }

  const priorityDelta = actorPanelPriorityRank(a.priority) - actorPanelPriorityRank(b.priority);
  if (priorityDelta !== 0) {
    return priorityDelta;
  }

  const bTime = timestampByTaskId
    ? (timestampByTaskId.get(b.taskId) ?? 0)
    : (() => {
        const parsed = Date.parse(b.updatedAt ?? b.createdAt ?? "");
        return Number.isFinite(parsed) ? parsed : 0;
      })();
  const aTime = timestampByTaskId
    ? (timestampByTaskId.get(a.taskId) ?? 0)
    : (() => {
        const parsed = Date.parse(a.updatedAt ?? a.createdAt ?? "");
        return Number.isFinite(parsed) ? parsed : 0;
      })();
  return bTime - aTime;
}

export function createLocalDashboardSummary(
  agents: WorkspaceAgent[],
  tasks: WorkspaceTask[],
): WorkspaceDashboardSummary {
  let errorAgentCount = 0;
  let runningAgentCount = 0;
  for (let i = 0; i < agents.length; i += 1) {
    const status = agents[i].status;
    if (status === "READY") {
      runningAgentCount += 1;
    } else if (status === "ERROR" || status === "SYNC_FAILED") {
      errorAgentCount += 1;
    }
  }

  let runningTaskCount = 0;
  let completedTaskCount = 0;
  let failedTaskCount = 0;
  let waitingApprovalTaskCount = 0;
  for (let i = 0; i < tasks.length; i += 1) {
    const status = tasks[i].status;
    if (status === "WAITING_USER") {
      runningTaskCount += 1;
      waitingApprovalTaskCount += 1;
    } else if (status === "ASSIGNED" || status === "IN_PROGRESS") {
      runningTaskCount += 1;
    } else if (status === "COMPLETED") {
      completedTaskCount += 1;
    } else if (status === "FAILED") {
      failedTaskCount += 1;
    }
  }

  return {
    agentCount: agents.length,
    runningAgentCount,
    idleAgentCount: Math.max(0, agents.length - runningAgentCount - errorAgentCount),
    errorAgentCount,
    taskCount: tasks.length,
    runningTaskCount,
    completedTaskCount,
    failedTaskCount,
    waitingApprovalTaskCount,
    todayUsedTokens: 0,
    dailyTokenLimit: null,
    avgExecutionDurationMs: null,
    recentReports: [],
    recentLogs: [],
  };
}

export function artifactPath(artifact: TaskArtifact): string | null {
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

export function defaultExpandedArtifactPaths(tree: ArtifactTree): Set<string> {
  return new Set(tree.children.filter((node) => node.type === "DIRECTORY").map((node) => node.path));
}

export function toggleExpandedArtifactPath(current: Set<string>, path: string): Set<string> {
  const next = new Set(current);
  if (next.has(path)) {
    next.delete(path);
  } else {
    next.add(path);
  }
  return next;
}

export function isMarkdownArtifactFile(
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

export function normalizeAgentSkillFiles(
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

export function createLocalId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
