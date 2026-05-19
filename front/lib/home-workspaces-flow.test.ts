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
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(source, /\/workspaces\/gateway/);
  assert.match(source, /\/workspaces\/new/);
  assert.match(source, /WORKPLACES JOINED/);
  assert.match(source, /TASKS · ACTIVE/);
  assert.match(source, /AGENT ALLIES/);
  assert.match(source, /TASKS · DONE/);
  assert.match(source, /W\$\{String\(index \+ 1\)\.padStart\(3, "0"\)\}/);
  assert.match(source, /workspace\.agentCount/);
  assert.match(source, /workspace\.runningTaskCount/);
  assert.match(source, /ENTER/);
  assert.match(source, /가입된 워크스페이스가 없습니다/);
  assert.match(source, /목록을 불러오지 못했습니다/);
});

test("new workspace route returns to the workspace list", () => {
  const source = readSource("app/workspaces/new/page.tsx");

  assert.match(source, /useRouter/);
  assert.match(source, /readAuthTokens/);
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(source, /href="\/workspaces"/);
  assert.doesNotMatch(source, /href="\/"/);
  assert.match(source, /NAME_MAX_LENGTH = 100/);
  assert.match(source, /DESCRIPTION_MAX_LENGTH = 500/);
  assert.match(source, /name\.trim\(\)/);
  assert.match(source, /description\.trim\(\) \|\| undefined/);
  assert.match(source, /router\.replace\(`\/workspaces\/\$\{created\.workspaceId\}`\)/);
  assert.match(source, /CANCEL/);
  assert.match(source, /FORM WORKSPACE/);
  assert.doesNotMatch(source, /createdWorkspaceHref/);
  assert.doesNotMatch(source, /상세 보기/);
  assert.doesNotMatch(source, /form-notice-success/);
});
