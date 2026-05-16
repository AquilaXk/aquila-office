import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

const pageSource = readFileSync(path.join(appRoot, "app/page.tsx"), "utf8");
const cssSource = readFileSync(path.join(appRoot, "app/globals.css"), "utf8");

test("home dashboard uses aquila office brand assets", () => {
  assert.match(pageSource, /aquila office/);
  assert.match(pageSource, /brand-mascot\.png/);
  assert.match(pageSource, /office-bank-preview\.png/);
  assert.match(pageSource, /office-blog-preview\.png/);
});

test("home dashboard exposes gothic operations visual tokens", () => {
  assert.match(cssSource, /--gothic-page/);
  assert.match(cssSource, /--gothic-brass/);
  assert.match(cssSource, /\.brand-crest/);
  assert.match(cssSource, /\.asset-strip/);
  assert.match(cssSource, /color-scheme:\s*dark/);
});
