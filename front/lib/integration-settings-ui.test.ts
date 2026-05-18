import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace settings page renders integration overview and action rail", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.match(source, /createIntegrationSettingsOverview/);
  assert.match(source, /createIntegrationSettingsActions/);
  assert.match(source, /integrationSettingsOverview/);
  assert.match(source, /integrationSettingsActions/);
  assert.match(source, /aria-label="연동 설정 요약"/);
  assert.match(source, /workspace-integration-overview/);
  assert.match(source, /workspace-integration-action-grid/);
  assert.match(source, /IntegrationSettingsActionCard/);
});

test("workspace settings styles expose responsive integration cards", () => {
  const source = readSource("app/globals.css");

  assert.match(source, /\.workspace-integration-overview/);
  assert.match(source, /\.workspace-integration-section-grid/);
  assert.match(source, /\.workspace-integration-action-card/);
  assert.match(source, /\.workspace-integration-action-state/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
