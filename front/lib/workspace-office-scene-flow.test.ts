import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = path.resolve(import.meta.dirname, "..");

const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const headerSource = readSource("components/workspace/chrome/WorkspaceChromeHeader.tsx");
const hireSource = readSource("components/workspace/modals/AgentHireModal.tsx");
const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
const sceneSource = readSource("lib/workspace-2d.ts");
const cssSource = readSource("app/globals.css");

test("workspace detail promotes the office scene as the default view inside desktop chrome", () => {
  assert.match(detailSource, /useState<WorkspaceViewId>\("office"\)/);
  assert.match(detailSource, /workspace-office-scene/);
  assert.match(detailSource, /aria-label="워크스페이스 오피스 씬"/);
  assert.match(headerSource, /role="tablist"/);
  assert.match(headerSource, /에이전트 추가/);
  assert.match(headerSource, /새 작업/);
  assert.match(headerSource, /연결 설정/);
});

test("workspace office scene opens agent hire modal with role, icon, and skill files", () => {
  assert.match(detailSource, /agentHireOpen/);
  assert.match(detailSource, /AgentHireModal/);
  assert.match(hireSource, /aria-label="에이전트 추가"/);
  assert.match(hireSource, /에이전트 이름/);
  assert.match(hireSource, /역할 분류/);
  assert.match(hireSource, /작업 경로/);
  assert.match(hireSource, /표시 아이콘/);
  assert.match(hireSource, /스킬 파일/);
  assert.match(hireSource, /에이전트 등록/);
  assert.match(hireSource, /MAX_AGENT_SKILL_FILES/);
});

test("workspace 2d canvas exposes HUD, minimap, dialogue, furniture hooks, and keyboard controls", () => {
  assert.match(canvasSource, /workspace-2d-hud/);
  assert.match(canvasSource, /workspace-2d-minimap/);
  assert.match(canvasSource, /workspace-2d-dialogue/);
  assert.match(canvasSource, /workspace-2d-actor-layer/);
  assert.match(canvasSource, /workspace-2d-hero-hud/);
  assert.match(canvasSource, /function ArcadeOfficeStage/);
  assert.match(canvasSource, /function StageRoom/);
  assert.match(canvasSource, /function StageCorridors/);
  assert.match(canvasSource, /function QuestMarker/);
  assert.match(canvasSource, /function MapHud/);
  assert.match(canvasSource, /function MapZoomControls/);
  assert.match(canvasSource, /StageActor/);
  assert.match(canvasSource, /PixelAvatar/);
  assert.match(canvasSource, /requestAnimationFrame\(tick\)/);
  assert.match(canvasSource, /window\.addEventListener\("keydown", handleKeyDown\)/);
  assert.match(canvasSource, /isNearQuestMarker\(keyboardPosition\)/);
  assert.match(canvasSource, /findNearestInteractableStageActor/);
  assert.match(canvasSource, /stage-actor--\$\{motion\}/);
  assert.match(canvasSource, /stage-actor__proximity-ring/);
  assert.match(canvasSource, /onOpenOrchestration/);
  assert.match(canvasSource, /onOpenArtifacts/);
  assert.match(canvasSource, /getStageMiniMapRooms/);
  assert.match(canvasSource, /getStagePanForPosition/);
  assert.match(canvasSource, /smoothStagePan/);
  assert.match(canvasSource, /stage-quest-marker/);
  assert.match(canvasSource, /workspace-2d-minimap-actor/);
  assert.match(canvasSource, /미니맵/);
  assert.match(canvasSource, /작업 보드/);
});

test("workspace detail passes the current player into the 2d workspace", () => {
  assert.match(detailSource, /playerPositionOverride/);
  assert.match(detailSource, /defaultPlayerPosition/);
  assert.match(detailSource, /handlePlayerPositionChange/);
  assert.match(detailSource, /playerPosition=\{playerPositionOverride \?\? defaultPlayerPosition\}/);
  assert.match(detailSource, /playerAvatar=\{storedCharacterProfile\}/);
  assert.match(detailSource, /onPlayerPositionChange=\{handlePlayerPositionChange\}/);
});

test("workspace 2d canvas renders a distinct player actor contract", () => {
  assert.match(canvasSource, /type PixelAvatarDirection/);
  assert.match(canvasSource, /playerAvatar/);
  assert.match(canvasSource, /playerDirection/);
  assert.match(canvasSource, /isPlayer/);
  assert.match(canvasSource, /stage-actor__player-marker/);
  assert.match(canvasSource, /플레이어/);
  assert.match(canvasSource, /나/);
  assert.match(canvasSource, /direction=\{direction\}/);
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
  assert.match(cssSource, /\.workspace-2d-stage-shell/);
  assert.match(cssSource, /\.workspace-2d-minimap/);
  assert.match(cssSource, /\.workspace-2d-map-hud/);
  assert.match(cssSource, /\.workspace-2d-hero-hud/);
  assert.match(cssSource, /\.workspace-2d-minimap-actor/);
  assert.match(cssSource, /\.workspace-2d-dialogue/);
  assert.match(cssSource, /\.stage-room/);
  assert.match(cssSource, /\.stage-room-label/);
  assert.match(cssSource, /\.stage-corridor/);
  assert.match(cssSource, /\.stage-quest-marker/);
  assert.match(cssSource, /\.stage-actor/);
  assert.match(cssSource, /\.stage-actor--walking/);
  assert.match(cssSource, /@keyframes stage-actor-working/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
