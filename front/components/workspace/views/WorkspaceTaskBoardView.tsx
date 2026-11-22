"use client";

import { useMemo, useState, type DragEvent as ReactDragEvent } from "react";
import {
  AlertCircle,
  Bot,
  CheckCircle2,
  Coins,
  LayoutGrid,
  List,
  Loader2,
  Play,
  Plus,
  RotateCcw,
  Search,
  ShieldAlert,
  XCircle,
} from "lucide-react";
import PixelAvatar from "@/components/arcade/PixelAvatar";
import {
  formatDurationMs,
  formatTaskPriority,
  formatTaskStatus,
  formatTaskType,
  formatTokenUsageSummary,
  TASK_GROUPS,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgent } from "@/lib/agents";
import { searchBitapItems } from "@/lib/bitap-fuzzy-search";
import type { TaskStatus, WorkspaceTask } from "@/lib/tasks";
import type { WorkspaceDashboardSummary } from "@/lib/workspaces";

export type WorkspaceTaskBoardViewProps = {
  tasks: WorkspaceTask[];
  agents: WorkspaceAgent[];
  summary: WorkspaceDashboardSummary;
  selectedTaskId: number | null;
  searchQuery: string;
  statusFilter: TaskStatus | "ALL";
  layoutMode: "kanban" | "list";
  canManage: boolean;
  mutatingTaskId: number | null;
  recoveringStale: boolean;
  staleRecoveryNotice: { tone: "success" | "danger"; message: string } | null;
  onSearchQueryChange: (value: string) => void;
  onStatusFilterChange: (value: TaskStatus | "ALL") => void;
  onLayoutModeChange: (mode: "kanban" | "list") => void;
  onOpenCreateModal: () => void;
  onRecoverStaleTasks: () => void;
  onSelectTask: (taskId: number) => void;
  onRunTask: (task: WorkspaceTask) => void;
  onRequestApproval: (task: WorkspaceTask) => void;
  onCancelTask: (task: WorkspaceTask) => void;
  onUpdateTaskStatus?: (taskId: number, status: TaskStatus) => void;
};

const COLUMN_DROP_STATUS: Record<string, TaskStatus> = {
  waiting: "ASSIGNED",
  progress: "IN_PROGRESS",
  done: "COMPLETED",
  stopped: "CANCELED",
};

const GROUP_DOT_COLORS: Record<string, string> = {
  done: "var(--studio-ok)",
  progress: "var(--studio-warn)",
  waiting: "var(--studio-info)",
  stopped: "var(--studio-danger)",
};

function parseAssignedAgentId(value: WorkspaceTask["assignedAgentId"]): number | null {
  const parsed =
    typeof value === "string" ? Number.parseInt(value, 10) : value;
  return typeof parsed === "number" && Number.isFinite(parsed) ? parsed : null;
}

const ALL_STATUS_OPTIONS: Array<{ value: TaskStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "전체 상태" },
  { value: "REQUESTED", label: "요청됨" },
  { value: "ASSIGNED", label: "배정됨" },
  { value: "IN_PROGRESS", label: "진행 중" },
  { value: "WAITING_USER", label: "승인 대기" },
  { value: "COMPLETED", label: "완료" },
  { value: "FAILED", label: "실패" },
  { value: "CANCELED", label: "취소" },
];

export function WorkspaceTaskBoardView({
  tasks,
  agents,
  summary,
  selectedTaskId,
  searchQuery,
  statusFilter,
  layoutMode,
  canManage,
  mutatingTaskId,
  recoveringStale,
  staleRecoveryNotice,
  onSearchQueryChange,
  onStatusFilterChange,
  onLayoutModeChange,
  onOpenCreateModal,
  onRecoverStaleTasks,
  onSelectTask,
  onRunTask,
  onRequestApproval,
  onCancelTask,
  onUpdateTaskStatus,
}: WorkspaceTaskBoardViewProps) {
  const [agentFilter, setAgentFilter] = useState<string>("ALL");
  const [draggedTaskId, setDraggedTaskId] = useState<number | null>(null);
  const [dropColumnId, setDropColumnId] = useState<string | null>(null);

  const agentById = useMemo(() => {
    const map = new Map<number, WorkspaceAgent>();
    for (let i = 0; i < agents.length; i += 1) {
      map.set(agents[i].agentId, agents[i]);
    }
    return map;
  }, [agents]);

  const filteredTasks = useMemo(() => {
    const trimmed = searchQuery.trim();
    const matched: WorkspaceTask[] = [];
    for (let i = 0; i < tasks.length; i += 1) {
      const task = tasks[i];
      if (statusFilter !== "ALL" && task.status !== statusFilter) {
        continue;
      }
      if (agentFilter !== "ALL") {
        const assignedId = parseAssignedAgentId(task.assignedAgentId);
        if (agentFilter === "UNASSIGNED") {
          if (assignedId !== null) continue;
        } else if (String(assignedId ?? "") !== agentFilter) {
          continue;
        }
      }
      matched.push(task);
    }
    if (!trimmed) {
      return matched;
    }
    return searchBitapItems(
      matched,
      trimmed,
      (task) => `${task.title} ${task.description ?? ""} ${task.type ?? ""}`,
      { maxDistance: 1 },
    ).map((hit) => hit.item);
  }, [agentFilter, searchQuery, statusFilter, tasks]);

  const hasActiveFilters =
    searchQuery.trim().length > 0 || statusFilter !== "ALL" || agentFilter !== "ALL";

  const handleTaskDragStart = (
    event: ReactDragEvent<HTMLElement>,
    taskId: number,
  ) => {
    setDraggedTaskId(taskId);
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(taskId));
  };

  const handleTaskDragEnd = () => {
    setDraggedTaskId(null);
    setDropColumnId(null);
  };

  const handleColumnDrop = (
    event: ReactDragEvent<HTMLDivElement>,
    groupKey: string,
    groupStatuses: readonly TaskStatus[],
  ) => {
    event.preventDefault();
    setDropColumnId(null);
    if (!canManage || !onUpdateTaskStatus) {
      setDraggedTaskId(null);
      return;
    }
    const rawId = draggedTaskId ?? Number.parseInt(event.dataTransfer.getData("text/plain"), 10);
    setDraggedTaskId(null);
    if (!Number.isFinite(rawId)) return;
    const task = tasks.find((item) => item.taskId === rawId);
    const targetStatus = COLUMN_DROP_STATUS[groupKey];
    if (!task || !targetStatus || groupStatuses.includes(task.status)) {
      return;
    }
    onUpdateTaskStatus(task.taskId, targetStatus);
  };

  const groupedTasks = useMemo(() => {
    const buckets = new Map<string, WorkspaceTask[]>();
    const statusToGroupKey = new Map<TaskStatus, string>();
    for (let i = 0; i < TASK_GROUPS.length; i += 1) {
      const group = TASK_GROUPS[i];
      buckets.set(group.key, []);
      for (let j = 0; j < group.statuses.length; j += 1) {
        statusToGroupKey.set(group.statuses[j], group.key);
      }
    }
    for (let i = 0; i < filteredTasks.length; i += 1) {
      const task = filteredTasks[i];
      const key = statusToGroupKey.get(task.status) ?? "waiting";
      buckets.get(key)?.push(task);
    }
    return buckets;
  }, [filteredTasks]);

  const tokenSummary = formatTokenUsageSummary(summary.todayUsedTokens, summary.dailyTokenLimit);
  const budgetBarTone =
    tokenSummary.percent !== null && tokenSummary.percent >= 95
      ? " chrome-budget-fill-danger"
      : tokenSummary.percent !== null && tokenSummary.percent >= 80
        ? " chrome-budget-fill-warning"
        : "";

  const resolveTaskAssignee = (task: WorkspaceTask) => {
    const assignedId = parseAssignedAgentId(task.assignedAgentId);
    const assignedAgent = assignedId !== null ? agentById.get(assignedId) : undefined;
    const assignedName =
      assignedId !== null ? assignedAgent?.name ?? `에이전트 #${assignedId}` : "미배정";
    return { assignedId, assignedAgent, assignedName };
  };

  const renderTaskActions = (task: WorkspaceTask, assignedId: number | null, busy: boolean) => {
    const canRun =
      task.status === "REQUESTED" ||
      task.status === "ASSIGNED" ||
      task.status === "FAILED" ||
      task.status === "CANCELED";
    const canRequestApproval = task.status === "IN_PROGRESS" || task.status === "ASSIGNED";
    const canCancel =
      task.status === "REQUESTED" ||
      task.status === "ASSIGNED" ||
      task.status === "IN_PROGRESS" ||
      task.status === "WAITING_USER";

    return (
      <>
        {canRun ? (
          <button
            type="button"
            className="tb-btn tb-btn-primary"
            disabled={busy || (assignedId == null && agents.length === 0)}
            onClick={() => onRunTask(task)}
          >
            {busy ? (
              <Loader2 size={12} className="spin" aria-hidden="true" />
            ) : (
              <Play size={12} aria-hidden="true" />
            )}
            <span>실행</span>
          </button>
        ) : null}
        {canRequestApproval ? (
          <button
            type="button"
            className="tb-btn"
            disabled={busy}
            onClick={() => onRequestApproval(task)}
          >
            <ShieldAlert size={12} aria-hidden="true" />
            <span>승인 요청</span>
          </button>
        ) : null}
        {canCancel ? (
          <button
            type="button"
            className="tb-btn"
            disabled={busy}
            onClick={() => onCancelTask(task)}
          >
            <XCircle size={12} aria-hidden="true" />
            <span>취소</span>
          </button>
        ) : null}
      </>
    );
  };

  const renderTaskCard = (task: WorkspaceTask) => {
    const selected = selectedTaskId === task.taskId;
    const busy = mutatingTaskId === task.taskId;
    const { assignedId, assignedAgent, assignedName } = resolveTaskAssignee(task);
    const draggable = Boolean(canManage && onUpdateTaskStatus && !busy);

    return (
      <article
        key={task.taskId}
        draggable={draggable}
        onDragStart={(event) => handleTaskDragStart(event, task.taskId)}
        onDragEnd={handleTaskDragEnd}
        className={`ao-surface-panel ao-cv-card chrome-kanban-card cursor-pointer transition-colors${
          selected ? " border-[var(--accent-primary)] bg-[var(--ui-selected)]" : ""
        }`}
        onClick={() => onSelectTask(task.taskId)}
      >
        <div className="flex items-start justify-between gap-2 mb-2">
          <div className="min-w-0">
            <span className="text-[12px] text-[var(--ui-text-muted)] font-mono">
              #{task.taskId}
            </span>
            <h4 className="m-0 text-[13px] font-semibold text-[var(--ui-text-strong)]">
              {task.title}
            </h4>
          </div>
          <span
            className={`task-status-badge task-status-${task.status.toLowerCase().replaceAll("_", "-")}`}
          >
            {formatTaskStatus(task.status)}
          </span>
        </div>

        {task.description ? (
          <p className="m-0 mb-2 text-[12px] text-[var(--ui-text-secondary)] line-clamp-2">
            {task.description}
          </p>
        ) : null}

        <div className="flex flex-wrap items-center gap-2 text-[12px] text-[var(--ui-text-muted)] mb-2">
          <span className="inline-flex items-center gap-1.5 min-w-0">
            {assignedId !== null ? (
              <PixelAvatar
                kind="agent"
                variant="mugshot"
                variantKey={assignedName}
                agentCategory={assignedAgent?.category}
                size={1.5}
              />
            ) : (
              <Bot size={12} aria-hidden="true" />
            )}
            <span className="truncate">{assignedName}</span>
          </span>
          <span className="role-badge">{formatTaskPriority(task.priority)}</span>
          {task.type ? <span className="role-badge">{formatTaskType(task.type)}</span> : null}
        </div>

        <div
          className="flex flex-wrap items-center gap-1.5 pt-2 border-t border-[var(--ui-border-subtle)]"
          onClick={(event) => event.stopPropagation()}
        >
          {renderTaskActions(task, assignedId, busy)}
        </div>
      </article>
    );
  };

  const renderTaskListRow = (task: WorkspaceTask) => {
    const selected = selectedTaskId === task.taskId;
    const busy = mutatingTaskId === task.taskId;
    const { assignedId, assignedName } = resolveTaskAssignee(task);

    return (
      <article
        key={task.taskId}
        className={`ao-task-list-row ao-cv-card${
          selected ? " border-[var(--accent-primary)] bg-[var(--ui-selected)]" : ""
        }`}
        onClick={() => onSelectTask(task.taskId)}
      >
        <div className="min-w-0 flex flex-col gap-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[12px] font-mono text-[var(--ui-text-muted)]">
              #{task.taskId}
            </span>
            <h4 className="m-0 text-[13px] font-semibold text-[var(--ui-text-strong)] truncate">
              {task.title}
            </h4>
            <span
              className={`task-status-badge task-status-${task.status.toLowerCase().replaceAll("_", "-")}`}
            >
              {formatTaskStatus(task.status)}
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-2 text-[12px] text-[var(--ui-text-muted)]">
            <span>{assignedName}</span>
            <span className="role-badge">{formatTaskPriority(task.priority)}</span>
            {task.type ? <span className="role-badge">{formatTaskType(task.type)}</span> : null}
          </div>
        </div>
        <div
          className="flex flex-wrap items-center gap-1.5 shrink-0"
          onClick={(event) => event.stopPropagation()}
        >
          {renderTaskActions(task, assignedId, busy)}
        </div>
      </article>
    );
  };

  return (
    <div className="chrome-view-scroll" aria-label="작업 보드 뷰">
      <div className="ao-page-header">
        <div>
          <p className="ao-page-eyebrow">작업 관리</p>
          <h2 className="ao-page-title">작업 보드</h2>
          <p className="ao-page-subtitle">
            에이전트 작업 상태, 일일 토큰 예산, 승인 요청 및 고착 복구를 관리합니다.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {canManage ? (
            <button
              type="button"
              className="secondary-action"
              onClick={onRecoverStaleTasks}
              disabled={recoveringStale}
            >
              {recoveringStale ? (
                <Loader2 size={14} className="spin" aria-hidden="true" />
              ) : (
                <RotateCcw size={14} aria-hidden="true" />
              )}
              <span>{recoveringStale ? "복구 중" : "고착 작업 복구"}</span>
            </button>
          ) : null}
          <button
            type="button"
            className="primary-action primary-action-active"
            onClick={onOpenCreateModal}
          >
            <Plus size={14} aria-hidden="true" />
            <span>새 작업 생성</span>
          </button>
        </div>
      </div>

      <section className="ao-kpi-grid" aria-label="작업 요약 지표">
        <article className="ao-kpi-card">
          <span>전체 작업</span>
          <strong>{summary.taskCount}</strong>
          <small>진행 중 {summary.runningTaskCount}건</small>
        </article>
        <article className="ao-kpi-card">
          <span>승인 대기</span>
          <strong>{summary.waitingApprovalTaskCount ?? 0}</strong>
          <small>검토가 필요한 작업</small>
        </article>
        <article className="ao-kpi-card">
          <span>완료 / 실패</span>
          <strong>
            {summary.completedTaskCount} / {summary.failedTaskCount}
          </strong>
          <small>누적 처리 결과</small>
        </article>
        <article className="ao-kpi-card">
          <span>평균 실행 시간</span>
          <strong>{formatDurationMs(summary.avgExecutionDurationMs)}</strong>
          <small>에이전트 실행 기준</small>
        </article>
      </section>

      <div className="ao-telemetry-grid">
        <section className="ao-telemetry-card" aria-label="일일 토큰 사용량">
          <div className="flex items-center justify-between gap-2 text-[13px]">
            <span className="inline-flex items-center gap-2 font-semibold text-[var(--ui-text-strong)]">
              <Coins size={15} aria-hidden="true" />
              <span>일일 토큰 예산</span>
            </span>
            <span className="font-mono text-[12px] text-[var(--ui-text-secondary)]">
              {tokenSummary.label}
            </span>
          </div>
          <div className="chrome-budget-track">
            <div
              className={`chrome-budget-fill${budgetBarTone}`}
              style={{ width: `${tokenSummary.percent ?? 12}%` }}
            />
          </div>
        </section>

        <section className="ao-telemetry-card" aria-label="작업 상태 분포">
          <div className="flex items-center justify-between gap-2 text-[13px]">
            <span className="font-semibold text-[var(--ui-text-strong)]">작업 상태 분포</span>
            <span className="font-mono text-[12px] text-[var(--ui-text-secondary)]">
              {filteredTasks.length}건
            </span>
          </div>
          <div className="ao-task-segment-bar" aria-label="작업 상태 비율">
            {TASK_GROUPS.map((group) => {
              const count = groupedTasks.get(group.key)?.length ?? 0;
              if (count === 0 || filteredTasks.length === 0) return null;
              const widthPct = Math.max(4, Math.round((count / filteredTasks.length) * 100));
              return (
                <span
                  key={group.key}
                  className={`ao-task-segment ao-task-segment--${group.key}`}
                  style={{ width: `${widthPct}%` }}
                  title={`${group.label}: ${count}건`}
                />
              );
            })}
          </div>
          <div className="ao-telemetry-legend">
            {TASK_GROUPS.map((group) => {
              const count = groupedTasks.get(group.key)?.length ?? 0;
              return (
                <span key={group.key} className="ao-telemetry-legend-item">
                  <span
                    className="ao-telemetry-dot"
                    style={{ background: GROUP_DOT_COLORS[group.key] ?? "var(--ui-border)" }}
                  />
                  <span>
                    {group.label} {count}
                  </span>
                </span>
              );
            })}
          </div>
        </section>
      </div>

      {staleRecoveryNotice ? (
        <div
          className={`ao-inline-notice ${
            staleRecoveryNotice.tone === "danger"
              ? "ao-inline-notice-danger"
              : "ao-inline-notice-success"
          }`}
          role={staleRecoveryNotice.tone === "danger" ? "alert" : "status"}
        >
          {staleRecoveryNotice.tone === "danger" ? (
            <AlertCircle size={15} aria-hidden="true" />
          ) : (
            <CheckCircle2 size={15} aria-hidden="true" />
          )}
          <span>{staleRecoveryNotice.message}</span>
        </div>
      ) : null}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2 flex-1 min-w-[240px]">
          <label className="relative flex-1 min-w-[200px] max-w-[360px]">
            <Search
              size={14}
              aria-hidden="true"
              className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[var(--ui-text-muted)]"
            />
            <input
              type="search"
              aria-label="작업 검색"
              placeholder="작업 제목, 설명, 유형 검색"
              value={searchQuery}
              onChange={(event) => onSearchQueryChange(event.target.value)}
              className="w-full h-8 pl-8 pr-3 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-surface)] text-[13px] text-[var(--ui-text)]"
            />
          </label>

          <select
            aria-label="작업 상태 필터"
            value={statusFilter}
            onChange={(event) => onStatusFilterChange(event.target.value as TaskStatus | "ALL")}
            className="h-8 px-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-surface)] text-[13px] text-[var(--ui-text)]"
          >
            {ALL_STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <select
            aria-label="담당 에이전트 필터"
            value={agentFilter}
            onChange={(event) => setAgentFilter(event.target.value)}
            className="h-8 px-2.5 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-surface)] text-[13px] text-[var(--ui-text)]"
          >
            <option value="ALL">전체 담당자</option>
            <option value="UNASSIGNED">미배정</option>
            {agents.map((agent) => (
              <option key={agent.agentId} value={String(agent.agentId)}>
                {agent.name}
              </option>
            ))}
          </select>

          {hasActiveFilters ? (
            <button
              type="button"
              className="tb-btn"
              onClick={() => {
                onSearchQueryChange("");
                onStatusFilterChange("ALL");
                setAgentFilter("ALL");
              }}
            >
              <span>필터 초기화</span>
            </button>
          ) : null}
        </div>

        <div className="flex items-center gap-1" role="group" aria-label="보드 보기 형식">
          <button
            type="button"
            className={`tb-btn${layoutMode === "kanban" ? " tb-btn-primary" : ""}`}
            onClick={() => onLayoutModeChange("kanban")}
          >
            <LayoutGrid size={13} aria-hidden="true" />
            <span>칸반</span>
          </button>
          <button
            type="button"
            className={`tb-btn${layoutMode === "list" ? " tb-btn-primary" : ""}`}
            onClick={() => onLayoutModeChange("list")}
          >
            <List size={13} aria-hidden="true" />
            <span>목록</span>
          </button>
        </div>
      </div>

      {filteredTasks.length === 0 ? (
        <div className="ao-surface-panel text-center py-8">
          <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
            조건에 맞는 작업이 없습니다. 새 작업을 생성하거나 검색 필터를 초기화해 주세요.
          </p>
        </div>
      ) : layoutMode === "kanban" ? (
        <section className="chrome-kanban-board" aria-label="작업 칸반 보드">
          {TASK_GROUPS.map((group) => {
            const items = groupedTasks.get(group.key) ?? [];
            return (
              <div
                className={`chrome-kanban-col chrome-kanban-col-${group.key}`}
                key={group.key}
                data-drop-active={dropColumnId === group.key ? "true" : undefined}
                onDragOver={(event) => {
                  if (!canManage || !onUpdateTaskStatus || draggedTaskId == null) return;
                  event.preventDefault();
                  if (dropColumnId !== group.key) {
                    setDropColumnId(group.key);
                  }
                }}
                onDragLeave={() => {
                  if (dropColumnId === group.key) {
                    setDropColumnId(null);
                  }
                }}
                onDrop={(event) => handleColumnDrop(event, group.key, group.statuses)}
              >
                <div className="chrome-kanban-col-header">
                  <span>{group.label}</span>
                  <span className="role-badge">{items.length}</span>
                </div>
                {items.length === 0 ? (
                  <p className="m-0 py-4 text-center text-[12px] text-[var(--ui-text-muted)]">
                    항목 없음
                  </p>
                ) : (
                  <div className="flex flex-col gap-2">{items.map(renderTaskCard)}</div>
                )}
              </div>
            );
          })}
        </section>
      ) : (
        <section className="flex flex-col gap-2" aria-label="작업 목록">
          {filteredTasks.map(renderTaskListRow)}
        </section>
      )}
    </div>
  );
}
