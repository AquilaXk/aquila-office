import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";

const appRoot = path.resolve(import.meta.dirname, "..");
const source = readFileSync(path.join(appRoot, "app/auth/character/page.tsx"), "utf8");
const cssSource = readFileSync(path.join(appRoot, "app/globals.css"), "utf8");
const workspaceSource = readFileSync(path.join(appRoot, "app/workspaces/[workspaceId]/page.tsx"), "utf8");
const helpersSource = readFileSync(
  path.join(appRoot, "components/workspace/workspace-ui-helpers.ts"),
  "utf8",
);
const pixelAvatarPath = path.join(appRoot, "components/arcade/PixelAvatar.tsx");

test("character profile flow restores local and API profile state", () => {
  assert.match(source, /CHARACTER_STORAGE_KEY\s*=\s*"ai-office\.character"/);
  assert.match(source, /loadSavedCharacter/);
  assert.match(source, /writeSavedCharacter/);
  assert.match(source, /localStorage\.getItem\(CHARACTER_STORAGE_KEY\)/);
  assert.match(source, /localStorage\.setItem\(\s*CHARACTER_STORAGE_KEY/);
  assert.match(source, /getMyMemberProfile/);
  assert.match(source, /updateMyMemberProfile/);
});

test("character profile flow exposes avatar kind and color mode controls", () => {
  assert.match(source, /AVATARS/);
  assert.match(source, /COLOR_OPTIONS/);
  assert.match(source, /colorMode/);
  assert.match(source, /기본 팔레트/);
  assert.match(source, /사용자 지정 색상/);
});

test("character profile flow supports reference role options and color storage shape", () => {
  for (const role of ["Frontend", "Design", "Backend", "Product", "DevOps", "AI"]) {
    assert.match(source, new RegExp(role));
  }

  assert.match(source, /paletteOverride/);
  assert.match(source, /skin/);
  assert.match(source, /hair/);
  assert.match(source, /shirt/);
});

test("character profile flow supports safe completion and retry", () => {
  assert.match(source, /safeReturnTo/);
  assert.match(source, /router\.replace\(returnTo\)/);
  assert.match(source, /enterOffice/);
  assert.match(source, /저장 중/);
  assert.match(source, /저장 후 계속하기/);
});

test("character profile uses desktop chrome tokens without arcade wrappers", () => {
  assert.match(source, /AppTopChrome/);
  assert.match(source, /ao-surface-panel/);
  assert.doesNotMatch(source, /T4Screen|T4Panel|GlyphText/);
  assert.match(cssSource, /styles\/tokens\.css/);
  assert.match(cssSource, /styles\/chrome\.css/);
  assert.match(cssSource, /styles\/theme-web\.css/);

  const disallowedProductMarks = [
    "War" + "hammer",
    "Space " + "Marine",
    "Imper" + "ium",
    "Adep" + "tus",
    "Inqui" + "sition",
  ];
  const disallowedGenreNames = [
    "Iron " + "Marshal",
    "Relic " + "Scribe",
    "Forge " + "Warden",
    "Vox " + "Architect",
    "Command " + "Mantle",
    "Data " + "Reliquary",
    "Iron " + "Harness",
    "Signal " + "Choir",
    "dos" + "sier",
    "her" + "aldry",
    "sanction" + "ed",
  ];

  for (const term of [...disallowedProductMarks, ...disallowedGenreNames]) {
    assert.doesNotMatch(source, new RegExp(term));
  }
});

test("pixel avatar component exposes the required character sprite contract", () => {
  assert.equal(existsSync(pixelAvatarPath), true, "PixelAvatar.tsx must exist");
  const pixelAvatarSource = readFileSync(pixelAvatarPath, "utf8");

  for (const kind of ["alex", "mira", "kenji", "yuna", "diego", "iris", "agent"]) {
    assert.match(pixelAvatarSource, new RegExp(`["']${kind}["']`));
  }

  assert.match(pixelAvatarSource, /export type PixelAvatarKind/);
  assert.match(pixelAvatarSource, /export type PixelAvatarDirection\s*=\s*"S"\s*\|\s*"N"\s*\|\s*"E"\s*\|\s*"W"/);
  assert.match(pixelAvatarSource, /export const PA_PALETTES/);
  assert.match(pixelAvatarSource, /HERO_FRAMES/);
  assert.match(pixelAvatarSource, /HERO_WALK_FRAMES/);
  assert.match(pixelAvatarSource, /paletteOverride/);
});

test("character profile uses pixel avatar kinds and storage shape", () => {
  assert.match(source, /PixelAvatar/);
  assert.match(source, /PA_PALETTES/);
  assert.match(source, /type PixelAvatarKind/);
  assert.match(source, /DEFAULT_COLORS/);

  for (const kind of ["alex", "mira", "kenji", "yuna", "diego", "iris"]) {
    assert.match(source, new RegExp(`kind:\\s*["']${kind}["']`));
  }

  for (const legacyKind of ["frontend", "design", "backend", "product", "devops"]) {
    assert.doesNotMatch(source, new RegExp(`value:\\s*["']${legacyKind}["']`));
  }

  assert.match(source, /kind,/);
  assert.match(source, /name,/);
  assert.match(source, /colors:/);
  assert.match(source, /colorMode,/);
  assert.match(source, /paletteOverride/);
});

test("workspace detail consumes stored pixel avatar profiles via UI helpers", () => {
  assert.match(helpersSource, /CHARACTER_STORAGE_KEY\s*=\s*"ai-office\.character"/);
  assert.match(helpersSource, /loadStoredCharacterProfile/);
  assert.match(helpersSource, /pickAvatarKind/);
  assert.match(workspaceSource, /loadStoredCharacterProfile/);
  assert.match(workspaceSource, /playerAvatar=\{storedCharacterProfile\}/);

  for (const kind of ["mira", "alex", "kenji", "yuna", "diego", "iris"]) {
    assert.match(helpersSource, new RegExp(`["']${kind}["']`));
  }
});

test("workspace detail applies stored avatar profile to the authenticated member", () => {
  assert.match(workspaceSource, /readAuthUser/);
  assert.match(workspaceSource, /currentMemberId/);
  assert.match(workspaceSource, /Workspace2DCanvas/);
  assert.match(workspaceSource, /playerMemberId=\{currentMemberId\}/);
  assert.match(workspaceSource, /playerAvatar=\{storedCharacterProfile\}/);
  assert.doesNotMatch(workspaceSource, /member\.memberId === currentMemberId/);
  assert.doesNotMatch(workspaceSource, /index === 0 && storedCharacterProfile/);
});
