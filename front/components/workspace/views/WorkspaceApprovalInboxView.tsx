"use client";

import { useMemo, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  FileText,
  Loader2,
  ShieldCheck,
  XCircle,
} from "lucide-react";
import PixelAvatar from "@/components/arcade/PixelAvatar";
import {
  formatDateTime,
  formatTaskPriority,
  formatTaskStatus,
  messageFromError,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgent } from "@/lib/agents";
import type { WorkspaceTask } from "@/lib/tasks";
import {
  MAX_TASK_APPROVAL_TEXT_CHARS,
  validateTaskApprovalText,
} from "@/lib/tasks-utils";

export type WorkspaceApprovalInboxViewProps = {
  tasks: WorkspaceTask[];
  agents: WorkspaceAgent[];
  canManage: boolean;
  onSelectTask: (taskId: number) => void;
  onOpenReports: (task: WorkspaceTask) => void;
  onReviewApproval: (
    taskId: number,
    input: { decision: "APPROVE" | "REJECT"; comment?: string; runAfterApprove?: boolean },
  ) => Promise<void>;
};

export function WorkspaceApprovalInboxView({
  tasks,
  agents,
  canManage,
  onSelectTask,
  onOpenReports,
  onReviewApproval,
}: WorkspaceApprovalInboxViewProps) {
  const [commentsByTaskId, setCommentsByTaskId] = useState<Record<number, string>>({});
  const [runAfterByTaskId, setRunAfterByTaskId] = useState<Record<number, boolean>>({});
  const [submittingTaskId, setSubmittingTaskId] = useState<number | null>(null);
  const [notice, setNotice] = useState<{ tone: "success" | "danger"; message: string } | null>(
    null,
  );

  const agentNameById = useMemo(() => {
    const map = new Map<number, string>();
    for (let i = 0; i < agents.length; i += 1) {
      map.set(agents[i].agentId, agents[i].name);
    }
    return map;
  }, [agents]);

  const pendingTasks = useMemo(
    () => tasks.filter((task) => task.status === "WAITING_USER"),
    [tasks],
  );

  async function handleReview(taskId: number, decision: "APPROVE" | "REJECT") {
    const rawComment = commentsByTaskId[taskId] ?? "";
    const commentCheck = validateTaskApprovalText(rawComment, "검토 의견");
    if (!commentCheck.ok) {
      setNotice({ tone: "danger", message: commentCheck.error });
      return;
    }

    setSubmittingTaskId(taskId);
    setNotice(null);
    try {
      await onReviewApproval(taskId, {
        decision,
        comment: commentCheck.value ?? undefined,
        runAfterApprove: decision === "APPROVE" ? (runAfterByTaskId[taskId] ?? true) : false,
      });
      setNotice({
        tone: "success",
        message:
          decision === "APPROVE"
            ? `작업 #${taskId} 승인을 완료했습니다.`
            : `작업 #${taskId}을(를) 반려했습니다.`,
      });
    } catch (error) {
      setNotice({
        tone: "danger",
        message: messageFromError(error, "승인 처리에 실패했습니다."),
      });
    } finally {
      setSubmittingTaskId(null);
    }
  }

  return (
    <div className="chrome-view-scroll" aria-label="승인 대기함 뷰">
      <div className="ao-page-header">
        <div>
          <p className="ao-page-eyebrow">사람 검토</p>
          <h2 className="ao-page-title">승인 대기함</h2>
          <p className="ao-page-subtitle">
            에이전트가 승인을 요청한 작업 내역과 리포트를 검토하고 승인 또는 반려합니다.
          </p>
        </div>
        <span className="role-badge">대기 {pendingTasks.length}건</span>
      </div>

      {!canManage ? (
        <div className="ao-inline-notice ao-inline-notice-warning" role="status">
          <AlertCircle size={15} aria-hidden="true" />
          <span>관리자(ADMIN) 역할만 승인 및 반려를 결정할 수 있습니다.</span>
        </div>
      ) : null}

      {notice ? (
        <div
          className={`ao-inline-notice ${
            notice.tone === "danger" ? "ao-inline-notice-danger" : "ao-inline-notice-success"
          }`}
          role={notice.tone === "danger" ? "alert" : "status"}
        >
          {notice.tone === "danger" ? (
            <AlertCircle size={15} aria-hidden="true" />
          ) : (
            <CheckCircle2 size={15} aria-hidden="true" />
          )}
          <span>{notice.message}</span>
        </div>
      ) : null}

      {pendingTasks.length === 0 ? (
        <div className="ao-surface-panel text-center py-10">
          <ShieldCheck size={24} className="mx-auto mb-2 text-[var(--ui-text-muted)]" aria-hidden="true" />
          <p className="m-0 text-[13px] font-medium text-[var(--ui-text-strong)]">
            승인 대기 중인 작업이 없습니다
          </p>
          <p className="m-0 mt-1 text-[12px] text-[var(--ui-text-secondary)]">
            에이전트가 검토를 요청하면 이곳에 표시됩니다.
          </p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {pendingTasks.map((task) => {
            const comment = commentsByTaskId[task.taskId] ?? "";
            const runAfterApprove = runAfterByTaskId[task.taskId] ?? true;
            const submitting = submittingTaskId === task.taskId;
            const assignedId =
              typeof task.assignedAgentId === "string"
                ? Number.parseInt(task.assignedAgentId, 10)
                : task.assignedAgentId;
            const assignedName =
              typeof assignedId === "number" && Number.isFinite(assignedId)
                ? agentNameById.get(assignedId) ?? `에이전트 #${assignedId}`
                : "미배정";

            return (
              <article className="ao-surface-panel ao-approval-card" key={task.taskId}>
                <div className="flex flex-wrap items-start justify-between gap-2 mb-2">
                  <div className="flex items-start gap-3 min-w-0">
                    <div className="ao-crew-mugshot-chip">
                      <PixelAvatar
                        kind="agent"
                        variant="mugshot"
                        variantKey={assignedName}
                        size={1.9}
                      />
                    </div>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2 text-[12px] text-[var(--ui-text-muted)]">
                        <span className="font-mono">작업 #{task.taskId}</span>
                        <span>·</span>
                        <span>{assignedName}</span>
                        {task.updatedAt ? (
                          <>
                            <span>·</span>
                            <span>{formatDateTime(task.updatedAt)}</span>
                          </>
                        ) : null}
                      </div>
                      <h3 className="m-0 mt-1 text-[15px] font-semibold text-[var(--ui-text-strong)]">
                        {task.title}
                      </h3>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="role-badge">{formatTaskPriority(task.priority)}</span>
                    <span className="task-status-badge task-status-waiting-user">
                      {formatTaskStatus(task.status)}
                    </span>
                  </div>
                </div>

                {task.description ? (
                  <p className="m-0 mb-3 text-[13px] text-[var(--ui-text-secondary)] whitespace-pre-wrap">
                    {task.description}
                  </p>
                ) : null}

                <label className="form-field mb-3">
                  <span>
                    <strong>검토 의견</strong>
                    <small>
                      {comment.length}/{MAX_TASK_APPROVAL_TEXT_CHARS}
                    </small>
                  </span>
                  <textarea
                    rows={2}
                    maxLength={MAX_TASK_APPROVAL_TEXT_CHARS}
                    disabled={!canManage || submitting}
                    placeholder="승인 또는 반려 사유를 입력하세요. (선택)"
                    aria-label={`작업 #${task.taskId} 검토 의견`}
                    value={comment}
                    onChange={(event) =>
                      setCommentsByTaskId((current) => ({
                        ...current,
                        [task.taskId]: event.target.value,
                      }))
                    }
                  />
                </label>

                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-3">
                    <label className="inline-flex items-center gap-2 text-[12px] cursor-pointer">
                      <input
                        type="checkbox"
                        checked={runAfterApprove}
                        disabled={!canManage || submitting}
                        onChange={(event) =>
                          setRunAfterByTaskId((current) => ({
                            ...current,
                            [task.taskId]: event.target.checked,
                          }))
                        }
                      />
                      <span>승인 후 즉시 실행</span>
                    </label>

                    <button
                      type="button"
                      className="tb-btn"
                      onClick={() => onOpenReports(task)}
                    >
                      <FileText size={13} aria-hidden="true" />
                      <span>리포트 보기</span>
                    </button>

                    <button
                      type="button"
                      className="tb-btn"
                      onClick={() => onSelectTask(task.taskId)}
                    >
                      <span>상세 보기</span>
                    </button>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      className="danger-action"
                      disabled={!canManage || submitting}
                      onClick={() => void handleReview(task.taskId, "REJECT")}
                    >
                      {submitting ? (
                        <Loader2 size={14} className="spin" aria-hidden="true" />
                      ) : (
                        <XCircle size={14} aria-hidden="true" />
                      )}
                      <span>반려</span>
                    </button>
                    <button
                      type="button"
                      className="primary-action primary-action-active"
                      disabled={!canManage || submitting}
                      onClick={() => void handleReview(task.taskId, "APPROVE")}
                    >
                      {submitting ? (
                        <Loader2 size={14} className="spin" aria-hidden="true" />
                      ) : (
                        <ShieldCheck size={14} aria-hidden="true" />
                      )}
                      <span>승인</span>
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
