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
  assert.match(source, /stopLoopbackOAuth/);
  assert.match(source, /saveSecureToken/);
  assert.match(source, /getSecureToken/);
  assert.match(source, /deleteSecureToken/);
  assert.match(source, /invokeDesktop/);
  assert.match(source, /listenDesktop/);
});

test("desktop window header supports macOS padding and control contracts", () => {
  const source = readSource("components/desktop/DesktopWindowHeader.tsx");

  assert.match(source, /desktop-titlebar-macos/);
  assert.match(source, /desktop-window-controls/);
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

test("parseUnifiedDiff parses hunks, lines, line numbers, and ignore no-newline markers", async () => {
  const { parseUnifiedDiff } = await import("./desktop/diff.ts");

  const sampleDiff = [
    "diff --git a/test.txt b/test.txt",
    "index 83db48f..bf269ef 100644",
    "--- a/test.txt",
    "+++ b/test.txt",
    "@@ -1,3 +1,4 @@",
    " line 1",
    "-line 2",
    "+line 2 modified",
    "+line 3 added",
    " line 4",
    "\\ No newline at end of file",
  ].join("\n");

  const result = parseUnifiedDiff(sampleDiff);
  assert.equal(result.additions, 2);
  assert.equal(result.deletions, 1);

  // Check lines
  const hunkLine = result.lines.find((l) => l.type === "hunk");
  assert.ok(hunkLine);

  const addLines = result.lines.filter((l) => l.type === "add");
  assert.equal(addLines.length, 2);
  assert.equal(addLines[0].text, "+line 2 modified");
  assert.equal(addLines[0].newNum, 2);

  const delLines = result.lines.filter((l) => l.type === "del");
  assert.equal(delLines.length, 1);
  assert.equal(delLines[0].text, "-line 2");
  assert.equal(delLines[0].oldNum, 2);

  const headerLines = result.lines.filter((l) => l.type === "header");
  assert.ok(headerLines.some((l) => l.text.startsWith("\\")));
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
