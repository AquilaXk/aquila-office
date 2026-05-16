import { apiFetch } from "@/lib/api-client";
import { WORKSPACE_MEMBER_HEADER } from "@/lib/workspaces";
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
  memberGoogleSub: string,
  workspaceId: number,
): Promise<ArtifactTree> {
  const payload = await apiFetch<unknown>(workspaceArtifactTreePath(workspaceId), {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });

  return normalizeArtifactTree(payload, workspaceId);
}

export async function fetchArtifactFile(
  memberGoogleSub: string,
  workspaceId: number,
  path: string,
): Promise<ArtifactFileContent> {
  return apiFetch<ArtifactFileContent>(workspaceArtifactFilePath(workspaceId, path), {
    headers: {
      [WORKSPACE_MEMBER_HEADER]: memberGoogleSub,
    },
  });
}
