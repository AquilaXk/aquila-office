// API module alias for Next consumers: @/lib/api/artifacts
// Delegated functions keep the accessToken contract in the domain API module.
// type OrchestrationPlanArtifact remains part of this public client contract.
export {
  fetchArtifactFile,
  fetchArtifactTree,
  fetchOrchestrationPlanArtifact,
  fetchOrchestrationPlanStepArtifact,
} from "./api/artifacts.ts";
export type {
  ArtifactFileContent,
  ArtifactNode,
  ArtifactNodeType,
  ArtifactTree,
  FlattenedArtifactNode,
  OrchestrationPlanArtifact,
  OrchestrationPlanFile,
  OrchestrationPlanStep,
} from "./artifacts-utils.ts";
export {
  flattenArtifactNodes,
  normalizeArtifactTree,
  workspaceArtifactFilePath,
  workspaceArtifactTreePath,
  workspaceOrchestrationPlanArtifactPath,
  workspaceOrchestrationPlanStepArtifactPath,
} from "./artifacts-utils.ts";
