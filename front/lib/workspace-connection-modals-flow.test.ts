import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const slackSource = readSource("lib/slack.ts");
const githubSource = readSource("lib/github.ts");
const gatewaySource = readSource("lib/gateway.ts");
const successSource = readSource("app/settings/integrations/success/page.tsx");
const failSource = readSource("app/settings/integrations/fail/page.tsx");
const cssSource = readSource("app/globals.css");

test("workspace scene connections opens selector and individual modals", () => {
  assert.match(detailSource, /ConnectionModalKind/);
  assert.match(detailSource, /workspace-connection-selector-modal/);
  assert.match(detailSource, /workspace-connection-modal-\$\{modal\}/);
  assert.match(detailSource, /aria-label="Connection settings"/);
  assert.match(detailSource, /Slack/);
  assert.match(detailSource, /Discord/);
  assert.match(detailSource, /GitHub/);
  assert.match(detailSource, /AI \/ OpenClaw/);
  assert.match(detailSource, /aria-label="AI \/ OpenClaw"/);
});

test("slack modal handles install state list delete and pending return storage", () => {
  assert.match(detailSource, /fetchSlackInstallUrl/);
  assert.match(detailSource, /fetchSlackIntegrations/);
  assert.match(detailSource, /deleteSlackIntegration/);
  assert.match(slackSource, /SLACK_PENDING_WORKSPACE_STORAGE_KEY = "aio\.slack\.pendingWorkspaceId"/);
  assert.match(slackSource, /SLACK_RETURN_TO_STORAGE_KEY = "aio\.slack\.returnTo"/);
  assert.match(detailSource, /saveSlackPendingConnectionState\(window\.localStorage, numericWorkspaceId\)/);
  assert.match(successSource, /writeStoredSlackIntegration\(window\.localStorage, workspaceId, latestIntegration\)/);
  assert.match(successSource, /clearSlackPendingConnectionState\(window\.localStorage\)/);
  assert.match(failSource, /readSlackReturnTo\(window\.localStorage\)/);
  assert.match(detailSource, /slackConnectionMutation/);
});

test("github modal handles create edit delete selected credential and token reset", () => {
  assert.match(detailSource, /createGithubCredential/);
  assert.match(detailSource, /updateGithubCredential/);
  assert.match(detailSource, /deleteGithubCredential/);
  assert.match(githubSource, /githubIntegrationStorageKey/);
  assert.match(githubSource, /aio\.workspace\.\$\{workspaceId\}\.integrations\.github/);
  assert.match(detailSource, /readStoredGithubCredential\(window\.localStorage, numericWorkspaceId\)/);
  assert.match(detailSource, /writeStoredGithubCredential\(window\.localStorage, numericWorkspaceId/);
  assert.match(detailSource, /deleteStoredGithubCredential\(window\.localStorage, numericWorkspaceId\)/);
  assert.match(detailSource, /selectedGithubCredential/);
  assert.match(detailSource, /setGithubTokenInput\(""\)/);
  assert.match(detailSource, /setGithubEditTokenInput\(""\)/);
});

test("gateway modal handles local fallback status list test and binding", () => {
  assert.match(detailSource, /fetchWorkspaceGatewayStatus/);
  assert.match(detailSource, /testWorkspaceGatewayConnection/);
  assert.match(detailSource, /createWorkspaceGatewayBinding/);
  assert.match(gatewaySource, /openClawIntegrationStorageKey/);
  assert.match(gatewaySource, /aio\.workspace\.\$\{workspaceId\}\.integrations\.openclaw/);
  assert.match(detailSource, /readStoredOpenClawGatewayStatus\(window\.localStorage, numericWorkspaceId\)/);
  assert.match(detailSource, /writeStoredOpenClawGatewayStatus\(window\.localStorage, numericWorkspaceId/);
  assert.match(detailSource, /deleteStoredOpenClawGatewayStatus\(window\.localStorage, numericWorkspaceId\)/);
  assert.match(detailSource, /gatewayBindingToStatus/);
  assert.match(detailSource, /OpenClaw Gateway/);
  assert.match(detailSource, /workspace-gateway-status-list/);
});

test("connection modal styles are present", () => {
  assert.match(cssSource, /\.workspace-connection-selector-modal/);
  assert.match(cssSource, /\.workspace-connection-grid/);
  assert.match(cssSource, /\.workspace-connection-modal/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
