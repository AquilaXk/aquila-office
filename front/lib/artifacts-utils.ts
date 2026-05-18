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

export function normalizeArtifactTree(payload: unknown, workspaceId = 0): ArtifactTree {
  if (payload && typeof payload === "object") {
    const record = payload as Partial<ArtifactTree>;
    if (typeof record.workspaceId === "number" && Array.isArray(record.children)) {
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
  return nodes.flatMap((node) => [
    { node, depth },
    ...flattenArtifactNodes(node.children ?? [], depth + 1),
  ]);
}
