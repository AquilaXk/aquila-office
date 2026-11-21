"use client";

import { useRef, type FormEvent } from "react";
import { AlertCircle, Loader2, ShieldCheck, UserPlus, X } from "lucide-react";
import { useDialogA11y } from "@/components/ui/useDialogA11y";
import {
  AGENT_CATEGORY_OPTIONS,
  formatAgentCategory,
  type AgentCreateFormState,
  type AgentMutationState,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgentCategory } from "@/lib/agents";
import {
  MAX_AGENT_SKILL_CONTENT_CHARS,
  MAX_AGENT_SKILL_FILE_NAME_CHARS,
  MAX_AGENT_SKILL_FILES,
} from "@/lib/agents-utils";

export type AgentHireModalProps = {
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
};

export function AgentHireModal({
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
}: AgentHireModalProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  useDialogA11y(open, dialogRef, onClose);

  if (!open) {
    return null;
  }

  return (
    <div
      className="workspace-modal-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        ref={dialogRef}
        className="workspace-modal workspace-agent-hire-modal"
        role="dialog"
        aria-modal="true"
        aria-label="에이전트 추가"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">AI 에이전트 등록</span>
            <h2>에이전트 추가</h2>
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

        <form className="workspace-modal-body workspace-agent-hire-form" onSubmit={onSubmit}>
          <div className="workspace-agent-hire-fields">
            <label className="form-field">
              <span>
                <strong>에이전트 이름</strong>
                <small>필수</small>
              </span>
              <input
                autoFocus
                disabled={submitting}
                maxLength={100}
                onChange={(event) => onNameChange(event.target.value)}
                placeholder="예: 백엔드 에이전트"
                value={form.name}
              />
            </label>

            <label className="form-field">
              <span>
                <strong>역할 분류</strong>
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
                <strong>작업 경로</strong>
                <small>선택</small>
              </span>
              <input
                disabled={submitting}
                maxLength={255}
                onChange={(event) => onWorkspacePathChange(event.target.value)}
                placeholder="/workspace/backend"
                value={form.workspacePath}
              />
            </label>

            <label className="form-field">
              <span>
                <strong>표시 아이콘</strong>
                <small>선택</small>
              </span>
              <input
                disabled={submitting}
                maxLength={16}
                onChange={(event) => onEmojiChange(event.target.value)}
                placeholder="예: BE"
                value={form.emoji}
              />
            </label>

            <section className="workspace-agent-skill-files" aria-label="스킬 파일">
              <div className="workspace-section-heading">
                <div>
                  <span className="workspace-detail-label">
                    스킬 파일 ({form.skillFiles.length}/{MAX_AGENT_SKILL_FILES})
                  </span>
                  <p>비워 두면 워크스페이스 기본 템플릿이 적용됩니다.</p>
                </div>
                <button
                  className="secondary-action workspace-task-refresh"
                  type="button"
                  onClick={onSkillFileAdd}
                  disabled={submitting || form.skillFiles.length >= MAX_AGENT_SKILL_FILES}
                >
                  스킬 파일 추가
                </button>
              </div>

              {form.skillFiles.map((skillFile, index) => (
                <div className="workspace-agent-skill-file" key={skillFile.id}>
                  <div className="workspace-section-heading">
                    <span className="workspace-detail-label">스킬 {index + 1}</span>
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
                    maxLength={MAX_AGENT_SKILL_FILE_NAME_CHARS}
                    onChange={(event) =>
                      onSkillFileChange(skillFile.id, "fileName", event.target.value)
                    }
                    placeholder="backend-agent.md"
                    value={skillFile.fileName}
                  />
                  <textarea
                    disabled={submitting}
                    maxLength={MAX_AGENT_SKILL_CONTENT_CHARS}
                    onChange={(event) =>
                      onSkillFileChange(skillFile.id, "content", event.target.value)
                    }
                    placeholder="# Role&#10;- 에이전트가 준수할 작업 지침을 입력하세요."
                    rows={5}
                    value={skillFile.content}
                  />
                </div>
              ))}
            </section>

            {mutation.status === "error" && mutation.message ? (
              <div className="form-notice form-notice-error" role="alert">
                <AlertCircle aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}
            {mutation.status === "success" && mutation.message ? (
              <div className="form-notice form-notice-success" role="status">
                <ShieldCheck aria-hidden="true" />
                <span>{mutation.message}</span>
              </div>
            ) : null}
          </div>

          <aside className="workspace-agent-hire-preview" aria-label="에이전트 미리보기">
            <div className="workspace-row-icon">
              <UserPlus aria-hidden="true" />
            </div>
            {form.emoji.trim() ? <strong>{form.emoji.trim()}</strong> : null}
            <h3>{formatAgentCategory(form.category)}</h3>
            <p>OpenClaw 게이트웨이 연동 시 2D 오피스 스테이지에 자동 배치됩니다.</p>
          </aside>

          <div className="workspace-modal-footer">
            <button className="primary-action primary-action-active" type="submit" disabled={!canSubmit}>
              {submitting ? (
                <Loader2 className="spin" aria-hidden="true" />
              ) : (
                <UserPlus aria-hidden="true" />
              )}
              <span>{submitting ? "등록 중" : "에이전트 등록"}</span>
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
