"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { use, useEffect, useState, type FormEvent, type ReactNode } from "react";
import {
  AlertCircle,
  CheckCircle2,
  Clipboard,
  Info,
  Mail,
  RefreshCw,
  Save,
  Trash2,
  UserPlus,
  Users,
} from "lucide-react";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { Button, Input, Modal } from "@/components/ui";
import { formatWorkspaceRole } from "@/components/workspace/workspace-ui-helpers";
import { formatTokenUsageSummary } from "@/components/workspace/workspace-ui-helpers";
import {
  WorkspaceErrorState,
  WorkspaceLoadingState,
} from "@/components/workspace/WorkspaceShell";
import { getStoredUser, type ApiError } from "@/lib/api-client";
import {
  createInviteLink,
  deleteInvite,
  extendInvite,
  listSentInvites,
  type InviteManagement,
  type InviteStatus,
} from "@/lib/api/invites";
import {
  changeMemberRole,
  deleteWorkspace,
  getWorkspace,
  listWorkspaceMembers,
  removeMember,
  updateWorkspace,
  type WorkspaceDetail,
  type WorkspaceMember,
  type WorkspaceRole,
} from "@/lib/api/workspaces";
import { hydrateDesktopAccessToken } from "@/lib/auth";
import { useCopyFeedback } from "@/lib/use-copy-feedback";

type LoadState =
  | { kind: "loading" }
  | { kind: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[] }
  | { kind: "error"; message: string };

type SettingsSection = "workspace" | "members" | "invite" | "invites";
type InviteCreateMode = "email" | "link";

const INVITE_STATUS_LABEL: Record<InviteStatus, string> = {
  PENDING: "대기 중",
  ACCEPTED: "수락됨",
  EXPIRED: "만료됨",
  REVOKED: "폐기됨",
};

const EMAIL_STATUS_LABEL: Record<string, string> = {
  NOT_REQUESTED: "링크 전용",
  PENDING: "발송 대기",
  SENT: "발송됨",
  FAILED: "발송 실패",
};

export default function WorkspaceSettingsPage({
  params,
}: {
  params: Promise<{ workspaceId: string }>;
}) {
  const { workspaceId } = use(params);
  const workspaceIdNumber = Number(workspaceId);
  const router = useRouter();

  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [activeSection, setActiveSection] = useState<SettingsSection>("workspace");

  const [nameInput, setNameInput] = useState("");
  const [descriptionInput, setDescriptionInput] = useState("");
  const [dailyTokenLimitInput, setDailyTokenLimitInput] = useState("");
  const [updateBusy, setUpdateBusy] = useState(false);
  const [updateError, setUpdateError] = useState("");
  const [updateSuccess, setUpdateSuccess] = useState("");

  const [removingMemberId, setRemovingMemberId] = useState<number | null>(null);
  const [changingRoleMemberId, setChangingRoleMemberId] = useState<number | null>(null);
  const [memberError, setMemberError] = useState("");

  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<WorkspaceRole>("MEMBER");
  const [inviteExpiresInDays, setInviteExpiresInDays] = useState(7);
  const [inviteCreateMode, setInviteCreateMode] = useState<InviteCreateMode>("email");
  const [inviteBusy, setInviteBusy] = useState(false);
  const [inviteError, setInviteError] = useState("");
  const [createdInviteUrl, setCreatedInviteUrl] = useState("");
  const { copiedKey, copy: copyToClipboard } = useCopyFeedback<string | number>();
  const createdInviteCopied = copiedKey === "created-invite";

  const [inviteStatus, setInviteStatus] = useState<InviteStatus | "ALL">("PENDING");
  const [invites, setInvites] = useState<InviteManagement[]>([]);
  const [invitesLoading, setInvitesLoading] = useState(false);
  const [invitesError, setInvitesError] = useState("");
  const [inviteActionId, setInviteActionId] = useState<number | null>(null);
  const copiedInviteId = typeof copiedKey === "number" ? copiedKey : null;

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  useEffect(() => {
    let cancelled = false;
    void hydrateDesktopAccessToken().then(async () => {
      if (cancelled) return;
      if (!getStoredUser()) {
        router.replace("/");
        return;
      }
      try {
        const [workspace, members, inviteList] = await Promise.all([
          getWorkspace(workspaceIdNumber),
          listWorkspaceMembers(workspaceIdNumber),
          listSentInvites(workspaceIdNumber, "PENDING").catch(() => []),
        ]);
        if (cancelled) return;
        setNameInput(workspace.name);
        setDescriptionInput(workspace.description ?? "");
        setDailyTokenLimitInput(
          workspace.dailyTokenLimit && workspace.dailyTokenLimit > 0
            ? String(workspace.dailyTokenLimit)
            : "",
        );
        setInvites(inviteList);
        setState({ kind: "ready", workspace, members });
      } catch (err) {
        const apiErr = err as ApiError;
        if (!cancelled) {
          setState({
            kind: "error",
            message: apiErr?.message ?? "워크스페이스 설정을 불러오지 못했습니다.",
          });
        }
      }
    });

    return () => {
      cancelled = true;
    };
  }, [router, workspaceIdNumber]);

  if (state.kind === "loading") {
    return (
      <WorkspaceLoadingState
        title="워크스페이스 설정 불러오는 중"
        message="설정과 구성원 정보를 확인하고 있습니다."
        backHref={`/workspaces/${workspaceIdNumber}`}
        backLabel="워크스페이스로 돌아가기"
      />
    );
  }

  if (state.kind === "error") {
    return (
      <WorkspaceErrorState
        title="설정을 불러올 수 없음"
        message={state.message}
        backHref={`/workspaces/${workspaceIdNumber}`}
        backLabel="워크스페이스로 돌아가기"
      />
    );
  }

  const isAdmin = state.workspace.myRole === "ADMIN";
  const adminCount = state.members.filter((member) => member.role === "ADMIN").length;
  const tokenSummary = formatTokenUsageSummary(
    state.workspace.todayUsedTokens,
    state.workspace.dailyTokenLimit,
  );

  async function refreshInvites(status = inviteStatus) {
    setInvitesLoading(true);
    setInvitesError("");
    try {
      const nextInvites = await listSentInvites(
        workspaceIdNumber,
        status === "ALL" ? undefined : status,
      );
      setInvites(nextInvites);
    } catch (err) {
      const apiErr = err as ApiError;
      setInvitesError(apiErr?.message ?? "초대 목록을 불러오지 못했습니다.");
    } finally {
      setInvitesLoading(false);
    }
  }

  async function handleUpdateWorkspace(event: FormEvent) {
    event.preventDefault();
    if (!isAdmin) return;
    const name = nameInput.trim();
    if (!name) {
      setUpdateError("워크스페이스 이름을 입력하세요.");
      return;
    }

    const rawLimit = dailyTokenLimitInput.trim();
    let dailyTokenLimit: number | null = null;
    if (rawLimit.length > 0) {
      const parsed = Number(rawLimit);
      if (!Number.isInteger(parsed) || parsed < 0 || parsed > 1_000_000_000) {
        setUpdateError("일일 토큰 예산 한도는 0 이상 1,000,000,000 이하의 정수여야 합니다.");
        return;
      }
      dailyTokenLimit = parsed === 0 ? null : parsed;
    }

    setUpdateBusy(true);
    setUpdateError("");
    setUpdateSuccess("");
    try {
      const updated = await updateWorkspace(workspaceIdNumber, {
        name,
        description: descriptionInput.trim(),
        dailyTokenLimit,
      });
      setState((prev) => (prev.kind === "ready" ? { ...prev, workspace: updated } : prev));
      setNameInput(updated.name);
      setDescriptionInput(updated.description ?? "");
      setDailyTokenLimitInput(
        updated.dailyTokenLimit && updated.dailyTokenLimit > 0
          ? String(updated.dailyTokenLimit)
          : "",
      );
      setUpdateSuccess("워크스페이스 설정을 저장했습니다.");
    } catch (err) {
      const apiErr = err as ApiError;
      setUpdateError(apiErr?.message ?? "워크스페이스 정보 변경에 실패했습니다.");
    } finally {
      setUpdateBusy(false);
    }
  }

  async function handleChangeRole(memberId: number, nextRole: WorkspaceRole) {
    if (!isAdmin) return;
    setChangingRoleMemberId(memberId);
    setMemberError("");
    try {
      await changeMemberRole(workspaceIdNumber, memberId, nextRole);
      setState((prev) =>
        prev.kind === "ready"
          ? {
              ...prev,
              members: prev.members.map((member) =>
                member.memberId === memberId ? { ...member, role: nextRole } : member,
              ),
            }
          : prev,
      );
    } catch (err) {
      const apiErr = err as ApiError;
      setMemberError(apiErr?.message ?? "구성원 역할 변경에 실패했습니다.");
    } finally {
      setChangingRoleMemberId(null);
    }
  }

  async function handleRemoveMember(memberId: number) {
    if (!isAdmin) return;
    setRemovingMemberId(memberId);
    setMemberError("");
    try {
      await removeMember(workspaceIdNumber, memberId);
      setState((prev) =>
        prev.kind === "ready"
          ? {
              ...prev,
              members: prev.members.filter((member) => member.memberId !== memberId),
            }
          : prev,
      );
    } catch (err) {
      const apiErr = err as ApiError;
      setMemberError(apiErr?.message ?? "구성원 삭제에 실패했습니다.");
    } finally {
      setRemovingMemberId(null);
    }
  }

  async function handleCreateInvite(event: FormEvent) {
    event.preventDefault();
    if (!isAdmin) return;
    setInviteBusy(true);
    setInviteError("");
    setCreatedInviteUrl("");
    try {
      const targetEmail = inviteEmail.trim();
      if (inviteCreateMode === "email" && !targetEmail) {
        setInviteError("초대할 이메일 주소를 입력하세요.");
        return;
      }
      const invite = await createInviteLink(workspaceIdNumber, {
        expiresInDays: inviteExpiresInDays,
        role: inviteRole,
        ...(inviteCreateMode === "email" ? { targetEmail } : {}),
      });
      const url = `${window.location.origin}/invites/${invite.token}`;
      setCreatedInviteUrl(url);
      await copyToClipboard(url, "created-invite");
      await refreshInvites();
    } catch (err) {
      const apiErr = err as ApiError;
      setInviteError(apiErr?.message ?? "초대 링크 생성에 실패했습니다.");
    } finally {
      setInviteBusy(false);
    }
  }

  async function handleExtendInvite(inviteId: number) {
    setInviteActionId(inviteId);
    setInvitesError("");
    try {
      const updated = await extendInvite(workspaceIdNumber, inviteId, 7);
      setInvites((prev) => prev.map((invite) => (invite.inviteId === inviteId ? updated : invite)));
    } catch (err) {
      const apiErr = err as ApiError;
      setInvitesError(apiErr?.message ?? "초대 만료일 연장에 실패했습니다.");
    } finally {
      setInviteActionId(null);
    }
  }

  async function handleDeleteInvite(inviteId: number) {
    setInviteActionId(inviteId);
    setInvitesError("");
    try {
      await deleteInvite(workspaceIdNumber, inviteId);
      setInvites((prev) => prev.filter((invite) => invite.inviteId !== inviteId));
    } catch (err) {
      const apiErr = err as ApiError;
      setInvitesError(apiErr?.message ?? "초대 삭제에 실패했습니다.");
    } finally {
      setInviteActionId(null);
    }
  }

  async function handleCopyInvite(invite: InviteManagement) {
    const url = invite.inviteUrl || `${window.location.origin}/invites/${invite.token}`;
    await copyToClipboard(url, invite.inviteId);
  }

  async function handleDeleteWorkspace() {
    if (!isAdmin) return;
    setDeleteBusy(true);
    setDeleteError("");
    try {
      await deleteWorkspace(workspaceIdNumber);
      router.replace("/workspaces");
    } catch (err) {
      const apiErr = err as ApiError;
      setDeleteError(apiErr?.message ?? "워크스페이스 삭제에 실패했습니다.");
    } finally {
      setDeleteBusy(false);
    }
  }

  return (
    <AppTopChrome
      title={`${state.workspace.name} · 설정`}
      backHref={`/workspaces/${workspaceIdNumber}`}
      backLabel="워크스페이스로 돌아가기"
    >
      <div className="ao-page-container">
        <header className="ao-page-header">
          <div>
            <p className="ao-page-eyebrow">역할 · {formatWorkspaceRole(state.workspace.myRole)}</p>
            <h1 className="ao-page-title">{state.workspace.name} 설정</h1>
            <p className="ao-page-subtitle">
              워크스페이스 기본 정보, 일일 토큰 예산 한도, 구성원 권한 및 초대를 관리합니다.
            </p>
          </div>
          <Link href={`/workspaces/${workspaceIdNumber}`} className="secondary-action">
            <span>오피스로 돌아가기</span>
          </Link>
        </header>

        <div className="grid gap-5 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className="flex flex-col gap-3">
            <section className="ao-surface-panel p-3 flex flex-col gap-1" aria-label="설정 메뉴">
              <SectionNavButton
                active={activeSection === "workspace"}
                icon={<Info size={14} aria-hidden="true" />}
                label="워크스페이스 정보"
                onClick={() => setActiveSection("workspace")}
              />
              <SectionNavButton
                active={activeSection === "members"}
                icon={<Users size={14} aria-hidden="true" />}
                label={`구성원 관리 (${state.members.length})`}
                onClick={() => setActiveSection("members")}
              />
              <SectionNavButton
                active={activeSection === "invite"}
                icon={<UserPlus size={14} aria-hidden="true" />}
                label="초대 생성"
                onClick={() => setActiveSection("invite")}
              />
              <SectionNavButton
                active={activeSection === "invites"}
                icon={<Mail size={14} aria-hidden="true" />}
                label={`초대 이력 (${invites.length})`}
                onClick={() => {
                  setActiveSection("invites");
                  void refreshInvites(inviteStatus);
                }}
              />
            </section>
          </aside>

          <div className="flex flex-col gap-4 min-w-0">
            {activeSection === "workspace" ? (
              <>
                <section className="ao-surface-panel" aria-label="워크스페이스 정보 수정">
                  <div className="ao-surface-panel-header">
                    <h2>기본 정보 및 일일 토큰 예산</h2>
                    <span className="role-badge">오늘 사용: {tokenSummary.label}</span>
                  </div>

                  <form className="flex flex-col gap-4" onSubmit={handleUpdateWorkspace}>
                    <label className="form-field">
                      <span>
                        <strong>워크스페이스 이름</strong>
                        <small>{nameInput.length}/100</small>
                      </span>
                      <Input
                        value={nameInput}
                        maxLength={100}
                        disabled={!isAdmin || updateBusy}
                        onChange={(event) => setNameInput(event.target.value)}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>설명</strong>
                        <small>{descriptionInput.length}/500</small>
                      </span>
                      <textarea
                        rows={3}
                        maxLength={500}
                        disabled={!isAdmin || updateBusy}
                        value={descriptionInput}
                        onChange={(event) => setDescriptionInput(event.target.value)}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>일일 토큰 예산 한도</strong>
                        <small>비우거나 0 입력 시 무제한</small>
                      </span>
                      <Input
                        type="number"
                        min={0}
                        max={1_000_000_000}
                        step={1000}
                        aria-label="일일 토큰 예산 한도"
                        placeholder="무제한 (예: 250000)"
                        disabled={!isAdmin || updateBusy}
                        value={dailyTokenLimitInput}
                        onChange={(event) => setDailyTokenLimitInput(event.target.value)}
                      />
                    </label>

                    {updateError ? (
                      <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                        <AlertCircle size={15} aria-hidden="true" />
                        <span>{updateError}</span>
                      </div>
                    ) : null}

                    {updateSuccess ? (
                      <div className="ao-inline-notice ao-inline-notice-success" role="status">
                        <CheckCircle2 size={15} aria-hidden="true" />
                        <span>{updateSuccess}</span>
                      </div>
                    ) : null}

                    <div className="flex justify-end">
                      <Button type="submit" loading={updateBusy} disabled={!isAdmin}>
                        <Save size={14} aria-hidden="true" />
                        <span>저장</span>
                      </Button>
                    </div>
                  </form>
                </section>

                <section
                  className="ao-surface-panel border-[var(--status-danger-border)]"
                  aria-label="워크스페이스 삭제"
                >
                  <div className="ao-surface-panel-header">
                    <h2 className="text-[var(--status-danger)]">위험 구역</h2>
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <strong className="block text-[14px] text-[var(--ui-text-strong)]">
                        워크스페이스 삭제
                      </strong>
                      <p className="m-0 mt-1 text-[13px] text-[var(--ui-text-secondary)]">
                        워크스페이스와 연결된 에이전트, 작업, 산출물 기록이 영구 삭제됩니다.
                      </p>
                    </div>
                    <Button
                      type="button"
                      variant="danger"
                      disabled={!isAdmin}
                      onClick={() => setDeleteOpen(true)}
                    >
                      <Trash2 size={14} aria-hidden="true" />
                      <span>워크스페이스 삭제</span>
                    </Button>
                  </div>
                </section>
              </>
            ) : null}

            {activeSection === "members" ? (
              <section className="ao-surface-panel" aria-label="구성원 목록">
                <div className="ao-surface-panel-header">
                  <h2>구성원 ({state.members.length})</h2>
                </div>

                {memberError ? (
                  <div className="ao-inline-notice ao-inline-notice-danger mb-3" role="alert">
                    <AlertCircle size={15} aria-hidden="true" />
                    <span>{memberError}</span>
                  </div>
                ) : null}

                <div className="flex flex-col gap-2">
                  {state.members.map((member) => {
                    const lastAdmin = member.role === "ADMIN" && adminCount <= 1;
                    return (
                      <article
                        key={member.memberId}
                        className="p-3 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] flex flex-wrap items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <strong className="text-[14px] text-[var(--ui-text-strong)]">
                              {member.name}
                            </strong>
                            <span className="role-badge">{formatWorkspaceRole(member.role)}</span>
                          </div>
                          <p className="m-0 mt-0.5 text-[12px] text-[var(--ui-text-muted)]">
                            {member.email} · 합류일 {formatDate(member.joinedAt)}
                          </p>
                        </div>

                        <div className="flex items-center gap-2">
                          <select
                            aria-label={`${member.name} 역할`}
                            disabled={!isAdmin || lastAdmin || changingRoleMemberId === member.memberId}
                            value={member.role}
                            onChange={(event) =>
                              void handleChangeRole(
                                member.memberId,
                                event.target.value as WorkspaceRole,
                              )
                            }
                            className="h-8 px-2 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-bg)] text-[12px] text-[var(--ui-text)]"
                          >
                            <option value="ADMIN">관리자</option>
                            <option value="MEMBER">구성원</option>
                          </select>

                          <Button
                            type="button"
                            variant="danger"
                            size="sm"
                            loading={removingMemberId === member.memberId}
                            disabled={!isAdmin || lastAdmin}
                            onClick={() => void handleRemoveMember(member.memberId)}
                          >
                            삭제
                          </Button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              </section>
            ) : null}

            {activeSection === "invite" ? (
              <section className="ao-surface-panel" aria-label="새 초대 생성">
                <div className="ao-surface-panel-header">
                  <h2>새 구성원 초대</h2>
                </div>

                <div className="flex items-center gap-2 mb-4">
                  <button
                    type="button"
                    className={`tb-btn${inviteCreateMode === "email" ? " tb-btn-primary" : ""}`}
                    onClick={() => setInviteCreateMode("email")}
                  >
                    이메일 초대
                  </button>
                  <button
                    type="button"
                    className={`tb-btn${inviteCreateMode === "link" ? " tb-btn-primary" : ""}`}
                    onClick={() => setInviteCreateMode("link")}
                  >
                    공유 링크 생성
                  </button>
                </div>

                <form className="flex flex-col gap-4" onSubmit={handleCreateInvite}>
                  {inviteCreateMode === "email" ? (
                    <label className="form-field">
                      <span>
                        <strong>초대할 이메일</strong>
                        <small>필수</small>
                      </span>
                      <Input
                        type="email"
                        placeholder="member@company.com"
                        disabled={!isAdmin || inviteBusy}
                        value={inviteEmail}
                        onChange={(event) => setInviteEmail(event.target.value)}
                      />
                    </label>
                  ) : null}

                  <div className="workspace-gateway-form-grid">
                    <label className="form-field">
                      <span>
                        <strong>부여할 역할</strong>
                      </span>
                      <select
                        disabled={!isAdmin || inviteBusy}
                        value={inviteRole}
                        onChange={(event) => setInviteRole(event.target.value as WorkspaceRole)}
                      >
                        <option value="MEMBER">구성원</option>
                        <option value="ADMIN">관리자</option>
                      </select>
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>유효 기간</strong>
                      </span>
                      <select
                        disabled={!isAdmin || inviteBusy}
                        value={inviteExpiresInDays}
                        onChange={(event) => setInviteExpiresInDays(Number(event.target.value))}
                      >
                        <option value={1}>1일</option>
                        <option value={7}>7일</option>
                        <option value={14}>14일</option>
                        <option value={30}>30일</option>
                      </select>
                    </label>
                  </div>

                  {inviteError ? (
                    <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                      <AlertCircle size={15} aria-hidden="true" />
                      <span>{inviteError}</span>
                    </div>
                  ) : null}

                  {createdInviteUrl ? (
                    <div className="p-3 rounded-[var(--radius-md)] border border-[var(--status-success-border)] bg-[var(--status-success-bg)] flex flex-col gap-2">
                      <strong className="text-[13px] text-[var(--status-success)]">
                        초대 링크가 생성되었습니다
                        {createdInviteCopied ? " (클립보드에 복사됨)" : ""}
                      </strong>
                      <code className="text-[12px] break-all font-mono">{createdInviteUrl}</code>
                    </div>
                  ) : null}

                  <div className="flex justify-end">
                    <Button type="submit" loading={inviteBusy} disabled={!isAdmin}>
                      <UserPlus size={14} aria-hidden="true" />
                      <span>{inviteCreateMode === "email" ? "초대 발송" : "초대 링크 생성"}</span>
                    </Button>
                  </div>
                </form>
              </section>
            ) : null}

            {activeSection === "invites" ? (
              <section className="ao-surface-panel" aria-label="초대 이력">
                <div className="ao-surface-panel-header">
                  <h2>초대 이력 ({invites.length})</h2>
                  <div className="flex items-center gap-2">
                    <select
                      aria-label="초대 상태 필터"
                      value={inviteStatus}
                      onChange={(event) => {
                        const next = event.target.value as InviteStatus | "ALL";
                        setInviteStatus(next);
                        void refreshInvites(next);
                      }}
                      className="h-8 px-2 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-bg)] text-[12px] text-[var(--ui-text)]"
                    >
                      <option value="PENDING">대기 중</option>
                      <option value="ACCEPTED">수락됨</option>
                      <option value="EXPIRED">만료됨</option>
                      <option value="REVOKED">폐기됨</option>
                      <option value="ALL">전체</option>
                    </select>
                    <button
                      type="button"
                      className="tb-btn"
                      onClick={() => void refreshInvites(inviteStatus)}
                      disabled={invitesLoading}
                    >
                      <RefreshCw
                        size={13}
                        className={invitesLoading ? "spin" : undefined}
                        aria-hidden="true"
                      />
                      <span>새로고침</span>
                    </button>
                  </div>
                </div>

                {invitesError ? (
                  <div className="ao-inline-notice ao-inline-notice-danger mb-3" role="alert">
                    <AlertCircle size={15} aria-hidden="true" />
                    <span>{invitesError}</span>
                  </div>
                ) : null}

                {invites.length === 0 ? (
                  <p className="m-0 py-6 text-center text-[13px] text-[var(--ui-text-muted)]">
                    조건에 맞는 초대 이력이 없습니다.
                  </p>
                ) : (
                  <div className="flex flex-col gap-2">
                    {invites.map((invite) => {
                      const inviteUrl =
                        invite.inviteUrl || `${window.location.origin}/invites/${invite.token}`;
                      const busy = inviteActionId === invite.inviteId;
                      return (
                        <article
                          key={invite.inviteId}
                          className="p-3 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] flex flex-wrap items-start justify-between gap-3"
                        >
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2 mb-1">
                              <span className="role-badge">
                                {INVITE_STATUS_LABEL[invite.status]}
                              </span>
                              <span className="role-badge">{invite.role}</span>
                              <span className="role-badge">
                                {EMAIL_STATUS_LABEL[invite.emailStatus] ?? invite.emailStatus}
                              </span>
                            </div>
                            <p className="m-0 text-[12px] font-mono break-all text-[var(--ui-text-strong)]">
                              {inviteUrl}
                            </p>
                            <p className="m-0 mt-1 text-[12px] text-[var(--ui-text-muted)]">
                              {invite.targetEmail ?? "공유 링크"} · 만료{" "}
                              {formatDate(invite.expiresAt)}
                            </p>
                          </div>

                          <div className="flex flex-wrap gap-1.5 shrink-0">
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => void handleCopyInvite(invite)}
                            >
                              <Clipboard size={13} aria-hidden="true" />
                              <span>{copiedInviteId === invite.inviteId ? "복사됨" : "복사"}</span>
                            </Button>
                            <Button
                              type="button"
                              variant="secondary"
                              size="sm"
                              loading={busy}
                              disabled={!isAdmin || invite.status !== "PENDING"}
                              onClick={() => void handleExtendInvite(invite.inviteId)}
                            >
                              7일 연장
                            </Button>
                            <Button
                              type="button"
                              variant="danger"
                              size="sm"
                              loading={busy}
                              disabled={!isAdmin || invite.status === "ACCEPTED"}
                              onClick={() => void handleDeleteInvite(invite.inviteId)}
                            >
                              <Trash2 size={13} aria-hidden="true" />
                              <span>삭제</span>
                            </Button>
                          </div>
                        </article>
                      );
                    })}
                  </div>
                )}
              </section>
            ) : null}
          </div>
        </div>

        <Modal
          open={deleteOpen}
          disableEscapeClose={deleteBusy}
          onClose={() => !deleteBusy && setDeleteOpen(false)}
          title="워크스페이스 영구 삭제"
        >
          <p className="m-0 text-[13px] text-[var(--ui-text)]">
            <strong className="text-[var(--status-danger)]">{state.workspace.name}</strong>{" "}
            워크스페이스를 영구 삭제합니다. 이 작업은 되돌릴 수 없습니다.
          </p>
          {deleteError ? (
            <div className="ao-inline-notice ao-inline-notice-danger mt-3" role="alert">
              <AlertCircle size={15} aria-hidden="true" />
              <span>{deleteError}</span>
            </div>
          ) : null}
          <Modal.Footer>
            <Button
              type="button"
              variant="secondary"
              disabled={deleteBusy}
              onClick={() => setDeleteOpen(false)}
            >
              취소
            </Button>
            <Button
              type="button"
              variant="danger"
              loading={deleteBusy}
              onClick={() => void handleDeleteWorkspace()}
            >
              삭제 확인
            </Button>
          </Modal.Footer>
        </Modal>
      </div>
    </AppTopChrome>
  );
}

function SectionNavButton({
  active,
  icon,
  label,
  onClick,
}: {
  active: boolean;
  icon: ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`chrome-sidebar-item${active ? " chrome-sidebar-item-active" : ""}`}
    >
      <span className="inline-flex items-center gap-2">
        {icon}
        <span>{label}</span>
      </span>
    </button>
  );
}

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  } catch {
    return iso;
  }
}
