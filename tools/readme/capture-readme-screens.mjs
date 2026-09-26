#!/usr/bin/env node

import { spawn } from "node:child_process";
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const frontRequire = createRequire(resolve(root, "front/package.json"));
const { chromium } = frontRequire("playwright");

let baseUrl = process.env.README_CAPTURE_BASE_URL ?? "http://127.0.0.1:3000";
const outputDir = resolve(root, "front/public/readme");
const workspaceId = 101;
const taskId = 301;
const agentId = 501;

const authTokens = {
  memberId: 1,
  name: "Aquila User",
  email: "aquila@example.com",
  role: "ADMIN",
  accessToken: "readme-capture-access-token",
};

const primaryWorkspace = {
  workspaceId,
  name: "Product Engineering",
  description: "서비스 운영 자동화",
  creatorGoogleSub: "google-1",
  myRole: "ADMIN",
  agentCount: 2,
  runningTaskCount: 1,
  completedTaskCount: 8,
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
};

const orchestratorAgent = {
  ...readyAgent,
  agentId: 502,
  name: "Ops Orchestrator",
  category: "ORCHESTRATOR",
  openClawAgentId: "agent-ops",
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

const deployTask = {
  ...regressionTask,
  taskId: 302,
  title: "Deploy checklist",
  description: "배포 전 체크리스트 정리",
  priority: "MEDIUM",
  assignedAgentId: 502,
  status: "COMPLETED",
};

let serverProcess = null;

await mkdir(outputDir, { recursive: true });

try {
  const capturePort = await resolveCapturePort();
  if (capturePort) {
    baseUrl = `http://127.0.0.1:${capturePort}`;
    serverProcess = spawn(
      "npm",
      ["--prefix", "front", "run", "start", "--", "--hostname", "127.0.0.1", "--port", String(capturePort)],
      {
        cwd: root,
        env: {
          ...process.env,
          NEXT_TELEMETRY_DISABLED: "1",
          NEXT_PUBLIC_DEV_ACCESS_TOKEN: "readme-capture-access-token",
        },
        stdio: ["ignore", "pipe", "pipe"],
      },
    );
    await waitForServer(serverProcess);
  }

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    baseURL: baseUrl,
    viewport: { width: 1440, height: 980 },
    deviceScaleFactor: 1,
    colorScheme: "dark",
  });

  await installCaptureState(context);
  await installMockApi(context);

  const page = await context.newPage();
  page.setDefaultTimeout(60_000);

  await captureWorkspaceList(page);
  await captureWorkspaceDashboard(page);
  await captureTaskChat(page);
  await captureArtifactReport(page);
  await captureIntegrationSettings(page);

  await browser.close();
  console.log(`README screenshots written to ${outputDir}`);
} finally {
  if (serverProcess) {
    serverProcess.kill("SIGTERM");
  }
}

async function captureWorkspaceList(page) {
  await page.goto("/workspaces");
  await stabilize(page);
  await page.getByRole("heading", { name: "WORKSPACES" }).waitFor({ state: "visible", timeout: 60_000 });
  await screenshot(page, "workspace-list.png");
}

async function captureWorkspaceDashboard(page) {
  await openWorkspaceDetail(page);
  await screenshot(page, "workspace-dashboard.png");
  await page.screenshot({
    path: resolve(root, "front/public/aquila-office-readme-preview.png"),
    fullPage: false,
  });
  console.log("captured aquila-office-readme-preview.png");
}

async function captureTaskChat(page) {
  await openWorkspaceDetail(page);
  await openTaskDashboardBoard(page);
  const taskDashboard = page.getByRole("dialog", { name: "Task dashboard" });
  await taskDashboard
    .locator('section[aria-label="selectedDashboardTask detail"]')
    .getByRole("button", { name: "Chat" })
    .click();
  const chatDialog = page.getByRole("dialog", { name: "Task chat 메시지" });
  await chatDialog.waitFor();
  await chatDialog.getByLabel("Message").fill("이번 릴리즈 위험 요소를 점검해줘");
  await chatDialog.getByRole("button", { name: "메시지 전송" }).click();
  await page.getByText("브라우저 회귀 확인을 진행합니다.").waitFor();
  const planDialog = page.getByRole("dialog", { name: "Orchestration plan artifact" });
  if (await planDialog.isVisible().catch(() => false)) {
    await planDialog.getByLabel("닫기").click();
  }
  await stabilize(page);
  await screenshot(page, "task-run-chat.png");
}

async function captureArtifactReport(page) {
  await openWorkspaceDetail(page);
  await openTaskDashboardBoard(page);
  const taskDashboard = page.getByRole("dialog", { name: "Task dashboard" });
  await taskDashboard
    .locator('section[aria-label="selectedDashboardTask detail"]')
    .getByRole("button", { name: "Report" })
    .click();
  const reportDialog = page.getByRole("dialog", { name: "Task report" });
  await reportDialog.waitFor();
  await reportDialog.getByRole("button", { name: "result.md" }).click();
  await page.getByRole("dialog", { name: "Artifact file" }).waitFor();
  await page.getByText("검증 결과").waitFor();
  await stabilize(page);
  await screenshot(page, "artifact-report.png");
}

async function captureIntegrationSettings(page) {
  await openWorkspaceDetail(page);
  await page
    .getByRole("navigation", { name: "워크스페이스 내비게이션" })
    .getByRole("button", { name: "Connection settings" })
    .click();
  await page.getByRole("dialog", { name: "Connection settings" }).waitFor();
  await stabilize(page);
  await screenshot(page, "integration-settings.png");
}

async function openWorkspaceDetail(page) {
  await page.goto(`/workspaces/${workspaceId}`);
  await stabilize(page);
  await page.getByRole("heading", { name: primaryWorkspace.name }).waitFor();
  await page.locator('section[aria-label="워크스페이스 오피스 씬"]').waitFor();
}

async function openTaskDashboardBoard(page) {
  const officeScene = page.locator('section[aria-label="워크스페이스 오피스 씬"]');
  await officeScene.getByRole("button", { name: "Tasks", exact: true }).click();
  const taskDashboard = page.getByRole("dialog", { name: "Task dashboard" });
  await taskDashboard.waitFor();
  await taskDashboard.getByRole("tab", { name: "Board" }).click();
  await taskDashboard.getByRole("button", { name: /Regression smoke/ }).click();
}

async function screenshot(page, filename) {
  await page.screenshot({
    path: resolve(outputDir, filename),
    fullPage: false,
  });
  console.log(`captured ${filename}`);
}

async function stabilize(page) {
  await page.addStyleTag({
    content: `
      *, *::before, *::after {
        animation-duration: 0s !important;
        animation-delay: 0s !important;
        transition-duration: 0s !important;
        scroll-behavior: auto !important;
      }
      nextjs-portal,
      [data-nextjs-toast],
      [data-nextjs-dialog-overlay],
      [data-nextjs-dev-tools],
      [data-nextjs-devtools] {
        display: none !important;
        opacity: 0 !important;
        visibility: hidden !important;
      }
    `,
  }).catch(() => undefined);
  await page.waitForTimeout(500);
}

async function installCaptureState(context) {
  await context.addInitScript((tokens) => {
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
  }, authTokens);
}

async function installMockApi(context) {
  await context.route("**/api/health", (route) =>
    fulfillJson(route, {
      status: "UP",
      service: "aquila-office-back",
      checkedAt: "2026-05-18T09:00:00Z",
    }),
  );

  await context.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    const path = url.pathname;

    if (method === "POST" && path === "/api/v1/auth/token/refresh") {
      await fulfillJson(route, { accessToken: "readme-capture-access-token-refreshed" });
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

    if (method === "GET" && path === "/api/v1/workspaces") {
      await fulfillJson(route, [
        primaryWorkspace,
        {
          ...primaryWorkspace,
          workspaceId: 102,
          name: "Platform Ops",
          description: "배포와 장애 대응 자동화",
          agentCount: 3,
          runningTaskCount: 2,
          completedTaskCount: 12,
        },
        {
          ...primaryWorkspace,
          workspaceId: 103,
          name: "Design Review",
          description: "제품 화면 품질 점검",
          agentCount: 1,
          runningTaskCount: 0,
          completedTaskCount: 6,
        },
      ]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}`) {
      await fulfillJson(route, primaryWorkspace);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/members`) {
      await fulfillJson(route, [
        workspaceMember,
        {
          memberId: 2,
          memberGoogleSub: "google-2",
          email: "ops@example.com",
          name: "Ops Lead",
          role: "MEMBER",
          joinedAt: "2026-05-18T09:10:00Z",
        },
      ]);
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
      await fulfillJson(route, [readyAgent, orchestratorAgent]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks`) {
      await fulfillJson(route, [regressionTask, deployTask]);
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
      await fulfillJson(route, []);
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

async function fulfillJson(route, body, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
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
    agentCount: 2,
    runningAgentCount: 2,
    idleAgentCount: 0,
    errorAgentCount: 0,
    taskCount: 2,
    runningTaskCount: 1,
    completedTaskCount: 1,
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
    orchestrationPlanId: null,
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

function artifactFile(path) {
  return {
    workspaceId,
    path,
    name: path.split("/").pop() ?? "result.md",
    contentType: "text/markdown",
    sizeBytes: 128,
    content: "# 검증 결과\n\n브라우저 회귀 경로가 정상입니다.\n\n- Task 실행 상태: RUNNING\n- Report 생성: 완료\n- Artifact 저장: reports/result.md",
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

async function isReachable(url) {
  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(1500) });
    return response.ok;
  } catch {
    return false;
  }
}

async function isAquilaApp(url) {
  try {
    const response = await fetch(`${url}/workspaces`, { signal: AbortSignal.timeout(1500) });
    return response.status !== 404;
  } catch {
    return false;
  }
}

async function resolveCapturePort() {
  if (process.env.README_CAPTURE_BASE_URL) {
    if (!(await isAquilaApp(baseUrl))) {
      throw new Error(`${baseUrl} does not look like the Aquila Office frontend.`);
    }
    return null;
  }

  if (await isAquilaApp(baseUrl)) {
    return null;
  }

  for (const port of [3000, 3100, 3101, 3102, 3103]) {
    const url = `http://127.0.0.1:${port}`;
    if (!(await isReachable(url))) {
      return port;
    }
  }

  throw new Error("No free local port found for README capture.");
}

async function waitForServer(child) {
  let output = "";
  child.stdout.on("data", (chunk) => {
    output += chunk.toString();
  });
  child.stderr.on("data", (chunk) => {
    output += chunk.toString();
  });

  for (let attempt = 0; attempt < 80; attempt += 1) {
    if (await isAquilaApp(baseUrl)) {
      return;
    }
    if (child.exitCode !== null) {
      throw new Error(`Next dev server exited early.\n${output}`);
    }
    await new Promise((resolveAttempt) => setTimeout(resolveAttempt, 500));
  }

  throw new Error(`Timed out waiting for ${baseUrl}.\n${output}`);
}
