"use client";

import { AlertCircle, ArrowLeft, CheckCircle2, Loader2, Plus, Save } from "lucide-react";
import Link from "next/link";
import { useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api-client";
import { createWorkspace, type WorkspaceSummary } from "@/lib/workspaces";

const TEMP_MEMBER_GOOGLE_SUB =
  process.env.NEXT_PUBLIC_DEV_MEMBER_GOOGLE_SUB ?? "google-1";
const NAME_MAX_LENGTH = 100;
const DESCRIPTION_MAX_LENGTH = 500;

type SubmitState =
  | { status: "idle"; created: null; error: null }
  | { status: "submitting"; created: null; error: null }
  | { status: "created"; created: WorkspaceSummary; error: null }
  | { status: "error"; created: null; error: string };

export default function NewWorkspacePage() {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [submitState, setSubmitState] = useState<SubmitState>({
    status: "idle",
    created: null,
    error: null,
  });

  const isSubmitting = submitState.status === "submitting";
  const trimmedName = name.trim();
  const trimmedDescription = description.trim();
  const canSubmit = trimmedName.length > 0 && !isSubmitting;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!trimmedName) {
      setSubmitState({
        status: "error",
        created: null,
        error: "워크스페이스 이름을 입력해 주세요.",
      });
      return;
    }

    setSubmitState({ status: "submitting", created: null, error: null });

    try {
      const created = await createWorkspace(TEMP_MEMBER_GOOGLE_SUB, {
        name: trimmedName,
        description: trimmedDescription || undefined,
      });
      setSubmitState({ status: "created", created, error: null });
    } catch (error) {
      setSubmitState({
        status: "error",
        created: null,
        error: messageFromError(error),
      });
    }
  };

  return (
    <main className="workspace-create-shell">
      <section className="workspace-create-panel" aria-label="워크스페이스 생성">
        <div className="workspace-create-header">
          <Link className="secondary-action" href="/">
            <ArrowLeft aria-hidden="true" />
            <span>목록으로</span>
          </Link>
          <div className="workspace-create-title">
            <div className="workspace-create-icon">
              <Plus aria-hidden="true" />
            </div>
            <div>
              <p className="eyebrow">Workspace</p>
              <h1>워크스페이스 생성</h1>
            </div>
          </div>
        </div>

        <form className="workspace-create-form" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>
              <strong>이름</strong>
              <small>{name.length}/{NAME_MAX_LENGTH}</small>
            </span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={NAME_MAX_LENGTH}
              placeholder="예: Product Engineering"
              disabled={isSubmitting}
            />
          </label>

          <label className="form-field">
            <span>
              <strong>설명</strong>
              <small>{description.length}/{DESCRIPTION_MAX_LENGTH}</small>
            </span>
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              maxLength={DESCRIPTION_MAX_LENGTH}
              placeholder="워크스페이스 목적이나 팀 설명을 입력하세요."
              rows={5}
              disabled={isSubmitting}
            />
          </label>

          {submitState.status === "error" ? (
            <div className="form-notice form-notice-error" role="alert">
              <AlertCircle aria-hidden="true" />
              <span>{submitState.error}</span>
            </div>
          ) : null}

          {submitState.status === "created" ? (
            <div className="form-notice form-notice-success" role="status">
              <CheckCircle2 aria-hidden="true" />
              <span>{submitState.created.name} 워크스페이스가 생성되었습니다.</span>
            </div>
          ) : null}

          <div className="form-actions">
            <Link className="secondary-action" href="/">
              취소
            </Link>
            <button className="primary-action primary-action-active" type="submit" disabled={!canSubmit}>
              {isSubmitting ? <Loader2 className="spin" aria-hidden="true" /> : <Save aria-hidden="true" />}
              <span>{isSubmitting ? "생성 중" : "생성"}</span>
            </button>
          </div>
        </form>
      </section>

      <aside className="workspace-create-side" aria-label="생성 정보">
        <strong>{TEMP_MEMBER_GOOGLE_SUB}</strong>
        <span>현재 임시 생성자 식별자</span>
      </aside>
    </main>
  );
}

function messageFromError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "워크스페이스 생성 요청을 처리하지 못했습니다.";
}
