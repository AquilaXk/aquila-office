import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace settings page renders desktop chrome, daily token limit, and four sections", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.match(source, /AppTopChrome/);
  assert.doesNotMatch(source, /T4Screen/);
  assert.doesNotMatch(source, /COMMAND DECK/);
  assert.match(source, /워크스페이스 정보/);
  assert.match(source, /구성원 관리/);
  assert.match(source, /초대 생성/);
  assert.match(source, /초대 이력/);
  assert.match(source, /dailyTokenLimit/);
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
