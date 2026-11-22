import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import test from "node:test";
import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import ts from "typescript";

async function loadMarkdownContent() {
  const filePath = path.join(path.resolve(import.meta.dirname, ".."), "components/ui/MarkdownContent.tsx");
  const source = readFileSync(filePath, "utf8");
  // Transpile with React classic runtime + dynamic ESM wrappers avoided via Node module load
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.React,
      esModuleInterop: true,
    },
  }).outputText;

  const modExports: Record<string, unknown> = {};
  const mod = { exports: modExports };
  const localRequire = createRequire(filePath);
  const reactMarkdown = await import("react-markdown");
  const remarkGfm = await import("remark-gfm");
  const lucideReact = await import("lucide-react");

  const customRequire = (id: string) => {
    if (id === "react") return { __esModule: true, default: React, ...React };
    if (id === "react-markdown") return { __esModule: true, ...reactMarkdown };
    if (id === "remark-gfm") return { __esModule: true, ...remarkGfm };
    if (id === "lucide-react") return lucideReact;
    return localRequire(id);
  };

  const fn = new Function("React", "require", "module", "exports", transpiled);
  fn(React, customRequire, mod, modExports);
  return (mod.exports.default ?? modExports) as React.ComponentType<{ content: string }>;
}

test("MarkdownContent renders unfenced/no-language multiline fenced blocks inside MarkdownCodeBlock", async () => {
  const MarkdownContent = await loadMarkdownContent();
  const sample = [
    "Use `inlineVar` here.",
    "",
    "```",
    "const plain = 1;",
    "const two = 2;",
    "```",
    "",
    "```ts",
    "const typed = true;",
    "```",
  ].join("\n");

  const html = renderToStaticMarkup(React.createElement(MarkdownContent, { content: sample }));

  assert.match(html, /<code class="markdown-artifact-inline-code">inlineVar<\/code>/);
  const blockMatches = html.match(/class="markdown-artifact-code"/g) ?? [];
  assert.equal(blockMatches.length, 2, `expected 2 code blocks, rendered html:\n${html}`);
  assert.match(
    html,
    /<span>code<\/span>[\s\S]*<pre><code>const plain = 1;\nconst two = 2;\n<\/code><\/pre>/,
  );
  assert.match(html, /<span>ts<\/span>[\s\S]*<pre><code>const typed = true;\n<\/code><\/pre>/);
});
