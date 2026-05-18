import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace detail page exposes action rail and modal entry surface", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /createWorkspaceDetailActionItems/);
  assert.match(source, /workspace-detail-action-rail/);
  assert.match(source, /workspace-detail-action-card/);
  assert.match(source, /openWorkspaceFiles/);
  assert.match(source, /openTaskChat/);
});

test("workspace detail page keeps report, chat, artifact, and plan modals mounted", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /TaskReportsModal/);
  assert.match(source, /TaskChatModal/);
  assert.match(source, /ArtifactTreeModal/);
  assert.match(source, /ArtifactFileModal/);
  assert.match(source, /OrchestrationPlanModal/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
