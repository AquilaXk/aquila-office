import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");
const repoRoot = path.resolve(appRoot, "..");

function readFrontSource(relativePath: string): string {
  return readFileSync(path.join(appRoot, relativePath), "utf8");
}

function readRepoSource(relativePath: string): string {
  return readFileSync(path.join(repoRoot, relativePath), "utf8");
}

test("frontend scripts expose the documented quality gates", () => {
  const packageJson = JSON.parse(readFrontSource("package.json")) as {
    scripts?: Record<string, string>;
  };

  assert.equal(packageJson.scripts?.lint, "eslint");
  assert.equal(packageJson.scripts?.["test:node"], "node --test lib/*.test.ts");
  assert.equal(packageJson.scripts?.["test:browser"], "playwright test");
});

test("frontend ci runs lint node and browser regression gates", () => {
  const workflow = readRepoSource(".github/workflows/ci.yml");

  assert.match(workflow, /name: Run frontend lint[\s\S]*?working-directory: front[\s\S]*?run: npm run lint/);
  assert.match(
    workflow,
    /name: Run frontend node tests[\s\S]*?working-directory: front[\s\S]*?run: npm run test:node/,
  );
  assert.match(
    workflow,
    /name: Run frontend browser regression[\s\S]*?working-directory: front[\s\S]*?run: npm run test:browser/,
  );
});

test("browser regression covers routes and interactions without screenshot assertions", () => {
  const browserSpec = readFrontSource("e2e/browser-ux.spec.ts");

  for (const routeEvidence of [
    'page.goto("/")',
    'page.goto("/auth?returnTo=/workspaces/new")',
    'page.goto("/auth/character?returnTo=/workspaces")',
    'page.goto("/workspaces")',
    'page.getByRole("link", { name: /NEW WORKSPACE/ }).click()',
    "page.goto(`/workspaces/${workspaceId}`)",
    "page.goto(`/workspaces/${workspaceId}/settings`)",
    'page.goto("/workspaces/gateway")',
    "page.goto(`/invites/${inviteToken}`)",
  ]) {
    assert.match(browserSpec, new RegExp(escapeRegExp(routeEvidence)));
  }

  for (const interactionEvidence of [
    'page.getByRole("menuitem", { name: /CONTINUE/ }).click()',
    'page.getByRole("menuitem", { name: /LOGIN/ }).click()',
    'page.getByRole("status")',
    'page.keyboard.press("ArrowDown")',
    'page.keyboard.press("d")',
    'page.keyboard.down("Shift")',
    'page.keyboard.press("ArrowRight")',
    'page.getByRole("button", { name: "LOGOUT" }).click()',
    'getByRole("dialog", { name: "Connection settings" })',
    'getByRole("dialog", { name: "Task dashboard" })',
    'getByRole("dialog", { name: "Workspace files" })',
    'getByLabel("Agent 메시지")',
    'getByLabel("Task 상태 변경").selectOption("COMPLETED")',
    'getByRole("heading", { name: "REGISTER A GATEWAY" })',
    'getByRole("heading", { name: "JOIN A WORKSPACE" })',
    'getByRole("button", { name: /WORKSPACE INFO/ })',
    'getByRole("button", { name: /ROSTER/ })',
    'getByRole("button", { name: /^INVITE$/ })',
    'getByRole("button", { name: /INVITE LOG/ })',
    'getByRole("button", { name: /ACCEPT SUMMON/ })',
    'path === "/api/v1/auth/google/login"',
    'path === "/api/v1/auth/logout"',
    'path === "/api/v1/members/me/profile"',
    'path === `/api/v1/workspaces/${workspaceId}/invites`',
    'path === `/api/v1/workspaces/${workspaceId}/gateway/connection-test`',
    'path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/status`',
    'path === `/api/v1/invites/${inviteToken}`',
    'path === `/api/v1/invites/${inviteToken}/accept`',
    'window.localStorage.getItem("aquila-office.auth.accessToken")',
    'window.localStorage.getItem("aquila-office.auth.user")',
    'workspaceMembers: "current-user-second"',
    'memberId: 2',
    'getByRole("button", { name: /Aquila User 플레이어/ })',
    'getByRole("button", { name: /First Member 채팅/ })',
  ]) {
    assert.match(browserSpec, new RegExp(escapeRegExp(interactionEvidence)));
  }

  assert.doesNotMatch(browserSpec, /toHaveScreenshot|toMatchSnapshot|screenshot\(/);
});

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
