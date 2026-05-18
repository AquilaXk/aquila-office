import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace 2d canvas component wires Phaser scene, event bus, pan zoom, and context menu", () => {
  const source = readSource("components/workspace/Workspace2DCanvas.tsx");

  assert.match(source, /Phaser/);
  assert.match(source, /createWorkspaceSceneEventBus/);
  assert.match(source, /contextmenu/);
  assert.match(source, /workspace-2d-canvas/);
  assert.match(source, /onWheel/);
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
