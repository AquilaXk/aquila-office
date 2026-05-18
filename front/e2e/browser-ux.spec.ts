import { expect, test, type Locator, type Page, type Route } from "@playwright/test";

const workspaceId = 101;
const taskId = 301;
const agentId = 501;
const inviteToken = "team-token";

type WorkspaceListFixture = "default" | "empty" | "error";

type MockApiOptions = {
  workspaceList?: WorkspaceListFixture;
  workspaceListGate?: Promise<void>;
};

const authTokens = {
  memberId: 1,
  name: "Aquila User",
  email: "aquila@example.com",
  role: "ADMIN",
  accessToken: "e2e-access-token",
  refreshToken: "e2e-refresh-token",
} as const;

const primaryWorkspace = {
  workspaceId,
  name: "Product Engineering",
  description: "서비스 운영 자동화",
  creatorGoogleSub: "google-1",
  myRole: "ADMIN",
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

    await page.goto("/workspaces");

    await expect(page.getByText("목록 확인 중")).toBeVisible();
    gate.resolve();
    await expect(page.getByText("가입된 워크스페이스가 없습니다")).toBeVisible();
  });

  test("워크스페이스 목록 오류 상태를 표시한다", async ({ page }) => {
    await installMockApi(page, { workspaceList: "error" });

    await page.goto("/workspaces");

    await expect(page.getByText("목록을 불러오지 못했습니다")).toBeVisible();
    await expect(page.getByText("워크스페이스 fixture 오류").first()).toBeVisible();
  });

  test("title screen에서 워크스페이스 목록으로 이동하고 워크스페이스를 생성한다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);

    await page.goto("/");
    await expect(page.getByRole("menuitem", { name: /CONTINUE/ })).toBeVisible();
    await page.keyboard.press("ArrowDown");
    await page.keyboard.press("Enter");
    await page.waitForURL((url) => url.pathname === "/workspaces");

    await page.getByRole("link", { name: "새 Workspace" }).click();
    await page.getByPlaceholder("예: Product Engineering").fill("Platform Ops");
    await page.getByPlaceholder("워크스페이스 목적이나 팀 설명을 입력하세요.").fill("운영 자동화 점검");
    await page.getByRole("button", { name: "생성" }).click();

    await expect(page.getByText("Platform Ops 워크스페이스가 생성되었습니다.")).toBeVisible();
    await expect(page.getByRole("link", { name: "상세 보기" })).toHaveAttribute("href", `/workspaces/${workspaceId}`);
  });

  test("캐릭터 프로필 설정에서 avatar와 색상 저장 흐름을 검증한다", async ({ page }) => {
    await installMockApi(page);
    await seedAuthTokens(page);
    await seedCharacterProfile(page);

    await page.goto("/auth/character?returnTo=/workspaces");
    await expect(page.getByRole("heading", { name: "캐릭터 프로필" })).toBeVisible();
    await expect(page.getByLabel("표시 이름")).toHaveValue("Aquila User");
    await expect(page.getByRole("radio", { name: /Operator/ })).toHaveAttribute("aria-checked", "true");

    await page.getByRole("radio", { name: /Builder/ }).click();
    await page.getByRole("radio", { name: /Custom/ }).click();
    await page.getByLabel("상의 색상").fill("#1f7a5f");
    await page.getByLabel("표시 이름").fill("Aquila Pilot");
    await expect(page.getByLabel("표시 이름")).toHaveValue("Aquila Pilot");
    await expect(page.getByRole("radio", { name: /Builder/ })).toHaveAttribute("aria-checked", "true");
    await expect(page.getByRole("radio", { name: /Custom/ })).toHaveAttribute("aria-checked", "true");
    const profileUpdateRequest = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === "PATCH" && url.pathname === "/api/v1/members/me/profile";
    });
    await page.getByRole("button", { name: "프로필 저장" }).click();
    const profileUpdateBody = (await profileUpdateRequest).postData() ?? "";
    expect(profileUpdateBody).toContain("Aquila Pilot");
    expect(profileUpdateBody).toContain("builder");

    await page.waitForURL((url) => url.pathname === "/workspaces");
    const storedCharacter = await page.evaluate(() => window.localStorage.getItem("ai-office.character"));
    expect(storedCharacter).toContain("Aquila Pilot");
    expect(storedCharacter).toContain("builder");
    expect(storedCharacter).toContain("#1f7a5f");
  });

  test("워크스페이스 상세, Task 모달, artifact, 2D canvas 상호작용을 검증한다", async ({ page }) => {
    await installMockApi(page);

    await page.goto(`/workspaces/${workspaceId}`);

    await expect(page.getByRole("heading", { name: primaryWorkspace.name })).toBeVisible();
    const officeScene = page.locator('section[aria-label="워크스페이스 오피스 씬"]');
    await expect(officeScene).toBeVisible();
    await expect(officeScene.getByRole("link", { name: "Connections", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("button", { name: "Tasks", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("button", { name: "Hire agent", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("button", { name: "Files", exact: true })).toBeVisible();
    await expect(officeScene.getByRole("link", { name: "Leave workspace", exact: true })).toBeVisible();
    await expect(page.getByRole("heading", { name: "운영 요약" })).toBeVisible();
    await expect(page.getByText("Regression smoke")).toBeVisible();

    const canvasFrame = page.getByRole("img", { name: "Agent and member 2D workspace scene" });
    await expect(canvasFrame).toBeVisible();
    await expect(page.locator(".workspace-2d-hud")).toBeVisible();
    await expect(page.locator(".workspace-2d-minimap")).toBeVisible();
    await expect(page.locator(".workspace-2d-dialogue")).toBeVisible();
    const canvas = page.locator(".workspace-2d-canvas canvas");
    await expect(canvas).toBeVisible();
    await expectCanvasNotBlank(canvas);
    await expectKeyboardToMoveViewport(page, canvasFrame);
    await expectPanAndZoomToMoveViewport(page, canvasFrame);
    await openAndAssert2DContextMenu(page, canvasFrame);

    const actorPanel = page.locator('aside[aria-label="Agent 대화와 Task 대시보드"]');
    await expect(actorPanel).toBeVisible();
    await actorPanel.getByLabel("Agent 메시지").fill("Agent 상태 요약");
    await actorPanel.getByRole("button", { name: "Agent 메시지 전송" }).click();
    await expect(actorPanel.getByText("브라우저 회귀 확인을 진행합니다.")).toBeVisible();
    await actorPanel.getByRole("tab", { name: "Tasks" }).click();
    await expect(actorPanel.getByText(regressionTask.title)).toBeVisible();

    await officeScene.locator(".workspace-office-nav").getByRole("button", { name: "Tasks", exact: true }).click();
    const taskDashboard = page.getByRole("dialog", { name: "Task dashboard" });
    await expect(taskDashboard).toBeVisible();
    await expect(taskDashboard.getByText("API summary")).toBeVisible();
    await expect(taskDashboard.getByText("Local fallback")).toBeVisible();
    await taskDashboard.getByRole("tab", { name: "Board" }).click();
    await taskDashboard.getByRole("button", { name: /Regression smoke/ }).click();
    await expect(taskDashboard.getByText(`Task #${taskId}`)).toBeVisible();
    await closeTopDialog(page);

    const taskCard = page.locator(".workspace-task-card").filter({ hasText: regressionTask.title });
    await taskCard.getByRole("button", { name: "Report" }).click();
    await expect(page.getByRole("dialog", { name: "Task report" })).toBeVisible();
    await expect(page.getByText("Report #701")).toBeVisible();
    await page.getByRole("button", { name: "result.md" }).click();
    await expect(page.getByRole("dialog", { name: "Artifact file" })).toBeVisible();
    await expect(page.getByText("검증 결과")).toBeVisible();
    await closeTopDialog(page);
    await closeTopDialog(page);

    await taskCard.getByRole("button", { name: "Chat" }).click();
    await expect(page.getByRole("dialog", { name: "Task chat 메시지" })).toBeVisible();
    await expect(page.getByText("초기 점검을 시작합니다.")).toBeVisible();
    await page.getByLabel("Message").fill("브라우저 회귀 확인");
    await page.getByRole("button", { name: "메시지 전송" }).click();
    await expect(page.getByText(`Task #${taskId} chat 메시지가 전송되었습니다.`)).toBeVisible();
  });

  test("Gateway, 통합 설정, 초대 화면을 검증한다", async ({ page }) => {
    await installMockApi(page);

    await page.goto("/workspaces/gateway");
    await expect(page.getByRole("heading", { name: "Gateway 관리" })).toBeVisible();
    await page.getByLabel("Gateway URL").fill("https://gateway.example.test");
    await page.getByLabel("Token").fill("gateway-secret");
    await page.getByRole("button", { name: "Gateway 연결 테스트" }).click();
    await expect(
      page.locator('section[aria-label="선택된 Gateway 상태"]').getByText("연결 가능").first(),
    ).toBeVisible();
    await page.getByRole("button", { name: "Gateway binding 등록" }).click();
    await expect(page.getByText("Gateway binding 등록이 완료되었습니다.")).toBeVisible();

    await page.goto(`/workspaces/${workspaceId}/settings`);
    await expect(page.locator('section[aria-label="워크스페이스 설정"]')).toBeVisible();
    await expect(page.locator('section[aria-label="Slack 연동 설정"]')).toBeVisible();
    await expect(page.locator('section[aria-label="Slack 채널 매핑 설정"]')).toBeVisible();
    await expect(page.locator('section[aria-label="GitHub 자격 증명 관리"]')).toBeVisible();
    const settingsGateway = page.locator('section[aria-label="Gateway 설정"]');
    await settingsGateway.getByLabel("Gateway URL").fill("https://gateway.example.test");
    await settingsGateway.getByLabel("Token").fill("gateway-secret");
    await settingsGateway.getByRole("button", { name: "연결 테스트" }).click();
    await expect(settingsGateway.getByText("연결 가능").first()).toBeVisible();

    await page.goto(`/invites/${inviteToken}`);
    await expect(page.getByRole("heading", { name: "워크스페이스 초대" })).toBeVisible();
    await expect(page.getByText("invitee@example.com")).toBeVisible();
    await page.getByRole("button", { name: "초대 수락" }).click();
    await expect(page.getByText("google-1 계정이 MEMBER 역할로 참여했습니다.")).toBeVisible();
  });
});

async function installMockApi(page: Page, options: MockApiOptions = {}) {
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
        refreshToken: "e2e-refresh-token-refreshed",
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
      await fulfillJson(route, options.workspaceList === "empty" ? [] : [primaryWorkspace]);
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
      await fulfillJson(route, primaryWorkspace);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/members`) {
      await fulfillJson(route, [workspaceMember]);
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
      await fulfillJson(route, artifactTree());
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/artifacts/files`) {
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
      await fulfillJson(route, workspaceInvite());
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

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/slack/channel-mappings`) {
      await fulfillJson(route, [slackChannelMapping()]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/github/credentials`) {
      await fulfillJson(route, [githubCredential()]);
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

async function seedAuthTokens(page: Page) {
  await page.addInitScript((tokens) => {
    window.localStorage.setItem("aquila-office.auth", JSON.stringify(tokens));
  }, authTokens);
}

async function seedCharacterProfile(page: Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem(
      "ai-office.character",
      JSON.stringify({
        displayName: "Local Builder",
        avatarKind: "builder",
        colorMode: "custom",
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

async function expectKeyboardToMoveViewport(page: Page, frame: Locator) {
  const statusLabel = page.locator(".workspace-2d-status span").first();
  const initialStatus = await statusLabel.textContent();

  await frame.focus();
  await page.keyboard.press("ArrowRight");
  await expect.poll(() => statusLabel.textContent()).not.toBe(initialStatus);
}

async function openAndAssert2DContextMenu(page: Page, frame: Locator) {
  await frame.click({ button: "right", position: { x: 240, y: 180 } });
  const focusAction = page.getByRole("button", { name: "Focus" }).first();
  if (!(await focusAction.isVisible().catch(() => false))) {
    await page.getByRole("button", { name: /QA Agent/ }).click({ button: "right" });
  }
  await expect(focusAction).toBeVisible();
  await expect(page.getByRole("button", { name: "Profile" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Chat" }).first()).toBeVisible();
}

async function expectCanvasNotBlank(canvas: Locator) {
  await expect
    .poll(async () =>
      canvas.evaluate(async (element) => {
        const source = element as HTMLCanvasElement;
        if (source.width === 0 || source.height === 0) {
          return false;
        }

        const image = new Image();
        const dataUrl = source.toDataURL("image/png");
        await new Promise<void>((resolve, reject) => {
          image.onload = () => resolve();
          image.onerror = () => reject(new Error("canvas snapshot decode failed"));
          image.src = dataUrl;
        });

        const scratch = document.createElement("canvas");
        scratch.width = source.width;
        scratch.height = source.height;
        const context = scratch.getContext("2d", { willReadFrequently: true });
        if (!context) {
          return false;
        }

        context.drawImage(image, 0, 0);
        const pixels = context.getImageData(0, 0, scratch.width, scratch.height).data;
        const step = 4 * 53;
        let firstColor: string | null = null;
        let visibleSamples = 0;
        let variedSamples = 0;

        for (let index = 0; index < pixels.length; index += step) {
          const color = `${pixels[index]},${pixels[index + 1]},${pixels[index + 2]},${pixels[index + 3]}`;
          if (pixels[index + 3] > 0) {
            visibleSamples += 1;
          }
          if (firstColor === null) {
            firstColor = color;
          } else if (color !== firstColor) {
            variedSamples += 1;
          }
          if (visibleSamples > 25 && variedSamples > 25) {
            return true;
          }
        }

        return false;
      }),
    )
    .toBe(true);
}

function memberProfile() {
  return {
    memberId: 1,
    displayName: "Aquila User",
    avatarKind: "operator",
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
    token: inviteToken,
    email: "invitee@example.com",
    role: "MEMBER",
    status: "PENDING",
    expiresAt: "2026-05-19T09:00:00Z",
    createdAt: "2026-05-18T09:00:00Z",
  };
}

function workspaceInviteManagement() {
  return {
    ...workspaceInvite(),
    createdByGoogleSub: "google-1",
  };
}

function gatewayStatus() {
  return {
    status: "ACTIVE",
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
