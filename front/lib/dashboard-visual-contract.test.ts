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

function cssRuleBodiesForSelector(selector: string): string[] {
  const matchedBodies: string[] = [];
  for (const match of cssSource.matchAll(/([^{}]+)\{([^{}]*)\}/g)) {
    const selectorList = match[1]
      .split(",")
      .map((item) => item.trim());

    if (selectorList.includes(selector)) {
      matchedBodies.push(match[2]);
    }
  }

  assert.ok(matchedBodies.length > 0, `missing CSS rule for ${selector}`);
  return matchedBodies;
}

function cssRuleBodyForSelector(selector: string): string {
  return cssRuleBodiesForSelector(selector)[0];
}

function finalCssRuleBodyForSelector(selector: string): string {
  return cssRuleBodiesForSelector(selector).at(-1) ?? "";
}

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
  assert.match(authSource, /T4Screen/);
  assert.match(authSource, /T4Panel/);
  assert.match(authSource, /SIGN IN TO CONTINUE/);
  assert.match(authSource, /WORKSPACE MEMBER VERIFICATION/);
  assert.match(authSource, /GoogleLogin/);
  assert.match(cssSource, /styles\/tokens\.css/);
  assert.match(cssSource, /styles\/theme-web\.css/);
});

test("workspace detail operation panels use readable gothic contrast", () => {
  const panelSelectors = [
    ".workspace-task-section",
    ".workspace-member-section",
    ".workspace-detail-meta div",
    ".workspace-member-row",
    ".workspace-member-empty",
  ];

  for (const selector of panelSelectors) {
    const body = cssRuleBodyForSelector(selector);

    assert.match(body, /border:\s*1px solid var\(--gothic-line\)/);
    assert.match(body, /background:[^;]*var\(--gothic-surface(?:-raised)?\)/s);
    assert.doesNotMatch(body, /#(?:fbfcfe|ffffff|eff6ff)/i);
  }

  assert.match(cssRuleBodyForSelector(".workspace-detail-label"), /color:\s*var\(--gothic-muted\)/);
  assert.match(cssRuleBodyForSelector(".workspace-section-title h2"), /color:\s*var\(--gothic-parchment\)/);
  assert.match(cssRuleBodyForSelector(".workspace-member-main strong"), /color:\s*var\(--gothic-parchment\)/);
  assert.match(cssRuleBodyForSelector(".workspace-member-main p"), /color:\s*var\(--gothic-muted\)/);
  assert.match(cssRuleBodyForSelector(".workspace-member-meta"), /color:\s*var\(--gothic-muted\)/);

  const countBadgeBody = cssRuleBodyForSelector(".workspace-count-badge");
  assert.match(countBadgeBody, /background:\s*rgba\(50,\s*121,\s*249,\s*0\.12\)/);
  assert.match(countBadgeBody, /color:\s*var\(--gothic-parchment\)/);

  const avatarBody = cssRuleBodyForSelector(".workspace-member-avatar");
  assert.match(avatarBody, /background:\s*rgba\(50,\s*121,\s*249,\s*0\.14\)/);
  assert.match(avatarBody, /color:\s*var\(--gothic-brass\)/);

  const roleBadgeBody = finalCssRuleBodyForSelector(".role-badge");
  assert.match(roleBadgeBody, /background:\s*rgba\(50,\s*121,\s*249,\s*0\.1\)/);
  assert.match(roleBadgeBody, /color:\s*var\(--gothic-brass\)/);
});
