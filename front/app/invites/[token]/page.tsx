"use client";

import {
  AlertCircle,
  Clock3,
  Loader2,
  Mail,
  RefreshCw,
  ShieldCheck,
  UserPlus,
  WifiOff,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { use, useCallback, useEffect, useMemo, useState } from "react";
import { ApiError } from "@/lib/api";
import { isAuthenticated, resolveAccessToken } from "@/lib/auth";
import {
  acceptWorkspaceInvite,
  fetchWorkspaceInvite,
  type AcceptedWorkspaceInvite,
  type WorkspaceInvite,
} from "@/lib/invites";

type InviteState =
  | { status: "loading"; invite: null; accepted: null; error: null }
  | { status: "ready"; invite: WorkspaceInvite; accepted: null; error: null }
  | { status: "accepted"; invite: WorkspaceInvite; accepted: AcceptedWorkspaceInvite; error: null }
  | { status: "error" | "offline"; invite: null; accepted: null; error: string };

export default function WorkspaceInviteAcceptPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = use(params);
  const router = useRouter();
  const [state, setState] = useState<InviteState>({
    status: "loading",
    invite: null,
    accepted: null,
    error: null,
  });
  const [isAccepting, setIsAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState<string | null>(null);

  const reloadInvite = useCallback(async () => {
    if (!isAuthenticated()) {
      router.replace("/");
      return;
    }

    setState({ status: "loading", invite: null, accepted: null, error: null });
    setAcceptError(null);

    try {
      const invite = await fetchWorkspaceInvite(token);
      setState({ status: "ready", invite, accepted: null, error: null });
    } catch (error) {
      setState(toFailureState(error, "초대 정보를 확인하지 못했습니다."));
    }
  }, [router, token]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialInvite() {
      if (!isAuthenticated()) {
        router.replace("/");
        return;
      }

      try {
        const invite = await fetchWorkspaceInvite(token);
        if (!ignore) {
          setState({ status: "ready", invite, accepted: null, error: null });
        }
      } catch (error) {
        if (!ignore) {
          setState(toFailureState(error, "초대 정보를 확인하지 못했습니다."));
        }
      }
    }

    void loadInitialInvite();

    return () => {
      ignore = true;
    };
  }, [router, token]);

  const invite = state.invite;
  const expired = invite ? isInviteExpired(invite) : false;
  const isPending = invite?.status === "PENDING" && !expired;
  const inviteRole = invite?.role ?? "MEMBER";
  const workspaceName = invite?.workspaceName ?? (invite ? `Workspace #${invite.workspaceId}` : "-");
  const blockedReason = useMemo(() => {
    if (!invite || (invite.status === "PENDING" && !expired)) {
      return null;
    }
    if (invite.status === "ACCEPTED") {
      return "이미 수락된 초대입니다.";
    }
    if (invite.status === "REVOKED") {
      return "취소된 초대입니다.";
    }
    return "만료된 초대입니다.";
  }, [expired, invite]);

  const acceptInvite = async () => {
    if (!isPending) {
      return;
    }

    setIsAccepting(true);
    setAcceptError(null);

    try {
      await acceptWorkspaceInvite(token, resolveAccessToken());
      router.replace("/workspaces");
    } catch (error) {
      setAcceptError(messageFromError(error, "초대를 수락하지 못했습니다."));
    } finally {
      setIsAccepting(false);
    }
  };

  return (
    <main className="invite-shell">
      <section className="invite-panel" aria-label="워크스페이스 초대">
        <header className="invite-header">
          <div className="invite-header-icon">
            <UserPlus aria-hidden="true" />
          </div>
          <div>
            <p className="eyebrow">Workspace Invite</p>
            <h1>워크스페이스 초대</h1>
          </div>
        </header>

        {state.status === "loading" ? (
          <InviteStateMessage
            tone="neutral"
            icon={<Loader2 className="spin" aria-hidden="true" />}
            title="초대 확인 중"
            message="초대 링크 정보를 불러오고 있습니다."
          />
        ) : null}

        {state.status === "error" || state.status === "offline" ? (
          <InviteStateMessage
            tone={state.status === "offline" ? "offline" : "danger"}
            icon={
              state.status === "offline" ? (
                <WifiOff aria-hidden="true" />
              ) : (
                <AlertCircle aria-hidden="true" />
              )
            }
            title={state.status === "offline" ? "연결 실패" : "초대 확인 실패"}
            message={state.error}
            action={
              <button className="secondary-action" type="button" onClick={() => void reloadInvite()}>
                <RefreshCw aria-hidden="true" />
                <span>다시 시도</span>
              </button>
            }
          />
        ) : null}

        {invite ? (
          <section className="invite-content" aria-label="초대 정보">
            <div className="invite-summary">
              <div className="invite-summary-icon">
                <Mail aria-hidden="true" />
              </div>
              <div>
                <span className="invite-label">Workspace</span>
                <strong>{workspaceName}</strong>
                <p>{formatInviteTarget(invite)}</p>
              </div>
            </div>

            <dl className="invite-meta">
              <div>
                <dt>Role</dt>
                <dd>{inviteRole}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>
                  <span className={`invite-status invite-status-${invite.status.toLowerCase()}`}>
                    {invite.status}
                  </span>
                </dd>
              </div>
              <div>
                <dt>Expires</dt>
                <dd>{formatDateTime(invite.expiresAt)}</dd>
              </div>
              <div>
                <dt>Expired</dt>
                <dd>{expired ? "YES" : "NO"}</dd>
              </div>
            </dl>

            {blockedReason ? (
              <div className="invite-notice invite-notice-danger">
                <AlertCircle aria-hidden="true" />
                <span>{blockedReason}</span>
              </div>
            ) : null}

            {acceptError ? (
              <div className="invite-notice invite-notice-danger">
                <AlertCircle aria-hidden="true" />
                <span>{acceptError}</span>
              </div>
            ) : null}

            <button
              className="invite-accept-button"
              type="button"
              onClick={() => void acceptInvite()}
              disabled={!isPending || isAccepting}
            >
              {isAccepting ? <Loader2 className="spin" aria-hidden="true" /> : <ShieldCheck aria-hidden="true" />}
              <span>{isAccepting ? "수락 중" : "초대 수락"}</span>
            </button>
          </section>
        ) : null}
      </section>

      <aside className="invite-side" aria-label="수락 정보">
        <div>
          <Clock3 aria-hidden="true" />
          <strong>{invite ? formatDateTime(invite.expiresAt) : "-"}</strong>
          <span>초대 만료 시각</span>
        </div>
        <div>
          <ShieldCheck aria-hidden="true" />
          <strong>{resolveAccessToken() ? "설정됨" : "미설정"}</strong>
          <span>개발 access token</span>
        </div>
      </aside>
    </main>
  );
}

function InviteStateMessage({
  tone,
  icon,
  title,
  message,
  action,
}: {
  tone: "neutral" | "danger" | "offline";
  icon: React.ReactNode;
  title: string;
  message: string;
  action?: React.ReactNode;
}) {
  return (
    <div className={`invite-state invite-state-${tone}`}>
      {icon}
      <strong>{title}</strong>
      <p>{message}</p>
      {action}
    </div>
  );
}

function toFailureState(error: unknown, fallback: string): InviteState {
  if (error instanceof ApiError) {
    return {
      status: "error",
      invite: null,
      accepted: null,
      error: error.message,
    };
  }

  return {
    status: "offline",
    invite: null,
    accepted: null,
    error: fallback,
  };
}

function messageFromError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return fallback;
}

function isInviteExpired(invite: WorkspaceInvite): boolean {
  if (typeof invite.expired === "boolean") {
    return invite.expired;
  }

  const expiresAt = new Date(invite.expiresAt);
  return !Number.isNaN(expiresAt.getTime()) && expiresAt.getTime() <= Date.now();
}

function formatInviteTarget(invite: WorkspaceInvite): string {
  return invite.targetEmail?.trim() || "SHARE LINK";
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
