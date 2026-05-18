import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
const cssSource = readSource("app/globals.css");

test("workspace actor selection opens agent chat and task side panel", () => {
  assert.match(canvasSource, /onActorSelect/);
  assert.match(detailSource, /selectedActor/);
  assert.match(detailSource, /actorPanelTab/);
  assert.match(detailSource, /workspace-actor-panel/);
  assert.match(detailSource, /aria-label="Agent 대화와 Task 대시보드"/);
  assert.match(detailSource, /Chat/);
  assert.match(detailSource, /Tasks/);
});

test("agent chat flow keeps optimistic messages and session polling", () => {
  assert.match(detailSource, /optimistic/);
  assert.match(detailSource, /actorChatSessionId/);
  assert.match(detailSource, /sendWorkspaceChatMessage/);
  assert.match(detailSource, /fetchChatSessionMessages/);
  assert.match(detailSource, /pollActorChatMessages/);
  assert.match(detailSource, /actorChatMutation/);
});

test("task dashboard modal exposes summary board and selected task detail", () => {
  assert.match(detailSource, /taskDashboardOpen/);
  assert.match(detailSource, /taskDashboardTab/);
  assert.match(detailSource, /aria-label="Task dashboard"/);
  assert.match(detailSource, /API summary/);
  assert.match(detailSource, /Local fallback/);
  assert.match(detailSource, /selectedDashboardTask/);
});

test("agent task side panel styles are present", () => {
  assert.match(cssSource, /\.workspace-actor-panel/);
  assert.match(cssSource, /\.workspace-actor-tabs/);
  assert.match(cssSource, /\.workspace-task-dashboard-modal/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
