import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const source = readFileSync(path.join(appRoot, "app/auth/character/page.tsx"), "utf8");
const cssSource = readFileSync(path.join(appRoot, "app/globals.css"), "utf8");
const workspaceSource = readFileSync(path.join(appRoot, "app/workspaces/[workspaceId]/page.tsx"), "utf8");
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
  assert.match(source, /ModeButton/);
  assert.match(source, /ORIGINAL/);
  assert.match(source, /CUSTOM/);
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
  assert.match(source, /SAVING/);
  assert.match(source, /ENTER OFFICE/);
});

test("character profile exposes grimdark gothic sci-fi avatar art direction", () => {
  assert.match(source, /T4Screen/);
  assert.match(source, /T4Panel/);
  assert.match(source, /GlyphText/);
  assert.match(source, /CHOOSE YOUR AVATAR/);
  assert.match(source, /SET UP YOUR OFFICE PROFILE/);
  assert.match(cssSource, /styles\/tokens\.css/);
  assert.match(cssSource, /styles\/theme-web\.css/);
  assert.match(cssSource, /--gothic-brass/);
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

test("workspace detail consumes stored pixel avatar profiles", () => {
  assert.match(workspaceSource, /PixelAvatar/);
  assert.match(workspaceSource, /type PixelAvatarKind/);
  assert.match(workspaceSource, /CHARACTER_STORAGE_KEY\s*=\s*"ai-office\.character"/);
  assert.match(workspaceSource, /loadStoredCharacterProfile/);
  assert.match(workspaceSource, /memberProfileToStoredCharacter/);
  assert.match(workspaceSource, /pickAvatarKind/);

  for (const kind of ["mira", "alex", "kenji", "yuna", "diego", "iris"]) {
    assert.match(workspaceSource, new RegExp(`["']${kind}["']`));
  }
});

test("workspace detail applies stored avatar profile to the authenticated member", () => {
  assert.match(workspaceSource, /readAuthUser/);
  assert.match(workspaceSource, /currentMemberId/);
  assert.match(workspaceSource, /playerMemberId=\{currentMemberId\}/);
  assert.match(workspaceSource, /member\.memberId === currentMemberId/);
  assert.doesNotMatch(workspaceSource, /index === 0 && storedCharacterProfile/);
});
