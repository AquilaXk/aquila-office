import { apiFetch } from "./index.ts";
import {
  workspaceArtifactFilePath,
  workspaceArtifactTreePath,
  workspaceOrchestrationPlanArtifactPath,
  type ArtifactFileContent,
  type ArtifactTree,
  type OrchestrationPlanArtifact,
  type OrchestrationPlanStep,
} from "../artifacts-utils.ts";

export async function fetchArtifactTree(
  accessToken: string,
  workspaceId: number,
): Promise<ArtifactTree> {
  return apiFetch<ArtifactTree>(workspaceArtifactTreePath(workspaceId), { accessToken });
}

export async function fetchArtifactFile(
  accessToken: string,
  workspaceId: number,
  path: string,
): Promise<ArtifactFileContent> {
  return apiFetch<ArtifactFileContent>(workspaceArtifactFilePath(workspaceId, path), { accessToken });
}

export async function fetchOrchestrationPlanArtifact(
  accessToken: string,
  workspaceId: number,
  planId: number,
): Promise<OrchestrationPlanArtifact> {
  return apiFetch<OrchestrationPlanArtifact>(
    workspaceOrchestrationPlanArtifactPath(workspaceId, planId),
    { accessToken },
  );
}

export async function fetchOrchestrationPlanStepArtifact(
  accessToken: string,
  workspaceId: number,
  planId: number,
  stepId: number,
): Promise<OrchestrationPlanStep> {
  return apiFetch<OrchestrationPlanStep>(
    `/api/v1/workspaces/${workspaceId}/artifacts/orchestration-plans/${planId}/steps/${stepId}`,
    { accessToken },
  );
}
