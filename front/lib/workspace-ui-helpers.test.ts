import assert from "node:assert/strict";
import test from "node:test";
import {
  buildAgentQuickPrompts,
  formatDurationMs,
  formatOrchestrationStatus,
  formatOrchestrationStepStatus,
  formatTaskStatus,
  formatTaskType,
  formatTokenUsageSummary,
} from "./workspace-formatters.ts";
import {
  MAX_TASK_APPROVAL_TEXT_CHARS,
  validateTaskApprovalText,
} from "./tasks-utils.ts";

test("formatTokenUsageSummary formats unlimited and bounded daily token budgets", () => {
  assert.deepEqual(formatTokenUsageSummary(12500, null), {
    used: 12500,
    limit: null,
    percent: null,
    label: "12,500 / 무제한",
  });

  assert.deepEqual(formatTokenUsageSummary(50000, 100000), {
    used: 50000,
    limit: 100000,
    percent: 50,
    label: "50,000 / 100,000 (50%)",
  });

  assert.deepEqual(formatTokenUsageSummary(150000, 100000), {
    used: 150000,
    limit: 100000,
    percent: 100,
    label: "150,000 / 100,000 (100%)",
  });
});

test("formatDurationMs and status formatters return clean Korean descriptors", () => {
  assert.equal(formatDurationMs(null), "-");
  assert.equal(formatDurationMs(450), "450ms");
  assert.equal(formatDurationMs(2400), "2.4초");
  assert.equal(formatDurationMs(75000), "1분 15초");

  assert.equal(formatTaskStatus("WAITING_USER"), "승인 대기");
  assert.equal(formatOrchestrationStatus("PLANNED"), "계획됨");
  assert.equal(formatOrchestrationStepStatus("DISPATCHED"), "전달됨");
});

test("validateTaskApprovalText enforces fail-closed length bounds", () => {
  assert.deepEqual(validateTaskApprovalText("   "), { ok: true, value: null });
  assert.deepEqual(validateTaskApprovalText("  승인합니다  "), {
    ok: true,
    value: "승인합니다",
  });

  const oversized = "가".repeat(MAX_TASK_APPROVAL_TEXT_CHARS + 1);
  const rejected = validateTaskApprovalText(oversized, "검토 의견");
  assert.equal(rejected.ok, false);
});

test("formatTaskType maps standard enums to Korean labels and bounds unknown types", () => {
  assert.equal(formatTaskType("TEST_CREATION"), "테스트 작성");
  assert.equal(formatTaskType("PR_REVIEW"), "PR 검토");
  assert.equal(formatTaskType("CODE_REVIEW"), "코드 리뷰");
  assert.equal(formatTaskType("CODE_CHANGE"), "코드 변경");
  assert.equal(formatTaskType("FEATURE_IMPLEMENTATION"), "기능 구현");
  assert.equal(formatTaskType("PLANNING"), "설계·기획");
  assert.equal(formatTaskType("DOCS"), "문서 정리");
  assert.equal(formatTaskType(undefined), "일반 작업");
  assert.equal(formatTaskType("CUSTOM_FLOW_NAME_THAT_IS_VERY_LONG_OVER_32"), "CUSTOM FLOW NAME THAT IS VERY LO");
});

test("buildAgentQuickPrompts returns 3 role-tailored prompts per category", () => {
  for (const cat of ["ORCHESTRATOR", "BACKEND", "FRONTEND", "QA", "CUSTOM"] as const) {
    const prompts = buildAgentQuickPrompts(cat, "Tester");
    assert.equal(prompts.length, 3);
    for (const p of prompts) {
      assert.ok(p.label.length > 0 && p.prompt.length > 0);
    }
  }
});
