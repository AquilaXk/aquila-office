import test from "node:test";
import assert from "node:assert/strict";
import {
  APPLE_TOUCH_ICON_PATH,
  BRAND_MASCOT_PATH,
  FAVICON_PATH,
  REPRESENTATIVE_IMAGE_PATHS,
  SERVICE_DESCRIPTION,
  SERVICE_NAME,
} from "./branding.ts";

test("branding constants expose aquila office service identity", () => {
  assert.equal(SERVICE_NAME, "aquila office");
  assert.match(SERVICE_DESCRIPTION, /workspace/i);
  assert.match(SERVICE_DESCRIPTION, /agent/i);
});

test("branding constants expose required public asset paths", () => {
  assert.equal(FAVICON_PATH, "/favicon.ico");
  assert.equal(APPLE_TOUCH_ICON_PATH, "/apple-touch-icon.png");
  assert.equal(BRAND_MASCOT_PATH, "/brand-mascot.png");
  assert.deepEqual(REPRESENTATIVE_IMAGE_PATHS, [
    "/aquila-office-readme-preview.png",
  ]);
  assert(!REPRESENTATIVE_IMAGE_PATHS.includes("/office-bank-preview.png"));
  assert(!REPRESENTATIVE_IMAGE_PATHS.includes("/office-blog-preview.png"));
});
