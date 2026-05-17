import { apiFetch } from "@/lib/api-client";

export type GithubCredential = {
  credentialId: number;
  workspaceId: number;
  displayName: string;
  maskedToken: string;
};

export type CreateGithubCredentialInput = {
  displayName: string;
  token: string;
};

export type UpdateGithubCredentialInput = {
  displayName?: string;
  token?: string;
};

export async function fetchGithubCredentials(
  accessToken: string,
  workspaceId: number,
): Promise<GithubCredential[]> {
  return apiFetch<GithubCredential[]>(
    `/api/v1/workspaces/${workspaceId}/github/credentials`,
    {
      accessToken,
    },
  );
}

export async function createGithubCredential(
  accessToken: string,
  workspaceId: number,
  input: CreateGithubCredentialInput,
): Promise<GithubCredential> {
  return apiFetch<GithubCredential>(
    `/api/v1/workspaces/${workspaceId}/github/credentials`,
    {
      method: "POST",
      accessToken,
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(input),
    },
  );
}

export async function updateGithubCredential(
  accessToken: string,
  workspaceId: number,
  credentialId: number,
  input: UpdateGithubCredentialInput,
): Promise<GithubCredential> {
  return apiFetch<GithubCredential>(
    `/api/v1/workspaces/${workspaceId}/github/credentials/${credentialId}`,
    {
      method: "PATCH",
      accessToken,
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(input),
    },
  );
}

export async function deleteGithubCredential(
  accessToken: string,
  workspaceId: number,
  credentialId: number,
): Promise<void> {
  return apiFetch<void>(
    `/api/v1/workspaces/${workspaceId}/github/credentials/${credentialId}`,
    {
      method: "DELETE",
      accessToken,
    },
  );
}
