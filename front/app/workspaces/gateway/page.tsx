"use client";

import { AlertCircle, ArrowLeft, Loader2, RefreshCw, ServerCog } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError } from "@/lib/api-client";
import { resolveAccessToken } from "@/lib/auth";
import { fetchWorkspaceGatewayStatus, type WorkspaceGatewayStatus } from "@/lib/gateway";
import { gatewayConnectionStatusLabel, gatewayStatusLabel } from "@/lib/gateway-utils";
import { fetchWorkspaces, type WorkspaceSummary } from "@/lib/workspaces";

type GatewayStatusEntry = {
  workspace: WorkspaceSummary;
  gateway: WorkspaceGatewayStatus | null;
  error: string | null;
};

type GatewayRouteState =
  | { status: "loading"; entries: GatewayStatusEntry[]; error: null }
  | { status: "ready"; entries: GatewayStatusEntry[]; error: null }
  | { status: "error"; entries: GatewayStatusEntry[]; error: string };

export default function GatewayRoutePage() {
  const [state, setState] = useState<GatewayRouteState>({
    status: "loading",
    entries: [],
    error: null,
  });
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<number | null>(null);

  const loadGatewayStatus = useCallback(async () => {
    setState((current) => ({ status: "loading", entries: current.entries, error: null }));

    try {
      const accessToken = resolveAccessToken();
      const workspaces = await fetchWorkspaces(accessToken);
      const entries = await Promise.all(
        workspaces.map(async (workspace) => {
          try {
            return {
              workspace,
              gateway: await fetchWorkspaceGatewayStatus(accessToken, workspace.workspaceId),
              error: null,
            } satisfies GatewayStatusEntry;
          } catch (error) {
            return {
              workspace,
              gateway: null,
              error: messageFromError(error, "gateway 상태를 확인하지 못했습니다."),
            } satisfies GatewayStatusEntry;
          }
        }),
      );

      setState({ status: "ready", entries, error: null });
      setSelectedWorkspaceId((current) => current ?? entries[0]?.workspace.workspaceId ?? null);
    } catch (error) {
      setState({
        status: "error",
        entries: [],
        error: messageFromError(error, "워크스페이스 gateway 목록을 불러오지 못했습니다."),
      });
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      void loadGatewayStatus();
    });
  }, [loadGatewayStatus]);

  const selectedEntry = useMemo(
    () =>
      state.entries.find((entry) => entry.workspace.workspaceId === selectedWorkspaceId) ??
      state.entries[0] ??
      null,
    [selectedWorkspaceId, state.entries],
  );

  return (
    <main className="workspace-create-shell">
      <section className="workspace-create-panel" aria-label="Gateway 관리">
        <div className="workspace-create-header">
          <div className="workspace-create-icon">
            <ServerCog aria-hidden="true" />
          </div>
          <div className="workspace-create-title">
            <p className="eyebrow">Gateway</p>
            <h1>Gateway 관리</h1>
            <p>워크스페이스별 gateway binding 상태를 확인합니다.</p>
          </div>
        </div>

        <div className="workspace-gateway-actions">
          <Link className="secondary-action" href="/workspaces">
            <ArrowLeft aria-hidden="true" />
            <span>워크스페이스</span>
          </Link>
          <button
            className="secondary-action"
            type="button"
            onClick={() => void loadGatewayStatus()}
            disabled={state.status === "loading"}
          >
            {state.status === "loading" ? (
              <Loader2 className="spin" aria-hidden="true" />
            ) : (
              <RefreshCw aria-hidden="true" />
            )}
            <span>새로고침</span>
          </button>
        </div>

        {state.status === "loading" ? (
          <div className="workspace-task-notice">
            <Loader2 className="spin" aria-hidden="true" />
            <span>gateway 상태를 불러오고 있습니다.</span>
          </div>
        ) : null}
        {state.status === "error" ? (
          <div className="workspace-task-notice" role="alert">
            <AlertCircle aria-hidden="true" />
            <span>{state.error}</span>
          </div>
        ) : null}

        <div className="workspace-list" aria-label="Gateway workspace 목록">
          {state.entries.map((entry) => {
            const selected = entry.workspace.workspaceId === selectedEntry?.workspace.workspaceId;
            return (
              <button
                className="workspace-row workspace-row-link"
                type="button"
                key={entry.workspace.workspaceId}
                onClick={() => setSelectedWorkspaceId(entry.workspace.workspaceId)}
                aria-pressed={selected}
              >
                <div className="workspace-row-icon">
                  <ServerCog aria-hidden="true" />
                </div>
                <div className="workspace-row-main">
                  <strong>{entry.workspace.name}</strong>
                  <p>{entry.error ?? entry.gateway?.gatewayUrl ?? "등록된 gateway URL이 없습니다"}</p>
                </div>
                <div className="workspace-row-meta">
                  <span className="role-badge">
                    {entry.gateway ? gatewayStatusLabel(entry.gateway.status) : "확인 실패"}
                  </span>
                  {entry.gateway?.lastStatus ? (
                    <span>{gatewayConnectionStatusLabel(entry.gateway.lastStatus)}</span>
                  ) : null}
                </div>
              </button>
            );
          })}
        </div>

        {state.status === "ready" && state.entries.length === 0 ? (
          <div className="workspace-empty">
            <strong>워크스페이스가 없습니다</strong>
            <p>워크스페이스를 만든 뒤 gateway를 연결할 수 있습니다.</p>
          </div>
        ) : null}

        {selectedEntry ? (
          <section className="workspace-section" aria-label="선택된 Gateway 상태">
            <div className="workspace-section-heading">
              <div>
                <p className="eyebrow">Selected</p>
                <h2>{selectedEntry.workspace.name}</h2>
              </div>
              <Link
                className="secondary-action"
                href={`/workspaces/${selectedEntry.workspace.workspaceId}/settings`}
              >
                설정 열기
              </Link>
            </div>
            <div className="workspace-summary-grid">
              <StatusTile
                label="Binding"
                value={
                  selectedEntry.gateway
                    ? gatewayStatusLabel(selectedEntry.gateway.status)
                    : "확인 실패"
                }
              />
              <StatusTile
                label="Mode"
                value={selectedEntry.gateway?.mode ?? "UNBOUND"}
              />
              <StatusTile
                label="Last status"
                value={
                  selectedEntry.gateway?.lastStatus
                    ? gatewayConnectionStatusLabel(selectedEntry.gateway.lastStatus)
                    : "확인 전"
                }
              />
            </div>
          </section>
        ) : null}
      </section>
    </main>
  );
}

function StatusTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="workspace-stat-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function messageFromError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}
