import { apiFetch } from "@/lib/api-client";
import {
  normalizeArtifactTree,
  workspaceArtifactFilePath,
  workspaceOrchestrationPlanArtifactPath,
  workspaceArtifactTreePath,
  type ArtifactFileContent,
  type ArtifactTree,
  type OrchestrationPlanArtifact,
} from "@/lib/artifacts-utils";

export {
  flattenArtifactNodes,
  normalizeArtifactTree,
  workspaceArtifactFilePath,
  workspaceOrchestrationPlanArtifactPath,
  workspaceArtifactTreePath,
  type ArtifactFileContent,
  type ArtifactNode,
  type ArtifactNodeType,
  type ArtifactTree,
  type FlattenedArtifactNode,
  type OrchestrationPlanArtifact,
  type OrchestrationPlanFile,
  type OrchestrationPlanStep,
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

export async function fetchOrchestrationPlanArtifact(
  accessToken: string,
  workspaceId: number,
  planId: number,
): Promise<OrchestrationPlanArtifact> {
  return apiFetch<OrchestrationPlanArtifact>(
    workspaceOrchestrationPlanArtifactPath(workspaceId, planId),
    {
      accessToken,
    },
  );
}
