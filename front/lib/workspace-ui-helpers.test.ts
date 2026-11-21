import assert from "node:assert/strict";
import test from "node:test";
import {
  formatDurationMs,
  formatOrchestrationStatus,
  formatOrchestrationStepStatus,
  formatTaskStatus,
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
