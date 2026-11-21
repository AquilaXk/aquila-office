import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("workspace detail page and chrome components render realtime presence surface", () => {
  const pageSource = readSource("app/workspaces/[workspaceId]/page.tsx");
  const sidebarSource = readSource("components/workspace/chrome/WorkspaceLeftSidebar.tsx");
  const statusBarSource = readSource("components/workspace/chrome/WorkspaceStatusBar.tsx");

  assert.match(pageSource, /createPresenceLiveState/);
  assert.match(pageSource, /presenceLiveState/);
  assert.match(pageSource, /WorkspaceLeftSidebar/);
  assert.match(sidebarSource, /aria-label="실시간 Presence"/);
  assert.match(sidebarSource, /chrome-sidebar-presence-box/);
  assert.match(sidebarSource, /aria-label="Presence member positions"/);
  assert.match(sidebarSource, /aria-label="Presence event log"/);
  assert.match(sidebarSource, /formatPresenceConnectionState/);
  assert.match(statusBarSource, /formatPresenceConnectionState/);
});

test("workspace chrome styles expose realtime presence layout", () => {
  const chromeCss = readSource("styles/chrome.css");
  const cssSource = readSource("app/globals.css");

  assert.match(chromeCss, /\.chrome-sidebar-presence-box/);
  assert.match(chromeCss, /\.chrome-statusbar/);
  assert.match(cssSource, /\.workspace-presence-panel/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
