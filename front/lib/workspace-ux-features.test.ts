import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import { test } from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");

function readSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}

test("WorkspaceCommandPalette enforces closed null render, WAI-ARIA combobox/listbox, and Bitap fuzzy filtering", () => {
  const paletteSource = readSource("components/workspace/WorkspaceCommandPalette.tsx");

  assert.match(paletteSource, /if \(!open\) \{\s*return null;\s*\}/);
  assert.match(paletteSource, /role="dialog"/);
  assert.match(paletteSource, /aria-modal="true"/);
  assert.match(paletteSource, /role="combobox"/);
  assert.match(paletteSource, /role="listbox"/);
  assert.match(paletteSource, /role="option"/);
  assert.match(paletteSource, /aria-activedescendant=/);
  assert.match(paletteSource, /searchBitapItems/);
  assert.match(paletteSource, /MAX_QUERY_LENGTH/);
  assert.match(paletteSource, /MAX_CANDIDATE_LENGTH/);
  assert.match(paletteSource, /event\.key === "ArrowDown"/);
  assert.match(paletteSource, /event\.key === "ArrowUp"/);
  assert.match(paletteSource, /event\.key === "Escape"/);
  assert.match(paletteSource, /event\.key === "Enter"/);
});

test("workspace detail integrates Cmd+K command palette, live Bitap filters, precomputed sort, and dialog a11y", () => {
  const pageSource = readSource("app/workspaces/[workspaceId]/page.tsx");
  const taskBoardSource = readSource("components/workspace/views/WorkspaceTaskBoardView.tsx");
  const artifactsViewSource = readSource("components/workspace/views/WorkspaceArtifactsAndDiffView.tsx");
  const inspectorSource = readSource("components/workspace/inspector/WorkspaceContextInspector.tsx");

  assert.match(pageSource, /WorkspaceCommandPalette/);
  assert.match(pageSource, /\(event\.metaKey \|\| event\.ctrlKey\) && event\.key\.toLowerCase\(\) === "k"/);
  assert.match(pageSource, /cameraFocusTarget=\{cameraFocusTarget\}/);
  assert.match(pageSource, /handleToggleTheme/);
  assert.match(taskBoardSource, /aria-label="작업 검색"/);
  assert.match(artifactsViewSource, /aria-label="파일 검색"/);
  assert.match(artifactsViewSource, /flattenArtifactNodes/);
  assert.match(taskBoardSource, /searchBitapItems/);
  assert.match(artifactsViewSource, /searchBitapItems/);
  assert.match(inspectorSource, /const timestampByTaskId = new Map<number, number>\(\)/);
  assert.match(inspectorSource, /compareActorPanelTasks\(a, b, timestampByTaskId\)/);

  for (const modalFile of [
    "components/workspace/modals/AgentHireModal.tsx",
    "components/workspace/modals/TaskCreateModal.tsx",
    "components/workspace/modals/ConnectionSettingsModal.tsx",
    "components/workspace/modals/TaskDetailModals.tsx",
    "components/workspace/modals/ArtifactFileModal.tsx",
  ]) {
    const src = readSource(modalFile);
    assert.match(src, /className="workspace-modal-backdrop"/, modalFile);
    assert.match(src, /event\.target === event\.currentTarget/, modalFile);
    assert.match(src, /useDialogA11y/, modalFile);
  }
});

test("tokens.css defines symmetric :root[data-theme=\"light\"] semantic tokens while keeping globals.css dark-safe", () => {
  const tokensSource = readSource("styles/tokens.css");
  const globalsSource = readSource("app/globals.css");

  assert.match(tokensSource, /:root\s*\{[\s\S]*color-scheme:\s*dark;/);
  assert.match(tokensSource, /:root\[data-theme="light"\]\s*\{[\s\S]*color-scheme:\s*light;/);
  for (const token of [
    "--ui-bg",
    "--ui-bg-light",
    "--ui-surface",
    "--ui-panel-bg",
    "--ui-border",
    "--ui-border-subtle",
    "--ui-text",
    "--ui-text-strong",
    "--ui-text-muted",
    "--ui-hover",
    "--ui-selected",
    "--accent-primary",
    "--accent-strong",
    "--focus-ring",
    "--gothic-page",
    "--gothic-surface",
  ]) {
    const lightBlock = tokensSource.split(':root[data-theme="light"]')[1] ?? "";
    assert.ok(lightBlock.includes(`${token}:`), `missing ${token} in :root[data-theme="light"]`);
  }

  assert.match(globalsSource, /\.workspace-command-dialog/);
  assert.match(globalsSource, /background:\s*var\(--ui-surface\)/);
  assert.match(globalsSource, /--gothic-page/);

  const lightBlock = tokensSource.split(':root[data-theme="light"]')[1] ?? "";
  assert.ok(lightBlock.includes("--stage-hud-bg:"), "missing --stage-hud-bg in :root[data-theme=\"light\"]");
  assert.ok(lightBlock.includes("--stage-room-bg:"), "missing --stage-room-bg in :root[data-theme=\"light\"]");
  assert.match(readSource("components/workspace/Workspace2DCanvas.tsx"), /var\(--stage-room-bg\)/);
});

test("Workspace2DCanvas uses per-instance bitsets, idle-stop rAF, A* pathfinding, KD-tree interact target, and proportional minimap", () => {
  const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");

  assert.match(canvasSource, /createSpatialBitset\(/);
  assert.doesNotMatch(canvasSource, /STAGE_SPATIAL_BITSET/);
  assert.match(canvasSource, /requestAnimationFrame\(tick\)/);
  assert.match(canvasSource, /ensureTickLoop/);
  assert.match(canvasSource, /findOfficePath\(/);
  assert.match(canvasSource, /buildOfficeSpatialKdTree/);
  assert.match(canvasSource, /findNearestWithin/);
  assert.match(canvasSource, /data-interact-target/);
  assert.match(canvasSource, /viewportSize/);
  assert.match(canvasSource, /workspace-2d-minimap-actor-player/);
  assert.match(canvasSource, /clampContextMenuPosition/);
});

test("Desktop Tauri config exposes Apache-2.0 license, explicit CSP, licenseFile, and always-on-top permission", () => {
  const pkg = JSON.parse(readSource("package.json")) as { license?: string };
  const cargoToml = readSource("src-tauri/Cargo.toml");
  const tauriConf = JSON.parse(readSource("src-tauri/tauri.conf.json")) as {
    app?: { security?: { csp?: string | null } };
    bundle?: { licenseFile?: string };
  };
  const capabilities = JSON.parse(readSource("src-tauri/capabilities/default.json")) as {
    permissions?: string[];
  };

  assert.equal(pkg.license, "Apache-2.0");
  assert.match(cargoToml, /license\s*=\s*"Apache-2\.0"/);
  assert.equal(typeof tauriConf.app?.security?.csp, "string");
  assert.ok((tauriConf.app?.security?.csp ?? "").includes("default-src 'self'"));
  assert.equal(tauriConf.bundle?.licenseFile, "../../LICENSE");
  assert.ok(capabilities.permissions?.includes("core:window:allow-set-always-on-top"));
});
