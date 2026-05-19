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

test("workspace stage helper boundary lives outside the react component", () => {
  assert.match(readSource("lib/workspace-2d.ts"), /computeAgentPath/);
  assert.match(readSource("lib/workspace-2d.ts"), /findNearestInteractableStageActor/);
  assert.doesNotMatch(readSource("components/workspace/Workspace2DCanvas.tsx"), /class WorkspaceScene/);
});

test("workspace detail page mounts the 2d workspace screen without replacing existing sections", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /Workspace2DCanvas/);
  assert.match(source, /workspace-2d-section/);
  assert.match(source, /workspaceAgents/);
  assert.match(source, /onlinePresenceMembers/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
