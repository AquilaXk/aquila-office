import { expect, test, type Locator, type Page, type Route } from "@playwright/test";

const workspaceId = 101;
const taskId = 301;
const waitingTaskId = 302;
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
  dailyTokenLimit: 200000 as number | null,
  todayUsedTokens: 45000,
  createdAt: "2026-05-18T08:50:00Z",
  joinedAt: "2026-05-18T09:00:00Z",
};

const workspaceMember = {
  memberId: 1,
  memberGoogleSub: "google-1",
  email: "aquila@example.com",
  name: "Aquila User",
  role: "ADMIN",
  joinedAt: "2026-05-18T09:00:00Z",
};

const firstListedWorkspaceMember = {
  memberId: 1,
  memberGoogleSub: "google-fallback",
  email: "first@example.com",
  name: "First Member",
  role: "MEMBER",
  joinedAt: "2026-05-18T08:59:00Z",
};

const secondListedCurrentWorkspaceMember = {
  ...workspaceMember,
  memberId: 2,
  memberGoogleSub: "google-2",
};

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
  skillFiles: [
    {
      skillFileId: 91,
      fileName: "qa-rules.md",
      content: "# QA Rules\n- 검증 명령을 반드시 실행한다.",
      syncStatus: "SYNCED",
      syncError: null,
    },
  ],
};

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
};

const waitingApprovalTask = {
  taskId: waitingTaskId,
  workspaceId,
  createdByGoogleSub: "google-1",
  title: "Release approval check",
  description: "배포 전 사람 검토가 필요합니다.",
  type: "PR_REVIEW",
  priority: "URGENT",
  assignedAgentId: agentId,
  status: "WAITING_USER",
  createdAt: "2026-05-18T09:12:00Z",
  updatedAt: "2026-05-18T09:25:00Z",
};

test.describe("브라우저 UI UX 회귀", () => {
  test("워크스페이스 목록의 loading, empty 상태를 표시한다", async ({ page }) => {
    const gate = deferred();
    await installMockApi(page, { workspaceList: "empty", workspaceListGate: gate.promise });
    await seedAuthTokens(page);

    await page.goto("/workspaces");

    await expect(page.getByRole("heading", { name: "내 워크스페이스 (0)" })).toBeVisible();
    await expect(page.getByText("아직 참여 중인 워크스페이스가 없습니다.")).toHaveCount(0);
    gate.resolve();
    await expect(page.getByText("아직 참여 중인 워크스페이스가 없습니다.")).toBeVisible();
    await expect(page.getByRole("link", { name: "첫 워크스페이스 만들기" })).toHaveAttribute(
      "href",
      "/workspaces/new",
    );
  });

  test("워크스페이스 목록 9개 이상 상태에서도 목록 행과 토큰 요약을 표시한다", async ({ page }) => {
    await installMockApi(page, { workspaceList: "many" });
    await seedAuthTokens(page);

    await page.goto("/workspaces");

    await expect(page.getByRole("heading", { name: "내 워크스페이스 (9)" })).toBeVisible();
    await expect(page.getByText("W009")).toBeVisible();
    await expect(page.getByText(/45,000 \/ 200,000/).first()).toBeVisible();
  });

  test("모바일 워크스페이스 목록은 핵심 action과 KPI를 viewport 안에 표시한다", async ({ page }) => {
    test.skip((page.viewportSize()?.width ?? 0) > 600, "mobile viewport 전용 회귀");

    await installMockApi(page);
    await seedAuthTokens(page);

    await page.goto("/workspaces");
    await expect(page.getByRole("heading", { name: "워크스페이스 목록" })).toBeVisible();

    await expectElementBoxInsideViewport(
      page,
      page.getByRole("link", { name: "새 워크스페이스" }).first(),
      "new workspace action",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByRole("button", { name: "로그아웃" }),
      "logout action",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByText("등록된 에이전트").first(),
      "agents KPI",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByText("완료된 작업").first(),
      "done KPI",
    );
    await expectElementBoxInsideViewport(
      page,
      page.getByRole("link", { name: "열기" }).first(),
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
    await expect(page.getByRole("heading", { name: /aquila office/i })).toBeVisible();
  });

  test("인증 없는 Gateway registry 접근은 title screen으로 되돌린다", async ({ page }) => {
    await installMockApi(page);

    await page.goto("/workspaces/gateway");

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: /aquila office/i })).toBeVisible();
  });

  test("인증 없는 settings 접근은 title screen으로 되돌린다", async ({ page }) => {
    await installMockApi(page);

    await page.goto(`/workspaces/${workspaceId}/settings`);

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: /aquila office/i })).toBeVisible();
  });

  test("인증 없는 invite 접근은 리다이렉트 없이 초대 프리뷰와 로그인 링크를 렌더링한다", async ({
    page,
  }) => {
    await installMockApi(page);

    await page.goto(`/invites/${inviteToken}`);

    await expect(page).toHaveURL((url) => url.pathname === `/invites/${inviteToken}`);
    await expect(page.getByRole("heading", { name: "초대 확인 및 참여" })).toBeVisible();
    await expect(page.getByText("Product Engineering")).toBeVisible();
    const loginAcceptLink = page.getByRole("link", { name: "로그인 후 초대 수락" });
    await expect(loginAcceptLink).toBeVisible();
    await expect(loginAcceptLink).toHaveAttribute(
      "href",
      `/auth?returnTo=${encodeURIComponent(`/invites/${inviteToken}`)}`,
    );
  });

  test("title screen은 미인증 계속하기를 title에 머물게 하고 로그인으로 인증 화면에 진입한다", async ({
    page,
  }) => {
    await installMockApi(page);

    await page.goto("/");
    await page.getByRole("menuitem", { name: /워크스페이스로 이동/ }).click();

    await expect(page).toHaveURL((url) => url.pathname === "/");
    await expect(page.getByRole("status")).toContainText("먼저 로그인을 완료해 주세요.");

    await page.getByRole("menuitem", { name: /로그인/ }).click();
    await page.waitForURL((url) => url.pathname === "/auth");
    await expect(page.getByRole("heading", { name: "Google 계정으로 로그인" })).toBeVisible();
  });

  test("title screen에서 워크스페이스 목록으로 이동하고 워크스페이스를 생성한다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);

    await page.goto("/");
    await expect(page.getByText(/Aquila User/)).toBeVisible();
    await page.keyboard.press("ArrowDown");
    await expect(page.getByRole("menuitem", { name: /워크스페이스로 이동/ })).toHaveClass(
      /title-menu-item-active/,
    );
    await page.keyboard.press("Enter");
    await page.waitForURL((url) => url.pathname === "/workspaces");
    await expect(page.getByText("W001")).toBeVisible();
    await expect(page.getByText("에이전트 1")).toBeVisible();
    await expect(page.getByText("진행 1")).toBeVisible();

    await page.getByRole("link", { name: "새 워크스페이스" }).first().click();
    await expect(page.getByRole("heading", { name: "새 워크스페이스 만들기" })).toBeVisible();
    await expect(page.getByText("0/100")).toBeVisible();
    await expect(page.getByText("0/500")).toBeVisible();
    await expect(page.getByRole("link", { name: "취소" })).toHaveAttribute("href", "/workspaces");

    await page.getByLabel(/워크스페이스 이름/).fill(" Platform Ops ");
    await page.getByLabel(/설명/).fill(" 운영 자동화 점검 ");
    const createRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === "POST" && url.pathname === "/api/v1/workspaces";
    });
    await page.getByRole("button", { name: /워크스페이스 생성/ }).click();
    const createPayload = JSON.parse((await createRequest).postData() ?? "{}");
    expect(createPayload).toEqual({
      name: "Platform Ops",
      description: "운영 자동화 점검",
    });
    await page.waitForURL((url) => url.pathname === `/workspaces/${workspaceId}`);
  });

  test("타이틀과 인증 화면은 라이트/다크 테마 전환과 Google 로그인 프레임을 유지한다", async ({
    page,
  }) => {
    await installMockApi(page);

    await page.goto("/");
    await expect(page.getByRole("heading", { name: /aquila office/i })).toBeVisible();
    await page.getByRole("button", { name: "라이트 모드로 전환" }).click();
    await expect(page.locator("html")).toHaveAttribute("data-theme", "light");

    await page.goto("/auth?returnTo=/workspaces/new");
    await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
    await expect(page.getByText("Google 계정으로 로그인")).toBeVisible();
    await expect(page.getByText("워크스페이스 계정 인증")).toBeVisible();
    await expect(page.locator(".auth-google-visible-label")).toHaveText("Google 계정으로 계속하기");
    await expect(page.locator(".auth-google-visible-label")).toBeVisible();

    await seedAuthTokens(page);
    await Promise.all([
      page.waitForURL(
        (url) =>
          url.pathname === "/auth/character" && url.searchParams.get("returnTo") === "/workspaces/new",
      ),
      page.reload(),
    ]);
    await expect(page.getByRole("heading", { name: "오피스 아바타 및 표시 이름" })).toBeVisible();
  });

  test("캐릭터 프로필 설정에서 avatar와 색상 저장 흐름을 검증한다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);
    await seedCharacterProfile(page);

    await page.goto("/auth/character?returnTo=/workspaces");
    await expect(page.getByRole("heading", { name: "오피스 아바타 및 표시 이름" })).toBeVisible();
    await expect(page.getByPlaceholder("예: 민수")).toHaveValue("Aquila User");

    await page.getByRole("button", { name: /kenji/i }).click();
    await page.getByRole("button", { name: "사용자 지정 색상" }).click();
    await page.getByLabel("상의 색상 02").click();
    await page.getByPlaceholder("예: 민수").fill("Aquila Pilot");
    const profileUpdateRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === "PATCH" && url.pathname === "/api/v1/members/me/profile";
    });
    await page.getByRole("button", { name: /저장 후 계속하기/ }).click();
    const profileUpdateBody = (await profileUpdateRequest).postData() ?? "";
    expect(profileUpdateBody).toContain("Aquila Pilot");
    expect(profileUpdateBody).toContain("kenji");

    await page.waitForURL((url) => url.pathname === "/workspaces");
    const storedCharacter = await page.evaluate(() =>
      window.localStorage.getItem("ai-office.character"),
    );
    expect(storedCharacter).toContain("Aquila Pilot");
    expect(storedCharacter).toContain("kenji");
    expect(storedCharacter).toContain("#3a5a3a");
  });

  test("워크스페이스 목록 로그아웃은 세션을 지우고 title screen으로 돌아간다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);

    await page.goto("/workspaces");
    await expect(page.getByRole("heading", { name: "워크스페이스 목록" })).toBeVisible();

    const logoutRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === "POST" && url.pathname === "/api/v1/auth/logout";
    });
    await page.getByRole("button", { name: "로그아웃" }).click();
    await logoutRequest;

    await page.waitForURL((url) => url.pathname === "/");
    await expect(page.getByRole("heading", { name: /aquila office/i })).toBeVisible();
    await expect
      .poll(() =>
        page.evaluate(() => window.localStorage.getItem("aquila-office.auth.accessToken")),
      )
      .toBeNull();
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem("aquila-office.auth.user")))
      .toBeNull();
  });

  test("워크스페이스 5영역 데스크톱 크롬, 5대 멀티뷰, 인스펙터, 에이전트/태스크/승인/오케스트레이션/산출물 흐름을 검증한다", async ({
    page,
  }) => {
    await installMockApi(page);
    await seedAuthTokens(page);
    await page.addInitScript(() => {
      window.open = () => null;
    });

    await page.goto(`/workspaces/${workspaceId}`);

    // 1. 5-Region Chrome & 2D Office Stage
    await expect(page.locator(".chrome-menu-title")).toHaveText(primaryWorkspace.name);
    await expect(page.locator(".chrome-statusbar")).toContainText(/실시간 (연결됨|연결 중)/);
    await expect(page.locator(".chrome-statusbar")).toContainText("참여자 1명");
    await expect(page.locator(".chrome-statusbar")).toContainText("게이트웨이 연결 가능");
    await expect(page.locator(".chrome-statusbar")).toContainText("45,000 / 200,000");

    const officeScene = page.locator('section[aria-label="워크스페이스 오피스 씬"]');
    await expect(officeScene).toBeVisible();

    // Agent Hire Modal
    await page.getByRole("button", { name: "에이전트 추가" }).first().click();
    const hireDialog = page.getByRole("dialog", { name: "에이전트 추가" });
    await expect(hireDialog).toBeVisible();
    await hireDialog.getByRole("button", { name: "스킬 파일 추가" }).click();
    await expect(hireDialog.getByPlaceholder("backend-agent.md")).toBeVisible();
    await closeTopDialog(page);

    // Connections Modal (Slack / Discord / GitHub / OpenClaw Gateway)
    await page.getByRole("button", { name: "연결 설정" }).click();
    const connectionDialog = page.getByRole("dialog", { name: "연결 설정" });
    await expect(connectionDialog).toBeVisible();
    await connectionDialog.getByRole("button", { name: "Discord", exact: true }).click();
    await expect(connectionDialog.getByText("Discord 연동 준비 중")).toBeVisible();
    await connectionDialog.getByRole("button", { name: "뒤로" }).click();
    await connectionDialog.getByRole("button", { name: "Slack", exact: true }).click();
    await connectionDialog.getByRole("button", { name: "Slack 설치 열기" }).click();
    await expect
      .poll(() => page.evaluate(() => localStorage.getItem("aio.slack.pendingWorkspaceId")))
      .toBe(String(workspaceId));
    await connectionDialog.getByRole("button", { name: "뒤로" }).click();
    await connectionDialog.getByRole("button", { name: "GitHub", exact: true }).click();
    await connectionDialog.locator(".workspace-connection-form").getByLabel(/표시 이름/).fill("Deploy token");
    await connectionDialog.locator(".workspace-connection-form").getByLabel(/토큰/).fill("ghp_new_secret");
    await connectionDialog.getByRole("button", { name: "자격 증명 등록" }).click();
    await expect(
      connectionDialog.locator(".workspace-connection-select").filter({ hasText: "Deploy token" }),
    ).toBeVisible();
    await connectionDialog.getByRole("button", { name: "뒤로" }).click();
    await connectionDialog.getByRole("button", { name: "AI / OpenClaw", exact: true }).click();
    await connectionDialog.getByLabel("Gateway URL").fill("https://gateway.example.test");
    await connectionDialog.getByLabel("Token").fill("gateway-secret");
    await connectionDialog.getByRole("button", { name: "연결 테스트" }).click();
    await expect(connectionDialog.getByText("연결 가능").first()).toBeVisible();
    await connectionDialog.getByRole("button", { name: "OpenClaw 연결 등록" }).click();
    await expect(connectionDialog.getByText("OpenClaw 게이트웨이 연결을 저장했습니다.")).toBeVisible();
    await connectionDialog.getByRole("button", { name: "뒤로" }).click();
    await connectionDialog.getByRole("button", { name: "AI 모델 프로바이더", exact: true }).click();
    await expect(
      connectionDialog.getByRole("tab", { name: "전체" }),
    ).toBeVisible();
    await connectionDialog.getByRole("button", { name: /OpenAI/ }).first().click();
    await connectionDialog.getByLabel("API 키").fill("sk-e2e-test-secret");
    await connectionDialog.getByRole("button", { name: "모델 목록 조회" }).click();
    await expect(connectionDialog.getByText("조회된 모델 (2개)")).toBeVisible();
    await connectionDialog.getByRole("button", { name: "실호출 테스트" }).click();
    await expect(connectionDialog.getByText("E2E 연결 확인 응답")).toBeVisible();
    await connectionDialog.getByRole("button", { name: "프로바이더 등록" }).click();
    await expect(
      connectionDialog.locator(".ao-model-provider-row").filter({ hasText: "OpenAI" }),
    ).toBeVisible();
    await closeTopDialog(page);

    // 2D Stage keyboard, zoom, pan, and actor context menu -> Inspector
    const canvasFrame = page.getByRole("region", { name: "오피스 2D 스테이지" });
    await expect(canvasFrame).toBeVisible();
    await expect(page.locator(".stage-actor__proximity-ring")).toBeVisible();
    const playerHudValue = page
      .locator(".workspace-2d-hud div")
      .filter({ hasText: "내 위치" })
      .locator("strong");
    await expectKeyboardToMovePlayer(page, canvasFrame, playerHudValue);
    const stageFrame = page.locator(".workspace-2d-stage").first();
    await expectPanAndZoomToMoveViewport(page, stageFrame);
    await openAndAssert2DContextMenu(page);

    // Right Context Inspector: Agent Chat, Tasks, Edit & Skill Update
    const inspector = page.locator('aside[aria-label="컨텍스트 인스펙터"]');
    await expect(inspector).toBeVisible();
    await inspector.getByLabel("에이전트 대화 메시지").fill("에이전트 상태 점검");
    await inspector.getByRole("button", { name: "메시지 전송" }).click();
    await expect(inspector.getByText("브라우저 회귀 확인을 진행합니다.")).toBeVisible();
    await inspector.getByRole("tab", { name: /작업/ }).click();
    await expect(inspector.getByText(regressionTask.title)).toBeVisible();

    await inspector.getByRole("tab", { name: "설정 · 스킬" }).click();
    await inspector.getByLabel("에이전트 이름 수정").fill("QA Lead Agent");
    const patchAgentReq = page.waitForRequest(
      (req) => req.method() === "PATCH" && req.url().includes(`/agents/${agentId}`),
    );
    await inspector.getByRole("button", { name: "설정 저장" }).click();
    await patchAgentReq;
    await expect(inspector.getByText("에이전트 설정을 저장했습니다.")).toBeVisible();

    await page.keyboard.press("Escape");
    await expect(inspector).toHaveCount(0);

    // 2. Task Board View + Task Inspector + Create Modal + Stale Recovery
    await page.getByRole("tab", { name: /작업 보드/ }).click();
    await expect(page.getByRole("heading", { name: "작업 보드" })).toBeVisible();
    await expect(page.getByText("45,000 / 200,000 (23%)", { exact: true })).toBeVisible();
    await page.getByLabel("작업 검색").fill("Regression");
    await expect(page.getByText("Regression smoke")).toBeVisible();
    await page.getByLabel("작업 검색").fill("");

    // Open Task Inspector & check Logs / Reports / Modal Chat
    await page.getByText("Regression smoke").click();
    await expect(inspector).toBeVisible();
    await expect(inspector.getByText("UI smoke step complete")).toBeVisible();
    await inspector.getByRole("tab", { name: /리포트/ }).click();
    await expect(inspector.getByText("리포트 #701")).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(inspector).toHaveCount(0);

    // Recover stale tasks
    const taskBoardView = page.getByLabel("작업 보드 뷰");
    await taskBoardView.getByRole("button", { name: "고착 작업 복구" }).click();
    await expect(taskBoardView.getByText(/고착 복구 완료: 작업 1건, 실행 1건/)).toBeVisible();

    // Create task modal
    await taskBoardView.getByRole("button", { name: "새 작업 생성" }).click();
    const createTaskDialog = page.getByRole("dialog", { name: "새 작업 생성" });
    await createTaskDialog.getByPlaceholder(/로그인 인증 오류/).fill("신규 E2E 작업");
    await createTaskDialog.getByRole("button", { name: "작업만 생성" }).click();
    await expect(taskBoardView.getByText("신규 E2E 작업")).toBeVisible();

    // 3. Approval Inbox View
    await page.getByRole("tab", { name: /승인 대기함/ }).click();
    const approvalView = page.getByLabel("승인 대기함 뷰");
    await expect(approvalView.getByRole("heading", { name: "승인 대기함" })).toBeVisible();
    await expect(approvalView.getByText("Release approval check")).toBeVisible();
    await approvalView.getByLabel(`작업 #${waitingTaskId} 검토 의견`).fill("검증 완료, 승인합니다.");
    const reviewReq = page.waitForRequest(
      (req) =>
        req.method() === "POST" && req.url().includes(`/tasks/${waitingTaskId}/review-approval`),
    );
    await approvalView.getByRole("button", { name: "승인", exact: true }).click();
    await reviewReq;
    await expect(approvalView.getByText(`작업 #${waitingTaskId} 승인을 완료했습니다.`)).toBeVisible();

    // 4. Orchestration View
    await page.getByRole("tab", { name: "오케스트레이션" }).click();
    const orchestrationView = page.getByLabel("오케스트레이션 뷰");
    await expect(orchestrationView.getByRole("heading", { name: "오케스트레이션 세션" })).toBeVisible();
    await orchestrationView
      .getByPlaceholder("이 단계에서 수행할 작업 지시를 입력하세요.")
      .fill("백엔드 스키마 검증");
    const createOrchReq = page.waitForRequest(
      (req) => req.method() === "POST" && req.url().endsWith("/orchestrations"),
    );
    await orchestrationView.getByRole("button", { name: "오케스트레이션 세션 생성" }).click();
    await createOrchReq;
    await expect(orchestrationView.getByText("오케스트레이션 세션을 생성했습니다.")).toBeVisible();
    const dispatchReq = page.waitForRequest(
      (req) => req.method() === "POST" && req.url().includes("/orchestrations/701/dispatch"),
    );
    await orchestrationView.getByRole("button", { name: "세션 실행" }).click();
    await dispatchReq;
    await expect(orchestrationView.getByText(/세션 #701 실행을 전달했습니다/)).toBeVisible();
    const dagNodeBtn = orchestrationView.locator(".ao-dag-node-btn").first();
    await expect(dagNodeBtn).toBeVisible();
    await dagNodeBtn.click();
    await expect(dagNodeBtn).toHaveAttribute("aria-pressed", "true");
    await expect(orchestrationView.locator(".ao-dag-detail-card")).toBeVisible();
    await expect(
      orchestrationView.getByRole("button", { name: "작업 인스펙터 열기" }),
    ).toBeVisible();

    // 5. Artifacts & Local Git Diff View
    await page.getByRole("tab", { name: "산출물 및 변경" }).click();
    const artifactsView = page.getByLabel("산출물 및 변경 뷰");
    await expect(artifactsView.getByRole("heading", { name: "산출물 및 로컬 Git 변경사항" })).toBeVisible();
    await expect(artifactsView.getByRole("button", { name: /result\.md/ })).toBeVisible();
    await artifactsView.getByRole("button", { name: /result\.md/ }).click();
    await expect(artifactsView.getByText("브라우저 회귀 경로가 정상입니다.")).toBeVisible();

    await artifactsView.getByRole("button", { name: "로컬 Git 변경사항" }).click();
    await artifactsView
      .getByPlaceholder(/--- a\/file\.ts/)
      .fill("--- a/front/sample.ts\n+++ b/front/sample.ts\n@@ -1 +1 @@\n-oldLine\n+newLine");
    await expect(artifactsView.getByText(/newLine/).first()).toBeVisible();
  });

  test("모바일 워크스페이스 2D 컨트롤 및 사이드바 토글이 viewport 안에서 동작한다", async ({
    page,
  }) => {
    test.skip((page.viewportSize()?.width ?? 0) > 600, "mobile viewport 전용 회귀");

    await installMockApi(page);
    await seedAuthTokens(page);
    await seedCharacterProfile(page);

    await page.goto(`/workspaces/${workspaceId}`);
    await expect(page.locator(".chrome-menu-title")).toHaveText(primaryWorkspace.name);

    const dialogue = page
      .locator(".workspace-2d-frame-fullscreen .workspace-2d-stage-shell .workspace-2d-dialogue")
      .first();
    await expectElementBoxInsideViewport(page, dialogue, "dialogue");
    await expectNoContentClipping(dialogue, "dialogue");
    await expectMinimumTouchTargets(
      page.locator(".workspace-2d-map-controls button"),
      "map zoom control",
    );
    await expectElementBoxInsideViewport(
      page,
      page.locator(".workspace-2d-frame-fullscreen .workspace-2d-map-hud").first(),
      "map HUD",
    );
  });

  test("2D workspace는 인증된 멤버를 player actor로 쓰고 저장된 avatar를 적용한다", async ({
    page,
  }) => {
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
  });

  test("워크스페이스 파일 탐색 실패 시 fail-closed 에러 상태를 표시한다", async ({ page }) => {
    await installMockApi(page, { artifactFailureMode: true });
    await seedAuthTokens(page);

    await page.goto(`/workspaces/${workspaceId}`);
    await page.getByRole("tab", { name: "산출물 및 변경" }).click();

    await expect(page.getByText("artifact tree fixture failure")).toBeVisible();
    await expect(page.locator(".workspace-artifact-node")).toHaveCount(0);
  });

  test("실API 기반 Gateway 페이지, 워크스페이스 설정(일일 토큰 예산), 인증 후 초대 수락 및 Slack 콜백을 검증한다", async ({
    page,
  }) => {
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

    // Real API Gateway page
    await page.goto("/workspaces/gateway");
    await expect(
      page.getByRole("heading", { name: "OpenClaw 게이트웨이 연동 관리" }),
    ).toBeVisible();
    await expect(page.getByText("연결됨").first()).toBeVisible();
    await page.getByLabel("Gateway URL").fill("https://gateway.example.test");
    await page.getByLabel("Gateway Token").fill("gateway-secret");
    await page.getByRole("button", { name: "연결 테스트" }).click();
    await expect(page.getByText("연결 가능").first()).toBeVisible();
    await page.getByRole("button", { name: "OpenClaw 연결 저장" }).click();
    await expect(page.getByText("OpenClaw 게이트웨이 연결을 저장했습니다.")).toBeVisible();
    await expect(page.getByLabel("모델 프로바이더 카탈로그")).toBeVisible();
    await expect(page.getByLabel("등록된 모델 프로바이더 체인")).toBeVisible();

    // Workspace settings + dailyTokenLimit + roster + invite
    await page.goto(`/workspaces/${workspaceId}/settings`);
    await expect(page.getByRole("heading", { name: /Product Engineering 설정/ })).toBeVisible();
    await page.getByLabel("일일 토큰 예산 한도").fill("500000");
    const patchWsReq = page.waitForRequest(
      (req) => req.method() === "PATCH" && req.url().endsWith(`/workspaces/${workspaceId}`),
    );
    await page.getByRole("button", { name: "저장" }).click();
    const patchWsBody = JSON.parse((await patchWsReq).postData() ?? "{}");
    expect(patchWsBody.dailyTokenLimit).toBe(500000);
    await expect(page.getByText("워크스페이스 설정을 저장했습니다.")).toBeVisible();

    await page.getByRole("button", { name: /구성원 관리/ }).click();
    await expect(page.getByText("aquila@example.com")).toBeVisible();

    await page.getByRole("button", { name: "초대 생성" }).click();
    await page.getByRole("button", { name: "공유 링크 생성" }).click();
    await page.getByRole("button", { name: "초대 링크 생성" }).click();
    await expect(page.getByText(/초대 링크가 생성되었습니다/)).toBeVisible();

    await page.getByRole("button", { name: /초대 이력/ }).click();
    await page.getByRole("button", { name: "7일 연장" }).click();
    await page.getByRole("button", { name: "삭제" }).click();
    await expect(page.getByRole("button", { name: "삭제" })).toHaveCount(0);

    // Authenticated Invite Accept
    await page.goto(`/invites/${inviteToken}`);
    await expect(page.getByRole("heading", { name: "초대 확인 및 참여" })).toBeVisible();
    await page.getByRole("button", { name: "초대 수락" }).click();
    await page.waitForURL((url) => url.pathname === "/workspaces");

    // Slack callbacks
    await page.evaluate((id) => {
      window.localStorage.setItem("aio.slack.pendingWorkspaceId", String(id));
      window.localStorage.setItem("aio.slack.returnTo", `/workspaces/${id}`);
    }, workspaceId);
    await page.goto("/settings/integrations/success");
    await expect(page.locator('section[aria-label="Slack 연동 성공"]')).toBeVisible();
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

    await expect(page.getByRole("link", { name: "오피스로 돌아가기" })).toHaveAttribute(
      "href",
      `/workspaces/${workspaceId}`,
    );
    await page.getByRole("button", { name: "초대 생성" }).click();
    await expect(page.getByRole("button", { name: "초대 발송" })).toBeDisabled();
  });
});

async function installMockApi(page: Page, options: MockApiOptions = {}) {
  const workspaceRole = options.workspaceRole ?? "ADMIN";
  let currentWorkspace: Omit<typeof primaryWorkspace, "dailyTokenLimit"> & {
    dailyTokenLimit: number | null;
  } = {
    ...primaryWorkspace,
    myRole: workspaceRole,
  };
  const currentMember = { ...workspaceMember, role: workspaceRole };
  const workspaceMembers =
    options.workspaceMembers === "current-user-second"
      ? [firstListedWorkspaceMember, { ...secondListedCurrentWorkspaceMember, role: workspaceRole }]
      : [currentMember];

  let agentsState = [{ ...readyAgent }];
  let tasksState = [{ ...regressionTask }, { ...waitingApprovalTask }];
  let orchestrationsState: Array<{
    sessionId: number;
    workspaceId: number;
    sourceTaskId: number;
    status: string;
    createdAt: string;
    steps: Array<{
      stepKey: string;
      instruction: string;
      agentId: number;
      taskId: number | null;
      dependsOn: string[];
      status: string;
    }>;
  }> = [
    {
      sessionId: 701,
      workspaceId,
      sourceTaskId: taskId,
      status: "PLANNED",
      createdAt: "2026-05-18T09:15:00Z",
      steps: [
        {
          stepKey: "plan",
          instruction: "초기 설계 검증",
          agentId,
          taskId,
          dependsOn: [],
          status: "PENDING",
        },
      ],
    },
  ];
  let gatewayState = gatewayStatus();
  let modelProvidersState: Array<Record<string, unknown>> = [];
  let nextModelProviderId = 901;

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
      await fulfillJson(route, { accessToken: "e2e-access-token-refreshed" });
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
        displayName:
          typeof body.displayName === "string" ? body.displayName : memberProfile().displayName,
        avatarKind:
          typeof body.avatarKind === "string" ? body.avatarKind : memberProfile().avatarKind,
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
        description:
          typeof body.description === "string" ? body.description : primaryWorkspace.description,
      });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}`) {
      await fulfillJson(route, currentWorkspace);
      return;
    }

    if (method === "PATCH" && path === `/api/v1/workspaces/${workspaceId}`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      currentWorkspace = {
        ...currentWorkspace,
        name: typeof body.name === "string" ? body.name : currentWorkspace.name,
        description:
          typeof body.description === "string" ? body.description : currentWorkspace.description,
        dailyTokenLimit:
          body.dailyTokenLimit === null || typeof body.dailyTokenLimit === "number"
            ? body.dailyTokenLimit
            : currentWorkspace.dailyTokenLimit,
      };
      await fulfillJson(route, currentWorkspace);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/members`) {
      await fulfillJson(route, workspaceMembers);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/dashboard/summary`) {
      await fulfillJson(route, dashboardSummary(currentWorkspace.dailyTokenLimit, currentWorkspace.todayUsedTokens));
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/agents`) {
      await fulfillJson(route, agentsState);
      return;
    }

    if (method === "PATCH" && path === `/api/v1/workspaces/${workspaceId}/agents/${agentId}`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      agentsState = agentsState.map((agent) =>
        agent.agentId === agentId
          ? {
              ...agent,
              name: typeof body.name === "string" ? body.name : agent.name,
              category: typeof body.category === "string" ? body.category : agent.category,
              workspacePath:
                typeof body.workspacePath === "string" ? body.workspacePath : agent.workspacePath,
            }
          : agent,
      );
      await fulfillJson(route, agentsState[0]);
      return;
    }

    if (method === "DELETE" && path === `/api/v1/workspaces/${workspaceId}/agents/${agentId}`) {
      agentsState = [];
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks`) {
      await fulfillJson(route, tasksState);
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/tasks`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      const created = {
        ...regressionTask,
        taskId: 399,
        title: typeof body.title === "string" ? body.title : "Created task",
        status: "REQUESTED",
      };
      tasksState = [created, ...tasksState];
      await fulfillJson(route, created, 201);
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/tasks/recover-stale`) {
      await fulfillJson(route, { recoveredTaskCount: 1, recoveredExecutionCount: 1 });
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/tasks/${waitingTaskId}/review-approval`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      const nextStatus = body.decision === "APPROVE" ? "IN_PROGRESS" : "REQUESTED";
      tasksState = tasksState.map((t) =>
        t.taskId === waitingTaskId ? { ...t, status: nextStatus } : t,
      );
      await fulfillJson(route, tasksState.find((t) => t.taskId === waitingTaskId));
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/cancel`) {
      tasksState = tasksState.map((t) =>
        t.taskId === taskId ? { ...t, status: "CANCELED" } : t,
      );
      await fulfillJson(route, tasksState.find((t) => t.taskId === taskId));
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/request-approval`) {
      tasksState = tasksState.map((t) =>
        t.taskId === taskId ? { ...t, status: "WAITING_USER" } : t,
      );
      await fulfillJson(route, tasksState.find((t) => t.taskId === taskId));
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/logs`) {
      await fulfillJson(route, [
        {
          logId: 1001,
          taskId,
          taskExecutionId: 901,
          agentId,
          level: "INFO",
          message: "UI smoke step complete",
          createdAt: "2026-05-18T09:31:00Z",
        },
      ]);
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

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/orchestrations`) {
      await fulfillJson(route, orchestrationsState);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/orchestrations/701`) {
      await fulfillJson(route, orchestrationsState[0]);
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/orchestrations`) {
      const body = (request.postDataJSON() ?? {}) as { sourceTaskId?: number; steps?: Array<Record<string, unknown>> };
      const created = {
        sessionId: 701,
        workspaceId,
        sourceTaskId: body.sourceTaskId ?? taskId,
        status: "PLANNED",
        createdAt: "2026-05-18T09:40:00Z",
        steps: (body.steps ?? []).map((s) => ({
          stepKey: String(s.stepKey ?? "step"),
          instruction: String(s.instruction ?? ""),
          agentId: Number(s.agentId ?? agentId),
          taskId: null,
          dependsOn: Array.isArray(s.dependsOn) ? s.dependsOn : [],
          status: "PENDING",
        })),
      };
      orchestrationsState = [created, ...orchestrationsState.filter((item) => item.sessionId !== 701)];
      await fulfillJson(route, created, 201);
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/orchestrations/701/dispatch`) {
      const dispatchedSteps = (orchestrationsState[0]?.steps ?? []).map((step) => ({
        ...step,
        taskId: step.taskId ?? taskId,
        status: "RUNNING",
      }));
      if (orchestrationsState[0]) {
        orchestrationsState[0] = {
          ...orchestrationsState[0],
          status: "RUNNING",
          steps: dispatchedSteps,
        };
      }
      await fulfillJson(route, {
        sessionId: 701,
        workspaceId,
        sourceTaskId: taskId,
        status: "RUNNING",
        dispatchedCount: dispatchedSteps.length,
        steps: dispatchedSteps,
      });
      return;
    }

    if (method === "PATCH" && path === `/api/v1/workspaces/${workspaceId}/presence/me/position`) {
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/presence/stream-ticket`) {
      await fulfillJson(route, {
        ticket: "e2e-sse-ticket-1",
        expiresInSeconds: 60,
      });
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

    if (method === "GET" && path === "/api/v1/model-catalog") {
      await fulfillJson(route, {
        presets: [
          {
            providerKind: "OPENAI",
            displayName: "OpenAI",
            defaultBaseUrl: "https://api.openai.com/v1",
            wireProtocol: "OPENAI_RESPONSES",
            tier: "PAID",
            authRequired: true,
            models: [
              {
                id: "gpt-5.2",
                displayName: "GPT-5.2",
                contextWindow: 400000,
                maxOutputTokens: 128000,
                supportsReasoning: true,
                supportsTools: true,
                supportsVision: true,
              },
              {
                id: "gpt-5-mini",
                displayName: "GPT-5 Mini",
                contextWindow: 400000,
                maxOutputTokens: 128000,
                supportsReasoning: true,
                supportsTools: true,
                supportsVision: true,
              },
            ],
          },
          {
            providerKind: "OLLAMA",
            displayName: "Ollama (Local)",
            defaultBaseUrl: "http://127.0.0.1:11434",
            wireProtocol: "OLLAMA_NATIVE",
            tier: "LOCAL",
            authRequired: false,
            models: [
              {
                id: "qwen3:14b",
                displayName: "Qwen 3 14B",
                contextWindow: 32768,
                maxOutputTokens: 8192,
                supportsReasoning: true,
                supportsTools: true,
                supportsVision: false,
              },
            ],
          },
        ],
      });
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/model-providers`) {
      await fulfillJson(route, modelProvidersState);
      return;
    }

    if (method === "POST" && path === `/api/v1/workspaces/${workspaceId}/model-providers`) {
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      const priorityOrder =
        typeof body.priorityOrder === "number"
          ? body.priorityOrder
          : typeof body.priority === "number"
            ? body.priority
            : 10;
      const defaultModel = typeof body.defaultModel === "string" ? body.defaultModel : "gpt-5.2";
      const enabledModels = Array.isArray(body.enabledModels) ? body.enabledModels : [defaultModel];
      const created = {
        providerId: nextModelProviderId++,
        workspaceId,
        providerKind: String(body.providerKind ?? "OPENAI"),
        wireProtocol: String(body.wireProtocol ?? "OPENAI_CHAT"),
        displayName: String(body.displayName ?? "OpenAI"),
        baseUrl: String(body.baseUrl ?? "https://api.openai.com/v1"),
        defaultModel,
        enabledModels,
        priorityOrder,
        enabled: typeof body.enabled === "boolean" ? body.enabled : true,
        hasApiKey: typeof body.apiKey === "string" && body.apiKey.trim().length > 0,
        maskedApiKey: typeof body.apiKey === "string" && body.apiKey.trim() ? "sk-e****" : null,
        lastStatus: "CONNECTED",
        lastLatencyMs: 42,
        lastCheckedAt: "2026-05-18T09:15:00Z",
        lastError: null,
      };
      modelProvidersState = [...modelProvidersState, created];
      await fulfillJson(route, created);
      return;
    }

    if (
      method === "POST" &&
      path === `/api/v1/workspaces/${workspaceId}/model-providers/connection-test`
    ) {
      await fulfillJson(route, {
        ok: true,
        status: 200,
        latencyMs: 42,
        discoveredModels: ["gpt-5.2", "gpt-5-mini"],
        resolvedModel: "gpt-5.2",
        replyPreview: "E2E 연결 확인 응답",
        promptTokens: 12,
        completionTokens: 8,
        message: "연결 확인 완료",
      });
      return;
    }

    if (
      (method === "PATCH" || method === "DELETE") &&
      path.startsWith(`/api/v1/workspaces/${workspaceId}/model-providers/`)
    ) {
      const targetId = Number(path.split("/").pop());
      if (method === "DELETE") {
        modelProvidersState = modelProvidersState.filter((item) => item.providerId !== targetId);
        await route.fulfill({ status: 204, body: "" });
        return;
      }
      const body = (request.postDataJSON() ?? {}) as Record<string, unknown>;
      let updated: Record<string, unknown> | null = null;
      modelProvidersState = modelProvidersState.map((item) => {
        if (item.providerId !== targetId) return item;
        updated = {
          ...item,
          ...body,
        };
        return updated;
      });
      await fulfillJson(route, updated ?? {});
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
      await fulfillJson(route, gatewayState);
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
      gatewayState = {
        ...gatewayState,
        status: "BOUND",
        bound: true,
        gatewayUrl: "https://gateway.example.test",
        lastStatus: "CONNECTED",
      };
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
      await fulfillJson(route, { id: 301, workspaceId, active: false });
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
  let resolve: () => void = () => {};
  const promise = new Promise<void>((resolver) => {
    resolve = resolver;
  });
  return { promise, resolve };
}

async function closeTopDialog(page: Page) {
  await page.getByRole("button", { name: "닫기" }).last().click();
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

async function expectNoContentClipping(locator: Locator, label: string) {
  const metrics = await locator.evaluate((element) => ({
    clientHeight: element.clientHeight,
    clientWidth: element.clientWidth,
    scrollHeight: element.scrollHeight,
    scrollWidth: element.scrollWidth,
  }));

  expect(metrics.scrollWidth, `${label} should not clip horizontally`).toBeLessThanOrEqual(
    metrics.clientWidth + 1,
  );
  expect(metrics.scrollHeight, `${label} should not clip vertically`).toBeLessThanOrEqual(
    metrics.clientHeight + 1,
  );
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
  const zoomLabel = page.locator('.workspace-2d-map-controls button[aria-label="배율 초기화"]').first();
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

async function openAndAssert2DContextMenu(page: Page) {
  const qaAgentActor = page
    .locator(".workspace-2d-actor-layer")
    .getByRole("button", { name: /QA Agent 채팅/ })
    .first();
  await expect(qaAgentActor).toBeVisible();

  const viewport = page.viewportSize();
  const actorBox = await qaAgentActor.boundingBox();
  const isInsideViewport =
    viewport !== null &&
    actorBox !== null &&
    actorBox.x >= 0 &&
    actorBox.y >= 80 &&
    actorBox.x + actorBox.width <= viewport.width &&
    actorBox.y + actorBox.height <= viewport.height - 90;

  if (!isInsideViewport) {
    const engineeringRoomBtn = page.getByRole("button", { name: "엔지니어링실 구역으로 이동" });
    if (await engineeringRoomBtn.isVisible().catch(() => false)) {
      await engineeringRoomBtn.click();
    }
  }

  await qaAgentActor.click({ button: "right" });

  const chatAction = page
    .locator(".workspace-2d-context-menu")
    .getByRole("button", { name: "대화하기" });
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

function dashboardSummary(dailyTokenLimit: number | null = 200000, todayUsedTokens = 45000) {
  return {
    agentCount: 1,
    runningAgentCount: 1,
    idleAgentCount: 0,
    errorAgentCount: 0,
    taskCount: 2,
    runningTaskCount: 2,
    completedTaskCount: 0,
    failedTaskCount: 0,
    waitingApprovalTaskCount: 1,
    dailyTokenLimit,
    todayUsedTokens,
    avgExecutionDurationMs: 2400,
    recentReports: [],
    recentLogs: [],
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
