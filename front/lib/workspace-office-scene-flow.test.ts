import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();

const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
const canvasSource = readSource("components/workspace/Workspace2DCanvas.tsx");
const sceneSource = readSource("lib/workspace-2d.ts");
const cssSource = readSource("app/globals.css");

test("workspace detail promotes the office scene as the first screen", () => {
  assert.match(detailSource, /workspace-office-scene/);
  assert.match(detailSource, /aria-label="워크스페이스 오피스 씬"/);
  assert.match(detailSource, /officeSceneNavItems/);
  assert.match(detailSource, /connections/);
  assert.match(detailSource, /tasks/);
  assert.match(detailSource, /hire-agent/);
  assert.match(detailSource, /files/);
  assert.match(detailSource, /leave-workspace/);
});

test("workspace office scene opens agent hire modal with role emoji and skill files", () => {
  assert.match(detailSource, /agentHireOpen/);
  assert.match(detailSource, /AgentHireModal/);
  assert.match(detailSource, /aria-label="Agent hire"/);
  assert.match(detailSource, /emoji:\s*agentCreateForm\.emoji\.trim\(\)/);
  assert.match(detailSource, /skillFiles:\s*skillFiles\.length > 0/);
  assert.match(detailSource, /form\.skillFiles\.map/);
  assert.match(detailSource, /normalizeAgentSkillFiles/);
  assert.match(detailSource, /Agent 이름/);
  assert.match(detailSource, /역할/);
  assert.match(detailSource, /Workspace Path/);
  assert.match(detailSource, /Emoji/);
  assert.match(detailSource, /Skill 파일/);
  assert.match(detailSource, /고용/);
});

test("workspace 2d canvas exposes HUD, minimap, dialogue, and keyboard stage controls", () => {
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
  assert.match(canvasSource, /getStageMiniMapRooms/);
  assert.match(canvasSource, /getStagePanForPosition/);
  assert.match(canvasSource, /smoothStagePan/);
  assert.match(canvasSource, /stage-quest-marker/);
  assert.match(canvasSource, /workspace-2d-minimap-actor/);
  assert.match(canvasSource, /aria-label="Office scene minimap"/);
  assert.match(canvasSource, /aria-label="Quest"/);
});

test("workspace detail passes the current player into the 2d workspace", () => {
  assert.match(detailSource, /playerPositionOverride/);
  assert.match(detailSource, /defaultPlayerPosition/);
  assert.match(detailSource, /handlePlayerPositionChange/);
  assert.match(detailSource, /playerPosition=\{playerPosition\}/);
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
  assert.match(canvasSource, /YOU/);
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
  assert.match(cssSource, /min-height:\s*calc\(100vh - 64px\)/);
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
