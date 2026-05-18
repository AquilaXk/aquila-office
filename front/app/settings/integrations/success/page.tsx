"use client";

import { ArrowLeft, CheckCircle2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const RETURN_TO_KEY = "aio.slack.returnTo";
const PENDING_WORKSPACE_KEY = "aio.slack.pendingWorkspaceId";

export default function SlackIntegrationSuccessPage() {
  const router = useRouter();
  const [returnTo] = useState(() =>
    typeof window === "undefined" ? "/workspaces" : safeReturnTo(window.localStorage.getItem(RETURN_TO_KEY)),
  );

  useEffect(() => {
    window.localStorage.removeItem(PENDING_WORKSPACE_KEY);
    window.localStorage.removeItem(RETURN_TO_KEY);
  }, []);

  return (
    <main className="workspace-create-shell">
      <section className="workspace-create-panel" aria-label="Slack 연동 성공">
        <div className="workspace-create-header">
          <div className="workspace-create-icon">
            <CheckCircle2 aria-hidden="true" />
          </div>
          <div className="workspace-create-title">
            <p className="eyebrow">Slack</p>
            <h1>연동 완료</h1>
            <p>Slack 연동 요청이 완료되었습니다. 워크스페이스 설정에서 연결 상태를 확인하세요.</p>
          </div>
        </div>

        <button
          className="primary-action primary-action-active workspace-invite-submit"
          type="button"
          onClick={() => router.push(returnTo)}
        >
          <ArrowLeft aria-hidden="true" />
          <span>연동 목록으로 이동</span>
        </button>
      </section>
    </main>
  );
}

function safeReturnTo(value: string | null): string {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return "/workspaces";
  }
  return value;
}
