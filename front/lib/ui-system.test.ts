import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("theme tokens expose gothic operation color and radius contracts", () => {
  const source = readSource("lib/theme.ts");

  assert.match(source, /gothicThemeTokens/);
  assert.match(source, /ThemeTokenName/);
  assert.match(source, /surfaceRaised/);
  assert.match(source, /brass/);
  assert.match(source, /radiusControl/);
});

test("common ui index exports shared controls", () => {
  const source = readSource("components/ui/index.ts");

  assert.match(source, /Button/);
  assert.match(source, /IconButton/);
  assert.match(source, /Badge/);
  assert.match(source, /Panel/);
  assert.match(source, /TextField/);
  assert.match(source, /cn/);
});

test("icon button requires an accessible label contract", () => {
  const source = readSource("components/ui/IconButton.tsx");

  assert.match(source, /aria-label/);
  assert.match(source, /label: string/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
