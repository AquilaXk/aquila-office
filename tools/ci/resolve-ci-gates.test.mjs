import assert from "node:assert/strict";
import test from "node:test";

import { classifyChangedFiles } from "./resolve-ci-gates.mjs";

test("README와 라이선스·고지 문서 전용 변경은 문서 게이트만 켠다", () => {
  assert.deepEqual(classifyChangedFiles([
    "README.md",
    "LICENSE",
    "NOTICE",
    "THIRD_PARTY_NOTICES.md",
    "front/public/aquila-office-readme-preview.png",
    "front/public/readme/workspace-dashboard.png",
    "tools/readme/validate-readme.mjs",
  ]), {
    backend: false,
    deploy: false,
    docs: true,
    frontend: false,
    infra: false,
    smoke: false,
    workflow: false,
  });
});

test("프론트엔드 앱 변경은 frontend 게이트와 배포 영향을 켠다", () => {
  const gates = classifyChangedFiles(["front/app/workspaces/page.tsx"]);

  assert.equal(gates.frontend, true);
  assert.equal(gates.deploy, true);
  assert.equal(gates.backend, false);
  assert.equal(gates.docs, false);
});

test("데스크톱 src-tauri 변경은 frontend 게이트만 켠다", () => {
  const gates = classifyChangedFiles(["front/src-tauri/src/lib.rs"]);

  assert.equal(gates.frontend, true);
  assert.equal(gates.deploy, false);
  assert.equal(gates.backend, false);
  assert.equal(gates.docs, false);
  assert.equal(gates.infra, false);
  assert.equal(gates.smoke, false);
  assert.equal(gates.workflow, false);
});

test("백엔드 변경은 backend 게이트와 배포 영향을 켠다", () => {
  const gates = classifyChangedFiles(["back/src/main/java/com/aquila/office/App.java"]);

  assert.equal(gates.backend, true);
  assert.equal(gates.deploy, true);
  assert.equal(gates.frontend, false);
});

test("workflow 변경은 모든 CI 게이트를 켠다", () => {
  assert.deepEqual(classifyChangedFiles([".github/workflows/ci.yml"]), {
    backend: true,
    deploy: false,
    docs: true,
    frontend: true,
    infra: true,
    smoke: true,
    workflow: true,
  });
});

test("배포 workflow와 배포 스크립트 변경은 CD 배포 영향으로 분류한다", () => {
  const gates = classifyChangedFiles([
    ".github/workflows/backend-cd.yml",
    "scripts/deploy/backend-blue-green.sh",
  ]);

  assert.equal(gates.workflow, true);
  assert.equal(gates.deploy, true);
});

test("데스크톱 릴리스 workflow 및 릴리스 에셋 스크립트 변경은 workflow 게이트를 켠다", () => {
  const gates = classifyChangedFiles([
    ".github/workflows/desktop-release.yml",
    "tools/release/release-assets.mjs",
  ]);

  assert.equal(gates.workflow, true);
  assert.equal(gates.deploy, false);
  assert.equal(gates.frontend, false);
});

test("GitHub 템플릿·보안·기여·TODO·gitignore 변경은 workflow 위생·계약 게이트를 켠다", () => {
  const gates = classifyChangedFiles([
    ".github/ISSUE_TEMPLATE/bug_report.yml",
    ".github/ISSUE_TEMPLATE/config.yml",
    ".github/release.yml",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "TODO.md",
    ".gitignore",
  ]);

  assert.equal(gates.workflow, true);
  assert.equal(gates.deploy, false);
  assert.equal(gates.backend, false);
  assert.equal(gates.frontend, false);
});

test("Terraform 변경은 infra 게이트만 켠다", () => {
  const gates = classifyChangedFiles(["infra/terraform/oci/always-free-a1-flex/main.tf"]);

  assert.equal(gates.infra, true);
  assert.equal(gates.deploy, false);
  assert.equal(gates.backend, false);
  assert.equal(gates.frontend, false);
});

test("강제 전체 실행 모드는 모든 CI 게이트와 배포 영향을 켠다", () => {
  assert.deepEqual(classifyChangedFiles(["README.md"], { runAll: true }), {
    backend: true,
    deploy: true,
    docs: true,
    frontend: true,
    infra: true,
    smoke: true,
    workflow: true,
  });
});

test("저장소 위생 가드는 로컬 전용 에이전트 문서·시크릿·레거시 템플릿·금지 브랜치와 커밋 트레일러를 차단한다", async () => {
  const {
    findForbiddenBranchName,
    findForbiddenCommitMessages,
    findForbiddenTrackedFiles,
    findMissingRequiredTrackedFiles,
  } = await import("./check-repo-hygiene.mjs");

  assert.deepEqual(
    findMissingRequiredTrackedFiles([
      "README.md",
      "LICENSE",
      "NOTICE",
      "THIRD_PARTY_NOTICES.md",
      "CONTRIBUTING.md",
      "SECURITY.md",
      "TODO.md",
      ".github/ISSUE_TEMPLATE/config.yml",
      ".github/ISSUE_TEMPLATE/bug_report.yml",
      ".github/release.yml",
    ]),
    [],
  );

  assert.equal(findMissingRequiredTrackedFiles(["README.md"]).length, 9);

  assert.deepEqual(
    findForbiddenTrackedFiles([
      "README.md",
      "CONTRIBUTING.md",
      "SECURITY.md",
      "TODO.md",
      ".github/ISSUE_TEMPLATE/config.yml",
      ".github/ISSUE_TEMPLATE/bug_report.yml",
      ".github/release.yml",
      ".env.example",
      "back/.env.example",
      "front/.env.example",
    ]),
    [],
  );

  assert.equal(
    findForbiddenTrackedFiles([
      "AGENTS.md",
      "CLAUDE.md",
      "CLAUDE.local.md",
      "GEMINI.md",
      "implementation_plan.md",
      "walkthrough.md",
      ".claude/settings.json",
      ".codex/config.toml",
      ".agents/rules.md",
      ".gemini/settings.json",
      ".coderabbit.yaml",
      ".github/dependabot.yml",
      ".github/pull_request_template.md",
      ".github/ISSUE_TEMPLATE/epic.yml",
      ".github/ISSUE_TEMPLATE/feature.yml",
      ".github/ISSUE_TEMPLATE/bug.yml",
      ".env",
      "vertex-key.json",
    ]).length,
    18,
  );

  assert.deepEqual(findForbiddenBranchName("chore/github-setup"), []);
  assert.equal(findForbiddenBranchName("claude/fix-auth").length, 1);
  assert.equal(findForbiddenBranchName("codex/task-1").length, 1);

  assert.deepEqual(
    findForbiddenCommitMessages([
      { sha: "abc1234", message: "[Perf] 백엔드 JPA 쿼리·저수준 핫 경로 최적화 및 저장소 위생 가드 추가" },
    ]),
    [],
  );

  assert.equal(
    findForbiddenCommitMessages([
      { sha: "bad0001", message: "feat: test\n\nCo-Authored-By: Bot <bot@example.com>" },
      { sha: "bad0002", message: "chore: test\n\nGenerated with Tool" },
    ]).length,
    2,
  );
});
