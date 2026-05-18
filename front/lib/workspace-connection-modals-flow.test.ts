import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const cssSource = readSource("app/globals.css");

test("workspace scene connections opens selector and individual modals", () => {
  assert.match(detailSource, /ConnectionModalKind/);
  assert.match(detailSource, /workspace-connection-selector-modal/);
  assert.match(detailSource, /aria-label="Connection settings"/);
  assert.match(detailSource, /Slack/);
  assert.match(detailSource, /Discord/);
  assert.match(detailSource, /GitHub/);
  assert.match(detailSource, /Gateway/);
});

test("slack modal handles install state list delete and pending return storage", () => {
  assert.match(detailSource, /fetchSlackInstallUrl/);
  assert.match(detailSource, /fetchSlackIntegrations/);
  assert.match(detailSource, /deleteSlackIntegration/);
  assert.match(detailSource, /aquila-office\.pendingSlackWorkspaceId/);
  assert.match(detailSource, /aquila-office\.pendingSlackReturnTo/);
  assert.match(detailSource, /slackConnectionMutation/);
});

test("github modal handles create edit delete selected credential and token reset", () => {
  assert.match(detailSource, /createGithubCredential/);
  assert.match(detailSource, /updateGithubCredential/);
  assert.match(detailSource, /deleteGithubCredential/);
  assert.match(detailSource, /selectedGithubCredential/);
  assert.match(detailSource, /setGithubTokenInput\(""\)/);
  assert.match(detailSource, /setGithubEditTokenInput\(""\)/);
});

test("gateway modal handles local fallback status list test and binding", () => {
  assert.match(detailSource, /fetchWorkspaceGatewayStatus/);
  assert.match(detailSource, /testWorkspaceGatewayConnection/);
  assert.match(detailSource, /createWorkspaceGatewayBinding/);
  assert.match(detailSource, /aquila-office\.gatewayUrl/);
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
