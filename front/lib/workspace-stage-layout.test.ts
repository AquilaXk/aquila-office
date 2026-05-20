import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";

const workspacePageSource = readFileSync("app/workspaces/[workspaceId]/page.tsx", "utf8");

test("workspace detail keeps management sections out of the first screen", () => {
  assert.match(workspacePageSource, /<TaskDashboardModal[\s\S]+open=\{taskDashboardOpen\}/);
  assert.match(workspacePageSource, /<AgentHireModal[\s\S]+open=\{agentHireOpen\}/);

  assert.doesNotMatch(workspacePageSource, /aria-label="워크스페이스 대시보드 요약"/);
  assert.doesNotMatch(workspacePageSource, /<section[^>]+id="workspace-hire-agent"/);
  assert.doesNotMatch(workspacePageSource, /aria-label="Task 생성"/);
  assert.doesNotMatch(workspacePageSource, /aria-label="워크스페이스 Task"/);
  assert.doesNotMatch(workspacePageSource, /aria-label="워크스페이스 멤버"/);
  assert.doesNotMatch(workspacePageSource, /aria-label="워크스페이스 초대 생성"/);
});
