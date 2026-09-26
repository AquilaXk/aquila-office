import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("desktop env helper exports isDesktop function", () => {
  const source = readSource("lib/desktop/env.ts");
  assert.match(source, /export function isDesktop/);
  assert.match(source, /__TAURI_INTERNALS__/);
  assert.match(source, /__TAURI__/);
});

test("desktop IPC module exports typed superpowers contracts", () => {
  const source = readSource("lib/desktop/ipc.ts");

  assert.match(source, /getWorkspaceGitStatus/);
  assert.match(source, /getWorkspaceGitDiff/);
  assert.match(source, /startWatchingWorkspace/);
  assert.match(source, /stopWatchingWorkspace/);
  assert.match(source, /startLoopbackOAuth/);
  assert.match(source, /saveSecureToken/);
  assert.match(source, /getSecureToken/);
  assert.match(source, /deleteSecureToken/);
  assert.match(source, /invokeDesktop/);
  assert.match(source, /listenDesktop/);
});

test("desktop window header supports macOS padding and control contracts", () => {
  const source = readSource("components/desktop/DesktopWindowHeader.tsx");

  assert.match(source, /desktop-titlebar-macos/);
  assert.match(source, /data-tauri-drag-region/);
  assert.match(source, /aria-label="Minimize"/);
  assert.match(source, /aria-label="Close"/);
});

test("workspace git diff viewer renders diff hunks and copy controls", () => {
  const source = readSource("components/workspace/WorkspaceGitDiffViewer.tsx");

  assert.match(source, /WorkspaceGitDiffViewer/);
  assert.match(source, /aria-label="Copy diff"/);
  assert.match(source, /additions/);
  assert.match(source, /deletions/);
  assert.match(source, /ao-card/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
