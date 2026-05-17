"use client";

import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarDays,
  Copy,
  FileText,
  FolderTree,
  Hash,
  ListChecks,
  Loader2,
  Mail,
  RefreshCw,
  Send,
  Settings,
  ShieldCheck,
  UserRound,
  Users,
  WifiOff,
  X,
} from "lucide-react";
import Link from "next/link";
import { use, useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { ApiError } from "@/lib/api-client";
import {
  fetchArtifactFile,
  fetchArtifactTree,
  type ArtifactFileContent,
  type ArtifactNode,
  type ArtifactTree,
} from "@/lib/artifacts";
import { createWorkspaceInvite, type WorkspaceInvite } from "@/lib/invites";
import {
  fetchTaskReports,
  fetchWorkspaceTasks,
  type AgentReport,
  type TaskArtifact,
  type TaskPriority,
  type TaskStatus,
  type TaskType,
  type WorkspaceTask,
} from "@/lib/tasks";
import {
  fetchWorkspaceDetail,
  fetchWorkspaceMembers,
  type WorkspaceDetail,
  type WorkspaceMember,
  type WorkspaceMemberRole,
} from "@/lib/workspaces";

const DEV_ACCESS_TOKEN =
  process.env.NEXT_PUBLIC_DEV_ACCESS_TOKEN ?? "";

type WorkspaceDetailState =
  | { status: "loading"; workspace: null; error: null }
  | { status: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[]; error: null }
  | { status: "error" | "offline"; workspace: null; error: string };

type InviteCreateState =
  | { status: "idle"; invite: null; invitePath: null; inviteUrl: null; error: null }
  | { status: "submitting"; invite: null; invitePath: null; inviteUrl: null; error: null }
  | { status: "created"; invite: WorkspaceInvite; invitePath: string; inviteUrl: string; error: null }
  | { status: "error"; invite: null; invitePath: null; inviteUrl: null; error: string };

type TaskLoadState =
  | { status: "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

type ModalLoadState =
  | { status: "idle"; error: null }
  | { status: "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

type TaskGroup = {
  key: string;
  label: string;
  statuses: TaskStatus[];
  tone: "muted" | "blue" | "green" | "red";
};

const TASK_GROUPS: TaskGroup[] = [
  { key: "waiting", label: "대기", statuses: ["REQUESTED", "ASSIGNED"], tone: "muted" },
  { key: "progress", label: "진행", statuses: ["IN_PROGRESS", "WAITING_USER"], tone: "blue" },
  { key: "done", label: "완료", statuses: ["COMPLETED"], tone: "green" },
  { key: "stopped", label: "중단", statuses: ["FAILED", "CANCELED"], tone: "red" },
];

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
  const [inviteEmail, setInviteEmail] = useState("");
  const [copiedInvite, setCopiedInvite] = useState(false);
  const [inviteState, setInviteState] = useState<InviteCreateState>({
    status: "idle",
    invite: null,
    invitePath: null,
    inviteUrl: null,
    error: null,
  });
  const [tasks, setTasks] = useState<WorkspaceTask[]>([]);
  const [taskState, setTaskState] = useState<TaskLoadState>({
    status: "loading",
    error: null,
  });
  const [selectedTask, setSelectedTask] = useState<WorkspaceTask | null>(null);
  const [taskReports, setTaskReports] = useState<AgentReport[]>([]);
  const [reportState, setReportState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [filesOpen, setFilesOpen] = useState(false);
  const [artifactTree, setArtifactTree] = useState<ArtifactTree | null>(null);
  const [artifactTreeState, setArtifactTreeState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });
  const [artifactFile, setArtifactFile] = useState<ArtifactFileContent | null>(null);
  const [artifactFilePath, setArtifactFilePath] = useState<string | null>(null);
  const [artifactFileState, setArtifactFileState] = useState<ModalLoadState>({
    status: "idle",
    error: null,
  });

  const reloadTasks = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setTasks([]);
      setTaskState({
        status: "error",
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    setTaskState({ status: "loading", error: null });

    try {
      const nextTasks = await fetchWorkspaceTasks(DEV_ACCESS_TOKEN, numericWorkspaceId);
      setTasks(nextTasks);
      setTaskState({ status: "ready", error: null });
    } catch (error) {
      setTasks([]);
      setTaskState({
        status: "error",
        error: messageFromError(error, "Task 목록을 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const openTaskReports = useCallback(
    async (task: WorkspaceTask) => {
      setSelectedTask(task);
      setTaskReports([]);
      setReportState({ status: "loading", error: null });

      try {
        const reports = await fetchTaskReports(DEV_ACCESS_TOKEN, numericWorkspaceId, task.taskId);
        setTaskReports(reports);
        setReportState({ status: "ready", error: null });
      } catch (error) {
        setReportState({
          status: "error",
          error: messageFromError(error, "Task report를 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

  const openWorkspaceFiles = useCallback(async () => {
    setFilesOpen(true);
    setArtifactTree(null);
    setArtifactTreeState({ status: "loading", error: null });

    try {
      const tree = await fetchArtifactTree(DEV_ACCESS_TOKEN, numericWorkspaceId);
      setArtifactTree(tree);
      setArtifactTreeState({ status: "ready", error: null });
    } catch (error) {
      setArtifactTreeState({
        status: "error",
        error: messageFromError(error, "파일 트리를 불러오지 못했습니다."),
      });
    }
  }, [numericWorkspaceId]);

  const openArtifactFile = useCallback(
    async (path: string) => {
      setArtifactFile(null);
      setArtifactFilePath(path);
      setArtifactFileState({ status: "loading", error: null });

      try {
        const file = await fetchArtifactFile(DEV_ACCESS_TOKEN, numericWorkspaceId, path);
        setArtifactFile(file);
        setArtifactFileState({ status: "ready", error: null });
      } catch (error) {
        setArtifactFileState({
          status: "error",
          error: messageFromError(error, "파일 내용을 불러오지 못했습니다."),
        });
      }
    },
    [numericWorkspaceId],
  );

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
        fetchWorkspaceDetail(DEV_ACCESS_TOKEN, numericWorkspaceId),
        fetchWorkspaceMembers(DEV_ACCESS_TOKEN, numericWorkspaceId),
      ]);
      setState({ status: "ready", workspace, members, error: null });
      void reloadTasks();
    } catch (error) {
      setState(toFailureState(error, "워크스페이스 정보를 확인하지 못했습니다."));
    }
  }, [numericWorkspaceId, reloadTasks]);

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
          setTasks([]);
          setTaskState({
            status: "error",
            error: "올바르지 않은 워크스페이스입니다.",
          });
        }
        return;
      }

      try {
        const [workspace, members, initialTasksResult] = await Promise.all([
          fetchWorkspaceDetail(DEV_ACCESS_TOKEN, numericWorkspaceId),
          fetchWorkspaceMembers(DEV_ACCESS_TOKEN, numericWorkspaceId),
          fetchWorkspaceTasks(DEV_ACCESS_TOKEN, numericWorkspaceId)
            .then((nextTasks) => ({ ok: true as const, nextTasks }))
            .catch((error) => ({ ok: false as const, error })),
        ]);
        if (!ignore) {
          setState({ status: "ready", workspace, members, error: null });
          if (initialTasksResult.ok) {
            setTasks(initialTasksResult.nextTasks);
            setTaskState({ status: "ready", error: null });
          } else {
            setTasks([]);
            setTaskState({
              status: "error",
              error: messageFromError(initialTasksResult.error, "Task 목록을 불러오지 못했습니다."),
            });
          }
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
  const groupedTasks = useMemo(
    () =>
      TASK_GROUPS.map((group) => ({
        ...group,
        tasks: tasks.filter((task) => group.statuses.includes(task.status)),
      })),
    [tasks],
  );
  const canCreateInvite = workspace?.myRole === "ADMIN";
  const isCreatingInvite = inviteState.status === "submitting";
  const canSubmitInvite = canCreateInvite && inviteEmail.trim().length > 0 && !isCreatingInvite;

  const handleCreateInvite = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!isValidWorkspaceId(numericWorkspaceId)) {
      setInviteState({
        status: "error",
        invite: null,
        invitePath: null,
        inviteUrl: null,
        error: "올바르지 않은 워크스페이스입니다.",
      });
      return;
    }

    const email = inviteEmail.trim();
    if (!email) {
      setInviteState({
        status: "error",
        invite: null,
        invitePath: null,
        inviteUrl: null,
        error: "초대할 이메일을 입력해 주세요.",
      });
      return;
    }

    setInviteState({
      status: "submitting",
      invite: null,
      invitePath: null,
      inviteUrl: null,
      error: null,
    });
    setCopiedInvite(false);

    try {
      const invite = await createWorkspaceInvite(
        DEV_ACCESS_TOKEN,
        numericWorkspaceId,
        { email },
      );
      const invitePath = `/invites/${invite.token}`;
      const inviteUrl = `${window.location.origin}${invitePath}`;
      setInviteState({ status: "created", invite, invitePath, inviteUrl, error: null });
      setCopiedInvite(false);
      setInviteEmail("");
    } catch (error) {
      setInviteState({
        status: "error",
        invite: null,
        invitePath: null,
        inviteUrl: null,
        error: messageFromError(error, "초대 생성을 처리하지 못했습니다."),
      });
    }
  };

  const copyInviteUrl = async () => {
    if (inviteState.status !== "created") {
      return;
    }

    try {
      await window.navigator.clipboard?.writeText(inviteState.inviteUrl);
      setCopiedInvite(true);
    } catch {
      setCopiedInvite(false);
    }
  };

  return (
    <main className="workspace-detail-shell">
      <section className="workspace-detail-panel" aria-label="워크스페이스 상세">
        <header className="workspace-detail-header">
          <div className="workspace-detail-actions">
            <Link className="secondary-action workspace-detail-back" href="/">
              <ArrowLeft aria-hidden="true" />
              <span>목록으로</span>
            </Link>
            {isValidWorkspaceId(numericWorkspaceId) ? (
              <Link
                className="secondary-action workspace-detail-back"
                href={`/workspaces/${numericWorkspaceId}/settings`}
              >
                <Settings aria-hidden="true" />
                <span>설정</span>
              </Link>
            ) : null}
          </div>
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

            <section className="workspace-task-section" aria-label="워크스페이스 Task">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Tasks</span>
                  <h2>작업 보드</h2>
                </div>
                <div className="workspace-task-actions">
                  <button
                    className="secondary-action workspace-task-refresh"
                    type="button"
                    onClick={() => void openWorkspaceFiles()}
                    disabled={artifactTreeState.status === "loading"}
                  >
                    <FolderTree aria-hidden="true" />
                    <span>파일</span>
                  </button>
                  <button
                    className="secondary-action workspace-task-refresh"
                    type="button"
                    onClick={() => void reloadTasks()}
                    disabled={taskState.status === "loading"}
                  >
                    {taskState.status === "loading" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <RefreshCw aria-hidden="true" />
                    )}
                    <span>새로고침</span>
                  </button>
                </div>
              </div>

              {taskState.status === "error" ? (
                <div className="workspace-task-notice" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{taskState.error}</span>
                </div>
              ) : null}

              {taskState.status === "loading" ? (
                <div className="workspace-task-notice">
                  <Loader2 className="spin" aria-hidden="true" />
                  <span>Task 목록을 불러오고 있습니다.</span>
                </div>
              ) : null}

              <div className="workspace-task-board">
                {groupedTasks.map((group) => (
                  <article className={`workspace-task-column workspace-task-column-${group.tone}`} key={group.key}>
                    <div className="workspace-task-column-header">
                      <strong>{group.label}</strong>
                      <span>{group.tasks.length}</span>
                    </div>
                    {group.tasks.length > 0 ? (
                      <div className="workspace-task-list">
                        {group.tasks.map((task) => (
                          <TaskCard task={task} key={task.taskId} onOpenReports={openTaskReports} />
                        ))}
                      </div>
                    ) : (
                      <div className="workspace-task-empty">
                        <ListChecks aria-hidden="true" />
                        <span>표시할 Task가 없습니다.</span>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            </section>

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

            <section className="workspace-invite-section" aria-label="워크스페이스 초대 생성">
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Invite</span>
                  <h2>멤버 초대</h2>
                </div>
                <span className={canCreateInvite ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {canCreateInvite ? "ADMIN" : "MEMBER"}
                </span>
              </div>

              <form className="workspace-invite-form" onSubmit={handleCreateInvite}>
                <label className="form-field">
                  <span>
                    <strong>초대 이메일</strong>
                    <small>{canCreateInvite ? "7일 유효" : "관리자 전용"}</small>
                  </span>
                  <input
                    type="email"
                    value={inviteEmail}
                    onChange={(event) => setInviteEmail(event.target.value)}
                    placeholder="member@example.com"
                    disabled={!canCreateInvite || isCreatingInvite}
                  />
                </label>

                {inviteState.status === "error" ? (
                  <div className="form-notice form-notice-error" role="alert">
                    <AlertCircle aria-hidden="true" />
                    <span>{inviteState.error}</span>
                  </div>
                ) : null}

                {inviteState.status === "created" ? (
                  <div className="workspace-invite-result" role="status">
                    <Mail aria-hidden="true" />
                    <div>
                      <strong>{inviteState.invite.email}</strong>
                      <p>{inviteState.inviteUrl}</p>
                      <div className="workspace-invite-actions">
                        <Link className="secondary-action workspace-invite-open" href={inviteState.invitePath}>
                          초대 열기
                        </Link>
                        <button
                          className="secondary-action workspace-invite-copy"
                          type="button"
                          onClick={() => void copyInviteUrl()}
                        >
                          <Copy aria-hidden="true" />
                          <span>{copiedInvite ? "복사됨" : "복사"}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ) : null}

                {!canCreateInvite ? (
                  <p className="workspace-invite-help">
                    관리자 역할만 새 초대를 생성할 수 있습니다.
                  </p>
                ) : null}

                <button
                  className="primary-action primary-action-active workspace-invite-submit"
                  type="submit"
                  disabled={!canSubmitInvite}
                >
                  {isCreatingInvite ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <Send aria-hidden="true" />
                  )}
                  <span>{isCreatingInvite ? "생성 중" : "초대 생성"}</span>
                </button>
              </form>
            </section>
          </section>
        ) : null}
      </section>

      <aside className="workspace-detail-side" aria-label="조회 정보">
        <div>
          <UserRound aria-hidden="true" />
          <strong>{DEV_ACCESS_TOKEN ? "설정됨" : "미설정"}</strong>
          <span>개발 access token</span>
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
        <div>
          <ListChecks aria-hidden="true" />
          <strong>{tasks.length}개</strong>
          <span>Task board 표시 항목</span>
        </div>
      </aside>

      <TaskReportsModal
        task={selectedTask}
        reports={taskReports}
        state={reportState}
        onClose={() => {
          setSelectedTask(null);
          setTaskReports([]);
          setReportState({ status: "idle", error: null });
        }}
        onOpenArtifact={(path) => void openArtifactFile(path)}
      />
      <WorkspaceFilesModal
        open={filesOpen}
        tree={artifactTree}
        state={artifactTreeState}
        onClose={() => {
          setFilesOpen(false);
          setArtifactTree(null);
          setArtifactTreeState({ status: "idle", error: null });
        }}
        onReload={() => void openWorkspaceFiles()}
        onOpenFile={(path) => void openArtifactFile(path)}
      />
      <ArtifactFileModal
        path={artifactFilePath}
        file={artifactFile}
        state={artifactFileState}
        onClose={() => {
          setArtifactFile(null);
          setArtifactFilePath(null);
          setArtifactFileState({ status: "idle", error: null });
        }}
      />
    </main>
  );
}

function TaskCard({
  task,
  onOpenReports,
}: {
  task: WorkspaceTask;
  onOpenReports: (task: WorkspaceTask) => void;
}) {
  return (
    <button className="workspace-task-card" type="button" onClick={() => onOpenReports(task)}>
      <div className="workspace-task-card-title">
        <strong>{task.title}</strong>
        <span className={`task-status-badge task-status-${task.status.toLowerCase().replaceAll("_", "-")}`}>
          {formatTaskStatus(task.status)}
        </span>
      </div>
      {task.description ? <p>{task.description}</p> : null}
      <div className="workspace-task-card-meta">
        <span>{formatTaskType(task.type)}</span>
        <span>{formatTaskPriority(task.priority)}</span>
        <span>{formatDateTime(task.updatedAt ?? task.createdAt ?? "")}</span>
      </div>
    </button>
  );
}

function TaskReportsModal({
  task,
  reports,
  state,
  onClose,
  onOpenArtifact,
}: {
  task: WorkspaceTask | null;
  reports: AgentReport[];
  state: ModalLoadState;
  onClose: () => void;
  onOpenArtifact: (path: string) => void;
}) {
  if (!task) {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal" role="dialog" aria-modal="true" aria-label="Task report">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">Task #{task.taskId}</span>
            <h2>Report</h2>
          </div>
          <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          <div className="workspace-modal-summary">
            <strong>{task.title}</strong>
            {task.description ? <p>{task.description}</p> : null}
          </div>

          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="Report를 불러오고 있습니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && reports.length === 0 ? (
            <ModalState icon={<FileText aria-hidden="true" />} text="아직 표시할 report가 없습니다." />
          ) : null}

          {reports.length > 0 ? (
            <div className="workspace-report-list">
              {reports.map((report) => (
                <article className="workspace-report-card" key={report.reportId}>
                  <div className="workspace-report-card-header">
                    <strong>Report #{report.reportId}</strong>
                    <span className={`task-status-badge task-status-${report.status.toLowerCase().replaceAll("_", "-")}`}>
                      {formatTaskStatus(report.status)}
                    </span>
                  </div>
                  {report.summary ? <p>{report.summary}</p> : null}
                  {report.recommendedAction ? (
                    <p className="workspace-report-action">{report.recommendedAction}</p>
                  ) : null}
                  {report.artifacts && report.artifacts.length > 0 ? (
                    <div className="workspace-report-artifacts">
                      {report.artifacts.map((artifact) => {
                        const path = artifactPath(artifact);
                        return (
                          <button
                            className="secondary-action workspace-report-artifact"
                            type="button"
                            key={artifact.artifactId}
                            onClick={() => path && onOpenArtifact(path)}
                            disabled={!path}
                          >
                            <FileText aria-hidden="true" />
                            <span>{artifact.name}</span>
                          </button>
                        );
                      })}
                    </div>
                  ) : null}
                </article>
              ))}
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function WorkspaceFilesModal({
  open,
  tree,
  state,
  onClose,
  onReload,
  onOpenFile,
}: {
  open: boolean;
  tree: ArtifactTree | null;
  state: ModalLoadState;
  onClose: () => void;
  onReload: () => void;
  onOpenFile: (path: string) => void;
}) {
  if (!open) {
    return null;
  }

  const nodes = tree?.children ?? [];

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal" role="dialog" aria-modal="true" aria-label="Workspace files">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">{tree?.rootPath ?? "/"}</span>
            <h2>Files</h2>
          </div>
          <div className="workspace-modal-actions">
            <button
              className="secondary-action workspace-task-refresh"
              type="button"
              onClick={onReload}
              disabled={state.status === "loading"}
            >
              <RefreshCw aria-hidden="true" />
              <span>새로고침</span>
            </button>
            <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
              <X aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="workspace-modal-body">
          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="파일 트리를 불러오고 있습니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {state.status === "ready" && nodes.length === 0 ? (
            <ModalState icon={<FolderTree aria-hidden="true" />} text="아직 표시할 파일이 없습니다." />
          ) : null}
          {nodes.length > 0 ? <ArtifactTreeNodes nodes={nodes} depth={0} onOpenFile={onOpenFile} /> : null}
        </div>
      </section>
    </div>
  );
}

function ArtifactTreeNodes({
  nodes,
  depth,
  onOpenFile,
}: {
  nodes: ArtifactNode[];
  depth: number;
  onOpenFile: (path: string) => void;
}) {
  return (
    <ul className="workspace-artifact-tree">
      {nodes.map((node) => (
        <li key={node.path}>
          <button
            className="workspace-artifact-row"
            type="button"
            onClick={() => {
              if (node.type === "FILE") {
                onOpenFile(node.path);
              }
            }}
            style={{ paddingLeft: 10 + depth * 16 }}
          >
            <span aria-hidden="true">{node.type === "DIRECTORY" ? "▾" : "·"}</span>
            <strong>{node.name}</strong>
            {typeof node.sizeBytes === "number" ? <small>{formatFileSize(node.sizeBytes)}</small> : null}
          </button>
          {node.children && node.children.length > 0 ? (
            <ArtifactTreeNodes nodes={node.children} depth={depth + 1} onOpenFile={onOpenFile} />
          ) : null}
        </li>
      ))}
    </ul>
  );
}

function ArtifactFileModal({
  path,
  file,
  state,
  onClose,
}: {
  path: string | null;
  file: ArtifactFileContent | null;
  state: ModalLoadState;
  onClose: () => void;
}) {
  if (!path && state.status === "idle") {
    return null;
  }

  return (
    <div className="workspace-modal-backdrop" role="presentation">
      <section className="workspace-modal workspace-file-modal" role="dialog" aria-modal="true" aria-label="Artifact file">
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">{path ?? ""}</span>
            <h2>{file?.name ?? "File"}</h2>
          </div>
          <button className="icon-button workspace-modal-close" type="button" onClick={onClose} aria-label="닫기">
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          {state.status === "loading" ? (
            <ModalState icon={<Loader2 className="spin" aria-hidden="true" />} text="파일 내용을 불러오고 있습니다." />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}
          {file ? <pre className="workspace-file-content">{file.content}</pre> : null}
        </div>
      </section>
    </div>
  );
}

function ModalState({
  icon,
  text,
  tone = "neutral",
}: {
  icon: ReactNode;
  text: string;
  tone?: "neutral" | "danger";
}) {
  return (
    <div className={`workspace-modal-state workspace-modal-state-${tone}`}>
      {icon}
      <span>{text}</span>
    </div>
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

function formatTaskStatus(status: TaskStatus): string {
  switch (status) {
    case "REQUESTED":
      return "요청됨";
    case "ASSIGNED":
      return "배정됨";
    case "IN_PROGRESS":
      return "진행 중";
    case "WAITING_USER":
      return "대기";
    case "COMPLETED":
      return "완료";
    case "FAILED":
      return "실패";
    case "CANCELED":
      return "취소";
  }
}

function formatTaskType(type?: TaskType): string {
  if (!type) {
    return "유형 없음";
  }
  return type.replaceAll("_", " ");
}

function formatTaskPriority(priority?: TaskPriority): string {
  if (!priority) {
    return "우선순위 없음";
  }
  if (priority === "URGENT") {
    return "긴급";
  }
  if (priority === "HIGH") {
    return "높음";
  }
  if (priority === "MEDIUM") {
    return "보통";
  }
  return "낮음";
}

function artifactPath(artifact: TaskArtifact): string | null {
  if (artifact.url) {
    try {
      const url = new URL(artifact.url, "http://local");
      return url.searchParams.get("path");
    } catch {
      return null;
    }
  }

  if (artifact.artifactType === "FILE_PATH" || artifact.artifactType === "RESULT_FILE") {
    return artifact.name;
  }

  return null;
}

function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return "";
  }
  if (bytes < 1024) {
    return `${bytes}B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)}KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
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
