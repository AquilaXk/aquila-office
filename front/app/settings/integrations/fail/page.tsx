"use client";

import { ArrowLeft, ShieldAlert } from "lucide-react";
import { useRouter } from "next/navigation";

const RETURN_TO_KEYS = ["aquila-office.pendingSlackReturnTo", "aio.slack.returnTo"];

export default function SlackIntegrationFailPage() {
  const router = useRouter();

  function retry() {
    const returnTo =
      typeof window === "undefined"
        ? "/workspaces"
        : safeReturnTo(firstStoredValue(RETURN_TO_KEYS));
    router.push(returnTo);
  }

  return (
    <main className="workspace-create-shell">
      <section className="workspace-create-panel" aria-label="Slack 연동 실패">
        <div className="workspace-create-header">
          <div className="workspace-create-icon">
            <ShieldAlert aria-hidden="true" />
          </div>
          <div className="workspace-create-title">
            <p className="eyebrow">Slack</p>
            <h1>연동 실패</h1>
            <p>Slack 연동을 완료하지 못했습니다. 워크스페이스 설정 화면으로 돌아가 다시 시도하세요.</p>
          </div>
        </div>

        <button
          className="primary-action primary-action-active workspace-invite-submit"
          type="button"
          onClick={retry}
        >
          <ArrowLeft aria-hidden="true" />
          <span>재시도</span>
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

function firstStoredValue(keys: string[]): string | null {
  for (const key of keys) {
    const value = window.localStorage.getItem(key);
    if (value) {
      return value;
    }
  }

  return null;
}
