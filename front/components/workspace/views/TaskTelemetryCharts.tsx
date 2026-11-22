"use client";

import { useId, useMemo } from "react";
import { Activity, Coins } from "lucide-react";
import { formatTaskStatus } from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceTask } from "@/lib/tasks";
import type { WorkspaceDashboardSummary } from "@/lib/workspaces";

export type TaskTelemetrySeriesPoint = {
  label: string;
  value: number;
  status: string;
};

export type TaskTelemetrySnapshot = {
  points: TaskTelemetrySeriesPoint[];
  completedCount: number;
  runningCount: number;
  waitingCount: number;
  failedCount: number;
  tokenUsagePercent: number | null;
  sparklinePath: string;
  areaPath: string;
};

const SPARK_WIDTH = 240;
const SPARK_HEIGHT = 48;
const SPARK_PAD_X = 6;
const SPARK_PAD_Y = 6;

function statusWeight(status: string): number {
  switch (status) {
    case "COMPLETED":
    case "SUCCEEDED":
    case "DONE":
      return 100;
    case "IN_PROGRESS":
    case "RUNNING":
      return 72;
    case "WAITING_USER":
    case "ASSIGNED":
      return 54;
    case "REQUESTED":
    case "QUEUED":
      return 36;
    case "FAILED":
    case "CANCELED":
      return 18;
    default:
      return 48;
  }
}

export function buildSparklinePaths(values: readonly number[]): {
  linePath: string;
  areaPath: string;
} {
  if (values.length === 0) {
    const midY = SPARK_HEIGHT - SPARK_PAD_Y;
    return {
      linePath: `M ${SPARK_PAD_X} ${midY} L ${SPARK_WIDTH - SPARK_PAD_X} ${midY}`,
      areaPath: "",
    };
  }
  const min = Math.min(...values, 0);
  const max = Math.max(...values, 100);
  const span = Math.max(1, max - min);
  const plotW = SPARK_WIDTH - SPARK_PAD_X * 2;
  const plotH = SPARK_HEIGHT - SPARK_PAD_Y * 2;

  const coords: Array<{ x: number; y: number }> = [];
  for (let i = 0; i < values.length; i += 1) {
    const ratioX = values.length === 1 ? 0.5 : i / (values.length - 1);
    const x = Number((SPARK_PAD_X + ratioX * plotW).toFixed(2));
    const normY = (values[i] - min) / span;
    const y = Number((SPARK_HEIGHT - SPARK_PAD_Y - normY * plotH).toFixed(2));
    coords.push({ x, y });
  }

  const lineParts = coords.map((pt, idx) => `${idx === 0 ? "M" : "L"} ${pt.x} ${pt.y}`);
  if (coords.length === 1) {
    lineParts.push(`L ${SPARK_WIDTH - SPARK_PAD_X} ${coords[0].y}`);
  }
  const linePath = lineParts.join(" ");
  const lastX = coords.length === 1 ? SPARK_WIDTH - SPARK_PAD_X : coords[coords.length - 1].x;
  const firstX = coords[0].x;
  const baselineY = SPARK_HEIGHT - 2;
  const areaPath = `${linePath} L ${lastX} ${baselineY} L ${firstX} ${baselineY} Z`;
  return { linePath, areaPath };
}

export function buildTaskTelemetrySnapshot(
  tasks: readonly WorkspaceTask[],
  summary: WorkspaceDashboardSummary,
): TaskTelemetrySnapshot {
  let completedCount = 0;
  let runningCount = 0;
  let waitingCount = 0;
  let failedCount = 0;

  for (let i = 0; i < tasks.length; i += 1) {
    const status = tasks[i].status;
    if (status === "COMPLETED") completedCount += 1;
    else if (status === "WAITING_USER") waitingCount += 1;
    else if (status === "FAILED" || status === "CANCELED") failedCount += 1;
    else runningCount += 1;
  }

  const recentReports = summary.recentReports ?? [];
  const points: TaskTelemetrySeriesPoint[] = [];

  if (recentReports.length > 0) {
    const slice = recentReports.slice(-16);
    for (let i = 0; i < slice.length; i += 1) {
      const report = slice[i];
      points.push({
        label: `실행 #${report.executionId}`,
        value: statusWeight(report.status),
        status: report.status,
      });
    }
  } else if (tasks.length > 0) {
    const slice = tasks.slice(0, 16).reverse();
    for (let i = 0; i < slice.length; i += 1) {
      const task = slice[i];
      points.push({
        label: `#${task.taskId}`,
        value: statusWeight(task.status),
        status: task.status,
      });
    }
  }

  const { linePath, areaPath } = buildSparklinePaths(points.map((p) => p.value));
  const used = summary.todayUsedTokens ?? 0;
  const limit = summary.dailyTokenLimit ?? null;
  const tokenUsagePercent =
    typeof limit === "number" && limit > 0 ? Math.min(100, Math.round((used / limit) * 100)) : null;

  return {
    points,
    completedCount,
    runningCount,
    waitingCount,
    failedCount,
    tokenUsagePercent,
    sparklinePath: linePath,
    areaPath,
  };
}

export function TaskTelemetryCharts({
  tasks,
  summary,
}: {
  tasks: readonly WorkspaceTask[];
  summary: WorkspaceDashboardSummary;
}) {
  const gradientId = useId().replace(/:/g, "");
  const snapshot = useMemo(() => buildTaskTelemetrySnapshot(tasks, summary), [summary, tasks]);

  return (
    <section className="ao-telemetry-card ao-telemetry-spark-card" aria-label="실행 추이 및 결과 분포">
      <div className="flex flex-wrap items-center justify-between gap-2 text-[13px]">
        <span className="inline-flex items-center gap-2 font-semibold text-[var(--ui-text-strong)]">
          <Activity size={15} aria-hidden="true" />
          <span>최근 실행·작업 추이</span>
        </span>
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="role-badge ao-tone-ok">완료 {snapshot.completedCount}</span>
          <span className="role-badge ao-tone-info">진행 {snapshot.runningCount}</span>
          <span className="role-badge ao-tone-warn">승인 대기 {snapshot.waitingCount}</span>
          <span className="role-badge ao-tone-danger">중단 {snapshot.failedCount}</span>
          {snapshot.tokenUsagePercent !== null ? (
            <span className="role-badge" title="일일 토큰 사용률">
              <Coins size={12} aria-hidden="true" />
              <span>예산 {snapshot.tokenUsagePercent}%</span>
            </span>
          ) : null}
        </div>
      </div>

      <div className="ao-sparkline-frame">
        <svg
          className="ao-sparkline-svg"
          viewBox={`0 0 ${SPARK_WIDTH} ${SPARK_HEIGHT}`}
          role="img"
          aria-label={`최근 작업 및 실행 ${snapshot.points.length}건 추이 스파크라인`}
          preserveAspectRatio="none"
        >
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--chart-1)" stopOpacity="0.28" />
              <stop offset="100%" stopColor="var(--chart-1)" stopOpacity="0.02" />
            </linearGradient>
          </defs>
          {snapshot.areaPath ? <path d={snapshot.areaPath} fill={`url(#${gradientId})`} /> : null}
          <path
            d={snapshot.sparklinePath}
            fill="none"
            stroke="var(--chart-1)"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
          />
        </svg>
        <div className="ao-sparkline-meta">
          <span>
            {snapshot.points.length > 0
              ? `최근 ${snapshot.points.length}건 (${snapshot.points[snapshot.points.length - 1]?.label ?? ""})`
              : "표시할 최근 실행 이력이 없습니다"}
          </span>
          {snapshot.points.length > 0 ? (
            <span className="font-mono">
              최근 상태:{" "}
              {formatTaskStatus(
                snapshot.points[snapshot.points.length - 1]?.status as WorkspaceTask["status"],
              )}
            </span>
          ) : null}
        </div>
      </div>
    </section>
  );
}
