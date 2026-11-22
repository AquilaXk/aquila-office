"use client";

import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type FormEvent,
  type KeyboardEvent as ReactKeyboardEvent,
  type PointerEvent as ReactPointerEvent,
} from "react";
import {
  AlertCircle,
  Bot,
  Check,
  Copy,
  FileText,
  Loader2,
  MessageSquare,
  Play,
  Plus,
  RefreshCw,
  Send,
  ShieldAlert,
  UserRound,
  X,
  XCircle,
} from "lucide-react";
import { PixelAvatar } from "@/components/arcade";
import MarkdownContent from "@/components/ui/MarkdownContent";
import { AgentEditPanel } from "@/components/workspace/inspector/AgentEditPanel";
import {
  appendComposerToken,
  ComposerContextChips,
  useComposerPromptHistory,
} from "@/components/workspace/inspector/ChatComposerEnhancements";
import {
  actorChatSenderLabel,
  artifactPath,
  buildAgentQuickPrompts,
  compareActorPanelTasks,
  formatAgentCategory,
  formatAgentStatus,
  formatChatRole,
  formatDateTime,
  formatTaskPriority,
  formatTaskStatus,
  formatTaskType,
  isTaskAssignedToAgent,
  pickAvatarKind,
  type ActorChatMessage,
  type AgentMutationState,
  type ChatMutationState,
  type ModalLoadState,
} from "@/components/workspace/workspace-ui-helpers";
import type {
  UpdateWorkspaceAgentInput,
  WorkspaceAgent,
} from "@/lib/agents";
import type { ChatMessage } from "@/lib/chat";
import type { WorkspaceModelProvider } from "@/lib/model-providers";
import type {
  AgentReport,
  TaskExecutionLog,
  TaskStatus,
  WorkspaceTask,
} from "@/lib/tasks";
import { useCopyFeedback } from "@/lib/use-copy-feedback";
import type { WorkspaceSceneNode } from "@/lib/workspace-2d";
import { clampInspectorWidth } from "@/lib/workspace-shortcuts";

export type InspectorTarget =
  | { type: "actor"; nodeId: string }
  | { type: "task"; taskId: number }
  | null;

type ActorInspectorTab = "chat" | "tasks" | "settings";
type TaskInspectorTab = "logs" | "reports" | "chat";

const TASK_STATUS_OPTIONS: TaskStatus[] = [
  "REQUESTED",
  "ASSIGNED",
  "IN_PROGRESS",
  "WAITING_USER",
  "COMPLETED",
  "FAILED",
  "CANCELED",
];

export type WorkspaceContextInspectorProps = {
  target: InspectorTarget;
  selectedActorNode: WorkspaceSceneNode | null;
  selectedAgent: WorkspaceAgent | null;
  selectedTask: WorkspaceTask | null;
  agents: WorkspaceAgent[];
  modelProviders?: readonly WorkspaceModelProvider[];
  tasks: WorkspaceTask[];
  canManage: boolean;
  mobileOpen: boolean;
  widthPx?: number;
  // Actor chat
  actorChatMessages: ActorChatMessage[];
  actorChatInput: string;
  actorChatMutation: ChatMutationState;
  canSendActorChat: boolean;
  // Agent CRUD
  agentDeleteConfirmId: number | null;
  agentMutation: AgentMutationState;
  // Task execution logs / reports / chat
  taskLogs: TaskExecutionLog[];
  taskLogsState: ModalLoadState;
  taskReports: AgentReport[];
  taskReportsState: ModalLoadState;
  taskChatMessages: ChatMessage[];
  taskChatState: ModalLoadState;
  taskChatInput: string;
  taskChatMutation: ChatMutationState;
  mutatingTaskId: number | null;
  // Callbacks
  onClose: () => void;
  onWidthChange?: (width: number) => void;
  onWidthReset?: () => void;
  onActorChatInputChange: (value: string) => void;
  onSendActorChat: (event: FormEvent<HTMLFormElement>) => void;
  onOpenPlanArtifact: (planId: number) => void;
  onInspectTask: (taskId: number) => void;
  onOpenCreateTaskForAgent: (agentId: number) => void;
  onOpenTaskReportsModal: (task: WorkspaceTask) => void;
  onOpenTaskChatModal: (task: WorkspaceTask) => void;
  onUpdateAgent: (agentId: number, input: UpdateWorkspaceAgentInput) => Promise<void>;
  onRequestDeleteAgent: (agentId: number | null) => void;
  onConfirmDeleteAgent: (agent: WorkspaceAgent) => void;
  onReloadTaskLogs: (taskId: number) => void;
  onReloadTaskReports: (taskId: number) => void;
  onReloadTaskChat: (task: WorkspaceTask) => void;
  onTaskChatInputChange: (value: string) => void;
  onSendTaskChat: (event: FormEvent<HTMLFormElement>, task: WorkspaceTask) => void;
  onUpdateTaskStatus: (taskId: number, status: TaskStatus) => void;
  onRunTask: (task: WorkspaceTask) => void;
  onRequestTaskApproval: (task: WorkspaceTask) => void;
  onCancelTask: (task: WorkspaceTask) => void;
  onOpenArtifactPath: (path: string, name?: string) => void;
  onRetryActorChat?: () => void;
  onRetryTaskChat?: (task: WorkspaceTask) => void;
};

export function WorkspaceContextInspector({
  target,
  selectedActorNode,
  selectedAgent,
  selectedTask,
  agents,
  modelProviders = [],
  tasks,
  canManage,
  mobileOpen,
  widthPx,
  actorChatMessages,
  actorChatInput,
  actorChatMutation,
  canSendActorChat,
  agentDeleteConfirmId,
  agentMutation,
  taskLogs,
  taskLogsState,
  taskReports,
  taskReportsState,
  taskChatMessages,
  taskChatState,
  taskChatInput,
  taskChatMutation,
  mutatingTaskId,
  onRetryActorChat,
  onRetryTaskChat,
  onClose,
  onWidthChange,
  onWidthReset,
  onActorChatInputChange,
  onSendActorChat,
  onOpenPlanArtifact,
  onInspectTask,
  onOpenCreateTaskForAgent,
  onOpenTaskReportsModal,
  onOpenTaskChatModal,
  onUpdateAgent,
  onRequestDeleteAgent,
  onConfirmDeleteAgent,
  onReloadTaskLogs,
  onReloadTaskReports,
  onReloadTaskChat,
  onTaskChatInputChange,
  onSendTaskChat,
  onUpdateTaskStatus,
  onRunTask,
  onRequestTaskApproval,
  onCancelTask,
  onOpenArtifactPath,
}: WorkspaceContextInspectorProps) {
  const [actorTab, setActorTab] = useState<ActorInspectorTab>("chat");
  const [taskTab, setTaskTab] = useState<TaskInspectorTab>("logs");
  const [resizing, setResizing] = useState(false);
  const dragStartRef = useRef<{ startX: number; startWidth: number } | null>(null);
  const actorMessagesEndRef = useRef<HTMLDivElement | null>(null);
  const { copiedKey, copy } = useCopyFeedback<"logs">();
  const actorHistory = useComposerPromptHistory(
    selectedActorNode ? `actor:${selectedActorNode.id}` : "actor:default",
    actorChatInput,
    onActorChatInputChange,
  );
  const taskHistory = useComposerPromptHistory(
    selectedTask ? `task:${selectedTask.taskId}` : "task:default",
    taskChatInput,
    onTaskChatInputChange,
  );

  const handleResizerPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!onWidthChange) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    dragStartRef.current = {
      startX: event.clientX,
      startWidth: widthPx ?? 320,
    };
    setResizing(true);
  };

  const handleResizerPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!dragStartRef.current || !onWidthChange) return;
    const delta = dragStartRef.current.startX - event.clientX;
    onWidthChange(clampInspectorWidth(dragStartRef.current.startWidth + delta, window.innerWidth));
  };

  const handleResizerPointerEnd = () => {
    if (!dragStartRef.current) return;
    dragStartRef.current = null;
    setResizing(false);
  };

  const handleResizerKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    if (!onWidthChange) return;
    const current = widthPx ?? 320;
    if (event.key === "ArrowLeft") {
      event.preventDefault();
      onWidthChange(clampInspectorWidth(current + 16, window.innerWidth));
    } else if (event.key === "ArrowRight") {
      event.preventDefault();
      onWidthChange(clampInspectorWidth(current - 16, window.innerWidth));
    } else if (event.key === "Home" && onWidthReset) {
      event.preventDefault();
      onWidthReset();
    }
  };

  const handleCopyLogs = () => {
    if (taskLogs.length === 0) {
      return;
    }
    const text = taskLogs
      .map(
        (log) =>
          `[실행 #${log.taskExecutionId} · 로그 #${log.logId} · ${log.level}${
            log.createdAt ? ` · ${formatDateTime(log.createdAt)}` : ""
          }]\n${log.message}`,
      )
      .join("\n\n");
    if (!text) return;
    void copy(text, "logs");
  };

  useEffect(() => {
    if (actorTab === "chat" && actorChatMessages.length > 0) {
      actorMessagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
    }
  }, [actorChatMessages.length, actorTab]);

  const agentById = useMemo(() => {
    const map = new Map<number, WorkspaceAgent>();
    for (let i = 0; i < agents.length; i += 1) {
      map.set(agents[i].agentId, agents[i]);
    }
    return map;
  }, [agents]);

  const actorAssignedTasks = useMemo(() => {
    if (!selectedAgent) {
      return [];
    }
    const assigned = tasks.filter((task) => isTaskAssignedToAgent(task, selectedAgent.agentId));
    const timestampByTaskId = new Map<number, number>();
    for (let i = 0; i < assigned.length; i += 1) {
      const task = assigned[i];
      const parsed = Date.parse(task.updatedAt ?? task.createdAt ?? "");
      timestampByTaskId.set(task.taskId, Number.isFinite(parsed) ? parsed : 0);
    }
    return assigned.sort((a, b) => compareActorPanelTasks(a, b, timestampByTaskId));
  }, [selectedAgent, tasks]);

  if (!target) {
    return null;
  }

  if (target.type === "actor" && !selectedActorNode && !selectedAgent) {
    return null;
  }

  if (target.type === "task" && !selectedTask) {
    return null;
  }

  return (
    <aside
      className={`chrome-inspector${mobileOpen ? "" : " max-[860px]:hidden"}`}
      aria-label="컨텍스트 인스펙터"
      data-resizing={resizing ? "true" : undefined}
      style={
        widthPx
          ? ({ "--inspector-width": `${widthPx}px` } as CSSProperties)
          : undefined
      }
    >
      {onWidthChange ? (
        <div
          role="separator"
          tabIndex={0}
          aria-orientation="vertical"
          aria-label="인스펙터 너비 조절"
          aria-valuemin={280}
          aria-valuemax={520}
          aria-valuenow={widthPx ?? 320}
          title="드래그하여 너비 조절 (더블클릭으로 초기화)"
          className="chrome-inspector-resizer chrome-desktop-only"
          onPointerDown={handleResizerPointerDown}
          onPointerMove={handleResizerPointerMove}
          onPointerUp={handleResizerPointerEnd}
          onPointerCancel={handleResizerPointerEnd}
          onDoubleClick={() => onWidthReset?.()}
          onKeyDown={handleResizerKeyDown}
        />
      ) : null}
      {target.type === "actor" ? (
        <>
          <header className="chrome-inspector-header">
            <div className="flex items-center gap-2.5 min-w-0">
              {selectedActorNode ? (
                <div className="ao-crew-mugshot-chip">
                  <PixelAvatar
                    kind={pickAvatarKind(
                      selectedAgent?.name ?? selectedActorNode.label,
                      selectedActorNode.kind === "agent",
                    )}
                    variant="mugshot"
                    variantKey={selectedAgent?.name ?? selectedActorNode.label}
                    size={2.4}
                  />
                </div>
              ) : (
                <div className="w-9 h-9 rounded-[var(--radius-md)] border border-[var(--ui-border)] flex items-center justify-center bg-[var(--ui-surface-raised)]">
                  <Bot size={16} aria-hidden="true" />
                </div>
              )}
              <div className="min-w-0">
                <h2 className="m-0 text-[14px] font-semibold text-[var(--ui-text-strong)] truncate">
                  {selectedAgent?.name ?? selectedActorNode?.label ?? "참여자"}
                </h2>
                <p className="m-0 text-[12px] text-[var(--ui-text-muted)] truncate">
                  {selectedAgent
                    ? `${formatAgentCategory(selectedAgent.category)} · ${formatAgentStatus(selectedAgent.status)}`
                    : selectedActorNode?.detail ?? "워크스페이스 구성원"}
                </p>
                {selectedAgent?.modelId ? (
                  <span
                    className="role-badge font-mono mt-1 max-w-full truncate"
                    title={
                      selectedAgent.fallbackModelId
                        ? `기본: ${selectedAgent.modelId} · 폴백: ${selectedAgent.fallbackModelId}`
                        : `모델: ${selectedAgent.modelId}`
                    }
                  >
                    {selectedAgent.modelId}
                  </span>
                ) : null}
              </div>
            </div>

            <button
              type="button"
              className="tb-btn"
              onClick={onClose}
              aria-label="인스펙터 닫기"
              title="닫기 (Esc)"
            >
              <X size={14} aria-hidden="true" />
            </button>
          </header>

          {selectedAgent ? (
            <>
              <div className="chrome-inspector-tabs" role="tablist" aria-label="에이전트 상세 탭">
                <button
                  type="button"
                  role="tab"
                  aria-selected={actorTab === "chat"}
                  className={`chrome-inspector-tab${actorTab === "chat" ? " chrome-inspector-tab-active" : ""}`}
                  onClick={() => setActorTab("chat")}
                >
                  대화
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={actorTab === "tasks"}
                  className={`chrome-inspector-tab${actorTab === "tasks" ? " chrome-inspector-tab-active" : ""}`}
                  onClick={() => setActorTab("tasks")}
                >
                  작업 ({actorAssignedTasks.length})
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={actorTab === "settings"}
                  className={`chrome-inspector-tab${actorTab === "settings" ? " chrome-inspector-tab-active" : ""}`}
                  onClick={() => setActorTab("settings")}
                >
                  설정 · 스킬
                </button>
              </div>

              <div className="chrome-inspector-body">
                {actorTab === "chat" ? (
                  <>
                    <div className="flex flex-col gap-2 flex-1 min-h-[200px]">
                      {actorChatMessages.length === 0 ? (
                        <div className="flex flex-col gap-3 py-2">
                          <p className="m-0 text-[12px] text-[var(--ui-text-muted)] text-center">
                            이 에이전트에게 첫 작업 지시나 질문을 보내보세요.
                          </p>
                          <div className="ao-quick-prompts" aria-label="빠른 시작 프롬프트">
                            {buildAgentQuickPrompts(selectedAgent.category, selectedAgent.name).map(
                              (item) => (
                                <button
                                  key={item.label}
                                  type="button"
                                  className="ao-quick-prompt-btn"
                                  onClick={() => onActorChatInputChange(item.prompt)}
                                >
                                  <strong className="text-[var(--ui-text-strong)]">
                                    {item.label}
                                  </strong>
                                  <span className="text-[var(--ui-text-secondary)]">
                                    {item.prompt}
                                  </span>
                                </button>
                              ),
                            )}
                          </div>
                        </div>
                      ) : (
                        actorChatMessages.map((msg) => (
                          <article
                            key={msg.id}
                            className="p-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] text-[12px]"
                          >
                            <div className="flex items-center justify-between gap-2 mb-1">
                              <strong className="text-[var(--ui-text-strong)]">
                                {actorChatSenderLabel(msg.sender)}
                              </strong>
                              {msg.optimistic ? (
                                <span className="text-[var(--ui-text-muted)]">전송 중</span>
                              ) : null}
                            </div>
                            <div className="ao-markdown-chat-body">
                        <MarkdownContent content={msg.text} compact />
                      </div>
                            {msg.orchestrationPlanId || msg.taskId ? (
                              <div className="flex flex-wrap gap-1.5 mt-2">
                                {msg.orchestrationPlanId ? (
                                  <button
                                    type="button"
                                    className="tb-btn"
                                    onClick={() => onOpenPlanArtifact(msg.orchestrationPlanId!)}
                                  >
                                    <FileText size={12} aria-hidden="true" />
                                    <span>계획 #{msg.orchestrationPlanId}</span>
                                  </button>
                                ) : null}
                                {msg.taskId ? (
                                  <button
                                    type="button"
                                    className="tb-btn"
                                    onClick={() => onInspectTask(msg.taskId!)}
                                  >
                                    <span>작업 #{msg.taskId}</span>
                                  </button>
                                ) : null}
                              </div>
                            ) : null}
                          </article>
                        ))
                      )}
                      <div ref={actorMessagesEndRef} />
                    </div>

                    {actorChatMutation.status === "error" && actorChatMutation.message ? (
                      <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                        <AlertCircle size={14} aria-hidden="true" />
                        <span>{actorChatMutation.message}</span>
                        {onRetryActorChat ? (
                          <button
                            type="button"
                            className="ao-inline-retry-btn"
                            onClick={onRetryActorChat}
                          >
                            <RefreshCw size={11} aria-hidden="true" />
                            <span>다시 시도</span>
                          </button>
                        ) : null}
                      </div>
                    ) : null}

                    <form
                      className="ao-composer-card"
                      onSubmit={(event) => {
                        if (actorChatInput.trim()) {
                          actorHistory.recordPrompt(actorChatInput);
                        }
                        onSendActorChat(event);
                      }}
                    >
                      <ComposerContextChips
                        tasks={tasks}
                        onInsert={(token) =>
                          onActorChatInputChange(appendComposerToken(actorChatInput, token))
                        }
                      />
                      <textarea
                        rows={3}
                        maxLength={1000}
                        aria-label="에이전트 대화 메시지"
                        placeholder="에이전트에게 보낼 지시나 질문을 입력하세요. (Enter 전송, Shift+Enter 줄바꿈)"
                        disabled={actorChatMutation.status === "submitting"}
                        value={actorChatInput}
                        onChange={(event) => onActorChatInputChange(event.target.value)}
                        onKeyDown={(event) => {
                          if (
                            event.key === "Enter" &&
                            !event.shiftKey &&
                            !event.nativeEvent.isComposing
                          ) {
                            if (!canSendActorChat) return;
                            event.preventDefault();
                            event.currentTarget.form?.requestSubmit();
                            return;
                          }
                          actorHistory.handleHistoryKeyDown(event);
                        }}
                        className="ao-composer-textarea"
                      />
                      <div className="ao-composer-footer">
                        <span className="text-[12px] text-[var(--ui-text-muted)] font-mono">
                          {actorChatInput.length}/1000
                        </span>
                        <button
                          type="submit"
                          className="primary-action primary-action-active"
                          disabled={!canSendActorChat}
                        >
                          {actorChatMutation.status === "submitting" ? (
                            <Loader2 size={14} className="spin" aria-hidden="true" />
                          ) : (
                            <Send size={14} aria-hidden="true" />
                          )}
                          <span>{actorChatMutation.status === "submitting" ? "전송 중" : "메시지 전송"}</span>
                        </button>
                      </div>
                    </form>
                  </>
                ) : null}

                {actorTab === "tasks" ? (
                  <div className="flex flex-col gap-2">
                    <button
                      type="button"
                      className="primary-action"
                      onClick={() => onOpenCreateTaskForAgent(selectedAgent.agentId)}
                    >
                      <Plus size={14} aria-hidden="true" />
                      <span>새 작업 할당</span>
                    </button>

                    {actorAssignedTasks.length === 0 ? (
                      <p className="m-0 py-4 text-center text-[12px] text-[var(--ui-text-muted)]">
                        배정된 작업이 없습니다.
                      </p>
                    ) : (
                      actorAssignedTasks.map((task) => (
                        <article
                          key={task.taskId}
                          className="p-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] flex flex-col gap-2"
                        >
                          <div className="flex items-start justify-between gap-2">
                            <button
                              type="button"
                              className="text-left bg-transparent border-0 p-0 cursor-pointer text-[13px] font-semibold text-[var(--ui-text-strong)]"
                              onClick={() => onInspectTask(task.taskId)}
                            >
                              #{task.taskId} {task.title}
                            </button>
                            <span className="role-badge shrink-0">
                              {formatTaskStatus(task.status)}
                            </span>
                          </div>
                          <div className="flex flex-wrap gap-1.5">
                            <button
                              type="button"
                              className="tb-btn"
                              onClick={() => onInspectTask(task.taskId)}
                            >
                              <span>상세</span>
                            </button>
                            <button
                              type="button"
                              className="tb-btn"
                              onClick={() => onOpenTaskReportsModal(task)}
                            >
                              <FileText size={12} aria-hidden="true" />
                              <span>리포트</span>
                            </button>
                            <button
                              type="button"
                              className="tb-btn"
                              onClick={() => onOpenTaskChatModal(task)}
                            >
                              <MessageSquare size={12} aria-hidden="true" />
                              <span>대화</span>
                            </button>
                          </div>
                        </article>
                      ))
                    )}
                  </div>
                ) : null}

                {actorTab === "settings" ? (
                  <AgentEditPanel
                    key={selectedAgent.agentId}
                    agent={selectedAgent}
                    modelProviders={modelProviders}
                    canManage={canManage}
                    agentDeleteConfirmId={agentDeleteConfirmId}
                    agentMutation={agentMutation}
                    onUpdateAgent={onUpdateAgent}
                    onRequestDeleteAgent={onRequestDeleteAgent}
                    onConfirmDeleteAgent={onConfirmDeleteAgent}
                  />
                ) : null}
              </div>
            </>
          ) : (
            <div className="chrome-inspector-body">
              <div className="ao-surface-panel">
                <div className="flex items-center gap-2 mb-2">
                  <UserRound size={15} aria-hidden="true" />
                  <strong className="text-[13px] text-[var(--ui-text-strong)]">
                    구성원 정보
                  </strong>
                </div>
                <p className="m-0 text-[12px] text-[var(--ui-text-secondary)]">
                  {selectedActorNode?.detail ?? "오피스 스테이지에 접속 중인 사용자입니다."}
                </p>
              </div>
            </div>
          )}
        </>
      ) : selectedTask ? (
        <>
          <header className="chrome-inspector-header">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-1.5 text-[12px] text-[var(--ui-text-muted)]">
                <span className="font-mono">작업 #{selectedTask.taskId}</span>
                <span>·</span>
                <span>{formatTaskPriority(selectedTask.priority)}</span>
                {selectedTask.type ? (
                  <>
                    <span>·</span>
                    <span>{formatTaskType(selectedTask.type)}</span>
                  </>
                ) : null}
              </div>
              <h2 className="m-0 mt-0.5 text-[14px] font-semibold text-[var(--ui-text-strong)]">
                {selectedTask.title}
              </h2>
            </div>

            <button
              type="button"
              className="tb-btn"
              onClick={onClose}
              aria-label="인스펙터 닫기"
              title="닫기 (Esc)"
            >
              <X size={14} aria-hidden="true" />
            </button>
          </header>

          <div className="p-3 border-b border-[var(--ui-border-subtle)] flex flex-col gap-2">
            {selectedTask.description ? (
              <p className="m-0 text-[12px] text-[var(--ui-text-secondary)] whitespace-pre-wrap">
                {selectedTask.description}
              </p>
            ) : null}

            <div className="flex items-center justify-between gap-2">
              <select
                aria-label="작업 상태 변경"
                disabled={mutatingTaskId === selectedTask.taskId}
                value={selectedTask.status}
                onChange={(event) =>
                  onUpdateTaskStatus(selectedTask.taskId, event.target.value as TaskStatus)
                }
                className="flex-1 h-8 px-2 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-bg)] text-[12px] text-[var(--ui-text)]"
              >
                {TASK_STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>
                    {formatTaskStatus(status)}
                  </option>
                ))}
              </select>

              <span className="role-badge">
                {typeof selectedTask.assignedAgentId === "number"
                  ? agentById.get(selectedTask.assignedAgentId)?.name ??
                    `에이전트 #${selectedTask.assignedAgentId}`
                  : "미배정"}
              </span>
            </div>

            <div className="flex flex-wrap gap-1.5">
              <button
                type="button"
                className="tb-btn tb-btn-primary"
                disabled={mutatingTaskId === selectedTask.taskId || agents.length === 0}
                onClick={() => onRunTask(selectedTask)}
              >
                <Play size={12} aria-hidden="true" />
                <span>실행</span>
              </button>
              <button
                type="button"
                className="tb-btn"
                disabled={mutatingTaskId === selectedTask.taskId}
                onClick={() => onRequestTaskApproval(selectedTask)}
              >
                <ShieldAlert size={12} aria-hidden="true" />
                <span>승인 요청</span>
              </button>
              <button
                type="button"
                className="tb-btn"
                disabled={mutatingTaskId === selectedTask.taskId}
                onClick={() => onCancelTask(selectedTask)}
              >
                <XCircle size={12} aria-hidden="true" />
                <span>취소</span>
              </button>
            </div>
          </div>

          <div className="chrome-inspector-tabs" role="tablist" aria-label="작업 상세 탭">
            <button
              type="button"
              role="tab"
              aria-selected={taskTab === "logs"}
              className={`chrome-inspector-tab${taskTab === "logs" ? " chrome-inspector-tab-active" : ""}`}
              onClick={() => setTaskTab("logs")}
            >
              실행 로그
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={taskTab === "reports"}
              className={`chrome-inspector-tab${taskTab === "reports" ? " chrome-inspector-tab-active" : ""}`}
              onClick={() => setTaskTab("reports")}
            >
              리포트 ({taskReports.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={taskTab === "chat"}
              className={`chrome-inspector-tab${taskTab === "chat" ? " chrome-inspector-tab-active" : ""}`}
              onClick={() => setTaskTab("chat")}
            >
              작업 대화
            </button>
          </div>

          <div className="chrome-inspector-body">
            {taskTab === "logs" ? (
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between gap-2">
                  <strong className="text-[12px] text-[var(--ui-text-strong)]">실행 이력</strong>
                  <div className="flex items-center gap-1">
                    {taskLogs.length > 0 ? (
                      <button
                        type="button"
                        className="tb-btn"
                        aria-label="실행 로그 복사"
                        onClick={() => void handleCopyLogs()}
                      >
                        {copiedKey === "logs" ? (
                          <Check size={12} aria-hidden="true" />
                        ) : (
                          <Copy size={12} aria-hidden="true" />
                        )}
                        <span>{copiedKey === "logs" ? "복사됨" : "로그 복사"}</span>
                      </button>
                    ) : null}
                    <button
                      type="button"
                      className="tb-btn"
                      onClick={() => onReloadTaskLogs(selectedTask.taskId)}
                      disabled={taskLogsState.status === "loading"}
                    >
                      <RefreshCw
                        size={12}
                        className={taskLogsState.status === "loading" ? "spin" : undefined}
                        aria-hidden="true"
                      />
                      <span>새로고침</span>
                    </button>
                  </div>
                </div>

                {taskLogsState.status === "error" ? (
                  <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                    <AlertCircle size={14} aria-hidden="true" />
                    <span>{taskLogsState.error}</span>
                  </div>
                ) : null}

                {taskLogs.length === 0 && taskLogsState.status !== "loading" ? (
                  <p className="m-0 py-4 text-center text-[12px] text-[var(--ui-text-muted)]">
                    실행 로그가 없습니다.
                  </p>
                ) : (
                  taskLogs.map((log) => (
                    <article
                      key={log.logId}
                      className="p-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] text-[12px]"
                    >
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <strong className="font-mono">
                          실행 #{log.taskExecutionId} · 로그 #{log.logId}
                        </strong>
                        <span className="role-badge">{log.level}</span>
                      </div>
                      {log.createdAt ? (
                        <p className="m-0 text-[var(--ui-text-muted)]">
                          {formatDateTime(log.createdAt)}
                        </p>
                      ) : null}
                      <p className="m-0 mt-1 text-[var(--ui-text-secondary)] whitespace-pre-wrap">
                        {log.message}
                      </p>
                    </article>
                  ))
                )}
              </div>
            ) : null}

            {taskTab === "reports" ? (
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <strong className="text-[12px] text-[var(--ui-text-strong)]">리포트 목록</strong>
                  <button
                    type="button"
                    className="tb-btn"
                    onClick={() => onReloadTaskReports(selectedTask.taskId)}
                    disabled={taskReportsState.status === "loading"}
                  >
                    <RefreshCw
                      size={12}
                      className={taskReportsState.status === "loading" ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>새로고침</span>
                  </button>
                </div>

                {taskReportsState.status === "error" ? (
                  <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                    <AlertCircle size={14} aria-hidden="true" />
                    <span>{taskReportsState.error}</span>
                  </div>
                ) : null}

                {taskReports.length === 0 && taskReportsState.status !== "loading" ? (
                  <p className="m-0 py-4 text-center text-[12px] text-[var(--ui-text-muted)]">
                    아직 생성된 리포트가 없습니다.
                  </p>
                ) : (
                  taskReports.map((report) => (
                    <article
                      key={report.reportId}
                      className="p-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] text-[12px]"
                    >
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <strong>리포트 #{report.reportId}</strong>
                        <span className="role-badge">{formatTaskStatus(report.status)}</span>
                      </div>
                      {report.summary ? (
                        <div className="ao-markdown-report-body">
                          <MarkdownContent content={report.summary} compact />
                        </div>
                      ) : null}
                      {report.recommendedAction ? (
                        <p className="m-0 mt-1 text-[var(--ui-text)]">
                          권장 조치: {report.recommendedAction}
                        </p>
                      ) : null}
                      {report.artifacts && report.artifacts.length > 0 ? (
                        <div className="flex flex-wrap gap-1 mt-2">
                          {report.artifacts.map((artifact) => {
                            const resolvedPath = artifactPath(artifact);
                            return (
                              <button
                                key={artifact.artifactId}
                                type="button"
                                className="tb-btn"
                                disabled={!resolvedPath}
                                onClick={() =>
                                  resolvedPath && onOpenArtifactPath(resolvedPath, artifact.name)
                                }
                              >
                                <FileText size={12} aria-hidden="true" />
                                <span>{artifact.name}</span>
                              </button>
                            );
                          })}
                        </div>
                      ) : null}
                    </article>
                  ))
                )}
              </div>
            ) : null}

            {taskTab === "chat" ? (
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <strong className="text-[12px] text-[var(--ui-text-strong)]">작업 대화</strong>
                  <button
                    type="button"
                    className="tb-btn"
                    onClick={() => onReloadTaskChat(selectedTask)}
                    disabled={taskChatState.status === "loading"}
                  >
                    <RefreshCw
                      size={12}
                      className={taskChatState.status === "loading" ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>새로고침</span>
                  </button>
                </div>

                {taskChatMessages.map((msg) => (
                  <article
                    key={msg.messageId}
                    className="p-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] text-[12px]"
                  >
                    <div className="flex items-center justify-between gap-2 mb-1">
                      <strong>{formatChatRole(msg.role)}</strong>
                      <span className="text-[var(--ui-text-muted)]">
                        {formatDateTime(msg.createdAt)}
                      </span>
                    </div>
                    <div className="ao-markdown-chat-body">
                      <MarkdownContent content={msg.content} compact />
                    </div>
                  </article>
                ))}

                {taskChatMutation.status === "error" && taskChatMutation.message ? (
                  <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                    <AlertCircle size={14} aria-hidden="true" />
                    <span>{taskChatMutation.message}</span>
                    {onRetryTaskChat ? (
                      <button
                        type="button"
                        className="ao-inline-retry-btn"
                        onClick={() => onRetryTaskChat(selectedTask)}
                      >
                        <RefreshCw size={11} aria-hidden="true" />
                        <span>다시 시도</span>
                      </button>
                    ) : null}
                  </div>
                ) : null}

                <form
                  className="ao-composer-card"
                  onSubmit={(event) => {
                    if (taskChatInput.trim()) {
                      taskHistory.recordPrompt(taskChatInput);
                    }
                    onSendTaskChat(event, selectedTask);
                  }}
                >
                  <ComposerContextChips
                    tasks={tasks}
                    onInsert={(token) =>
                      onTaskChatInputChange(appendComposerToken(taskChatInput, token))
                    }
                  />
                  <textarea
                    rows={3}
                    maxLength={1000}
                    aria-label="작업 대화 입력"
                    placeholder="이 작업과 관련된 후속 지시를 입력하세요. (Enter 전송, Shift+Enter 줄바꿈)"
                    disabled={taskChatMutation.status === "submitting"}
                    value={taskChatInput}
                    onChange={(event) => onTaskChatInputChange(event.target.value)}
                    onKeyDown={(event) => {
                      if (
                        event.key === "Enter" &&
                        !event.shiftKey &&
                        !event.nativeEvent.isComposing
                      ) {
                        if (
                          taskChatMutation.status === "submitting" ||
                          !taskChatInput.trim() ||
                          agents.length === 0
                        ) {
                          return;
                        }
                        event.preventDefault();
                        event.currentTarget.form?.requestSubmit();
                        return;
                      }
                      taskHistory.handleHistoryKeyDown(event);
                    }}
                    className="ao-composer-textarea"
                  />
                  <div className="ao-composer-footer">
                    <span className="text-[12px] text-[var(--ui-text-muted)] font-mono">
                      {taskChatInput.length}/1000
                    </span>
                    <button
                      type="submit"
                      className="primary-action primary-action-active"
                      disabled={
                        taskChatMutation.status === "submitting" ||
                        !taskChatInput.trim() ||
                        agents.length === 0
                      }
                    >
                      <Send size={14} aria-hidden="true" />
                      <span>전송</span>
                    </button>
                  </div>
                </form>
              </div>
            ) : null}
          </div>
        </>
      ) : null}
    </aside>
  );
}
