import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();
const scanRoots = ["app", "components"];
const sourceExtensions = new Set([".css", ".ts", ".tsx"]);
const minimumReadableFontSizePx = 12;

test("frontend avoids unreadable explicit micro font sizes", () => {
  const violations: string[] = [];

  for (const filePath of scanRoots.flatMap((root) => collectSourceFiles(path.join(appRoot, root)))) {
    const source = readFileSync(filePath, "utf8");
    const relativePath = path.relative(appRoot, filePath);

    for (const match of source.matchAll(/font-size:\s*(\d+)px\b/g)) {
      collectViolation(violations, relativePath, source, match.index, Number(match[1]));
    }

    for (const match of source.matchAll(/fontSize:\s*(\d+)\b/g)) {
      collectViolation(violations, relativePath, source, match.index, Number(match[1]));
    }
  }

  assert.deepEqual(violations, []);
});

function collectSourceFiles(directory: string): string[] {
  return readdirSync(directory).flatMap((entry) => {
    const filePath = path.join(directory, entry);
    const stats = statSync(filePath);

    if (stats.isDirectory()) {
      return collectSourceFiles(filePath);
    }

    return sourceExtensions.has(path.extname(filePath)) ? [filePath] : [];
  });
}

function collectViolation(
  violations: string[],
  relativePath: string,
  source: string,
  matchIndex: number | undefined,
  fontSize: number,
): void {
  if (fontSize >= minimumReadableFontSizePx) {
    return;
  }

  const line = source.slice(0, matchIndex).split("\n").length;
  violations.push(`${relativePath}:${line} uses ${fontSize}px`);
}
