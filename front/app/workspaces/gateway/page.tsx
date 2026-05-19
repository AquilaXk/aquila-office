"use client";

import { AlertCircle, ArrowLeft, CheckCircle2, KeyRound, Loader2, RefreshCw, ServerCog } from "lucide-react";
import Link from "next/link";
import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { ApiError } from "@/lib/api";
import { resolveAccessToken } from "@/lib/auth";
import {
  createWorkspaceGatewayBinding,
  fetchWorkspaceGatewayStatus,
  testWorkspaceGatewayConnection,
  type WorkspaceGatewayConnectionTest,
  type WorkspaceGatewayStatus,
} from "@/lib/gateway";
import { gatewayConnectionStatusLabel, gatewayStatusLabel, normalizeGatewayForm } from "@/lib/gateway-utils";
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

type GatewayMutationState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
  result: WorkspaceGatewayConnectionTest | null;
};

type GatewayDisplayNames = Record<string, string>;

const GATEWAY_DISPLAY_NAMES_STORAGE_KEY = "aquila-office.gateway-display-names";
const DISPLAY_NAME_MAX_LENGTH = 80;

const idleGatewayMutationState: GatewayMutationState = {
  status: "idle",
  message: null,
  result: null,
};

export default function GatewayRoutePage() {
  const [state, setState] = useState<GatewayRouteState>({
    status: "loading",
    entries: [],
    error: null,
  });
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<number | null>(null);
  const [gatewayDisplayNameDraft, setGatewayDisplayNameDraft] = useState<{
    workspaceId: number | null;
    value: string;
  }>({ workspaceId: null, value: "" });
  const [gatewayDisplayNames, setGatewayDisplayNames] = useState<GatewayDisplayNames>(() =>
    readGatewayDisplayNames(),
  );
  const [gatewayUrlInput, setGatewayUrlInput] = useState("");
  const [gatewayTokenInput, setGatewayTokenInput] = useState("");
  const [gatewayTestState, setGatewayTestState] =
    useState<GatewayMutationState>(idleGatewayMutationState);
  const [gatewayBindingState, setGatewayBindingState] =
    useState<GatewayMutationState>(idleGatewayMutationState);

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
      setGatewayUrlInput((current) => current || entries[0]?.gateway?.gatewayUrl || "");
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
  const selectedWorkspaceKey = selectedEntry ? String(selectedEntry.workspace.workspaceId) : null;
  const selectedGatewayDisplayName = selectedEntry
    ? (selectedWorkspaceKey ? gatewayDisplayNames[selectedWorkspaceKey] : null) ??
      selectedEntry.workspace.name
    : "";
  const gatewayDisplayNameInput =
    selectedEntry && gatewayDisplayNameDraft.workspaceId === selectedEntry.workspace.workspaceId
      ? gatewayDisplayNameDraft.value
      : selectedGatewayDisplayName;
  const displayNameError =
    gatewayDisplayNameInput.trim().length === 0 ? "표시 이름을 입력해 주세요." : null;
  const normalizedGatewayForm = useMemo(
    () =>
      normalizeGatewayForm({
        gatewayUrl: gatewayUrlInput,
        token: gatewayTokenInput,
        validateConnection: true,
      }),
    [gatewayTokenInput, gatewayUrlInput],
  );
  const isTestingGateway = gatewayTestState.status === "submitting";
  const isBindingGateway = gatewayBindingState.status === "submitting";
  const canTestGateway =
    selectedEntry !== null &&
    normalizedGatewayForm.isValid &&
    !isTestingGateway &&
    !isBindingGateway;
  const canSubmitGateway = canTestGateway && displayNameError === null;
  const maskedGatewayTokenLabel =
    selectedEntry?.gateway?.maskedToken ??
    maskTransientGatewayToken(gatewayTokenInput) ??
    "저장된 token 없음";

  const handleSelectWorkspace = (entry: GatewayStatusEntry) => {
    setSelectedWorkspaceId(entry.workspace.workspaceId);
    setGatewayDisplayNameDraft({
      workspaceId: entry.workspace.workspaceId,
      value: gatewayDisplayNames[String(entry.workspace.workspaceId)] ?? entry.workspace.name,
    });
    setGatewayUrlInput(entry.gateway?.gatewayUrl ?? "");
    setGatewayTokenInput("");
    setGatewayTestState(idleGatewayMutationState);
    setGatewayBindingState(idleGatewayMutationState);
  };

  const handleTestGatewayConnection = async () => {
    if (!selectedEntry) {
      return;
    }

    if (!normalizedGatewayForm.isValid) {
      setGatewayTestState({
        status: "error",
        result: null,
        message:
          normalizedGatewayForm.urlError ??
          normalizedGatewayForm.tokenError ??
          "gateway 연결 정보를 확인해 주세요.",
      });
      return;
    }

    const workspaceId = selectedEntry.workspace.workspaceId;
    setGatewayTestState({ status: "submitting", result: null, message: null });
    try {
      const result = await testWorkspaceGatewayConnection(resolveAccessToken(), workspaceId, {
        gatewayUrl: normalizedGatewayForm.gatewayUrl,
        token: normalizedGatewayForm.token,
      });
      setGatewayTestState({
        status: "success",
        result,
        message: result.message || gatewayConnectionStatusLabel(result.status),
      });
    } catch (error) {
      setGatewayTestState({
        status: "error",
        result: null,
        message: messageFromError(error, "gateway 연결 테스트를 처리하지 못했습니다."),
      });
    }
  };

  const handleCreateGatewayBinding = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedEntry) {
      return;
    }

    if (displayNameError) {
      setGatewayBindingState({
        status: "error",
        result: null,
        message: displayNameError,
      });
      return;
    }

    if (!normalizedGatewayForm.isValid) {
      setGatewayBindingState({
        status: "error",
        result: null,
        message:
          normalizedGatewayForm.urlError ??
          normalizedGatewayForm.tokenError ??
          "gateway 연결 정보를 확인해 주세요.",
      });
      return;
    }

    const workspaceId = selectedEntry.workspace.workspaceId;
    const accessToken = resolveAccessToken();
    setGatewayBindingState({ status: "submitting", result: null, message: null });
    try {
      await createWorkspaceGatewayBinding(accessToken, workspaceId, {
        gatewayUrl: normalizedGatewayForm.gatewayUrl,
        token: normalizedGatewayForm.token,
        validateConnection: normalizedGatewayForm.validateConnection,
      });
      const nextGatewayDisplayNames = {
        ...gatewayDisplayNames,
        [String(workspaceId)]: gatewayDisplayNameInput.trim(),
      };
      setGatewayDisplayNames(nextGatewayDisplayNames);
      setGatewayDisplayNameDraft({
        workspaceId,
        value: gatewayDisplayNameInput.trim(),
      });
      writeGatewayDisplayNames(nextGatewayDisplayNames);
      setGatewayTokenInput("");
      setGatewayBindingState({
        status: "success",
        result: null,
        message: "Gateway binding 등록이 완료되었습니다.",
      });

      const gateway = await fetchWorkspaceGatewayStatus(accessToken, workspaceId);
      setState((current) => ({
        ...current,
        entries: current.entries.map((entry) =>
          entry.workspace.workspaceId === workspaceId ? { ...entry, gateway, error: null } : entry,
        ),
      }));
    } catch (error) {
      setGatewayBindingState({
        status: "error",
        result: null,
        message: messageFromError(error, "gateway binding을 등록하지 못했습니다."),
      });
    }
  };

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

        <div className="workspace-list" aria-label="등록된 Gateway 목록">
          {state.entries.map((entry) => {
            const selected = entry.workspace.workspaceId === selectedEntry?.workspace.workspaceId;
            const displayName =
              gatewayDisplayNames[String(entry.workspace.workspaceId)] ?? entry.workspace.name;
            return (
              <button
                className="workspace-row workspace-row-link"
                type="button"
                key={entry.workspace.workspaceId}
                onClick={() => handleSelectWorkspace(entry)}
                aria-pressed={selected}
              >
                <div className="workspace-row-icon">
                  <ServerCog aria-hidden="true" />
                </div>
                <div className="workspace-row-main">
                  <strong>{displayName}</strong>
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
                <h2>{selectedGatewayDisplayName}</h2>
              </div>
              <Link
                className="secondary-action"
                href={`/workspaces/${selectedEntry.workspace.workspaceId}/settings`}
              >
                설정 열기
              </Link>
            </div>
            <div className="workspace-gateway-selected-summary" aria-label="선택된 Gateway 요약">
              <div>
                <span>Gateway</span>
                <strong>{selectedGatewayDisplayName}</strong>
                <p>{selectedEntry.gateway?.gatewayUrl ?? (gatewayUrlInput || "Gateway URL 미등록")}</p>
              </div>
              <div>
                <span>Token</span>
                <strong>{maskedGatewayTokenLabel}</strong>
                <p>
                  {selectedEntry.gateway?.lastStatus
                    ? gatewayConnectionStatusLabel(selectedEntry.gateway.lastStatus)
                    : "연결 확인 전"}
                </p>
              </div>
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
            <form
              className="workspace-settings-form workspace-gateway-form"
              onSubmit={handleCreateGatewayBinding}
            >
              <div className="workspace-gateway-form-grid">
                <label className="form-field">
                  <span>
                    <strong>표시 이름</strong>
                    <small>{`${gatewayDisplayNameInput.length}/${DISPLAY_NAME_MAX_LENGTH}`}</small>
                  </span>
                  <input
                    value={gatewayDisplayNameInput}
                    onChange={(event) => {
                      setGatewayDisplayNameDraft({
                        workspaceId: selectedEntry.workspace.workspaceId,
                        value: event.target.value,
                      });
                      setGatewayBindingState(idleGatewayMutationState);
                    }}
                    maxLength={DISPLAY_NAME_MAX_LENGTH}
                    placeholder="예: 로컬 OpenClaw Gateway"
                    disabled={isTestingGateway || isBindingGateway}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>OpenClaw Gateway URL</strong>
                    <small>http:// 또는 https://</small>
                  </span>
                  <input
                    value={gatewayUrlInput}
                    onChange={(event) => {
                      setGatewayUrlInput(event.target.value);
                      setGatewayTestState(idleGatewayMutationState);
                      setGatewayBindingState(idleGatewayMutationState);
                    }}
                    placeholder="https://gateway.example.com"
                    disabled={isTestingGateway || isBindingGateway}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>Token</strong>
                    <small>저장 후 masked token만 표시됩니다</small>
                  </span>
                  <input
                    value={gatewayTokenInput}
                    onChange={(event) => {
                      setGatewayTokenInput(event.target.value);
                      setGatewayTestState(idleGatewayMutationState);
                      setGatewayBindingState(idleGatewayMutationState);
                    }}
                    type="password"
                    autoComplete="off"
                    disabled={isTestingGateway || isBindingGateway}
                  />
                </label>
              </div>

              {displayNameError && gatewayDisplayNameInput.trim().length === 0 ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{displayNameError}</span>
                </div>
              ) : null}

              {normalizedGatewayForm.urlError && gatewayUrlInput.trim().length > 0 ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{normalizedGatewayForm.urlError}</span>
                </div>
              ) : null}

              {gatewayTestState.status === "error" ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{gatewayTestState.message}</span>
                </div>
              ) : null}

              {gatewayTestState.status === "success" && gatewayTestState.result ? (
                <div className="workspace-task-notice">
                  <CheckCircle2 aria-hidden="true" />
                  <span>
                    {`${gatewayConnectionStatusLabel(gatewayTestState.result.status)} · ${
                      gatewayTestState.message
                    } · agent ${gatewayTestState.result.agentCount}`}
                  </span>
                </div>
              ) : null}

              {gatewayBindingState.status === "error" ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{gatewayBindingState.message}</span>
                </div>
              ) : null}

              {gatewayBindingState.status === "success" ? (
                <div className="workspace-task-notice">
                  <CheckCircle2 aria-hidden="true" />
                  <span>{gatewayBindingState.message}</span>
                </div>
              ) : null}

              <div className="workspace-settings-actions workspace-gateway-actions">
                <button
                  className="secondary-action"
                  type="button"
                  onClick={() => void handleTestGatewayConnection()}
                  disabled={!canTestGateway}
                >
                  {isTestingGateway ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <RefreshCw aria-hidden="true" />
                  )}
                  <span>{isTestingGateway ? "테스트 중" : "Gateway 연결 테스트"}</span>
                </button>
                <button
                  className="primary-action primary-action-active"
                  type="submit"
                  aria-label="Gateway binding 등록"
                  disabled={!canSubmitGateway}
                >
                  {isBindingGateway ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <KeyRound aria-hidden="true" />
                  )}
                  <span>{isBindingGateway ? "등록 중" : "Gateway 등록"}</span>
                </button>
              </div>
            </form>
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

function readGatewayDisplayNames(): GatewayDisplayNames {
  if (typeof window === "undefined") {
    return {};
  }

  const rawValue = window.localStorage.getItem(GATEWAY_DISPLAY_NAMES_STORAGE_KEY);
  if (!rawValue) {
    return {};
  }

  try {
    const parsed = JSON.parse(rawValue);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }

    return Object.fromEntries(
      Object.entries(parsed).filter(
        (entry): entry is [string, string] =>
          typeof entry[0] === "string" && typeof entry[1] === "string",
      ),
    );
  } catch {
    return {};
  }
}

function writeGatewayDisplayNames(displayNames: GatewayDisplayNames): void {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(GATEWAY_DISPLAY_NAMES_STORAGE_KEY, JSON.stringify(displayNames));
}

function maskTransientGatewayToken(token: string): string | null {
  const trimmedToken = token.trim();
  if (trimmedToken.length === 0) {
    return null;
  }
  if (trimmedToken.length <= 8) {
    return "****";
  }
  return `${trimmedToken.slice(0, 4)}****${trimmedToken.slice(-4)}`;
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
