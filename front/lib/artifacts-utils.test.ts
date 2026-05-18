import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import {
  flattenArtifactNodes,
  normalizeArtifactTree,
  workspaceArtifactFilePath,
  workspaceArtifactTreePath,
  workspaceOrchestrationPlanArtifactPath,
  type ArtifactTree,
} from "./artifacts-utils.ts";

const appRoot = process.cwd();

const tree: ArtifactTree = {
  workspaceId: 101,
  rootPath: "/",
  children: [
    {
      name: "reports",
      path: "reports",
      type: "DIRECTORY",
      children: [
        {
          name: "result.md",
          path: "reports/result.md",
          type: "FILE",
          contentType: "text/markdown",
          sizeBytes: 120,
        },
      ],
    },
  ],
};

test("workspace artifact paths target a single workspace", () => {
  assert.equal(workspaceArtifactTreePath(101), "/api/v1/workspaces/101/artifacts/tree");
  assert.equal(
    workspaceArtifactFilePath(101, "reports/result.md"),
    "/api/v1/workspaces/101/artifacts/files?path=reports%2Fresult.md",
  );
  assert.equal(
    workspaceOrchestrationPlanArtifactPath(101, 701),
    "/api/v1/workspaces/101/artifacts/orchestration-plans/701",
  );
});

test("artifact client exposes orchestration plan artifact contract", () => {
  const source = readFileSync(path.join(appRoot, "lib/artifacts.ts"), "utf8");

  assert.match(source, /type OrchestrationPlanArtifact/);
  assert.match(source, /fetchOrchestrationPlanArtifact/);
  assert.match(source, /workspaceOrchestrationPlanArtifactPath/);
});

test("normalizeArtifactTree accepts direct tree payloads", () => {
  assert.deepEqual(normalizeArtifactTree(tree), tree);
});

test("normalizeArtifactTree returns an empty root for invalid payloads", () => {
  assert.deepEqual(normalizeArtifactTree(null, 101), {
    workspaceId: 101,
    rootPath: "/",
    children: [],
  });
});

test("flattenArtifactNodes returns nested files with depth", () => {
  assert.deepEqual(
    flattenArtifactNodes(tree.children).map((item) => ({
      path: item.node.path,
      depth: item.depth,
    })),
    [
      { path: "reports", depth: 0 },
      { path: "reports/result.md", depth: 1 },
    ],
  );
});
