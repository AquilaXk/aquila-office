import { apiFetch } from "@/lib/api-client";
import { WORKSPACE_MEMBER_HEADER } from "@/lib/workspaces";

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
  memberGoogleSub: string,
  workspaceId: number,
): Promise<GithubCredential[]> {
  return apiFetch<GithubCredential[]>(
    `/api/v1/workspaces/${workspaceId}/github/credentials`,
    {
      headers: {
        [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
      },
    },
  );
}

export async function createGithubCredential(
  memberGoogleSub: string,
  workspaceId: number,
  input: CreateGithubCredentialInput,
): Promise<GithubCredential> {
  return apiFetch<GithubCredential>(
    `/api/v1/workspaces/${workspaceId}/github/credentials`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
      },
      body: JSON.stringify(input),
    },
  );
}

export async function updateGithubCredential(
  memberGoogleSub: string,
  workspaceId: number,
  credentialId: number,
  input: UpdateGithubCredentialInput,
): Promise<GithubCredential> {
  return apiFetch<GithubCredential>(
    `/api/v1/workspaces/${workspaceId}/github/credentials/${credentialId}`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
      },
      body: JSON.stringify(input),
    },
  );
}

export async function deleteGithubCredential(
  memberGoogleSub: string,
  workspaceId: number,
  credentialId: number,
): Promise<void> {
  return apiFetch<void>(
    `/api/v1/workspaces/${workspaceId}/github/credentials/${credentialId}`,
    {
      method: "DELETE",
      headers: {
        [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
      },
    },
  );
}
