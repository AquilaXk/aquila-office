import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import {
  BRAND_MASCOT_PATH,
  SERVICE_NAME,
} from "./branding.ts";

const appRoot = process.cwd();

const pageSource = readFileSync(path.join(appRoot, "app/page.tsx"), "utf8");
const authSource = readFileSync(path.join(appRoot, "app/auth/page.tsx"), "utf8");
const cssSource = readFileSync(path.join(appRoot, "app/globals.css"), "utf8");

test("title screen uses aquila office brand assets", () => {
  assert.equal(SERVICE_NAME, "aquila office");
  assert.match(pageSource, /SERVICE_NAME/);
  assert.match(pageSource, /BRAND_MASCOT_PATH/);
  assert.equal(BRAND_MASCOT_PATH, "/brand-mascot.png");
});

test("title screen exposes gothic operations visual tokens", () => {
  assert.match(cssSource, /--gothic-page/);
  assert.match(cssSource, /--gothic-brass/);
  assert.match(cssSource, /\.title-screen/);
  assert.match(cssSource, /\.title-menu-item-active/);
  assert.match(cssSource, /color-scheme:\s*dark/);
});

test("auth screen follows the title screen visual system", () => {
  assert.match(authSource, /SERVICE_NAME/);
  assert.match(authSource, /BRAND_MASCOT_PATH/);
  assert.match(authSource, /auth-crest/);
  assert.match(authSource, /auth-menu-line/);
  assert.match(cssSource, /\.auth-page/);
  assert.match(cssSource, /\.auth-crest/);
  assert.match(cssSource, /\.auth-google-action/);
  assert.match(cssSource, /max-width:\s*100%/);
});
