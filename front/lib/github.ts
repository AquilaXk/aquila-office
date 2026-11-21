import { apiFetch } from "@/lib/api";

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

export type GithubCredentialStorage = Pick<Storage, "getItem" | "setItem" | "removeItem">;

export function githubIntegrationStorageKey(workspaceId: number): string {
  return `aio.workspace.${workspaceId}.integrations.github`;
}

export function readStoredGithubCredential(
  storage: GithubCredentialStorage | null | undefined,
  workspaceId: number,
): GithubCredential | null {
  if (!storage) {
    return null;
  }

  const raw = storage.getItem(githubIntegrationStorageKey(workspaceId));
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as GithubCredential;
  } catch {
    return null;
  }
}

export function writeStoredGithubCredential(
  storage: GithubCredentialStorage | null | undefined,
  workspaceId: number,
  credential: GithubCredential,
): void {
  storage?.setItem(githubIntegrationStorageKey(workspaceId), JSON.stringify(credential));
}

export function deleteStoredGithubCredential(
  storage: GithubCredentialStorage | null | undefined,
  workspaceId: number,
): void {
  storage?.removeItem(githubIntegrationStorageKey(workspaceId));
}

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
      json: input,
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
      json: input,
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
