import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();

const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
const sceneSource = readSource("lib/workspace-2d.ts");
const cssSource = readSource("app/globals.css");

test("workspace detail promotes the office scene as the first screen", () => {
  assert.match(detailSource, /workspace-office-scene/);
  assert.match(detailSource, /aria-label="워크스페이스 오피스 씬"/);
  assert.match(detailSource, /officeSceneNavItems/);
  assert.match(detailSource, /connections/);
  assert.match(detailSource, /tasks/);
  assert.match(detailSource, /hire-agent/);
  assert.match(detailSource, /files/);
  assert.match(detailSource, /leave-workspace/);
});

test("workspace 2d canvas exposes HUD, minimap, dialogue, and keyboard stage controls", () => {
  assert.match(canvasSource, /workspace-2d-hud/);
  assert.match(canvasSource, /workspace-2d-minimap/);
  assert.match(canvasSource, /workspace-2d-dialogue/);
  assert.match(canvasSource, /aria-label="Office scene minimap"/);
  assert.match(canvasSource, /aria-label="Stage keyboard controls"/);
  assert.match(canvasSource, /onKeyDown/);
  assert.match(canvasSource, /tabIndex=\{0\}/);
  assert.match(canvasSource, /moveWorkspaceViewportByKey/);
});

test("workspace scene helpers model rooms and desks for office placement", () => {
  assert.match(sceneSource, /WorkspaceSceneZoneKind/);
  assert.match(sceneSource, /"room"/);
  assert.match(sceneSource, /"desk"/);
  assert.match(sceneSource, /createWorkspaceSceneZones/);
  assert.match(sceneSource, /moveWorkspaceViewportByKey/);
});

test("workspace office scene styles define full screen surface", () => {
  assert.match(cssSource, /\.workspace-office-scene/);
  assert.match(cssSource, /min-height:\s*calc\(100vh - 64px\)/);
  assert.match(cssSource, /\.workspace-2d-minimap/);
  assert.match(cssSource, /\.workspace-2d-dialogue/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
