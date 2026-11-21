"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { AlertCircle, ArrowLeft, FolderPlus } from "lucide-react";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { Button, Input, Textarea } from "@/components/ui";
import { getStoredUser, type ApiError } from "@/lib/api-client";
import { hydrateDesktopAccessToken } from "@/lib/auth";
import { createWorkspace } from "@/lib/api/workspaces";

const NAME_MAX = 100;
const DESCRIPTION_MAX = 500;

export default function NewWorkspacePage() {
  const router = useRouter();
  const [hydrated, setHydrated] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    void hydrateDesktopAccessToken().then(() => {
      if (cancelled) return;
      if (!getStoredUser()) {
        router.replace("/");
        return;
      }
      setHydrated(true);
    });
    return () => {
      cancelled = true;
    };
  }, [router]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmedName = name.trim();
    if (!trimmedName) {
      setError("워크스페이스 이름은 필수입니다.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      const created = await createWorkspace({
        name: trimmedName,
        description: description.trim() || undefined,
      });
      router.replace(`/workspaces/${created.workspaceId}`);
    } catch (err) {
      const apiErr = err as ApiError;
      setError(apiErr?.message ?? "워크스페이스 생성에 실패했습니다.");
      setSubmitting(false);
    }
  }

  if (!hydrated) {
    return null;
  }

  return (
    <AppTopChrome
      title="새 워크스페이스"
      backHref="/workspaces"
      backLabel="워크스페이스 목록"
    >
      <div className="ao-page-container" style={{ maxWidth: 680 }}>
        <div>
          <Link href="/workspaces" className="app-top-chrome-link px-0 mb-2 inline-flex">
            <ArrowLeft size={14} aria-hidden="true" />
            <span>워크스페이스 목록으로 돌아가기</span>
          </Link>
          <p className="ao-page-eyebrow">워크스페이스 개설</p>
          <h1 className="ao-page-title">새 워크스페이스 만들기</h1>
          <p className="ao-page-subtitle">
            팀 또는 프로젝트 단위의 워크스페이스를 생성합니다. 생성자에게는 관리자(ADMIN) 권한이
            부여됩니다.
          </p>
        </div>

        <section className="ao-surface-panel" aria-label="워크스페이스 기본 정보">
          <div className="ao-surface-panel-header">
            <h2>워크스페이스 정보</h2>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-5">
            <label className="form-field">
              <span>
                <strong>워크스페이스 이름</strong>
                <small>
                  {name.length}/{NAME_MAX}
                </small>
              </span>
              <Input
                value={name}
                onChange={(event) => setName(event.target.value)}
                maxLength={NAME_MAX}
                placeholder="예: 백엔드 엔지니어링 팀"
                autoFocus
                disabled={submitting}
              />
            </label>

            <label className="form-field">
              <span>
                <strong>설명</strong>
                <small>
                  {description.length}/{DESCRIPTION_MAX}
                </small>
              </span>
              <Textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                maxLength={DESCRIPTION_MAX}
                rows={4}
                placeholder="이 워크스페이스에서 다룰 저장소, 목표, 에이전트 운영 기준을 입력하세요. (선택)"
                disabled={submitting}
              />
            </label>

            {error ? (
              <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                <AlertCircle size={15} aria-hidden="true" />
                <span>{error}</span>
              </div>
            ) : null}

            <div className="flex items-center justify-end gap-2">
              <Link href="/workspaces" className="secondary-action">
                <span>취소</span>
              </Link>
              <Button type="submit" loading={submitting} disabled={!name.trim()}>
                <FolderPlus size={15} aria-hidden="true" />
                <span>{submitting ? "생성 중" : "워크스페이스 생성"}</span>
              </Button>
            </div>
          </form>
        </section>
      </div>
    </AppTopChrome>
  );
}
