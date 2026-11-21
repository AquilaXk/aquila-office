import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";

const workspacePageSource = readFileSync("app/workspaces/[workspaceId]/page.tsx", "utf8");

test("workspace detail separates 5-view panels cleanly and mounts modals", () => {
  assert.match(workspacePageSource, /<AgentHireModal/);
  assert.match(workspacePageSource, /<TaskCreateModal/);
  assert.match(workspacePageSource, /<ConnectionSettingsModal/);
  assert.match(workspacePageSource, /activeView === "office"/);
  assert.match(workspacePageSource, /activeView === "tasks"/);
  assert.match(workspacePageSource, /activeView === "approvals"/);
  assert.match(workspacePageSource, /activeView === "orchestrations"/);
  assert.match(workspacePageSource, /activeView === "artifacts"/);

  assert.doesNotMatch(workspacePageSource, /<section[^>]+id="workspace-hire-agent"/);
  assert.doesNotMatch(workspacePageSource, /aria-label="워크스페이스 초대 생성"/);
});
