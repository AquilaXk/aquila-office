import test from "node:test";
import assert from "node:assert/strict";

import {
  MAX_HIGHLIGHT_CHARS,
  MAX_HIGHLIGHT_LINES,
  inferCodeLanguage,
  sanitizeDownloadFilename,
  tokenizeCodeLines,
} from "./code-highlight.ts";

test("inferCodeLanguage resolves common extensions and MIME hints", () => {
  assert.equal(inferCodeLanguage("src/App.tsx", "text/plain"), "ts");
  assert.equal(inferCodeLanguage("TaskController.java", null), "java");
  assert.equal(inferCodeLanguage("oauth/mod.rs", null), "rust");
  assert.equal(inferCodeLanguage("no-ext", "application/json"), "json");
  assert.equal(inferCodeLanguage("README.md", null), "md");
  assert.equal(inferCodeLanguage("notes.txt", null), "text");
});

test("tokenizeCodeLines highlights keywords, functions, types, strings, comments across lines", () => {
  const sample = [
    "/* multi-line",
    "   comment */",
    "export async function runTask(item: TaskResult): Promise<number> {",
    "  const count = 42;",
    "  return count; // inline",
    "}",
  ].join("\n");

  const result = tokenizeCodeLines(sample, "ts");
  assert.equal(result.highlighted, true);
  assert.equal(result.highlightSkipped, false);
  assert.equal(result.lines.length, 6);

  assert.equal(result.lines[0][0].kind, "com");
  assert.equal(result.lines[1][0].kind, "com");

  const line3Kinds = result.lines[2].map((token) => `${token.kind}:${token.text}`);
  assert.ok(line3Kinds.includes("kw:export"));
  assert.ok(line3Kinds.includes("kw:async"));
  assert.ok(line3Kinds.includes("kw:function"));
  assert.ok(line3Kinds.includes("fn:runTask"));
  assert.ok(line3Kinds.includes("type:TaskResult"));

  const line5 = result.lines[4];
  assert.ok(line5.some((token) => token.kind === "com" && token.text === "// inline"));
});

test("tokenizeCodeLines skips highlighting fail-closed when character or line budget is exceeded", () => {
  const oversizedChars = "const x = 1;\n".repeat(Math.ceil((MAX_HIGHLIGHT_CHARS + 64) / 13));
  const charResult = tokenizeCodeLines(oversizedChars, "ts");
  assert.equal(charResult.highlighted, false);
  assert.equal(charResult.highlightSkipped, true);

  const oversizedLines = "const x = 1;\n".repeat(MAX_HIGHLIGHT_LINES + 5);
  const lineResult = tokenizeCodeLines(oversizedLines, "ts");
  assert.equal(lineResult.highlighted, false);
  assert.equal(lineResult.highlightSkipped, true);
});

test("sanitizeDownloadFilename strips traversal and invalid characters", () => {
  assert.equal(sanitizeDownloadFilename("../../etc/passwd"), "passwd");
  assert.equal(sanitizeDownloadFilename("src/main/App.java"), "App.java");
  assert.equal(sanitizeDownloadFilename("..hidden"), "hidden");
  assert.equal(sanitizeDownloadFilename("   "), "artifact.txt");
});
