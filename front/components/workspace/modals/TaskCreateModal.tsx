"use client";

import { useRef, useState } from "react";
import { AlertCircle, Loader2, Play, Plus, X } from "lucide-react";
import { useDialogA11y } from "@/components/ui/useDialogA11y";
import {
  formatAgentStatus,
  formatTaskPriority,
  MAX_TASK_DESCRIPTION_CHARS,
  MAX_TASK_TITLE_CHARS,
  type AgentMutationState,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgent } from "@/lib/agents";
import type {
  CreateAndRunWorkspaceTaskInput,
  TaskPriority,
  TaskType,
} from "@/lib/tasks";

export type TaskCreateModalProps = {
  open: boolean;
  agents: WorkspaceAgent[];
  initialAgentId?: number | null;
  mutation: AgentMutationState;
  submitting: boolean;
  onClose: () => void;
  onCreate: (input: CreateAndRunWorkspaceTaskInput, runImmediately: boolean) => Promise<void> | void;
};

const PRIORITY_OPTIONS: TaskPriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const TASK_TYPE_OPTIONS: Array<{ value: TaskType; label: string }> = [
  { value: "FEATURE_IMPLEMENTATION", label: "기능 구현" },
  { value: "BUG_FIX", label: "버그 수정" },
  { value: "REFACTORING", label: "리팩토링" },
  { value: "TEST_CREATION", label: "테스트 작성" },
  { value: "CODE_REVIEW", label: "코드 리뷰" },
  { value: "PR_REVIEW", label: "PR 검토" },
  { value: "DOCUMENTATION", label: "문서 작성" },
  { value: "PR_CREATION", label: "PR 생성" },
  { value: "OTHER", label: "기타" },
];

export function TaskCreateModal({
  open,
  agents,
  initialAgentId = null,
  mutation,
  submitting,
  onClose,
  onCreate,
}: TaskCreateModalProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  const { onBackdropPointerDown, onBackdropClick } = useDialogA11y(open, dialogRef, onClose);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [assignedAgentId, setAssignedAgentId] = useState<string>(
    initialAgentId ? String(initialAgentId) : "",
  );
  const [priority, setPriority] = useState<TaskPriority>("MEDIUM");
  const [taskType, setTaskType] = useState<TaskType>("FEATURE_IMPLEMENTATION");
  const [createPr, setCreatePr] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  if (!open) {
    return null;
  }

  async function submitTask(runImmediately: boolean) {
    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      setValidationError("작업 제목을 입력해 주세요.");
      return;
    }
    if (trimmedTitle.length > MAX_TASK_TITLE_CHARS) {
      setValidationError(`작업 제목은 ${MAX_TASK_TITLE_CHARS}자를 넘길 수 없습니다.`);
      return;
    }
    const trimmedDescription = description.trim();
    if (trimmedDescription.length > MAX_TASK_DESCRIPTION_CHARS) {
      setValidationError(`작업 설명은 ${MAX_TASK_DESCRIPTION_CHARS}자를 넘길 수 없습니다.`);
      return;
    }

    const parsedAgentId = assignedAgentId.trim() ? Number.parseInt(assignedAgentId, 10) : null;
    if (runImmediately && (!parsedAgentId || parsedAgentId <= 0)) {
      setValidationError("즉시 실행하려면 담당 에이전트를 먼저 선택해 주세요.");
      return;
    }

    setValidationError(null);
    await onCreate(
      {
        title: trimmedTitle,
        description: trimmedDescription || undefined,
        type: taskType,
        priority,
        assignedAgentId: parsedAgentId && parsedAgentId > 0 ? parsedAgentId : undefined,
        createPr,
      },
      runImmediately,
    );
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
        aria-label="새 작업 생성"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">작업 요청</span>
            <h2>새 작업 생성</h2>
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

        <form
          className="workspace-modal-body"
          onSubmit={(event) => {
            event.preventDefault();
            void submitTask(false);
          }}
        >
          <label className="form-field">
            <span>
              <strong>작업 제목</strong>
              <small>
                {title.length}/{MAX_TASK_TITLE_CHARS}
              </small>
            </span>
            <input
              autoFocus
              disabled={submitting}
              maxLength={MAX_TASK_TITLE_CHARS}
              placeholder="예: 로그인 인증 오류 원인 분석 및 수정"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>

          <label className="form-field">
            <span>
              <strong>상세 설명</strong>
              <small>
                {description.length}/{MAX_TASK_DESCRIPTION_CHARS}
              </small>
            </span>
            <textarea
              disabled={submitting}
              maxLength={MAX_TASK_DESCRIPTION_CHARS}
              placeholder="완료 기준, 검증 명령, 제약 사항을 구체적으로 입력하세요."
              rows={4}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>

          <div className="workspace-gateway-form-grid">
            <label className="form-field">
              <span>
                <strong>담당 에이전트</strong>
                <small>{agents.length}명</small>
              </span>
              <select
                disabled={submitting}
                value={assignedAgentId}
                onChange={(event) => setAssignedAgentId(event.target.value)}
              >
                <option value="">미배정 (대기열 등록)</option>
                {agents.map((agent) => (
                  <option key={agent.agentId} value={agent.agentId}>
                    {agent.name} · {formatAgentStatus(agent.status)}
                  </option>
                ))}
              </select>
            </label>

            <label className="form-field">
              <span>
                <strong>우선순위</strong>
                <small>{formatTaskPriority(priority)}</small>
              </span>
              <select
                disabled={submitting}
                value={priority}
                onChange={(event) => setPriority(event.target.value as TaskPriority)}
              >
                {PRIORITY_OPTIONS.map((item) => (
                  <option key={item} value={item}>
                    {formatTaskPriority(item)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="form-field">
            <span>
              <strong>작업 유형</strong>
            </span>
            <select
              disabled={submitting}
              value={taskType}
              onChange={(event) => setTaskType(event.target.value as TaskType)}
            >
              {TASK_TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="inline-flex items-center gap-2 text-[13px] cursor-pointer">
            <input
              type="checkbox"
              checked={createPr}
              disabled={submitting}
              onChange={(event) => setCreatePr(event.target.checked)}
            />
            <span>작업 완료 시 GitHub PR 생성 요청</span>
          </label>

          {validationError ? (
            <div className="form-notice form-notice-error" role="alert">
              <AlertCircle aria-hidden="true" />
              <span>{validationError}</span>
            </div>
          ) : null}

          {mutation.status === "error" && mutation.message ? (
            <div className="form-notice form-notice-error" role="alert">
              <AlertCircle aria-hidden="true" />
              <span>{mutation.message}</span>
            </div>
          ) : null}

          <div className="workspace-modal-footer flex flex-wrap gap-2 justify-end">
            <button className="secondary-action" type="submit" disabled={submitting}>
              {submitting ? (
                <Loader2 className="spin" aria-hidden="true" />
              ) : (
                <Plus aria-hidden="true" />
              )}
              <span>작업만 생성</span>
            </button>
            <button
              className="primary-action primary-action-active"
              type="button"
              disabled={submitting || agents.length === 0}
              onClick={() => void submitTask(true)}
            >
              {submitting ? (
                <Loader2 className="spin" aria-hidden="true" />
              ) : (
                <Play aria-hidden="true" />
              )}
              <span>생성 후 즉시 실행</span>
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
