import { apiFetch } from "@/lib/api";
import type {
  TaskExecutionStatus,
  TaskPriority,
  TaskStatus,
  TaskType,
} from "@/lib/tasks-utils";

export type ChatMessageRole = "USER" | "ASSISTANT" | "SYSTEM";

export type ChatMessage = {
  messageId: number;
  chatSessionId: number;
  taskId: number | null;
  taskExecutionId: number | null;
  orchestrationPlanId: number | null;
  role: ChatMessageRole;
  content: string;
  createdAt: string;
};

export type SendChatMessageInput = {
  message: string;
  agentId: number;
  repositoryId?: number | null;
  taskType?: TaskType;
  priority?: TaskPriority;
  title?: string;
  createPr?: boolean;
  chatSessionId?: number | null;
};

export type SendChatMessageResult = {
  chatSessionId: number;
  taskId: number;
  workspaceId: number;
  assignedAgentId: number;
  taskStatus: TaskStatus;
  taskExecutionId: number | null;
  executionStatus: TaskExecutionStatus | null;
  orchestrationPlanId: number | null;
  finalText: string | null;
  failureReason: string | null;
  createdAt: string;
  messages: ChatMessage[];
};

export type ChatSessionMessages = {
  chatSessionId: number;
  messages: ChatMessage[];
  nextCursor: number | null;
  hasMore: boolean;
};

export type FetchChatSessionMessagesInput = {
  afterMessageId?: number | null;
  limit?: number | null;
};

export type ActorChatSessionIds = Record<string, number>;

export type ActorChatSessionStorage = Pick<Storage, "getItem" | "setItem">;

export const CHAT_SESSION_MESSAGE_PAGE_SIZE = 30;

export function chatSessionMessagePageInput(
  afterMessageId?: number | null,
): FetchChatSessionMessagesInput {
  return {
    afterMessageId,
    limit: CHAT_SESSION_MESSAGE_PAGE_SIZE,
  };
}

export function workspaceChatMessagesPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/chat/messages`;
}

export function actorChatSessionKey(actorId: string | number): string {
  return String(actorId).trim();
}

export function actorChatSessionIdsStorageKey(workspaceId: number): string {
  return `aquila-office.workspace.${workspaceId}.actorChatSessions`;
}

export function readActorChatSessionIds(
  storage: ActorChatSessionStorage | null | undefined,
  workspaceId: number,
): ActorChatSessionIds {
  if (!storage) {
    return {};
  }

  const raw = storage.getItem(actorChatSessionIdsStorageKey(workspaceId));
  if (!raw) {
    return {};
  }

  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return Object.fromEntries(
      Object.entries(parsed)
        .map(([key, value]) => [key, Number(value)] as const)
        .filter(([key, value]) => key.length > 0 && Number.isInteger(value) && value > 0),
    );
  } catch {
    return {};
  }
}

export function writeActorChatSessionIds(
  storage: ActorChatSessionStorage | null | undefined,
  workspaceId: number,
  sessionIds: ActorChatSessionIds,
) {
  if (!storage) {
    return;
  }

  storage.setItem(actorChatSessionIdsStorageKey(workspaceId), JSON.stringify(sessionIds));
}

export function workspaceTaskChatMessagesPath(workspaceId: number, taskId: number): string {
  return `/api/v1/workspaces/${workspaceId}/chat/tasks/${taskId}/messages`;
}

export function workspaceChatSessionMessagesPath(
  workspaceId: number,
  chatSessionId: number,
  input: FetchChatSessionMessagesInput = {},
): string {
  const params = new URLSearchParams();
  if (input.afterMessageId) {
    params.set("afterMessageId", String(input.afterMessageId));
  }
  if (input.limit) {
    params.set("limit", String(input.limit));
  }

  const query = params.toString();
  const path = `/api/v1/workspaces/${workspaceId}/chat/sessions/${chatSessionId}/messages`;
  return query ? `${path}?${query}` : path;
}

export async function sendWorkspaceChatMessage(
  accessToken: string,
  workspaceId: number,
  input: SendChatMessageInput,
): Promise<SendChatMessageResult> {
  return apiFetch<SendChatMessageResult>(workspaceChatMessagesPath(workspaceId), {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function fetchTaskChatMessages(
  accessToken: string,
  workspaceId: number,
  taskId: number,
): Promise<ChatMessage[]> {
  return apiFetch<ChatMessage[]>(workspaceTaskChatMessagesPath(workspaceId, taskId), {
    accessToken,
  });
}

export async function fetchChatSessionMessages(
  accessToken: string,
  workspaceId: number,
  chatSessionId: number,
  input: FetchChatSessionMessagesInput = {},
): Promise<ChatSessionMessages> {
  return apiFetch<ChatSessionMessages>(
    workspaceChatSessionMessagesPath(workspaceId, chatSessionId, input),
    {
      accessToken,
    },
  );
}
