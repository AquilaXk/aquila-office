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
const extraArtifactDir = process.env.README_EXTRA_ARTIFACT_DIR
  ? resolve(process.env.README_EXTRA_ARTIFACT_DIR)
  : null;

const workspaceId = 101;
const taskId = 301;
const waitingTaskId = 303;
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
  dailyTokenLimit: 200000,
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
      content: "# QA Rules\n- 검증 명령과 브라우저 회귀 게이트를 반드시 실행한다.",
      syncStatus: "SYNCED",
      syncError: null,
    },
  ],
};

const orchestratorAgent = {
  ...readyAgent,
  agentId: 502,
  name: "Ops Orchestrator",
  category: "ORCHESTRATOR",
  openClawAgentId: "agent-ops",
  skillFiles: [
    {
      skillFileId: 92,
      fileName: "orchestrator.md",
      content: "# Orchestrator\n- 작업 의존 그래프에 따라 하위 스텝을 배분한다.",
      syncStatus: "SYNCED",
      syncError: null,
    },
  ],
};

const backendAgent = {
  ...readyAgent,
  agentId: 503,
  name: "Backend Architect",
  category: "BACKEND",
  openClawAgentId: "agent-backend",
  skillFiles: [
    {
      skillFileId: 93,
      fileName: "backend-service.md",
      content: "# Backend Architect\n- JPA 쿼리 최적화와 스키마 계약을 검증한다.",
      syncStatus: "SYNCED",
      syncError: null,
    },
  ],
};

const designAgent = {
  ...readyAgent,
  agentId: 504,
  name: "UI Studio Agent",
  category: "FRONTEND",
  openClawAgentId: "agent-frontend",
  skillFiles: [
    {
      skillFileId: 94,
      fileName: "ui-tokens.md",
      content: "# UI Studio\n- 시맨틱 토큰과 접근성 도구를 검수한다.",
      syncStatus: "SYNCED",
      syncError: null,
    },
  ],
};

const securityAgent = {
  ...readyAgent,
  agentId: 505,
  name: "Security Auditor",
  category: "SECURITY",
  status: "DISABLED",
  openClawAgentId: "agent-sec",
  skillFiles: [
    {
      skillFileId: 95,
      fileName: "security-audit.md",
      content: "# Security Auditor\n- Fail-Closed 경계와 토큰 암호화를 감사한다.",
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

const apiSchemaTask = {
  ...regressionTask,
  taskId: 304,
  title: "API query tuning",
  description: "JPA 복합 인덱스 및 배치 조회 최적화",
  type: "CODE_CHANGE",
  priority: "HIGH",
  assignedAgentId: 503,
  status: "IN_PROGRESS",
};

const planningSyncTask = {
  ...regressionTask,
  taskId: 305,
  title: "DAG sync review",
  description: "다중 에이전트 파이프라인 의존성 조정",
  type: "PLANNING",
  priority: "HIGH",
  assignedAgentId: 502,
  status: "IN_PROGRESS",
};

const artifactDocTask = {
  ...regressionTask,
  taskId: 306,
  title: "Open-source notices",
  description: "라이선스 고지 및 아키텍처 문서 갱신",
  type: "DOCS",
  priority: "MEDIUM",
  assignedAgentId: 504,
  status: "IN_PROGRESS",
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

const waitingApprovalTask = {
  taskId: waitingTaskId,
  workspaceId,
  createdByGoogleSub: "google-1",
  title: "Release approval check",
  description: "프로덕션 배포 전 관리자 승인 검토가 필요합니다.",
  type: "PR_REVIEW",
  priority: "URGENT",
  assignedAgentId: agentId,
  status: "WAITING_USER",
  createdAt: "2026-05-18T09:12:00Z",
  updatedAt: "2026-05-18T09:25:00Z",
};

let serverProcess = null;

await mkdir(outputDir, { recursive: true });
if (extraArtifactDir) {
  await mkdir(extraArtifactDir, { recursive: true });
}

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
        },
        stdio: ["ignore", "pipe", "pipe"],
      },
    );
    await waitForServer(serverProcess);
  }

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    baseURL: baseUrl,
    viewport: { width: 1440, height: 920 },
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
  if (extraArtifactDir) {
    await captureApprovalAndOrchestration(page);
  }
  await captureArtifactReport(page);
  await captureIntegrationSettings(page);
  if (extraArtifactDir) {
    await captureCharacterStudio(page);
  }

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
  await page.getByRole("heading", { name: /내 워크스페이스/ }).waitFor({ state: "visible", timeout: 60_000 });
  await screenshot(page, "workspace-list.png", extraArtifactDir ? "after-workspace-list-dark.png" : null);

  if (extraArtifactDir) {
    await page.getByRole("button", { name: "라이트 모드로 전환" }).click();
    await page.waitForFunction(() => document.documentElement.dataset.theme === "light");
    await stabilize(page);
    await extraScreenshot(page, "after-workspace-list-light.png");
    await page.getByRole("button", { name: "다크 모드로 전환" }).click();
    await page.waitForFunction(() => document.documentElement.dataset.theme !== "light");
  }
}

async function captureWorkspaceDashboard(page) {
  await openWorkspaceDetail(page);
  const leftSidebar = page.locator('aside[aria-label="워크스페이스 탐색"]');
  await leftSidebar.getByText("QA Agent").first().click();
  await page.locator('aside[aria-label="컨텍스트 인스펙터"]').waitFor({ state: "visible" });
  const stageBox = await page.locator(".workspace-2d-stage").first().boundingBox();
  if (stageBox) {
    const startX = stageBox.x + stageBox.width * 0.5;
    const startY = stageBox.y + stageBox.height * 0.55;
    await page.mouse.move(startX, startY);
    await page.mouse.down();
    await page.mouse.move(startX - 90, startY + 110, { steps: 6 });
    await page.mouse.up();
  }
  await stabilize(page);
  await screenshot(page, "workspace-dashboard.png", extraArtifactDir ? "after-workspace-dashboard-dark.png" : null);
  await page.screenshot({
    path: resolve(root, "front/public/aquila-office-readme-preview.png"),
    fullPage: false,
  });
  console.log("captured aquila-office-readme-preview.png");

  if (extraArtifactDir) {
    await page.getByRole("button", { name: "라이트 테마로 전환" }).click();
    await page.waitForFunction(() => document.documentElement.dataset.theme === "light");
    await stabilize(page);
    await extraScreenshot(page, "after-workspace-dashboard-light.png");
    await page.getByRole("button", { name: "다크 테마로 전환" }).click();
    await page.waitForFunction(() => document.documentElement.dataset.theme !== "light");
  }
}

async function captureTaskChat(page) {
  await openWorkspaceDetail(page);
  await page.getByRole("tab", { name: /작업 보드/ }).click();
  await page.getByRole("heading", { name: "작업 보드" }).waitFor({ state: "visible" });
  await page.getByText("Regression smoke").first().click();
  const inspector = page.locator('aside[aria-label="컨텍스트 인스펙터"]');
  await inspector.waitFor({ state: "visible" });
  await inspector.getByText("UI smoke step complete").waitFor({ state: "visible" });
  await stabilize(page);
  await screenshot(page, "task-run-chat.png", extraArtifactDir ? "after-task-run-chat.png" : null);
}

async function captureApprovalAndOrchestration(page) {
  await openWorkspaceDetail(page);
  await page.getByRole("tab", { name: /승인 대기함/ }).click();
  const approvalView = page.getByLabel("승인 대기함 뷰");
  await approvalView.getByRole("heading", { name: "승인 대기함" }).waitFor({ state: "visible" });
  await approvalView.getByText("Release approval check").waitFor({ state: "visible" });
  await stabilize(page);
  await extraScreenshot(page, "after-approval-inbox.png");

  await page.getByRole("tab", { name: "오케스트레이션" }).click();
  const orchView = page.getByLabel("오케스트레이션 뷰");
  await orchView.getByRole("heading", { name: "오케스트레이션 세션" }).waitFor({ state: "visible" });
  await orchView.getByRole("button", { name: /세션 #701/ }).first().click();
  await orchView.getByText("스키마 및 API 계약 검증").waitFor({ state: "visible" });
  await stabilize(page);
  await extraScreenshot(page, "after-orchestration-dag.png");
}

async function captureArtifactReport(page) {
  await openWorkspaceDetail(page);
  await page.getByRole("tab", { name: "산출물 및 변경" }).click();
  const artifactsView = page.getByLabel("산출물 및 변경 뷰");
  await artifactsView.getByRole("heading", { name: "산출물 및 로컬 Git 변경사항" }).waitFor({ state: "visible" });
  await artifactsView.getByRole("button", { name: /result\.md/ }).click();
  await artifactsView.getByText("브라우저 회귀 경로가 정상입니다.").waitFor({ state: "visible" });
  await stabilize(page);
  await screenshot(page, "artifact-report.png", extraArtifactDir ? "after-artifact-report.png" : null);
}

async function captureIntegrationSettings(page) {
  await openWorkspaceDetail(page);
  await page.getByRole("button", { name: "연결 설정" }).click();
  await page.getByRole("dialog", { name: "연결 설정" }).waitFor({ state: "visible" });
  await stabilize(page);
  await screenshot(page, "integration-settings.png", extraArtifactDir ? "after-integration-settings.png" : null);
}

async function captureCharacterStudio(page) {
  await page.goto("/auth/character?returnTo=/workspaces");
  await page.getByRole("heading", { name: "오피스 아바타 및 표시 이름" }).waitFor({ state: "visible" });
  await page.getByRole("button", { name: /사이버 바이저/ }).click();
  await page.getByRole("button", { name: /봄버 재킷/ }).click();
  await page.getByRole("button", { name: /스파이키/ }).click();
  await page.evaluate(() => window.scrollTo(0, 0));
  await stabilize(page);
  await extraScreenshot(page, "after-character-studio.png");
}

async function openWorkspaceDetail(page) {
  await page.goto(`/workspaces/${workspaceId}`);
  await stabilize(page);
  await page.locator(".chrome-menu-title").waitFor({ state: "visible" });
  await page.locator('section[aria-label="워크스페이스 오피스 씬"]').waitFor({ state: "visible" });
}

async function screenshot(page, filename, extraFilename = null) {
  await page.screenshot({
    path: resolve(outputDir, filename),
    fullPage: false,
  });
  console.log(`captured ${filename}`);
  if (extraFilename && extraArtifactDir) {
    await extraScreenshot(page, extraFilename);
  }
}

async function extraScreenshot(page, filename) {
  if (!extraArtifactDir) return;
  await page.screenshot({
    path: resolve(extraArtifactDir, filename),
    fullPage: false,
  });
  console.log(`captured extra ${filename}`);
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
  await page.waitForTimeout(450);
}

async function installCaptureState(context) {
  await context.addInitScript((tokens) => {
    window.localStorage.setItem("aquila-office.theme", "dark");
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
          dailyTokenLimit: 500000,
          todayUsedTokens: 120000,
        },
        {
          ...primaryWorkspace,
          workspaceId: 103,
          name: "Design Review",
          description: "제품 화면 품질 점검",
          agentCount: 1,
          runningTaskCount: 0,
          completedTaskCount: 6,
          dailyTokenLimit: null,
          todayUsedTokens: 18500,
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
      await fulfillJson(route, [readyAgent, orchestratorAgent, backendAgent, designAgent, securityAgent]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks`) {
      await fulfillJson(route, [
        regressionTask,
        waitingApprovalTask,
        apiSchemaTask,
        planningSyncTask,
        artifactDocTask,
        deployTask,
      ]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/reports`) {
      await fulfillJson(route, taskReports());
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/logs`) {
      await fulfillJson(route, [
        {
          logId: 1,
          workspaceId,
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

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/messages`) {
      await fulfillJson(route, sendChatResult().messages);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/chat/tasks/${taskId}/messages`) {
      await fulfillJson(route, sendChatResult().messages);
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

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/orchestrations`) {
      await fulfillJson(route, [orchestrationSession()]);
      return;
    }

    if (method === "GET" && path === `/api/v1/workspaces/${workspaceId}/orchestrations/701`) {
      await fulfillJson(route, orchestrationSession());
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
    agentCount: 5,
    runningAgentCount: 4,
    idleAgentCount: 0,
    errorAgentCount: 0,
    taskCount: 6,
    runningTaskCount: 4,
    completedTaskCount: 1,
    failedTaskCount: 0,
    waitingApprovalTaskCount: 1,
    todayUsedTokens: 45000,
    dailyTokenLimit: 200000,
    avgExecutionDurationMs: 14200,
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
    waitingUserTaskCount: 1,
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

function orchestrationSession() {
  return {
    sessionId: 701,
    workspaceId,
    sourceTaskId: taskId,
    status: "RUNNING",
    createdAt: "2026-05-18T09:30:00Z",
    updatedAt: "2026-05-18T09:35:00Z",
    steps: [
      {
        stepKey: "plan",
        instruction: "스키마 및 API 계약 검증",
        agentId: 502,
        taskId: 305,
        dependsOn: [],
        status: "SUCCEEDED",
      },
      {
        stepKey: "backend-api",
        instruction: "도메인 서비스 및 JPA 쿼리 최적화",
        agentId: 503,
        taskId: 304,
        dependsOn: ["plan"],
        status: "SUCCEEDED",
      },
      {
        stepKey: "ui-stage",
        instruction: "2D 오피스 스테이지 및 컴포넌트 통합",
        agentId: 504,
        taskId: 306,
        dependsOn: ["plan"],
        status: "RUNNING",
      },
      {
        stepKey: "verify",
        instruction: "브라우저 E2E 회귀 검증 실행",
        agentId: 501,
        taskId: 301,
        dependsOn: ["backend-api", "ui-stage"],
        status: "PENDING",
      },
    ],
  };
}

function presenceSnapshot() {
  return {
    type: "presence.snapshot",
    workspaceId,
    memberId: 1,
    position: { left: "44%", top: "38%" },
    status: "ACTIVE",
    members: [
      {
        memberId: 1,
        position: { left: "44%", top: "38%" },
        status: "ACTIVE",
      },
      {
        memberId: 2,
        position: { left: "60%", top: "24%" },
        status: "ACTIVE",
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

  for (const port of [3100, 3101, 3102, 3103, 3000]) {
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
