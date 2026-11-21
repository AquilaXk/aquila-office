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
  assert.match(source, /로그인/);
  assert.match(source, /워크스페이스로 이동/);
  assert.doesNotMatch(source, /SETTINGS/);
  assert.doesNotMatch(source, /\/auth\/character/);
  assert.match(source, /ArrowDown|ArrowRight/);
  assert.match(source, /ArrowUp|ArrowLeft/);
  assert.match(source, /Enter/);
  assert.match(source, /router\.push\("\/auth"\)|router\.push\(item\.href\)/);
});

test("workspaces route owns the list flow instead of re-exporting home", () => {
  const source = readSource("app/workspaces/page.tsx");
  const chromeSource = readSource("components/desktop/AppTopChrome.tsx");

  assert.doesNotMatch(source, /export default HomePage/);
  assert.match(source, /AppTopChrome/);
  assert.match(chromeSource, /logout/);
  assert.match(source, /listWorkspaces/);
  assert.match(source, /getStoredUser/);
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(source, /\/workspaces\/gateway/);
  assert.match(source, /\/workspaces\/new/);
  assert.match(source, /참여 워크스페이스/);
  assert.match(source, /등록된 에이전트/);
  assert.match(source, /진행 중 작업/);
  assert.match(source, /완료된 작업/);
  assert.match(source, /W\{String\(index \+ 1\)\.padStart\(3, "0"\)\}/);
  assert.match(source, /ws\.agentCount/);
  assert.match(source, /ws\.runningTaskCount/);
  assert.match(source, /formatTokenUsageSummary/);
  assert.match(source, /아직 참여 중인 워크스페이스가 없습니다/);
  assert.match(source, /새 워크스페이스/);
  assert.match(source, /워크스페이스 목록을 불러오지 못했습니다/);
});

test("new workspace route returns to the workspace list", () => {
  const source = readSource("app/workspaces/new/page.tsx");

  assert.match(source, /AppTopChrome/);
  assert.match(source, /useRouter/);
  assert.match(source, /getStoredUser/);
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(source, /href="\/workspaces"/);
  assert.doesNotMatch(source, /href="\/"/);
  assert.match(source, /NAME_MAX = 100/);
  assert.match(source, /DESCRIPTION_MAX = 500/);
  assert.match(source, /name\.trim\(\)/);
  assert.match(source, /description\.trim\(\) \|\| undefined/);
  assert.match(source, /router\.replace\(`\/workspaces\/\$\{created\.workspaceId\}`\)/);
  assert.match(source, /취소/);
  assert.match(source, /워크스페이스 생성/);
  assert.doesNotMatch(source, /createdWorkspaceHref/);
  assert.doesNotMatch(source, /form-notice-success/);
});
