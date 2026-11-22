"use client";

import { useRef, type FormEvent, type ReactNode } from "react";
import { AlertCircle, FileText, Loader2, RefreshCw, Send, X } from "lucide-react";
import MarkdownContent from "@/components/ui/MarkdownContent";
import { useDialogA11y } from "@/components/ui/useDialogA11y";
import {
  artifactPath,
  formatAgentStatus,
  formatChatRole,
  formatDateTime,
  formatTaskStatus,
  type ChatMutationState,
  type ModalLoadState,
  type TaskChatPaginationState,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgent } from "@/lib/agents";
import type { OrchestrationPlanArtifact } from "@/lib/artifacts";
import type { ChatMessage } from "@/lib/chat";
import type { AgentReport, WorkspaceTask } from "@/lib/tasks";

export function ModalState({
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

export function TaskReportsModal({
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
  const dialogRef = useRef<HTMLElement | null>(null);
  const { onBackdropPointerDown, onBackdropClick } = useDialogA11y(Boolean(task), dialogRef, onClose);

  if (!task) {
    return null;
  }

  return (
    <div
      className="workspace-modal-backdrop"
      role="presentation"
      onPointerDown={(event) => onBackdropPointerDown(event.target === event.currentTarget)}
      onClick={(event) => onBackdropClick(event.target === event.currentTarget)}
    >
      <section
        ref={dialogRef}
        className="workspace-modal"
        role="dialog"
        aria-modal="true"
        aria-label="작업 리포트"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">작업 #{task.taskId}</span>
            <h2>실행 리포트</h2>
          </div>
          <button
            className="icon-button workspace-modal-close"
            type="button"
            onClick={onClose}
            aria-label="닫기"
          >
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          <div className="workspace-modal-summary">
            <strong>{task.title}</strong>
            {task.description ? <p>{task.description}</p> : null}
          </div>

          {state.status === "loading" ? (
            <ModalState
              icon={<Loader2 className="spin" aria-hidden="true" />}
              text="리포트를 불러오고 있습니다."
            />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && reports.length === 0 ? (
            <ModalState icon={<FileText aria-hidden="true" />} text="아직 등록된 리포트가 없습니다." />
          ) : null}

          {reports.length > 0 ? (
            <div className="workspace-report-list">
              {reports.map((report) => (
                <article className="workspace-report-card" key={report.reportId}>
                  <div className="workspace-report-card-header">
                    <strong>리포트 #{report.reportId}</strong>
                    <span
                      className={`task-status-badge task-status-${report.status.toLowerCase().replaceAll("_", "-")}`}
                    >
                      {formatTaskStatus(report.status)}
                    </span>
                  </div>
                  {report.summary ? <MarkdownContent content={report.summary} compact /> : null}
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

export function TaskChatModal({
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
  const dialogRef = useRef<HTMLElement | null>(null);
  const { onBackdropPointerDown, onBackdropClick } = useDialogA11y(Boolean(task), dialogRef, onClose);

  if (!task) {
    return null;
  }

  return (
    <div
      className="workspace-modal-backdrop"
      role="presentation"
      onPointerDown={(event) => onBackdropPointerDown(event.target === event.currentTarget)}
      onClick={(event) => onBackdropClick(event.target === event.currentTarget)}
    >
      <section
        ref={dialogRef}
        className="workspace-modal"
        role="dialog"
        aria-modal="true"
        aria-label="작업 대화 메시지"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">작업 #{task.taskId}</span>
            <h2>작업 대화</h2>
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
            <button
              className="icon-button workspace-modal-close"
              type="button"
              onClick={onClose}
              aria-label="닫기"
            >
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
            <ModalState
              icon={<Loader2 className="spin" aria-hidden="true" />}
              text="대화 메시지를 불러오고 있습니다."
            />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && messages.length === 0 ? (
            <ModalState icon={<Send aria-hidden="true" />} text="아직 표시할 대화 메시지가 없습니다." />
          ) : null}

          {messages.length > 0 ? (
            <div className="workspace-report-list">
              {messages.map((message) => (
                <article className="workspace-report-card" key={message.messageId}>
                  <div className="workspace-report-card-header">
                    <strong>{formatChatRole(message.role)}</strong>
                    <span>{formatDateTime(message.createdAt)}</span>
                  </div>
                  <MarkdownContent content={message.content} compact />
                  <div className="workspace-report-artifacts">
                    <span className="role-badge">세션 #{message.chatSessionId}</span>
                    {message.taskExecutionId ? <span>실행 #{message.taskExecutionId}</span> : null}
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
                <strong>에이전트</strong>
                <small>{agents.length}명</small>
              </span>
              <select
                value={agentIdInput}
                onChange={(event) => onAgentChange(event.target.value)}
                disabled={mutation.status === "submitting" || agents.length === 0}
              >
                <option value="">에이전트 선택</option>
                {agents.map((agent) => (
                  <option value={agent.agentId} key={agent.agentId}>
                    {agent.name} · {formatAgentStatus(agent.status)}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-field">
              <span>
                <strong>메시지</strong>
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

export function OrchestrationPlanModal({
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
  const dialogRef = useRef<HTMLElement | null>(null);
  const open = Boolean(plan || state.status !== "idle");
  const { onBackdropPointerDown, onBackdropClick } = useDialogA11y(open, dialogRef, onClose);

  if (!open) {
    return null;
  }

  return (
    <div
      className="workspace-modal-backdrop"
      role="presentation"
      onPointerDown={(event) => onBackdropPointerDown(event.target === event.currentTarget)}
      onClick={(event) => onBackdropClick(event.target === event.currentTarget)}
    >
      <section
        ref={dialogRef}
        className="workspace-modal"
        role="dialog"
        aria-modal="true"
        aria-label="오케스트레이션 계획 아티팩트"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">계획 {plan ? `#${plan.planId}` : ""}</span>
            <h2>오케스트레이션 계획</h2>
          </div>
          <button
            className="icon-button workspace-modal-close"
            type="button"
            onClick={onClose}
            aria-label="닫기"
          >
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
            <ModalState icon={<FileText aria-hidden="true" />} text="아직 등록된 단계가 없습니다." />
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
