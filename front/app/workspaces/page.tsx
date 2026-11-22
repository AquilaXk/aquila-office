"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  ArrowRight,
  Bot,
  Coins,
  FolderPlus,
  Loader2,
  PlugZap,
  Settings,
} from "lucide-react";
import { PixelAvatar } from "@/components/arcade";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { formatTokenUsageSummary } from "@/components/workspace/workspace-ui-helpers";
import { getStoredUser, type ApiError, type AuthUser } from "@/lib/api-client";
import { listWorkspaces, type WorkspaceSummary } from "@/lib/api/workspaces";
import { hydrateDesktopAccessToken } from "@/lib/auth";
import { searchBitapItems } from "@/lib/bitap-fuzzy-search";

export default function WorkspacesPage() {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [hydrated, setHydrated] = useState(false);
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>("");
  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<"ALL" | "ADMIN" | "MEMBER">("ALL");

  useEffect(() => {
    let cancelled = false;
    void hydrateDesktopAccessToken().then(() => {
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
        if (!cancelled) {
          setWorkspaces(data ?? []);
        }
      } catch (err) {
        const apiErr = err as ApiError;
        if (!cancelled) {
          setError(apiErr?.message ?? "워크스페이스 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [hydrated]);

  const { activeCount, agentCount, completedCount } = useMemo(() => {
    let active = 0;
    let agents = 0;
    let completed = 0;
    for (let i = 0; i < workspaces.length; i += 1) {
      const ws = workspaces[i];
      active += ws.runningTaskCount ?? 0;
      agents += ws.agentCount ?? 0;
      completed += ws.completedTaskCount ?? 0;
    }
    return { activeCount: active, agentCount: agents, completedCount: completed };
  }, [workspaces]);

  const indexedWorkspaces = useMemo(
    () =>
      workspaces.map((ws, index) => ({
        ws,
        index,
        code: `W${String(index + 1).padStart(3, "0")}`,
      })),
    [workspaces],
  );

  const filteredWorkspaces = useMemo(() => {
    const byRole =
      roleFilter === "ALL"
        ? indexedWorkspaces
        : indexedWorkspaces.filter((entry) => entry.ws.myRole === roleFilter);
    const trimmed = searchQuery.trim();
    if (!trimmed) {
      return byRole;
    }
    return searchBitapItems(
      byRole,
      trimmed,
      (entry) => `${entry.code} ${entry.ws.name} ${entry.ws.description ?? ""}`,
      { maxDistance: 1 },
    ).map((hit) => hit.item);
  }, [indexedWorkspaces, roleFilter, searchQuery]);

  if (!hydrated) {
    return (
      <AppTopChrome title="워크스페이스 목록">
        <div className="ao-page-container">
          <section
            className="ao-surface-panel flex items-center gap-2 text-[13px] text-[var(--ui-text-secondary)]"
            aria-live="polite"
            aria-busy="true"
          >
            <Loader2 size={15} className="spin" aria-hidden="true" />
            <span>워크스페이스 목록을 준비하는 중입니다.</span>
          </section>
        </div>
      </AppTopChrome>
    );
  }

  return (
    <AppTopChrome
      title="워크스페이스 목록"
      actions={
        <Link href="/workspaces/new" className="tb-btn tb-btn-primary">
          <FolderPlus size={13} aria-hidden="true" />
          <span>새 워크스페이스</span>
        </Link>
      }
    >
      <div className="ao-page-container">
        <header className="ao-page-header">
          <div>
            <p className="ao-page-eyebrow">{user?.name ?? "구성원"}</p>
            <h1 className="ao-page-title">워크스페이스 목록</h1>
            <p className="ao-page-subtitle">
              협업 중인 오피스 워크스페이스를 선택해 2D 스테이지, 작업 보드 및 에이전트 관리를
              시작하세요.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Link href="/workspaces/gateway" className="secondary-action">
              <PlugZap size={14} aria-hidden="true" />
              <span>게이트웨이 연동</span>
            </Link>
            <Link href="/workspaces/new" className="primary-action primary-action-active">
              <FolderPlus size={14} aria-hidden="true" />
              <span>새 워크스페이스</span>
            </Link>
          </div>
        </header>

        <div className="ao-studio-layout">
          <aside className="ao-studio-rail" aria-label="스튜디오 크루 요약">
            <div className="flex items-center gap-3 pb-3 border-b border-[var(--ui-border-subtle)]">
              <div className="p-2 rounded-[10px] border border-[var(--ui-border)] bg-[var(--ui-surface)] shrink-0">
                <PixelAvatar kind="alex" size={3.2} activity="idle" />
              </div>
              <div className="min-w-0">
                <span className="block text-[12px] text-[var(--ui-text-muted)]">활성 프로필</span>
                <strong className="block text-[14px] text-[var(--ui-text-strong)] truncate">
                  {user?.name ?? "구성원"}
                </strong>
                <span className="ao-live-pill ao-tone-ok mt-1" data-live="true">
                  세션 연결됨
                </span>
              </div>
            </div>

            <div>
              <span className="block text-[12px] font-semibold text-[var(--ui-text-muted)] mb-2">
                표준 에이전트 구성
              </span>
              <div className="ao-crew-avatar-grid">
                {(
                  [
                    { key: "mira", kind: "mira", variantKey: "mira", title: "디자인 리드" },
                    { key: "kenji", kind: "kenji", variantKey: "kenji", title: "백엔드 엔지니어" },
                    { key: "yuna", kind: "yuna", variantKey: "yuna", title: "프로덕트 매니저" },
                    { key: "diego", kind: "diego", variantKey: "diego", title: "인프라 리드" },
                    {
                      key: "ag-orch",
                      kind: "agent",
                      variantKey: "Ops Orchestrator",
                      category: "ORCHESTRATOR",
                      title: "오케스트레이터",
                    },
                    {
                      key: "ag-back",
                      kind: "agent",
                      variantKey: "Backend Architect",
                      category: "BACKEND",
                      title: "백엔드 에이전트",
                    },
                    {
                      key: "ag-design",
                      kind: "agent",
                      variantKey: "Design Specialist",
                      category: "DESIGN",
                      title: "UI/UX 에이전트",
                    },
                    {
                      key: "ag-sec",
                      kind: "agent",
                      variantKey: "Security Auditor",
                      category: "SECURITY",
                      title: "보안 검증 에이전트",
                    },
                  ] as const
                ).map((crew) => (
                  <div key={crew.key} className="ao-crew-mugshot-chip" title={crew.title}>
                    <PixelAvatar
                      kind={crew.kind}
                      variant="mugshot"
                      variantKey={crew.variantKey}
                      agentCategory={"category" in crew ? crew.category : undefined}
                      size={1.75}
                    />
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-3 border-t border-[var(--ui-border-subtle)] flex flex-col gap-1.5 text-[12px] text-[var(--ui-text-secondary)]">
              <div className="flex items-center justify-between gap-2">
                <span>참여 공간</span>
                <strong className="text-[var(--ui-text-strong)] font-mono">
                  {workspaces.length}
                </strong>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span>활성 에이전트</span>
                <strong className="text-[var(--ui-text-strong)] font-mono">{agentCount}</strong>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span>실행 중 작업</span>
                <strong className="text-[var(--ui-text-strong)] font-mono">{activeCount}</strong>
              </div>
            </div>
          </aside>

          <div className="min-w-0 flex flex-col gap-4">
            <section className="ao-kpi-grid" aria-label="전체 워크스페이스 요약">
              <article className="ao-kpi-card">
                <span>참여 워크스페이스</span>
                <strong>{workspaces.length}</strong>
                <small>접근 가능한 팀 공간</small>
              </article>
              <article className="ao-kpi-card">
                <span>등록된 에이전트</span>
                <strong>{agentCount}</strong>
                <small>전체 워크스페이스 합산</small>
              </article>
              <article className="ao-kpi-card">
                <span>진행 중 작업</span>
                <strong>{activeCount}</strong>
                <small>실행 및 대기 포함</small>
              </article>
              <article className="ao-kpi-card">
                <span>완료된 작업</span>
                <strong>{completedCount}</strong>
                <small>누적 완료 건수</small>
              </article>
            </section>

            {error ? (
              <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                <AlertCircle size={15} aria-hidden="true" />
                <span>{error}</span>
              </div>
            ) : null}

            <section className="ao-surface-panel" aria-label="내 워크스페이스">
              <div className="ao-surface-panel-header flex-wrap gap-2">
                <h2>내 워크스페이스 ({workspaces.length})</h2>
                {workspaces.length >= 2 ? (
                  <div className="flex flex-wrap items-center gap-2">
                    <input
                      type="search"
                      aria-label="워크스페이스 검색"
                      placeholder="코드·이름 검색"
                      value={searchQuery}
                      onChange={(event) => setSearchQuery(event.target.value)}
                      className="h-8 px-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-surface)] text-[13px] text-[var(--ui-text)] max-w-[200px]"
                    />
                    <select
                      aria-label="워크스페이스 역할 필터"
                      value={roleFilter}
                      onChange={(event) =>
                        setRoleFilter(event.target.value as "ALL" | "ADMIN" | "MEMBER")
                      }
                      className="h-8 px-2 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-surface)] text-[13px] text-[var(--ui-text)]"
                    >
                      <option value="ALL">전체 역할</option>
                      <option value="ADMIN">ADMIN</option>
                      <option value="MEMBER">MEMBER</option>
                    </select>
                  </div>
                ) : null}
              </div>

              {loading ? (
                <div className="inline-flex items-center gap-2 py-6 text-[13px] text-[var(--ui-text-secondary)]">
                  <Loader2 size={15} className="spin" aria-hidden="true" />
                  <span>워크스페이스 목록을 불러오는 중입니다.</span>
                </div>
              ) : workspaces.length === 0 ? (
                <div className="text-center py-8 flex flex-col items-center gap-3">
                  <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
                    아직 참여 중인 워크스페이스가 없습니다.
                  </p>
                  <Link href="/workspaces/new" className="primary-action primary-action-active">
                    <FolderPlus size={14} aria-hidden="true" />
                    <span>첫 워크스페이스 만들기</span>
                  </Link>
                </div>
              ) : filteredWorkspaces.length === 0 ? (
                <div className="text-center py-6 flex flex-col items-center gap-2">
                  <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
                    조건에 맞는 워크스페이스가 없습니다.
                  </p>
                  <button
                    type="button"
                    className="tb-btn"
                    onClick={() => {
                      setSearchQuery("");
                      setRoleFilter("ALL");
                    }}
                  >
                    필터 초기화
                  </button>
                </div>
              ) : (
                <div className="flex flex-col gap-2.5">
                  {filteredWorkspaces.map(({ ws, index }) => {
                    const tokenSummary = formatTokenUsageSummary(
                      ws.todayUsedTokens,
                      ws.dailyTokenLimit,
                    );
                    const memberKind = (
                      ["alex", "mira", "kenji", "yuna", "diego", "iris"] as const
                    )[index % 6];
                    const rowAgentCategory = (
                      ["ORCHESTRATOR", "BACKEND", "QA", "DESIGN"] as const
                    )[index % 4];
                    const roleClass =
                      ws.myRole === "ADMIN"
                        ? "role-badge role-admin"
                        : "role-badge role-member";
                    return (
                      <article
                        key={ws.workspaceId}
                        className="p-3.5 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] flex flex-wrap items-center justify-between gap-4"
                      >
                        <div className="flex items-center gap-3 min-w-0 flex-1">
                          <div className="flex items-center -space-x-2 shrink-0">
                            <div className="ao-crew-mugshot-chip">
                              <PixelAvatar
                                kind={memberKind}
                                variant="mugshot"
                                variantKey={`${ws.workspaceId}-${ws.name}-lead`}
                                size={1.9}
                              />
                            </div>
                            <div className="ao-crew-mugshot-chip">
                              <PixelAvatar
                                kind="agent"
                                variant="mugshot"
                                variantKey={`${ws.workspaceId}-${ws.name}`}
                                agentCategory={rowAgentCategory}
                                size={1.9}
                              />
                            </div>
                          </div>
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="font-mono text-[12px] text-[var(--ui-text-muted)]">
                                W{String(index + 1).padStart(3, "0")}
                              </span>
                              <Link
                                href={`/workspaces/${ws.workspaceId}`}
                                className="text-[15px] font-semibold text-[var(--ui-text-strong)] no-underline hover:underline truncate"
                              >
                                {ws.name}
                              </Link>
                              <span className={roleClass}>{ws.myRole}</span>
                            </div>
                            {ws.description ? (
                              <p className="m-0 mt-1 text-[13px] text-[var(--ui-text-secondary)] truncate">
                                {ws.description}
                              </p>
                            ) : null}
                            <div className="flex flex-wrap items-center gap-3 mt-1.5 text-[12px] text-[var(--ui-text-muted)]">
                              <span className="inline-flex items-center gap-1">
                                <Bot size={12} aria-hidden="true" />
                                에이전트 {ws.agentCount ?? 0}
                              </span>
                              <span>진행 {ws.runningTaskCount ?? 0}</span>
                              <span>완료 {ws.completedTaskCount ?? 0}</span>
                              <span className="inline-flex items-center gap-1">
                                <Coins size={12} aria-hidden="true" />
                                토큰 {tokenSummary.label}
                              </span>
                            </div>
                            {tokenSummary.percent !== null ? (
                              <div className="ao-usage-meter mt-2 max-w-[240px]">
                                <span style={{ width: `${Math.min(100, tokenSummary.percent)}%` }} />
                              </div>
                            ) : null}
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0">
                          <Link
                            href={`/workspaces/${ws.workspaceId}/settings`}
                            className="secondary-action"
                            aria-label={`${ws.name} 설정`}
                          >
                            <Settings size={14} aria-hidden="true" />
                            <span>설정</span>
                          </Link>
                          <Link
                            href={`/workspaces/${ws.workspaceId}`}
                            className="primary-action primary-action-active"
                          >
                            <span>열기</span>
                            <ArrowRight size={14} aria-hidden="true" />
                          </Link>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          </div>
        </div>
      </div>
    </AppTopChrome>
  );
}
