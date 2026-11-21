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

export type WorkspaceDetailActionItemId =
  | "agents"
  | "tasks"
  | "files"
  | "chat"
  | "invite"
  | "plan";

export type WorkspaceDetailActionItemState = "ready" | "empty" | "locked";

export type WorkspaceDetailActionItemTone = "neutral" | "success" | "warning";

export type WorkspaceDetailActionItemsInput = {
  agentCount: number;
  taskCount: number;
  memberCount: number;
  canCreateInvite: boolean;
  hasArtifactTree: boolean;
  hasOrchestrationPlan: boolean;
};

export type WorkspaceDetailActionItem = {
  id: WorkspaceDetailActionItemId;
  label: string;
  value: string;
  state: WorkspaceDetailActionItemState;
  tone: WorkspaceDetailActionItemTone;
};

const ACTIVE_TASK_STATUSES = new Set(["REQUESTED", "ASSIGNED", "IN_PROGRESS", "WAITING_USER"]);
const ATTENTION_AGENT_STATUSES = new Set(["CREATING", "SYNC_FAILED", "ERROR"]);

export function createWorkspaceDetailOverview(
  input: WorkspaceDetailOverviewInput,
): WorkspaceDetailOverview {
  const summary = input.dashboardSummary;
  let readyAgentCount = 0;
  let busyAgentCount = 0;
  for (let i = 0; i < input.agents.length; i += 1) {
    const status = input.agents[i].status;
    if (status === "READY") {
      readyAgentCount += 1;
    } else if (ATTENTION_AGENT_STATUSES.has(status)) {
      busyAgentCount += 1;
    }
  }

  let runningTaskCount = 0;
  let completedTaskCount = 0;
  let failedTaskCount = 0;
  let hasWaitingUserTask = false;
  for (let i = 0; i < input.tasks.length; i += 1) {
    const status = input.tasks[i].status;
    if (ACTIVE_TASK_STATUSES.has(status)) {
      runningTaskCount += 1;
      if (status === "WAITING_USER") {
        hasWaitingUserTask = true;
      }
    } else if (status === "COMPLETED") {
      completedTaskCount += 1;
    } else if (status === "FAILED" || status === "CANCELED") {
      failedTaskCount += 1;
    }
  }

  let onlineMembers = 0;
  for (let i = 0; i < input.presenceMembers.length; i += 1) {
    if (input.presenceMembers[i].status === "ACTIVE") {
      onlineMembers += 1;
    }
  }

  const totalAgents = summary?.agentCount ?? input.agents.length;
  const totalTasks = summary?.taskCount ?? input.tasks.length;
  const hasAttentionState =
    (summary?.errorAgentCount ?? busyAgentCount) > 0 ||
    (summary?.failedTaskCount ?? failedTaskCount) > 0 ||
    hasWaitingUserTask;

  return {
    readiness: readinessFor(totalAgents, totalTasks, input.members.length, hasAttentionState),
    agentSummary: {
      total: totalAgents,
      ready: readyAgentCount,
      running: summary?.runningAgentCount ?? 0,
      attention: summary ? Math.max(summary.errorAgentCount, busyAgentCount) : busyAgentCount,
    },
    taskSummary: {
      total: totalTasks,
      active: summary?.runningTaskCount ?? runningTaskCount,
      completed: summary?.completedTaskCount ?? completedTaskCount,
      failed: summary?.failedTaskCount ?? failedTaskCount,
    },
    memberSummary: {
      total: input.members.length,
      online: onlineMembers,
    },
  };
}

export function createWorkspaceDetailActionItems(
  input: WorkspaceDetailActionItemsInput,
): WorkspaceDetailActionItem[] {
  return [
    {
      id: "agents",
      label: "Agents",
      value: String(input.agentCount),
      state: input.agentCount > 0 ? "ready" : "empty",
      tone: input.agentCount > 0 ? "success" : "neutral",
    },
    {
      id: "tasks",
      label: "Tasks",
      value: String(input.taskCount),
      state: input.taskCount > 0 ? "ready" : "empty",
      tone: input.taskCount > 0 ? "warning" : "neutral",
    },
    {
      id: "files",
      label: "Files",
      value: input.hasArtifactTree ? "ready" : "empty",
      state: input.hasArtifactTree ? "ready" : "empty",
      tone: "neutral",
    },
    {
      id: "chat",
      label: "Task Chat",
      value: `${input.taskCount} tasks`,
      state: input.taskCount > 0 ? "ready" : "empty",
      tone: "neutral",
    },
    {
      id: "invite",
      label: "Invite",
      value: `${input.memberCount} members`,
      state: input.canCreateInvite ? "ready" : "locked",
      tone: input.canCreateInvite ? "success" : "neutral",
    },
    {
      id: "plan",
      label: "Plan",
      value: input.hasOrchestrationPlan ? "ready" : "empty",
      state: input.hasOrchestrationPlan ? "ready" : "empty",
      tone: "neutral",
    },
  ];
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
