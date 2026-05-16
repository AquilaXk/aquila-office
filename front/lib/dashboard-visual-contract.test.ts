import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import {
  BRAND_MASCOT_PATH,
  REPRESENTATIVE_IMAGE_PATHS,
  SERVICE_NAME,
} from "./branding.ts";

const appRoot = process.cwd();

const pageSource = readFileSync(path.join(appRoot, "app/page.tsx"), "utf8");
const cssSource = readFileSync(path.join(appRoot, "app/globals.css"), "utf8");

test("home dashboard uses aquila office brand assets", () => {
  assert.equal(SERVICE_NAME, "aquila office");
  assert.match(pageSource, /SERVICE_NAME/);
  assert.match(pageSource, /BRAND_MASCOT_PATH/);
  assert.match(pageSource, /REPRESENTATIVE_IMAGE_PATHS/);
  assert.match(pageSource, assetPattern(BRAND_MASCOT_PATH));
  for (const imagePath of REPRESENTATIVE_IMAGE_PATHS) {
    assert.match(pageSource, assetPattern(imagePath));
  }
});

test("home dashboard exposes gothic operations visual tokens", () => {
  assert.match(cssSource, /--gothic-page/);
  assert.match(cssSource, /--gothic-brass/);
  assert.match(cssSource, /\.brand-crest/);
  assert.match(cssSource, /\.asset-strip/);
  assert.match(cssSource, /color-scheme:\s*dark/);
});

function assetPattern(publicPath: string): RegExp {
  return new RegExp(path.basename(publicPath).replace(".", "\\."));
}
