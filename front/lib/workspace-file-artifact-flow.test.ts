import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const cssSource = readSource("app/globals.css");
const browserSource = readSource("e2e/browser-ux.spec.ts");

test("workspace file tree supports directory expand collapse and reload flow", () => {
  assert.match(detailSource, /expandedArtifactPaths/);
  assert.match(detailSource, /toggleArtifactDirectory/);
  assert.match(detailSource, /aria-expanded/);
  assert.match(detailSource, /workspace-artifact-row-directory/);
  assert.match(detailSource, /workspace-artifact-row-expanded/);
});

test("artifact fetch failures use demo workspace fallback content", () => {
  assert.match(detailSource, /demoWorkspaceArtifactTree/);
  assert.match(detailSource, /demoWorkspaceArtifactFile/);
  assert.match(detailSource, /파일 API 응답 대신 demo workspace 파일을 표시합니다/);
  assert.match(detailSource, /setArtifactTree\(demoWorkspaceArtifactTree\(numericWorkspaceId\)\)/);
  assert.match(detailSource, /setArtifactFile\(demoWorkspaceArtifactFile\(numericWorkspaceId, path\)\)/);
});

test("report and orchestration artifacts open the same file modal", () => {
  assert.match(detailSource, /<ArtifactTreeModal/);
  assert.match(detailSource, /<ArtifactFileModal/);
  assert.match(detailSource, /onOpenArtifact=\{\(path\) => void openArtifactFile\(path\)\}/);
  assert.match(detailSource, /onOpenFile=\{\(path\) => void openArtifactFile\(path\)\}/);
});

test("artifact modal distinguishes markdown and plain text rendering", () => {
  assert.match(detailSource, /isMarkdownArtifactFile/);
  assert.match(detailSource, /workspace-file-markdown/);
  assert.match(detailSource, /workspace-file-content/);
});

test("browser regression covers artifact fallback and directory toggles", () => {
  assert.match(browserSource, /artifactFailureMode/);
  assert.match(browserSource, /reports\/fallback.md/);
  assert.match(browserSource, /workspace-artifact-row-expanded/);
});

test("file artifact styles expose directory row states", () => {
  assert.match(cssSource, /\.workspace-artifact-row-directory/);
  assert.match(cssSource, /\.workspace-artifact-row-expanded/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
