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
  "lib/members.ts",
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

test("workspace client exposes dashboard summary and member task stats contracts", () => {
  const source = readSource("lib/workspaces.ts");

  assert.match(source, /type WorkspaceDashboardSummary/);
  assert.match(source, /type WorkspaceMemberTaskStats/);
  assert.match(source, /fetchWorkspaceDashboardSummary/);
  assert.match(source, /fetchWorkspaceMemberTaskStats/);
  assert.match(source, /\/dashboard\/summary/);
  assert.match(source, /\/members\/task-stats/);
});

test("home dashboard exposes auth token session actions", () => {
  const source = readSource("app/page.tsx");

  assert.match(source, /loginWithGoogle/);
  assert.match(source, /refreshAuthToken/);
  assert.match(source, /logoutAuth/);
  assert.match(source, /saveAuthTokens/);
  assert.match(source, /clearAuthTokens/);
});

test("workspace settings exposes slack integration controls", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.match(source, /fetchSlackInstallUrl/);
  assert.match(source, /fetchSlackIntegrations/);
  assert.match(source, /createSlackIntegration/);
  assert.match(source, /updateSlackIntegration/);
  assert.match(source, /deleteSlackIntegration/);
  assert.match(source, /aria-label="Slack 연동 설정"/);
  assert.match(source, /Slack/);
});

test("workspace settings exposes slack channel mapping controls", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.match(source, /fetchSlackChannelMappings/);
  assert.match(source, /createSlackChannelMapping/);
  assert.match(source, /aria-label="Slack 채널 매핑 설정"/);
  assert.match(source, /slackChannelMappings/);
});

test("workspace detail exposes dashboard summary sections", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /fetchWorkspaceDashboardSummary/);
  assert.match(source, /fetchWorkspaceMemberTaskStats/);
  assert.match(source, /aria-label="워크스페이스 대시보드 요약"/);
  assert.match(source, /recentReports/);
  assert.match(source, /memberTaskStats/);
});

test("workspace detail exposes task mutation controls", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /createWorkspaceTask/);
  assert.match(source, /updateWorkspaceTaskStatus/);
  assert.match(source, /runWorkspaceTask/);
  assert.match(source, /aria-label="Task 생성"/);
  assert.match(source, /createPr/);
});

test("workspace detail exposes agent list surface", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /fetchWorkspaceAgents/);
  assert.match(source, /type WorkspaceAgent/);
  assert.match(source, /aria-label="워크스페이스 Agent"/);
  assert.match(source, /workspaceAgents/);
});

test("workspace detail exposes agent management controls", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /createWorkspaceAgent/);
  assert.match(source, /deleteWorkspaceAgent/);
  assert.match(source, /aria-label="Agent 등록"/);
  assert.match(source, /handleDeleteAgent/);
});

test("home dashboard exposes member profile settings", () => {
  const source = readSource("app/page.tsx");

  assert.match(source, /fetchMemberProfile/);
  assert.match(source, /updateMemberProfile/);
  assert.match(source, /aria-label="회원 프로필 설정"/);
  assert.match(source, /avatarColors/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
