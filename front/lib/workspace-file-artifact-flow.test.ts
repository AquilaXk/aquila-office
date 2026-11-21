import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const viewSource = readSource("components/workspace/views/WorkspaceArtifactsAndDiffView.tsx");
const fileModalSource = readSource("components/workspace/modals/ArtifactFileModal.tsx");
const detailModalsSource = readSource("components/workspace/modals/TaskDetailModals.tsx");
const helpersSource = readSource("components/workspace/workspace-ui-helpers.ts");

test("workspace file tree supports directory expand collapse, search, and reload flow", () => {
  assert.match(viewSource, /toggleExpandedArtifactPath/);
  assert.match(viewSource, /aria-expanded=\{isDirectory \? expanded : undefined\}/);
  assert.match(viewSource, /aria-label="파일 검색"/);
  assert.match(viewSource, /onReloadTree/);
});

test("artifact fetch failures fail closed without demo workspace fallback content", () => {
  assert.doesNotMatch(detailSource, /demoWorkspaceArtifactTree/);
  assert.doesNotMatch(detailSource, /demoWorkspaceArtifactFile/);
  assert.doesNotMatch(viewSource, /demoWorkspaceArtifactTree/);
  assert.match(detailSource, /산출물 파일 트리를 불러오지 못했습니다/);
  assert.match(detailSource, /산출물 파일을 불러오지 못했습니다/);
});

test("report and orchestration artifacts open the file preview modal", () => {
  assert.match(detailSource, /<ArtifactFileModal/);
  assert.match(detailSource, /openModalArtifactFile/);
  assert.match(detailModalsSource, /onOpenArtifact\(path, artifact\.name\)/);
  assert.match(detailModalsSource, /onOpenFile\(file\.path, file\.name\)/);
});

test("artifact file modal preserves source item name while loading", () => {
  assert.match(detailSource, /modalArtifactNameHint/);
  assert.match(detailSource, /setModalArtifactNameHint\(nameHint \?\? path\.split\("\/"\)\.pop\(\) \?\? path\)/);
  assert.match(fileModalSource, /nameHint/);
  assert.match(fileModalSource, /file\?\.name \?\? nameHint \?\? path \?\? "파일"/);
});

test("artifacts view wires Tauri Git status, diff viewer, and file watcher fail-closed", () => {
  assert.match(viewSource, /getWorkspaceGitStatus/);
  assert.match(viewSource, /getWorkspaceGitDiff/);
  assert.match(viewSource, /startWatchingWorkspace/);
  assert.match(viewSource, /stopWatchingWorkspace/);
  assert.match(viewSource, /listenDesktop/);
  assert.match(viewSource, /WorkspaceGitDiffViewer/);
  assert.match(viewSource, /MAX_LOCAL_DIFF_CHARS/);
});

test("artifact view and modal distinguish markdown and plain text rendering", () => {
  assert.match(helpersSource, /isMarkdownArtifactFile/);
  assert.match(viewSource, /MarkdownContent/);
  assert.match(viewSource, /workspace-artifact-file-content/);
  assert.match(fileModalSource, /MarkdownContent/);
  assert.match(fileModalSource, /workspace-artifact-file-content/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
