"use client";

import { ArrowLeft, CheckCircle2, Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { resolveAccessToken } from "@/lib/auth";
import {
  clearSlackPendingConnectionState,
  fetchSlackIntegrations,
  readPendingSlackWorkspaceId,
  readSlackReturnTo,
  writeStoredSlackIntegration,
} from "@/lib/slack";

export default function SlackIntegrationSuccessPage() {
  const router = useRouter();
  const [returnTo, setReturnTo] = useState("/workspaces");
  const [message, setMessage] = useState("Slack 연동 정보를 갱신하는 중입니다.");
  const [refreshing, setRefreshing] = useState(true);

  useEffect(() => {
    let ignore = false;
    let redirectTimer: ReturnType<typeof setTimeout> | null = null;

    async function refreshIntegration() {
      const nextReturnTo = readSlackReturnTo(window.localStorage);
      setReturnTo(nextReturnTo);

      const params = new URLSearchParams(window.location.search);
      const workspaceId = parseWorkspaceId(
        params.get("workspaceId") ?? readPendingSlackWorkspaceId(window.localStorage),
      );

      if (workspaceId === null) {
        clearSlackPendingConnectionState(window.localStorage);
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
          writeStoredSlackIntegration(window.localStorage, workspaceId, latestIntegration);
        }
        clearSlackPendingConnectionState(window.localStorage);
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
    <AppTopChrome title="Slack 연동 완료" backHref={returnTo} backLabel="돌아가기">
      <div className="workspace-create-shell flex-1">
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
            {refreshing ? (
              <Loader2 className="spin" aria-hidden="true" />
            ) : (
              <ArrowLeft aria-hidden="true" />
            )}
            <span>연동 목록으로 이동</span>
          </button>
        </section>
      </div>
    </AppTopChrome>
  );
}

function parseWorkspaceId(value: string | null): number | null {
  const workspaceId = Number(value);
  return Number.isFinite(workspaceId) && workspaceId > 0 ? workspaceId : null;
}
