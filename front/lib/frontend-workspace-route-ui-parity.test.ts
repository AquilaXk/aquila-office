import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

function readFrontSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}

test("workspace list panel keeps section heading above clipped list cards", () => {
  const source = readFrontSource("app/workspaces/page.tsx");
  assert.match(source, /ao-surface-panel-header/);
  assert.match(source, /내 워크스페이스/);
  assert.doesNotMatch(source, /T4Panel/);
});

test("gateway and new workspace routes use desktop chrome with real API bindings and standard copy", () => {
  const gatewaySource = readFrontSource("app/workspaces/gateway/page.tsx");
  const newWorkspaceSource = readFrontSource("app/workspaces/new/page.tsx");

  for (const expected of [
    /AppTopChrome/,
    /listWorkspaces/,
    /fetchWorkspaceGatewayStatus/,
    /testWorkspaceGatewayConnection/,
    /createWorkspaceGatewayBinding/,
    /aria-label="Gateway URL"/,
    /aria-label="Gateway Token"/,
    /연결 테스트/,
    /OpenClaw 연결 저장/,
  ]) {
    assert.match(gatewaySource, expected);
  }
  assert.doesNotMatch(gatewaySource, /aio\.openclaw\.gateways/);

  for (const expected of [
    /AppTopChrome/,
    /새 워크스페이스 만들기/,
    /워크스페이스 정보/,
    /워크스페이스 이름/,
    /워크스페이스 생성/,
  ]) {
    assert.match(newWorkspaceSource, expected);
  }
});

test("workspace-facing screens avoid game and arcade styled visible copy", () => {
  const forbiddenTermsByRoute: Record<string, string[]> = {
    "app/page.tsx": ["HERO {", '"PLAYER"', "PRESS START"],
    "app/auth/page.tsx": ["LOADING SAVE FILE", "SIGN IN TO CONTINUE"],
    "app/workspaces/page.tsx": [
      "SELECT YOUR QUEST",
      "HERO {",
      '"PLAYER"',
      "WORK RPG",
      "소환",
    ],
    "app/workspaces/gateway/page.tsx": ["WARP", ">LINK GATE<", "소환"],
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
      ">QUESTS<",
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
