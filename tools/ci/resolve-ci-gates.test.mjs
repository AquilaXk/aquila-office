import assert from "node:assert/strict";
import test from "node:test";

import { classifyChangedFiles } from "./resolve-ci-gates.mjs";

test("README와 라이선스 전용 변경은 문서 게이트만 켠다", () => {
  assert.deepEqual(classifyChangedFiles([
    "README.md",
    "LICENSE",
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

test("데스크톱 릴리스 workflow 변경은 workflow 게이트를 켠다", () => {
  const gates = classifyChangedFiles([".github/workflows/desktop-release.yml"]);

  assert.equal(gates.workflow, true);
  assert.equal(gates.deploy, false);
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
