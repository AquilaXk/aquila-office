import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const settingsSource = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

test("workspace detail and settings expose shared navigation routes", () => {
  assert.match(detailSource, /aria-label="워크스페이스 내비게이션"/);
  assert.match(settingsSource, /aria-label="워크스페이스 내비게이션"/);
  assert.match(detailSource, /href="\/workspaces"/);
  assert.match(settingsSource, /href="\/workspaces"/);
  assert.match(detailSource, /<span>Connection settings<\/span>/);
});

test("workspace settings exposes integration entry actions", () => {
  assert.match(settingsSource, /const SHOW_CONNECTION_SETTINGS = true;/);
  assert.match(settingsSource, /activateIntegrationSettingsAction/);

  for (const actionLabel of [
    "Slack 설정으로",
    "Discord 상태 보기",
    "GitHub 설정으로",
    "Gateway 설정으로",
  ]) {
    assert.match(settingsSource, new RegExp(escapeRegExp(actionLabel)));
  }

  assert.match(settingsSource, /aria-label="Discord 설정"/);
});

function readSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
