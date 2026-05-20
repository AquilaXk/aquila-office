# 🤖 Aquila Office

> **협업형 AI 에이전트 워크스페이스 운영 플랫폼**<br>
> 업무 요청부터 AI Agent 실행, 결과 공유, 산출물 추적까지 하나의 팀 업무 흐름으로 연결합니다.

<p align="center">
  <a href="https://github.com/AquilaXk/aquila-office">
    <img src="https://img.shields.io/badge/GitHub-aquila--office-181717?style=for-the-badge&logo=github" alt="GitHub" />
  </a>
  &nbsp;
  <a href="COMPLETION_CHECKLIST.md">
    <img src="https://img.shields.io/badge/release-evidence-2563eb?style=for-the-badge" alt="Release evidence" />
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-f89820?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Boot 4.0.5" />
  <img src="https://img.shields.io/badge/Next.js-16-000000?style=flat-square&logo=next.js&logoColor=white" alt="Next.js 16" />
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL 17" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker Compose" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="MIT License" />
</p>

<img width="100%" alt="Aquila Office 워크스페이스 화면" src="front/public/aquila-office-readme-preview.png">

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [시스템 아키텍처](#시스템-아키텍처)
- [주요 프로세스 / AI 설계](#주요-프로세스--ai-설계)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [환경 변수](#환경-변수)
- [ERD](#erd)
- [API 명세](#api-명세)
- [테스트 커버리지](#테스트-커버리지)
- [모니터링](#모니터링)
- [CI/CD](#cicd)
- [트러블 슈팅](#트러블-슈팅)
- [성과 및 회고](#성과-및-회고)
- [관련 링크](#관련-링크)

---

## 프로젝트 개요

### 배경

AI Agent는 강력하지만, 팀 업무 안에서 안정적으로 운영하려면 실행 주체, 작업 상태, 결과물, 외부 연동 정보를 한 곳에서 관리해야 합니다.

| 문제 | 설명 |
| --- | --- |
| 작업 흐름 분산 | 요청, 실행, 보고, 산출물이 서로 다른 도구와 로그에 흩어짐 |
| 실행 추적 어려움 | 진행 중·완료·실패 상태를 팀원이 함께 보기 어려움 |
| 결과 재사용 부족 | Agent 결과가 Report, Artifact, Log로 구조화되지 않으면 다음 작업에 쓰기 어려움 |
| 연동 토큰 관리 위험 | GitHub, Slack, Gateway token material이 안전하게 보관되어야 함 |

### 목표

Aquila Office는 단순한 AI 호출 화면이 아니라, 업무 요청부터 실행·기록·공유까지 이어지는 협업 운영 플랫폼을 목표로 합니다.

| 구분 | AS-IS | TO-BE |
| --- | --- | --- |
| **요청 방식** | 개별 채팅, CLI, 수동 실행 | Workspace와 Slack에서 자연어 기반 업무 요청 |
| **실행 단위** | Agent 실행 로그 중심 | Task, TaskExecution, Report, Artifact로 구조화 |
| **추적 방식** | 터미널과 개별 로그 확인 | Dashboard에서 진행/완료/실패와 최근 로그 확인 |
| **연동 관리** | 수동 token 관리 | Workspace 단위 Gateway, Slack, GitHub credential 관리 |

### 핵심 가치

- **Workspace 기반 운영**: 팀 단위로 멤버, Agent, Task, 산출물, 외부 연동을 분리합니다.
- **역할 기반 Agent 관리**: WorkspaceAgent와 Skill file을 통해 Agent 역할과 실행 맥락을 유지합니다.
- **실행 추적**: TaskExecution, TaskExecutionLog, AgentReport로 요청부터 결과까지 남깁니다.
- **안전한 통합**: Google OAuth, JWT, AES-GCM 기반 외부 token 암호화, Slack 서명 검증을 사용합니다.

---

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| **Workspace 관리** | 팀 단위 워크스페이스 생성, 멤버 목록, 역할 분리, 초대 링크 발급 |
| **Agent 생성 및 Skill 동기화** | WorkspaceAgent 생성 시 역할별 skill file을 저장하고 Gateway agent와 연결 |
| **Task 실행** | 새 Task 생성과 즉시 실행, 기존 Task 재실행, 실행 상태와 보고서 조회 |
| **Dashboard 요약** | Agent 수, Task 상태, 최근 Report, 최근 Log를 한 화면에 표시 |
| **Artifact 조회** | Agent가 만든 산출물을 tree와 file 단위로 조회 |
| **Workspace Chat** | Workspace 또는 Task 맥락에서 Agent와 메시지 교환 |
| **Presence** | SSE 기반 presence stream과 멤버 위치 업데이트 |
| **Gateway 연동** | Workspace 단위 Gateway URL, encrypted token, 연결 상태 관리 |
| **Slack 연동** | Slack team/channel mapping, OAuth credential, event 처리 지표 관리 |
| **GitHub 연동** | Workspace별 GitHub credential 저장과 repository 작업 흐름 연결 |

---

## 시스템 아키텍처

```
[Client & Frontend]                 [Aquila Office Backend]
  Next.js 16                        ┌────────────────────────────────────┐
  ├── Workspace dashboard      ───► │  Spring Boot 4 / Java 21          │
  ├── Agent, Task, Artifact UI      │  ├── Auth / Workspace / Member    │
  ├── Gateway settings              │  ├── Agent / Task / Report        │
  └── Chat / Presence               │  ├── Artifact / Chat / Presence   │
                                    │  └── Integration adapters         │
                                    └────────────────────────────────────┘
                                                   │
                                    ┌──────────────┼──────────────┐
                                    ▼              ▼              ▼
                              PostgreSQL     OpenClaw Gateway   External APIs
                              JPA state      Agent runtime      Slack / GitHub / Vertex AI
```

배포 환경은 Docker Compose, Nginx, OCI VM, Terraform, blue/green 배포 스크립트를 기준으로 구성되어 있습니다. 로컬 개발에서는 `compose.dev.yml`로 PostgreSQL, backend, frontend, nginx entrypoint를 함께 올릴 수 있습니다.

---

## 주요 프로세스 / AI 설계

### 1. Workspace 중심 실행 모델

모든 실행은 Workspace를 기준으로 분리됩니다. Workspace는 멤버, Agent, Task, ChatSession, Artifact, SlackChannelMapping, GithubCredential, WorkspaceGatewayBinding을 소유합니다.

| 설계 포인트 | 내용 |
| --- | --- |
| 권한 분리 | `WorkspaceMember`가 멤버와 워크스페이스 role을 연결 |
| 실행 분리 | `Task`와 `TaskExecution`이 요청과 실행 lifecycle을 분리 |
| 결과 구조화 | `AgentReport`, `TaskExecutionLog`, Artifact tree로 결과와 로그를 저장 |
| 통합 분리 | Slack, GitHub, Gateway 설정을 Workspace 단위로 저장 |

### 2. Gateway 기반 Agent 실행

Backend가 모델 provider를 직접 호출하는 대신, Gateway binding을 통해 Agent runtime과 연결합니다. Gateway URL, `/rpc` path, encrypted token, connection status는 `WorkspaceGatewayBinding`으로 관리합니다.

```
Workspace → Gateway binding → Agent run request → TaskExecution → Report / Log / Artifact
```

### 3. Agent Skill file 동기화

Agent는 단순한 이름이 아니라 category, description, skill file을 함께 가집니다. `WorkspaceAgentSkillFile`은 Agent가 수행해야 할 역할과 실행 맥락을 파일 단위로 유지합니다.

### 4. Slack과 GitHub 통합 흐름

- Slack은 team/channel mapping, signing secret, duplicate event skip, processing failure metric을 통해 이벤트를 안전하게 처리합니다.
- GitHub credential은 encrypted token material로 저장하며, token은 log, issue, PR body, metric label에 노출하지 않습니다.

### 5. Smoke fixture 기반 검증

서비스 journey smoke는 `SPRING_PROFILES_ACTIVE=smoke`, `AQUILA_SMOKE_FIXTURE_ENABLED=true`에서 Google login과 Gateway execution을 결정적인 fixture adapter로 검증합니다.

---

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| **Frontend** | Next.js 16, React 19, TypeScript, lucide-react, React Markdown, Playwright |
| **Backend** | Java 21, Spring Boot 4.0.5, Spring Security, Spring Data JPA, Springdoc OpenAPI |
| **Database** | PostgreSQL 17, H2 test database |
| **Auth** | Google OAuth, JWT access/refresh token, hosted-domain allow list |
| **Realtime** | SSE presence stream |
| **AI / Agent** | OpenClaw Gateway, Vertex AI 설정, Agent skill file |
| **Security** | AES-GCM external token encryption, Slack HMAC signature verification |
| **Infra** | Docker Compose, Nginx, OCI VM, Terraform, blue/green deployment |
| **CI/CD** | GitHub Actions, Gradle Checkstyle, SpotBugs, JUnit 5, JaCoCo, frontend lint/build/browser test |
| **Monitoring** | Actuator, Prometheus metrics, k6 smoke |

---

## 시작하기

### 사전 요구사항

- Java 21
- Node.js 20 이상
- Docker Desktop 또는 호환 Docker engine
- PostgreSQL 17 (Compose 없이 실행할 때)
- k6 (로컬 smoke test)
- 선택 통합용 Google OAuth, Slack, GitHub, Gateway credential

### 1. 저장소 클론

```bash
git clone https://github.com/AquilaXk/aquila-office.git
cd aquila-office
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
```

`vertex-key.json`은 git에 올리지 않습니다. Compose에서는 `VERTEX_AI_CREDENTIALS_HOST_PATH`로 local service account key를 mount합니다.

### 3. Docker Compose 빠른 시작

```bash
docker compose --env-file .env.example -f compose.dev.yml up --build
```

기본 endpoint:

- 백엔드 API: `http://localhost:8080`
- 프론트엔드 앱: `http://localhost:3000`
- Health: `GET /api/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Prometheus metrics: `GET /actuator/prometheus`

### 4. 백엔드 직접 실행

```bash
./back/gradlew -p back check
./back/gradlew -p back bootRun
```

### 5. 프론트엔드 직접 실행

```bash
npm --prefix front install
npm --prefix front run test:node
npm --prefix front run lint
npm --prefix front run build
npx --prefix front playwright install chromium
npm --prefix front run test:browser
npm --prefix front run dev
```

### 6. 첫 사용자와 Gateway 연결

Google OAuth가 기본 로그인 경로입니다. `AQUILA_GOOGLE_OAUTH_CLIENT_IDS`와 `NEXT_PUBLIC_GOOGLE_CLIENT_ID`를 설정한 뒤 frontend에서 로그인합니다. Gateway 실행 검증은 workspace Gateway 설정에서 URL과 token을 등록한 뒤 `/api/v1/workspaces/{workspaceId}/gateway` 상태를 확인합니다.

---

## 환경 변수

루트 `.env` 또는 배포 환경 변수에 아래 값을 설정합니다.

### 인증

| 변수 | 설명 |
| --- | --- |
| `AQUILA_AUTH_TOKEN_ISSUER` | JWT issuer |
| `AQUILA_AUTH_TOKEN_SECRET` | JWT signing secret. 로컬 밖에서는 반드시 교체 |
| `AQUILA_AUTH_ACCESS_TOKEN_TTL` | access token lifetime |
| `AQUILA_AUTH_REFRESH_TOKEN_TTL` | refresh token lifetime |
| `AQUILA_GOOGLE_OAUTH_CLIENT_IDS` | 허용할 Google OAuth client ID |
| `AQUILA_GOOGLE_ALLOWED_HOSTED_DOMAINS` | 선택 Google hosted-domain allow list |
| `AQUILA_GOOGLE_REQUIRE_EMAIL_VERIFIED` | Google email verified claim 요구 여부 |
| `NEXT_PUBLIC_GOOGLE_CLIENT_ID` | frontend Google OAuth client ID |

### 데이터베이스

| 변수 | 설명 |
| --- | --- |
| `POSTGRES_DB` | 로컬 PostgreSQL database 이름 |
| `POSTGRES_USER` | 로컬 PostgreSQL user |
| `POSTGRES_PASSWORD` | 로컬 PostgreSQL password |
| `SPRING_DATASOURCE_URL` | backend datasource URL |
| `SPRING_DATASOURCE_USERNAME` | backend datasource user |
| `SPRING_DATASOURCE_PASSWORD` | backend datasource password |

### 외부 token 암호화

| 변수 | 설명 |
| --- | --- |
| `AQUILA_EXTERNAL_TOKEN_KEY` | 외부 token용 AES-GCM key |
| `AQUILA_EXTERNAL_TOKEN_KEY_ID` | 외부 token key identifier |

### Vertex AI

| 변수 | 설명 |
| --- | --- |
| `GOOGLE_CLOUD_PROJECT_ID` | Vertex AI project ID |
| `VERTEX_AI_LOCATION` | Vertex AI region |
| `VERTEX_AI_MODEL` | Vertex AI model |
| `GOOGLE_APPLICATION_CREDENTIALS` | runtime credential file path |
| `VERTEX_AI_CREDENTIALS_HOST_PATH` | Compose에 mount할 local key path |

### Slack 연동

| 변수 | 설명 |
| --- | --- |
| `AQUILA_SLACK_CLIENT_ID` | Slack OAuth client ID |
| `AQUILA_SLACK_CLIENT_SECRET` | Slack OAuth client secret |
| `AQUILA_SLACK_SIGNING_SECRET` | Slack event signature secret |

### 공통

| 변수 | 설명 |
| --- | --- |
| `BACKEND_PORT` | backend container host port |
| `FRONTEND_PORT` | frontend container host port |
| `NEXT_PUBLIC_API_BASE_URL` | frontend API base URL |
| `CUSTOM_CORS_ALLOWED_ORIGIN_PATTERNS` | backend가 허용하는 browser origin |
| `AQUILA_INVITE_EMAIL_ENABLED` | SMTP invite delivery 활성화 여부 |

---

## ERD

주요 테이블 관계 요약:

```
Member ──< WorkspaceMember >── Workspace ──< WorkspaceAgent ──< WorkspaceAgentSkillFile
                                      ├──< WorkspaceInvite
                                      ├──< Task ──< TaskExecution ──< TaskExecutionLog
                                      │                 └──< AgentReport / Artifact
                                      ├──< ChatSession ──< ChatMessage
                                      ├──< WorkspaceGatewayBinding
                                      ├──< SlackChannelMapping
                                      ├──< GithubCredential
                                      └──< OrchestrationSession ──< OrchestrationStep
```

| Entity | 역할 |
| --- | --- |
| `Workspace` | 팀 단위 업무 공간과 외부 연동의 소유자 |
| `WorkspaceAgent` | Workspace 안에서 역할을 가진 AI Agent |
| `Task` | 사용자 요청에서 생성되는 업무 단위 |
| `TaskExecution` | Task 실행 lifecycle과 최종 결과 저장 |
| `TaskExecutionLog` | 실행 중 발생한 log line 저장 |
| `SlackChannelMapping` | Slack team/channel과 workspace 연결 |
| `GithubCredential` | repository 작업을 위한 encrypted token material |

---

## API 명세

Swagger UI는 로컬 backend 실행 후 `http://localhost:8080/swagger-ui.html`에서 확인할 수 있습니다.

### 핵심 API 요약

| 도메인 | Method | Endpoint | 설명 | 기술 포인트 |
| --- | --- | --- | --- | --- |
| 상태 | GET | `/api/health` | 서비스 health envelope | Actuator 외 public health |
| 인증 | POST | `/api/v1/auth/google/login` | Google OAuth 로그인 | JWT access/refresh token |
| 워크스페이스 | GET | `/api/v1/workspaces` | 참여 중인 workspace 목록 | Membership |
| 워크스페이스 | POST | `/api/v1/workspaces` | workspace 생성 | Role |
| 대시보드 | GET | `/api/v1/workspaces/{workspaceId}/dashboard/summary` | Agent, Task, log, report 요약 | Dashboard |
| 에이전트 | GET | `/api/v1/workspaces/{workspaceId}/agents` | workspace agent 목록 | Skill file |
| 에이전트 | POST | `/api/v1/workspaces/{workspaceId}/agents` | agent 생성과 skill 동기화 | Gateway sync |
| 작업 | POST | `/api/v1/workspaces/{workspaceId}/tasks/run` | task 생성과 실행 | Agent execution |
| 리포트 | GET | `/api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports` | task execution report 목록 | Artifact |
| Gateway | GET | `/api/v1/workspaces/{workspaceId}/gateway` | Gateway binding 상태 | encrypted token |
| Presence | GET | `/api/v1/workspaces/{workspaceId}/presence/stream` | workspace presence stream | SSE |
| Presence | PATCH | `/api/v1/workspaces/{workspaceId}/presence/me/position` | 내 위치 갱신 | Workspace UI |

---

## 테스트 커버리지

GitHub Actions CI와 로컬 gate에서 backend, frontend, smoke, workflow contract를 검증합니다.

| 항목 | 검증 |
| --- | --- |
| Backend | `./back/gradlew -p back check` |
| Backend coverage | JaCoCo line coverage minimum 80% |
| Static analysis | Checkstyle, SpotBugs |
| Frontend unit | `npm --prefix front run test:node` |
| Frontend quality | `npm --prefix front run lint`, `npm --prefix front run build` |
| Browser regression | `npm --prefix front run test:browser` |
| README contract | `node tools/readme/validate-readme.mjs` |

---

## 모니터링

| 도구 | 역할 |
| --- | --- |
| **Actuator** | health, readiness, liveness endpoint |
| **Prometheus** | `/actuator/prometheus` metric scrape |
| **k6** | service journey smoke와 부하성 check |
| **GitHub Actions** | CI, workflow contract, 배포 전 검증 |

### 주요 metric

| Metric | Label | 용도 |
| --- | --- | --- |
| `aquila_task_execution_started_total` | none | 작업 실행 시작 count |
| `aquila_task_execution_completed_total` | `status=succeeded\|failed` | 작업 실행 결과 count |
| `aquila_task_execution_duration_seconds` | `status=succeeded\|failed` | 작업 실행 latency |
| `aquila_gateway_rpc_requests_total` | `method` | Gateway RPC request count |
| `aquila_gateway_rpc_completed_total` | `method`, `outcome=success\|failure\|timeout` | Gateway RPC result count |
| `aquila_slack_events_received_total` | `route=global\|workspace` | Slack event receive count |
| `aquila_slack_events_signature_failures_total` | `route=global\|workspace` | Slack signature verification failure |

Metric label은 고정된 low-cardinality 값만 사용합니다. Workspace ID, user ID, URL, token, prompt, failure text는 label로 쓰지 않습니다.

### 로컬 smoke 점검

```bash
tools/test/run-k6-smoke.sh
```

Smoke 범위에는 `GET /api/health`, `GET /actuator/prometheus`, Google login fixture, workspace 생성, Gateway binding, Agent 생성, Task 실행, Report 조회가 포함됩니다.

---

## CI/CD

```
Pull Request / main push
        │
        ▼
GitHub Actions CI
  ├── Backend Quality Gate
  ├── Frontend Quality Gate
  ├── Smoke Definition Gate
  ├── Workflow Contract Gate
  └── Terraform Infra Gate
        │
        ▼
Staging CD
  ├── backend/frontend image build
  ├── OCI self-hosted runner
  ├── blue/green deploy hook
  └── health 확인 후 Nginx reload
```

배포 스크립트:

- `scripts/deploy/backend-blue-green.sh`
- `scripts/deploy/frontend-blue-green.sh`
- `scripts/deploy/oci-backend-hook.sh`
- `scripts/deploy/oci-frontend-hook.sh`
- `scripts/deploy/validate-backend-cd.sh`

릴리즈 전 점검:

```bash
node tools/readme/validate-readme.mjs
node tools/perf/validate-k6-smoke.mjs
gh issue list --repo AquilaXk/aquila-office --state open --limit 50
gh run list --repo AquilaXk/aquila-office --branch main --limit 10
```

---

## 트러블 슈팅

### 1. Backend가 시작하지 않음

| | 내용 |
| --- | --- |
| **확인** | Java 21, port `8080`, database 상태 |
| **명령** | `./back/gradlew -p back bootRun` |

### 2. Frontend가 API에 연결하지 못함

| | 내용 |
| --- | --- |
| **확인** | `NEXT_PUBLIC_API_BASE_URL`, backend CORS pattern, browser network log |
| **해결** | `.env`의 API base URL과 `CUSTOM_CORS_ALLOWED_ORIGIN_PATTERNS`를 맞춤 |

### 3. Google login 실패

| | 내용 |
| --- | --- |
| **확인** | `AQUILA_GOOGLE_OAUTH_CLIENT_IDS`, hosted-domain allow list, email verified 설정 |
| **해결** | ID token audience가 backend에 설정된 client ID와 일치하도록 조정 |

### 4. Gateway agent run 실패

| | 내용 |
| --- | --- |
| **확인** | Gateway URL, `/rpc` path, encrypted token, `/api/v1/workspaces/{workspaceId}/gateway` |
| **해결** | workspace Gateway binding과 runtime connection status를 다시 확인 |

### 5. Slack event 실패

| | 내용 |
| --- | --- |
| **확인** | signing secret, OAuth callback URL, channel mapping, event deduplication state |
| **해결** | Slack App 설정과 workspace mapping을 재설정 |

### 6. k6 smoke health 실패

| | 내용 |
| --- | --- |
| **확인** | `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus` |
| **명령** | 각 endpoint를 `curl -i`로 개별 확인 |

---

## 성과 및 회고

### 성과

- Workspace 기반으로 Agent, Task, Report, Artifact, Chat, Presence, Integration을 한 화면에서 다루는 운영 흐름을 구성했습니다.
- Google OAuth, JWT, encrypted external token, Slack signature verification으로 인증과 외부 credential 보호를 분리했습니다.
- JaCoCo 80% line coverage gate, Checkstyle, SpotBugs, frontend lint/build/browser regression, smoke definition gate를 CI에 연결했습니다.
- 완료 기준은 [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md)에 기록합니다.

### 개선 방향

| 현재 | 발전 방향 |
| --- | --- |
| Slack/GitHub/Gateway 중심 통합 | Discord 등 추가 협업 채널 연동 |
| Workspace 단위 수동 credential 등록 | GitHub App, OAuth 기반 credential onboarding 강화 |
| Gateway run 실패 시 수동 확인 | 실패 유형별 자동 재시도와 사용자 안내 강화 |
| 운영 metric 기본 제공 | dashboard와 alert rule을 더 촘촘하게 연결 |

---

## 관련 링크

Aquila Office는 [MIT License](LICENSE)를 적용합니다.

| 구분 | URL |
| --- | --- |
| GitHub | [Aquila Office](https://github.com/AquilaXk/aquila-office) |
| 릴리즈 증적 | [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md) |
| 라이선스 | [MIT License](LICENSE) |
| 로컬 Swagger UI | `http://localhost:8080/swagger-ui.html` |
| 로컬 API health | `http://localhost:8080/api/health` |
| Prometheus metrics | `http://localhost:8080/actuator/prometheus` |

---

> Aquila Office는 팀 업무 흐름 안에서 AI Agent 실행을 운영·추적하기 위한 프로젝트입니다.
