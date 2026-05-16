import test from "node:test";
import assert from "node:assert/strict";
import {
  normalizeTaskReports,
  normalizeWorkspaceTasks,
  workspaceTaskReportsPath,
  workspaceTasksPath,
  type AgentReport,
  type WorkspaceTask,
} from "./tasks-utils.ts";

const task: WorkspaceTask = {
  taskId: 301,
  workspaceId: 101,
  title: "테스트 작성",
  description: "핵심 서비스 테스트를 추가해주세요.",
  type: "TEST_CREATION",
  priority: "HIGH",
  status: "COMPLETED",
  assignedAgentId: 501,
  createdAt: "2026-05-16T10:00:00",
  updatedAt: "2026-05-16T11:00:00",
};

const report: AgentReport = {
  reportId: 901,
  taskId: 301,
  status: "COMPLETED",
  summary: "테스트 생성 완료",
  detail: "테스트 생성 완료",
  artifacts: [
    {
      artifactId: 801,
      artifactType: "RESULT_FILE",
      name: "result.md",
      url: "/api/v1/workspaces/101/artifacts/files?path=result.md",
    },
  ],
  createdAt: "2026-05-16T12:00:00",
};

test("workspace task paths target a single workspace", () => {
  assert.equal(workspaceTasksPath(101), "/api/v1/workspaces/101/tasks");
  assert.equal(
    workspaceTaskReportsPath(101, 301),
    "/api/v1/workspaces/101/tasks/301/reports",
  );
});

test("normalizeWorkspaceTasks accepts direct arrays and page content", () => {
  assert.deepEqual(normalizeWorkspaceTasks([task]), [task]);
  assert.deepEqual(normalizeWorkspaceTasks({ content: [task] }), [task]);
});

test("normalizeTaskReports accepts direct arrays and page content", () => {
  assert.deepEqual(normalizeTaskReports([report]), [report]);
  assert.deepEqual(normalizeTaskReports({ content: [report] }), [report]);
});

test("task normalizers reject non-list payloads", () => {
  assert.deepEqual(normalizeWorkspaceTasks(null), []);
  assert.deepEqual(normalizeWorkspaceTasks({ content: task }), []);
  assert.deepEqual(normalizeTaskReports({ content: report }), []);
});
