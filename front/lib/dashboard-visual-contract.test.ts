import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import {
  BRAND_MASCOT_PATH,
  SERVICE_NAME,
} from "./branding.ts";

const appRoot = path.resolve(import.meta.dirname, "..");

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

test("title screen exposes desktop chrome and semantic visual tokens", () => {
  assert.match(cssSource, /--ui-bg/);
  assert.match(cssSource, /--gothic-page/);
  assert.match(cssSource, /\.title-screen/);
  assert.match(cssSource, /\.title-menu-item-active/);
  assert.match(cssSource, /color-scheme:\s*dark/);
});

test("auth screen uses AppTopChrome and standard desktop surfaces", () => {
  assert.match(authSource, /AppTopChrome/);
  assert.match(authSource, /ao-surface-panel/);
  assert.doesNotMatch(authSource, /T4Screen|T4Panel|GlyphText/);
  assert.match(authSource, /Google 계정으로 로그인/);
  assert.match(authSource, /워크스페이스 계정 인증/);
  assert.match(authSource, /GoogleLogin/);
  assert.match(cssSource, /styles\/tokens\.css/);
  assert.match(cssSource, /styles\/chrome\.css/);
  assert.match(cssSource, /styles\/theme-web\.css/);
});

test("workspace detail operation panels use readable semantic contrast", () => {
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
    assert.match(body, /background:[^;]*var\(--gothic-surface(?:-raised)?\)/);
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

test("route pages and shared wrappers eliminate inline neon glows, border-cutting legends, and arcade copy", () => {
  const checkedFiles = [
    "app/page.tsx",
    "app/auth/page.tsx",
    "app/auth/character/page.tsx",
    "app/workspaces/page.tsx",
    "app/workspaces/new/page.tsx",
    "app/workspaces/gateway/page.tsx",
    "app/workspaces/[workspaceId]/settings/page.tsx",
    "app/invites/[token]/page.tsx",
    "components/arcade/Panel.tsx",
    "components/workspace/WorkspaceShell.tsx",
    "components/workspace/Workspace2DCanvas.tsx",
    "components/desktop/AppTopChrome.tsx",
  ];

  for (const relPath of checkedFiles) {
    const content = readFileSync(path.join(appRoot, relPath), "utf8");
    assert.doesNotMatch(
      content,
      /textShadow\s*:/,
      `${relPath} must not contain inline textShadow glow`,
    );
    assert.doesNotMatch(
      content,
      /boxShadow\s*:\s*[`"'][^`"']*0 0 (?:10|12|14|16|18|20|22|24)px/,
      `${relPath} must not contain inline neon boxShadow glow`,
    );
  }

  for (const routeFile of [
    "app/page.tsx",
    "app/auth/page.tsx",
    "app/auth/character/page.tsx",
    "app/workspaces/page.tsx",
    "app/workspaces/new/page.tsx",
    "app/workspaces/gateway/page.tsx",
    "app/workspaces/[workspaceId]/settings/page.tsx",
    "app/invites/[token]/page.tsx",
  ]) {
    const content = readFileSync(path.join(appRoot, routeFile), "utf8");
    assert.doesNotMatch(content, /T4Screen|T4Panel|GlyphText/, `${routeFile} must not use arcade wrappers`);
    assert.doesNotMatch(content, /[◆▶★♦]/, `${routeFile} must not use arcade glyphs`);
  }

  const panelSource = readFileSync(path.join(appRoot, "components/arcade/Panel.tsx"), "utf8");
  assert.doesNotMatch(panelSource, /top:\s*-9/);

  const workspacesSource = readFileSync(path.join(appRoot, "app/workspaces/page.tsx"), "utf8");
  assert.match(workspacesSource, /ao-kpi-grid/);

  const newWorkspaceSource = readFileSync(path.join(appRoot, "app/workspaces/new/page.tsx"), "utf8");
  assert.doesNotMatch(newWorkspaceSource, /STEP 01 \/ 02/);

  const settingsSource = readFileSync(
    path.join(appRoot, "app/workspaces/[workspaceId]/settings/page.tsx"),
    "utf8",
  );
  assert.doesNotMatch(settingsSource, /COMMAND DECK/);
  assert.doesNotMatch(settingsSource, />KICK</);

  const sceneSource = readFileSync(path.join(appRoot, "lib/workspace-2d.ts"), "utf8");
  assert.doesNotMatch(sceneSource, /meeting in progress/);
  assert.match(sceneSource, /collaboration zone/);

  assert.doesNotMatch(pageSource, /PRESS START/);
});
