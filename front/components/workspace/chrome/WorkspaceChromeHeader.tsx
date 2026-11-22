"use client";

import Link from "next/link";
import {
  ArrowLeft,
  CheckSquare,
  Command,
  FolderTree,
  GitFork,
  HelpCircle,
  LayoutGrid,
  ListChecks,
  Loader2,
  Moon,
  PanelLeft,
  PanelRight,
  Plug,
  Plus,
  RefreshCw,
  RotateCcw,
  Settings,
  Sun,
  UserPlus,
} from "lucide-react";
import type { WorkspaceViewId } from "@/components/workspace/workspace-ui-helpers";
import type { ThemeMode } from "@/lib/theme";
import { formatCommandShortcut, type ShortcutStyle } from "@/lib/workspace-shortcuts";
import type { WorkspaceRole } from "@/lib/workspaces";

export type WorkspaceChromeHeaderProps = {
  workspaceId: number;
  workspaceName: string;
  myRole: WorkspaceRole;
  activeView: WorkspaceViewId;
  waitingApprovalCount: number;
  taskCount: number;
  theme: ThemeMode;
  canManage: boolean;
  isRefreshing: boolean;
  isRecoveringStale: boolean;
  hasInspector: boolean;
  mobileSidebarOpen: boolean;
  sidebarCollapsed?: boolean;
  shortcutStyle?: ShortcutStyle;
  onSelectView: (view: WorkspaceViewId) => void;
  onOpenTaskCreate: () => void;
  onOpenHireAgent: () => void;
  onOpenConnections: () => void;
  onOpenCommandPalette: () => void;
  onOpenShortcuts?: () => void;
  onRefreshWorkspace: () => void;
  onRecoverStaleTasks: () => void;
  onToggleTheme: () => void;
  onToggleMobileSidebar: () => void;
  onToggleMobileInspector: () => void;
  onToggleSidebar?: () => void;
};

type ViewTabSpec = {
  id: WorkspaceViewId;
  label: string;
  icon: "office" | "tasks" | "approvals" | "orchestrations" | "artifacts";
};

const VIEW_TABS: ViewTabSpec[] = [
  { id: "office", label: "오피스", icon: "office" },
  { id: "tasks", label: "작업 보드", icon: "tasks" },
  { id: "approvals", label: "승인 대기함", icon: "approvals" },
  { id: "orchestrations", label: "오케스트레이션", icon: "orchestrations" },
  { id: "artifacts", label: "산출물 및 변경", icon: "artifacts" },
];

function renderViewIcon(icon: ViewTabSpec["icon"]) {
  switch (icon) {
    case "office":
      return <LayoutGrid size={14} aria-hidden="true" />;
    case "tasks":
      return <ListChecks size={14} aria-hidden="true" />;
    case "approvals":
      return <CheckSquare size={14} aria-hidden="true" />;
    case "orchestrations":
      return <GitFork size={14} aria-hidden="true" />;
    case "artifacts":
      return <FolderTree size={14} aria-hidden="true" />;
  }
}

export function WorkspaceChromeHeader({
  workspaceId,
  workspaceName,
  myRole,
  activeView,
  waitingApprovalCount,
  taskCount,
  theme,
  canManage,
  isRefreshing,
  isRecoveringStale,
  hasInspector,
  mobileSidebarOpen,
  sidebarCollapsed = false,
  shortcutStyle = "browser-mac",
  onSelectView,
  onOpenTaskCreate,
  onOpenHireAgent,
  onOpenConnections,
  onOpenCommandPalette,
  onOpenShortcuts,
  onRefreshWorkspace,
  onRecoverStaleTasks,
  onToggleTheme,
  onToggleMobileSidebar,
  onToggleMobileInspector,
  onToggleSidebar,
}: WorkspaceChromeHeaderProps) {
  return (
    <header className="chrome-header">
      <div className="chrome-menu-bar">
        <div className="chrome-menu-group">
          <button
            className="chrome-menu-item chrome-mobile-only"
            type="button"
            onClick={onToggleMobileSidebar}
            aria-expanded={mobileSidebarOpen}
            aria-label="탐색 사이드바 전환"
            title="탐색 사이드바 전환"
          >
            <PanelLeft size={14} aria-hidden="true" />
            <span>목록</span>
          </button>

          {onToggleSidebar ? (
            <button
              className="chrome-menu-item chrome-desktop-only"
              type="button"
              onClick={onToggleSidebar}
              aria-expanded={!sidebarCollapsed}
              aria-label={sidebarCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
              title={sidebarCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
            >
              <PanelLeft size={14} aria-hidden="true" />
            </button>
          ) : null}

          <Link className="chrome-menu-item" href="/workspaces" title="워크스페이스 목록으로 이동">
            <ArrowLeft size={13} aria-hidden="true" />
            <span>워크스페이스 목록</span>
          </Link>

          <span className="chrome-menu-title" title={workspaceName}>
            {workspaceName}
          </span>
          <span className="role-badge">{myRole}</span>
        </div>

        <nav className="chrome-menu-group" aria-label="워크스페이스 빠른 메뉴">
          <button
            className="chrome-menu-item"
            type="button"
            onClick={onOpenConnections}
            title="외부 연동 및 게이트웨이 설정"
          >
            <Plug size={13} aria-hidden="true" />
            <span>연결 설정</span>
          </button>

          <Link
            className="chrome-menu-item"
            href={`/workspaces/${workspaceId}/settings`}
            title="워크스페이스 설정"
          >
            <Settings size={13} aria-hidden="true" />
            <span>설정</span>
          </Link>

          <button
            className="chrome-menu-item"
            type="button"
            onClick={onOpenCommandPalette}
            aria-label="명령 팔레트 열기"
            title={`명령 팔레트 열기 (${formatCommandShortcut(shortcutStyle, "K")})`}
          >
            <Command size={13} aria-hidden="true" />
            <span>명령</span>
            <kbd className="chrome-shortcut-kbd">{formatCommandShortcut(shortcutStyle, "K")}</kbd>
          </button>

          {onOpenShortcuts ? (
            <button
              className="chrome-menu-item"
              type="button"
              onClick={onOpenShortcuts}
              aria-label="키보드 단축키 안내"
              title="키보드 단축키 안내 (?)"
            >
              <HelpCircle size={13} aria-hidden="true" />
              <kbd className="chrome-shortcut-kbd">?</kbd>
            </button>
          ) : null}

          <button
            className="chrome-menu-item"
            type="button"
            onClick={onToggleTheme}
            aria-label={theme === "dark" ? "라이트 테마로 전환" : "다크 테마로 전환"}
            title={theme === "dark" ? "라이트 테마로 전환" : "다크 테마로 전환"}
          >
            {theme === "dark" ? (
              <Sun size={13} aria-hidden="true" />
            ) : (
              <Moon size={13} aria-hidden="true" />
            )}
            <span>{theme === "dark" ? "라이트" : "다크"}</span>
          </button>

          {hasInspector ? (
            <button
              className="chrome-menu-item chrome-mobile-only"
              type="button"
              onClick={onToggleMobileInspector}
              aria-label="상세 인스펙터 열기"
            >
              <PanelRight size={14} aria-hidden="true" />
              <span>상세</span>
            </button>
          ) : null}
        </nav>
      </div>

      <div className="chrome-toolbar">
        <div className="chrome-toolbar-tabs" role="tablist" aria-label="워크스페이스 작업 뷰">
          {VIEW_TABS.map((tab) => {
            const active = activeView === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={active}
                className={`chrome-view-tab${active ? " chrome-view-tab-active" : ""}`}
                onClick={() => onSelectView(tab.id)}
              >
                {renderViewIcon(tab.icon)}
                <span>{tab.label}</span>
                {tab.id === "tasks" && taskCount > 0 ? (
                  <span className="role-badge">{taskCount}</span>
                ) : null}
                {tab.id === "approvals" && waitingApprovalCount > 0 ? (
                  <span className="chrome-view-tab-badge">{waitingApprovalCount}</span>
                ) : null}
              </button>
            );
          })}
        </div>

        <div className="chrome-toolbar-actions">
          <button
            className="tb-btn tb-btn-primary"
            type="button"
            onClick={onOpenTaskCreate}
            title="새 작업 생성"
          >
            <Plus size={14} aria-hidden="true" />
            <span>새 작업</span>
          </button>

          {canManage ? (
            <button
              className="tb-btn"
              type="button"
              onClick={onOpenHireAgent}
              title="새 AI 에이전트 추가"
            >
              <UserPlus size={14} aria-hidden="true" />
              <span>에이전트 추가</span>
            </button>
          ) : null}

          {canManage ? (
            <button
              className="tb-btn"
              type="button"
              onClick={onRecoverStaleTasks}
              disabled={isRecoveringStale}
              title="멈춘 진행 중 작업을 복구합니다"
            >
              {isRecoveringStale ? (
                <Loader2 size={14} className="spin" aria-hidden="true" />
              ) : (
                <RotateCcw size={14} aria-hidden="true" />
              )}
              <span>{isRecoveringStale ? "복구 중" : "고착 복구"}</span>
            </button>
          ) : null}

          <span className="tb-sep" aria-hidden="true" />

          <button
            className="tb-btn"
            type="button"
            onClick={onRefreshWorkspace}
            disabled={isRefreshing}
            aria-label="워크스페이스 새로고침"
            title="워크스페이스 데이터 새로고침"
          >
            <RefreshCw size={14} className={isRefreshing ? "spin" : undefined} aria-hidden="true" />
            <span>새로고침</span>
          </button>
        </div>
      </div>
    </header>
  );
}
