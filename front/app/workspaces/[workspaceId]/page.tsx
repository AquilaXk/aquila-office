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

type WorkspaceDetailState =
  | { status: "loading"; workspace: null; error: null }
  | { status: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[]; error: null }
  | { status: "error" | "offline"; workspace: null; error: string };

export default function WorkspaceDetailPage({
  params,
}: {
  params: Promise<{ workspaceId: string }>;
}) {
  const { workspaceId } = use(params);
  const numericWorkspaceId = useMemo(() => Number(workspaceId), [workspaceId]);
  const [state, setState] = useState<WorkspaceDetailState>({
    status: "loading",
    workspace: null,
    error: null,
  });

  const reloadWorkspace = useCallback(async () => {
    setState({ status: "loading", workspace: null, error: null });

    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setState({
        status: "error",
        workspace: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    try {
      const [workspace, members] = await Promise.all([
        fetchWorkspaceDetail(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId),
        fetchWorkspaceMembers(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId),
      ]);
      setState({ status: "ready", workspace, members, error: null });
    } catch (error) {
      setState(toFailureState(error, "워크스페이스 정보를 확인하지 못했습니다."));
    }
  }, [numericWorkspaceId]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialWorkspace() {
      if (!isValidWorkspaceId(numericWorkspaceId)) {
        if (!ignore) {
          setState({
            status: "error",
            workspace: null,
            error: "올바르지 않은 워크스페이스입니다.",
          });
        }
        return;
      }

      try {
        const [workspace, members] = await Promise.all([
          fetchWorkspaceDetail(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId),
          fetchWorkspaceMembers(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId),
        ]);
        if (!ignore) {
          setState({ status: "ready", workspace, members, error: null });
        }
      } catch (error) {
        if (!ignore) {
          setState(toFailureState(error, "워크스페이스 정보를 확인하지 못했습니다."));
        }
      }
    }

    void loadInitialWorkspace();

    return () => {
      ignore = true;
    };
  }, [numericWorkspaceId]);

  const workspace = state.workspace;
  const members = state.status === "ready" ? state.members : [];
  const adminCount = members.filter((member) => member.role === "ADMIN").length;

  return (
    <main className="workspace-detail-shell">
      <section className="workspace-detail-panel" aria-label="워크스페이스 상세">
        <header className="workspace-detail-header">
          <Link className="secondary-action workspace-detail-back" href="/">
            <ArrowLeft aria-hidden="true" />
            <span>목록으로</span>
          </Link>
          <div className="workspace-detail-title">
            <div className="workspace-detail-icon">
              <Building2 aria-hidden="true" />
            </div>
            <div>
              <p className="eyebrow">Workspace</p>
              <h1>{workspace?.name ?? "워크스페이스 상세"}</h1>
            </div>
          </div>
        </header>

        {state.status === "loading" ? (
          <WorkspaceDetailStateMessage
            tone="neutral"
            icon={<Loader2 className="spin" aria-hidden="true" />}
            title="상세 확인 중"
            message="워크스페이스 정보를 불러오고 있습니다."
          />
        ) : null}

        {state.status === "error" || state.status === "offline" ? (
          <WorkspaceDetailStateMessage
            tone={state.status === "offline" ? "offline" : "danger"}
            icon={
              state.status === "offline" ? (
                <WifiOff aria-hidden="true" />
              ) : (
                <AlertCircle aria-hidden="true" />
              )
            }
            title={state.status === "offline" ? "연결 실패" : "상세 확인 실패"}
            message={state.error}
            action={
              <button
                className="secondary-action workspace-detail-retry"
                type="button"
                onClick={() => void reloadWorkspace()}
              >
                <RefreshCw aria-hidden="true" />
                <span>다시 시도</span>
              </button>
            }
          />
        ) : null}

        {workspace ? (
          <section className="workspace-detail-content" aria-label="워크스페이스 정보">
            <div className="workspace-detail-summary">
              <span className="workspace-detail-label">워크스페이스 이름</span>
              <strong>{workspace.name}</strong>
              <p>{workspace.description ?? "설명 없음"}</p>
            </div>

            <dl className="workspace-detail-meta">
              <DetailMetaItem
                icon={<ShieldCheck aria-hidden="true" />}
                label="내 역할"
                value={<span className="role-badge">{formatRole(workspace.myRole)}</span>}
              />
              <DetailMetaItem
                icon={<UserRound aria-hidden="true" />}
                label="생성자"
                value={workspace.creatorGoogleSub}
              />
              <DetailMetaItem
                icon={<CalendarDays aria-hidden="true" />}
                label="참여 일시"
                value={formatDateTime(workspace.joinedAt)}
              />
              <DetailMetaItem
                icon={<Hash aria-hidden="true" />}
                label="Workspace ID"
                value={workspace.workspaceId}
              />
            </dl>

            <section className="workspace-member-section" aria-label="워크스페이스 멤버">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Members</span>
                  <h2>멤버 목록</h2>
                </div>
                <span className="workspace-count-badge">{members.length}명</span>
              </div>

              {members.length > 0 ? (
                <div className="workspace-member-list">
                  {members.map((member) => (
                    <article className="workspace-member-row" key={member.memberId}>
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

      <aside className="workspace-detail-side" aria-label="조회 정보">
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
          <strong>{members.length}명</strong>
          <span>멤버 {members.length}명 · 관리자 {adminCount}명</span>
        </div>
      </aside>
    </main>
  );
}

function WorkspaceDetailStateMessage({
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

function DetailMetaItem({
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

function toFailureState(error: unknown, fallback: string): WorkspaceDetailState {
  if (error instanceof ApiError) {
    return {
      status: "error",
      workspace: null,
      error: error.message,
    };
  }

  return {
    status: "offline",
    workspace: null,
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
