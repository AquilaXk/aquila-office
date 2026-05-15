"use client";

import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarDays,
  CheckCircle2,
  Hash,
  Loader2,
  Mail,
  RefreshCw,
  Save,
  ShieldCheck,
  Trash2,
  UserRound,
  Users,
  WifiOff,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  use,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { ApiError } from "@/lib/api-client";
import {
  fetchWorkspaceInvites,
  revokeWorkspaceInvite,
  type WorkspaceInviteManagement,
  type WorkspaceInviteManagementStatus,
} from "@/lib/invites";
import {
  changeWorkspaceMemberRole,
  deleteWorkspace,
  fetchWorkspaceDetail,
  fetchWorkspaceMembers,
  removeWorkspaceMember,
  updateWorkspace,
  type WorkspaceDetail,
  type WorkspaceMember,
  type WorkspaceMemberRole,
} from "@/lib/workspaces";

const TEMP_MEMBER_GOOGLE_SUB =
  process.env.NEXT_PUBLIC_DEV_MEMBER_GOOGLE_SUB ?? "google-1";
const NAME_MAX_LENGTH = 100;
const DESCRIPTION_MAX_LENGTH = 500;

type WorkspaceSettingsState =
  | { status: "loading"; workspace: null; members: []; error: null }
  | { status: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[]; error: null }
  | { status: "error" | "offline"; workspace: null; members: []; error: string };

type MutationState =
  | { status: "idle"; message: null }
  | { status: "submitting"; message: null }
  | { status: "success"; message: string }
  | { status: "error"; message: string };

type MemberAction =
  | { type: "role"; memberId: number }
  | { type: "remove"; memberId: number };

type InviteLogState =
  | { status: "idle"; invites: []; error: null }
  | { status: "loading"; invites: []; error: null }
  | { status: "ready"; invites: WorkspaceInviteManagement[]; error: null }
  | { status: "error"; invites: []; error: string };

type InviteStatusFilter = WorkspaceInviteManagementStatus | "ALL";

const INVITE_STATUS_FILTERS: { value: InviteStatusFilter; label: string }[] = [
  { value: "PENDING", label: "대기" },
  { value: "ACCEPTED", label: "수락" },
  { value: "EXPIRED", label: "만료" },
  { value: "REVOKED", label: "취소" },
  { value: "ALL", label: "전체" },
];

export default function WorkspaceSettingsPage({
  params,
}: {
  params: Promise<{ workspaceId: string }>;
}) {
  const { workspaceId } = use(params);
  const numericWorkspaceId = useMemo(() => Number(workspaceId), [workspaceId]);
  const router = useRouter();
  const [state, setState] = useState<WorkspaceSettingsState>({
    status: "loading",
    workspace: null,
    members: [],
    error: null,
  });
  const [nameInput, setNameInput] = useState("");
  const [descriptionInput, setDescriptionInput] = useState("");
  const [updateState, setUpdateState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [deleteState, setDeleteState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [memberAction, setMemberAction] = useState<MemberAction | null>(null);
  const [memberState, setMemberState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [inviteLogState, setInviteLogState] = useState<InviteLogState>({
    status: "idle",
    invites: [],
    error: null,
  });
  const [inviteFilter, setInviteFilter] = useState<InviteStatusFilter>("PENDING");
  const [inviteActionId, setInviteActionId] = useState<number | null>(null);
  const [inviteActionState, setInviteActionState] = useState<MutationState>({
    status: "idle",
    message: null,
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

  const fetchInviteLog = useCallback(
    async (status: WorkspaceInviteManagementStatus | undefined = "PENDING") => {
      try {
        const invites = await fetchWorkspaceInvites(
          TEMP_MEMBER_GOOGLE_SUB,
          numericWorkspaceId,
          status,
        );
        return { status: "ready", invites, error: null } satisfies InviteLogState;
      } catch (error) {
        return {
          status: "error",
          invites: [],
          error: messageFromError(error, "초대 목록을 확인하지 못했습니다."),
        } satisfies InviteLogState;
      }
    },
    [numericWorkspaceId],
  );

  const reloadSettings = useCallback(async () => {
    setState({ status: "loading", workspace: null, members: [], error: null });
    const nextState = await fetchSettings();
    setState(nextState);
    if (nextState.status === "ready") {
      setNameInput(nextState.workspace.name);
      setDescriptionInput(nextState.workspace.description ?? "");
      setDeleteConfirmation("");
    }
  }, [fetchSettings]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialSettings() {
      const nextState = await fetchSettings();
      if (!ignore) {
        setState(nextState);
        if (nextState.status === "ready") {
          setNameInput(nextState.workspace.name);
          setDescriptionInput(nextState.workspace.description ?? "");
          setDeleteConfirmation("");
        }
      }
    }

    void loadInitialSettings();

    return () => {
      ignore = true;
    };
  }, [fetchSettings]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialInviteLog() {
      if (state.status !== "ready" || state.workspace.myRole !== "ADMIN") {
        return;
      }

      await Promise.resolve();
      if (!ignore) {
        setInviteLogState({ status: "loading", invites: [], error: null });
      }

      const nextState = await fetchInviteLog(toInviteStatusParam(inviteFilter));
      if (!ignore) {
        setInviteLogState(nextState);
      }
    }

    void loadInitialInviteLog();

    return () => {
      ignore = true;
    };
  }, [fetchInviteLog, inviteFilter, state]);

  const workspace = state.workspace;
  const members = state.status === "ready" ? state.members : [];
  const isAdmin = workspace?.myRole === "ADMIN";
  const adminCount = members.filter((member) => member.role === "ADMIN").length;
  const trimmedName = nameInput.trim();
  const trimmedDescription = descriptionInput.trim();
  const isUpdatingWorkspace = updateState.status === "submitting";
  const isDeletingWorkspace = deleteState.status === "submitting";
  const canManageWorkspace = state.status === "ready" && isAdmin;
  const canUpdateWorkspace =
    canManageWorkspace && trimmedName.length > 0 && !isUpdatingWorkspace;
  const canDeleteWorkspace =
    canManageWorkspace &&
    deleteConfirmation === workspace?.name &&
    !isDeletingWorkspace;
  const canManageMembers = state.status === "ready" && isAdmin;
  const isInviteLogLoading = inviteLogState.status === "loading";
  const hasInviteAction = inviteActionId !== null;

  const reloadInviteLog = async () => {
    if (!isAdmin) {
      return;
    }

    setInviteLogState({ status: "loading", invites: [], error: null });
    setInviteActionState({ status: "idle", message: null });
    setInviteLogState(await fetchInviteLog(toInviteStatusParam(inviteFilter)));
  };

  const handleRevokeInvite = async (invite: WorkspaceInviteManagement) => {
    if (!isAdmin || invite.status !== "PENDING" || hasInviteAction) {
      return;
    }

    setInviteActionId(invite.inviteId);
    setInviteActionState({ status: "submitting", message: null });
    try {
      await revokeWorkspaceInvite(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId, invite.inviteId);
      setInviteLogState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              invites: updateInviteLogAfterRevoke(previous.invites, invite.inviteId, inviteFilter),
            }
          : previous,
      );
      setInviteActionState({ status: "success", message: "초대가 취소되었습니다." });
    } catch (error) {
      setInviteActionState({
        status: "error",
        message: messageFromError(error, "초대를 취소하지 못했습니다."),
      });
    } finally {
      setInviteActionId(null);
    }
  };

  const handleUpdateWorkspace = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!canManageWorkspace || state.status !== "ready") {
      return;
    }

    if (!trimmedName) {
      setUpdateState({ status: "error", message: "워크스페이스 이름을 입력해 주세요." });
      return;
    }

    setUpdateState({ status: "submitting", message: null });
    try {
      const updated = await updateWorkspace(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId, {
        name: trimmedName,
        description: trimmedDescription || undefined,
      });
      setState({ ...state, workspace: updated });
      setNameInput(updated.name);
      setDescriptionInput(updated.description ?? "");
      setDeleteConfirmation("");
      setUpdateState({ status: "success", message: "워크스페이스 정보가 저장되었습니다." });
    } catch (error) {
      setUpdateState({
        status: "error",
        message: messageFromError(error, "워크스페이스 정보를 저장하지 못했습니다."),
      });
    }
  };

  const handleDeleteWorkspace = async () => {
    if (!canManageWorkspace || state.status !== "ready" || !workspace) {
      return;
    }

    if (deleteConfirmation !== workspace.name) {
      setDeleteState({ status: "error", message: "워크스페이스 이름을 정확히 입력해 주세요." });
      return;
    }

    setDeleteState({ status: "submitting", message: null });
    try {
      await deleteWorkspace(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId);
      router.replace("/");
    } catch (error) {
      setDeleteState({
        status: "error",
        message: messageFromError(error, "워크스페이스를 삭제하지 못했습니다."),
      });
    }
  };

  const handleChangeMemberRole = async (
    member: WorkspaceMember,
    nextRole: WorkspaceMemberRole,
  ) => {
    if (!canManageMembers || member.role === nextRole || isLastAdmin(member, adminCount, nextRole)) {
      return;
    }

    setMemberAction({ type: "role", memberId: member.memberId });
    setMemberState({ status: "submitting", message: null });
    try {
      await changeWorkspaceMemberRole(
        TEMP_MEMBER_GOOGLE_SUB,
        numericWorkspaceId,
        member.memberId,
        nextRole,
      );
      setState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              members: previous.members.map((currentMember) =>
                currentMember.memberId === member.memberId
                  ? { ...currentMember, role: nextRole }
                  : currentMember,
              ),
            }
          : previous,
      );
      setMemberState({ status: "success", message: "멤버 역할이 변경되었습니다." });
    } catch (error) {
      setMemberState({
        status: "error",
        message: messageFromError(error, "멤버 역할을 변경하지 못했습니다."),
      });
    } finally {
      setMemberAction(null);
    }
  };

  const handleRemoveMember = async (member: WorkspaceMember) => {
    if (!canManageMembers || isLastAdmin(member, adminCount)) {
      return;
    }

    setMemberAction({ type: "remove", memberId: member.memberId });
    setMemberState({ status: "submitting", message: null });
    try {
      await removeWorkspaceMember(TEMP_MEMBER_GOOGLE_SUB, numericWorkspaceId, member.memberId);
      setState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              members: previous.members.filter(
                (currentMember) => currentMember.memberId !== member.memberId,
              ),
            }
          : previous,
      );
      setMemberState({ status: "success", message: "멤버가 제거되었습니다." });
    } catch (error) {
      setMemberState({
        status: "error",
        message: messageFromError(error, "멤버를 제거하지 못했습니다."),
      });
    } finally {
      setMemberAction(null);
    }
  };

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

            <section className="workspace-settings-section" aria-label="워크스페이스 정보 수정">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Settings</span>
                  <h2>기본 정보</h2>
                </div>
                <span className={canManageWorkspace ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {canManageWorkspace ? "관리 가능" : "읽기 전용"}
                </span>
              </div>

              <form className="workspace-settings-form" onSubmit={handleUpdateWorkspace}>
                <label className="form-field">
                  <span>
                    <strong>이름</strong>
                    <small>{nameInput.length}/{NAME_MAX_LENGTH}</small>
                  </span>
                  <input
                    value={nameInput}
                    onChange={(event) => {
                      setNameInput(event.target.value);
                      setUpdateState({ status: "idle", message: null });
                    }}
                    maxLength={NAME_MAX_LENGTH}
                    disabled={!canManageWorkspace || isUpdatingWorkspace}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>설명</strong>
                    <small>{descriptionInput.length}/{DESCRIPTION_MAX_LENGTH}</small>
                  </span>
                  <textarea
                    value={descriptionInput}
                    onChange={(event) => {
                      setDescriptionInput(event.target.value);
                      setUpdateState({ status: "idle", message: null });
                    }}
                    maxLength={DESCRIPTION_MAX_LENGTH}
                    rows={4}
                    disabled={!canManageWorkspace || isUpdatingWorkspace}
                  />
                </label>

                {updateState.status === "error" ? (
                  <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {updateState.message}
                  </SettingsNotice>
                ) : null}

                {updateState.status === "success" ? (
                  <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                    {updateState.message}
                  </SettingsNotice>
                ) : null}

                <div className="workspace-settings-actions">
                  <button
                    className="primary-action primary-action-active"
                    type="submit"
                    disabled={!canUpdateWorkspace}
                  >
                    {isUpdatingWorkspace ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <Save aria-hidden="true" />
                    )}
                    <span>{isUpdatingWorkspace ? "저장 중" : "저장"}</span>
                  </button>
                </div>
              </form>
            </section>

            <section className="workspace-settings-section workspace-settings-danger" aria-label="워크스페이스 삭제">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Delete</span>
                  <h2>워크스페이스 삭제</h2>
                </div>
                <span className="workspace-lock-badge">ADMIN</span>
              </div>

              <div className="workspace-settings-form">
                <label className="form-field">
                  <span>
                    <strong>삭제 확인</strong>
                    <small>{workspace.name}</small>
                  </span>
                  <input
                    value={deleteConfirmation}
                    onChange={(event) => {
                      setDeleteConfirmation(event.target.value);
                      setDeleteState({ status: "idle", message: null });
                    }}
                    disabled={!canManageWorkspace || isDeletingWorkspace}
                  />
                </label>

                {deleteState.status === "error" ? (
                  <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {deleteState.message}
                  </SettingsNotice>
                ) : null}

                <div className="workspace-settings-actions">
                  <button
                    className="danger-action"
                    type="button"
                    onClick={() => void handleDeleteWorkspace()}
                    disabled={!canDeleteWorkspace}
                  >
                    {isDeletingWorkspace ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <Trash2 aria-hidden="true" />
                    )}
                    <span>{isDeletingWorkspace ? "삭제 중" : "삭제"}</span>
                  </button>
                </div>
              </div>
            </section>

            <section className="workspace-settings-section" aria-label="멤버 현황">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Members</span>
                  <h2>멤버 관리</h2>
                </div>
                <span className={canManageMembers ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {members.length}명
                </span>
              </div>

              {memberState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {memberState.message}
                </SettingsNotice>
              ) : null}

              {memberState.status === "success" ? (
                <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                  {memberState.message}
                </SettingsNotice>
              ) : null}

              {members.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {members.map((member) => {
                    const roleBusy =
                      memberAction?.type === "role" && memberAction.memberId === member.memberId;
                    const removeBusy =
                      memberAction?.type === "remove" && memberAction.memberId === member.memberId;
                    const lastAdmin = isLastAdmin(member, adminCount);

                    return (
                      <article className="workspace-settings-member-row" key={member.memberId}>
                        <div className="workspace-member-avatar">
                          <UserRound aria-hidden="true" />
                        </div>
                        <div className="workspace-member-main">
                          <strong>{member.name}</strong>
                          <p>{member.email}</p>
                          <span>{formatDateTime(member.joinedAt)}</span>
                        </div>
                        <div className="workspace-settings-member-actions">
                          <RoleSegmentedControl
                            role={member.role}
                            busy={roleBusy}
                            disabled={!canManageMembers || Boolean(memberAction)}
                            disableAdminDemotion={lastAdmin}
                            onChange={(nextRole) => void handleChangeMemberRole(member, nextRole)}
                          />
                          <button
                            className="danger-action workspace-member-remove"
                            type="button"
                            onClick={() => void handleRemoveMember(member)}
                            disabled={!canManageMembers || Boolean(memberAction) || lastAdmin}
                          >
                            {removeBusy ? (
                              <Loader2 className="spin" aria-hidden="true" />
                            ) : (
                              <Trash2 aria-hidden="true" />
                            )}
                            <span>{removeBusy ? "제거 중" : "제거"}</span>
                          </button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              ) : (
                <div className="workspace-member-empty">
                  <Users aria-hidden="true" />
                  <strong>표시할 멤버가 없습니다</strong>
                  <p>워크스페이스에 참여한 멤버가 생기면 이곳에 표시됩니다.</p>
                </div>
              )}
            </section>

            <section className="workspace-settings-section" aria-label="워크스페이스 초대 관리">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Invites</span>
                  <h2>초대 관리</h2>
                </div>
                <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {isAdmin ? "ADMIN" : "읽기 전용"}
                </span>
              </div>

              {!isAdmin ? (
                <p className="workspace-invite-help">
                  관리자 역할만 초대 목록을 확인할 수 있습니다.
                </p>
              ) : null}

              {isAdmin ? (
                <div className="workspace-invite-management-toolbar" aria-label="초대 상태 필터">
                  <div className="workspace-invite-filters">
                    {INVITE_STATUS_FILTERS.map((filter) => (
                      <button
                        className={
                          filter.value === inviteFilter
                            ? "workspace-invite-filter workspace-invite-filter-active"
                            : "workspace-invite-filter"
                        }
                        type="button"
                        key={filter.value}
                        onClick={() => setInviteFilter(filter.value)}
                        disabled={isInviteLogLoading}
                      >
                        {filter.label}
                      </button>
                    ))}
                  </div>
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={() => void reloadInviteLog()}
                    disabled={isInviteLogLoading}
                  >
                    <RefreshCw
                      className={isInviteLogLoading ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>새로고침</span>
                  </button>
                </div>
              ) : null}

              {isAdmin && inviteActionState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {inviteActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin && inviteActionState.status === "success" ? (
                <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                  {inviteActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin && inviteLogState.status === "loading" ? (
                <div className="workspace-member-empty">
                  <Loader2 className="spin" aria-hidden="true" />
                  <strong>초대 목록 확인 중</strong>
                  <p>워크스페이스 초대 목록을 불러오고 있습니다.</p>
                </div>
              ) : null}

              {isAdmin && inviteLogState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {inviteLogState.error}
                </SettingsNotice>
              ) : null}

              {isAdmin && inviteLogState.status === "ready" && inviteLogState.invites.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {inviteLogState.invites.map((invite) => (
                    <article className="workspace-settings-member-row" key={invite.inviteId}>
                      <div className="workspace-member-avatar">
                        <Mail aria-hidden="true" />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{invite.email}</strong>
                        <p>{`/invites/${invite.token}`}</p>
                        <span>생성 {formatDateTime(invite.createdAt)}</span>
                      </div>
                      <div className="workspace-settings-member-actions">
                        <div className="workspace-member-meta">
                          <span className={`invite-status-badge invite-status-${invite.status.toLowerCase()}`}>
                            {formatInviteStatus(invite.status)}
                          </span>
                          <span>만료 {formatDateTime(invite.expiresAt)}</span>
                        </div>
                        <button
                          className="danger-action workspace-invite-revoke"
                          type="button"
                          onClick={() => void handleRevokeInvite(invite)}
                          disabled={!isAdmin || invite.status !== "PENDING" || hasInviteAction}
                        >
                          {inviteActionId === invite.inviteId ? (
                            <Loader2 className="spin" aria-hidden="true" />
                          ) : (
                            <Trash2 aria-hidden="true" />
                          )}
                          <span>{inviteActionId === invite.inviteId ? "취소 중" : "취소"}</span>
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : null}

              {isAdmin && inviteLogState.status === "ready" && inviteLogState.invites.length === 0 ? (
                <div className="workspace-member-empty">
                  <Mail aria-hidden="true" />
                  <strong>표시할 초대가 없습니다</strong>
                  <p>조건에 맞는 워크스페이스 초대가 생기면 이곳에 표시됩니다.</p>
                </div>
              ) : null}
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

function SettingsNotice({
  tone,
  icon,
  children,
}: {
  tone: "success" | "danger";
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className={`workspace-settings-notice workspace-settings-notice-${tone}`} role="status">
      {icon}
      <span>{children}</span>
    </div>
  );
}

function RoleSegmentedControl({
  role,
  busy,
  disabled,
  disableAdminDemotion,
  onChange,
}: {
  role: WorkspaceMemberRole;
  busy: boolean;
  disabled: boolean;
  disableAdminDemotion: boolean;
  onChange: (role: WorkspaceMemberRole) => void;
}) {
  const roles: WorkspaceMemberRole[] = ["ADMIN", "MEMBER"];

  return (
    <div className="workspace-role-control" role="group" aria-label="멤버 역할 변경">
      {roles.map((candidateRole) => {
        const active = candidateRole === role;
        const blockedLastAdmin = disableAdminDemotion && role === "ADMIN" && candidateRole === "MEMBER";
        return (
          <button
            className={active ? "workspace-role-option workspace-role-option-active" : "workspace-role-option"}
            type="button"
            key={candidateRole}
            aria-pressed={active}
            onClick={() => {
              if (!active && !blockedLastAdmin) {
                onChange(candidateRole);
              }
            }}
            disabled={disabled || active || blockedLastAdmin}
          >
            {busy && active ? "..." : formatRole(candidateRole)}
          </button>
        );
      })}
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

function messageFromError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return fallback;
}

function isValidWorkspaceId(workspaceId: number): boolean {
  return Number.isInteger(workspaceId) && workspaceId > 0;
}

function formatRole(role: WorkspaceMemberRole): string {
  return role === "ADMIN" ? "관리자" : "멤버";
}

function formatInviteStatus(status: WorkspaceInviteManagementStatus): string {
  switch (status) {
    case "PENDING":
      return "대기";
    case "ACCEPTED":
      return "수락";
    case "EXPIRED":
      return "만료";
    case "REVOKED":
      return "취소";
  }
}

function toInviteStatusParam(
  filter: InviteStatusFilter,
): WorkspaceInviteManagementStatus | undefined {
  return filter === "ALL" ? undefined : filter;
}

function updateInviteLogAfterRevoke(
  invites: WorkspaceInviteManagement[],
  inviteId: number,
  filter: InviteStatusFilter,
): WorkspaceInviteManagement[] {
  if (filter === "PENDING") {
    return invites.filter((invite) => invite.inviteId !== inviteId);
  }

  const revokedAt = new Date().toISOString();
  return invites.map((invite) =>
    invite.inviteId === inviteId
      ? { ...invite, status: "REVOKED", revokedAt }
      : invite,
  );
}

function isLastAdmin(
  member: WorkspaceMember,
  adminCount: number,
  nextRole?: WorkspaceMemberRole,
): boolean {
  if (member.role !== "ADMIN" || adminCount > 1) {
    return false;
  }

  return nextRole === undefined || nextRole !== "ADMIN";
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
