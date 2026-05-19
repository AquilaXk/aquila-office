"use client";

import {
  AlertCircle,
  Building2,
  FolderOpen,
  LogOut,
  Plus,
  Server,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import {
  AUTH_STORAGE_KEY,
  clearAuthTokens,
  logoutAuth,
  readAuthTokens,
  resolveAccessToken,
  type AuthTokens,
} from "@/lib/auth";
import { fetchWorkspaces, type WorkspaceSummary } from "@/lib/workspaces";

type WorkspaceState =
  | { status: "idle" | "loading"; data: null; error: null }
  | { status: "ready"; data: WorkspaceSummary[]; error: null }
  | { status: "offline"; data: null; error: string };

type LogoutState =
  | { status: "idle"; message: string | null }
  | { status: "submitting"; message: string | null }
  | { status: "error"; message: string };

export default function WorkspacesPage() {
  const router = useRouter();
  const [tokens, setTokens] = useState<AuthTokens | null>(() => readAuthTokens());
  const [hydrated, setHydrated] = useState(false);
  const [workspaces, setWorkspaces] = useState<WorkspaceState>({
    status: "idle",
    data: null,
    error: null,
  });
  const [logoutState, setLogoutState] = useState<LogoutState>({
    status: "idle",
    message: null,
  });

  const loadWorkspaces = useCallback(async () => {
    const currentTokens = readAuthTokens();
    if (!currentTokens) {
      setTokens(null);
      setHydrated(false);
      setWorkspaces({ status: "idle", data: null, error: null });
      router.replace("/");
      return;
    }

    setTokens(currentTokens);
    setHydrated(true);
    setWorkspaces({ status: "loading", data: null, error: null });
    const accessToken = resolveAccessToken();

    try {
      const data = await fetchWorkspaces(accessToken);
      setWorkspaces({ status: "ready", data, error: null });
    } catch (error) {
      setWorkspaces({
        status: "offline",
        data: null,
        error: messageFromError(error, "워크스페이스를 불러오지 못했습니다."),
      });
    }
  }, [router]);

  useEffect(() => {
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) {
        return;
      }
      void loadWorkspaces();
    });

    const onStorage = (event: StorageEvent) => {
      if (event.key === AUTH_STORAGE_KEY) {
        void loadWorkspaces();
      }
    };

    window.addEventListener("storage", onStorage);
    return () => {
      cancelled = true;
      window.removeEventListener("storage", onStorage);
    };
  }, [loadWorkspaces]);

  const handleLogout = async () => {
    const currentTokens = readAuthTokens();
    setLogoutState({ status: "submitting", message: null });

    try {
      if (currentTokens) {
        await logoutAuth(currentTokens.refreshToken);
      }
      clearAuthTokens();
      setTokens(null);
      setLogoutState({ status: "idle", message: "인증 세션이 삭제되었습니다." });
    } catch (error) {
      clearAuthTokens();
      setTokens(null);
      setLogoutState({
        status: "error",
        message: messageFromError(error, "로그아웃 요청을 처리하지 못했습니다."),
      });
    } finally {
      setHydrated(false);
      router.replace("/");
    }
  };

  const workspaceList = workspaces.status === "ready" ? workspaces.data : [];
  const workspaceCount = workspaceList.length;
  const activeTaskCount = workspaceList.reduce(
    (count, workspace) => count + (workspace.runningTaskCount ?? 0),
    0,
  );
  const agentCount = workspaceList.reduce(
    (count, workspace) => count + (workspace.agentCount ?? 0),
    0,
  );
  const completedTaskCount = workspaceList.reduce(
    (count, workspace) => count + (workspace.completedTaskCount ?? 0),
    0,
  );

  if (!hydrated) {
    return null;
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-crest" aria-hidden="true">AO</div>
          <div className="brand-copy">
            <span>AQUILA OFFICE</span>
            <strong>Select quest</strong>
          </div>
        </div>
        <nav className="nav-list" aria-label="워크스페이스 메뉴">
          <Link className="nav-item nav-item-active" href="/workspaces">
            Workspace
          </Link>
          <Link className="nav-item" href="/workspaces/gateway">
            Gateway
          </Link>
          <Link className="nav-item" href="/auth">
            Auth
          </Link>
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">HERO {tokens?.name ?? "PLAYER"}</p>
            <h1>WORKSPACES</h1>
            <p className="topbar-summary">{tokens?.email}</p>
          </div>
          <div className="form-actions">
            <Link className="secondary-action" href="/workspaces/gateway">
              <Server aria-hidden="true" />
              <span>GATEWAY</span>
            </Link>
            <Link
              aria-label="새 Workspace"
              className="primary-action primary-action-active"
              href="/workspaces/new"
            >
              <Plus aria-hidden="true" />
              <span>NEW WORKSPACE</span>
            </Link>
            <button
              className="secondary-action"
              type="button"
              onClick={() => void handleLogout()}
              disabled={logoutState.status === "submitting"}
            >
              <LogOut aria-hidden="true" />
              <span>{logoutState.status === "submitting" ? "LOGOUT..." : "LOGOUT"}</span>
            </button>
          </div>
        </header>

        <section className="status-grid workspace-metric-grid" aria-label="워크스페이스 지표">
          <article className="status-panel status-panel-primary">
            <div className="panel-header">
              <Building2 aria-hidden="true" />
              <span>WORKPLACES JOINED</span>
            </div>
            <strong>{String(workspaceCount).padStart(2, "0")}</strong>
            <p>활성 워크스페이스</p>
          </article>
          <article className="status-panel">
            <div className="panel-header">
              <FolderOpen aria-hidden="true" />
              <span>TASKS · ACTIVE</span>
            </div>
            <strong>{String(activeTaskCount).padStart(2, "0")}</strong>
            <p>진행 중인 태스크</p>
          </article>
          <article className="status-panel">
            <div className="panel-header">
              <UserRound aria-hidden="true" />
              <span>AGENT ALLIES</span>
            </div>
            <strong>{String(agentCount).padStart(2, "0")}</strong>
            <p>소환된 에이전트</p>
          </article>
          <article className="status-panel">
            <div className="panel-header">
              <Server aria-hidden="true" />
              <span>TASKS · DONE</span>
            </div>
            <strong>{String(completedTaskCount).padStart(2, "0")}</strong>
            <p>누적 완료</p>
          </article>
        </section>

        <section className="workspace-section" aria-label="워크스페이스 목록">
          <div className="section-header">
            <div>
              <p className="eyebrow">ACTIVE WORKPLACES</p>
              <h2>참여 중인 워크스페이스</h2>
            </div>
            <div className="workspace-list-meta">
              <span className="role-badge">
                {workspaces.status === "ready"
                  ? `${String(workspaces.data.length).padStart(2, "0")} ENTRIES`
                  : workspaces.status}
              </span>
              <span
                className={
                  workspaces.status === "ready" && workspaces.data.length > 8
                    ? "workspace-scroll-indicator workspace-scroll-indicator-active"
                    : "workspace-scroll-indicator"
                }
              >
                {workspaces.status === "ready" && workspaces.data.length > 8 ? "SCROLL ↓" : "—"}
              </span>
            </div>
          </div>

          {workspaces.status === "ready" && workspaces.data.length > 0 ? (
            <div className="workspace-list">
              {workspaces.data.map((workspace, index) => (
                <Link
                  className="workspace-row workspace-row-link"
                  href={`/workspaces/${workspace.workspaceId}`}
                  key={workspace.workspaceId}
                  aria-label={`${workspace.name} ENTER`}
                >
                  <div className="workspace-row-icon">
                    <span>{`W${String(index + 1).padStart(3, "0")}`}</span>
                  </div>
                  <div className="workspace-row-main">
                    <strong>{workspace.name}</strong>
                    <p>{workspace.description || "no description"}</p>
                    <span className="role-badge">[{workspace.myRole}]</span>
                  </div>
                  <div className="workspace-row-meta">
                    <span>{workspace.agentCount ?? 0} agents</span>
                    <span>{workspace.runningTaskCount ?? 0} active</span>
                  </div>
                  <div className="workspace-row-enter">
                    <span>ENTER</span>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <WorkspaceStatePanel state={workspaces} />
          )}
        </section>
      </section>
    </main>
  );
}

function WorkspaceStatePanel({ state }: { state: WorkspaceState }) {
  if (state.status === "loading" || state.status === "idle") {
    return (
      <div className="state-panel">
        <FolderOpen aria-hidden="true" />
        <strong>목록 확인 중</strong>
        <p>워크스페이스 정보를 불러오고 있습니다.</p>
      </div>
    );
  }

  if (state.status === "offline") {
    return (
      <div className="state-panel state-panel-error">
        <AlertCircle aria-hidden="true" />
        <strong>목록을 불러오지 못했습니다</strong>
        <p>{state.error}</p>
      </div>
    );
  }

  return (
    <div className="state-panel">
      <UserRound aria-hidden="true" />
      <strong>no workspaces yet — start a new save to begin your work RPG</strong>
      <p>새 워크스페이스를 만들면 이곳에서 바로 확인할 수 있습니다.</p>
      <Link className="primary-action primary-action-active" href="/workspaces/new">
        <Plus aria-hidden="true" />
        <span>CREATE WORKSPACE</span>
      </Link>
    </div>
  );
}

function messageFromError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}
