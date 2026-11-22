"use client";

import { useMemo, useState } from "react";
import {
  Briefcase,
  Building2,
  FolderOpen,
  Loader2,
  MapPin,
  Plus,
  RefreshCw,
  Users,
} from "lucide-react";
import PixelAvatar from "@/components/arcade/PixelAvatar";
import {
  formatAgentStatus,
  formatPresenceConnectionState,
  formatPresenceEventType,
  formatPresencePosition,
  pickAvatarKind,
  type PresenceMutationState,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgent } from "@/lib/agents";
import { searchBitapItems } from "@/lib/bitap-fuzzy-search";
import type { WorkspacePresenceMessage } from "@/lib/presence";
import type { PresenceLiveState } from "@/lib/presence-summary";
import type { WorkspaceTask } from "@/lib/tasks";
import type { WorkspaceMember } from "@/lib/workspaces";

export type OfficeZoneId = "overview" | "work" | "meeting" | "library";

export type WorkspaceLeftSidebarProps = {
  agents: WorkspaceAgent[];
  members: WorkspaceMember[];
  tasks?: WorkspaceTask[];
  selectedAgentId: number | null;
  activeZone: OfficeZoneId;
  mobileOpen: boolean;
  desktopCollapsed?: boolean;
  canManage: boolean;
  presenceLiveState: PresenceLiveState;
  presenceEvents: WorkspacePresenceMessage[];
  presenceMutation: PresenceMutationState;
  isUpdatingPresence: boolean;
  onFocusZone: (zone: OfficeZoneId) => void;
  onSelectAgent: (agentId: number) => void;
  onOpenHireAgent: () => void;
  onUpdatePresencePosition: () => void;
};

const ZONE_ITEMS: Array<{ id: OfficeZoneId; label: string }> = [
  { id: "overview", label: "전체 구역" },
  { id: "work", label: "작업실" },
  { id: "meeting", label: "회의실" },
  { id: "library", label: "자료실" },
];

function renderZoneIcon(id: OfficeZoneId) {
  switch (id) {
    case "overview":
      return <Building2 size={14} aria-hidden="true" />;
    case "work":
      return <Briefcase size={14} aria-hidden="true" />;
    case "meeting":
      return <Users size={14} aria-hidden="true" />;
    case "library":
      return <FolderOpen size={14} aria-hidden="true" />;
  }
}

export function WorkspaceLeftSidebar({
  agents,
  members,
  tasks = [],
  selectedAgentId,
  activeZone,
  mobileOpen,
  desktopCollapsed = false,
  canManage,
  presenceLiveState,
  presenceEvents,
  presenceMutation,
  isUpdatingPresence,
  onFocusZone,
  onSelectAgent,
  onOpenHireAgent,
  onUpdatePresencePosition,
}: WorkspaceLeftSidebarProps) {
  const [agentQuery, setAgentQuery] = useState("");
  const recentEvents = presenceEvents.slice(0, 3);
  const memberNameById = new Map<number, string>();
  for (let i = 0; i < members.length; i += 1) {
    const member = members[i];
    memberNameById.set(member.memberId, member.name || `구성원 #${member.memberId}`);
  }

  const activeTaskCountByAgent = useMemo(() => {
    const counts = new Map<number, number>();
    for (let i = 0; i < tasks.length; i += 1) {
      const task = tasks[i];
      if (
        task.status !== "REQUESTED" &&
        task.status !== "ASSIGNED" &&
        task.status !== "IN_PROGRESS" &&
        task.status !== "WAITING_USER"
      ) {
        continue;
      }
      const rawId = task.assignedAgentId;
      const assignedId = typeof rawId === "string" ? Number.parseInt(rawId, 10) : rawId;
      if (typeof assignedId === "number" && Number.isFinite(assignedId)) {
        counts.set(assignedId, (counts.get(assignedId) ?? 0) + 1);
      }
    }
    return counts;
  }, [tasks]);

  const filteredAgents = useMemo(() => {
    const trimmed = agentQuery.trim();
    if (!trimmed || agents.length < 5) {
      return agents;
    }
    return searchBitapItems(
      agents,
      trimmed,
      (agent) => `${agent.name} ${agent.category}`,
      { maxDistance: 1 },
    ).map((hit) => hit.item);
  }, [agentQuery, agents]);

  return (
    <aside
      className={`chrome-sidebar${mobileOpen ? "" : " chrome-sidebar-collapsed-mobile"}${
        desktopCollapsed ? " chrome-sidebar-collapsed-desktop" : ""
      }`}
      aria-label="워크스페이스 탐색"
    >
      <section className="chrome-sidebar-section" aria-label="오피스 구역">
        <div className="chrome-sidebar-heading">
          <span>오피스 구역</span>
          <MapPin size={13} aria-hidden="true" />
        </div>

        {ZONE_ITEMS.map((zone) => (
          <button
            key={zone.id}
            type="button"
            className={`chrome-sidebar-item${activeZone === zone.id ? " chrome-sidebar-item-active" : ""}`}
            onClick={() => onFocusZone(zone.id)}
          >
            <span className="inline-flex items-center gap-2">
              {renderZoneIcon(zone.id)}
              <span>{zone.label}</span>
            </span>
          </button>
        ))}
      </section>

      <section className="chrome-sidebar-section" aria-label="에이전트 목록">
        <div className="chrome-sidebar-heading">
          <span>에이전트 ({agents.length})</span>
          {canManage ? (
            <button
              type="button"
              className="tb-btn"
              onClick={onOpenHireAgent}
              aria-label="사이드바에서 에이전트 추가"
              title="에이전트 추가"
            >
              <Plus size={13} aria-hidden="true" />
            </button>
          ) : null}
        </div>

        {agents.length >= 5 ? (
          <input
            type="search"
            aria-label="사이드바 에이전트 검색"
            placeholder="에이전트 검색"
            value={agentQuery}
            onChange={(event) => setAgentQuery(event.target.value)}
            className="w-full h-7 px-2 mb-1 rounded-[var(--radius-sm)] border border-[var(--ui-border)] bg-[var(--ui-bg)] text-[12px] text-[var(--ui-text)]"
          />
        ) : null}

        {agents.length === 0 ? (
          <p className="text-[12px] text-[var(--ui-text-muted)] px-1 py-1">
            등록된 에이전트가 없습니다.
          </p>
        ) : filteredAgents.length === 0 ? (
          <p className="text-[12px] text-[var(--ui-text-muted)] px-1 py-1">
            일치하는 에이전트가 없습니다.
          </p>
        ) : (
          filteredAgents.map((agent) => {
            const active = selectedAgentId === agent.agentId;
            const activeTaskCount = activeTaskCountByAgent.get(agent.agentId) ?? 0;
            const statusTone =
              agent.status === "READY"
                ? "ao-tone-ok"
                : agent.status === "CREATING"
                  ? "ao-tone-warn"
                  : agent.status === "SYNC_FAILED" ||
                      agent.status === "ERROR" ||
                      agent.status === "DISABLED"
                    ? "ao-tone-danger"
                    : "ao-tone-info";
            return (
              <button
                key={agent.agentId}
                type="button"
                className={`chrome-sidebar-item${active ? " chrome-sidebar-item-active" : ""}`}
                onClick={() => onSelectAgent(agent.agentId)}
              >
                <span className="inline-flex items-center gap-2 min-w-0">
                  <PixelAvatar
                    kind="agent"
                    variant="mugshot"
                    variantKey={agent.name}
                    agentCategory={agent.category}
                    size={1.6}
                  />
                  <span className="truncate">{agent.name}</span>
                </span>
                <span className="inline-flex items-center gap-1 shrink-0">
                  {activeTaskCount > 0 ? (
                    <span className="role-badge" title={`진행 중인 작업 ${activeTaskCount}건`}>
                      {activeTaskCount}
                    </span>
                  ) : null}
                  <span className={`role-badge ${statusTone}`}>{formatAgentStatus(agent.status)}</span>
                </span>
              </button>
            );
          })
        )}
      </section>

      <section className="chrome-sidebar-section" aria-label="실시간 Presence">
        <div className="chrome-sidebar-heading">
          <span>참여자 ({presenceLiveState.onlineCount}명)</span>
          <span
            className={`role-badge ${
              presenceLiveState.connectionState === "online"
                ? "ao-tone-ok"
                : presenceLiveState.connectionState === "error"
                  ? "ao-tone-danger"
                  : presenceLiveState.connectionState === "connecting"
                    ? "ao-tone-warn"
                    : "ao-tone-info"
            }`}
          >
            {formatPresenceConnectionState(presenceLiveState.connectionState)}
          </span>
        </div>

        <div className="chrome-sidebar-presence-box">
          <p className="text-[12px] text-[var(--ui-text-secondary)] m-0">
            {`${presenceLiveState.onlineCount}/${presenceLiveState.workspaceMemberCount}명 온라인`}
          </p>

          {presenceLiveState.members.length > 0 ? (
            <div className="flex flex-col gap-1.5" aria-label="Presence member positions">
              {presenceLiveState.members.slice(0, 4).map((member) => {
                const memberName =
                  memberNameById.get(member.memberId) ?? `구성원 #${member.memberId}`;
                return (
                  <div
                    key={member.memberId}
                    className="flex items-center justify-between gap-2 text-[12px]"
                  >
                    <span className="inline-flex items-center gap-1.5 min-w-0">
                      <PixelAvatar
                        kind={pickAvatarKind(memberName)}
                        variant="mugshot"
                        variantKey={memberName}
                        size={1.4}
                      />
                      <strong className="truncate font-medium">{memberName}</strong>
                    </span>
                    <span className="text-[var(--ui-text-muted)] shrink-0">
                      {formatPresencePosition(member.position)}
                    </span>
                  </div>
                );
              })}
            </div>
          ) : (
            <span className="text-[12px] text-[var(--ui-text-muted)]">온라인 참여자 없음</span>
          )}

          {recentEvents.length > 0 ? (
            <div className="flex flex-wrap gap-1" aria-label="Presence event log">
              {recentEvents.map((event, index) => (
                <span
                  key={`${event.type}-${event.memberId ?? "ws"}-${index}`}
                  className="role-badge"
                >
                  {formatPresenceEventType(event.type)}
                  {event.memberId ? ` #${event.memberId}` : ""}
                </span>
              ))}
            </div>
          ) : null}

          <button
            type="button"
            className="tb-btn w-full"
            onClick={onUpdatePresencePosition}
            disabled={isUpdatingPresence}
          >
            {isUpdatingPresence ? (
              <Loader2 size={13} className="spin" aria-hidden="true" />
            ) : (
              <RefreshCw size={13} aria-hidden="true" />
            )}
            <span>{isUpdatingPresence ? "갱신 중" : "위치 갱신"}</span>
          </button>

          {presenceMutation.status === "error" && presenceMutation.message ? (
            <span className="text-[12px] text-[var(--status-danger)]" role="alert">
              {presenceMutation.message}
            </span>
          ) : null}
          {presenceMutation.status === "success" && presenceMutation.message ? (
            <span className="text-[12px] text-[var(--status-success)]" role="status">
              {presenceMutation.message}
            </span>
          ) : null}
        </div>
      </section>
    </aside>
  );
}
