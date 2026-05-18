import assert from "node:assert/strict";
import test from "node:test";
import {
  createWorkspaceDetailActionItems,
  createWorkspaceDetailOverview,
} from "./workspace-detail.ts";

test("workspace detail overview summarizes dashboard, agents, tasks, members, and presence", () => {
  const overview = createWorkspaceDetailOverview({
    dashboardSummary: {
      agentCount: 3,
      runningAgentCount: 1,
      idleAgentCount: 1,
      errorAgentCount: 1,
      taskCount: 4,
      runningTaskCount: 2,
      completedTaskCount: 1,
      failedTaskCount: 1,
    },
    agents: [
      { status: "READY" },
      { status: "ERROR" },
      { status: "CREATING" },
    ],
    tasks: [
      { status: "IN_PROGRESS" },
      { status: "WAITING_USER" },
      { status: "COMPLETED" },
      { status: "FAILED" },
    ],
    members: [{ memberId: 1 }, { memberId: 2 }],
    presenceMembers: [{ memberId: 1, status: "ACTIVE" }],
  });

  assert.deepEqual(overview, {
    readiness: "attention",
    agentSummary: {
      total: 3,
      ready: 1,
      running: 1,
      attention: 2,
    },
    taskSummary: {
      total: 4,
      active: 2,
      completed: 1,
      failed: 1,
    },
    memberSummary: {
      total: 2,
      online: 1,
    },
  });
});

test("workspace detail overview falls back to loaded collections when dashboard is empty", () => {
  const overview = createWorkspaceDetailOverview({
    dashboardSummary: null,
    agents: [{ status: "READY" }],
    tasks: [{ status: "REQUESTED" }],
    members: [{ memberId: 7 }],
    presenceMembers: [],
  });

  assert.equal(overview.readiness, "ready");
  assert.deepEqual(overview.agentSummary, {
    total: 1,
    ready: 1,
    running: 0,
    attention: 0,
  });
  assert.deepEqual(overview.taskSummary, {
    total: 1,
    active: 1,
    completed: 0,
    failed: 0,
  });
  assert.deepEqual(overview.memberSummary, {
    total: 1,
    online: 0,
  });
});

test("workspace detail action items expose modal and workflow entry states", () => {
  const actions = createWorkspaceDetailActionItems({
    agentCount: 2,
    taskCount: 3,
    memberCount: 4,
    canCreateInvite: true,
    hasArtifactTree: true,
    hasOrchestrationPlan: false,
  });

  assert.deepEqual(actions, [
    {
      id: "agents",
      label: "Agents",
      value: "2",
      state: "ready",
      tone: "success",
    },
    {
      id: "tasks",
      label: "Tasks",
      value: "3",
      state: "ready",
      tone: "warning",
    },
    {
      id: "files",
      label: "Files",
      value: "ready",
      state: "ready",
      tone: "neutral",
    },
    {
      id: "chat",
      label: "Task Chat",
      value: "3 tasks",
      state: "ready",
      tone: "neutral",
    },
    {
      id: "invite",
      label: "Invite",
      value: "4 members",
      state: "ready",
      tone: "success",
    },
    {
      id: "plan",
      label: "Plan",
      value: "empty",
      state: "empty",
      tone: "neutral",
    },
  ]);
});
