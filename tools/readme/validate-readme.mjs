import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const readme = await readFile(resolve(root, "README.md"), "utf8");
const license = await readFile(resolve(root, "LICENSE"), "utf8");

const requiredHeadings = [
  "# Aquila Office",
  "## 프로젝트 소개",
  "# 팀을 위한 AI 협업 운영 워크스페이스",
  "## 릴리즈 기준",
  "### 기술 스택",
  "## 연락/논의",
  "## 최신 상태 확인",
  "## 시작하기",
  "### 준비 사항",
  "## 개발",
  "### 설정",
  "#### Docker Compose 빠른 시작",
  "#### 개발 팁",
  "#### 수동 설정",
  "#### 첫 사용자 설정",
  "### E2E 테스트",
  "#### 문제 해결",
  "### 이전 버전 업그레이드",
  "## 배포",
  "### Docker",
  "### OCI blue-green",
  "## 로드맵",
  "## 라이선스",
  "## 저장소 활동",
  "## 기여",
  "### 좋은 첫 이슈",
  "### 기여자",
  "## Content Security Policy 활성화",
  "## 단일 워크스페이스 모드",
  "## 통합",
  "### Google OAuth",
  "### Vertex AI",
  "### Gateway",
  "### Slack",
  "### GitHub",
  "## 워크플로",
  "### 로컬 smoke 점검",
  "### 릴리즈 사전 점검",
  "## 트러블슈팅",
];

for (const heading of requiredHeadings) {
  assert.match(readme, new RegExp(`^${escapeRegExp(heading)}$`, "m"), `missing heading: ${heading}`);
}

const requiredSnippets = [
  "front/public/aquila-office-readme-preview.png",
  "AI 에이전트, 작업, 산출물, 채팅, 외부 연동을 한 곳에서 운영하는 협업 워크스페이스입니다.",
  "https://img.shields.io/badge/License-MIT-green",
  "Aquila Office는 [MIT License](LICENSE)를 적용합니다.",
  "docker compose --env-file .env.example -f compose.dev.yml up --build",
  "./back/gradlew -p back check",
  "./back/gradlew -p back bootRun",
  "npm --prefix front install",
  "npm --prefix front run test:node",
  "npm --prefix front run lint",
  "npm --prefix front run build",
  "npx --prefix front playwright install chromium",
  "npm --prefix front run test:browser",
  "tools/test/run-k6-smoke.sh",
  "gh issue list --repo AquilaXk/aquila-office --state open --limit 50",
  "gh run list --repo AquilaXk/aquila-office --branch main --limit 10",
  "GET /api/health",
  "GET /actuator/prometheus",
  "POST /api/v1/auth/google/login",
  "GET /api/v1/workspaces/{workspaceId}/agents",
  "POST /api/v1/workspaces/{workspaceId}/tasks/run",
  "GET /api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports",
  "GET /api/v1/workspaces/{workspaceId}/gateway",
  "PATCH /api/v1/workspaces/{workspaceId}/presence/me/position",
  "Java 21",
  "Spring Boot",
  "Next.js",
  "Docker Compose",
  "PostgreSQL",
  "GOOGLE_CLOUD_PROJECT_ID",
  "VERTEX_AI_LOCATION",
  "VERTEX_AI_MODEL",
  "GOOGLE_APPLICATION_CREDENTIALS",
  "AQUILA_AUTH_TOKEN_SECRET",
  "AQUILA_EXTERNAL_TOKEN_KEY",
  "AQUILA_SLACK_CLIENT_ID",
  "Workspace",
  "WorkspaceAgent",
  "Task",
  "TaskExecution",
  "SlackChannelMapping",
  "GithubCredential",
  "완료 기준은 [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md)에 기록합니다.",
];

for (const snippet of requiredSnippets) {
  assert(readme.includes(snippet), `missing README snippet: ${snippet}`);
}

const forbiddenSnippets = [
  "front/public/office-blog-preview.png",
  "front/public/office-bank-preview.png",
  "License-not%20published",
  "license file이 공개되어 있지 않습니다",
  "About the Project",
  "Getting Started",
  "Troubleshooting",
  "## 감사의 말",
  "Aquila Office는 Java, Spring, Next.js, React, PostgreSQL, Docker, k6, Google Cloud, Slack, GitHub 생태계 위에서 동작합니다.",
];

for (const snippet of forbiddenSnippets) {
  assert(!readme.includes(snippet), `forbidden README snippet remains: ${snippet}`);
}

assert.match(license, /^MIT License$/m, "LICENSE must declare MIT License");
assert.match(
  license,
  /Permission is hereby granted, free of charge/,
  "LICENSE must contain the standard MIT permission grant",
);

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
