import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

function readFrontSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}

test("workspace list panel keeps ACTIVE WORKPLACES label outside clipped overflow", () => {
  const source = readFrontSource("app/workspaces/page.tsx");
  const labelIndex = source.indexOf('label="ACTIVE WORKPLACES"');

  assert.notEqual(labelIndex, -1);

  const panelEnd = source.indexOf("</T4Panel>", labelIndex);
  const panelSource = source.slice(labelIndex, panelEnd);

  assert.doesNotMatch(panelSource, /overflow:\s*"hidden"/);
});

test("gateway and new workspace routes keep the same control surface with clearer copy", () => {
  const gatewaySource = readFrontSource("app/workspaces/gateway/page.tsx");
  const newWorkspaceSource = readFrontSource("app/workspaces/new/page.tsx");

  for (const expected of [
    /REGISTER A GATEWAY/,
    /LINKED ·/,
    /ACTIVE LINK/,
    /GATE FORM/,
    /DISPLAY NAME/,
    /GATEWAY URL/,
    /TOKEN/,
    /LINK GATEWAY/,
  ]) {
    assert.match(gatewaySource, expected);
  }

  for (const expected of [
    /T4Screen title="NEW WORKSPACE"/,
    /CREATE A NEW WORKSPACE/,
    /WORKSPACE DETAILS/,
    /WORKSPACE NAME/,
    /DESCRIPTION/,
    /CREATE WORKSPACE/,
  ]) {
    assert.match(newWorkspaceSource, expected);
  }
});

test("workspace-facing screens avoid game and grimdark styled visible copy", () => {
  const forbiddenTermsByRoute: Record<string, string[]> = {
    "app/page.tsx": ["HERO {", '"PLAYER"'],
    "app/auth/page.tsx": ["LOADING SAVE FILE"],
    "app/workspaces/page.tsx": [
      "SELECT YOUR QUEST",
      "HERO {",
      '"PLAYER"',
      "WORK RPG",
      "소환",
    ],
    "app/workspaces/gateway/page.tsx": ["WARP", ">LINK GATE</GlyphText>", "소환"],
    "app/workspaces/new/page.tsx": [
      "CHARACTER CREATE",
      "FORM A NEW WORKSPACE",
      "FORM WORKSPACE",
      "BRIEFING",
      "길드",
    ],
    "app/invites/[token]/page.tsx": ["WARP", "SUMMON", "ACCEPT SUMMON"],
    "app/workspaces/[workspaceId]/settings/page.tsx": ["BRIEFING", "SUMMON LINK"],
    "components/workspace/WorkspaceShell.tsx": ["LOADING WORLD"],
    "components/workspace/Workspace2DCanvas.tsx": [
      'aria-label="Quest"',
      "<span>QUEST</span>",
      ">QUESTS</DialogueChoice>",
      "다음 quest",
      "hand off a quest",
    ],
  };
  const violations: string[] = [];

  for (const [route, forbiddenTerms] of Object.entries(forbiddenTermsByRoute)) {
    const source = readFrontSource(route);
    for (const term of forbiddenTerms) {
      if (source.includes(term)) {
        violations.push(`${route} includes ${term}`);
      }
    }
  }

  assert.deepEqual(violations, []);
});
