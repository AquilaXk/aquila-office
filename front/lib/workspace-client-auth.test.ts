import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();

const protectedClientFiles = [
  "lib/workspaces.ts",
  "lib/gateway.ts",
  "lib/invites.ts",
  "lib/tasks.ts",
  "lib/agents.ts",
  "lib/github.ts",
  "lib/artifacts.ts",
] as const;

const pageFiles = [
  "app/page.tsx",
  "app/invites/[token]/page.tsx",
  "app/workspaces/new/page.tsx",
  "app/workspaces/[workspaceId]/page.tsx",
  "app/workspaces/[workspaceId]/settings/page.tsx",
] as const;

test("workspace API clients use accessToken option without temporary member header", () => {
  for (const filePath of protectedClientFiles) {
    const source = readSource(filePath);

    assert.doesNotMatch(source, /WORKSPACE_MEMBER_HEADER|X-Aquila-Creator-Google-Sub/);
    assert.match(source, /\baccessToken\b/);
  }
});

test("workspace pages resolve access token through auth session helper", () => {
  for (const filePath of pageFiles) {
    const source = readSource(filePath);

    assert.doesNotMatch(source, /DEV_ACCESS_TOKEN|NEXT_PUBLIC_DEV_ACCESS_TOKEN/);
    assert.match(source, /resolveAccessToken/);
  }
});

test("home dashboard exposes auth token session actions", () => {
  const source = readSource("app/page.tsx");

  assert.match(source, /loginWithGoogle/);
  assert.match(source, /refreshAuthToken/);
  assert.match(source, /logoutAuth/);
  assert.match(source, /saveAuthTokens/);
  assert.match(source, /clearAuthTokens/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
