import { apiFetch } from "@/lib/api";

export type SlackIntegration = {
  id: number;
  workspaceId: number;
  slackTeamId: string;
  slackChannelId: string;
  channelName: string | null;
  maskedBotToken: string;
  createdByGoogleSub: string;
  active: boolean;
};

export type SlackInstallUrl = {
  url: string;
};

export type SlackChannelMapping = {
  mappingId: number;
  workspaceId: number;
  teamId: string;
  channelId: string;
  channelName: string | null;
  maskedBotToken: string;
  createdByGoogleSub: string;
  active: boolean;
};

export type CreateSlackIntegrationInput = {
  slackTeamId: string;
  slackChannelId: string;
  channelName?: string;
  botToken: string;
};

export type UpdateSlackIntegrationInput = {
  slackTeamId?: string;
  slackChannelId?: string;
  channelName?: string;
  botToken?: string;
};

export type CreateSlackChannelMappingInput = {
  teamId: string;
  channelId: string;
  channelName?: string;
  botToken: string;
};

export type DeleteSlackIntegrationResult = {
  id: number;
  workspaceId: number;
  active: boolean;
};

export type SlackIntegrationStorage = Pick<Storage, "getItem" | "setItem" | "removeItem">;

export const SLACK_PENDING_WORKSPACE_STORAGE_KEY = "aio.slack.pendingWorkspaceId";
export const SLACK_RETURN_TO_STORAGE_KEY = "aio.slack.returnTo";
export const SLACK_LEGACY_PENDING_WORKSPACE_STORAGE_KEY = "aquila-office.pendingSlackWorkspaceId";
export const SLACK_LEGACY_RETURN_TO_STORAGE_KEY = "aquila-office.pendingSlackReturnTo";

export function slackInstallPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/slack/install`;
}

export function slackIntegrationsPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/slack/integrations`;
}

export function slackIntegrationPath(workspaceId: number, integrationId: number): string {
  return `${slackIntegrationsPath(workspaceId)}/${integrationId}`;
}

export function slackChannelMappingsPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/slack/channel-mappings`;
}

export function slackIntegrationStorageKey(workspaceId: number): string {
  return `aio.workspace.${workspaceId}.integrations.slack`;
}

export function readStoredSlackIntegration(
  storage: SlackIntegrationStorage | null | undefined,
  workspaceId: number,
): SlackIntegration | null {
  return readJson<SlackIntegration>(storage, slackIntegrationStorageKey(workspaceId));
}

export function writeStoredSlackIntegration(
  storage: SlackIntegrationStorage | null | undefined,
  workspaceId: number,
  integration: SlackIntegration,
): void {
  storage?.setItem(slackIntegrationStorageKey(workspaceId), JSON.stringify(integration));
}

export function deleteStoredSlackIntegration(
  storage: SlackIntegrationStorage | null | undefined,
  workspaceId: number,
): void {
  storage?.removeItem(slackIntegrationStorageKey(workspaceId));
}

export function saveSlackPendingConnectionState(
  storage: SlackIntegrationStorage | null | undefined,
  workspaceId: number,
): void {
  if (!storage) {
    return;
  }

  const returnTo = `/workspaces/${workspaceId}`;
  storage.setItem(SLACK_PENDING_WORKSPACE_STORAGE_KEY, String(workspaceId));
  storage.setItem(SLACK_RETURN_TO_STORAGE_KEY, returnTo);
  storage.setItem(SLACK_LEGACY_PENDING_WORKSPACE_STORAGE_KEY, String(workspaceId));
  storage.setItem(SLACK_LEGACY_RETURN_TO_STORAGE_KEY, returnTo);
}

export function readPendingSlackWorkspaceId(
  storage: SlackIntegrationStorage | null | undefined,
): string | null {
  return firstStoredValue(storage, [
    SLACK_PENDING_WORKSPACE_STORAGE_KEY,
    SLACK_LEGACY_PENDING_WORKSPACE_STORAGE_KEY,
  ]);
}

export function readSlackReturnTo(storage: SlackIntegrationStorage | null | undefined): string {
  return safeReturnTo(
    firstStoredValue(storage, [
      SLACK_RETURN_TO_STORAGE_KEY,
      SLACK_LEGACY_RETURN_TO_STORAGE_KEY,
    ]),
  );
}

export function clearSlackPendingConnectionState(
  storage: SlackIntegrationStorage | null | undefined,
): void {
  if (!storage) {
    return;
  }

  for (const key of [
    SLACK_PENDING_WORKSPACE_STORAGE_KEY,
    SLACK_RETURN_TO_STORAGE_KEY,
    SLACK_LEGACY_PENDING_WORKSPACE_STORAGE_KEY,
    SLACK_LEGACY_RETURN_TO_STORAGE_KEY,
  ]) {
    storage.removeItem(key);
  }
}

export async function fetchSlackInstallUrl(
  accessToken: string,
  workspaceId: number,
): Promise<SlackInstallUrl> {
  return apiFetch<SlackInstallUrl>(slackInstallPath(workspaceId), {
    accessToken,
  });
}

export async function fetchSlackIntegrations(
  accessToken: string,
  workspaceId: number,
): Promise<SlackIntegration[]> {
  return apiFetch<SlackIntegration[]>(slackIntegrationsPath(workspaceId), {
    accessToken,
  });
}

export async function fetchSlackChannelMappings(
  accessToken: string,
  workspaceId: number,
): Promise<SlackChannelMapping[]> {
  return apiFetch<SlackChannelMapping[]>(slackChannelMappingsPath(workspaceId), {
    accessToken,
  });
}

export async function createSlackChannelMapping(
  accessToken: string,
  workspaceId: number,
  input: CreateSlackChannelMappingInput,
): Promise<SlackChannelMapping> {
  return apiFetch<SlackChannelMapping>(slackChannelMappingsPath(workspaceId), {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function createSlackIntegration(
  accessToken: string,
  workspaceId: number,
  input: CreateSlackIntegrationInput,
): Promise<SlackIntegration> {
  return apiFetch<SlackIntegration>(slackIntegrationsPath(workspaceId), {
    method: "POST",
    accessToken,
    json: input,
  });
}

export async function updateSlackIntegration(
  accessToken: string,
  workspaceId: number,
  integrationId: number,
  input: UpdateSlackIntegrationInput,
): Promise<SlackIntegration> {
  return apiFetch<SlackIntegration>(slackIntegrationPath(workspaceId, integrationId), {
    method: "PATCH",
    accessToken,
    json: input,
  });
}

export async function deleteSlackIntegration(
  accessToken: string,
  workspaceId: number,
  integrationId: number,
): Promise<DeleteSlackIntegrationResult> {
  return apiFetch<DeleteSlackIntegrationResult>(
    slackIntegrationPath(workspaceId, integrationId),
    {
      method: "DELETE",
      accessToken,
    },
  );
}

function readJson<T>(storage: SlackIntegrationStorage | null | undefined, key: string): T | null {
  if (!storage) {
    return null;
  }

  const raw = storage.getItem(key);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function firstStoredValue(
  storage: SlackIntegrationStorage | null | undefined,
  keys: string[],
): string | null {
  if (!storage) {
    return null;
  }

  for (const key of keys) {
    const value = storage.getItem(key);
    if (value) {
      return value;
    }
  }

  return null;
}

function safeReturnTo(value: string | null): string {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return "/workspaces";
  }

  return value;
}
