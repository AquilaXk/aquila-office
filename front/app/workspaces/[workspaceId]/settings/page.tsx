"use client";

import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarDays,
  Hash,
  Loader2,
  RefreshCw,
  ShieldCheck,
  UserRound,
  Users,
  WifiOff,
} from "lucide-react";
import Link from "next/link";
import { use, useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { ApiError } from "@/lib/api-client";
import {
  fetchWorkspaceDetail,
  fetchWorkspaceMembers,
  type WorkspaceDetail,
  type WorkspaceMember,
  type WorkspaceMemberRole,
} from "@/lib/workspaces";

const TEMP_MEMBER_GOOGLE_SUB =
  process.env.NEXT_PUBLIC_DEV_MEMBER_GOOGLE_SUB ?? "google-1";

type WorkspaceSettingsState =
  | { status: "loading"; workspace: null; members: []; error: null }
  | { status: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[]; error: null }
  | { status: "error" | "offline"; workspace: null; members: []; error: string };

export default function WorkspaceSettingsPage({
  params,
}: {
  params: Promise<{ workspaceId: string }>;
}) {
  const { workspaceId } = use(params);
  const numericWorkspaceId = useMemo(() => Number(workspaceId), [workspaceId]);
  const [state, setState] = useState<WorkspaceSettingsState>({
    status: "loading",
    workspace: null,
    members: [],
    error: null,
  });

  const fetchSettings = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      return {
        status: "error",
        workspace: null,
        members: [],
        error: "올바르지 않은 워크스페이스입니다.",
      } satisfies WorkspaceSettingsState;
    }

    try {
      const [workspace, members] = await Promise.all([
        fetchWorkspaceDetail(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId),
        fetchWorkspaceMembers(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId),
      ]);
      return { status: "ready", workspace, members, error: null } satisfies WorkspaceSettingsState;
    } catch (error) {
      return toFailureState(error, "워크스페이스 설정을 확인하지 못했습니다.");
    }
  }, [numericWorkspaceId]);

  const reloadSettings = useCallback(async () => {
    setState({ status: "loading", workspace: null, members: [], error: null });
    setState(await fetchSettings());
  }, [fetchSettings]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialSettings() {
      const nextState = await fetchSettings();
      if (!ignore) {
        setState(nextState);
      }
    }

    void loadInitialSettings();

    return () => {
      ignore = true;
    };
  }, [fetchSettings]);

  const workspace = state.workspace;
  const members = state.status === "ready" ? state.members : [];
  const isAdmin = workspace?.myRole === "ADMIN";
  const adminCount = members.filter((member) => member.role === "ADMIN").length;

  return (
    <main className="workspace-settings-shell">
      <section className="workspace-settings-panel" aria-label="워크스페이스 설정">
        <header className="workspace-settings-header">
          <div className="workspace-settings-nav">
            <Link className="secondary-action workspace-settings-back" href="/">
              <ArrowLeft aria-hidden="true" />
              <span>목록으로</span>
            </Link>
            {isValidWorkspaceId(numericWorkspaceId) ? (
              <Link
                className="secondary-action workspace-settings-back"
                href={`/workspaces/${numericWorkspaceId}`}
              >
                <Building2 aria-hidden="true" />
                <span>상세로</span>
              </Link>
            ) : null}
          </div>

          <div className="workspace-settings-title">
            <div className="workspace-settings-icon">
              <ShieldCheck aria-hidden="true" />
            </div>
            <div>
              <p className="eyebrow">Workspace Settings</p>
              <h1>{workspace?.name ?? "워크스페이스 설정"}</h1>
            </div>
          </div>
        </header>

        {state.status === "loading" ? (
          <WorkspaceSettingsStateMessage
            tone="neutral"
            icon={<Loader2 className="spin" aria-hidden="true" />}
            title="설정 확인 중"
            message="워크스페이스 설정과 멤버 정보를 불러오고 있습니다."
          />
        ) : null}

        {state.status === "error" || state.status === "offline" ? (
          <WorkspaceSettingsStateMessage
            tone={state.status === "offline" ? "offline" : "danger"}
            icon={
              state.status === "offline" ? (
                <WifiOff aria-hidden="true" />
              ) : (
                <AlertCircle aria-hidden="true" />
              )
            }
            title={state.status === "offline" ? "연결 실패" : "설정 확인 실패"}
            message={state.error}
            action={
              <button
                className="secondary-action workspace-settings-retry"
                type="button"
                onClick={() => void reloadSettings()}
              >
                <RefreshCw aria-hidden="true" />
                <span>다시 시도</span>
              </button>
            }
          />
        ) : null}

        {workspace ? (
          <section className="workspace-settings-content" aria-label="설정 내용">
            <div className="workspace-settings-summary">
              <div>
                <span className="workspace-detail-label">현재 워크스페이스</span>
                <strong>{workspace.name}</strong>
                <p>{workspace.description ?? "설명 없음"}</p>
              </div>
              <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                {isAdmin ? "ADMIN" : "MEMBER"}
              </span>
            </div>

            <dl className="workspace-settings-meta">
              <SettingsMetaItem
                icon={<ShieldCheck aria-hidden="true" />}
                label="내 역할"
                value={<span className="role-badge">{formatRole(workspace.myRole)}</span>}
              />
              <SettingsMetaItem
                icon={<Users aria-hidden="true" />}
                label="멤버"
                value={`${members.length}명 · 관리자 ${adminCount}명`}
              />
              <SettingsMetaItem
                icon={<CalendarDays aria-hidden="true" />}
                label="참여 일시"
                value={formatDateTime(workspace.joinedAt)}
              />
              <SettingsMetaItem
                icon={<Hash aria-hidden="true" />}
                label="Workspace ID"
                value={workspace.workspaceId}
              />
            </dl>

            <section className="workspace-settings-section" aria-label="멤버 현황">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Members</span>
                  <h2>멤버 현황</h2>
                </div>
                <span className="workspace-count-badge">{members.length}명</span>
              </div>

              {members.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {members.map((member) => (
                    <article className="workspace-settings-member-row" key={member.memberId}>
                      <div className="workspace-member-avatar">
                        <UserRound aria-hidden="true" />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{member.name}</strong>
                        <p>{member.email}</p>
                      </div>
                      <div className="workspace-member-meta">
                        <span className="role-badge">{formatRole(member.role)}</span>
                        <span>{formatDateTime(member.joinedAt)}</span>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="workspace-member-empty">
                  <Users aria-hidden="true" />
                  <strong>표시할 멤버가 없습니다</strong>
                  <p>워크스페이스에 참여한 멤버가 생기면 이곳에 표시됩니다.</p>
                </div>
              )}
            </section>
          </section>
        ) : null}
      </section>

      <aside className="workspace-settings-side" aria-label="설정 조회 정보">
        <div>
          <UserRound aria-hidden="true" />
          <strong>{TEMP_MEMBER_GOOGLE_SUB}</strong>
          <span>현재 임시 회원 식별자</span>
        </div>
        <div>
          <Hash aria-hidden="true" />
          <strong>{isValidWorkspaceId(numericWorkspaceId) ? numericWorkspaceId : "-"}</strong>
          <span>요청 워크스페이스 ID</span>
        </div>
        <div>
          <ShieldCheck aria-hidden="true" />
          <strong>{isAdmin ? "관리 가능" : "읽기 전용"}</strong>
          <span>멤버 {members.length}명 · 관리자 {adminCount}명</span>
        </div>
      </aside>
    </main>
  );
}

function WorkspaceSettingsStateMessage({
  tone,
  icon,
  title,
  message,
  action,
}: {
  tone: "neutral" | "danger" | "offline";
  icon: ReactNode;
  title: string;
  message: string;
  action?: ReactNode;
}) {
  return (
    <div className={`workspace-detail-state workspace-detail-state-${tone}`}>
      {icon}
      <strong>{title}</strong>
      <p>{message}</p>
      {action}
    </div>
  );
}

function SettingsMetaItem({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: ReactNode;
}) {
  return (
    <div>
      <dt>
        {icon}
        <span>{label}</span>
      </dt>
      <dd>{value}</dd>
    </div>
  );
}

function toFailureState(error: unknown, fallback: string): WorkspaceSettingsState {
  if (error instanceof ApiError) {
    return {
      status: "error",
      workspace: null,
      members: [],
      error: error.message,
    };
  }

  return {
    status: "offline",
    workspace: null,
    members: [],
    error: fallback,
  };
}

function isValidWorkspaceId(workspaceId: number): boolean {
  return Number.isInteger(workspaceId) && workspaceId > 0;
}

function formatRole(role: WorkspaceMemberRole): string {
  return role === "ADMIN" ? "관리자" : "멤버";
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}
