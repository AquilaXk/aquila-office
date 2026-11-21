export type ArtifactNodeType = "DIRECTORY" | "FILE";

export type ArtifactNode = {
  name: string;
  path: string;
  type: ArtifactNodeType;
  contentType?: string | null;
  sizeBytes?: number | null;
  children?: ArtifactNode[];
};

export type ArtifactTree = {
  workspaceId: number;
  rootPath: string;
  children: ArtifactNode[];
};

export type ArtifactFileContent = {
  workspaceId: number;
  path: string;
  name: string;
  contentType?: string | null;
  sizeBytes?: number | null;
  content: string;
};

export type OrchestrationPlanFile = {
  path: string;
  name: string;
  contentType?: string | null;
  sizeBytes?: number | null;
  exists: boolean;
};

export type OrchestrationPlanStep = {
  stepId: number;
  stepKey: string;
  sequenceNo: number;
  title: string;
  files: OrchestrationPlanFile[];
};

export type OrchestrationPlanArtifact = {
  workspaceId: number;
  planId: number;
  steps: OrchestrationPlanStep[];
};

export type FlattenedArtifactNode = {
  node: ArtifactNode;
  depth: number;
};

export function workspaceArtifactTreePath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/artifacts/tree`;
}

export function workspaceArtifactFilePath(workspaceId: number, path: string): string {
  const params = new URLSearchParams({ path });
  return `/api/v1/workspaces/${workspaceId}/artifacts/files?${params.toString()}`;
}

export function workspaceOrchestrationPlanArtifactPath(workspaceId: number, planId: number): string {
  return `/api/v1/workspaces/${workspaceId}/artifacts/orchestration-plans/${planId}`;
}

export function workspaceOrchestrationPlanStepArtifactPath(
  workspaceId: number,
  planId: number,
  stepId: number,
): string {
  return `/api/v1/workspaces/${workspaceId}/artifacts/orchestration-plans/${planId}/steps/${stepId}`;
}

export const MAX_ARTIFACT_TREE_DEPTH = 16;
export const MAX_ARTIFACT_TREE_NODES = 2000;

export function normalizeArtifactTree(payload: unknown, workspaceId = 0): ArtifactTree {
  if (payload && typeof payload === "object") {
    const record = payload as Partial<ArtifactTree>;
    if (typeof record.workspaceId === "number" && Array.isArray(record.children)) {
      let nodeCount = 0;
      const validateChildren = (items: readonly ArtifactNode[], depth: number): void => {
        if (depth > MAX_ARTIFACT_TREE_DEPTH) {
          throw new RangeError(
            `Artifact tree depth (${depth}) exceeds MAX_ARTIFACT_TREE_DEPTH (${MAX_ARTIFACT_TREE_DEPTH})`,
          );
        }
        for (let i = 0; i < items.length; i += 1) {
          nodeCount += 1;
          if (nodeCount > MAX_ARTIFACT_TREE_NODES) {
            throw new RangeError(
              `Artifact tree node count exceeds MAX_ARTIFACT_TREE_NODES (${MAX_ARTIFACT_TREE_NODES})`,
            );
          }
          const node = items[i];
          if (node && Array.isArray(node.children) && node.children.length > 0) {
            validateChildren(node.children, depth + 1);
          }
        }
      };
      validateChildren(record.children, 0);

      return {
        workspaceId: record.workspaceId,
        rootPath: typeof record.rootPath === "string" ? record.rootPath : "/",
        children: record.children,
      };
    }
  }

  return {
    workspaceId,
    rootPath: "/",
    children: [],
  };
}

export function flattenArtifactNodes(
  nodes: readonly ArtifactNode[],
  depth = 0,
): FlattenedArtifactNode[] {
  if (!Number.isInteger(depth) || depth < 0 || depth > MAX_ARTIFACT_TREE_DEPTH) {
    throw new RangeError(
      `Artifact flatten start depth (${depth}) must be within [0, ${MAX_ARTIFACT_TREE_DEPTH}]`,
    );
  }
  const out: FlattenedArtifactNode[] = [];
  function walk(items: readonly ArtifactNode[], currentDepth: number) {
    if (currentDepth > MAX_ARTIFACT_TREE_DEPTH) {
      throw new RangeError(
        `Artifact tree depth (${currentDepth}) exceeds MAX_ARTIFACT_TREE_DEPTH (${MAX_ARTIFACT_TREE_DEPTH})`,
      );
    }
    for (let i = 0; i < items.length; i += 1) {
      if (out.length >= MAX_ARTIFACT_TREE_NODES) {
        throw new RangeError(
          `Artifact tree node count exceeds MAX_ARTIFACT_TREE_NODES (${MAX_ARTIFACT_TREE_NODES})`,
        );
      }
      const node = items[i];
      out.push({ node, depth: currentDepth });
      if (node.children && node.children.length > 0) {
        walk(node.children, currentDepth + 1);
      }
    }
  }
  walk(nodes, depth);
  return out;
}
