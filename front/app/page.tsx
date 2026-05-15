"use client";

import { RefreshCw, Server, ShieldCheck, Wifi, WifiOff } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { API_BASE_URL, ApiError, apiFetch } from "@/lib/api-client";

type HealthResponse = {
  status: string;
  service: string;
  checkedAt: string;
};

type HealthState =
  | { status: "idle" | "loading"; data: null; error: null }
  | { status: "online"; data: HealthResponse; error: null }
  | { status: "offline"; data: null; error: string };

export default function Home() {
  const [health, setHealth] = useState<HealthState>({
    status: "idle",
    data: null,
    error: null,
  });

  const loadHealth = useCallback(async () => {
    setHealth({ status: "loading", data: null, error: null });

    const result = await fetchHealth();
    setHealth(result);
  }, []);

  useEffect(() => {
    let ignore = false;

    fetchHealth().then((result) => {
      if (!ignore) {
        setHealth(result);
      }
    });

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

  return (
    <main className="app-shell">
      <aside className="sidebar" aria-label="Aquila Office">
        <div className="brand-mark">AO</div>
        <nav className="nav-list">
          <span className="nav-item nav-item-active">System</span>
          <span className="nav-item">Workspace</span>
          <span className="nav-item">Members</span>
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Aquila Office</p>
            <h1>운영 콘솔</h1>
          </div>
          <button
            className="icon-button"
            type="button"
            onClick={() => void loadHealth()}
            disabled={isLoading}
            title="API 상태 새로고침"
            aria-label="API 상태 새로고침"
          >
            <RefreshCw aria-hidden="true" className={isLoading ? "spin" : undefined} />
          </button>
        </header>

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
              <ShieldCheck aria-hidden="true" />
              <span>Security</span>
            </div>
            <strong>CORS</strong>
            <p>localhost:3000, localhost:5173</p>
          </article>
        </section>
      </section>
    </main>
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
