import { expect, test, type Locator, type Page, type Route } from "@playwright/test";

const workspaceId = 101;
const taskId = 301;
const agentId = 501;
const inviteToken = "team-token";

type WorkspaceListFixture = "default" | "empty" | "error" | "many";
type WorkspaceMemberFixture = "default" | "current-user-second";

type MockApiOptions = {
  workspaceList?: WorkspaceListFixture;
  workspaceListGate?: Promise<void>;
  workspaceMembers?: WorkspaceMemberFixture;
  artifactFailureMode?: boolean;
  workspaceRole?: "ADMIN" | "MEMBER";
};

type AuthTokenFixture = {
  memberId: number;
  name: string;
  email: string;
  role: "ADMIN" | "MEMBER";
  accessToken: string;
};

const authTokens: AuthTokenFixture = {
  memberId: 1,
  name: "Aquila User",
  email: "aquila@example.com",
  role: "ADMIN",
  accessToken: "e2e-access-token",
};

const primaryWorkspace = {
  workspaceId,
  name: "Product Engineering",
  description: "서비스 운영 자동화",
  creatorGoogleSub: "google-1",
  myRole: "ADMIN",
  agentCount: 1,
  runningTaskCount: 1,
  completedTaskCount: 0,
  createdAt: "2026-05-18T08:50:00Z",
  joinedAt: "2026-05-18T09:00:00Z",
} as const;

const workspaceMember = {
  memberId: 1,
  memberGoogleSub: "google-1",
  email: "aquila@example.com",
  name: "Aquila User",
  role: "ADMIN",
  joinedAt: "2026-05-18T09:00:00Z",
} as const;

const firstListedWorkspaceMember = {
  memberId: 1,
  memberGoogleSub: "google-fallback",
  email: "first@example.com",
  name: "First Member",
  role: "MEMBER",
  joinedAt: "2026-05-18T08:59:00Z",
} as const;

const secondListedCurrentWorkspaceMember = {
  ...workspaceMember,
  memberId: 2,
  memberGoogleSub: "google-2",
} as const;

const readyAgent = {
  agentId,
  workspaceId,
  name: "QA Agent",
  category: "QA",
  openClawAgentId: "agent-qa",
  workspacePath: "/workspace/product",
  status: "READY",
  syncError: null,
  createdByGoogleSub: "google-1",
  createdAt: "2026-05-18T09:05:00Z",
  updatedAt: "2026-05-18T09:05:00Z",
  skillFiles: [],
} as const;

const regressionTask = {
  taskId,
  workspaceId,
  createdByGoogleSub: "google-1",
  title: "Regression smoke",
  description: "브라우저 회귀 흐름 점검",
  type: "TEST_CREATION",
  priority: "HIGH",
  assignedAgentId: agentId,
  status: "IN_PROGRESS",
  createdAt: "2026-05-18T09:10:00Z",
  updatedAt: "2026-05-18T09:20:00Z",
} as const;

test.describe("브라우저 UI UX 회귀", () => {
  test("워크스페이스 목록의 loading, empty 상태를 표시한다", async ({ page }) => {
    const gate = deferred();
    await installMockApi(page, { workspaceList: "empty", workspaceListGate: gate.promise });
    await seedAuthTokens(page);

    await page.goto("/workspaces");

    await expect(page.getByText("00 ENTRIES")).toBeVisible();
    await expect(page.getByText("no workspaces yet — create one to start collaborating")).toHaveCount(0);
    gate.resolve();
    await expect(page.getByText("no workspaces yet — create one to start collaborating")).toBeVisible();
    await expect(page.getByRole("link", { name: "CREATE WORKSPACE" })).toHaveAttribute(
      "href",
      "/workspaces/new",
    );
  });

  test("워크스페이스 목록 9개 이상 상태에서 scroll indicator를 표시한다", async ({ page }) => {
    await installMockApi(page, { workspaceList: "many" });
    await seedAuthTokens(page);

    await page.goto("/workspaces");

    await expect(page.getByText("09 ENTRIES")).toBeVisible();
    await expect(page.getByText("SCROLL ↓")).toBeVisible();
    await expect(page.getByText("W009")).toBeVisible();
    await expect(page.getByText("2026")).toHaveCount(0);
  });

  test("모바일 워크스페이스 목록은 핵심 action과 row 정보를 viewport 안에 표시한다", async ({ page }) => {
    test.skip((page.viewportSize()?.width ?? 0) > 600, "mobile viewport 전용 회귀");

    await installMockApi(page);
    await seedAuthTokens(page);

    await page.goto("/workspaces");
    await expect(page.getByRole("heading", { name: "WORKSPACES" })).toBeVisible();

    await expectElementBoxInsideViewport(
      page,
      page.getByRole("link", { name: /NEW WORKSPACE/ }),
      "new workspace action",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByRole("button", { name: "LOGOUT" }),
      "logout action",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByText("AGENTS").first(),
      "agents KPI",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByText("TASKS · DONE").first(),
      "done KPI",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByText("ENTER").first(),
      "workspace enter action",
    );
  });

  test("워크스페이스 목록 오류 상태를 표시한다", async ({ page }) => {
    await installMockApi(page, { workspaceList: "error" });
    await seedAuthTokens(page);

    await page.goto("/workspaces");

    await expect(page.getByText("워크스페이스 fixture 오류").first()).toBeVisible();
  });

  test("인증 없는 워크스페이스 생성 접근은 title screen으로 되돌린다", async ({ page }) => {
    await installMockApi(page);

    await page.goto("/workspaces/new");

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: "AQUILA OFFICE" })).toBeVisible();
  });

  test("인증 없는 Gateway registry 접근은 title screen으로 되돌린다", async ({ page }) => {
    await installMockApi(page);

    await page.goto("/workspaces/gateway");

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: "AQUILA OFFICE" })).toBeVisible();
  });

  test("인증 없는 settings 접근은 title screen으로 되돌린다", async ({ page }) => {
    await installMockApi(page);

    await page.goto(`/workspaces/${workspaceId}/settings`);

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: "AQUILA OFFICE" })).toBeVisible();
  });

  test("인증 없는 invite 접근은 title screen으로 되돌린다", async ({ page }) => {
    await installMockApi(page);

    await page.goto(`/invites/${inviteToken}`);

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: "AQUILA OFFICE" })).toBeVisible();
  });

  test("title screen은 미인증 CONTINUE를 title에 머물게 하고 LOGIN으로 인증 화면에 진입한다", async ({ page }) => {
    await installMockApi(page);

    await page.goto("/");
    await page.getByRole("menuitem", { name: /CONTINUE/ }).click();

    await expect(page).toHaveURL((url) => url.pathname === "/");
    await expect(page.getByRole("status")).toHaveCount(0);

    await page.getByRole("menuitem", { name: /LOGIN/ }).click();
    await page.waitForURL((url) => url.pathname === "/auth");
    await expect(page.getByText("SIGN IN TO CONTINUE")).toBeVisible();
  });

  test("title screen에서 워크스페이스 목록으로 이동하고 워크스페이스를 생성한다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);

    await page.goto("/");
    await expect(page.getByRole("menuitem", { name: /CONTINUE/ })).toBeVisible();
    await page.keyboard.press("ArrowDown");
    await page.keyboard.press("Enter");
    await page.waitForURL((url) => url.pathname === "/workspaces");
    await expect(page.getByText("W001")).toBeVisible();
    await expect(page.getByText("1 agents")).toBeVisible();
    await expect(page.getByText("1 active")).toBeVisible();
    await expect(page.getByText("ENTER")).toBeVisible();

    await page.getByRole("link", { name: /NEW WORKSPACE/ }).click();
    await expect(page.getByText("CREATE A NEW WORKSPACE")).toBeVisible();
    await expect(page.getByText("0/100")).toBeVisible();
    await expect(page.getByText("0/500")).toBeVisible();
    await expect(page.getByRole("link", { name: "CANCEL" })).toHaveAttribute("href", "/workspaces");

    await page.getByLabel(/WORKSPACE NAME/).fill(" Platform Ops ");
    await page.getByLabel(/DESCRIPTION/).fill(" 운영 자동화 점검 ");
    const createRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === "POST" && url.pathname === "/api/v1/workspaces";
    });
    await page.getByRole("button", { name: /CREATE WORKSPACE/ }).click();
    const createPayload = JSON.parse((await createRequest).postData() ?? "{}");
    expect(createPayload).toEqual({
      name: "Platform Ops",
      description: "운영 자동화 점검",
    });
    await page.waitForURL((url) => url.pathname === `/workspaces/${workspaceId}`);
  });

  test("타이틀과 인증 화면은 각 route shell을 유지한다", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "AQUILA OFFICE" })).toBeVisible();

    await page.goto("/auth");
    await expect(page.getByText("SIGN IN TO CONTINUE")).toBeVisible();
    await expect(page.getByText("WORKSPACE MEMBER VERIFICATION")).toBeVisible();
  });

  test("인증 라우트가 Google 로그인과 인증 후 returnTo 이동을 유지한다", async ({ page }) => {
    await installMockApi(page);

    await page.goto("/auth?returnTo=/workspaces/new");
    await expect(page.getByText("SIGN IN TO CONTINUE")).toBeVisible();
    await expect(page.getByText("WORKSPACE MEMBER VERIFICATION")).toBeVisible();
    await expect(page.getByText("PRESS TO CONTINUE")).toBeVisible();

    await seedAuthTokens(page);
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem("aquila-office.auth.accessToken")))
      .toContain("e2e-access-token");
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem("aquila-office.auth.user")))
      .toContain("Aquila User");
    await Promise.all([
      page.waitForURL((url) => url.pathname === "/auth/character" && url.searchParams.get("returnTo") === "/workspaces/new"),
      page.reload(),
    ]);
    await expect(page.getByText("SET UP YOUR OFFICE PROFILE")).toBeVisible();
  });

  test("캐릭터 프로필 설정에서 avatar와 색상 저장 흐름을 검증한다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);
    await seedCharacterProfile(page);

    await page.goto("/auth/character?returnTo=/workspaces");
    await expect(page.getByText("SET UP YOUR OFFICE PROFILE")).toBeVisible();
    await expect(page.getByPlaceholder("type your name…")).toHaveValue("Aquila User");

    await page.getByRole("button", { name: /KENJI/ }).click();
    await page.getByRole("button", { name: "CUSTOM" }).click();
    await page.getByLabel("OUTFIT 02").click();
    await page.getByPlaceholder("type your name…").fill("Aquila Pilot");
    await expect(page.getByPlaceholder("type your name…")).toHaveValue("Aquila Pilot");
    const profileUpdateRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === "PATCH" && url.pathname === "/api/v1/members/me/profile";
    });
    await page.getByRole("button", { name: /ENTER OFFICE/ }).click();
    const profileUpdateBody = (await profileUpdateRequest).postData() ?? "";
    expect(profileUpdateBody).toContain("Aquila Pilot");
    expect(profileUpdateBody).toContain("kenji");

    await page.waitForURL((url) => url.pathname === "/workspaces");
    const storedCharacter = await page.evaluate(() => window.localStorage.getItem("ai-office.character"));
    expect(storedCharacter).toContain("Aquila Pilot");
    expect(storedCharacter).toContain("kenji");
    expect(storedCharacter).toContain("#3a5a3a");
  });

  test("워크스페이스 목록 logout은 세션을 지우고 title screen으로 돌아간다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);

    await page.goto("/workspaces");
    await expect(page.getByRole("heading", { name: "WORKSPACES" })).toBeVisible();

    const logoutRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === "POST" && url.pathname === "/api/v1/auth/logout";
    });
    await page.getByRole("button", { name: "LOGOUT" }).click();
    await logoutRequest;

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: "AQUILA OFFICE" })).toBeVisible();
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem("aquila-office.auth.accessToken")))
      .toBeNull();
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem("aquila-office.auth.user")))
      .toBeNull();
  });

  test("워크스페이스 상세, Task 모달, artifact, 2D canvas 상호작용을 검증한다", async ({ page }) => {
    await installMockApi(page);
    await page.addInitScript(() => {
      window.open = () => null;
    });

    await page.goto(`/workspaces/${workspaceId}`);

    await expect(page.getByRole("heading", { name: primaryWorkspace.name })).toBeVisible();
    const officeScene = page.locator('section[aria-label="워크스페이스 오피스 씬"]');
    await expect(officeScene).toBeVisible();
    await expect(officeScene.getByRole("button", { name: "Connections", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("button", { name: "Tasks", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("button", { name: "Hire agent", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("button", { name: "Files", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("link", { name: "Leave workspace", exact: true })).toBeVisible();
    await officeScene.getByRole("button", { name: "Hire agent", exact: true }).click();
    const hireDialog = page.getByRole("dialog", { name: "Agent hire" });
    await expect(hireDialog).toBeVisible();
    await expect(hireDialog.getByLabel("Agent 이름")).toBeVisible();
    await expect(hireDialog.getByLabel("역할")).toBeVisible();
    await expect(hireDialog.getByLabel(/Workspace Path/)).toBeVisible();
    await expect(hireDialog.getByLabel(/Emoji/)).toBeVisible();
    await hireDialog.getByRole("button", { name: "Skill 파일 추가" }).click();
    await expect(hireDialog.getByPlaceholder("backend-agent.md")).toBeVisible();
    await expect(hireDialog.getByRole("button", { name: "고용" })).toBeVisible();
    await closeTopDialog(page);

    const detailNav = page.getByRole("navigation", { name: "워크스페이스 내비게이션" });
    await expect(detailNav.getByRole("link", { name: "목록으로" })).toHaveAttribute("href", "/workspaces");
    await expect(detailNav.getByRole("link", { name: "설정" })).toHaveAttribute(
      "href",
      `/workspaces/${workspaceId}/settings`,
    );
    await detailNav.getByRole("button", { name: "Connection settings" }).click();
    const connectionDialog = page.getByRole("dialog", { name: "Connection settings" });
    await expect(connectionDialog).toBeVisible();
    await connectionDialog.getByRole("button", { name: "Discord", exact: true }).click();
    await expect(connectionDialog.getByText("Discord 준비 중")).toBeVisible();
    await connectionDialog.getByRole("button", { name: "뒤로" }).click();
    await connectionDialog.getByRole("button", { name: "Slack", exact: true }).click();
    await expect(connectionDialog.getByRole("button", { name: "Slack 설치 열기" })).toBeVisible();
    await connectionDialog.getByRole("button", { name: "Slack 설치 열기" }).click();
    await expect
      .poll(() => page.evaluate(() => localStorage.getItem("aio.slack.pendingWorkspaceId")))
      .toBe(String(workspaceId));
    await connectionDialog.getByRole("button", { name: "뒤로" }).click();
    await connectionDialog.getByRole("button", { name: "GitHub", exact: true }).click();
    await expect(connectionDialog.getByLabel("selected credential")).toBeVisible();
    await connectionDialog.locator(".workspace-connection-form").getByLabel("표시 이름").fill("Deploy token");
    await connectionDialog.locator(".workspace-connection-form").getByLabel("Token").fill("ghp_new_secret");
    await connectionDialog.getByRole("button", { name: "등록", exact: true }).click();
    await expect(
      connectionDialog.locator(".workspace-connection-select").filter({ hasText: "Deploy token" }),
    ).toBeVisible();
    await expect
      .poll(() =>
        page.evaluate((id) => window.localStorage.getItem(`aio.workspace.${id}.integrations.github`), workspaceId),
      )
      .toContain("Deploy token");
    await connectionDialog.getByRole("button", { name: "뒤로" }).click();
    await connectionDialog.getByRole("button", { name: "AI / OpenClaw", exact: true }).click();
    await connectionDialog.getByLabel("Gateway URL").fill("https://gateway.example.test");
    await connectionDialog.getByLabel("Token").fill("gateway-secret");
    await connectionDialog.getByRole("button", { name: "연결 테스트" }).click();
    await expect(connectionDialog.getByText("연결 가능").first()).toBeVisible();
    await connectionDialog.getByRole("button", { name: "OpenClaw binding 등록" }).click();
    await expect(connectionDialog.getByText("OpenClaw Gateway 정보를 저장했습니다.")).toBeVisible();
    await expect
      .poll(() =>
        page.evaluate((id) => window.localStorage.getItem(`aio.workspace.${id}.integrations.openclaw`), workspaceId),
      )
      .toContain("https://gateway.example.test");
    await closeTopDialog(page);

    const canvasFrame = page.getByRole("img", { name: "Agent and member 2D workspace scene" });
    await expect(canvasFrame).toBeVisible();
    await expect(officeScene).toHaveClass(/workspace-stage-surface/);
    const stageShell = page.locator(".workspace-2d-stage-shell").first();
    await expect(stageShell).toBeVisible();
    await expectStageShellToFillFirstScreen(page, stageShell);
    await expectElementToTouchViewport(page, page.locator(".stage-room-engineering").first(), "engineering room");
    if ((page.viewportSize()?.width ?? 0) >= 900) {
      await expectElementToTouchViewport(page, page.locator(".stage-desk").first(), "engineering desk");
    }
    await expectElementToTouchViewport(page, page.locator(".stage-corridor").first(), "stage corridor");
    await expect(page.locator(".workspace-2d-hud")).toBeVisible();
    await expect(page.locator(".workspace-2d-minimap")).toBeVisible();
    await expect(page.locator(".workspace-2d-dialogue")).toBeVisible();
    await expect(page.locator(".stage-room")).toHaveCount(4);
    await expect(page.locator(".stage-quest-marker")).toBeVisible();
    await expect(page.locator(".workspace-2d-map-hud")).toBeVisible();
    const actorLayer = page.locator(".workspace-2d-actor-layer");
    await expect(actorLayer.getByRole("button", { name: /Aquila User 플레이어/ })).toBeVisible();
    await expect(actorLayer.getByRole("button", { name: /QA Agent 채팅/ })).toBeVisible();
    await expect(actorLayer.locator(".stage-actor__player-marker")).toBeVisible();
    const playerHudValue = page.locator(".workspace-2d-hud div").filter({ hasText: "Player" }).locator("strong");
    await expect(playerHudValue).not.toHaveText("No signal");
    const stageFrame = page.locator(".workspace-2d-stage").first();
    await expectKeyboardToMovePlayer(page, canvasFrame, playerHudValue);
    await expectPanAndZoomToMoveViewport(page, stageFrame);
    await openAndAssert2DContextMenu(page, stageFrame);

    const actorPanel = page.locator('aside[aria-label="Agent 대화와 Task 대시보드"]');
    await expect(actorPanel).toBeVisible();
    await actorPanel.getByLabel("Agent 메시지").fill("Agent 상태 요약");
    await actorPanel.getByRole("button", { name: "Agent 메시지 전송" }).click();
    await expect(actorPanel.getByText("브라우저 회귀 확인을 진행합니다.")).toBeVisible();
    await actorPanel.getByRole("tab", { name: "Tasks" }).click();
    await expect(actorPanel.getByText(regressionTask.title)).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(actorPanel).toHaveCount(0);

    await officeScene.locator(".workspace-office-nav").getByRole("button", { name: "Tasks", exact: true }).click();
    const taskDashboard = page.getByRole("dialog", { name: "Task dashboard" });
    await expect(taskDashboard).toBeVisible();
    await expect(taskDashboard.getByText("API summary")).toBeVisible();
    await expect(taskDashboard.getByText("Local fallback")).toBeVisible();
    await taskDashboard.getByRole("tab", { name: "Board" }).click();
    await taskDashboard.getByRole("button", { name: /Regression smoke/ }).click();
    await expect(taskDashboard.getByText(`Task #${taskId}`)).toBeVisible();

    const taskDashboardDetail = taskDashboard.locator('section[aria-label="selectedDashboardTask detail"]');
    await taskDashboardDetail.getByRole("button", { name: "Report" }).click();
    await expect(page.getByRole("dialog", { name: "Task report" })).toBeVisible();
    await expect(page.getByText("Report #701")).toBeVisible();
    await page.getByRole("button", { name: "result.md" }).click();
    await expect(page.getByRole("dialog", { name: "Artifact file" })).toBeVisible();
    await expect(page.getByText("검증 결과")).toBeVisible();
    await closeTopDialog(page);
    await closeTopDialog(page);

    await taskDashboardDetail.getByRole("button", { name: "Chat" }).click();
    await expect(page.getByRole("dialog", { name: "Task chat 메시지" })).toBeVisible();
    await expect(page.getByText("초기 점검을 시작합니다.")).toBeVisible();
    await page.getByLabel("Message").fill("브라우저 회귀 확인");
    await page.getByRole("button", { name: "메시지 전송" }).click();
    await expect(page.getByText(`Task #${taskId} chat 메시지가 전송되었습니다.`)).toBeVisible();
    await closeTopDialog(page);
    await closeTopDialog(page);
  });

  test("모바일 워크스페이스 상세 stage overlay는 viewport 안에서 세로 간격을 유지한다", async ({ page }) => {
    test.skip((page.viewportSize()?.width ?? 0) > 600, "mobile viewport 전용 회귀");

    await installMockApi(page);
    await seedAuthTokens(page);
    await seedCharacterProfile(page);

    await page.goto(`/workspaces/${workspaceId}`);
    await expect(page.getByRole("heading", { name: primaryWorkspace.name })).toBeVisible();

    const stageFrame = page.locator(".workspace-2d-frame-fullscreen").first();
    const nav = page.locator(".workspace-stage-route .workspace-office-nav").first();
    const controls = page.locator(".workspace-2d-frame-fullscreen .workspace-2d-header").first();
    const hud = page.locator(".workspace-2d-frame-fullscreen .workspace-2d-hud").first();
    const mapHud = page.locator(".workspace-2d-frame-fullscreen .workspace-2d-map-hud").first();
    const mapControls = page.locator(".workspace-2d-frame-fullscreen .workspace-2d-map-controls").first();
    const roster = page.locator(".workspace-2d-frame-fullscreen .workspace-2d-roster").first();
    const dialogue = page
      .locator(".workspace-2d-frame-fullscreen .workspace-2d-stage-shell .workspace-2d-dialogue")
      .first();

    for (const [label, locator] of [
      ["stage frame", stageFrame],
      ["office nav", nav],
      ["stage controls", controls],
      ["scene HUD", hud],
      ["map HUD", mapHud],
      ["map controls", mapControls],
      ["roster", roster],
      ["dialogue", dialogue],
    ] as const) {
      await expectElementBoxInsideViewport(page, locator, label);
    }

    await expectVerticalGapBetween(nav, controls, "office nav", "stage controls", 8);
    await expectVerticalGapBetween(controls, hud, "stage controls", "scene HUD", 8);
    await expectVerticalGapBetween(hud, mapHud, "scene HUD", "map HUD", 8);
    await expectVerticalGapBetween(mapHud, mapControls, "map HUD", "map controls", 8);
    await expectVerticalGapBetween(mapControls, roster, "map controls", "roster", 8);
    await expectVerticalGapBetween(roster, dialogue, "roster", "dialogue", 8);
  });

  test("데스크톱 워크스페이스 상세 stage overlay는 주요 scene 콘텐츠를 가리지 않는다", async ({ page }) => {
    test.skip((page.viewportSize()?.width ?? 0) < 900, "desktop viewport 전용 회귀");

    await installMockApi(page);
    await seedAuthTokens(page);
    await seedCharacterProfile(page);

    await page.goto(`/workspaces/${workspaceId}`);
    await expect(page.getByRole("heading", { name: primaryWorkspace.name })).toBeVisible();

    const workspaceTitle = page.locator(".workspace-stage-route .workspace-detail-title").first();
    const mapHud = page.locator(".workspace-2d-frame-fullscreen .workspace-2d-map-hud").first();
    const mapControls = page.locator(".workspace-2d-frame-fullscreen .workspace-2d-map-controls").first();
    const protectedTargets = [
      { label: "room labels", locator: page.locator(".stage-room-label") },
      { label: "stage actors", locator: page.locator(".stage-actor") },
      { label: "task marker", locator: page.locator(".stage-quest-marker") },
    ];
    const overlays = [
      { label: "workspace nav", locator: page.locator(".workspace-stage-route .workspace-office-nav").first() },
      { label: "scene HUD", locator: page.locator(".workspace-2d-frame-fullscreen .workspace-2d-hud").first() },
      { label: "selected actor HUD", locator: page.locator(".workspace-2d-frame-fullscreen .workspace-2d-hero-hud").first() },
      { label: "map HUD", locator: mapHud },
      { label: "map controls", locator: mapControls },
      { label: "roster", locator: page.locator(".workspace-2d-frame-fullscreen .workspace-2d-roster").first() },
      {
        label: "dialogue",
        locator: page
          .locator(".workspace-2d-frame-fullscreen .workspace-2d-stage-shell .workspace-2d-dialogue")
          .first(),
      },
    ];

    for (const overlay of overlays) {
      for (const target of protectedTargets) {
        await expectNoOverlapWithAny(overlay.locator, target.locator, overlay.label, target.label);
      }
    }

    await expectNoOverlapWithAny(workspaceTitle, mapHud, "workspace title", "map HUD");
    await expectNoOverlapWithAny(workspaceTitle, mapControls, "workspace title", "map controls");
  });

  test("모바일 2D map 조작 버튼은 최소 터치 영역을 확보한다", async ({ page }) => {
    test.skip((page.viewportSize()?.width ?? 0) > 600, "mobile viewport 전용 회귀");

    await installMockApi(page);
    await seedAuthTokens(page);
    await seedCharacterProfile(page);

    await page.goto(`/workspaces/${workspaceId}`);
    await expect(page.getByRole("heading", { name: primaryWorkspace.name })).toBeVisible();

    await expectMinimumTouchTargets(page.locator(".workspace-2d-minimap-room-button"), "minimap room button");
    await expectMinimumTouchTargets(page.locator(".workspace-2d-map-controls button"), "map zoom control");

    await expectElementBoxInsideViewport(
      page,
      page.locator(".workspace-2d-frame-fullscreen .workspace-2d-map-hud").first(),
      "map HUD",
    );
    await expectElementBoxInsideViewport(
      page,
      page.locator(".workspace-2d-frame-fullscreen .workspace-2d-map-controls").first(),
      "map controls",
    );
  });

  test("2D workspace는 인증된 멤버를 player actor로 쓰고 저장된 avatar를 적용한다", async ({ page }) => {
    await installMockApi(page, { workspaceMembers: "current-user-second" });
    await seedAuthTokens(page, { memberId: 2 });
    await seedCharacterProfile(page);

    await page.goto(`/workspaces/${workspaceId}`);

    const actorLayer = page.locator(".workspace-2d-actor-layer");
    await expect(actorLayer.getByRole("button", { name: /Aquila User 플레이어/ })).toBeVisible();
    await expect(actorLayer.getByRole("button", { name: /First Member 플레이어/ })).toHaveCount(0);
    await expect(actorLayer.getByRole("button", { name: /First Member 채팅/ })).toBeVisible();
    await expect(
      actorLayer.getByRole("button", { name: /Aquila User 플레이어/ }).locator(".pixel-avatar__frame"),
    ).toHaveCSS("box-shadow", /rgb\(31, 122, 95\)/);

    const firstMemberActor = actorLayer.getByRole("button", { name: /First Member 채팅/ });
    await expect(firstMemberActor.locator(".pixel-avatar__frame")).not.toHaveCSS(
      "box-shadow",
      /rgb\(31, 122, 95\)/,
    );
    await expect(page.locator('aside[aria-label="조회 정보"]')).toContainText("멤버 2명 · 관리자 1명");
  });

  test("워크스페이스 파일 탐색 fallback과 directory toggle을 검증한다", async ({ page }) => {
    await installMockApi(page, { artifactFailureMode: true });

    await page.goto(`/workspaces/${workspaceId}`);

    const officeScene = page.locator('section[aria-label="워크스페이스 오피스 씬"]');
    await officeScene.getByRole("button", { name: "Files", exact: true }).click();
    const filesDialog = page.getByRole("dialog", { name: "Workspace files" });
    await expect(filesDialog).toBeVisible();
    await expect(filesDialog.getByText("파일 API 응답 대신 demo workspace 파일을 표시합니다.")).toBeVisible();

    const reportsRow = filesDialog.locator(".workspace-artifact-row-directory").filter({ hasText: "reports" });
    await expect(reportsRow).toHaveClass(/workspace-artifact-row-expanded/);
    await reportsRow.click();
    await expect(filesDialog.getByRole("button", { name: /fallback.md/ })).toHaveCount(0);
    await reportsRow.click();
    await expect(reportsRow).toHaveClass(/workspace-artifact-row-expanded/);
    await filesDialog.getByRole("button", { name: /fallback.md/ }).click();
    await expect(page.getByRole("dialog", { name: "Artifact file" })).toBeVisible();
    await expect(page.getByText("reports/fallback.md")).toBeVisible();
    await expect(page.getByText("Demo workspace artifact")).toBeVisible();
    await closeTopDialog(page);

    const dailyRow = filesDialog.locator(".workspace-artifact-row-directory").filter({ hasText: "daily" });
    await dailyRow.click();
    await expect(filesDialog.getByRole("button", { name: /summary.md/ })).toBeVisible();
    await filesDialog.getByRole("button", { name: /summary.md/ }).click();
    await expect(page.getByRole("dialog", { name: "Artifact file" })).toBeVisible();
    await expect(page.getByText("Daily summary")).toBeVisible();
    await closeTopDialog(page);

    const logsRow = filesDialog.locator(".workspace-artifact-row-directory").filter({ hasText: "logs" });
    await logsRow.click();
    await expect(filesDialog.getByRole("button", { name: /plain-output.log/ })).toBeVisible();
    await filesDialog.getByRole("button", { name: /plain-output.log/ }).click();
    await expect(page.getByRole("dialog", { name: "Artifact file" })).toBeVisible();
    await expect(page.getByText("fallback artifact")).toBeVisible();
    await expect(page.getByText("demo workspace plain text")).toBeVisible();
    await closeTopDialog(page);

    await filesDialog.getByRole("button", { name: /missing-file.log/ }).click();
    await expect(page.getByRole("dialog", { name: "Artifact file" })).toBeVisible();
    await expect(page.getByText("파일을 찾을 수 없습니다.")).toBeVisible();
    await closeTopDialog(page);
    await closeTopDialog(page);
  });

  test("워크스페이스 파일 탐색 API tree와 reload preview를 검증한다", async ({ page }) => {
    await installMockApi(page);

    await page.goto(`/workspaces/${workspaceId}`);

    const officeScene = page.locator('section[aria-label="워크스페이스 오피스 씬"]');
    await officeScene.getByRole("button", { name: "Files", exact: true }).click();
    const filesDialog = page.getByRole("dialog", { name: "Workspace files" });
    await expect(filesDialog).toBeVisible();
    await expect(filesDialog.getByRole("button", { name: /result.md/ })).toBeVisible();
    await filesDialog.getByRole("button", { name: "RELOAD" }).click();
    await expect(filesDialog.getByRole("button", { name: /result.md/ })).toBeVisible();
    await filesDialog.getByRole("button", { name: /result.md/ }).click();
    await expect(page.getByRole("dialog", { name: "Artifact file" })).toBeVisible();
    await expect(page.getByText("reports/result.md")).toBeVisible();
    await expect(page.getByText("검증 결과")).toBeVisible();
    await closeTopDialog(page);
    await closeTopDialog(page);
  });

  test("Gateway registry와 workspace settings 초대 화면을 검증한다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);
    await page.addInitScript(() => {
      Object.defineProperty(window.navigator, "clipboard", {
        configurable: true,
        value: {
          writeText: async (value: string) => {
            window.localStorage.setItem("aquila-office.clipboard", value);
          },
        },
      });
    });

    await page.goto("/workspaces/gateway");
    await expect(page.getByRole("heading", { name: "REGISTER A GATEWAY" })).toBeVisible();
    await expect(page.getByText("LINKED · 00")).toBeVisible();
    await page.getByLabel(/DISPLAY NAME/).fill("Local Gate");
    await page.getByLabel(/GATEWAY URL/).fill("https://gateway.example.test");
    await page.getByLabel(/TOKEN/).fill("gateway-secret");
    await page.getByRole("button", { name: /LINK GATE/ }).click();
    await expect(page.getByText("GATEWAY LINKED.")).toBeVisible();
    await expect(page.getByText("LINKED · 01")).toBeVisible();
    await expect(page.getByRole("button", { name: /LOCAL GATE/ })).toBeVisible();
    await expect(page.getByRole("button", { name: /LOCAL GATE[\s\S]*ON/ })).toBeVisible();
    await expect(page.getByText("TOKEN · gate...cret")).toBeVisible();
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem("aio.openclaw.gateways")))
      .toContain("Local Gate");

    await page.goto(`/workspaces/${workspaceId}/settings`);
    await expect(page.getByText("WORKSPACE ADMIN · COMMAND DECK")).toBeVisible();
    await expect(page.getByRole("link", { name: /BACK TO WORKSPACE/ })).toHaveAttribute(
      "href",
      `/workspaces/${workspaceId}`,
    );
    await expect(page.getByRole("button", { name: /WORKSPACE INFO/ })).toBeVisible();
    await expect(page.getByRole("button", { name: /ROSTER/ })).toBeVisible();
    await expect(page.getByRole("button", { name: /^INVITE$/ })).toBeVisible();
    await expect(page.getByRole("button", { name: /INVITE LOG/ })).toBeVisible();
    await expect(page.getByRole("button", { name: "Slack 설정으로" })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "GitHub 설정으로" })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Gateway 설정으로" })).toHaveCount(0);

    await page.getByRole("button", { name: /ROSTER/ }).click();
    await expect(page.getByRole("heading", { name: /ROSTER/ })).toBeVisible();
    await expect(page.getByText("aquila@example.com")).toBeVisible();

    await page.getByRole("button", { name: /^INVITE$/ }).click();
    await expect(page.getByRole("heading", { name: "INVITE", exact: true })).toBeVisible();
    await page.getByRole("button", { name: "LINK" }).click();
    await page.getByLabel("EXPIRES").selectOption("14");
    await page.getByRole("button", { name: /COPY LINK/ }).click();
    await expect(page.getByRole("button", { name: "COPIED" })).toBeVisible();
    await expect(page.getByText("LINK COPIED")).toBeVisible();

    await page.getByRole("button", { name: /INVITE LOG/ }).click();
    await expect(page.getByRole("heading", { name: /INVITE LOG/ })).toBeVisible();
    await page.getByRole("button", { name: "+7D" }).click();
    await expect(page.getByRole("button", { name: "+7D" })).toBeVisible();
    await page.getByRole("button", { name: "COPY" }).first().click();
    await expect(page.getByRole("button", { name: /COPIED|COPY/ }).first()).toBeVisible();
    await page.getByRole("button", { name: "DEL" }).click();
    await expect(page.getByRole("button", { name: "DEL" })).toHaveCount(0);

    await page.goto(`/invites/${inviteToken}`);
    await expect(page.getByRole("heading", { name: "JOIN A WORKSPACE" })).toBeVisible();
    await expect(page.getByText("Product Engineering")).toBeVisible();
    await expect(page.getByText("MEMBER")).toBeVisible();
    await expect(page.getByText(/expires/)).toBeVisible();
    await expect(page.getByRole("button", { name: /ACCEPT INVITE/ })).toBeVisible();
    await page.getByRole("button", { name: /ACCEPT INVITE/ }).click();
    await page.waitForURL((url) => url.pathname === "/workspaces");
    await expect(page.getByRole("heading", { name: "WORKSPACES" })).toBeVisible();

    await page.evaluate((id) => {
      window.localStorage.setItem("aio.slack.pendingWorkspaceId", String(id));
      window.localStorage.setItem("aio.slack.returnTo", `/workspaces/${id}`);
    }, workspaceId);
    await page.goto("/settings/integrations/success");
    await expect(page.locator('section[aria-label="Slack 연동 성공"]')).toBeVisible();
    await expect(page.getByText("Slack 연동이 완료되었습니다.")).toBeVisible();
    await expect
      .poll(() =>
        page.evaluate((id) => window.localStorage.getItem(`aio.workspace.${id}.integrations.slack`), workspaceId),
      )
      .toContain("T123");
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem("aio.slack.pendingWorkspaceId")))
      .toBeNull();
    await page.waitForURL((url) => url.pathname === `/workspaces/${workspaceId}`);

    await page.evaluate((id) => {
      window.localStorage.setItem("aio.slack.returnTo", `/workspaces/${id}`);
    }, workspaceId);
    await page.goto("/settings/integrations/fail");
    await expect(page.locator('section[aria-label="Slack 연동 실패"]')).toBeVisible();
    await page.getByRole("button", { name: "재시도" }).click();
    await page.waitForURL((url) => url.pathname === `/workspaces/${workspaceId}`);
  });

  test("MEMBER 권한의 설정 화면은 navigation을 유지하고 관리 action을 잠근다", async ({ page }) => {
    await installMockApi(page, { workspaceRole: "MEMBER" });
    await seedAuthTokens(page);

    await page.goto(`/workspaces/${workspaceId}/settings`);

    await expect(page.getByRole("link", { name: /BACK TO WORKSPACE/ })).toHaveAttribute(
      "href",
      `/workspaces/${workspaceId}`,
    );
    await expect(page.getByText("MEMBER").first()).toBeVisible();
    await expect(page.getByRole("button", { name: "Slack 설정으로" })).toHaveCount(0);
    await page.getByRole("button", { name: /^INVITE$/ }).click();
    await expect(page.getByRole("button", { name: /SEND|COPY LINK/ })).toBeDisabled();
  });
});

async function installMockApi(page: Page, options: MockApiOptions = {}) {
  const workspaceRole = options.workspaceRole ?? "ADMIN";
  const currentWorkspace = { ...primaryWorkspace, myRole: workspaceRole };
  const currentMember = { ...workspaceMember, role: workspaceRole };
  const workspaceMembers =
    options.workspaceMembers === "current-user-second"
      ? [firstListedWorkspaceMember, { ...secondListedCurrentWorkspaceMember, role: workspaceRole }]
      : [currentMember];

  await page.route("**/api/health", (route) =>
    fulfillJson(route, {
      status: "UP",
      service: "aquila-office-back",
      checkedAt: "2026-05-18T09:00:00Z",
    }),
  );

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    const path = url.pathname;

    if (options.workspaceListGate && method === "GET" && path === "/api/v1/workspaces") {
      await options.workspaceListGate;
    }

    if (method === "POST" && path === "/api/v1/auth/google/login") {
      await fulfillJson(route, authTokens);
      return;
    }

    if (method === "POST" && path === "/api/v1/auth/token/refresh") {
      await fulfillJson(route, {
        accessToken: "e2e-access-token-refreshed",
      });
      return;
    }

    if (method === "POST" && path === "/api/v1/auth/logout") {
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    if (method === "GET" && path === "/api/v1/members/me/profile") {
      await fulfillJson(route, memberProfile());
      return;
    }

    if (method === "PATCH" && path === "/api/v1/members/me/profile") {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      await fulfillJson(route, {
        ...memberProfile(),
        displayName: typeof body.displayName === "string" ? body.displayName : memberProfile().displayName,
        avatarKind: typeof body.avatarKind === "string" ? body.avatarKind : memberProfile().avatarKind,
        avatarColors:
          typeof body.avatarColors === "object" && body.avatarColors
            ? body.avatarColors
            : memberProfile().avatarColors,
      });
      return;
    }

    if (method === "GET" && path === "/api/v1/workspaces") {
      if (options.workspaceList === "error") {
        await fulfillJson(route, { message: "워크스페이스 fixture 오류" }, 503);
        return;
      }
      const workspacePayload =
        options.workspaceList === "empty"
          ? []
          : options.workspaceList === "many"
            ? Array.from({ length: 9 }, (_, index) => ({
                ...currentWorkspace,
                workspaceId: workspaceId + index,
                name: `${currentWorkspace.name} ${index + 1}`,
              }))
            : [currentWorkspace];
      await fulfillJson(route, workspacePayload);
      return;
    }

    if (method === "POST" && path === "/api/v1/workspaces") {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      await fulfillJson(route, {
        ...primaryWorkspace,
        name: typeof body.name === "string" ? body.name : primaryWorkspace.name,
        description: typeof body.description === "string" ? body.description : primaryWorkspace.description,
      });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}`) {
      await fulfillJson(route, currentWorkspace);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/members`) {
      await fulfillJson(route, workspaceMembers);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/dashboard/summary`) {
      await fulfillJson(route, dashboardSummary());
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/members/task-stats`) {
      await fulfillJson(route, [memberTaskStats()]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/agents`) {
      await fulfillJson(route, [readyAgent]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks`) {
      await fulfillJson(route, [regressionTask]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/reports`) {
      await fulfillJson(route, taskReports());
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/chat/tasks/${taskId}/messages`) {
      await fulfillJson(route, chatMessages());
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/chat/sessions/401/messages`) {
      await fulfillJson(route, {
        chatSessionId: 401,
        messages: sendChatResult().messages,
        nextCursor: null,
        hasMore: false,
      });
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/chat/messages`) {
      await fulfillJson(route, sendChatResult());
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/run`) {
      await fulfillJson(route, {
        workspaceId,
        taskId,
        executionId: 901,
        agentId,
        taskStatus: "IN_PROGRESS",
        executionStatus: "RUNNING",
        finalText: null,
        failureReason: null,
      });
      return;
    }

    if (method === "PATCH" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/status`) {
      await fulfillJson(route, regressionTask);
      return;
    }

    if (method === "PATCH" && path === `/api/v1/workspaces/${workspaceId}/presence/me/position`) {
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/presence/stream`) {
      await route.fulfill({
        status: 200,
        contentType: "text/event-stream",
        body: `data: ${JSON.stringify(presenceSnapshot())}\n\n`,
      });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/artifacts/tree`) {
      if (options.artifactFailureMode) {
        await fulfillJson(route, { message: "artifact tree fixture failure" }, 503);
        return;
      }
      await fulfillJson(route, artifactTree());
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/artifacts/files`) {
      if (options.artifactFailureMode) {
        await fulfillJson(route, { message: "artifact file fixture failure" }, 503);
        return;
      }
      await fulfillJson(route, artifactFile(url.searchParams.get("path") ?? "reports/result.md"));
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/artifacts/orchestration-plans/801`) {
      await fulfillJson(route, orchestrationPlan());
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/invites`) {
      await fulfillJson(route, [workspaceInviteManagement()]);
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/invites`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      if ("email" in body || "mode" in body) {
        await fulfillJson(route, { message: "legacy invite payload" }, 422);
        return;
      }
      await fulfillJson(route, {
        ...workspaceInvite(),
        targetEmail: typeof body.targetEmail === "string" ? body.targetEmail : null,
      });
      return;
    }

    if (method === "PATCH" && path === `/api/v1/workspaces/${workspaceId}/invites/901/extend`) {
      await fulfillJson(route, {
        ...workspaceInviteManagement(),
        expiresAt: "2026-05-23T09:00:00Z",
      });
      return;
    }

    if (method === "DELETE" && path === `/api/v1/workspaces/${workspaceId}/invites/901`) {
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    if (method === "GET" && path === `/api/v1/invites/${inviteToken}`) {
      await fulfillJson(route, workspaceInvite());
      return;
    }

    if (method === "POST" && path === `/api/v1/invites/${inviteToken}/accept`) {
      await fulfillJson(route, {
        workspaceId,
        memberGoogleSub: "google-1",
        role: "MEMBER",
      });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/gateway`) {
      await fulfillJson(route, gatewayStatus());
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/gateway/connection-test`) {
      await fulfillJson(route, {
        status: "CONNECTED",
        connected: true,
        gatewayUrl: "https://gateway.example.test",
        message: "연결 가능",
        agentCount: 2,
      });
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/gateway/binding`) {
      await fulfillJson(route, {
        id: 401,
        workspaceId,
        mode: "EXTERNAL",
        gatewayUrl: "https://gateway.example.test",
        maskedToken: "gate****",
      });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/slack/install`) {
      await fulfillJson(route, { url: "https://slack.example.test/install" });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/slack/integrations`) {
      await fulfillJson(route, [slackIntegration()]);
      return;
    }

    if (method === "DELETE" && path === `/api/v1/workspaces/${workspaceId}/slack/integrations/301`) {
      await fulfillJson(route, {
        id: 301,
        workspaceId,
        active: false,
      });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/slack/channel-mappings`) {
      await fulfillJson(route, [slackChannelMapping()]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/github/credentials`) {
      await fulfillJson(route, [githubCredential()]);
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/github/credentials`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      await fulfillJson(route, {
        ...githubCredential(),
        credentialId: 202,
        displayName: typeof body.displayName === "string" ? body.displayName : "Deploy token",
        maskedToken: "ghp_new****",
      });
      return;
    }

    if (method === "PATCH" && path === `/api/v1/workspaces/${workspaceId}/github/credentials/201`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      await fulfillJson(route, {
        ...githubCredential(),
        displayName: typeof body.displayName === "string" ? body.displayName : "Automation token",
        maskedToken: typeof body.token === "string" ? "ghp_edit****" : "ghp_****",
      });
      return;
    }

    if (method === "DELETE" && path === `/api/v1/workspaces/${workspaceId}/github/credentials/201`) {
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    await fulfillJson(route, { message: `Unhandled fixture route: ${method} ${path}` }, 404);
  });
}

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function seedAuthTokens(page: Page, overrides: Partial<AuthTokenFixture> = {}) {
  const nextTokens = { ...authTokens, ...overrides };
  const storeTokens = (tokens: typeof authTokens) => {
    window.localStorage.setItem("aquila-office.auth.accessToken", tokens.accessToken);
    window.localStorage.setItem(
      "aquila-office.auth.user",
      JSON.stringify({
        memberId: tokens.memberId,
        name: tokens.name,
        email: tokens.email,
        role: tokens.role,
      }),
    );
  };

  await page.addInitScript(storeTokens, nextTokens);
  if (page.url() !== "about:blank") {
    await page.evaluate(storeTokens, nextTokens);
  }
}

async function seedCharacterProfile(page: Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem(
      "ai-office.character",
      JSON.stringify({
        kind: "kenji",
        name: "Local Builder",
        colors: {
          skin: "#d8b48a",
          hair: "#101820",
          shirt: "#1f7a5f",
        },
        colorMode: "custom",
        paletteOverride: {
          skin: "#d8b48a",
          hair: "#101820",
          shirt: "#1f7a5f",
        },
        displayName: "Local Builder",
        avatarKind: "kenji",
        avatarColors: {
          skin: "#d8b48a",
          hair: "#101820",
          shirt: "#1f7a5f",
        },
      }),
    );
  });
}

function deferred() {
  let resolve = () => undefined;
  const promise = new Promise<void>((resolver) => {
    resolve = resolver;
  });
  return { promise, resolve };
}

async function closeTopDialog(page: Page) {
  await page.getByRole("button", { name: "닫기" }).last().click();
}

async function expectStageShellToFillFirstScreen(page: Page, stageShell: Locator) {
  const viewport = page.viewportSize();
  const shellBox = await stageShell.boundingBox();
  expect(shellBox).not.toBeNull();
  expect(viewport).not.toBeNull();
  if (!shellBox || !viewport) {
    return;
  }

  expect(shellBox.y).toBeLessThanOrEqual(viewport.height * 0.34);
  expect(shellBox.width).toBeGreaterThanOrEqual(viewport.width * 0.86);
  expect(shellBox.height).toBeGreaterThanOrEqual(viewport.height * 0.64);
}

async function expectElementToTouchViewport(page: Page, locator: Locator, label: string) {
  const viewport = page.viewportSize();
  const box = await locator.boundingBox();
  expect(box, `${label} should be rendered`).not.toBeNull();
  expect(viewport).not.toBeNull();
  if (!box || !viewport) {
    return;
  }

  const intersectsViewport =
    box.x < viewport.width &&
    box.x + box.width > 0 &&
    box.y < viewport.height &&
    box.y + box.height > 0;
  expect(intersectsViewport, `${label} should be visible in the first viewport`).toBe(true);
}

async function expectElementBoxInsideViewport(page: Page, locator: Locator, label: string) {
  const viewport = page.viewportSize();
  const box = await locator.boundingBox();
  expect(box, `${label} should be rendered`).not.toBeNull();
  expect(viewport).not.toBeNull();
  if (!box || !viewport) {
    return;
  }

  expect(box.x, `${label} left edge should stay in viewport`).toBeGreaterThanOrEqual(0);
  expect(box.y, `${label} top edge should stay in viewport`).toBeGreaterThanOrEqual(0);
  expect(box.x + box.width, `${label} right edge should stay in viewport`).toBeLessThanOrEqual(
    viewport.width,
  );
  expect(box.y + box.height, `${label} bottom edge should stay in viewport`).toBeLessThanOrEqual(
    viewport.height,
  );
}

async function expectVerticalGapBetween(
  upper: Locator,
  lower: Locator,
  upperLabel: string,
  lowerLabel: string,
  gap: number,
) {
  const upperBox = await upper.boundingBox();
  const lowerBox = await lower.boundingBox();
  expect(upperBox, `${upperLabel} should be rendered`).not.toBeNull();
  expect(lowerBox, `${lowerLabel} should be rendered`).not.toBeNull();
  if (!upperBox || !lowerBox) {
    return;
  }

  expect(
    lowerBox.y - (upperBox.y + upperBox.height),
    `${upperLabel} and ${lowerLabel} should keep vertical gap`,
  ).toBeGreaterThanOrEqual(gap);
}

async function expectNoOverlapWithAny(
  overlay: Locator,
  targets: Locator,
  overlayLabel: string,
  targetLabel: string,
) {
  const overlayBox = await overlay.boundingBox();
  expect(overlayBox, `${overlayLabel} should be rendered`).not.toBeNull();
  if (!overlayBox) {
    return;
  }

  const count = await targets.count();
  for (let index = 0; index < count; index += 1) {
    const targetBox = await targets.nth(index).boundingBox();
    if (!targetBox) {
      continue;
    }

    const overlaps =
      overlayBox.x < targetBox.x + targetBox.width &&
      overlayBox.x + overlayBox.width > targetBox.x &&
      overlayBox.y < targetBox.y + targetBox.height &&
      overlayBox.y + overlayBox.height > targetBox.y;

    expect(
      overlaps,
      `${overlayLabel} should not overlap ${targetLabel} ${index + 1}`,
    ).toBe(false);
  }
}

async function expectMinimumTouchTargets(locator: Locator, label: string, minSize = 40) {
  const count = await locator.count();
  expect(count, `${label} should have targets`).toBeGreaterThan(0);

  for (let index = 0; index < count; index += 1) {
    const box = await locator.nth(index).boundingBox();
    expect(box, `${label} ${index + 1} should be rendered`).not.toBeNull();
    if (!box) {
      continue;
    }

    expect(box.width, `${label} ${index + 1} width should be touchable`).toBeGreaterThanOrEqual(
      minSize,
    );
    expect(box.height, `${label} ${index + 1} height should be touchable`).toBeGreaterThanOrEqual(
      minSize,
    );
  }
}

async function expectPanAndZoomToMoveViewport(page: Page, frame: Locator) {
  const zoomLabel = page.locator(".workspace-2d-controls span").first();
  const statusLabel = page.locator(".workspace-2d-status span").first();
  const initialZoom = await zoomLabel.textContent();
  const initialStatus = await statusLabel.textContent();

  await frame.hover();
  await page.mouse.wheel(0, -260);
  await expect.poll(() => zoomLabel.textContent()).not.toBe(initialZoom);

  const box = await frame.boundingBox();
  expect(box).not.toBeNull();
  if (!box) {
    return;
  }

  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2 - 120, box.y + box.height / 2 - 60);
  await page.mouse.up();
  await expect.poll(() => statusLabel.textContent()).not.toBe(initialStatus);
}

async function expectKeyboardToMovePlayer(page: Page, frame: Locator, playerHudValue: Locator) {
  const initialPlayerPosition = await playerHudValue.textContent();

  await frame.focus();
  await page.keyboard.press("d");
  await expect.poll(() => playerHudValue.textContent()).not.toBe(initialPlayerPosition);
  const wasdPlayerPosition = await playerHudValue.textContent();
  await page.keyboard.down("Shift");
  await page.keyboard.press("ArrowRight");
  await page.keyboard.up("Shift");
  await expect.poll(() => playerHudValue.textContent()).not.toBe(wasdPlayerPosition);
}

async function openAndAssert2DContextMenu(page: Page, frame: Locator) {
  const box = await frame.boundingBox();
  expect(box).not.toBeNull();
  if (!box) {
    return;
  }
  await frame.click({
    button: "right",
    position: {
      x: Math.min(100, box.width * 0.24),
      y: Math.min(Math.max(260, box.height * 0.52), box.height - 220),
    },
  });
  const focusAction = page.getByRole("button", { name: "Focus" }).first();
  if (!(await focusAction.isVisible().catch(() => false))) {
    const qaAgentActor = page
      .locator(".workspace-2d-actor-layer")
      .getByRole("button", { name: /QA Agent/ })
      .first();
    if ((page.viewportSize()?.width ?? 0) >= 900 && (await qaAgentActor.isVisible().catch(() => false))) {
      await qaAgentActor.click({ button: "right" });
    } else {
      await page
        .locator(".workspace-2d-roster")
        .getByRole("button", { name: /QA Agent/ })
        .first()
        .click({ button: "right" });
    }
  }
  await expect(focusAction).toBeVisible();
  await expect(page.getByRole("button", { name: "Profile" })).toBeVisible();
  const chatAction = page.getByRole("button", { name: "Chat" }).first();
  await expect(chatAction).toBeVisible();
  await chatAction.click();
}

function memberProfile() {
  return {
    memberId: 1,
    displayName: "Aquila User",
    avatarKind: "alex",
    avatarColors: {
      skin: "#c8916b",
      hair: "#2b1f1a",
      shirt: "#27364a",
    },
  };
}

function dashboardSummary() {
  return {
    agentCount: 1,
    runningAgentCount: 1,
    idleAgentCount: 0,
    errorAgentCount: 0,
    taskCount: 1,
    runningTaskCount: 1,
    completedTaskCount: 0,
    failedTaskCount: 0,
    recentReports: [
      {
        executionId: 901,
        taskId,
        agentId,
        status: "IN_PROGRESS",
        summary: "브라우저 회귀 report 생성",
        failureReason: null,
        createdAt: "2026-05-18T09:30:00Z",
      },
    ],
    recentLogs: [
      {
        executionId: 901,
        taskId,
        agentId,
        level: "INFO",
        message: "UI smoke step complete",
        createdAt: "2026-05-18T09:31:00Z",
      },
    ],
  };
}

function memberTaskStats() {
  return {
    memberId: 1,
    memberName: "Aquila User",
    rank: 1,
    taskCount: 4,
    completedTaskCount: 3,
    runningTaskCount: 1,
    failedTaskCount: 0,
    waitingUserTaskCount: 0,
    healthScore: 94,
    flowScore: 91,
    impactScore: 88,
    totalScore: 91,
  };
}

function taskReports() {
  return [
    {
      reportId: 701,
      taskId,
      status: "IN_PROGRESS",
      summary: "브라우저 회귀 report 생성",
      detail: "desktop/mobile 경로 확인",
      recommendedAction: "CI browser gate 유지",
      createdAt: "2026-05-18T09:35:00Z",
      artifacts: [
        {
          artifactId: 801,
          artifactType: "RESULT_FILE",
          name: "result.md",
          url: "http://local/artifacts?path=reports/result.md",
        },
      ],
    },
  ];
}

function chatMessages() {
  return [
    {
      messageId: 601,
      chatSessionId: 401,
      taskId,
      taskExecutionId: 901,
      role: "USER",
      content: "초기 점검을 시작합니다.",
      createdAt: "2026-05-18T09:40:00Z",
    },
  ];
}

function sendChatResult() {
  return {
    chatSessionId: 401,
    taskId,
    workspaceId,
    assignedAgentId: agentId,
    taskStatus: "IN_PROGRESS",
    taskExecutionId: 901,
    executionStatus: "RUNNING",
    orchestrationPlanId: 801,
    finalText: null,
    failureReason: null,
    createdAt: "2026-05-18T09:45:00Z",
    messages: [
      ...chatMessages(),
      {
        messageId: 602,
        chatSessionId: 401,
        taskId,
        taskExecutionId: 901,
        role: "ASSISTANT",
        content: "브라우저 회귀 확인을 진행합니다.",
        createdAt: "2026-05-18T09:45:00Z",
      },
    ],
  };
}

function presenceSnapshot() {
  return {
    type: "presence.snapshot",
    workspaceId,
    memberId: 1,
    position: { left: "360", top: "260" },
    status: "online",
    members: [
      {
        memberId: 1,
        position: { left: "360", top: "260" },
        status: "online",
      },
    ],
    message: null,
  };
}

function artifactTree() {
  return {
    workspaceId,
    rootPath: "/",
    children: [
      {
        name: "reports",
        path: "reports",
        type: "DIRECTORY",
        children: [
          {
            name: "result.md",
            path: "reports/result.md",
            type: "FILE",
            contentType: "text/markdown",
            sizeBytes: 128,
          },
        ],
      },
    ],
  };
}

function artifactFile(path: string) {
  return {
    workspaceId,
    path,
    name: path.split("/").pop() ?? "result.md",
    contentType: "text/markdown",
    sizeBytes: 128,
    content: "# 검증 결과\n\n브라우저 회귀 경로가 정상입니다.",
  };
}

function orchestrationPlan() {
  return {
    workspaceId,
    planId: 801,
    steps: [
      {
        stepId: 11,
        stepKey: "browser-regression",
        sequenceNo: 1,
        title: "브라우저 회귀",
        files: [
          {
            path: "reports/result.md",
            name: "result.md",
            contentType: "text/markdown",
            sizeBytes: 128,
            exists: true,
          },
        ],
      },
    ],
  };
}

function workspaceInvite() {
  return {
    inviteId: 901,
    workspaceId,
    workspaceName: "Product Engineering",
    token: inviteToken,
    targetEmail: "invitee@example.com",
    inviteUrl: `/invites/${inviteToken}`,
    emailStatus: "PENDING",
    role: "MEMBER",
    status: "PENDING",
    expired: false,
    expiresAt: "2026-05-19T09:00:00Z",
    createdAt: "2026-05-18T09:00:00Z",
    acceptedAt: null,
  };
}

function workspaceInviteManagement() {
  return {
    ...workspaceInvite(),
    createdByGoogleSub: "google-1",
    revokedAt: null,
  };
}

function gatewayStatus() {
  return {
    status: "BOUND",
    bound: true,
    bindingId: 401,
    mode: "EXTERNAL",
    gatewayUrl: "https://gateway.example.test",
    maskedToken: "gate****",
    lastStatus: "CONNECTED",
    lastCheckedAt: "2026-05-18T09:00:00Z",
    lastError: null,
    updatedAt: "2026-05-18T09:00:00Z",
  };
}

function slackIntegration() {
  return {
    id: 301,
    workspaceId,
    slackTeamId: "T123",
    slackChannelId: "C123",
    channelName: "ops",
    maskedBotToken: "xoxb****",
    createdByGoogleSub: "google-1",
    active: true,
  };
}

function slackChannelMapping() {
  return {
    mappingId: 302,
    workspaceId,
    teamId: "T123",
    channelId: "C123",
    channelName: "ops",
    maskedBotToken: "xoxb****",
    createdByGoogleSub: "google-1",
    active: true,
  };
}

function githubCredential() {
  return {
    credentialId: 201,
    workspaceId,
    displayName: "Automation token",
    maskedToken: "ghp_****",
    active: true,
    createdAt: "2026-05-18T09:00:00Z",
    updatedAt: "2026-05-18T09:00:00Z",
  };
}
