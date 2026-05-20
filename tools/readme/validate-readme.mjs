import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const readme = await readFile(resolve(root, "README.md"), "utf8");
const license = await readFile(resolve(root, "LICENSE"), "utf8");

const requiredHeadings = [
  "# 🤖 Aquila Office",
  "## 목차",
  "## 프로젝트 개요",
  "### 배경",
  "### 목표",
  "### 핵심 가치",
  "## 주요 기능",
  "## 시스템 아키텍처",
  "## 주요 프로세스 / AI 설계",
  "### 1. Workspace 중심 실행 모델",
  "### 2. Gateway 기반 Agent 실행",
  "### 3. Agent Skill file 동기화",
  "### 4. Slack과 GitHub 통합 흐름",
  "### 5. Smoke fixture 기반 검증",
  "## 기술 스택",
  "## 시작하기",
  "### 사전 요구사항",
  "### 1. 저장소 클론",
  "### 2. 환경 변수 설정",
  "### 3. Docker Compose 빠른 시작",
  "### 4. 백엔드 직접 실행",
  "### 5. 프론트엔드 직접 실행",
  "### 6. 첫 사용자와 Gateway 연결",
  "## 환경 변수",
  "### 인증",
  "### 데이터베이스",
  "### 외부 token 암호화",
  "### Vertex AI",
  "### Slack 연동",
  "### 공통",
  "## ERD",
  "## API 명세",
  "### 핵심 API 요약",
  "## 테스트 커버리지",
  "## 모니터링",
  "### 주요 metric",
  "### 로컬 smoke 점검",
  "## CI/CD",
  "## 트러블 슈팅",
  "### 1. Backend가 시작하지 않음",
  "### 2. Frontend가 API에 연결하지 못함",
  "### 3. Google login 실패",
  "### 4. Gateway agent run 실패",
  "### 5. Slack event 실패",
  "### 6. k6 smoke health 실패",
  "## 성과 및 회고",
  "### 성과",
  "### 개선 방향",
  "## 관련 링크",
];

for (const heading of requiredHeadings) {
  assert.match(readme, new RegExp(`^${escapeRegExp(heading)}$`, "m"), `missing heading: ${heading}`);
}

const requiredSnippets = [
  "front/public/aquila-office-readme-preview.png",
  "협업형 AI 에이전트 워크스페이스 운영 플랫폼",
  "업무 요청부터 AI Agent 실행, 결과 공유, 산출물 추적까지 하나의 팀 업무 흐름으로 연결합니다.",
  "https://img.shields.io/badge/License-MIT-green?style=flat-square",
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
  "http://localhost:8080/swagger-ui.html",
  "| 인증 | POST | `/api/v1/auth/google/login`",
  "| 에이전트 | GET | `/api/v1/workspaces/{workspaceId}/agents`",
  "| 작업 | POST | `/api/v1/workspaces/{workspaceId}/tasks/run`",
  "| 리포트 | GET | `/api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports`",
  "| Gateway | GET | `/api/v1/workspaces/{workspaceId}/gateway`",
  "| Presence | PATCH | `/api/v1/workspaces/{workspaceId}/presence/me/position`",
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
  "NEXT_PUBLIC_GOOGLE_CLIENT_ID",
  "Workspace",
  "WorkspaceAgent",
  "Task",
  "TaskExecution",
  "SlackChannelMapping",
  "GithubCredential",
  "완료 기준은 [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md)에 기록합니다.",
  "JaCoCo 80% line coverage gate",
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
  "## 릴리즈 기준",
  "## 연락/논의",
  "## 최신 상태 확인",
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
