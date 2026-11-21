import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace detail page exposes 5-region chrome, 5 views, and right inspector", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /WorkspaceChromeHeader/);
  assert.match(source, /WorkspaceLeftSidebar/);
  assert.match(source, /WorkspaceStatusBar/);
  assert.match(source, /WorkspaceContextInspector/);
  assert.match(source, /WorkspaceTaskBoardView/);
  assert.match(source, /WorkspaceApprovalInboxView/);
  assert.match(source, /WorkspaceOrchestrationView/);
  assert.match(source, /WorkspaceArtifactsAndDiffView/);
});

test("workspace detail page keeps task, report, chat, artifact, and plan modals mounted", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /AgentHireModal/);
  assert.match(source, /TaskCreateModal/);
  assert.match(source, /ConnectionSettingsModal/);
  assert.match(source, /TaskReportsModal/);
  assert.match(source, /TaskChatModal/);
  assert.match(source, /ArtifactFileModal/);
  assert.match(source, /OrchestrationPlanModal/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
