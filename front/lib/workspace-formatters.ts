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

const TASK_TYPE_LABELS: Readonly<Record<string, string>> = {
  CODE_REVIEW: "코드 리뷰",
  PR_REVIEW: "PR 검토",
  BUG_FIX: "버그 수정",
  FEATURE_IMPLEMENTATION: "기능 구현",
  FEATURE: "기능 구현",
  CODE_CHANGE: "코드 변경",
  REFACTORING: "리팩토링",
  REFACTOR: "리팩토링",
  TEST_CREATION: "테스트 작성",
  DOCUMENTATION: "문서 작성",
  DOCS: "문서 정리",
  PLANNING: "설계·기획",
  PR_CREATION: "PR 생성",
  DEPLOY: "배포 점검",
  SECURITY: "보안 점검",
  OTHER: "기타",
};

const MAX_UNKNOWN_TASK_TYPE_CHARS = 32;

export function formatTaskType(type?: string | null): string {
  const normalized = type?.trim();
  if (!normalized) {
    return "일반 작업";
  }
  const upper = normalized.toUpperCase();
  if (TASK_TYPE_LABELS[upper]) {
    return TASK_TYPE_LABELS[upper];
  }
  return normalized.slice(0, MAX_UNKNOWN_TASK_TYPE_CHARS).replaceAll("_", " ");
}

export type AgentQuickPrompt = {
  label: string;
  prompt: string;
};

export function buildAgentQuickPrompts(
  category: WorkspaceAgent["category"] | string,
  agentName: string,
): ReadonlyArray<AgentQuickPrompt> {
  const safeName = agentName.trim() || "에이전트";
  switch (category) {
    case "ORCHESTRATOR":
      return [
        {
          label: "진행 작업 요약",
          prompt: `${safeName}, 현재 진행 중인 작업과 병목 단계를 한눈에 요약해 주세요.`,
        },
        {
          label: "단계 의존성 점검",
          prompt: "오케스트레이션 DAG 단계 간 누락된 선행 조건이나 순환 의존성이 있는지 확인해 주세요.",
        },
        {
          label: "승인 대기 검토",
          prompt: "승인 대기 중인 작업의 핵심 변경 사항과 우선 처리 대상을 정리해 주세요.",
        },
      ];
    case "BACKEND":
      return [
        {
          label: "쿼리·인덱스 점검",
          prompt: `${safeName}, 최근 백엔드 엔드포인트에서 N+1 조회나 누락된 인덱스가 있는지 검토해 주세요.`,
        },
        {
          label: "보안 경계 확인",
          prompt: "외부 입력 크기 제한과 Fail-Closed 예외 처리가 빠진 경로가 있는지 확인해 주세요.",
        },
        {
          label: "단위 테스트 작성",
          prompt: "현재 할당된 백엔드 작업에 대해 경계값과 오류 경로 단위 테스트를 보강해 주세요.",
        },
      ];
    case "FRONTEND":
      return [
        {
          label: "토큰·테마 대조",
          prompt: `${safeName}, 라이트/다크 양쪽 테마에서 대비가 낮거나 깨진 레이아웃이 없는지 점검해 주세요.`,
        },
        {
          label: "접근성·키보드 확인",
          prompt: "대화상자 포커스 트랩, 탭 키보드 탐색, ARIA 라벨 누락 여부를 확인해 주세요.",
        },
        {
          label: "렌더링 성능 개선",
          prompt: "불필요한 전체 리렌더링이나 숨김 탭 타이머 점유를 줄일 수 있는 지점을 찾아 주세요.",
        },
      ];
    case "QA":
      return [
        {
          label: "회귀 체크 실행",
          prompt: `${safeName}, 최근 변경 범위에 대한 린트·단위 테스트·브라우저 회귀 시나리오를 점검해 주세요.`,
        },
        {
          label: "경계 입력 검증",
          prompt: "빈 값, 길이 초과, 비정상 JSON 및 권한 오류 시 Fail-Closed 동작을 확인해 주세요.",
        },
        {
          label: "실행 리포트 요약",
          prompt: "현재 작업의 실패 원인과 재현 절차를 검증 리포트 형식으로 정리해 주세요.",
        },
      ];
    default:
      return [
        {
          label: "현재 상태 보고",
          prompt: `${safeName}, 현재 맡은 작업의 진행률과 다음 실행 계획을 요약해 주세요.`,
        },
        {
          label: "이슈 원인 분석",
          prompt: "최근 실행 로그에서 오류 또는 지연이 발생한 지점의 원인을 분석해 주세요.",
        },
        {
          label: "산출물 초안 작성",
          prompt: "완료된 분석 결과를 워크스페이스 산출물 마크다운 문서로 정리해 주세요.",
        },
      ];
  }
}
