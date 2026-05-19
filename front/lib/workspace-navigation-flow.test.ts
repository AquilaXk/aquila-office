import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const settingsSource = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

test("workspace detail and settings expose shared navigation routes", () => {
  assert.match(detailSource, /aria-label="워크스페이스 내비게이션"/);
  assert.match(settingsSource, /BACK TO WORKSPACE/);
  assert.match(detailSource, /href="\/workspaces"/);
  assert.match(settingsSource, /href=\{`\/workspaces\/\$\{workspaceIdNumber\}`\}/);
  assert.match(detailSource, /<span>Connection settings<\/span>/);
});

test("workspace integrations are entered from the office scene instead of settings", () => {
  assert.doesNotMatch(settingsSource, /Slack 설정으로/);
  assert.doesNotMatch(settingsSource, /GitHub 설정으로/);
  assert.doesNotMatch(settingsSource, /Gateway 설정으로/);
  assert.match(detailSource, /officeSceneNavItems/);
  assert.match(detailSource, /connections/);
  assert.match(detailSource, /openConnectionModal\("selector"\)/);
});

function readSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}
