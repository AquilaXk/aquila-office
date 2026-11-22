"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import {
  AlertCircle,
  CheckCircle2,
  KeyRound,
  Loader2,
  PlugZap,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { ModelProvidersPanel } from "@/components/workspace/modals/ModelProvidersPanel";
import { getStoredUser, type ApiError } from "@/lib/api-client";
import { listWorkspaces, type WorkspaceSummary } from "@/lib/api/workspaces";
import { hydrateDesktopAccessToken, resolveAccessToken } from "@/lib/auth";
import {
  createWorkspaceGatewayBinding,
  fetchWorkspaceGatewayStatus,
  gatewayBindingToStatus,
  normalizeWorkspaceGatewayStatus,
  testWorkspaceGatewayConnection,
  type WorkspaceGatewayConnectionTest,
  type WorkspaceGatewayStatus,
} from "@/lib/gateway";
import {
  gatewayConnectionStatusLabel,
  gatewayStatusLabel,
  normalizeGatewayForm,
} from "@/lib/gateway-utils";
import { useWorkspaceModelProviders } from "@/lib/use-workspace-model-providers";

export default function GatewayPage() {
  const router = useRouter();
  const [hydrated, setHydrated] = useState(false);
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [workspacesLoading, setWorkspacesLoading] = useState(true);
  const [workspacesError, setWorkspacesError] = useState<string | null>(null);
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<number | null>(null);

  const [gatewayStatus, setGatewayStatus] = useState<WorkspaceGatewayStatus | null>(null);
  const [gatewayLoading, setGatewayLoading] = useState(false);
  const [gatewayLoadError, setGatewayLoadError] = useState<string | null>(null);

  const [gatewayUrl, setGatewayUrl] = useState("");
  const [token, setToken] = useState("");
  const [validateConnection, setValidateConnection] = useState(true);
  const [testResult, setTestResult] = useState<WorkspaceGatewayConnectionTest | null>(null);
  const [testing, setTesting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  const loadGatewayForWorkspace = useCallback(
    async (workspaceId: number, options?: { preserveFeedback?: boolean }) => {
      setGatewayLoading(true);
      setGatewayLoadError(null);
      if (!options?.preserveFeedback) {
        setTestResult(null);
        setFormError(null);
        setFormSuccess(null);
      }

    try {
      const raw = await fetchWorkspaceGatewayStatus(resolveAccessToken(), workspaceId);
      const normalized = normalizeWorkspaceGatewayStatus(raw);
      setGatewayStatus(normalized);
      if (normalized.gatewayUrl) {
        setGatewayUrl(normalized.gatewayUrl);
      }
    } catch (err) {
      const apiErr = err as ApiError;
      setGatewayStatus(null);
      setGatewayLoadError(
        apiErr?.message ?? "워크스페이스 게이트웨이 상태를 불러오지 못했습니다.",
      );
      } finally {
        setGatewayLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    let cancelled = false;
    void hydrateDesktopAccessToken().then(async () => {
      if (cancelled) return;
      if (!getStoredUser()) {
        router.replace("/");
        return;
      }
      setHydrated(true);
      setWorkspacesLoading(true);
      setWorkspacesError(null);
      try {
        const list = await listWorkspaces();
        if (cancelled) return;
        const resolved = list ?? [];
        setWorkspaces(resolved);
        const preferred =
          resolved.find((item) => item.myRole === "ADMIN") ?? resolved[0] ?? null;
        if (preferred) {
          setSelectedWorkspaceId(preferred.workspaceId);
          await loadGatewayForWorkspace(preferred.workspaceId);
        }
      } catch (err) {
        if (!cancelled) {
          const apiErr = err as ApiError;
          setWorkspacesError(apiErr?.message ?? "워크스페이스 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (!cancelled) {
          setWorkspacesLoading(false);
        }
      }
    });
    return () => {
      cancelled = true;
    };
  }, [loadGatewayForWorkspace, router]);

  const selectedWorkspace = useMemo(
    () => workspaces.find((item) => item.workspaceId === selectedWorkspaceId) ?? null,
    [selectedWorkspaceId, workspaces],
  );
  const canManage = selectedWorkspace?.myRole === "ADMIN";
  const modelProvidersController = useWorkspaceModelProviders({
    workspaceId: selectedWorkspaceId,
    canManage,
    enabled: hydrated && Boolean(selectedWorkspaceId),
  });
  const normalizedForm = useMemo(
    () =>
      normalizeGatewayForm({
        gatewayUrl,
        token,
        validateConnection,
      }),
    [gatewayUrl, token, validateConnection],
  );

  async function handleTestConnection() {
    if (!selectedWorkspaceId || !canManage) {
      return;
    }
    if (!normalizedForm.isValid) {
      setFormError(
        normalizedForm.urlError ??
          normalizedForm.tokenError ??
          "게이트웨이 URL과 토큰을 확인해 주세요.",
      );
      return;
    }

    setTesting(true);
    setFormError(null);
    setFormSuccess(null);
    try {
      const result = await testWorkspaceGatewayConnection(
        resolveAccessToken(),
        selectedWorkspaceId,
        {
          gatewayUrl: normalizedForm.gatewayUrl,
          token: normalizedForm.token,
        },
      );
      setTestResult(result);
      setFormSuccess(result.message || gatewayConnectionStatusLabel(result.status));
    } catch (err) {
      const apiErr = err as ApiError;
      setTestResult(null);
      setFormError(apiErr?.message ?? "게이트웨이 연결 테스트에 실패했습니다.");
    } finally {
      setTesting(false);
    }
  }

  async function handleBindSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedWorkspaceId || !canManage) {
      return;
    }
    if (!normalizedForm.isValid) {
      setFormError(
        normalizedForm.urlError ??
          normalizedForm.tokenError ??
          "게이트웨이 URL과 토큰을 확인해 주세요.",
      );
      return;
    }

    setSubmitting(true);
    setFormError(null);
    setFormSuccess(null);
    try {
      const binding = await createWorkspaceGatewayBinding(
        resolveAccessToken(),
        selectedWorkspaceId,
        {
          gatewayUrl: normalizedForm.gatewayUrl,
          token: normalizedForm.token,
          validateConnection: normalizedForm.validateConnection,
        },
      );
      const updated = gatewayBindingToStatus(binding);
      setGatewayStatus(updated);
      setToken("");
      setFormSuccess("OpenClaw 게이트웨이 연결을 저장했습니다.");
      await loadGatewayForWorkspace(selectedWorkspaceId, { preserveFeedback: true });
    } catch (err) {
      const apiErr = err as ApiError;
      setFormError(apiErr?.message ?? "OpenClaw 게이트웨이 연결을 저장하지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!hydrated) {
    return (
      <AppTopChrome
        title="OpenClaw 게이트웨이 연동"
        backHref="/workspaces"
        backLabel="워크스페이스 목록"
      >
        <div className="ao-page-container" aria-live="polite" aria-busy="true">
          <section className="ao-surface-panel">
            <div className="inline-flex items-center gap-2 text-[13px] text-[var(--ui-text-secondary)]">
              <Loader2 size={15} className="spin" aria-hidden="true" />
              <span>인증 및 게이트웨이 연동 상태를 확인하는 중입니다.</span>
            </div>
          </section>
        </div>
      </AppTopChrome>
    );
  }

  return (
    <AppTopChrome
      title="OpenClaw 게이트웨이 연동"
      backHref="/workspaces"
      backLabel="워크스페이스 목록"
    >
      <div className="ao-page-container">
        <header className="ao-page-header">
          <div>
            <p className="ao-page-eyebrow">실행 환경 연동</p>
            <h1 className="ao-page-title">OpenClaw 게이트웨이 연동 관리</h1>
            <p className="ao-page-subtitle">
              워크스페이스별 AI 에이전트 실행 게이트웨이의 실시간 상태를 조회하고 연결 테스트 및
              바인딩을 등록합니다.
            </p>
          </div>
          {selectedWorkspaceId ? (
            <button
              type="button"
              className="secondary-action"
              disabled={gatewayLoading}
              onClick={() => void loadGatewayForWorkspace(selectedWorkspaceId)}
            >
              <RefreshCw size={14} className={gatewayLoading ? "spin" : undefined} aria-hidden="true" />
              <span>상태 새로고침</span>
            </button>
          ) : null}
        </header>

        {workspacesError ? (
          <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
            <AlertCircle size={15} aria-hidden="true" />
            <span>{workspacesError}</span>
          </div>
        ) : null}

        {workspacesLoading ? (
          <section className="ao-surface-panel">
            <div className="inline-flex items-center gap-2 text-[13px]">
              <Loader2 size={15} className="spin" aria-hidden="true" />
              <span>워크스페이스 목록을 불러오는 중입니다.</span>
            </div>
          </section>
        ) : workspaces.length === 0 ? (
          <section className="ao-surface-panel flex flex-col items-start gap-3">
            <strong className="text-[14px] text-[var(--ui-text-strong)]">
              연동할 워크스페이스가 없습니다
            </strong>
            <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
              게이트웨이를 연결하려면 먼저 워크스페이스를 생성해 주세요.
            </p>
            <Link href="/workspaces/new" className="primary-action primary-action-active">
              <span>새 워크스페이스 만들기</span>
            </Link>
          </section>
        ) : (
          <div className="chrome-split-pane">
            <section className="ao-surface-panel flex flex-col gap-3" aria-label="대상 워크스페이스 선택">
              <div className="ao-surface-panel-header">
                <h2>워크스페이스 선택 ({workspaces.length})</h2>
              </div>
              <div className="flex flex-col gap-1.5">
                {workspaces.map((ws) => {
                  const active = ws.workspaceId === selectedWorkspaceId;
                  return (
                    <button
                      key={ws.workspaceId}
                      type="button"
                      onClick={() => {
                        setSelectedWorkspaceId(ws.workspaceId);
                        void loadGatewayForWorkspace(ws.workspaceId);
                      }}
                      className={`chrome-sidebar-item border border-[var(--ui-border-subtle)]${
                        active ? " chrome-sidebar-item-active" : ""
                      }`}
                    >
                      <span className="truncate">{ws.name}</span>
                      <span className="role-badge">{ws.myRole}</span>
                    </button>
                  );
                })}
              </div>
            </section>

            <section className="flex flex-col gap-4 min-w-0" aria-label="게이트웨이 상태 및 설정">
              <div className="ao-surface-panel flex flex-col gap-3">
                <div className="ao-surface-panel-header">
                  <h2>현재 게이트웨이 상태</h2>
                  {selectedWorkspace ? (
                    <Link
                      href={`/workspaces/${selectedWorkspace.workspaceId}`}
                      className="app-top-chrome-link"
                    >
                      워크스페이스 열기
                    </Link>
                  ) : null}
                </div>

                {gatewayLoadError ? (
                  <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                    <AlertCircle size={15} aria-hidden="true" />
                    <span>{gatewayLoadError}</span>
                  </div>
                ) : null}

                <div className="workspace-gateway-status-list" aria-label="게이트웨이 상태 상세">
                  <div>
                    <span>연결 상태</span>
                    <strong>
                      {gatewayStatus ? gatewayStatusLabel(gatewayStatus.status) : "정보 없음"}
                    </strong>
                  </div>
                  <div>
                    <span>게이트웨이 URL</span>
                    <strong>{gatewayStatus?.gatewayUrl || "-"}</strong>
                  </div>
                  <div>
                    <span>최근 검증 결과</span>
                    <strong>
                      {gatewayStatus?.lastStatus
                        ? gatewayConnectionStatusLabel(gatewayStatus.lastStatus)
                        : "-"}
                    </strong>
                  </div>
                  <div>
                    <span>테스트 응답</span>
                    <strong>
                      {testResult ? gatewayConnectionStatusLabel(testResult.status) : "-"}
                    </strong>
                  </div>
                </div>
              </div>

              <form className="ao-surface-panel flex flex-col gap-4" onSubmit={handleBindSubmit}>
                <div className="ao-surface-panel-header">
                  <h2>게이트웨이 연결 등록 및 수정</h2>
                  <PlugZap size={16} aria-hidden="true" />
                </div>

                {!canManage ? (
                  <div className="ao-inline-notice ao-inline-notice-warning" role="status">
                    <AlertCircle size={15} aria-hidden="true" />
                    <span>관리자(ADMIN) 권한이 있는 워크스페이스만 게이트웨이를 변경할 수 있습니다.</span>
                  </div>
                ) : null}

                <div className="workspace-gateway-form-grid">
                  <label className="form-field">
                    <span>
                      <strong>게이트웨이 URL</strong>
                      <small>http/https</small>
                    </span>
                    <input
                      aria-label="Gateway URL"
                      disabled={!canManage || submitting}
                      placeholder="https://gateway.example.com"
                      value={gatewayUrl}
                      onChange={(event) => setGatewayUrl(event.target.value)}
                    />
                  </label>

                  <label className="form-field">
                    <span>
                      <strong>액세스 토큰</strong>
                      <small>필수</small>
                    </span>
                    <input
                      aria-label="Gateway Token"
                      type="password"
                      disabled={!canManage || submitting}
                      placeholder="게이트웨이 인증 토큰 입력"
                      value={token}
                      onChange={(event) => setToken(event.target.value)}
                    />
                  </label>
                </div>

                <label className="inline-flex items-center gap-2 text-[13px] cursor-pointer">
                  <input
                    type="checkbox"
                    checked={validateConnection}
                    disabled={!canManage || submitting}
                    onChange={(event) => setValidateConnection(event.target.checked)}
                  />
                  <span>저장 전 게이트웨이 연결 상태 검증</span>
                </label>

                {formError ? (
                  <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                    <AlertCircle size={15} aria-hidden="true" />
                    <span>{formError}</span>
                  </div>
                ) : null}

                {formSuccess ? (
                  <div className="ao-inline-notice ao-inline-notice-success" role="status">
                    <CheckCircle2 size={15} aria-hidden="true" />
                    <span>{formSuccess}</span>
                  </div>
                ) : null}

                <div className="flex flex-wrap items-center justify-end gap-2">
                  <button
                    type="button"
                    className="secondary-action"
                    disabled={!canManage || testing || submitting || !normalizedForm.isValid}
                    onClick={() => void handleTestConnection()}
                  >
                    {testing ? (
                      <Loader2 size={14} className="spin" aria-hidden="true" />
                    ) : (
                      <ShieldCheck size={14} aria-hidden="true" />
                    )}
                    <span>{testing ? "테스트 중" : "연결 테스트"}</span>
                  </button>
                  <button
                    type="submit"
                    className="primary-action primary-action-active"
                    disabled={!canManage || testing || submitting || !normalizedForm.isValid}
                  >
                    {submitting ? (
                      <Loader2 size={14} className="spin" aria-hidden="true" />
                    ) : (
                      <KeyRound size={14} aria-hidden="true" />
                    )}
                    <span>{submitting ? "저장 중" : "OpenClaw 연결 저장"}</span>
                  </button>
                </div>
              </form>

              <section className="ao-surface-panel flex flex-col gap-4" aria-label="AI 모델 프로바이더 관리">
                <div className="ao-surface-panel-header">
                  <div>
                    <h2>AI 모델 프로바이더 및 폴백 체인</h2>
                    <p className="m-0 mt-1 text-[12px] text-[var(--ui-text-muted)]">
                      OpenAI · Anthropic · Google Gemini · Ollama 및 OpenAI 호환 API 프로바이더를
                      등록하고 연결을 검증합니다.
                    </p>
                  </div>
                </div>
                <ModelProvidersPanel controller={modelProvidersController} />
              </section>
            </section>
          </div>
        )}
      </div>
    </AppTopChrome>
  );
}
