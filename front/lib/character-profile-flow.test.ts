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
  assert.match(source, /loadStoredCharacter/);
  assert.match(source, /saveStoredCharacter/);
  assert.match(source, /localStorage\.getItem\(CHARACTER_STORAGE_KEY\)/);
  assert.match(source, /localStorage\.setItem\(CHARACTER_STORAGE_KEY/);
  assert.match(source, /fetchMemberProfile/);
  assert.match(source, /updateMemberProfile/);
});

test("character profile flow exposes avatar kind and color mode controls", () => {
  assert.match(source, /AVATAR_OPTIONS/);
  assert.match(source, /COLOR_MODE_OPTIONS/);
  assert.match(source, /colorMode/);
  assert.match(source, /role="radiogroup"/);
  assert.match(source, /aria-label="Avatar 종류 선택"/);
  assert.match(source, /aria-label="색상 모드 선택"/);
  assert.match(source, /Original/);
  assert.match(source, /Custom/);
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
  assert.match(source, /onClick=\{\(\) => void loadProfile\(\)\}/);
  assert.match(source, /다시 시도/);
  assert.match(source, /프로필이 저장되었습니다/);
});

test("character profile exposes grimdark gothic sci-fi avatar art direction", () => {
  assert.match(source, /CHARACTER_ARCHETYPES/);
  assert.match(source, /character-preview-gothic/);
  assert.match(source, /character-avatar-halo/);
  assert.match(source, /character-avatar-armor/);
  assert.match(source, /character-avatar-sigil/);
  assert.match(source, /Profile preview/);

  assert.match(cssSource, /character-preview-gothic/);
  assert.match(cssSource, /character-avatar-halo/);
  assert.match(cssSource, /character-avatar-armor/);
  assert.match(cssSource, /character-avatar-sigil/);
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
  assert.match(source, /DEFAULT_CHARACTER[\s\S]*avatarKind:\s*"mira"/);

  for (const kind of ["alex", "mira", "kenji", "yuna", "diego", "iris"]) {
    assert.match(source, new RegExp(`value:\\s*["']${kind}["']`));
  }

  for (const legacyKind of ["frontend", "design", "backend", "product", "devops"]) {
    assert.doesNotMatch(source, new RegExp(`value:\\s*["']${legacyKind}["']`));
  }

  assert.match(source, /kind:\s*normalizedCharacter\.avatarKind/);
  assert.match(source, /name:\s*normalizedCharacter\.displayName/);
  assert.match(source, /colors:\s*normalizedCharacter\.avatarColors/);
  assert.match(source, /colorMode:\s*normalizedCharacter\.colorMode/);
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
