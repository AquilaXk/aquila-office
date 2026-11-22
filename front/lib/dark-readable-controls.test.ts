import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");

test("auth google button is clipped inside a dark fixed frame", () => {
  const source = readSource("app/auth/page.tsx");

  assert.match(source, /className="auth-google-frame"/);
  assert.match(source, /width:\s*284/);
  assert.match(source, /height:\s*48/);
  assert.match(source, /overflow:\s*"hidden"/);
  assert.match(source, /background:\s*"#202124"/);
});

test("global app surfaces do not use bright hex backgrounds with dark theme text", () => {
  const css = readSource("app/globals.css");
  const lightBackgrounds = css
    .split("\n")
    .map((line, index) => ({ line: line.trim(), lineNumber: index + 1 }))
    .map(({ line, lineNumber }) => ({
      line,
      lineNumber,
      hex: line.match(/^background:\s*(#[0-9a-f]{6})\b/i)?.[1],
    }))
    .filter(
      ({ hex }) => hex !== undefined && relativeLuminance(hex) > 0.72,
    )
    .map(({ line, lineNumber }) => ({ line, lineNumber }));

  assert.deepEqual(lightBackgrounds, []);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}

function relativeLuminance(hex: string): number {
  const channels = [1, 3, 5].map((start) =>
    Number.parseInt(hex.slice(start, start + 2), 16) / 255,
  );
  const [red, green, blue] = channels.map((channel) =>
    channel <= 0.03928
      ? channel / 12.92
      : ((channel + 0.055) / 1.055) ** 2.4,
  );

  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}
