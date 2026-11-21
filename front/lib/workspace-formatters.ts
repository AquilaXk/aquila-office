import type { WorkspaceAgent } from "./agents-utils.ts";
import type { ChatMessage } from "./chat.ts";
import type {
  OrchestrationSessionStatus,
  OrchestrationStepStatus,
} from "./orchestrations.ts";
import type {
  WorkspacePresenceMessage,
  WorkspacePresencePosition,
} from "./presence.ts";
import type { PresenceConnectionState } from "./presence-summary.ts";
import type { TaskPriority, TaskStatus } from "./tasks-utils.ts";

export function formatAgentCategory(category: WorkspaceAgent["category"]): string {
  switch (category) {
    case "ORCHESTRATOR":
      return "오케스트레이터";
    case "BACKEND":
      return "백엔드";
    case "FRONTEND":
      return "프론트엔드";
    case "QA":
      return "QA";
    case "CUSTOM":
      return "커스텀";
  }
}

export function formatAgentStatus(status: WorkspaceAgent["status"]): string {
  switch (status) {
    case "CREATING":
      return "생성 중";
    case "READY":
      return "준비됨";
    case "SYNC_FAILED":
      return "동기화 실패";
    case "ERROR":
      return "오류";
    case "DISABLED":
      return "비활성";
  }
}

export function formatTaskStatus(status: TaskStatus): string {
  switch (status) {
    case "REQUESTED":
      return "요청됨";
    case "ASSIGNED":
      return "배정됨";
    case "IN_PROGRESS":
      return "진행 중";
    case "WAITING_USER":
      return "승인 대기";
    case "COMPLETED":
      return "완료";
    case "FAILED":
      return "실패";
    case "CANCELED":
      return "취소";
  }
}

export function formatTaskPriority(priority?: TaskPriority): string {
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

export function formatChatRole(role: ChatMessage["role"]): string {
  if (role === "USER") {
    return "사용자";
  }
  if (role === "ASSISTANT") {
    return "에이전트";
  }
  return "시스템";
}

export function formatPresenceConnectionState(state: PresenceConnectionState): string {
  if (state === "online") {
    return "연결됨";
  }
  if (state === "connecting") {
    return "연결 중";
  }
  if (state === "error") {
    return "오류";
  }
  return "대기";
}

export function formatPresencePosition(position: WorkspacePresencePosition | null): string {
  if (!position) {
    return "좌표 없음";
  }
  return `${position.left} · ${position.top}`;
}

export function formatPresenceEventType(type: WorkspacePresenceMessage["type"]): string {
  switch (type) {
    case "presence.snapshot":
      return "스냅샷";
    case "presence.join":
      return "입장";
    case "presence.leave":
      return "퇴장";
    case "presence.position":
      return "이동";
    case "presence.error":
      return "오류";
  }
}

export function formatWorkspaceDetailReadiness(readiness: "empty" | "ready" | "attention"): string {
  switch (readiness) {
    case "attention":
      return "주의 필요";
    case "empty":
      return "준비 중";
    case "ready":
      return "운영 가능";
  }
}

export function formatWorkspaceDetailActionState(state: "ready" | "empty" | "locked"): string {
  switch (state) {
    case "empty":
      return "대기";
    case "locked":
      return "잠김";
    case "ready":
      return "준비";
  }
}

export function formatOrchestrationStatus(status: OrchestrationSessionStatus): string {
  switch (status) {
    case "PLANNED":
      return "계획됨";
    case "RUNNING":
      return "실행 중";
    case "SUCCEEDED":
      return "완료";
    case "FAILED":
      return "실패";
    case "CANCELED":
      return "취소됨";
  }
}

export function formatOrchestrationStepStatus(status: OrchestrationStepStatus): string {
  switch (status) {
    case "PENDING":
      return "대기";
    case "DISPATCHED":
      return "전달됨";
    case "RUNNING":
      return "실행 중";
    case "SUCCEEDED":
      return "완료";
    case "FAILED":
      return "실패";
    case "SKIPPED":
      return "건너뜀";
  }
}

export function formatDurationMs(durationMs: number | null | undefined): string {
  if (typeof durationMs !== "number" || !Number.isFinite(durationMs) || durationMs <= 0) {
    return "-";
  }
  if (durationMs < 1000) {
    return `${Math.round(durationMs)}ms`;
  }
  const seconds = durationMs / 1000;
  if (seconds < 60) {
    return `${seconds.toFixed(1)}초`;
  }
  const minutes = Math.floor(seconds / 60);
  const remSeconds = Math.round(seconds % 60);
  return `${minutes}분 ${remSeconds}초`;
}

export function formatTokenUsageSummary(
  todayUsedTokens: number | undefined,
  dailyTokenLimit: number | null | undefined,
): {
  used: number;
  limit: number | null;
  percent: number | null;
  label: string;
} {
  const used =
    typeof todayUsedTokens === "number" && Number.isFinite(todayUsedTokens) && todayUsedTokens >= 0
      ? Math.trunc(todayUsedTokens)
      : 0;
  const limit =
    typeof dailyTokenLimit === "number" && Number.isFinite(dailyTokenLimit) && dailyTokenLimit > 0
      ? Math.trunc(dailyTokenLimit)
      : null;
  if (limit === null) {
    return {
      used,
      limit: null,
      percent: null,
      label: `${used.toLocaleString("ko-KR")} / 무제한`,
    };
  }
  const percent = Math.min(100, Math.round((used / limit) * 100));
  return {
    used,
    limit,
    percent,
    label: `${used.toLocaleString("ko-KR")} / ${limit.toLocaleString("ko-KR")} (${percent}%)`,
  };
}

export function formatFileSize(bytes: number): string {
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

export function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}
