"use client";

import { AlertCircle, ArrowLeft, Loader2, RefreshCw } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { Button } from "@/components/ui";
import type { WorkspaceDetail } from "@/lib/api/workspaces";

export default function WorkspaceShell({
  children,
}: {
  workspace: WorkspaceDetail;
  children: ReactNode;
  sidebar?: ReactNode;
}) {
  return <>{children}</>;
}

export function WorkspaceErrorState({
  message,
  title = "오류",
  backHref = "/workspaces",
  backLabel = "워크스페이스 목록",
  onRetry,
}: {
  message: string;
  title?: string;
  backHref?: string;
  backLabel?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="app-chrome-shell">
      <AppTopChrome title={title} backHref={backHref} backLabel={backLabel} />
      <main className="app-top-chrome-main items-center justify-center px-4">
        <section
          className="ao-surface-panel w-full max-w-md text-center"
          style={{ borderColor: "var(--status-danger-border)" }}
        >
          <p
            className="inline-flex items-center gap-1.5"
            style={{
              fontSize: 12,
              fontWeight: 600,
              color: "var(--status-danger)",
              marginBottom: 10,
            }}
          >
            <AlertCircle size={14} aria-hidden="true" />
            <span>{title}</span>
          </p>
          <p style={{ fontSize: 14, color: "var(--ui-text-strong)", margin: 0 }}>{message}</p>
          <div className="mt-5 flex flex-wrap items-center justify-center gap-2">
            <Link href={backHref} className="inline-flex">
              <Button variant="secondary" icon={<ArrowLeft size={14} />}>
                {backLabel}
              </Button>
            </Link>
            {onRetry ? (
              <Button variant="primary" icon={<RefreshCw size={14} />} onClick={onRetry}>
                다시 시도
              </Button>
            ) : null}
          </div>
        </section>
      </main>
    </div>
  );
}

export function WorkspaceLoadingState({
  title = "워크스페이스 불러오는 중",
  message = "워크스페이스를 불러오는 중입니다...",
  backHref = "/workspaces",
  backLabel = "워크스페이스 목록",
}: {
  title?: string;
  message?: string;
  backHref?: string;
  backLabel?: string;
} = {}) {
  return (
    <div className="app-chrome-shell">
      <AppTopChrome title={title} backHref={backHref} backLabel={backLabel} />
      <main className="app-top-chrome-main items-center justify-center px-4">
        <div
          className="inline-flex items-center gap-2"
          style={{ fontSize: 13, fontWeight: 500, color: "var(--ui-text-secondary)" }}
        >
          <Loader2 size={15} className="spin" aria-hidden="true" />
          <span>{message}</span>
        </div>
      </main>
    </div>
  );
}
