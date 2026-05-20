# Aquila Office

<!-- PROJECT LOGO -->
<p align="center">
  <a href="https://github.com/AquilaXk/aquila-office">
    <img src="front/public/brand-mascot.png" alt="Aquila Office" width="96">
  </a>
</p>

<h3 align="center">Aquila Office</h3>

<p align="center">
  AI 에이전트, 작업, 산출물, 채팅, 외부 연동을 한 곳에서 운영하는 협업 워크스페이스입니다.
  <br />
  <a href="COMPLETION_CHECKLIST.md"><strong>릴리즈 증적 보기 »</strong></a>
  <br />
  <br />
  <a href="#시작하기">시작하기</a>
  ·
  <a href="#개발">개발</a>
  ·
  <a href="#통합">통합</a>
  ·
  <a href="#트러블슈팅">트러블슈팅</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F" alt="Spring Boot 4.0.5">
  <img src="https://img.shields.io/badge/Next.js-16-black" alt="Next.js 16">
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1" alt="PostgreSQL 17">
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED" alt="Docker Compose">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License">
</p>

<!-- ABOUT THE PROJECT -->

## 프로젝트 소개

<img width="100%" alt="Aquila Office 워크스페이스 화면" src="front/public/aquila-office-readme-preview.png">

# 팀을 위한 AI 협업 운영 워크스페이스

Aquila Office는 Java 21 + Spring Boot 백엔드와 Next.js 프론트엔드를 결합해 워크스페이스 멤버, AI 에이전트, 작업 실행, 산출물, 채팅, presence, 외부 통합을 관리하는 서비스입니다.

서비스의 중심 단위는 워크스페이스입니다. 각 워크스페이스는 멤버, 에이전트, 작업 실행, 리포트, 채팅 세션, Gateway 연결, Slack 매핑, GitHub credential, 생성된 산출물을 소유합니다. 백엔드는 버전이 있는 API를 제공하고, 프론트엔드는 반복적인 프로젝트 운영에 맞춘 밀도 있는 화면을 제공합니다.

완료 기준은 [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md)에 기록합니다.

### API 지도

actuator나 정적 인프라 endpoint가 아닌 백엔드 응답은 공통 `RsData` envelope를 사용합니다.

| 영역 | 메서드와 경로 | 용도 |
| --- | --- | --- |
| 상태 | `GET /api/health` | 가벼운 서비스 상태 확인 |
| 지표 | `GET /actuator/prometheus` | Prometheus scrape endpoint |
| 인증 | `POST /api/v1/auth/google/login` | Google ID token 검증 후 access/refresh token 발급 |
| 인증 | `POST /api/v1/auth/token/refresh` | refresh token 회전과 새 token pair 발급 |
| 인증 | `POST /api/v1/auth/logout` | refresh token 폐기 |
| 워크스페이스 | `GET /api/v1/workspaces` | 참여 중인 워크스페이스 목록 조회 |
| 워크스페이스 | `POST /api/v1/workspaces` | 워크스페이스 생성 |
| 워크스페이스 | `GET /api/v1/workspaces/{workspaceId}` | 워크스페이스 상세 조회 |
| 워크스페이스 | `GET /api/v1/workspaces/{workspaceId}/dashboard/summary` | dashboard count, log, report 조회 |
| 멤버 | `GET /api/v1/workspaces/{workspaceId}/members` | 워크스페이스 멤버 목록 조회 |
| 멤버 | `GET /api/v1/workspaces/{workspaceId}/members/task-stats` | 멤버별 작업 점수 요약 조회 |
| 에이전트 | `GET /api/v1/workspaces/{workspaceId}/agents` | 워크스페이스 에이전트 목록 조회 |
| 에이전트 | `POST /api/v1/workspaces/{workspaceId}/agents` | 워크스페이스 에이전트 생성과 skill file 동기화 |
| 에이전트 | `DELETE /api/v1/workspaces/{workspaceId}/agents/{agentId}` | 워크스페이스 에이전트 삭제 |
| 작업 | `GET /api/v1/workspaces/{workspaceId}/tasks` | 워크스페이스 작업 목록 조회 |
| 작업 | `POST /api/v1/workspaces/{workspaceId}/tasks/run` | 새 작업 생성과 실행 |
| 작업 | `POST /api/v1/workspaces/{workspaceId}/tasks/{taskId}/run` | 기존 작업 실행 |
| 리포트 | `GET /api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports` | 작업 실행 리포트 목록 조회 |
| 채팅 | `POST /api/v1/workspaces/{workspaceId}/chat/messages` | 워크스페이스 또는 작업 채팅 메시지 전송 |
| 산출물 | `GET /api/v1/workspaces/{workspaceId}/artifacts/tree` | 산출물 tree 조회 |
| 산출물 | `GET /api/v1/workspaces/{workspaceId}/artifacts/files?path=...` | 산출물 파일 내용 조회 |
| 통합 | `GET /api/v1/workspaces/{workspaceId}/github/credentials` | GitHub credential 목록 조회 |
| 통합 | `GET /api/v1/workspaces/{workspaceId}/slack/integrations` | Slack 통합 목록 조회 |
| 통합 | `GET /api/v1/workspaces/{workspaceId}/gateway` | Gateway binding 상태 조회 |
| Presence | `GET /api/v1/workspaces/{workspaceId}/presence/stream` | 워크스페이스 presence SSE stream 열기 |
| Presence | `PATCH /api/v1/workspaces/{workspaceId}/presence/me/position` | 멤버 위치 갱신 |

### 도메인 모델

백엔드는 도메인 로직을 Java 객체에 두고, 현재 상태는 JPA adapter를 통해 저장합니다.

| 엔티티 | 핵심 관계 |
| --- | --- |
| `Member` | Google identity와 role을 소유하며 하나의 `MemberProfile`을 가집니다. |
| `Workspace` | Google subject가 생성하며 여러 `WorkspaceMember`, `WorkspaceAgent`, `Task`, invite, integration, artifact를 가집니다. |
| `WorkspaceMember` | `Member`와 `Workspace`를 워크스페이스 role로 연결합니다. |
| `WorkspaceInvite` | 초대 email, token, expiry, accepted, revoked timestamp를 저장합니다. |
| `WorkspaceAgent` | `Workspace`에 속하며 여러 `WorkspaceAgentSkillFile`과 Gateway agent ID를 가질 수 있습니다. |
| `Task` | `Workspace`에 속하며 agent에 배정될 수 있고 requested, running, completed, failed, canceled 상태로 진행됩니다. |
| `TaskExecution` | 작업 실행 lifecycle, final text, failure reason, report data를 저장합니다. |
| `TaskExecutionLog` | `TaskExecution`의 log line을 저장합니다. |
| `ChatSession` | 워크스페이스와 작업 상호작용의 채팅 메시지를 묶습니다. |
| `ChatMessage` | role, content, task linkage를 저장합니다. |
| `WorkspaceGatewayBinding` | Gateway mode, URL, 암호화 token, 연결 상태를 저장합니다. |
| `SlackChannelMapping` | Slack team/channel을 워크스페이스에 매핑하고 bot token material을 암호화해 저장합니다. |
| `GithubCredential` | repository 작업을 위한 GitHub token material을 암호화해 저장합니다. |
| `OrchestrationSession` | multi-agent plan 실행을 추적하며 여러 `OrchestrationStep` row를 가집니다. |

## 릴리즈 기준

현재 릴리즈 증적은 [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md)에 기록합니다. 릴리즈 전에는 증적 PR merge 후 열린 issue가 없어야 하고, 최신 `main` CI workflow run이 모두 통과해야 하며, checklist에 남은 release blocker나 final-state 미체크 항목이 없어야 합니다.

### 기술 스택

- [Java 21](https://openjdk.org/projects/jdk/21/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Gradle](https://gradle.org/)
- [Next.js](https://nextjs.org/)
- [React](https://react.dev/)
- [PostgreSQL](https://www.postgresql.org/)
- [Docker Compose](https://docs.docker.com/compose/)
- [k6](https://k6.io/)
- [Google Vertex AI](https://cloud.google.com/vertex-ai)

## 연락/논의

제품, 구현, 릴리즈 작업은 이 저장소의 GitHub issue와 pull request에서 다룹니다. secret이나 환경별 credential은 공개 issue와 PR 본문에 적지 않습니다.

## 최신 상태 확인

릴리즈와 workflow run을 확인해 변경 상태를 추적합니다.

```bash
gh issue list --repo AquilaXk/aquila-office --state open --limit 50
gh run list --repo AquilaXk/aquila-office --branch main --limit 10
```

<!-- GETTING STARTED -->

## 시작하기

로컬에서 실행하려면 아래 순서로 준비합니다.

### 준비 사항

필요한 도구:

- Java 21
- Node.js 20 이상
- Docker Desktop 또는 호환 Docker engine
- Compose 없이 실행할 경우 PostgreSQL 17
- 로컬 smoke check용 k6

> 선택 통합 기능에는 별도 credential이 필요합니다. Google OAuth, Vertex AI, Gateway, Slack, GitHub 설정은 [통합](#통합)을 확인하세요.

## 개발

### 설정

1. 저장소를 clone합니다.

   ```bash
   git clone https://github.com/AquilaXk/aquila-office.git
   cd aquila-office
   ```

2. 추적되는 sample에서 로컬 환경 파일을 만듭니다.

   ```bash
   cp .env.example .env
   ```

3. private 값은 로컬에만 둡니다. `vertex-key.json`은 의도적으로 ignore되며, Docker Compose에서는 `VERTEX_AI_CREDENTIALS_HOST_PATH`로 mount할 수 있습니다.

4. 프론트엔드를 직접 실행할 때는 dependency를 설치합니다.

   ```bash
   npm --prefix front install
   ```

#### Docker Compose 빠른 시작

가장 빠른 로컬 경로는 PostgreSQL, backend, frontend, nginx entrypoint를 함께 올리는 것입니다.

```bash
docker compose --env-file .env.example -f compose.dev.yml up --build
```

기본 로컬 endpoint:

- 백엔드 API: `http://localhost:8080`
- 프론트엔드 앱: `http://localhost:3000`
- Health: `GET /api/health`
- Actuator liveness: `GET /actuator/health/liveness`
- Actuator readiness: `GET /actuator/health/readiness`
- Prometheus metrics: `GET /actuator/prometheus`

#### 개발 팁

한쪽 stack만 반복 수정할 때는 backend와 frontend를 직접 실행합니다.

```bash
./back/gradlew -p back check
./back/gradlew -p back bootRun
```

```bash
npm --prefix front run test:node
npm --prefix front run lint
npm --prefix front run build
npx --prefix front playwright install chromium
npm --prefix front run test:browser
npm --prefix front run dev
```

#### 수동 설정

Compose 없이 실행할 때는 `.env`를 먼저 설정합니다.

| 이름 | 기본값 | 용도 |
| --- | --- | --- |
| `POSTGRES_DB` | `aquila_office` | 로컬 PostgreSQL database 이름 |
| `POSTGRES_USER` | `aquila` | 로컬 PostgreSQL user |
| `POSTGRES_PASSWORD` | `aquila_dev_password` | 로컬 PostgreSQL password |
| `BACKEND_PORT` | `8080` | backend container host port |
| `FRONTEND_PORT` | `3000` | frontend container host port |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | frontend API base URL |
| `CUSTOM_CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:3000,http://localhost` | backend가 허용하는 browser origin |
| `AQUILA_AUTH_TOKEN_ISSUER` | `aquila-office` | JWT issuer |
| `AQUILA_AUTH_TOKEN_SECRET` | local development secret | JWT signing secret. 로컬 밖에서는 교체합니다. |
| `AQUILA_AUTH_ACCESS_TOKEN_TTL` | `15m` | access token lifetime |
| `AQUILA_AUTH_REFRESH_TOKEN_TTL` | `14d` | refresh token lifetime |
| `AQUILA_EXTERNAL_TOKEN_KEY` | local development key | 외부 token용 AES-GCM key. 로컬 밖에서는 교체합니다. |
| `AQUILA_EXTERNAL_TOKEN_KEY_ID` | `local` | 외부 token key identifier |
| `AQUILA_GOOGLE_OAUTH_CLIENT_IDS` | development client ID | 허용할 Google OAuth client ID |
| `AQUILA_GOOGLE_ALLOWED_HOSTED_DOMAINS` | empty | 선택 Google hosted-domain allow list |
| `AQUILA_GOOGLE_REQUIRE_EMAIL_VERIFIED` | `true` | Google email verified claim 요구 여부 |
| `GOOGLE_CLOUD_PROJECT_ID` | `grepp-ai-project` | Vertex AI project ID |
| `VERTEX_AI_LOCATION` | `us-central1` | Vertex AI region |
| `VERTEX_AI_MODEL` | `gemini-2.5-flash` | Vertex AI model |
| `GOOGLE_APPLICATION_CREDENTIALS` | `/run/secrets/vertex-key.json` | runtime credential file path |
| `VERTEX_AI_CREDENTIALS_HOST_PATH` | `./vertex-key.json` | Compose에 mount할 로컬 service account key path |
| `AQUILA_SLACK_CLIENT_ID` | empty | Slack OAuth client ID |
| `AQUILA_SLACK_CLIENT_SECRET` | empty | Slack OAuth client secret |
| `AQUILA_SLACK_SIGNING_SECRET` | empty | Slack event signature secret |
| `AQUILA_INVITE_EMAIL_ENABLED` | `false` | SMTP invite delivery 활성화 여부 |

#### 첫 사용자 설정

일반 진입점은 Google OAuth입니다. `AQUILA_GOOGLE_OAUTH_CLIENT_IDS`와 `NEXT_PUBLIC_GOOGLE_CLIENT_ID`를 설정한 뒤 frontend에서 로그인합니다. 결정적인 local service-journey check가 필요하면 backend를 `SPRING_PROFILES_ACTIVE=smoke`, `AQUILA_SMOKE_FIXTURE_ENABLED=true`로 실행합니다.

### E2E 테스트

저장소 root에서 browser regression test를 실행합니다.

```bash
npx --prefix front playwright install chromium
npm --prefix front run test:browser
```

#### 문제 해결

- browser가 없으면 `npx --prefix front playwright install chromium`을 다시 실행합니다.
- frontend가 backend에 연결하지 못하면 `NEXT_PUBLIC_API_BASE_URL`과 backend CORS 설정을 확인합니다.
- 로컬 port가 이미 사용 중이면 `.env`의 `BACKEND_PORT`, `FRONTEND_PORT`, `POSTGRES_PORT`를 조정합니다.

### 이전 버전 업그레이드

dependency 또는 schema 변경 후에는 전체 로컬 gate를 다시 실행합니다.

```bash
./back/gradlew -p back check
npm --prefix front run test:node
npm --prefix front run lint
npm --prefix front run build
npm --prefix front run test:browser
node tools/readme/validate-readme.mjs
```

## 배포

### Docker

로컬 stack build와 실행:

```bash
docker compose --env-file .env.example -f compose.dev.yml up --build
```

공유 환경에서는 `AQUILA_AUTH_TOKEN_SECRET`, `AQUILA_EXTERNAL_TOKEN_KEY`, database password, Slack secret, Google credential의 로컬 기본값을 반드시 교체합니다.

### OCI blue-green

배포 스크립트:

- `scripts/deploy/backend-blue-green.sh`는 backend image를 build하고 health 확인 뒤 traffic을 전환합니다.
- `scripts/deploy/oci-backend-hook.sh`는 OCI host hook entrypoint입니다.
- `scripts/deploy/validate-backend-cd.sh`는 배포 스크립트 syntax와 필수 파일을 검사합니다.

## 로드맵

로드맵 단위 작업은 issue로 관리합니다. 현재 릴리즈 증적과 남은 완료 기준은 [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md)에 있습니다.

## 라이선스

Aquila Office는 [MIT License](LICENSE)를 적용합니다.

## 저장소 활동

GitHub issue, PR, Actions 화면에서 활동을 확인합니다.

```bash
gh issue list --repo AquilaXk/aquila-office --state open --limit 50
gh run list --repo AquilaXk/aquila-office --branch main --limit 10
```

## 기여

1. 작업 issue를 만들거나 선택합니다.
2. `main`에서 좁은 범위의 branch를 만듭니다.
3. 집중된 commit과 로컬 검증으로 구현합니다.
4. issue를 연결한 pull request를 엽니다.
5. check 통과 후 merge하고 main branch CI run을 확인합니다.

`AGENTS.md`, `docs/`, `.codex/` 같은 로컬 에이전트 파일은 의도적으로 ignore되며 runtime 문서에 포함하지 않습니다.

### 좋은 첫 이슈

좋은 첫 이슈는 검증 명령이 명확한 작은 문서, frontend copy, test, validation 개선입니다. 첫 기여 범위로 넓은 infrastructure 변경이나 secret-handling 변경은 피합니다.

### 기여자

기여자는 PR 범위를 작게 유지하고, 관련 없는 formatting 변경을 피하며, 변경 검증에 사용한 정확한 명령을 적습니다.

## Content Security Policy 활성화

공유 환경을 노출하기 전 reverse proxy, platform, frontend middleware 계층에서 Content Security Policy를 정의합니다. Google OAuth, frontend asset, backend API call, 허용할 integration origin을 명시적으로 포함해야 합니다.

## 단일 워크스페이스 모드

Aquila Office는 현재 여러 워크스페이스를 모델링합니다. 단일 워크스페이스 배포는 숨겨진 frontend state에 의존하지 말고 application 또는 deployment 계층에서 workspace 생성, membership, routing 정책을 강제해야 합니다.

## 통합

### Google OAuth

설정 항목:

- `AQUILA_GOOGLE_OAUTH_CLIENT_IDS`
- `AQUILA_GOOGLE_ALLOWED_HOSTED_DOMAINS`
- `AQUILA_GOOGLE_REQUIRE_EMAIL_VERIFIED`
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`

### Vertex AI

설정 항목:

- `GOOGLE_CLOUD_PROJECT_ID`
- `VERTEX_AI_LOCATION`
- `VERTEX_AI_MODEL`
- `GOOGLE_APPLICATION_CREDENTIALS`
- `VERTEX_AI_CREDENTIALS_HOST_PATH`

`vertex-key.json`은 git 밖에 둡니다. credential file이 저장소 root에 없으면 `VERTEX_AI_CREDENTIALS_HOST_PATH`를 사용합니다.

### Gateway

Gateway 설정은 암호화된 token material과 workspace-level binding state를 사용합니다. agent run이 실패하면 Gateway URL, `/rpc` path, encrypted token, `/api/v1/workspaces/{workspaceId}/gateway`를 확인합니다.

### Slack

설정 항목:

- `AQUILA_SLACK_CLIENT_ID`
- `AQUILA_SLACK_CLIENT_SECRET`
- `AQUILA_SLACK_SIGNING_SECRET`

Slack event metric에는 signature failure, duplicate skip, processing failure가 포함됩니다.

### GitHub

GitHub 작업에는 워크스페이스의 `GithubCredential`과 repository 권한이 있는 token이 필요합니다. token material은 log, issue, PR 본문, metric label에 남기지 않습니다.

## 워크플로

### 로컬 smoke 점검

backend가 `8080`에서 listening한 뒤 로컬 smoke test를 실행합니다.

```bash
tools/test/run-k6-smoke.sh
```

service journey smoke는 Google login과 Gateway execution이 결정적인 fixture adapter를 사용하도록 `SPRING_PROFILES_ACTIVE=smoke`, `AQUILA_SMOKE_FIXTURE_ENABLED=true`가 필요합니다.

k6 smoke 범위:

- `GET /api/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/prometheus`
- `POST /api/v1/auth/google/login`
- `POST /api/v1/workspaces`
- `POST /api/v1/workspaces/{workspaceId}/gateway/binding`
- `POST /api/v1/workspaces/{workspaceId}/agents`
- `POST /api/v1/workspaces/{workspaceId}/tasks`
- `POST /api/v1/workspaces/{workspaceId}/tasks/{taskId}/run`
- `GET /api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports`

### 릴리즈 사전 점검

실행 명령:

```bash
node tools/readme/validate-readme.mjs
node tools/perf/validate-k6-smoke.mjs
gh issue list --repo AquilaXk/aquila-office --state open --limit 50
gh run list --repo AquilaXk/aquila-office --branch main --limit 10
```

Prometheus scrape 설정:

- Scrape 경로: `/actuator/prometheus`
- 기본 target: Docker network에서는 `backend:8080`, 로컬 실행에서는 `localhost:8080`
- 권장 alert input: `http_server_requests_seconds`, `http_req_failed`, JVM memory, HikariCP connection gauge, process CPU usage

Workflow metric 목록:

| Metric | Label | 용도 |
| --- | --- | --- |
| `aquila_task_execution_started_total` | none | 작업 실행 시작 count |
| `aquila_task_execution_completed_total` | `status=succeeded\|failed` | 작업 실행 결과 count |
| `aquila_task_execution_duration_seconds` | `status=succeeded\|failed` | 작업 실행 latency |
| `aquila_gateway_rpc_requests_total` | `method` | Gateway RPC request count |
| `aquila_gateway_rpc_completed_total` | `method`, `outcome=success\|failure\|timeout` | Gateway RPC result count |
| `aquila_gateway_rpc_latency_seconds` | `method`, `outcome=success\|failure\|timeout` | Gateway RPC latency |
| `aquila_slack_events_received_total` | `route=global\|workspace` | Slack event receive count |
| `aquila_slack_events_signature_failures_total` | `route=global\|workspace` | Slack signature verification failure |
| `aquila_slack_events_dedupe_hits_total` | none | Slack duplicate event skip |
| `aquila_slack_events_processing_failures_total` | `stage=payload_parse\|signature_config\|use_case` | Slack processing failure |

Metric label은 고정된 low-cardinality 값만 사용합니다. Workspace ID, user ID, URL, token, prompt, failure text는 label로 쓰지 않습니다.

Alert 예시:

- 작업 실패 비율: `sum(rate(aquila_task_execution_completed_total{status="failed"}[5m])) / sum(rate(aquila_task_execution_completed_total[5m])) > 0.2`
- Gateway timeout 증가: `sum(rate(aquila_gateway_rpc_completed_total{outcome="timeout"}[5m])) > 0`
- Slack signature failure: `sum(rate(aquila_slack_events_signature_failures_total[5m])) > 0`

## 감사의 말

Aquila Office는 Java, Spring, Next.js, React, PostgreSQL, Docker, k6, Google Cloud, Slack, GitHub 생태계 위에서 동작합니다.

## 트러블슈팅

| 증상 | 확인할 것 |
| --- | --- |
| Backend가 시작하지 않음 | Java 21, port `8080`, database 상태를 확인합니다. 저장소 root에서 `./back/gradlew -p back bootRun`을 실행합니다. |
| Frontend가 API에 연결하지 못함 | `NEXT_PUBLIC_API_BASE_URL`, backend CORS pattern, browser network log를 확인합니다. |
| Google login 실패 | `AQUILA_GOOGLE_OAUTH_CLIENT_IDS`, hosted-domain 설정, ID token audience가 설정된 client와 맞는지 확인합니다. |
| Vertex AI call 실패 | `GOOGLE_CLOUD_PROJECT_ID`, `VERTEX_AI_LOCATION`, `VERTEX_AI_MODEL`, `GOOGLE_APPLICATION_CREDENTIALS`를 확인합니다. credential file은 commit하지 않습니다. |
| Gateway agent run 실패 | Gateway URL, `/rpc` path, encrypted token, `/api/v1/workspaces/{workspaceId}/gateway`를 확인합니다. |
| Slack event 실패 | signing secret, OAuth callback URL, channel mapping, event deduplication state를 확인합니다. |
| GitHub 작업 실패 | 워크스페이스에 `GithubCredential`이 있고 token에 repository 권한이 있는지 확인합니다. |
| k6 smoke health 실패 | `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus`를 각각 `curl -i`로 확인합니다. |
