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
