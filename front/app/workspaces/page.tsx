"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { getStoredUser, type AuthUser, type ApiError } from "@/lib/api-client";
import { logout } from "@/lib/auth";
import { listWorkspaces, type WorkspaceSummary } from "@/lib/api/workspaces";
import { GlyphText, T4Screen, T4Panel, PixelAvatar } from "@/components/arcade";
import { t4 } from "@/components/arcade/tokens";

export default function WorkspacesPage() {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [hydrated, setHydrated] = useState(false);
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;
      const stored = getStoredUser();
      if (!stored) {
        router.replace("/");
        return;
      }
      setUser(stored);
      setHydrated(true);
    });
    return () => {
      cancelled = true;
    };
  }, [router]);

  useEffect(() => {
    if (!hydrated) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError("");
      try {
        const data = await listWorkspaces();
        if (!cancelled) setWorkspaces(data ?? []);
      } catch (err) {
        const apiErr = err as ApiError;
        if (!cancelled)
          setError(apiErr?.message ?? "워크스페이스를 불러오지 못했습니다.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [hydrated]);

  async function handleLogout() {
    await logout();
    router.replace("/");
  }

  if (!hydrated) return null;

  const activeCount = workspaces.reduce(
    (acc, w) => acc + (w.runningTaskCount ?? 0),
    0,
  );
  const agentCount = workspaces.reduce(
    (acc, w) => acc + (w.agentCount ?? 0),
    0,
  );
  const completedCount = workspaces.reduce(
    (acc, w) => acc + (w.completedTaskCount ?? 0),
    0,
  );

  return (
    <T4Screen title="WORKSPACES" className="workspace-list-screen">
      <div
        className="workspace-list-layout"
        style={{
          padding: "24px 28px",
          height: "100%",
          maxWidth: 1200,
          width: "100%",
          margin: "0 auto",
          boxSizing: "border-box",
          display: "grid",
          gridTemplateRows: "auto auto auto",
          alignContent: "start",
          gap: 18,
          overflowY: "auto",
        }}
      >
        {/* Header */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: 16,
            flexWrap: "wrap",
          }}
        >
          <div>
            <div
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 12,
                fontWeight: 600,
                color: t4.mp,
                letterSpacing: "0.04em",
                marginBottom: 4,
              }}
            >
              <GlyphText glyph="◆">USER {user?.name ?? "MEMBER"}</GlyphText>
            </div>
            <h2
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 22,
                fontWeight: 600,
                letterSpacing: "-0.02em",
                margin: 0,
                color: t4.ink,
              }}
            >
              WORKSPACES
            </h2>
            <div
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 13,
                color: t4.dim,
                marginTop: 4,
              }}
            >
              {user?.email}
            </div>
          </div>
          <div className="workspace-list-header-actions" style={{ display: "flex", gap: 8 }}>
            <Link href="/workspaces/gateway">
              <ArcadeButton color={t4.mp}>
                <GlyphText glyph="◇">GATEWAY</GlyphText>
              </ArcadeButton>
            </Link>
            <Link href="/workspaces/new">
              <ArcadeButton color={t4.mp} primary>
                <GlyphText glyph="▶">NEW WORKSPACE</GlyphText>
              </ArcadeButton>
            </Link>
            <ArcadeButton color={t4.dim} onClick={handleLogout}>
              LOGOUT
            </ArcadeButton>
          </div>
        </div>

        {/* KPI tiles */}
        <div
          className="workspace-list-kpi-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(4, 1fr)",
            gap: 12,
          }}
        >
          {[
            {
              label: "WORKPLACES JOINED",
              value: String(workspaces.length).padStart(2, "0"),
              color: t4.ink,
              sub: "활성 워크스페이스",
            },
            {
              label: "TASKS · ACTIVE",
              value: String(activeCount).padStart(2, "0"),
              color: t4.mp,
              sub: "진행 중인 태스크",
            },
            {
              label: "AGENTS",
              value: String(agentCount).padStart(2, "0"),
              color: t4.agent,
              sub: "등록된 에이전트",
            },
            {
              label: "TASKS · DONE",
              value: String(completedCount).padStart(2, "0"),
              color: t4.ok,
              sub: "누적 완료",
            },
          ].map((tile) => (
            <T4Panel
              key={tile.label}
              accent={tile.color}
              style={{ padding: "14px 16px", position: "relative" }}
            >
              <div
                style={{
                  fontFamily: "var(--font-sans)",
                  fontSize: 12,
                  fontWeight: 600,
                  color: t4.dim,
                  letterSpacing: "0.04em",
                }}
              >
                {tile.label}
              </div>
              <div
                style={{
                  fontFamily: "var(--font-mono)",
                  fontSize: 24,
                  fontWeight: 600,
                  color: tile.color,
                  marginTop: 6,
                }}
              >
                {tile.value}
              </div>
              <div
                style={{
                  fontFamily: "var(--font-sans)",
                  fontSize: 12,
                  color: t4.mute,
                  marginTop: 4,
                }}
              >
                {tile.sub}
              </div>
            </T4Panel>
          ))}
        </div>

        {/* Workspace list */}
        <T4Panel
          label="ACTIVE WORKPLACES"
          accent={t4.mp}
          style={{
            position: "relative",
            padding: 16,
            display: "flex",
            flexDirection: "column",
            minHeight: 0,
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              padding: "0 4px 10px",
              borderBottom: `1px solid ${t4.line}`,
              marginBottom: 10,
              fontFamily: "var(--font-mono)",
              fontSize: 12,
              color: t4.dim,
              flexShrink: 0,
            }}
          >
            <span>
              <GlyphText glyph="◆">
                {String(workspaces.length).padStart(2, "0")} ENTRIES
              </GlyphText>
            </span>
            <span style={{ color: workspaces.length > 8 ? t4.mp : t4.mute }}>
              {workspaces.length > 8 ? "SCROLL ↓" : "—"}
            </span>
          </div>
          {loading ? (
            <ListSkeleton />
          ) : error ? (
            <div
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 13,
                color: t4.hp,
                padding: 20,
                textAlign: "center",
              }}
            >
              <GlyphText glyph="⚠">{error}</GlyphText>
            </div>
          ) : workspaces.length === 0 ? (
            <EmptyState />
          ) : (
            <div
              className="scrollbar-arcade"
              style={{
                display: "flex",
                flexDirection: "column",
                gap: 8,
                maxHeight: 460,
                overflowY: "auto",
              }}
            >
              {workspaces.map((ws, idx) => (
                <WorkspaceRow key={ws.workspaceId} workspace={ws} index={idx} />
              ))}
            </div>
          )}
        </T4Panel>
      </div>
    </T4Screen>
  );
}

function WorkspaceRow({
  workspace,
  index,
}: {
  workspace: WorkspaceSummary;
  index: number;
}) {
  const isAdmin = workspace.myRole === "ADMIN";
  const accent = isAdmin ? t4.mp : t4.dim;
  return (
    <Link
      className="workspace-list-row-card"
      href={`/workspaces/${workspace.workspaceId}`}
      style={{
        display: "grid",
        gridTemplateColumns: "64px 1fr 130px 130px 88px",
        gap: 14,
        alignItems: "center",
        padding: "14px 16px",
        borderRadius: 10,
        border: `1px solid ${t4.line}`,
        background: "#141519",
        textDecoration: "none",
        color: "inherit",
        transition: "background 120ms ease, border-color 120ms ease",
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = "#1d1f25";
        e.currentTarget.style.borderColor = "rgba(50, 121, 249, 0.36)";
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = "#141519";
        e.currentTarget.style.borderColor = t4.line;
      }}
    >
      <div
        style={{
          fontFamily: "var(--font-mono)",
          fontSize: 12,
          fontWeight: 500,
          color: t4.mute,
        }}
      >
        W{String(index + 1).padStart(3, "0")}
      </div>
      <div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            flexWrap: "wrap",
          }}
        >
          <span
            style={{
              fontFamily: "var(--font-sans)",
              fontSize: 14,
              fontWeight: 600,
              lineHeight: 1.3,
              color: t4.ink,
            }}
          >
            {workspace.name}
          </span>
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              padding: "2px 8px",
              borderRadius: 9999,
              fontFamily: "var(--font-sans)",
              fontSize: 12,
              fontWeight: 500,
              background: isAdmin
                ? "rgba(50, 121, 249, 0.14)"
                : "rgba(230, 234, 240, 0.08)",
              color: isAdmin ? "#79a6fc" : t4.dim,
            }}
          >
            {workspace.myRole}
          </span>
        </div>
        <div
          style={{
            fontFamily: "var(--font-sans)",
            fontSize: 13,
            color: t4.dim,
            marginTop: 4,
          }}
        >
          {workspace.description || "No description"}
        </div>
      </div>
      <div
        className="workspace-list-row-enter"
        style={{
          fontFamily: "var(--font-mono)",
          fontSize: 12,
          color: t4.dim,
        }}
      >
        <GlyphText glyph="◇">{workspace.agentCount} agents</GlyphText>
      </div>
      <div
        style={{
          fontFamily: "var(--font-mono)",
          fontSize: 12,
          color: (workspace.runningTaskCount ?? 0) > 0 ? t4.ok : t4.mute,
        }}
      >
        <GlyphText glyph="▷">{workspace.runningTaskCount ?? 0} active</GlyphText>
      </div>
      <div
        style={{
          fontFamily: "var(--font-sans)",
          fontSize: 12,
          fontWeight: 500,
          color: t4.ink,
          textAlign: "center",
          padding: "6px 10px",
          borderRadius: 8,
          background: "#212226",
          border: `1px solid ${accent === t4.mp ? "rgba(50, 121, 249, 0.35)" : t4.line}`,
        }}
      >
        <GlyphText glyph="▶">ENTER</GlyphText>
      </div>
    </Link>
  );
}

function ArcadeButton({
  children,
  onClick,
  primary = false,
}: {
  children: React.ReactNode;
  color?: string;
  onClick?: () => void;
  primary?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        fontFamily: "var(--font-sans)",
        fontSize: 12,
        fontWeight: 500,
        letterSpacing: "0.02em",
        padding: "8px 14px",
        borderRadius: 8,
        background: primary ? "#3279f9" : "#212226",
        color: primary ? "#ffffff" : t4.ink,
        border: primary
          ? "1px solid #3279f9"
          : "1px solid rgba(230, 234, 240, 0.12)",
        cursor: "pointer",
      }}
    >
      {children}
    </button>
  );
}

function EmptyState() {
  return (
    <div
      style={{
        padding: "36px 20px",
        textAlign: "center",
        fontFamily: "var(--font-sans)",
        color: t4.dim,
        fontSize: 13,
      }}
    >
      <div style={{ marginBottom: 12 }}>
        <PixelAvatar kind="agent" size={3} style={{ margin: "0 auto" }} />
      </div>
      <div style={{ marginBottom: 16 }}>
        아직 참여 중인 워크스페이스가 없습니다. 새 워크스페이스를 생성해 협업을 시작하세요.
      </div>
      <Link
        href="/workspaces/new"
        style={{
          display: "inline-flex",
          fontFamily: "var(--font-sans)",
          fontSize: 12,
          fontWeight: 500,
          color: "#ffffff",
          background: "#3279f9",
          padding: "9px 16px",
          borderRadius: 8,
          textDecoration: "none",
        }}
      >
        <GlyphText glyph="▶">CREATE WORKSPACE</GlyphText>
      </Link>
    </div>
  );
}

function ListSkeleton() {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      {Array.from({ length: 3 }).map((_, i) => (
        <div
          key={i}
          style={{
            height: 60,
            borderRadius: 10,
            border: `1px solid ${t4.line}`,
            background: "#141519",
            opacity: 0.5,
          }}
        />
      ))}
    </div>
  );
}
