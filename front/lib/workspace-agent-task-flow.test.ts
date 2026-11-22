import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = path.resolve(import.meta.dirname, "..");
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const liveSyncSource = readSource("lib/use-workspace-live-sync.ts");
const inspectorSource = readSource("components/workspace/inspector/WorkspaceContextInspector.tsx");
const agentEditSource = readSource("components/workspace/inspector/AgentEditPanel.tsx");
const taskBoardSource = readSource("components/workspace/views/WorkspaceTaskBoardView.tsx");
const helpersSource = readSource("components/workspace/workspace-ui-helpers.ts");
const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
const chromeCss = readSource("styles/chrome.css");
const chatSource = readSource("lib/chat.ts");

test("workspace actor selection opens right context inspector with chat, tasks, and settings tabs", () => {
  assert.match(canvasSource, /onActorSelect/);
  assert.match(detailSource, /selectedActorNode/);
  assert.match(detailSource, /WorkspaceContextInspector/);
  assert.match(inspectorSource, /aria-label="컨텍스트 인스펙터"/);
  assert.match(inspectorSource, /대화/);
  assert.match(inspectorSource, /작업 \(/);
  assert.match(inspectorSource, /설정 · 스킬/);
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

test("actor inspector exposes member details and agent task/plan links", () => {
  assert.match(inspectorSource, /구성원 정보/);
  assert.match(inspectorSource, /onOpenPlanArtifact/);
  assert.match(inspectorSource, /onInspectTask/);
  assert.match(inspectorSource, /계획 #/);
  assert.match(inspectorSource, /작업 #/);
  assert.match(agentEditSource, /updateWorkspaceAgent|onUpdateAgent/);
  assert.match(agentEditSource, /onConfirmDeleteAgent/);
});

test("actor inspector closes cleanly and keeps chat scrolled to latest message", () => {
  assert.match(detailSource, /setInspectorTarget\(null\)/);
  assert.match(detailSource, /useWorkspaceLiveSync/);
  assert.match(liveSyncSource, /event\.key === "Escape"/);
  assert.match(inspectorSource, /actorMessagesEndRef/);
  assert.match(inspectorSource, /scrollIntoView/);
});

test("actor inspector keeps agent task flow sorted", () => {
  assert.match(helpersSource, /compareActorPanelTasks/);
  assert.match(inspectorSource, /actorAssignedTasks/);
  assert.match(inspectorSource, /compareActorPanelTasks/);
});

test("task board view exposes KPI summary, daily token gauge, kanban/list toggle, and task mutations", () => {
  assert.match(detailSource, /WorkspaceTaskBoardView/);
  assert.match(taskBoardSource, /aria-label="작업 보드 뷰"/);
  assert.match(taskBoardSource, /aria-label="작업 요약 지표"/);
  assert.match(taskBoardSource, /aria-label="일일 토큰 사용량"/);
  assert.match(taskBoardSource, /chrome-kanban-board/);
  assert.match(taskBoardSource, /onRecoverStaleTasks/);
});

test("desktop chrome inspector and kanban styles are present", () => {
  assert.match(chromeCss, /\.chrome-inspector/);
  assert.match(chromeCss, /\.chrome-inspector-tabs/);
  assert.match(chromeCss, /\.chrome-kanban-board/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
