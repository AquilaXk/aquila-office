import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const headerSource = readSource("components/workspace/chrome/WorkspaceChromeHeader.tsx");
const settingsSource = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

test("workspace detail and settings expose shared navigation routes", () => {
  assert.match(headerSource, /aria-label="워크스페이스 빠른 메뉴"/);
  assert.match(headerSource, /href="\/workspaces"/);
  assert.match(headerSource, /href=\{`\/workspaces\/\$\{workspaceId\}\/settings`\}/);
  assert.match(headerSource, /<span>연결 설정<\/span>/);
  assert.match(settingsSource, /href=\{`\/workspaces\/\$\{workspaceIdNumber\}`\}/);
  assert.match(settingsSource, /오피스로 돌아가기/);
});

test("workspace integrations are entered from workspace chrome instead of settings", () => {
  assert.doesNotMatch(settingsSource, /Slack 설정으로/);
  assert.doesNotMatch(settingsSource, /GitHub 설정으로/);
  assert.doesNotMatch(settingsSource, /Gateway 설정으로/);
  assert.match(detailSource, /openConnectionModal\("selector"\)/);
  assert.match(headerSource, /onOpenConnections/);
});

function readSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}
