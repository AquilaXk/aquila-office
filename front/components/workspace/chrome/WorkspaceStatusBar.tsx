"use client";

import { Activity, Coins, PlugZap, Users } from "lucide-react";
import {
  formatPresenceConnectionState,
  formatTokenUsageSummary,
  type GatewayConnectionState,
} from "@/components/workspace/workspace-ui-helpers";
import { gatewayConnectionStatusLabel, gatewayStatusLabel } from "@/lib/gateway-utils";
import type { PresenceConnectionState } from "@/lib/presence-summary";

export type WorkspaceStatusBarProps = {
  presenceConnectionState: PresenceConnectionState;
  onlineCount: number;
  gatewayState: GatewayConnectionState;
  todayUsedTokens?: number;
  dailyTokenLimit?: number | null;
  statusNotice?: string | null;
};

export function WorkspaceStatusBar({
  presenceConnectionState,
  onlineCount,
  gatewayState,
  todayUsedTokens,
  dailyTokenLimit,
  statusNotice,
}: WorkspaceStatusBarProps) {
  const tokenSummary = formatTokenUsageSummary(todayUsedTokens, dailyTokenLimit);

  let gatewayText = "확인 중";
  if (gatewayState.status === "ready") {
    gatewayText = gatewayState.gateway.lastStatus
      ? gatewayConnectionStatusLabel(gatewayState.gateway.lastStatus)
      : gatewayStatusLabel(gatewayState.gateway.status);
  } else if (gatewayState.status === "error") {
    gatewayText = "오류";
  }

  return (
    <footer className="chrome-statusbar" aria-label="워크스페이스 상태 표시줄">
      <div className="chrome-statusbar-group">
        <span className="chrome-statusbar-item">
          <Activity size={12} aria-hidden="true" />
          <span>실시간 {formatPresenceConnectionState(presenceConnectionState)}</span>
        </span>
        <span className="chrome-statusbar-item">
          <Users size={12} aria-hidden="true" />
          <span>참여자 {onlineCount}명</span>
        </span>
        <span className="chrome-statusbar-item">
          <PlugZap size={12} aria-hidden="true" />
          <span>게이트웨이 {gatewayText}</span>
        </span>
      </div>

      <div className="chrome-statusbar-group">
        {statusNotice ? (
          <span className="chrome-statusbar-item text-[var(--accent-primary)]">{statusNotice}</span>
        ) : null}
        <span className="chrome-statusbar-item">
          <Coins size={12} aria-hidden="true" />
          <span>일일 토큰 {tokenSummary.label}</span>
        </span>
      </div>
    </footer>
  );
}
