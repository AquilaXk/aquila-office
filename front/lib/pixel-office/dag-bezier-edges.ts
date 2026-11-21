/**
 * Cubic-Bezier DAG Edge Path Calculator & Deterministic Topological Level Layout.
 *
 * Bezier control offset & path midpoint math adapted from xyflow/xyflow
 * (packages/system/src/utils/edges/bezier-edge.ts, MIT License, Copyright (c) 2019-2026 webkid GmbH).
 */

export interface DagStepInput {
  stepKey: string;
  dependsOn: readonly string[];
  status: string;
}

export interface DagLayoutOptions {
  nodeWidth?: number;
  nodeHeight?: number;
  colGap?: number;
  rowGap?: number;
  padX?: number;
  padY?: number;
  bottomPad?: number;
  curvature?: number;
}

export interface DagNodeBox {
  stepKey: string;
  level: number;
  row: number;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface DagBezierEdge {
  id: string;
  fromKey: string;
  toKey: string;
  status: string;
  path: string;
  labelX: number;
  labelY: number;
}

export interface DagLevelHeader {
  level: number;
  x: number;
  width: number;
  stepKeys: string[];
}

export interface TopologicalDagLayout {
  width: number;
  height: number;
  levels: DagLevelHeader[];
  nodes: DagNodeBox[];
  edges: DagBezierEdge[];
}

export function calculateControlOffset(distance: number, curvature: number): number {
  if (distance >= 0) {
    return 0.5 * distance;
  }
  return curvature * 25 * Math.sqrt(-distance);
}

export function getHorizontalBezierPath(params: {
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
  curvature?: number;
}): { path: string; labelX: number; labelY: number } {
  const { sourceX, sourceY, targetX, targetY, curvature = 0.25 } = params;
  const xOffset = calculateControlOffset(targetX - sourceX, curvature);
  const sourceControlX = sourceX + xOffset;
  const targetControlX = targetX - xOffset;

  const labelX =
    0.125 * sourceX + 0.375 * sourceControlX + 0.375 * targetControlX + 0.125 * targetX;
  const labelY = 0.5 * (sourceY + targetY);

  return {
    path: `M ${sourceX},${sourceY} C ${sourceControlX},${sourceY} ${targetControlX},${targetY} ${targetX},${targetY}`,
    labelX,
    labelY,
  };
}

export function layoutTopologicalDag(
  steps: readonly DagStepInput[],
  options: DagLayoutOptions = {},
): TopologicalDagLayout {
  if (steps.length === 0) {
    return { width: 0, height: 0, levels: [], nodes: [], edges: [] };
  }

  const nodeWidth = options.nodeWidth ?? 216;
  const nodeHeight = options.nodeHeight ?? 88;
  const colGap = options.colGap ?? 68;
  const rowGap = options.rowGap ?? 18;
  const padX = options.padX ?? 14;
  const padY = options.padY ?? 34;
  const bottomPad = options.bottomPad ?? 14;
  const curvature = options.curvature ?? 0.25;

  const stepByKey = new Map<string, DagStepInput>();
  for (let i = 0; i < steps.length; i += 1) {
    stepByKey.set(steps[i].stepKey, steps[i]);
  }

  const levelMemo = new Map<string, number>();
  const visiting = new Set<string>();

  function resolveLevel(stepKey: string): number {
    const cached = levelMemo.get(stepKey);
    if (cached !== undefined) return cached;
    if (visiting.has(stepKey)) return 0;
    visiting.add(stepKey);
    const step = stepByKey.get(stepKey);
    let level = 0;
    if (step) {
      for (let i = 0; i < step.dependsOn.length; i += 1) {
        const depKey = step.dependsOn[i];
        if (stepByKey.has(depKey)) {
          level = Math.max(level, resolveLevel(depKey) + 1);
        }
      }
    }
    visiting.delete(stepKey);
    levelMemo.set(stepKey, level);
    return level;
  }

  let maxLevel = 0;
  for (let i = 0; i < steps.length; i += 1) {
    const lvl = resolveLevel(steps[i].stepKey);
    if (lvl > maxLevel) maxLevel = lvl;
  }

  const buckets: DagStepInput[][] = Array.from({ length: maxLevel + 1 }, () => []);
  for (let i = 0; i < steps.length; i += 1) {
    const lvl = levelMemo.get(steps[i].stepKey) ?? 0;
    buckets[lvl].push(steps[i]);
  }

  const nonEmptyBuckets = buckets.filter((b) => b.length > 0);
  let maxRows = 1;
  for (let i = 0; i < nonEmptyBuckets.length; i += 1) {
    if (nonEmptyBuckets[i].length > maxRows) {
      maxRows = nonEmptyBuckets[i].length;
    }
  }

  const contentHeight = maxRows * nodeHeight + Math.max(0, maxRows - 1) * rowGap;
  const width =
    padX * 2 +
    nonEmptyBuckets.length * nodeWidth +
    Math.max(0, nonEmptyBuckets.length - 1) * colGap;
  const height = padY + contentHeight + bottomPad;

  const nodes: DagNodeBox[] = [];
  const levels: DagLevelHeader[] = [];
  const nodeByKey = new Map<string, DagNodeBox>();

  for (let col = 0; col < nonEmptyBuckets.length; col += 1) {
    const colSteps = nonEmptyBuckets[col];
    const colX = padX + col * (nodeWidth + colGap);
    const colHeight = colSteps.length * nodeHeight + Math.max(0, colSteps.length - 1) * rowGap;
    const startY = padY + Math.round((contentHeight - colHeight) / 2);

    levels.push({
      level: col,
      x: colX,
      width: nodeWidth,
      stepKeys: colSteps.map((s) => s.stepKey),
    });

    for (let row = 0; row < colSteps.length; row += 1) {
      const step = colSteps[row];
      const box: DagNodeBox = {
        stepKey: step.stepKey,
        level: col,
        row,
        x: colX,
        y: startY + row * (nodeHeight + rowGap),
        width: nodeWidth,
        height: nodeHeight,
      };
      nodes.push(box);
      nodeByKey.set(step.stepKey, box);
    }
  }

  const edges: DagBezierEdge[] = [];
  for (let i = 0; i < steps.length; i += 1) {
    const targetStep = steps[i];
    const targetBox = nodeByKey.get(targetStep.stepKey);
    if (!targetBox) continue;

    for (let j = 0; j < targetStep.dependsOn.length; j += 1) {
      const fromKey = targetStep.dependsOn[j];
      const sourceBox = nodeByKey.get(fromKey);
      if (!sourceBox || fromKey === targetStep.stepKey) continue;

      const sourceX = sourceBox.x + sourceBox.width;
      const sourceY = sourceBox.y + Math.round(sourceBox.height / 2);
      const targetX = targetBox.x;
      const targetY = targetBox.y + Math.round(targetBox.height / 2);
      const { path, labelX, labelY } = getHorizontalBezierPath({
        sourceX,
        sourceY,
        targetX,
        targetY,
        curvature,
      });

      edges.push({
        id: `${fromKey}->${targetStep.stepKey}`,
        fromKey,
        toKey: targetStep.stepKey,
        status: targetStep.status,
        path,
        labelX,
        labelY,
      });
    }
  }

  return { width, height, levels, nodes, edges };
}
