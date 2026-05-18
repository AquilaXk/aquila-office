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
