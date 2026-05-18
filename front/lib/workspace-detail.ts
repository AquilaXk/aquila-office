export type WorkspaceDetailReadiness = "empty" | "ready" | "attention";

export type WorkspaceDetailDashboardSummary = {
  agentCount: number;
  runningAgentCount: number;
  idleAgentCount: number;
  errorAgentCount: number;
  taskCount: number;
  runningTaskCount: number;
  completedTaskCount: number;
  failedTaskCount: number;
};

export type WorkspaceDetailAgentLike = {
  status: string;
};

export type WorkspaceDetailTaskLike = {
  status: string;
};

export type WorkspaceDetailMemberLike = {
  memberId: number;
};

export type WorkspaceDetailPresenceLike = {
  memberId: number;
  status: string | null;
};

export type WorkspaceDetailOverviewInput = {
  dashboardSummary: WorkspaceDetailDashboardSummary | null;
  agents: WorkspaceDetailAgentLike[];
  tasks: WorkspaceDetailTaskLike[];
  members: WorkspaceDetailMemberLike[];
  presenceMembers: WorkspaceDetailPresenceLike[];
};

export type WorkspaceDetailOverview = {
  readiness: WorkspaceDetailReadiness;
  agentSummary: {
    total: number;
    ready: number;
    running: number;
    attention: number;
  };
  taskSummary: {
    total: number;
    active: number;
    completed: number;
    failed: number;
  };
  memberSummary: {
    total: number;
    online: number;
  };
};

const ACTIVE_TASK_STATUSES = new Set(["REQUESTED", "ASSIGNED", "IN_PROGRESS", "WAITING_USER"]);
const ATTENTION_AGENT_STATUSES = new Set(["CREATING", "SYNC_FAILED", "ERROR"]);

export function createWorkspaceDetailOverview(
  input: WorkspaceDetailOverviewInput,
): WorkspaceDetailOverview {
  const summary = input.dashboardSummary;
  const readyAgents = input.agents.filter((agent) => agent.status === "READY").length;
  const attentionAgents = input.agents.filter((agent) =>
    ATTENTION_AGENT_STATUSES.has(agent.status),
  ).length;
  const activeTasks = input.tasks.filter((task) => ACTIVE_TASK_STATUSES.has(task.status)).length;
  const completedTasks = input.tasks.filter((task) => task.status === "COMPLETED").length;
  const failedTasks = input.tasks.filter((task) =>
    task.status === "FAILED" || task.status === "CANCELED",
  ).length;
  const onlineMembers = input.presenceMembers.filter((member) => member.status === "ACTIVE").length;
  const totalAgents = summary?.agentCount ?? input.agents.length;
  const totalTasks = summary?.taskCount ?? input.tasks.length;
  const hasAttention =
    (summary?.errorAgentCount ?? attentionAgents) > 0 ||
    (summary?.failedTaskCount ?? failedTasks) > 0 ||
    input.tasks.some((task) => task.status === "WAITING_USER");

  return {
    readiness: readinessFor(totalAgents, totalTasks, input.members.length, hasAttention),
    agentSummary: {
      total: totalAgents,
      ready: readyAgents,
      running: summary?.runningAgentCount ?? 0,
      attention: summary ? Math.max(summary.errorAgentCount, attentionAgents) : attentionAgents,
    },
    taskSummary: {
      total: totalTasks,
      active: summary?.runningTaskCount ?? activeTasks,
      completed: summary?.completedTaskCount ?? completedTasks,
      failed: summary?.failedTaskCount ?? failedTasks,
    },
    memberSummary: {
      total: input.members.length,
      online: onlineMembers,
    },
  };
}

function readinessFor(
  totalAgents: number,
  totalTasks: number,
  totalMembers: number,
  hasAttention: boolean,
): WorkspaceDetailReadiness {
  if (hasAttention) {
    return "attention";
  }
  if (totalAgents === 0 && totalTasks === 0 && totalMembers === 0) {
    return "empty";
  }
  return "ready";
}
