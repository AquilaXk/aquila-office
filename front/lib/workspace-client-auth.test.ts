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
  "app/workspaces/page.tsx",
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
  }

  assert.match(readSource("lib/workspaces.ts"), /resolveAccessToken/);
  assert.match(readSource("lib/api/invites.ts"), /resolveAccessToken/);
  assert.match(readSource("lib/api/members.ts"), /resolveAccessToken/);
});

test("invite accept page redirects unauthenticated users and returns to workspace list on success", () => {
  const source = readSource("app/invites/[token]/page.tsx");
  const inviteApiSource = readSource("lib/api/invites.ts");

  assert.match(source, /getStoredUser/);
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(source, /router\.replace\("\/workspaces"\)/);
  assert.match(source, /workspaceName/);
  assert.match(source, /getInviteInfo/);
  assert.match(source, /acceptInvite/);
  assert.doesNotMatch(source, /invite\.email/);
  assert.match(source, /blockedReason/);
  assert.match(inviteApiSource, /workspaceName\?: string/);
  assert.match(inviteApiSource, /expired\?: boolean/);
  assert.match(inviteApiSource, /"REVOKED"/);
});

test("workspace invite creation lives in workspace settings", () => {
  const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
  const settingsSource = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.match(settingsSource, /createInviteLink/);
  assert.match(settingsSource, /const targetEmail = inviteEmail\.trim\(\)/);
  assert.match(settingsSource, /\{ targetEmail \}/);
  assert.match(settingsSource, /listSentInvites/);
  assert.match(detailSource, /router\.push\(`\/workspaces\/\$\{numericWorkspaceId\}\/settings`\)/);
  assert.doesNotMatch(detailSource, /createWorkspaceInvite/);
  assert.doesNotMatch(detailSource, /aria-label="워크스페이스 초대 생성"/);
  assert.doesNotMatch(settingsSource, /\{ email \}/);
  assert.doesNotMatch(settingsSource, /inviteState\.invite\.email/);
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

test("workspace list exposes auth token session actions", () => {
  const source = readSource("app/workspaces/page.tsx");

  assert.match(source, /getStoredUser/);
  assert.match(source, /logout/);
  assert.match(source, /router\.replace\("\/"\)/);
});

test("workspace list matches workspace metadata and empty states", () => {
  const source = readSource("app/workspaces/page.tsx");

  assert.match(source, /GATEWAY/);
  assert.match(source, /NEW WORKSPACE/);
  assert.match(source, /LOGOUT/);
  assert.match(source, /WORKPLACES JOINED/);
  assert.match(source, /TASKS · ACTIVE/);
  assert.match(source, /AGENTS/);
  assert.match(source, /TASKS · DONE/);
  assert.match(source, /진행 중인 태스크/);
  assert.match(source, /등록된 에이전트/);
  assert.match(source, /ENTRIES/);
  assert.match(source, /SCROLL ↓/);
  assert.match(source, /workspaces\.length > 8/);
  assert.match(source, /아직 참여 중인 워크스페이스가 없습니다/);
  assert.match(source, /CREATE WORKSPACE/);
  assert.match(source, /워크스페이스를 불러오지 못했습니다\./);
  assert.match(source, /workspace\.description \|\| "No description"/);
  assert.match(source, /workspace\.agentCount/);
  assert.match(source, /workspace\.runningTaskCount/);
  assert.match(source, /ENTER/);
  assert.doesNotMatch(source, /formatDate/);
  assert.doesNotMatch(source, /workspace\.joinedAt/);
});

test("new workspace form matches creation contract without session side panel", () => {
  const source = readSource("app/workspaces/new/page.tsx");

  assert.match(source, /WORKSPACE DETAILS/);
  assert.match(source, /NAME_MAX = 100/);
  assert.match(source, /DESCRIPTION_MAX = 500/);
  assert.match(source, /placeholder="예: 백엔드 엔지니어링 팀"/);
  assert.match(source, /placeholder="워크스페이스의 미션, 진행 중인 작업 등을 설명해 주세요\."/);
  assert.match(source, /router\.replace\(`\/workspaces\/\$\{created\.workspaceId\}`\)/);
  assert.match(source, /CREATE WORKSPACE/);
  assert.match(source, /워크스페이스 생성에 실패했습니다\./);
  assert.doesNotMatch(source, /workspace-create-side/);
  assert.doesNotMatch(source, /SESSION READY/);
  assert.doesNotMatch(source, />access token</);
});

test("frontend route aliases expose workspace and auth entries", () => {
  assert.match(readSource("app/page.tsx"), /MENU/);
  assert.match(readSource("app/workspaces/page.tsx"), /WorkspacesPage/);
  assert.doesNotMatch(readSource("app/workspaces/page.tsx"), /HomePage/);
  assert.match(readSource("app/auth/page.tsx"), /GoogleLogin/);
  assert.match(readSource("app/auth/page.tsx"), /safeReturnTo/);
});

test("frontend exposes character profile route", () => {
  const source = readSource("app/auth/character/page.tsx");

  assert.match(source, /CharacterPage/);
  assert.match(source, /getMyMemberProfile/);
  assert.match(source, /updateMyMemberProfile/);
  assert.match(source, /safeReturnTo/);
});

test("character profile route exposes avatar color controls", () => {
  const source = readSource("app/auth/character/page.tsx");

  assert.match(source, /COLOR_OPTIONS/);
  assert.match(source, /avatarColorsToPalette/);
  assert.match(source, /ColorPicker/);
});

test("frontend exposes workspace gateway route status surface", () => {
  const source = readSource("app/workspaces/gateway/page.tsx");

  assert.match(source, /GatewayPage/);
  assert.match(source, /getStoredUser/);
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(source, /STORAGE_KEY = "aio\.openclaw\.gateways"/);
  assert.match(source, /readGateways/);
  assert.match(source, /writeGateways/);
  assert.match(source, /T4Screen title="GATEWAY"/);
});

test("frontend exposes workspace gateway route binding controls", () => {
  const source = readSource("app/workspaces/gateway/page.tsx");

  assert.match(source, /isHttpUrl/);
  assert.match(source, /DISPLAY NAME/);
  assert.match(source, /GATEWAY URL/);
  assert.match(source, /TOKEN/);
  assert.match(source, /LINK GATE/);
  assert.match(source, /CANCEL/);
});

test("frontend exposes workspace gateway registration UX contract", () => {
  const source = readSource("app/workspaces/gateway/page.tsx");

  assert.match(source, /REGISTER A GATEWAY/);
  assert.match(source, /LINKED ·/);
  assert.match(source, /ACTIVE LINK/);
  assert.match(source, /selectedGateway\.maskedToken/);
  assert.match(source, /const nextGateways = \[nextGateway, \.\.\.gateways\]/);
  assert.match(source, /setGateways\(nextGateways\)/);
  assert.match(source, /setSelectedId\(nextGateway\.id\)/);
  assert.doesNotMatch(source, /fetchWorkspaces/);
  assert.doesNotMatch(source, /createWorkspaceGatewayBinding/);
});

test("workspace settings exposes slack integration controls", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.doesNotMatch(source, /fetchSlackInstallUrl/);
  assert.doesNotMatch(source, /aria-label="Slack 연동 설정"/);
  assert.match(source, /SettingsSection = "workspace" \| "members" \| "invite" \| "invites"/);
});

test("workspace settings exposes slack channel mapping controls", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.doesNotMatch(source, /fetchSlackChannelMappings/);
  assert.doesNotMatch(source, /slackChannelMappings/);
  assert.match(source, /listSentInvites/);
});

test("frontend exposes slack integration result routes", () => {
  const successSource = readSource("app/settings/integrations/success/page.tsx");
  const failSource = readSource("app/settings/integrations/fail/page.tsx");

  assert.match(successSource, /SlackIntegrationSuccessPage/);
  assert.match(successSource, /readSlackReturnTo/);
  assert.match(successSource, /readPendingSlackWorkspaceId/);
  assert.match(successSource, /fetchSlackIntegrations/);
  assert.match(successSource, /writeStoredSlackIntegration/);
  assert.match(successSource, /setTimeout\(\(\) => router\.push\(nextReturnTo\), 2500\)/);
  assert.match(successSource, /연동 목록으로 이동/);
  assert.match(failSource, /SlackIntegrationFailPage/);
  assert.match(failSource, /readSlackReturnTo/);
  assert.match(failSource, /재시도/);
});

test("workspace detail opens dashboard data through the task modal", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /fetchWorkspaceDashboardSummary/);
  assert.match(source, /<TaskDashboardModal/);
  assert.match(source, /summary=\{taskDashboardSummary\}/);
  assert.match(source, /apiSummary=\{summary\}/);
  assert.match(source, /localSummary=\{localDashboardSummary\}/);
  assert.doesNotMatch(source, /aria-label="워크스페이스 대시보드 요약"/);
  assert.doesNotMatch(source, /fetchWorkspaceMemberTaskStats/);
});

test("workspace detail enters task work through the dashboard modal", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");
  const clientSource = readSource("lib/tasks.ts");

  assert.match(clientSource, /createAndRunWorkspaceTask/);
  assert.match(clientSource, /workspaceTasksRunPath/);
  assert.match(source, /fetchWorkspaceTasks/);
  assert.match(source, /TaskDashboardModal/);
  assert.match(source, /setTaskDashboardOpen\(true\)/);
  assert.match(source, /setTaskDashboardTab\("board"\)/);
  assert.match(source, /openTaskChat/);
  assert.match(source, /openTaskReports/);
  assert.match(source, /createPr/);
  assert.doesNotMatch(source, /createWorkspaceTask/);
  assert.doesNotMatch(source, /updateWorkspaceTaskStatus/);
  assert.doesNotMatch(source, /runWorkspaceTask/);
  assert.doesNotMatch(source, /aria-label="Task 생성"/);
  assert.doesNotMatch(source, /aria-label="워크스페이스 Task"/);
});

test("workspace detail exposes task chat surface", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /fetchTaskChatMessages/);
  assert.match(source, /fetchChatSessionMessages/);
  assert.match(source, /chatSessionMessagePageInput/);
  assert.match(source, /sendWorkspaceChatMessage/);
  assert.match(source, /aria-label="Task chat 메시지"/);
  assert.match(source, /openTaskChat/);
  assert.match(source, /taskChatCursor/);
  assert.match(source, /onLoadMoreMessages/);
  assert.match(source, /다음 메시지 더 보기/);
});

test("workspace detail exposes presence surface", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /createWorkspacePresenceStream/);
  assert.match(source, /updateWorkspacePresencePosition/);
  assert.match(source, /aria-label="Workspace presence"/);
  assert.match(source, /presenceEvents/);
});

test("workspace detail exposes orchestration plan artifact surface", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /fetchOrchestrationPlanArtifact/);
  assert.match(source, /aria-label="Orchestration plan artifact"/);
  assert.match(source, /orchestrationPlan/);
});

test("frontend exposes markdown artifact renderer contract", () => {
  const source = readSource("components/ui/MarkdownContent.tsx");

  assert.match(source, /ReactMarkdown/);
  assert.match(source, /remarkGfm/);
  assert.match(source, /copyMarkdownCodeBlock/);
  assert.match(source, /markdown-artifact-content/);
});

test("workspace detail renders markdown artifact files", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /MarkdownContent/);
  assert.match(source, /isMarkdownArtifactFile/);
  assert.match(source, /workspace-file-markdown/);
});

test("workspace detail opens agent management through the hire modal", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /fetchWorkspaceAgents/);
  assert.match(source, /type WorkspaceAgent/);
  assert.match(source, /workspaceAgents/);
  assert.match(source, /AgentHireModal/);
  assert.match(source, /item\.id === "agents"/);
  assert.match(source, /setAgentHireOpen\(true\)/);
  assert.doesNotMatch(source, /aria-label="워크스페이스 Agent"/);
});

test("workspace detail keeps agent creation in AgentHireModal", () => {
  const source = readSource("app/workspaces/[workspaceId]/page.tsx");

  assert.match(source, /createWorkspaceAgent/);
  assert.match(source, /aria-label="Agent hire"/);
  assert.match(source, /normalizeAgentSkillFiles/);
  assert.match(source, /form\.skillFiles\.map/);
  assert.doesNotMatch(source, /deleteWorkspaceAgent/);
  assert.doesNotMatch(source, /aria-label="Agent 등록"/);
  assert.doesNotMatch(source, /handleDeleteAgent/);
});

test("character profile screen exposes member profile settings", () => {
  const source = readSource("app/auth/character/page.tsx");

  assert.match(source, /getMyMemberProfile/);
  assert.match(source, /updateMyMemberProfile/);
  assert.match(source, /SET UP YOUR OFFICE PROFILE/);
  assert.match(source, /avatarColorsToPalette/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
