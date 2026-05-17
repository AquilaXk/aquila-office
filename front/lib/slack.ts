import { apiFetch } from "@/lib/api-client";

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

export type DeleteSlackIntegrationResult = {
  id: number;
  workspaceId: number;
  active: boolean;
};

export function slackInstallPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/slack/install`;
}

export function slackIntegrationsPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/slack/integrations`;
}

export function slackIntegrationPath(workspaceId: number, integrationId: number): string {
  return `${slackIntegrationsPath(workspaceId)}/${integrationId}`;
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

export async function createSlackIntegration(
  accessToken: string,
  workspaceId: number,
  input: CreateSlackIntegrationInput,
): Promise<SlackIntegration> {
  return apiFetch<SlackIntegration>(slackIntegrationsPath(workspaceId), {
    method: "POST",
    accessToken,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
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
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
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
