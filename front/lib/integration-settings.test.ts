import assert from "node:assert/strict";
import test from "node:test";
import {
  createIntegrationSettingsActions,
  createIntegrationSettingsOverview,
} from "./integration-settings.ts";
import type { GithubCredential } from "./github.ts";
import type { WorkspaceGatewayStatus } from "./gateway.ts";
import type { SlackChannelMapping, SlackIntegration } from "./slack.ts";

const connectedGateway = {
  status: "BOUND",
  bound: true,
  bindingId: 44,
  mode: "EXTERNAL",
  gatewayUrl: "https://gateway.example.com",
  maskedToken: "gate****prod",
  lastStatus: "CONNECTED",
  lastCheckedAt: "2026-05-18T08:00:00.000Z",
  lastError: null,
  updatedAt: "2026-05-18T08:01:00.000Z",
} satisfies WorkspaceGatewayStatus;

const githubCredentials = [
  {
    credentialId: 7,
    workspaceId: 3,
    displayName: "ops credential",
    maskedToken: "ghp_****prod",
  },
] satisfies GithubCredential[];

const slackIntegrations = [
  {
    id: 9,
    workspaceId: 3,
    slackTeamId: "T01",
    slackChannelId: "C01",
    channelName: "ops",
    maskedBotToken: "xoxb****prod",
    createdByGoogleSub: "google-sub",
    active: true,
  },
] satisfies SlackIntegration[];

const slackChannelMappings = [
  {
    mappingId: 11,
    workspaceId: 3,
    teamId: "T01",
    channelId: "C02",
    channelName: "deploy",
    maskedBotToken: "xoxb****map",
    createdByGoogleSub: "google-sub",
    active: true,
  },
] satisfies SlackChannelMapping[];

test("integration settings overview summarizes connected services", () => {
  const overview = createIntegrationSettingsOverview({
    githubCredentials,
    slackIntegrations,
    slackChannelMappings,
    gatewayStatus: connectedGateway,
  });

  assert.equal(overview.readiness, "ready");
  assert.equal(overview.connectedCount, 4);
  assert.equal(overview.totalCount, 4);
  assert.deepEqual(
    overview.sections.map((section) => [section.id, section.state, section.count]),
    [
      ["slack", "ready", 1],
      ["github", "ready", 1],
      ["gateway", "ready", 1],
      ["channel-mapping", "ready", 1],
    ],
  );
});

test("integration settings overview exposes empty and attention states", () => {
  const overview = createIntegrationSettingsOverview({
    githubCredentials: [],
    slackIntegrations: [],
    slackChannelMappings: [],
    gatewayStatus: { ...connectedGateway, status: "UNBOUND", bound: false, bindingId: null },
  });

  assert.equal(overview.readiness, "empty");
  assert.equal(overview.connectedCount, 0);
  assert.match(overview.summary, /0\/4/);
  assert.equal(overview.sections[2]?.state, "attention");
  assert.equal(overview.sections[2]?.detail, "gateway binding이 필요합니다.");
});

test("integration settings actions expose expected route and form entries", () => {
  const actions = createIntegrationSettingsActions({
    githubCredentials,
    slackIntegrations: [],
    slackChannelMappings,
    gatewayStatus: connectedGateway,
  });

  assert.deepEqual(
    actions.map((action) => [action.id, action.state, action.target]),
    [
      ["slack-install", "attention", "oauth"],
      ["github-credential", "ready", "form"],
      ["gateway-binding", "ready", "form"],
      ["channel-mapping", "ready", "form"],
    ],
  );
});
