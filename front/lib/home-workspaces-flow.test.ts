import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");

function readSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}

test("home route is a title screen with keyboard and click navigation", () => {
  const source = readSource("app/page.tsx");

  assert.match(source, /MENU/);
  assert.match(source, /LOGIN/);
  assert.match(source, /CONTINUE/);
  assert.match(source, /ArrowDown|ArrowRight/);
  assert.match(source, /ArrowUp|ArrowLeft/);
  assert.match(source, /Enter/);
  assert.match(source, /router\.push\("\/auth"\)|router\.push\(item\.href\)/);
});

test("workspaces route owns the list flow instead of re-exporting home", () => {
  const source = readSource("app/workspaces/page.tsx");

  assert.doesNotMatch(source, /export default HomePage/);
  assert.match(source, /fetchWorkspaces/);
  assert.match(source, /logoutAuth/);
  assert.match(source, /\/workspaces\/gateway/);
  assert.match(source, /\/workspaces\/new/);
  assert.match(source, /가입된 워크스페이스가 없습니다/);
  assert.match(source, /목록을 불러오지 못했습니다/);
});

test("new workspace route returns to the workspace list", () => {
  const source = readSource("app/workspaces/new/page.tsx");

  assert.match(source, /href="\/workspaces"/);
  assert.doesNotMatch(source, /href="\/"/);
});
