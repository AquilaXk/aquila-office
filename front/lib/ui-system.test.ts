import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");

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
  assert.match(source, /Card/);
  assert.match(source, /Modal/);
  assert.match(source, /Toast/);
  assert.match(source, /Dropdown/);
  assert.match(source, /Tab/);
  assert.match(source, /ChatBubble/);
  assert.match(source, /Input/);
  assert.match(source, /cn/);
  assert.match(source, /Textarea/);
});

test("shared interaction components expose accessibility contracts", () => {
  assert.match(readSource("components/ui/Modal.tsx"), /role="dialog"/);
  assert.match(readSource("components/ui/Modal.tsx"), /Escape/);
  assert.match(readSource("components/ui/Modal.tsx"), /Modal\.Body|ModalBody/);
  assert.match(readSource("components/ui/Modal.tsx"), /Modal\.Footer|ModalFooter/);
  assert.match(readSource("components/ui/Dropdown.tsx"), /aria-expanded/);
  assert.match(readSource("components/ui/Tab.tsx"), /role="tablist"/);
  assert.match(readSource("components/ui/Toast.tsx"), /role="status"/);
  assert.match(readSource("components/ui/Input.tsx"), /aria-invalid/);
  assert.match(readSource("components/ui/Input.tsx"), /Textarea/);
  assert.match(readSource("components/ui/ChatBubble.tsx"), /aria-label/);
});

test("shared source base supports imported arcade and modal contracts", () => {
  const arcadeIndex = readSource("components/arcade/index.ts");
  const buttonSource = readSource("components/ui/Button.tsx");
  const modalSource = readSource("components/ui/Modal.tsx");
  const globalsSource = readSource("app/globals.css");

  assert.match(arcadeIndex, /T4Screen/);
  assert.match(arcadeIndex, /T4Panel/);
  assert.match(arcadeIndex, /T4Bar/);
  assert.match(arcadeIndex, /GlyphText/);
  assert.match(arcadeIndex, /t4/);
  assert.match(buttonSource, /icon\?: ReactNode/);
  assert.match(buttonSource, /loading\?: boolean/);
  assert.match(buttonSource, /"lg"/);
  assert.match(modalSource, /size\?: "sm" \| "md" \| "lg" \| "full"/);
  assert.match(modalSource, /disableEscapeClose/);
  assert.match(globalsSource, /styles\/tokens\.css/);
  assert.match(globalsSource, /styles\/theme-web\.css/);
});

test("icon button requires an accessible label contract", () => {
  const source = readSource("components/ui/IconButton.tsx");

  assert.match(source, /aria-label/);
  assert.match(source, /label: string/);
});

test("arcade index exports gothic operation controls", () => {
  const source = readSource("components/arcade/index.ts");

  assert.match(source, /ArcadeFrame/);
  assert.match(source, /CommandBar/);
  assert.match(source, /MetricTile/);
  assert.match(source, /StatusPip/);
  assert.match(source, /T4Screen/);
  assert.match(source, /GlyphText/);
});

test("arcade controls use shared UI and gothic class contracts", () => {
  assert.match(readSource("components/arcade/ArcadeFrame.tsx"), /ao-arcade-frame/);
  assert.match(readSource("components/arcade/CommandBar.tsx"), /ao-command-bar/);
  assert.match(readSource("components/arcade/MetricTile.tsx"), /ao-metric-tile/);
  assert.match(readSource("components/arcade/StatusPip.tsx"), /ao-status-pip/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
