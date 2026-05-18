import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace detail page renders realtime presence surface", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /createPresenceLiveState/);
  assert.match(source, /presenceLiveState/);
  assert.match(source, /WorkspacePresencePanel/);
  assert.match(source, /aria-label="실시간 Presence"/);
  assert.match(source, /workspace-presence-panel/);
  assert.match(source, /workspace-presence-member-grid/);
  assert.match(source, /workspace-presence-event-list/);
  assert.match(source, /formatPresenceConnectionState/);
});

test("workspace detail styles expose realtime presence layout", () => {
  const source = readSource("app/globals.css");

  assert.match(source, /\.workspace-presence-panel/);
  assert.match(source, /\.workspace-presence-status-grid/);
  assert.match(source, /\.workspace-presence-member-card/);
  assert.match(source, /\.workspace-presence-event-list/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
