import { apiFetch } from "@/lib/api-client";
import {
  normalizeArtifactTree,
  workspaceArtifactFilePath,
  workspaceArtifactTreePath,
  type ArtifactFileContent,
  type ArtifactTree,
} from "@/lib/artifacts-utils";

export {
  flattenArtifactNodes,
  normalizeArtifactTree,
  workspaceArtifactFilePath,
  workspaceArtifactTreePath,
  type ArtifactFileContent,
  type ArtifactNode,
  type ArtifactNodeType,
  type ArtifactTree,
  type FlattenedArtifactNode,
} from "@/lib/artifacts-utils";

export async function fetchArtifactTree(
  accessToken: string,
  workspaceId: number,
): Promise<ArtifactTree> {
  const payload = await apiFetch<unknown>(workspaceArtifactTreePath(workspaceId), {
    accessToken,
  });

  return normalizeArtifactTree(payload, workspaceId);
}

export async function fetchArtifactFile(
  accessToken: string,
  workspaceId: number,
  path: string,
): Promise<ArtifactFileContent> {
  return apiFetch<ArtifactFileContent>(workspaceArtifactFilePath(workspaceId, path), {
    accessToken,
  });
}
