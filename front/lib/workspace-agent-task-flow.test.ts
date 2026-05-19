import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
const cssSource = readSource("app/globals.css");
const chatSource = readSource("lib/chat.ts");

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

test("actor chat sessions persist per workspace and agent", () => {
  assert.match(chatSource, /readActorChatSessionIds/);
  assert.match(chatSource, /writeActorChatSessionIds/);
  assert.match(chatSource, /actorChatSessionIdsStorageKey/);
  assert.match(chatSource, /aquila-office\.workspace\.\$\{workspaceId\}\.actorChatSessions/);
  assert.match(detailSource, /actorChatSessionIds/);
  assert.match(detailSource, /actorChatSessionKey/);
  assert.match(detailSource, /readActorChatSessionIds\(window\.localStorage, numericWorkspaceId\)/);
  assert.match(detailSource, /writeActorChatSessionIds\(window\.localStorage, numericWorkspaceId/);
});

test("actor chat panel exposes member chat and server task plan links", () => {
  assert.match(detailSource, /setActorPanelTab\("chat"\)/);
  assert.doesNotMatch(detailSource, /disabled=\{!isAgent\}/);
  assert.match(detailSource, /현재 멤버 대화는 화면 내 대화로 표시됩니다/);
  assert.match(detailSource, /actor-message-plan/);
  assert.match(detailSource, /Plan #/);
  assert.match(detailSource, /Task #/);
  assert.match(detailSource, /onOpenPlan/);
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
