import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();
const source = readFileSync(path.join(appRoot, "app/auth/character/page.tsx"), "utf8");

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

test("character profile flow supports safe completion and retry", () => {
  assert.match(source, /safeReturnTo/);
  assert.match(source, /router\.replace\(returnTo\)/);
  assert.match(source, /onClick=\{\(\) => void loadProfile\(\)\}/);
  assert.match(source, /다시 시도/);
  assert.match(source, /프로필이 저장되었습니다/);
});
