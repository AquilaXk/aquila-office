"use client";

import {
  AlertCircle,
  ArrowLeft,
  Loader2,
  Plus,
  Save,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api";
import { readAuthTokens, resolveAccessToken } from "@/lib/auth";
import { createWorkspace } from "@/lib/workspaces";

const NAME_MAX_LENGTH = 100;
const DESCRIPTION_MAX_LENGTH = 500;

type SubmitState =
  | { status: "idle"; error: null }
  | { status: "submitting"; error: null }
  | { status: "error"; error: string };

export default function NewWorkspacePage() {
  const router = useRouter();
  const [hydrated, setHydrated] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [submitState, setSubmitState] = useState<SubmitState>({
    status: "idle",
    error: null,
  });

  useEffect(() => {
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) {
        return;
      }

      if (!readAuthTokens()) {
        router.replace("/");
        return;
      }

      setHydrated(true);
    });

    return () => {
      cancelled = true;
    };
  }, [router]);

  const isSubmitting = submitState.status === "submitting";
  const trimmedName = name.trim();
  const canSubmit = trimmedName.length > 0 && !isSubmitting;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!trimmedName) {
      setSubmitState({
        status: "error",
        error: "워크스페이스 이름은 필수입니다.",
      });
      return;
    }

    setSubmitState({ status: "submitting", error: null });

    try {
      const created = await createWorkspace(resolveAccessToken(), {
        name: trimmedName,
        description: description.trim() || undefined,
      });
      router.replace(`/workspaces/${created.workspaceId}`);
    } catch (error) {
      setSubmitState({
        status: "error",
        error: messageFromError(error),
      });
    }
  };

  if (!hydrated) {
    return null;
  }

  return (
    <main className="workspace-create-shell">
      <section className="workspace-create-panel" aria-label="워크스페이스 생성">
        <div className="workspace-create-header">
          <Link className="secondary-action" href="/workspaces">
            <ArrowLeft aria-hidden="true" />
            <span>BACK TO WORKSPACES</span>
          </Link>
          <div className="workspace-create-title">
            <div className="workspace-create-icon">
              <Plus aria-hidden="true" />
            </div>
            <div>
              <p className="eyebrow">STEP 01 / 02</p>
              <h1>FORM A NEW WORKSPACE</h1>
              <p className="topbar-summary">
                워크스페이스 이름과 간단한 소개를 적어주세요. 생성자에게는 ADMIN 권한이 부여됩니다.
              </p>
            </div>
          </div>
        </div>

        <form className="workspace-create-form" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>
              <strong>WORKSPACE NAME</strong>
              <small>{name.length}/{NAME_MAX_LENGTH}</small>
            </span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={NAME_MAX_LENGTH}
              placeholder="예: Product Engineering"
              autoFocus
              disabled={isSubmitting}
            />
          </label>

          <label className="form-field">
            <span>
              <strong>BRIEFING</strong>
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

          <div className="form-actions">
            <Link className="secondary-action" href="/workspaces">
              CANCEL
            </Link>
            <button className="primary-action primary-action-active" type="submit" disabled={!canSubmit}>
              {isSubmitting ? <Loader2 className="spin" aria-hidden="true" /> : <Save aria-hidden="true" />}
              <span>{isSubmitting ? "FORMING" : "FORM WORKSPACE"}</span>
            </button>
          </div>
        </form>
      </section>

      <aside className="workspace-create-side" aria-label="생성 정보">
        <strong>{resolveAccessToken() ? "SESSION READY" : "SESSION EMPTY"}</strong>
        <span>access token</span>
      </aside>
    </main>
  );
}

function messageFromError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "워크스페이스 생성에 실패했습니다.";
}
