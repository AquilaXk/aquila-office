"use client";

import {
  AlertCircle,
  Building2,
  FolderOpen,
  Plus,
  RefreshCw,
  Server,
  UserRound,
  Wifi,
  WifiOff,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { API_BASE_URL, ApiError, apiFetch } from "@/lib/api-client";
import {
  BRAND_MASCOT_PATH,
  REPRESENTATIVE_IMAGE_PATHS,
  SERVICE_NAME,
} from "@/lib/branding";
import { fetchWorkspaces, type WorkspaceSummary } from "@/lib/workspaces";

const DEV_ACCESS_TOKEN =
  process.env.NEXT_PUBLIC_DEV_ACCESS_TOKEN ?? "";

type HealthResponse = {
  status: string;
  service: string;
  checkedAt: string;
};

type HealthState =
  | { status: "idle" | "loading"; data: null; error: null }
  | { status: "online"; data: HealthResponse; error: null }
  | { status: "offline"; data: null; error: string };

type WorkspaceState =
  | { status: "idle" | "loading"; data: null; error: null }
  | { status: "ready"; data: WorkspaceSummary[]; error: null }
  | { status: "offline"; data: null; error: string };

const assetHighlights = [
  {
    src: REPRESENTATIVE_IMAGE_PATHS[0],
    name: "office-bank-preview.png",
    title: "Treasury desk",
    caption: "고밀도 업무 패널",
  },
  {
    src: REPRESENTATIVE_IMAGE_PATHS[1],
    name: "office-blog-preview.png",
    title: "Archive desk",
    caption: "운영 피드 대시보드",
  },
] as const;

const brandMascotAsset = "brand-mascot.png";

export default function Home() {
  const [health, setHealth] = useState<HealthState>({
    status: "idle",
    data: null,
    error: null,
  });
  const [workspaces, setWorkspaces] = useState<WorkspaceState>({
    status: "idle",
    data: null,
    error: null,
  });

  const refreshDashboard = useCallback(async () => {
    setHealth({ status: "loading", data: null, error: null });
    setWorkspaces({ status: "loading", data: null, error: null });

    const [healthResult, workspaceResult] = await Promise.all([
      fetchHealth(),
      fetchWorkspaceList(),
    ]);
    setHealth(healthResult);
    setWorkspaces(workspaceResult);
  }, []);

  useEffect(() => {
    let ignore = false;

    Promise.all([fetchHealth(), fetchWorkspaceList()]).then(
      ([healthResult, workspaceResult]) => {
        if (!ignore) {
          setHealth(healthResult);
          setWorkspaces(workspaceResult);
        }
      },
    );

    return () => {
      ignore = true;
    };
  }, []);

  const checkedAt = health.data?.checkedAt
    ? new Intl.DateTimeFormat("ko-KR", {
        dateStyle: "medium",
        timeStyle: "medium",
      }).format(new Date(health.data.checkedAt))
    : "-";

  const isLoading = health.status === "loading";
  const isOnline = health.status === "online";
  const isWorkspaceLoading = workspaces.status === "loading";
  const workspaceCount = workspaces.data?.length ?? 0;

  return (
    <main className="app-shell">
      <aside className="sidebar" aria-label={SERVICE_NAME}>
        <div className="brand-block">
          <div className="brand-crest">
            <Image
              src={BRAND_MASCOT_PATH}
              alt={`${SERVICE_NAME} crest`}
              width={72}
              height={72}
              priority
              data-asset={brandMascotAsset}
            />
          </div>
          <div className="brand-copy">
            <span>{SERVICE_NAME}</span>
            <strong>Command office</strong>
          </div>
        </div>
        <nav className="nav-list">
          <span className="nav-item">System</span>
          <span className="nav-item nav-item-active">Workspace</span>
          <span className="nav-item">Members</span>
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">{SERVICE_NAME}</p>
            <h1>워크스페이스 작전 현황</h1>
            <p className="topbar-summary">
              API 상태와 워크스페이스 접근 흐름을 한 화면에서 점검합니다.
            </p>
          </div>
          <button
            className="icon-button"
            type="button"
            onClick={() => void refreshDashboard()}
            disabled={isLoading || isWorkspaceLoading}
            title="상태 새로고침"
            aria-label="상태 새로고침"
          >
            <RefreshCw
              aria-hidden="true"
              className={isLoading || isWorkspaceLoading ? "spin" : undefined}
            />
          </button>
        </header>

        <section className="command-band" aria-label="대표 운영 화면">
          <div className="command-band-copy">
            <span>Operational archive</span>
            <strong>gothic command surface</strong>
            <p>브랜드 자산과 운영 패널을 어두운 금속 톤의 업무 화면으로 묶었습니다.</p>
          </div>
          <div className="asset-strip">
            {assetHighlights.map((asset) => (
              <figure className="asset-card" data-asset={asset.name} key={asset.name}>
                <Image
                  src={asset.src}
                  alt={asset.title}
                  width={360}
                  height={220}
                  sizes="(max-width: 860px) 100vw, 360px"
                />
                <figcaption>
                  <span>{asset.title}</span>
                  <strong>{asset.caption}</strong>
                </figcaption>
              </figure>
            ))}
          </div>
        </section>

        <section className="status-grid" aria-label="시스템 상태">
          <article className="status-panel status-panel-primary">
            <div className="panel-header">
              <Server aria-hidden="true" />
              <span>Backend API</span>
            </div>
            <strong>{health.data?.service ?? "aquila-office-back"}</strong>
            <p>{API_BASE_URL}</p>
          </article>

          <article className={isOnline ? "status-panel online" : "status-panel offline"}>
            <div className="panel-header">
              {isOnline ? <Wifi aria-hidden="true" /> : <WifiOff aria-hidden="true" />}
              <span>연결 상태</span>
            </div>
            <strong>{isLoading ? "확인 중" : isOnline ? "온라인" : "오프라인"}</strong>
            <p>{health.error ?? checkedAt}</p>
          </article>

          <article className="status-panel">
            <div className="panel-header">
              <Building2 aria-hidden="true" />
              <span>Workspaces</span>
            </div>
            <strong>{isWorkspaceLoading ? "확인 중" : `${workspaceCount}개`}</strong>
            <p>{workspaces.error ?? "가입된 워크스페이스"}</p>
          </article>
        </section>

        <section className="workspace-section" aria-label="워크스페이스 목록">
          <div className="section-header">
            <div>
              <p className="eyebrow">Workspace</p>
              <h2>내 워크스페이스</h2>
            </div>
            <Link className="primary-action primary-action-active" href="/workspaces/new">
              <Plus aria-hidden="true" />
              <span>생성</span>
            </Link>
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

async function fetchHealth(): Promise<HealthState> {
  try {
    const data = await apiFetch<HealthResponse>("/api/health");
    return { status: "online", data, error: null };
  } catch (error) {
    const message =
      error instanceof ApiError ? error.message : "API 연결을 확인하지 못했습니다.";
    return { status: "offline", data: null, error: message };
  }
}

async function fetchWorkspaceList(): Promise<WorkspaceState> {
  try {
    const data = await fetchWorkspaces(DEV_ACCESS_TOKEN);
    return { status: "ready", data, error: null };
  } catch (error) {
    const message =
      error instanceof ApiError ? error.message : "워크스페이스 목록을 확인하지 못했습니다.";
    return { status: "offline", data: null, error: message };
  }
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
  }).format(new Date(value));
}
