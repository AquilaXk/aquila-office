"use client";

import { ArrowLeft, CheckCircle2, Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { resolveAccessToken } from "@/lib/auth";
import { fetchSlackIntegrations, type SlackIntegration } from "@/lib/slack";

const RETURN_TO_KEYS = ["aquila-office.pendingSlackReturnTo", "aio.slack.returnTo"];
const PENDING_WORKSPACE_KEYS = ["aquila-office.pendingSlackWorkspaceId", "aio.slack.pendingWorkspaceId"];

export default function SlackIntegrationSuccessPage() {
  const router = useRouter();
  const [returnTo, setReturnTo] = useState("/workspaces");
  const [message, setMessage] = useState("Slack 연동 정보를 갱신하는 중입니다.");
  const [refreshing, setRefreshing] = useState(true);

  useEffect(() => {
    let ignore = false;
    let redirectTimer: ReturnType<typeof setTimeout> | null = null;

    async function refreshIntegration() {
      const nextReturnTo = safeReturnTo(firstStoredValue(RETURN_TO_KEYS));
      setReturnTo(nextReturnTo);

      const params = new URLSearchParams(window.location.search);
      const workspaceId = parseWorkspaceId(
        params.get("workspaceId") ?? firstStoredValue(PENDING_WORKSPACE_KEYS),
      );

      if (workspaceId === null) {
        clearPendingSlackStorage();
        setRefreshing(false);
        setMessage("Slack 연동이 완료되었습니다.");
        redirectTimer = setTimeout(() => router.push(nextReturnTo), 2500);
        return;
      }

      try {
        const integrations = await fetchSlackIntegrations(resolveAccessToken(), workspaceId);
        if (ignore) {
          return;
        }

        const latestIntegration = integrations[0] ?? null;
        if (latestIntegration) {
          writeSlackIntegration(workspaceId, latestIntegration);
        }
        clearPendingSlackStorage();
        setRefreshing(false);
        setMessage("Slack 연동이 완료되었습니다.");
        redirectTimer = setTimeout(() => router.push(nextReturnTo), 2500);
      } catch {
        if (ignore) {
          return;
        }
        setRefreshing(false);
        setMessage("Slack 연동은 완료되었지만 목록 갱신에 실패했습니다.");
      }
    }

    void refreshIntegration();

    return () => {
      ignore = true;
      if (redirectTimer) {
        clearTimeout(redirectTimer);
      }
    };
  }, [router]);

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
            <p>{message}</p>
          </div>
        </div>

        <button
          className="primary-action primary-action-active workspace-invite-submit"
          type="button"
          onClick={() => router.push(returnTo)}
        >
          {refreshing ? <Loader2 className="spin" aria-hidden="true" /> : <ArrowLeft aria-hidden="true" />}
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

function firstStoredValue(keys: string[]): string | null {
  for (const key of keys) {
    const value = window.localStorage.getItem(key);
    if (value) {
      return value;
    }
  }

  return null;
}

function parseWorkspaceId(value: string | null): number | null {
  const workspaceId = Number(value);
  return Number.isFinite(workspaceId) && workspaceId > 0 ? workspaceId : null;
}

function clearPendingSlackStorage() {
  for (const key of [...RETURN_TO_KEYS, ...PENDING_WORKSPACE_KEYS]) {
    window.localStorage.removeItem(key);
  }
}

function writeSlackIntegration(workspaceId: number, integration: SlackIntegration) {
  window.localStorage.setItem(
    `aio.workspace.${workspaceId}.integrations.slack`,
    JSON.stringify(integration),
  );
}
