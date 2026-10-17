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

test("artifact fetch failures fail closed without demo workspace fallback content", () => {
  assert.doesNotMatch(detailSource, /demoWorkspaceArtifactTree/);
  assert.doesNotMatch(detailSource, /demoWorkspaceArtifactFile/);
  assert.doesNotMatch(detailSource, /파일 API 응답 대신 demo workspace 파일을 표시합니다/);
  assert.match(detailSource, /setArtifactTreeError/);
  assert.match(detailSource, /setArtifactFileError/);
  assert.match(detailSource, /파일 트리를 불러오지 못했습니다/);
  assert.match(detailSource, /파일을 찾을 수 없습니다/);
});

test("report and orchestration artifacts open the same file modal", () => {
  assert.match(detailSource, /<ArtifactTreeModal/);
  assert.match(detailSource, /<ArtifactFileModal/);
  assert.match(detailSource, /onOpenArtifact=\{\(path, name\) => void openArtifactFile\(path, name\)\}/);
  assert.match(detailSource, /onOpenFile=\{\(path, name\) => void openArtifactFile\(path, name\)\}/);
  assert.match(detailSource, /onOpenFile\(file\.path, file\.name\)/);
});

test("artifact file modal preserves source item name while loading", () => {
  assert.match(detailSource, /artifactFileNameHint/);
  assert.match(detailSource, /setArtifactFileNameHint\(nameHint \?\? path\.split\("\/"\)\.pop\(\) \?\? path\)/);
  assert.match(detailSource, /nameHint=\{artifactFileNameHint\}/);
  assert.match(detailSource, /file\?\.name \?\? nameHint \?\? path \?\? "ARTIFACT"/);
});

test("workspace artifact modal copy matches interaction contract", () => {
  assert.match(detailSource, /<h2>WORKSPACE FILES<\/h2>/);
  assert.match(detailSource, /<span>RELOAD<\/span>/);
  assert.match(detailSource, /파일 트리를 불러오는 중입니다\./);
  assert.match(detailSource, /아직 생성된 파일이 없습니다\./);
  assert.match(detailSource, /파일을 불러오는 중입니다\./);
  assert.match(detailSource, /계획을 불러오는 중입니다\./);
  assert.match(detailSource, /아직 등록된 step이 없습니다\./);
  assert.match(detailSource, /생성된 파일이 없습니다\./);
});

test("artifact modal distinguishes markdown and plain text rendering", () => {
  assert.match(detailSource, /isMarkdownArtifactFile/);
  assert.match(detailSource, /workspace-file-markdown/);
  assert.match(detailSource, /workspace-file-content/);
});

test("browser regression covers artifact fail-closed error and directory toggles", () => {
  assert.match(browserSource, /artifactFailureMode/);
  assert.match(browserSource, /artifact tree fixture failure/);
  assert.match(browserSource, /reports\/result.md/);
  assert.match(browserSource, /getByRole\("button", \{ name: "RELOAD" \}\)\.click/);
  assert.match(browserSource, /workspace-artifact-row-expanded/);
});

test("file artifact styles expose directory row states", () => {
  assert.match(cssSource, /\.workspace-artifact-row-directory/);
  assert.match(cssSource, /\.workspace-artifact-row-expanded/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
