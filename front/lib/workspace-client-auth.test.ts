import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = path.resolve(import.meta.dirname, "..");

const protectedClientFiles = [
  "lib/workspaces.ts",
  "lib/gateway.ts",
  "lib/invites.ts",
  "lib/tasks.ts",
  "lib/agents.ts",
  "lib/github.ts",
  "lib/artifacts.ts",
  "lib/members.ts",
  "lib/orchestrations.ts",
] as const;

const pageFiles = [
  "app/workspaces/page.tsx",
  "app/invites/[token]/page.tsx",
  "app/workspaces/new/page.tsx",
  "app/workspaces/gateway/page.tsx",
  "app/workspaces/[workspaceId]/page.tsx",
  "app/workspaces/[workspaceId]/settings/page.tsx",
] as const;

test("workspace API clients use accessToken option without temporary member header", () => {
  for (const filePath of protectedClientFiles) {
    const source = readSource(filePath);

    assert.doesNotMatch(source, /WORKSPACE_MEMBER_HEADER|X-Aquila-Creator-Google-Sub/);
    if (filePath !== "lib/orchestrations.ts") {
      assert.match(source, /\baccessToken\b/);
    } else {
      assert.match(source, /resolveAccessToken|accessToken/);
    }
  }
});

test("workspace pages resolve access token through auth session helper without dev token fallback", () => {
  for (const filePath of pageFiles) {
    const source = readSource(filePath);

    assert.doesNotMatch(source, /DEV_ACCESS_TOKEN|NEXT_PUBLIC_DEV_ACCESS_TOKEN/);
  }

  assert.match(readSource("lib/workspaces.ts"), /resolveAccessToken/);
  assert.match(readSource("lib/api/invites.ts"), /resolveAccessToken/);
  assert.match(readSource("lib/api/members.ts"), /resolveAccessToken/);
});

test("invite accept page renders preview for unauthenticated users and offers login returnTo", () => {
  const source = readSource("app/invites/[token]/page.tsx");
  const inviteApiSource = readSource("lib/api/invites.ts");

  assert.match(source, /getStoredUser/);
  assert.doesNotMatch(source, /router\.replace\("\/"\)/);
  assert.match(source, /loginReturnUrl/);
  assert.match(source, /\/auth\?returnTo=/);
  assert.match(source, /로그인 후 초대 수락/);
  assert.match(source, /router\.replace\("\/workspaces"\)/);
  assert.match(source, /workspaceName/);
  assert.match(source, /getInviteInfo/);
  assert.match(source, /acceptInvite/);
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
  assert.doesNotMatch(detailSource, /createWorkspaceInvite/);
  assert.doesNotMatch(detailSource, /aria-label="워크스페이스 초대 생성"/);
});

test("workspace client exposes dashboard summary and member task stats contracts", () => {
  const source = readSource("lib/workspaces.ts");

  assert.match(source, /type WorkspaceDashboardSummary/);
  assert.match(source, /dailyTokenLimit\?: number \| null/);
  assert.match(source, /todayUsedTokens\?: number/);
  assert.match(source, /waitingApprovalTaskCount\?: number/);
  assert.match(source, /avgExecutionDurationMs\?: number \| null/);
  assert.match(source, /type WorkspaceMemberTaskStats/);
  assert.match(source, /fetchWorkspaceDashboardSummary/);
  assert.match(source, /fetchWorkspaceMemberTaskStats/);
  assert.match(source, /\/dashboard\/summary/);
  assert.match(source, /\/members\/task-stats/);
});

test("workspace list exposes auth session and top chrome", () => {
  const source = readSource("app/workspaces/page.tsx");
  const chromeSource = readSource("components/desktop/AppTopChrome.tsx");

  assert.match(source, /getStoredUser/);
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(chromeSource, /logout/);
});

test("workspace list matches workspace metadata and empty states", () => {
  const source = readSource("app/workspaces/page.tsx");

  assert.match(source, /\/workspaces\/gateway/);
  assert.match(source, /\/workspaces\/new/);
  assert.match(source, /참여 워크스페이스/);
  assert.match(source, /등록된 에이전트/);
  assert.match(source, /진행 중 작업/);
  assert.match(source, /완료된 작업/);
  assert.match(source, /아직 참여 중인 워크스페이스가 없습니다/);
  assert.match(source, /새 워크스페이스/);
  assert.match(source, /워크스페이스 목록을 불러오지 못했습니다\./);
  assert.match(source, /ws\.agentCount/);
  assert.match(source, /ws\.runningTaskCount/);
});

test("new workspace form matches creation contract", () => {
  const source = readSource("app/workspaces/new/page.tsx");

  assert.match(source, /NAME_MAX = 100/);
  assert.match(source, /DESCRIPTION_MAX = 500/);
  assert.match(source, /name\.trim\(\)/);
  assert.match(source, /description\.trim\(\) \|\| undefined/);
  assert.match(source, /router\.replace\(`\/workspaces\/\$\{created\.workspaceId\}`\)/);
  assert.match(source, /워크스페이스 생성/);
  assert.match(source, /워크스페이스 생성에 실패했습니다\./);
});

test("frontend route aliases expose workspace and auth entries", () => {
  assert.match(readSource("app/page.tsx"), /MENU/);
  assert.match(readSource("app/workspaces/page.tsx"), /WorkspacesPage/);
  assert.doesNotMatch(readSource("app/workspaces/page.tsx"), /HomePage/);
  assert.match(readSource("app/auth/page.tsx"), /GoogleLogin/);
  assert.match(readSource("app/auth/page.tsx"), /safeReturnTo/);
});

test("frontend exposes character profile route with avatar color controls", () => {
  const source = readSource("app/auth/character/page.tsx");

  assert.match(source, /CharacterPage/);
  assert.match(source, /getMyMemberProfile/);
  assert.match(source, /updateMyMemberProfile/);
  assert.match(source, /safeReturnTo/);
  assert.match(source, /COLOR_OPTIONS/);
  assert.match(source, /avatarColorsToPalette/);
  assert.match(source, /ColorRow/);
});

test("frontend gateway route uses real backend gateway status, test, and binding APIs", () => {
  const source = readSource("app/workspaces/gateway/page.tsx");

  assert.match(source, /GatewayPage/);
  assert.match(source, /getStoredUser/);
  assert.match(source, /router\.replace\("\/"\)/);
  assert.match(source, /listWorkspaces/);
  assert.match(source, /fetchWorkspaceGatewayStatus/);
  assert.match(source, /testWorkspaceGatewayConnection/);
  assert.match(source, /createWorkspaceGatewayBinding/);
  assert.match(source, /normalizeGatewayForm/);
  assert.doesNotMatch(source, /aio\.openclaw\.gateways/);
  assert.doesNotMatch(source, />ON</);
});

test("workspace settings keeps connections out of settings and exposes dailyTokenLimit", () => {
  const source = readSource("app/workspaces/[workspaceId]/settings/page.tsx");

  assert.doesNotMatch(source, /fetchSlackInstallUrl/);
  assert.match(source, /SettingsSection = "workspace" \| "members" \| "invite" \| "invites"/);
  assert.match(source, /listSentInvites/);
  assert.match(source, /dailyTokenLimit/);
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

test("workspace detail wires dashboard summary, tasks lifecycle, orchestrations, and agents", () => {
  const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
  const liveSyncSource = readSource("lib/use-workspace-live-sync.ts");
  const taskBoardSource = readSource("components/workspace/views/WorkspaceTaskBoardView.tsx");
  const approvalSource = readSource("components/workspace/views/WorkspaceApprovalInboxView.tsx");
  const orchSource = readSource("components/workspace/views/WorkspaceOrchestrationView.tsx");
  const inspectorSource = readSource("components/workspace/inspector/WorkspaceContextInspector.tsx");
  const agentEditSource = readSource("components/workspace/inspector/AgentEditPanel.tsx");
  const hireSource = readSource("components/workspace/modals/AgentHireModal.tsx");

  assert.match(detailSource, /fetchWorkspaceDashboardSummary/);
  assert.match(detailSource, /fetchWorkspaceTasks/);
  assert.match(detailSource, /createWorkspaceTask/);
  assert.match(detailSource, /createAndRunWorkspaceTask/);
  assert.match(detailSource, /updateWorkspaceTaskStatus/);
  assert.match(detailSource, /runWorkspaceTask/);
  assert.match(detailSource, /cancelWorkspaceTask/);
  assert.match(detailSource, /requestTaskApproval/);
  assert.match(detailSource, /reviewTaskApproval/);
  assert.match(detailSource, /recoverStaleWorkspaceTasks/);
  assert.match(detailSource, /fetchTaskInspectorSnapshot/);
  assert.match(liveSyncSource, /fetchTaskExecutionLogs/);
  assert.match(detailSource, /fetchOrchestrationSessions/);
  assert.match(detailSource, /createOrchestrationSession/);
  assert.match(detailSource, /dispatchOrchestrationSession/);
  assert.match(detailSource, /fetchWorkspaceAgents/);
  assert.match(detailSource, /createWorkspaceAgent/);
  assert.match(detailSource, /updateWorkspaceAgent/);
  assert.match(detailSource, /deleteWorkspaceAgent/);

  assert.match(taskBoardSource, /WorkspaceTaskBoardView/);
  assert.match(approvalSource, /WorkspaceApprovalInboxView/);
  assert.match(orchSource, /WorkspaceOrchestrationView/);
  assert.match(inspectorSource, /WorkspaceContextInspector/);
  assert.match(agentEditSource, /AgentEditPanel/);
  assert.match(hireSource, /aria-label="에이전트 추가"/);
});

test("workspace detail exposes task chat, presence, orchestration plan, and markdown rendering", () => {
  const detailSource = readSource("app/workspaces/[workspaceId]/page.tsx");
  const modalSource = readSource("components/workspace/modals/TaskDetailModals.tsx");
  const artifactsViewSource = readSource("components/workspace/views/WorkspaceArtifactsAndDiffView.tsx");
  const artifactModalSource = readSource("components/workspace/modals/ArtifactFileModal.tsx");
  const mdSource = readSource("components/ui/MarkdownContent.tsx");

  assert.match(detailSource, /fetchTaskChatMessages/);
  assert.match(detailSource, /fetchChatSessionMessages/);
  assert.match(detailSource, /sendWorkspaceChatMessage/);
  assert.match(modalSource, /다음 메시지 더 보기/);
  assert.match(detailSource, /createWorkspacePresenceStream/);
  assert.match(detailSource, /updateWorkspacePresencePosition/);
  assert.match(detailSource, /fetchOrchestrationPlanArtifact/);
  assert.match(mdSource, /ReactMarkdown/);
  assert.match(mdSource, /remarkGfm/);
  assert.match(artifactsViewSource, /MarkdownContent/);
  assert.match(artifactModalSource, /MarkdownContent/);
  assert.match(artifactModalSource, /isMarkdownArtifactFile/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
