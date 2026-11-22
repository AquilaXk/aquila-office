"use client";

import { useMemo } from "react";
import type { WorkspaceAgent } from "@/lib/agents";
import type { WorkspaceCommandItem } from "@/components/workspace/WorkspaceCommandPalette";
import type { WorkspaceViewId } from "@/components/workspace/workspace-ui-helpers";
import type { PresenceMember } from "@/lib/presence-summary";
import type { WorkspaceTask } from "@/lib/tasks";
import type { ThemeMode } from "@/lib/theme";
import {
  createWorkspaceStageActors,
  parseStagePct,
  type StageActorPosition,
  type WorkspaceSceneNode,
} from "@/lib/workspace-2d";
import type { WorkspaceMember } from "@/lib/workspaces";

export function useWorkspaceCommandItems(params: {
  agents: WorkspaceAgent[];
  members: WorkspaceMember[];
  presenceMembers: PresenceMember[];
  tasks: WorkspaceTask[];
  sidebarCollapsed: boolean;
  themeMode: ThemeMode;
  onSelectView: (view: WorkspaceViewId) => void;
  onOpenCreateTask: () => void;
  onOpenHireAgent: () => void;
  onOpenConnections: () => void;
  onToggleSidebar: () => void;
  onToggleTheme: () => void;
  onOpenShortcuts: () => void;
  onSelectOfficeView: () => void;
  onFocusCamera: (position: StageActorPosition) => void;
  onSelectActor: (node: WorkspaceSceneNode) => void;
}): WorkspaceCommandItem[] {
  const {
    agents,
    members,
    presenceMembers,
    tasks,
    sidebarCollapsed,
    themeMode,
    onSelectView,
    onOpenCreateTask,
    onOpenHireAgent,
    onOpenConnections,
    onToggleSidebar,
    onToggleTheme,
    onOpenShortcuts,
    onSelectOfficeView,
    onFocusCamera,
    onSelectActor,
  } = params;

  return useMemo<WorkspaceCommandItem[]>(() => {
    const items: WorkspaceCommandItem[] = [
      {
        id: "view-office",
        label: "오피스 스테이지 보기",
        description: "2D 가상 오피스 및 참여자 위치",
        keywords: "office stage 2d",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => onSelectView("office"),
      },
      {
        id: "view-tasks",
        label: "작업 보드 열기",
        description: "칸반 보드 및 일일 토큰 예산 현황",
        keywords: "tasks board kanban",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => onSelectView("tasks"),
      },
      {
        id: "view-approvals",
        label: "승인 대기함 열기",
        description: "에이전트 승인 요청 검토",
        keywords: "approvals inbox waiting",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => onSelectView("approvals"),
      },
      {
        id: "view-orchestrations",
        label: "오케스트레이션 열기",
        description: "다중 에이전트 파이프라인 구성 및 실행",
        keywords: "orchestration dag pipeline",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => onSelectView("orchestrations"),
      },
      {
        id: "view-artifacts",
        label: "산출물 및 Git 변경사항 열기",
        description: "파일 트리 미리보기 및 로컬 Diff 조회",
        keywords: "artifacts files git diff",
        category: "navigation",
        categoryLabel: "뷰",
        onSelect: () => onSelectView("artifacts"),
      },
      {
        id: "action-create-task",
        label: "새 작업 생성",
        description: "에이전트 작업 등록 및 즉시 실행",
        keywords: "new task create run",
        category: "action",
        categoryLabel: "동작",
        onSelect: onOpenCreateTask,
      },
      {
        id: "action-hire-agent",
        label: "에이전트 추가",
        description: "새 AI 에이전트 등록 및 스킬 파일 설정",
        keywords: "agent hire create skill",
        category: "action",
        categoryLabel: "동작",
        onSelect: onOpenHireAgent,
      },
      {
        id: "action-connections",
        label: "연결 설정 열기",
        description: "Slack · GitHub · OpenClaw 게이트웨이 설정",
        keywords: "connections slack github gateway",
        category: "action",
        categoryLabel: "동작",
        onSelect: onOpenConnections,
      },
      {
        id: "toggle-sidebar",
        label: sidebarCollapsed ? "탐색 사이드바 펼치기" : "탐색 사이드바 접기",
        description: "데스크톱 좌측 탐색 영역 전환",
        keywords: "sidebar collapse expand toggle",
        category: "navigation",
        categoryLabel: "화면",
        onSelect: onToggleSidebar,
      },
      {
        id: "toggle-theme",
        label: `테마 전환 (현재: ${themeMode === "light" ? "라이트" : "다크"})`,
        description: "라이트/다크 시맨틱 테마 전환",
        keywords: "theme light dark",
        category: "theme",
        categoryLabel: "테마",
        onSelect: onToggleTheme,
      },
      {
        id: "open-shortcuts",
        label: "키보드 단축키 (?)",
        description: "탐색 · 인스펙터 · 대화 단축키 안내 열기",
        keywords: "shortcuts help keyboard ?",
        category: "action",
        categoryLabel: "도움말",
        onSelect: onOpenShortcuts,
      },
    ];

    const stageActors = createWorkspaceStageActors({
      agents,
      members,
      presenceMembers,
      tasks,
    });
    for (const actor of stageActors) {
      items.push({
        id: `actor-${actor.id}`,
        label: actor.label,
        description: actor.taskSummary ?? actor.detail,
        keywords: `${actor.kind} ${actor.status}`,
        category: actor.kind === "agent" ? "agent" : "member",
        categoryLabel: actor.kind === "agent" ? "에이전트" : "구성원",
        onSelect: () => {
          onSelectOfficeView();
          onFocusCamera(actor.position);
          onSelectActor({
            id: actor.id,
            kind: actor.kind,
            label: actor.label,
            detail: actor.detail,
            status: actor.sourceStatus,
            x: parseStagePct(actor.position.left),
            y: parseStagePct(actor.position.top),
          });
        },
      });
    }

    return items;
  }, [
    agents,
    members,
    onFocusCamera,
    onOpenConnections,
    onOpenCreateTask,
    onOpenHireAgent,
    onOpenShortcuts,
    onSelectActor,
    onSelectOfficeView,
    onSelectView,
    onToggleSidebar,
    onToggleTheme,
    presenceMembers,
    sidebarCollapsed,
    tasks,
    themeMode,
  ]);
}
