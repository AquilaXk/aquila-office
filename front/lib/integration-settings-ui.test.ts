import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace settings page renders integration overview and action rail", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.match(source, /T4Screen/);
  assert.match(source, /WORKSPACE ADMIN · COMMAND DECK/);
  assert.match(source, /WORKSPACE INFO/);
  assert.match(source, /ROSTER/);
  assert.match(source, /INVITE/);
  assert.match(source, /INVITE LOG/);
  assert.doesNotMatch(source, /createIntegrationSettingsOverview/);
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
