import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace 2d canvas component wires DOM stage, pan zoom, and context menu", () => {
  const source = readSource("components/workspace/Workspace2DCanvas.tsx");

  assert.match(source, /ArcadeOfficeStage/);
  assert.match(source, /workspace-2d-stage-shell/);
  assert.match(source, /MapHud/);
  assert.match(source, /MapZoomControls/);
  assert.match(source, /requestAnimationFrame\(tick\)/);
  assert.match(source, /onContextMenu/);
  assert.match(source, /computeAgentPath/);
  assert.doesNotMatch(source, /createWorkspace2DGame/);
});

test("workspace 2d stage keeps authenticated player and keyboard interaction contracts", () => {
  const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
  const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(canvasSource, /playerMemberId/);
  assert.match(canvasSource, /members\.find\(\(member\) => member\.memberId === playerMemberId\)/);
  assert.match(canvasSource, /keys\.shift \? PLAYER_MOVE_FAST_SPEED_PX : PLAYER_MOVE_SPEED_PX/);
  assert.match(canvasSource, /key === "\/"/);
  assert.match(canvasSource, /isNearQuestMarker\(keyboardPosition\)/);
  assert.match(canvasSource, /findNearestInteractableStageActor/);
  assert.match(detailSource, /playerMemberId=\{currentMemberId\}/);
});

test("workspace stage helper boundary lives outside the react component", () => {
  assert.match(readSource("lib/workspace-2d.ts"), /computeAgentPath/);
  assert.match(readSource("lib/workspace-2d.ts"), /findNearestInteractableStageActor/);
  assert.doesNotMatch(readSource("components/workspace/Workspace2DCanvas.tsx"), /class WorkspaceScene/);
});

test("workspace detail page mounts the 2d workspace screen inside 5-region desktop chrome", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /Workspace2DCanvas/);
  assert.match(source, /chrome-workspace-shell/);
  assert.match(source, /workspace-2d-section/);
  assert.match(source, /workspaceAgents/);
  assert.match(source, /onlinePresenceMembers/);
});

test("workspace detail promotes the 2d workspace into a fullscreen viewport inside chrome", () => {
  const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
  const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
  const chromeSource = readSource("styles/chrome.css");

  assert.match(detailSource, /chrome-workspace-shell/);
  assert.match(detailSource, /workspace-stage-surface/);
  assert.match(detailSource, /variant="fullscreen"/);
  assert.match(canvasSource, /variant\?: "panel" \| "fullscreen"/);
  assert.match(canvasSource, /workspace-2d-frame-fullscreen/);
  assert.match(chromeSource, /\.chrome-workspace-shell/);
  assert.match(chromeSource, /\.chrome-viewport \.workspace-2d-frame-fullscreen/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
