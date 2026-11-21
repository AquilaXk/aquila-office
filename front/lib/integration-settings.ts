export type IntegrationSettingsReadiness = "empty" | "partial" | "ready";

export type IntegrationSettingsSectionId =
  | "slack"
  | "github"
  | "gateway"
  | "channel-mapping";

export type IntegrationSettingsSectionState = "empty" | "attention" | "ready";

export type IntegrationSettingsActionId =
  | "slack-install"
  | "discord-status"
  | "github-credential"
  | "gateway-binding"
  | "channel-mapping";

export type IntegrationSettingsActionTarget = "oauth" | "form" | "status";

export type IntegrationSettingsListItem = {
  active?: boolean;
};

export type IntegrationSettingsGateway = {
  status: string;
  bound: boolean;
  gatewayUrl: string | null;
  maskedToken: string | null;
  lastStatus: string | null;
};

export type IntegrationSettingsOverviewInput = {
  githubCredentials: unknown[];
  slackIntegrations: IntegrationSettingsListItem[];
  slackChannelMappings: IntegrationSettingsListItem[];
  gatewayStatus: IntegrationSettingsGateway | null;
};

export type IntegrationSettingsOverviewSection = {
  id: IntegrationSettingsSectionId;
  label: string;
  count: number;
  state: IntegrationSettingsSectionState;
  detail: string;
};

export type IntegrationSettingsOverview = {
  readiness: IntegrationSettingsReadiness;
  connectedCount: number;
  totalCount: number;
  summary: string;
  sections: IntegrationSettingsOverviewSection[];
};

export type IntegrationSettingsAction = {
  id: IntegrationSettingsActionId;
  label: string;
  detail: string;
  state: IntegrationSettingsSectionState;
  target: IntegrationSettingsActionTarget;
};

const TOTAL_INTEGRATION_SECTIONS = 4;

export function createIntegrationSettingsOverview(
  input: IntegrationSettingsOverviewInput,
): IntegrationSettingsOverview {
  const activeSlackIntegrations = countActive(input.slackIntegrations);
  const githubCredentialCount = input.githubCredentials.length;
  const activeChannelMappings = countActive(input.slackChannelMappings);
  const gatewayConnected = Boolean(
    input.gatewayStatus?.bound && input.gatewayStatus.status === "BOUND",
  );

  const sections: IntegrationSettingsOverviewSection[] = [
    {
      id: "slack",
      label: "Slack",
      count: activeSlackIntegrations,
      state: activeSlackIntegrations > 0 ? "ready" : "empty",
      detail:
        activeSlackIntegrations > 0
          ? `${activeSlackIntegrations}개 채널 연동`
          : "Slack 설치 또는 수동 등록이 필요합니다.",
    },
    {
      id: "github",
      label: "GitHub",
      count: githubCredentialCount,
      state: githubCredentialCount > 0 ? "ready" : "empty",
      detail:
        githubCredentialCount > 0
          ? `${githubCredentialCount}개 자격 증명`
          : "GitHub token 등록이 필요합니다.",
    },
    {
      id: "gateway",
      label: "Gateway",
      count: gatewayConnected ? 1 : 0,
      state: gatewayConnected ? "ready" : "attention",
      detail: gatewayConnected
        ? input.gatewayStatus?.gatewayUrl ?? "gateway binding 등록됨"
        : "gateway binding이 필요합니다.",
    },
    {
      id: "channel-mapping",
      label: "Channel Mapping",
      count: activeChannelMappings,
      state: activeChannelMappings > 0 ? "ready" : "empty",
      detail:
        activeChannelMappings > 0
          ? `${activeChannelMappings}개 채널 매핑`
          : "Slack 채널 매핑이 필요합니다.",
    },
  ];

  const connectedCount = sections.filter((section) => section.state === "ready").length;

  return {
    readiness: readinessFor(connectedCount),
    connectedCount,
    totalCount: TOTAL_INTEGRATION_SECTIONS,
    summary: `${connectedCount}/${TOTAL_INTEGRATION_SECTIONS} 연동 준비`,
    sections,
  };
}

export function createIntegrationSettingsActions(
  input: IntegrationSettingsOverviewInput,
): IntegrationSettingsAction[] {
  const overview = createIntegrationSettingsOverview(input);
  const sectionById = new Map(overview.sections.map((section) => [section.id, section]));

  return [
    {
      id: "slack-install",
      label: "Slack 설치",
      detail: sectionById.get("slack")?.detail ?? "Slack 설정을 확인합니다.",
      state: sectionById.get("slack")?.state === "ready" ? "ready" : "attention",
      target: "oauth",
    },
    {
      id: "discord-status",
      label: "Discord 준비 중",
      detail: "Discord 연결은 같은 설정 흐름에서 상태를 확인합니다.",
      state: "attention",
      target: "status",
    },
    {
      id: "github-credential",
      label: "GitHub 자격 증명",
      detail: sectionById.get("github")?.detail ?? "GitHub 설정을 확인합니다.",
      state: sectionById.get("github")?.state ?? "empty",
      target: "form",
    },
    {
      id: "gateway-binding",
      label: "Gateway Binding",
      detail: sectionById.get("gateway")?.detail ?? "Gateway 설정을 확인합니다.",
      state: sectionById.get("gateway")?.state ?? "attention",
      target: "form",
    },
    {
      id: "channel-mapping",
      label: "채널 매핑",
      detail:
        sectionById.get("channel-mapping")?.detail ?? "Slack 채널 매핑을 확인합니다.",
      state: sectionById.get("channel-mapping")?.state ?? "empty",
      target: "form",
    },
  ];
}

function readinessFor(connectedCount: number): IntegrationSettingsReadiness {
  if (connectedCount === 0) {
    return "empty";
  }
  if (connectedCount === TOTAL_INTEGRATION_SECTIONS) {
    return "ready";
  }
  return "partial";
}

function countActive(items: IntegrationSettingsListItem[]): number {
  return items.filter((item) => item.active !== false).length;
}
