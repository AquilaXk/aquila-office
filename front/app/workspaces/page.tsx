"use client";

import {
  AlertCircle,
  Building2,
  FolderOpen,
  LogOut,
  Plus,
  RefreshCw,
  Server,
  UserRound,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
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
import { BRAND_MASCOT_PATH, SERVICE_NAME } from "@/lib/branding";
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
  const [tokens, setTokens] = useState<AuthTokens | null>(() => readAuthTokens());
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
    setWorkspaces({ status: "loading", data: null, error: null });
    const accessToken = resolveAccessToken();

    try {
      const data = await fetchWorkspaces(accessToken);
      setWorkspaces({ status: "ready", data, error: null });
    } catch (error) {
      setWorkspaces({
        status: "offline",
        data: null,
        error: messageFromError(error, "워크스페이스 목록을 확인하지 못했습니다."),
      });
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      void loadWorkspaces();
    });

    const onStorage = (event: StorageEvent) => {
      if (event.key === AUTH_STORAGE_KEY) {
        setTokens(readAuthTokens());
        void loadWorkspaces();
      }
    };

    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
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
      await loadWorkspaces();
    } catch (error) {
      clearAuthTokens();
      setTokens(null);
      setLogoutState({
        status: "error",
        message: messageFromError(error, "로그아웃 요청을 처리하지 못했습니다."),
      });
      await loadWorkspaces();
    }
  };

  const workspaceCount = workspaces.status === "ready" ? workspaces.data.length : 0;

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-crest">
            <Image
              src={BRAND_MASCOT_PATH}
              alt={`${SERVICE_NAME} crest`}
              width={72}
              height={72}
              priority
            />
          </div>
          <div className="brand-copy">
            <span>{SERVICE_NAME}</span>
            <strong>Workspace list</strong>
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
            <p className="eyebrow">Workspace</p>
            <h1>내 워크스페이스</h1>
            <p className="topbar-summary">
              로그인한 사용자와 참여 중인 워크스페이스를 확인합니다.
            </p>
          </div>
          <button
            className="icon-button"
            type="button"
            onClick={() => void loadWorkspaces()}
            disabled={workspaces.status === "loading"}
            title="목록 새로고침"
            aria-label="목록 새로고침"
          >
            <RefreshCw
              aria-hidden="true"
              className={workspaces.status === "loading" ? "spin" : undefined}
            />
          </button>
        </header>

        <section className="status-grid" aria-label="사용자와 작업 상태">
          <article className="status-panel status-panel-primary">
            <div className="panel-header">
              <UserRound aria-hidden="true" />
              <span>User</span>
            </div>
            <strong>{tokens?.name ?? "브라우저 세션"}</strong>
            <p>{tokens?.email ?? "저장된 인증 세션이 없습니다."}</p>
          </article>
          <article className="status-panel">
            <div className="panel-header">
              <Building2 aria-hidden="true" />
              <span>Workspaces</span>
            </div>
            <strong>
              {workspaces.status === "loading" ? "확인 중" : `${workspaceCount}개`}
            </strong>
            <p>{workspaces.status === "offline" ? workspaces.error : "가입된 워크스페이스"}</p>
          </article>
          <article className="status-panel">
            <div className="panel-header">
              <Server aria-hidden="true" />
              <span>Session</span>
            </div>
            <strong>{tokens ? "token ready" : "token empty"}</strong>
            <p>{logoutState.message ?? "API 요청에는 저장된 access token을 사용합니다."}</p>
          </article>
        </section>

        <section className="workspace-section" aria-label="워크스페이스 작업">
          <div className="section-header">
            <div>
              <p className="eyebrow">Actions</p>
              <h2>빠른 작업</h2>
            </div>
            <div className="form-actions">
              <Link className="secondary-action" href="/workspaces/gateway">
                <Server aria-hidden="true" />
                <span>Gateway</span>
              </Link>
              <Link className="primary-action primary-action-active" href="/workspaces/new">
                <Plus aria-hidden="true" />
                <span>새 Workspace</span>
              </Link>
              <button
                className="secondary-action"
                type="button"
                onClick={() => void handleLogout()}
                disabled={logoutState.status === "submitting"}
              >
                <LogOut aria-hidden="true" />
                <span>{logoutState.status === "submitting" ? "Logout..." : "Logout"}</span>
              </button>
            </div>
          </div>

          {logoutState.status === "error" ? (
            <div className="form-notice form-notice-error" role="alert">
              <AlertCircle aria-hidden="true" />
              <span>{logoutState.message}</span>
            </div>
          ) : null}
        </section>

        <section className="workspace-section" aria-label="워크스페이스 목록">
          <div className="section-header">
            <div>
              <p className="eyebrow">List</p>
              <h2>참여 중인 워크스페이스</h2>
            </div>
            <span className="role-badge">
              {workspaces.status === "ready" ? `${workspaces.data.length} entries` : workspaces.status}
            </span>
          </div>

          {workspaces.status === "ready" && workspaces.data.length > 0 ? (
            <div className="workspace-list">
              {workspaces.data.map((workspace) => (
                <Link
                  className="workspace-row workspace-row-link"
                  href={`/workspaces/${workspace.workspaceId}`}
                  key={workspace.workspaceId}
                  aria-label={`${workspace.name} 상세 보기`}
                >
                  <div className="workspace-row-icon">
                    <Building2 aria-hidden="true" />
                  </div>
                  <div className="workspace-row-main">
                    <strong>{workspace.name}</strong>
                    <p>{workspace.description ?? "설명 없음"}</p>
                  </div>
                  <div className="workspace-row-meta">
                    <span className="role-badge">{workspace.myRole}</span>
                    <span>{formatDate(workspace.joinedAt)}</span>
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
      <strong>가입된 워크스페이스가 없습니다</strong>
      <p>새 워크스페이스를 만들면 이곳에서 바로 확인할 수 있습니다.</p>
    </div>
  );
}

function messageFromError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
  }).format(new Date(value));
}
