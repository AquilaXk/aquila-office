# Aquila Office Completion Checklist

이 문서는 Aquila Office가 기능, 화면, 운영 관점에서 100% 완료되었는지 판단하는 단일 공개 checklist다. 각 PR은 이 문서의 해당 항목을 닫는 단위로 진행하며, 완료 판정은 코드 병합보다 검증 결과를 우선한다.

## 100% 완료 기준

Aquila Office는 다음 조건을 모두 만족할 때 100% 완료로 본다.

- Backend API가 인증, workspace, member, invite, agent, task, chat, artifact, Slack, GitHub, Gateway 흐름을 실제 서비스 시나리오대로 제공한다.
- Frontend UI UX 구조가 workspace 중심 업무 흐름을 끊기지 않게 제공하고, 서비스명은 Aquila Office로 통일한다.
- Favicon, 대표 이미지, brand preview asset은 Aquila 계열 asset을 사용하며 깨진 이미지나 기본 placeholder가 남지 않는다.
- 운영 환경에서 build, test, coverage, static analysis, container, health check, deployment script가 같은 절차로 재현된다.
- CI와 main branch 검증이 모두 통과하고, 실패 시 원인 수정 후 다시 검증한다.
- README, env sample, API/운영 설명이 실제 실행 절차와 어긋나지 않는다.

## 완료 판정 원칙

- PR은 하나의 검증 가능한 문제만 해결한다.
- 계획된 commit_plan과 실제 commit 수는 1:1로 맞춘다.
- 테스트가 필요한 기능은 실패하는 검증을 먼저 확인한 뒤 구현한다.
- 문서 PR도 command 기반 검증과 사람이 읽는 review 기준을 모두 통과해야 한다.
- 완료는 PR merge 후 main CI까지 통과한 상태를 뜻한다.

## 출시 차단 조건

아래 항목 중 하나라도 남아 있으면 100% 완료로 보지 않는다.

- 인증 token, 외부 연동 token, secret이 평문으로 저장되거나 log에 노출된다.
- Gateway, Slack, GitHub, SMTP 같은 외부 연동 실패가 사용자에게 불명확하게 표시된다.
- workspace 동시 실행이나 artifact path에서 충돌, 경로 탈출, 권한 우회 가능성이 있다.
- 주요 화면에서 서비스명, favicon, 대표 이미지, tone, spacing이 서로 다르다.
- 로컬 실행 절차와 CI/CD 절차가 문서와 다르다.

## Backend API checklist

Backend는 실제 업무 흐름을 HTTP API, domain rule, persistence, 외부 연동 adapter까지 닫아야 완료로 본다.

### 공통 API 품질

- 모든 공개 API가 `/api/v1` path, 인증 요구 여부, request/response schema, error response를 일관되게 유지한다.
- validation 실패, 인증 실패, 권한 실패, 리소스 없음, 외부 연동 실패가 구분 가능한 error code로 반환된다.
- controller test는 정상 요청, validation 실패, 인증 실패를 최소 1개 이상 포함한다.
- application service test는 domain rule, port 호출, 실패 상태를 검증한다.
- persistence adapter test는 저장, 조회, 갱신, 삭제 또는 soft delete 흐름을 검증한다.
- OpenAPI 문서는 실제 API path와 schema 변경을 따라간다.

### 인증과 보안

- JWT access token은 서명, 만료, subject, role claim을 검증한다.
- refresh token은 저장, 회전, 만료, 폐기, logout 삭제 흐름을 제공한다.
- Google login은 ID token verifier를 통과한 profile만 member로 초기화한다.
- GitHub, Slack, Gateway token은 암호화 저장하고 응답과 log에는 masking한다.
- CORS, bearer token resolver, security filter chain은 local/dev/prod profile 차이를 문서화한다.
- 보안 예외 path는 health, auth callback처럼 필요한 endpoint로만 제한한다.

### Workspace와 member

- workspace 생성, 목록, 상세, 수정, 삭제가 member 권한과 함께 검증된다.
- invite 생성, 조회, 연장, 취소, 수락이 token 만료와 중복 수락을 처리한다.
- member role 변경, 추방, self leave가 owner 보호 rule을 지킨다.
- member profile과 avatar color 변경은 인증된 사용자 범위 안에서만 허용된다.
- presence stream과 위치 업데이트는 workspace member에게만 열리고 종료 cleanup을 수행한다.

### Agent, task, chat, orchestration

- agent 생성, 삭제, 목록, skill file sync가 workspace 권한과 agent 상태를 검증한다.
- 역할별 template과 AGENTS.md 생성 결과가 저장되고 조회 가능하다.
- task 생성, 실행, 상태 변경, report 저장이 execution lifecycle을 보존한다.
- chat message는 session, intent, task trigger, agent 응답을 추적 가능하게 저장한다.
- multi agent orchestration은 plan, step, dependsOn, worker dispatch, 실패 전파를 검증한다.
- workspace path lock은 동시 실행을 차단하고 실패 시 unlock cleanup을 수행한다.

### Artifact와 file 안전성

- artifact tree, file content, report, plan file 조회가 workspace와 task 권한을 확인한다.
- artifact path는 정규화 후 workspace root 밖 접근을 차단한다.
- text, markdown, JSON, log file은 content type과 size limit을 지킨다.
- 없는 file, directory 요청, binary preview 제한이 명확한 error로 반환된다.

### 외부 연동

- Gateway RPC client는 WebSocket 연결, pending request, timeout, URL normalization을 검증한다.
- Gateway device 인증은 challenge/connect handshake와 device identity 저장을 완료한다.
- Slack event는 signature, timestamp, dedupe, channel mapping, mention parsing, thread reply를 처리한다.
- Slack OAuth는 install URL, callback, state 검증, bot token 저장/삭제를 제공한다.
- GitHub 작업 adapter는 credential 확인, repository 작업 준비, clone/push/PR 인터페이스를 분리한다.
- 초대 email은 transaction commit 이후 발송하고 실패 기록을 남긴다.

## Frontend UI UX checklist

Frontend는 화면 구조, 정보 밀도, asset, interaction, API client contract가 실제 업무 흐름과 맞을 때 완료로 본다.

### Branding과 asset

- 서비스명은 navigation, auth, dashboard, settings, metadata에서 Aquila Office로 통일한다.
- favicon, apple touch icon, Open Graph 이미지, 대표 preview 이미지는 Aquila 계열 asset을 사용한다.
- `aquila-bank`, `aquila-blog`에서 쓰는 gothic sci-fi tone을 Aquila Office에 맞게 적용한다.
- Warhammer 40k st visual direction은 과한 장식보다 묵직한 contrast, metallic border, command panel density, 붉은 accent로 표현한다.
- 깨진 이미지, 임시 logo, generic placeholder, 빈 alt text가 남지 않는다.
- brand color token과 shadow, border, focus ring은 공통 theme에서만 확장한다.

### 공통 UI system

- `components/ui`는 button, input, textarea, select, tabs, modal, toast, badge, skeleton, tooltip을 일관된 API로 제공한다.
- `components/arcade` 또는 domain component는 workspace, agent, task, artifact 같은 업무 대상의 반복 UI를 재사용한다.
- icon button은 의미가 명확한 lucide icon과 tooltip을 제공한다.
- form은 loading, disabled, validation, API error 상태를 모두 표현한다.
- table, list, panel은 빈 상태, loading 상태, error 상태, pagination 또는 overflow를 처리한다.
- mobile viewport에서 버튼 text와 주요 label이 잘리지 않는다.

### Route와 화면 구조

- auth 화면은 Google login, token session, logout 흐름과 연결된다.
- workspace 목록, 생성, 상세, 설정, 초대 수락 route가 서로 이동 가능한 구조를 가진다.
- workspace 상세는 dashboard, agents, tasks, chat, artifacts, settings 진입점을 한 화면 흐름 안에 둔다.
- gateway route는 연결 상태, binding 생성/수정, test connection, 실패 메시지를 제공한다.
- integration route는 Slack, GitHub, Gateway 설정과 success/fail callback 화면을 제공한다.
- member profile, avatar, role, invite 관리 화면은 권한에 따라 행동 가능 여부를 표시한다.

### 2D workspace experience

- Phaser scene은 agent/member avatar, workspace layer, pan/zoom, context menu를 제공한다.
- EventBus는 React state와 scene event를 단방향으로 동기화한다.
- agent와 member 상태는 idle/running/error/offline처럼 구분 가능한 색과 icon을 가진다.
- scene은 빈 workspace, 많은 avatar, 작은 viewport에서 layout shift 없이 동작한다.
- visual check는 desktop과 mobile viewport에서 canvas가 비지 않고 interaction이 가능한지 확인한다.

### Task, chat, artifact interaction

- task 생성, 즉시 실행, 상태 변경, report 조회가 frontend client와 화면에서 같은 contract를 사용한다.
- chat은 workspace session, message list, agent 응답, task 생성 응답을 구분해서 보여준다.
- task log와 report는 성공, 실패, 진행 중, 취소 상태를 분명히 표시한다.
- artifact modal은 tree 탐색, markdown rendering, plan/report preview, 파일 error 상태를 처리한다.
- orchestration plan modal은 step, dependency, worker status, 실패 원인을 스캔 가능하게 보여준다.

### Realtime과 접근성

- SSE presence stream은 연결, 재연결, 종료 상태를 사용자에게 노출한다.
- 위치 업데이트는 낙관적 UI와 실패 복구를 함께 처리한다.
- keyboard focus, aria label, semantic heading, modal focus trap을 유지한다.
- 주요 action은 mouse 없이 keyboard로 실행 가능하다.
- color만으로 상태를 구분하지 않고 icon 또는 text label을 함께 제공한다.

## 운영 검증 checklist

운영 준비는 local 실행, CI, container, deployment, 관측, 문서가 같은 결과를 낼 때 완료로 본다.

### 품질 gate

- Backend `check`는 test, static analysis, coverage report를 포함한다.
- coverage gate는 전체 coverage와 핵심 service/controller coverage를 모두 확인한다.
- Frontend는 lint, node test, build, visual smoke check를 통과한다.
- PR CI는 backend, frontend, docs 변경을 각각 필요한 job으로 검증한다.
- main CI는 merge commit 기준으로 다시 통과해야 한다.

### 환경과 배포

- `.env.example`, `back/.env.example`, `front/.env.example`은 필수 key, 기본값, 비밀값 처리 방식을 설명한다.
- Dockerfile은 non-root runtime, health check, deterministic build를 만족한다.
- dev compose는 local DB, backend, frontend, gateway mock 또는 연동 대상을 명확히 제공한다.
- prod compose와 nginx 설정은 TLS termination, reverse proxy, static asset cache, API timeout을 다룬다.
- blue/green script는 현재 slot 확인, 새 slot health check, traffic switch, rollback path를 제공한다.
- OCI deployment hook은 secret 주입, image pull, container restart, health polling을 분리한다.

### 관측과 장애 대응

- actuator health, readiness, metrics endpoint가 profile별로 의도대로 열린다.
- Prometheus registry는 API latency, error count, task execution, gateway RPC, Slack event 지표를 노출한다.
- log는 request id, workspace id, task id 같은 추적 정보를 포함하되 secret을 masking한다.
- k6 smoke는 login, workspace, agent, task, report 흐름을 낮은 부하로 반복한다.
- 운영 문서는 장애 증상, 확인 명령, rollback 절차, known limitation을 포함한다.

### 문서와 release

- README는 설치, 실행, test, build, 주요 URL을 최신 상태로 유지한다.
- API 설명은 실제 controller path와 request/response 이름을 따라간다.
- ERD 또는 domain model 설명은 workspace, member, task, execution, artifact, integration 관계를 보여준다.
- 발표용 README는 서비스 가치, 주요 화면, 기술 선택, 검증 결과를 간결하게 설명한다.
- release 전에는 open issue, failed CI, unchecked checklist가 없는지 확인한다.

## 28단계 완료 추적

| 순서 | PR 제목 | 완료 기준 | 검증 |
| --- | --- | --- | --- |
| 1 | `[Docs] 완성도 체크리스트 정리` | 100% 완료 기준, API/화면/운영 checklist 고정 | 문서 리뷰 |
| 2 | `[Build] 백엔드 품질 게이트 확장` | Checkstyle, SpotBugs, JaCoCo, coverage gate, test report 구성 | `./back/gradlew -p back check` |
| 3 | `[Build] 운영 환경 구성 추가` | Dockerfile, dev/prod compose, nginx 설정, env sample 정리 | Docker build, health check |
| 4 | `[CI] 백엔드 배포 워크플로 추가` | blue/green 배포 script, CD workflow, OCI 배포 hook 작성 | workflow dry-run 수준 검증 |
| 5 | `[Feat] JWT 인증 구현 고도화` | JWT 발급/검증, refresh token 만료/폐기, 보안 예외 정리 | auth controller/service tests |
| 6 | `[Feat] Google 로그인 검증 구현` | Google ID token verifier, OAuth 설정, profile 초기화 | auth integration tests |
| 7 | `[Feat] 외부 토큰 암호화 저장` | GitHub/Slack/Gateway token 암호화, key 설정, masking | persistence/security tests |
| 8 | `[Feat] Gateway RPC 연결 구현` | WebSocket RPC client, pending request, timeout, URL normalization | gateway client tests |
| 9 | `[Feat] Gateway 장치 인증 구현` | challenge/connect handshake, device identity 저장, 연결 상태 판정 | gateway auth tests |
| 10 | `[Feat] Agent 템플릿 동기화 구현` | 역할별 template, AGENTS.md 생성, skill file sync | agent service tests |
| 11 | `[Feat] Agent 생성 실행 연동` | Agent create/delete/list를 Gateway RPC와 연결 | agent controller/service tests |
| 12 | `[Feat] Task 실행 코어 완성` | task run, execution lifecycle, log/report 저장, 실패 상태 처리 | task/execution tests |
| 13 | `[Feat] Chat 실행 연동 완성` | chat intent parsing, agent chat, task 생성 응답 저장 | chat service tests |
| 14 | `[Feat] Multi Agent 조율 구현` | orchestration session/plan/step, dependsOn, worker dispatch | orchestrator tests |
| 15 | `[Feat] 작업 충돌 방지 구현` | workspace path lock, 동시 실행 방지, unlock/failure cleanup | concurrency tests |
| 16 | `[Feat] 산출물 저장 조회 완성` | artifact tree/file/report/plan file 조회, 경로 방어 | artifact tests |
| 17 | `[Feat] Slack 이벤트 처리 완성` | signature, dedupe, channel mapping, mention parsing, thread reply | slack controller/service tests |
| 18 | `[Feat] Slack OAuth 설정 완성` | install URL, callback, state 검증, bot token 저장/삭제 | slack OAuth tests |
| 19 | `[Feat] GitHub 작업 연동 구현` | credential 별 repository 작업 준비, clone/push/PR 실행 interface | GitHub adapter tests |
| 20 | `[Feat] 초대 이메일 발송 구현` | SMTP 설정, afterCommit 발송, 실패 기록 | invite email tests |
| 21 | `[Feat] 프론트 공통 UI 시스템 추가` | components/ui, components/arcade, theme token 정리 | lint/build |
| 22 | `[Feat] 2D 워크스페이스 화면 구현` | Phaser scene, EventBus, agent/member avatar, pan/zoom/context menu | Playwright/visual check |
| 23 | `[Feat] 워크스페이스 상세 화면 완성` | dashboard, agents, tasks, chat, artifact modal, plan modal 통합 | lint/build/node tests |
| 24 | `[Feat] 연동 설정 화면 완성` | Slack/GitHub/Gateway 설정, channel mapping, success/fail route | lint/build/node tests |
| 25 | `[Feat] 실시간 Presence 화면 완성` | SSE stream, 위치 업데이트, member 상태 표시 | BE/FE integration tests |
| 26 | `[Test] 통합 시나리오 검증 추가` | login, workspace, agent, task, report 흐름 검증 | backend test + FE build |
| 27 | `[Perf] 관측 및 부하 테스트 구성` | Prometheus registry, k6 smoke, actuator metrics | k6 smoke, actuator check |
| 28 | `[Docs] 실행 문서와 발표용 README 정리` | 설치, env, API, ERD, 운영/문제 해결 문서 정리 | 문서 링크/명령 검증 |
