"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { use, useEffect, useState } from "react";
import { AlertCircle, ArrowRight, CheckCircle2, Clock, MailCheck } from "lucide-react";
import { PixelAvatar } from "@/components/arcade";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { Button } from "@/components/ui";
import { getStoredUser, type ApiError } from "@/lib/api-client";
import { hydrateDesktopAccessToken } from "@/lib/auth";
import {
  acceptInvite,
  getInviteInfo,
  type InviteInfo,
} from "@/lib/api/invites";

type LoadState =
  | { kind: "loading" }
  | { kind: "ready"; invite: InviteInfo }
  | { kind: "error"; message: string };

export default function InviteAcceptPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = use(params);
  const router = useRouter();
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [authenticated, setAuthenticated] = useState(false);
  const [accepting, setAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState("");

  useEffect(() => {
    let cancelled = false;
    void hydrateDesktopAccessToken().then(() => {
      if (!cancelled) {
        setAuthenticated(Boolean(getStoredUser()));
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const invite = await getInviteInfo(token);
        if (!cancelled) {
          setState({ kind: "ready", invite });
        }
      } catch (err) {
        const apiErr = err as ApiError;
        if (!cancelled) {
          setState({
            kind: "error",
            message: apiErr?.message ?? "초대 정보를 불러오지 못했습니다.",
          });
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  async function handleAccept() {
    setAcceptError("");
    setAccepting(true);
    try {
      await acceptInvite(token);
      router.replace("/workspaces");
    } catch (err) {
      const apiErr = err as ApiError;
      setAcceptError(apiErr?.message ?? "초대 수락에 실패했습니다.");
      setAccepting(false);
    }
  }

  const loginReturnUrl = `/auth?returnTo=${encodeURIComponent(`/invites/${token}`)}`;

  return (
    <AppTopChrome
      title="워크스페이스 초대"
      backHref={authenticated ? "/workspaces" : "/"}
      backLabel={authenticated ? "워크스페이스 목록" : "시작 화면"}
      showWorkspaceLinks={authenticated}
    >
      <div className="ao-page-container ao-page-container-narrow">
        <div className="text-center">
          <p className="ao-page-eyebrow">워크스페이스 초대</p>
          <h1 className="ao-page-title">초대 확인 및 참여</h1>
          <p className="ao-page-subtitle">
            워크스페이스 정보와 부여된 역할을 확인한 뒤 초대를 수락하세요.
          </p>
        </div>

        <section className="ao-surface-panel" aria-label="초대 상세 정보">
          {state.kind === "loading" ? (
            <div className="flex flex-col gap-3 py-4">
              <div className="h-5 w-48 rounded bg-[var(--ui-surface-muted)]" />
              <div className="h-4 w-64 rounded bg-[var(--ui-surface-muted)]" />
              <div className="h-10 w-full rounded bg-[var(--ui-surface-muted)]" />
            </div>
          ) : null}

          {state.kind === "error" ? (
            <div className="flex flex-col gap-4">
              <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                <AlertCircle size={16} aria-hidden="true" />
                <span>{state.message}</span>
              </div>
              <Link
                href={authenticated ? "/workspaces" : "/auth"}
                className="secondary-action justify-center"
              >
                <span>{authenticated ? "내 워크스페이스로 이동" : "로그인 화면으로 이동"}</span>
              </Link>
            </div>
          ) : null}

          {state.kind === "ready" ? (
            <InviteDetail
              invite={state.invite}
              authenticated={authenticated}
              loginReturnUrl={loginReturnUrl}
              accepting={accepting}
              acceptError={acceptError}
              onAccept={() => void handleAccept()}
            />
          ) : null}
        </section>
      </div>
    </AppTopChrome>
  );
}

function InviteDetail({
  invite,
  authenticated,
  loginReturnUrl,
  accepting,
  acceptError,
  onAccept,
}: {
  invite: InviteInfo;
  authenticated: boolean;
  loginReturnUrl: string;
  accepting: boolean;
  acceptError: string;
  onAccept: () => void;
}) {
  const blocked = invite.expired || invite.status !== "PENDING";
  const blockedReason =
    invite.status === "ACCEPTED"
      ? "이미 수락된 초대입니다."
      : invite.status === "REVOKED"
        ? "폐기된 초대입니다."
        : invite.expired || invite.status === "EXPIRED"
          ? "만료된 초대입니다."
          : null;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-start gap-4">
        <div className="p-2 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-surface-raised)] shrink-0">
          <PixelAvatar kind="mira" size={3} />
        </div>
        <div className="min-w-0 flex flex-col gap-2">
          <h2 className="m-0 text-[16px] font-semibold text-[var(--ui-text-strong)] break-words">
            {invite.workspaceName}
          </h2>
          <div className="flex flex-wrap items-center gap-2 text-[12px]">
            <span className="role-badge">{invite.role}</span>
            <span className="inline-flex items-center gap-1 text-[var(--ui-text-muted)] font-mono">
              <Clock size={12} aria-hidden="true" />
              만료: {formatDateTime(invite.expiresAt)}
            </span>
          </div>
          {invite.targetEmail ? (
            <p className="m-0 text-[12px] text-[var(--ui-text-secondary)]">
              초대 대상: {invite.targetEmail}
            </p>
          ) : null}
        </div>
      </div>

      {blockedReason ? (
        <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
          <AlertCircle size={15} aria-hidden="true" />
          <span>{blockedReason}</span>
        </div>
      ) : null}

      {acceptError ? (
        <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
          <AlertCircle size={15} aria-hidden="true" />
          <span>{acceptError}</span>
        </div>
      ) : null}

      {!authenticated ? (
        <div className="flex flex-col gap-3">
          <div className="ao-inline-notice">
            <MailCheck size={15} aria-hidden="true" />
            <span>초대를 수락하려면 먼저 Google 계정으로 로그인해 주세요.</span>
          </div>
          <Link
            href={loginReturnUrl}
            className="primary-action primary-action-active justify-center"
          >
            <span>로그인 후 초대 수락</span>
            <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </div>
      ) : (
        <Button onClick={onAccept} loading={accepting} disabled={blocked} className="w-full">
          <CheckCircle2 size={15} aria-hidden="true" />
          <span>{blocked ? "수락 불가" : "초대 수락"}</span>
        </Button>
      )}
    </div>
  );
}

function formatDateTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString("ko-KR", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}
