"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { use, useEffect, useState } from "react";
import { Button } from "@/components/ui";
import { GlyphText, T4Screen, T4Panel, PixelAvatar } from "@/components/arcade";
import { t4 } from "@/components/arcade/tokens";
import { getStoredUser, type ApiError } from "@/lib/api-client";
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
  const [accepting, setAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState("");

  useEffect(() => {
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;
      if (!getStoredUser()) {
        router.replace("/");
      }
    });
    return () => {
      cancelled = true;
    };
  }, [router, token]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const invite = await getInviteInfo(token);
        if (!cancelled) setState({ kind: "ready", invite });
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

  return (
    <T4Screen title="WORKSPACE INVITE">
      <div
        style={{
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "30px 22px",
        }}
      >
        <div
          style={{
            width: "100%",
            maxWidth: 520,
            display: "flex",
            flexDirection: "column",
            gap: 20,
          }}
        >
          <header style={{ textAlign: "center" }}>
            <div
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 12,
                fontWeight: 600,
                letterSpacing: "0.04em",
                color: t4.mp,
                marginBottom: 6,
              }}
            >
              <GlyphText glyph="◆">INVITATION</GlyphText>
            </div>
            <h1
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 22,
                fontWeight: 600,
                letterSpacing: "-0.02em",
                margin: 0,
                color: t4.ink,
              }}
            >
              JOIN A WORKSPACE
            </h1>
            <p
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 13,
                color: t4.dim,
                marginTop: 8,
              }}
            >
              Confirm the invitation and join the workspace.
            </p>
          </header>

          <T4Panel label="INVITE" accent={t4.mp} style={{ position: "relative", padding: 22 }}>
            {state.kind === "loading" && <Skeleton />}
            {state.kind === "error" && (
              <p
                style={{
                  textAlign: "center",
                  fontFamily: "var(--font-sans)",
                  fontSize: 14,
                  color: t4.hp,
                }}
              >
                <GlyphText glyph="⚠">{state.message}</GlyphText>
              </p>
            )}
            {state.kind === "ready" && (
              <InviteBody
                invite={state.invite}
                accepting={accepting}
                acceptError={acceptError}
                onAccept={handleAccept}
              />
            )}
          </T4Panel>

          <Link
            href="/workspaces"
            style={{
              fontFamily: "var(--font-sans)",
              fontSize: 12,
              fontWeight: 500,
              color: t4.dim,
              textDecoration: "none",
              textAlign: "center",
            }}
          >
            <GlyphText glyph="▷">MY WORKSPACES</GlyphText>
          </Link>
        </div>
      </div>
    </T4Screen>
  );
}

function InviteBody({
  invite,
  accepting,
  acceptError,
  onAccept,
}: {
  invite: InviteInfo;
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
  const roleColor = invite.role === "ADMIN" ? t4.mp : t4.dim;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-start gap-4">
        <div
          style={{
            padding: 8,
            borderRadius: 10,
            border: `1px solid ${t4.line}`,
            background: "#121317",
            flexShrink: 0,
          }}
        >
          <PixelAvatar kind="mira" size={3} />
        </div>
        <div className="min-w-0 flex flex-col gap-2">
          <p
            className="break-words"
            style={{
              fontFamily: "var(--font-sans)",
              fontSize: 15,
              fontWeight: 600,
              color: t4.ink,
              margin: 0,
            }}
          >
            <GlyphText glyph="♦" truncate>{invite.workspaceName.toUpperCase()}</GlyphText>
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <span
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 12,
                fontWeight: 500,
                color: roleColor,
                padding: "2px 8px",
                borderRadius: 9999,
                border: `1px solid ${t4.line}`,
                background: "rgba(50, 121, 249, 0.1)",
              }}
            >
              {invite.role}
            </span>
            <span
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 12,
                color: t4.dim,
              }}
            >
              Expires {formatDateTime(invite.expiresAt)}
            </span>
          </div>
        </div>
      </div>

      {blockedReason && (
        <p
          style={{
            fontFamily: "var(--font-mono-arcade)",
            fontSize: 13,
            color: t4.hp,
            textAlign: "center",
            padding: "10px",
            border: `1px solid ${t4.hp}`,
            background: "rgba(255,85,119,0.06)",
          }}
        >
          <GlyphText glyph="⚠">{blockedReason}</GlyphText>
        </p>
      )}

      {acceptError && (
        <p
          style={{
            fontFamily: "var(--font-mono-arcade)",
            fontSize: 13,
            color: t4.hp,
            textAlign: "center",
          }}
        >
          <GlyphText glyph="⚠">{acceptError}</GlyphText>
        </p>
      )}

      <Button
        onClick={onAccept}
        loading={accepting}
        disabled={blocked}
        className="w-full"
      >
        <GlyphText glyph="▶">{blocked ? "LOCKED" : "ACCEPT INVITE"}</GlyphText>
      </Button>
    </div>
  );
}

function Skeleton() {
  return (
    <div className="flex flex-col gap-3" style={{ animation: "t4-pulse 1.4s ease-in-out infinite" }}>
      <div className="flex gap-4">
        <div
          style={{
            width: 56,
            height: 56,
            border: `1px solid ${t4.line}`,
            background: "rgba(0,0,0,0.3)",
          }}
        />
        <div className="flex-1 flex flex-col gap-2">
          <div style={{ height: 10, background: "rgba(255,255,255,0.04)", width: "70%" }} />
          <div style={{ height: 8, background: "rgba(255,255,255,0.04)", width: "40%" }} />
        </div>
      </div>
      <div style={{ height: 36, background: "rgba(255,255,255,0.04)" }} />
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
