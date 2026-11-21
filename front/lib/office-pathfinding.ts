/**
 * SPDX-License-Identifier: Apache-2.0
 *
 * Zero-allocation 128x64 bitgrid A* pathfinding and Bresenham line-of-sight smoothing.
 * Reference implementation: qiao/PathFinding.js (MIT License, https://github.com/qiao/PathFinding.js)
 * Adapted to fixed-size Uint32Array bitgrids (row << 2, 1 << (gx & 31)) and pre-allocated TypedArray buffers.
 */

export const PATHFINDING_GRID_COLS = 128;
export const PATHFINDING_GRID_ROWS = 64;
export const PATHFINDING_ROW_WORDS = PATHFINDING_GRID_COLS >>> 5; // 4 words (128 bits) per row
export const PATHFINDING_CELL_COUNT = PATHFINDING_GRID_COLS * PATHFINDING_GRID_ROWS; // 8192 cells
export const PATHFINDING_BITSET_WORDS = PATHFINDING_GRID_ROWS * PATHFINDING_ROW_WORDS; // 256 u32 words
export const DEFAULT_MAX_PATHFINDING_ITERATIONS = 4096;
export const MAX_PATHFINDING_OBSTACLES = 2048;

export type WorkspaceWallRect = {
  leftPct: number;
  topPct: number;
  widthPct: number;
  heightPct: number;
};

export type PathPoint = {
  x: number;
  y: number;
};

export type OfficePathfindingOptions = {
  maxIterations?: number;
  snapRadiusCells?: number;
  smooth?: boolean;
};

export type OfficePathfindingContext = {
  blockedBits: Uint32Array;
  openHeapNodes: Uint16Array;
  openHeapF: Uint32Array;
  heapPos: Int16Array;
  gScore: Uint32Array;
  parent: Int32Array;
  visitStamp: Uint16Array;
  closedBits: Uint32Array;
  rawPathBuffer: Uint16Array;
  smoothBuffer: Uint16Array;
  stamp: number;
};

// 8-direction neighbor offsets (orthogonal first, diagonal last)
const DIR_DX = new Int8Array([1, -1, 0, 0, 1, -1, 1, -1]);
const DIR_DY = new Int8Array([0, 0, 1, -1, 1, 1, -1, -1]);
const DIR_COST = new Uint8Array([10, 10, 10, 10, 14, 14, 14, 14]);

export function createOfficePathfindingContext(): OfficePathfindingContext {
  return {
    blockedBits: new Uint32Array(PATHFINDING_BITSET_WORDS),
    openHeapNodes: new Uint16Array(PATHFINDING_CELL_COUNT),
    openHeapF: new Uint32Array(PATHFINDING_CELL_COUNT),
    heapPos: new Int16Array(PATHFINDING_CELL_COUNT).fill(-1),
    gScore: new Uint32Array(PATHFINDING_CELL_COUNT),
    parent: new Int32Array(PATHFINDING_CELL_COUNT),
    visitStamp: new Uint16Array(PATHFINDING_CELL_COUNT),
    closedBits: new Uint32Array(PATHFINDING_BITSET_WORDS),
    rawPathBuffer: new Uint16Array(PATHFINDING_CELL_COUNT),
    smoothBuffer: new Uint16Array(PATHFINDING_CELL_COUNT),
    stamp: 0,
  };
}

export function clearPathfindingBitgrid(blockedBits: Uint32Array): void {
  blockedBits.fill(0);
}

export function pctToGridX(xPct: number): number {
  if (!Number.isFinite(xPct)) {
    return 0;
  }
  const cell = Math.floor((xPct / 100) * PATHFINDING_GRID_COLS);
  if (cell < 0) {
    return 0;
  }
  if (cell >= PATHFINDING_GRID_COLS) {
    return PATHFINDING_GRID_COLS - 1;
  }
  return cell;
}

export function pctToGridY(yPct: number): number {
  if (!Number.isFinite(yPct)) {
    return 0;
  }
  const cell = Math.floor((yPct / 100) * PATHFINDING_GRID_ROWS);
  if (cell < 0) {
    return 0;
  }
  if (cell >= PATHFINDING_GRID_ROWS) {
    return PATHFINDING_GRID_ROWS - 1;
  }
  return cell;
}

export function gridToPctX(gx: number): number {
  return ((gx + 0.5) / PATHFINDING_GRID_COLS) * 100;
}

export function gridToPctY(gy: number): number {
  return ((gy + 0.5) / PATHFINDING_GRID_ROWS) * 100;
}

export function setPathfindingCellBlocked(
  blockedBits: Uint32Array,
  gx: number,
  gy: number,
  blocked: boolean,
): void {
  if (gx < 0 || gx >= PATHFINDING_GRID_COLS || gy < 0 || gy >= PATHFINDING_GRID_ROWS) {
    return;
  }
  const wordIndex = (gy << 2) | (gx >>> 5);
  const bitMask = 1 << (gx & 31);
  if (blocked) {
    blockedBits[wordIndex] |= bitMask;
  } else {
    blockedBits[wordIndex] &= ~bitMask;
  }
}

export function hasPathfindingCellBlocked(
  blockedBits: Uint32Array,
  gx: number,
  gy: number,
): boolean {
  if (gx < 0 || gx >= PATHFINDING_GRID_COLS || gy < 0 || gy >= PATHFINDING_GRID_ROWS) {
    return true;
  }
  const wordIndex = (gy << 2) | (gx >>> 5);
  return (blockedBits[wordIndex] & (1 << (gx & 31))) !== 0;
}

export function rasterizeWallRectsToBitgrid(
  blockedBits: Uint32Array,
  obstacles: readonly WorkspaceWallRect[],
  clearExisting = false,
): void {
  if (!Array.isArray(obstacles)) {
    throw new TypeError("obstacles must be an array");
  }
  if (obstacles.length > MAX_PATHFINDING_OBSTACLES) {
    throw new RangeError(
      `obstacles length (${obstacles.length}) exceeds MAX_PATHFINDING_OBSTACLES (${MAX_PATHFINDING_OBSTACLES})`,
    );
  }
  if (clearExisting) {
    blockedBits.fill(0);
  }

  for (let i = 0; i < obstacles.length; i += 1) {
    const rect = obstacles[i];
    if (
      !Number.isFinite(rect.leftPct) ||
      !Number.isFinite(rect.topPct) ||
      !Number.isFinite(rect.widthPct) ||
      !Number.isFinite(rect.heightPct) ||
      rect.widthPct <= 0 ||
      rect.heightPct <= 0
    ) {
      continue;
    }

    const x0 = Math.max(0, Math.min(100, rect.leftPct));
    const y0 = Math.max(0, Math.min(100, rect.topPct));
    const x1 = Math.max(0, Math.min(100, rect.leftPct + rect.widthPct));
    const y1 = Math.max(0, Math.min(100, rect.topPct + rect.heightPct));
    if (x1 <= x0 || y1 <= y0) {
      continue;
    }

    const minGx = pctToGridX(x0);
    const maxGx = Math.max(minGx, pctToGridX(x1 - 1e-5));
    const minGy = pctToGridY(y0);
    const maxGy = Math.max(minGy, pctToGridY(y1 - 1e-5));

    const startWord = minGx >>> 5;
    const endWord = maxGx >>> 5;
    const startMask = (0xffffffff << (minGx & 31)) >>> 0;
    const endMask = (0xffffffff >>> (31 - (maxGx & 31))) >>> 0;

    if (startWord === endWord) {
      const combinedMask = (startMask & endMask) >>> 0;
      for (let gy = minGy; gy <= maxGy; gy += 1) {
        blockedBits[(gy << 2) | startWord] |= combinedMask;
      }
      continue;
    }

    for (let gy = minGy; gy <= maxGy; gy += 1) {
      const rowBase = gy << 2;
      blockedBits[rowBase | startWord] |= startMask;
      for (let w = startWord + 1; w < endWord; w += 1) {
        blockedBits[rowBase | w] = 0xffffffff;
      }
      blockedBits[rowBase | endWord] |= endMask;
    }
  }
}

export function findNearestWalkableCell(
  blockedBits: Uint32Array,
  originGx: number,
  originGy: number,
  maxRadiusCells = 4,
): { gx: number; gy: number } | null {
  if (!hasPathfindingCellBlocked(blockedBits, originGx, originGy)) {
    return { gx: originGx, gy: originGy };
  }
  const boundedRadius = Math.max(0, Math.min(32, Math.floor(maxRadiusCells)));
  let bestDistSq = Number.POSITIVE_INFINITY;
  let bestGx = -1;
  let bestGy = -1;

  for (let r = 1; r <= boundedRadius; r += 1) {
    const minY = Math.max(0, originGy - r);
    const maxY = Math.min(PATHFINDING_GRID_ROWS - 1, originGy + r);
    const minX = Math.max(0, originGx - r);
    const maxX = Math.min(PATHFINDING_GRID_COLS - 1, originGx + r);

    for (let gy = minY; gy <= maxY; gy += 1) {
      for (let gx = minX; gx <= maxX; gx += 1) {
        if (Math.abs(gx - originGx) !== r && Math.abs(gy - originGy) !== r) {
          continue;
        }
        if (hasPathfindingCellBlocked(blockedBits, gx, gy)) {
          continue;
        }
        const dx = gx - originGx;
        const dy = gy - originGy;
        const distSq = dx * dx + dy * dy;
        if (distSq < bestDistSq) {
          bestDistSq = distSq;
          bestGx = gx;
          bestGy = gy;
        }
      }
    }
    if (bestGx !== -1) {
      return { gx: bestGx, gy: bestGy };
    }
  }
  return null;
}

/**
 * Supercover-safe Bresenham line-of-sight test on the 128x64 bitgrid.
 * Checks diagonal side cells whenever both X and Y step together so paths never clip wall corners.
 */
export function hasBitgridLineOfSight(
  blockedBits: Uint32Array,
  x0: number,
  y0: number,
  x1: number,
  y1: number,
): boolean {
  let cx = x0;
  let cy = y0;
  const dx = Math.abs(x1 - x0);
  const dy = Math.abs(y1 - y0);
  const sx = x0 < x1 ? 1 : -1;
  const sy = y0 < y1 ? 1 : -1;
  let err = dx - dy;

  for (let guard = 0; guard <= PATHFINDING_GRID_COLS + PATHFINDING_GRID_ROWS; guard += 1) {
    if (hasPathfindingCellBlocked(blockedBits, cx, cy)) {
      return false;
    }
    if (cx === x1 && cy === y1) {
      return true;
    }
    const e2 = err << 1;
    const stepX = e2 > -dy;
    const stepY = e2 < dx;
    if (stepX && stepY) {
      if (
        hasPathfindingCellBlocked(blockedBits, cx + sx, cy) ||
        hasPathfindingCellBlocked(blockedBits, cx, cy + sy)
      ) {
        return false;
      }
    }
    if (stepX) {
      err -= dy;
      cx += sx;
    }
    if (stepY) {
      err += dx;
      cy += sy;
    }
  }
  return false;
}

function heapSwap(
  openHeapNodes: Uint16Array,
  openHeapF: Uint32Array,
  heapPos: Int16Array,
  i: number,
  j: number,
): void {
  const nodeI = openHeapNodes[i];
  const nodeJ = openHeapNodes[j];
  const fI = openHeapF[i];
  const fJ = openHeapF[j];
  openHeapNodes[i] = nodeJ;
  openHeapNodes[j] = nodeI;
  openHeapF[i] = fJ;
  openHeapF[j] = fI;
  heapPos[nodeI] = j;
  heapPos[nodeJ] = i;
}

function heapSiftUp(
  openHeapNodes: Uint16Array,
  openHeapF: Uint32Array,
  heapPos: Int16Array,
  idx: number,
): void {
  let pos = idx;
  while (pos > 0) {
    const parentPos = (pos - 1) >>> 1;
    if (openHeapF[pos] >= openHeapF[parentPos]) {
      break;
    }
    heapSwap(openHeapNodes, openHeapF, heapPos, pos, parentPos);
    pos = parentPos;
  }
}

function heapSiftDown(
  openHeapNodes: Uint16Array,
  openHeapF: Uint32Array,
  heapPos: Int16Array,
  size: number,
  idx: number,
): void {
  let pos = idx;
  while (true) {
    const left = (pos << 1) + 1;
    if (left >= size) {
      break;
    }
    const right = left + 1;
    let smallest = left;
    if (right < size && openHeapF[right] < openHeapF[left]) {
      smallest = right;
    }
    if (openHeapF[pos] <= openHeapF[smallest]) {
      break;
    }
    heapSwap(openHeapNodes, openHeapF, heapPos, pos, smallest);
    pos = smallest;
  }
}

function octileHeuristic(gx: number, gy: number, goalGx: number, goalGy: number): number {
  const dx = gx > goalGx ? gx - goalGx : goalGx - gx;
  const dy = gy > goalGy ? gy - goalGy : goalGy - gy;
  return dx < dy ? 14 * dx + 10 * (dy - dx) : 14 * dy + 10 * (dx - dy);
}

function nextSearchStamp(ctx: OfficePathfindingContext): number {
  ctx.stamp = (ctx.stamp + 1) & 0xffff;
  if (ctx.stamp === 0) {
    ctx.visitStamp.fill(0);
    ctx.stamp = 1;
  }
  ctx.closedBits.fill(0);
  return ctx.stamp;
}

export function findOfficePath(
  ctx: OfficePathfindingContext,
  startPct: PathPoint,
  goalPct: PathPoint,
  options: OfficePathfindingOptions = {},
): PathPoint[] {
  if (
    !Number.isFinite(startPct.x) ||
    !Number.isFinite(startPct.y) ||
    !Number.isFinite(goalPct.x) ||
    !Number.isFinite(goalPct.y) ||
    startPct.x < 0 ||
    startPct.x > 100 ||
    startPct.y < 0 ||
    startPct.y > 100 ||
    goalPct.x < 0 ||
    goalPct.x > 100 ||
    goalPct.y < 0 ||
    goalPct.y > 100
  ) {
    return [];
  }

  const requestedMax = options.maxIterations ?? DEFAULT_MAX_PATHFINDING_ITERATIONS;
  if (!Number.isFinite(requestedMax) || requestedMax <= 0) {
    return [];
  }
  const maxIterations = Math.min(DEFAULT_MAX_PATHFINDING_ITERATIONS, Math.floor(requestedMax));
  const snapRadius = options.snapRadiusCells ?? 0;

  let startGx = pctToGridX(startPct.x);
  let startGy = pctToGridY(startPct.y);
  let goalGx = pctToGridX(goalPct.x);
  let goalGy = pctToGridY(goalPct.y);

  let goalSnapped = false;
  if (hasPathfindingCellBlocked(ctx.blockedBits, startGx, startGy)) {
    if (snapRadius <= 0) {
      return [];
    }
    const snappedStart = findNearestWalkableCell(ctx.blockedBits, startGx, startGy, snapRadius);
    if (!snappedStart) {
      return [];
    }
    startGx = snappedStart.gx;
    startGy = snappedStart.gy;
  }

  if (hasPathfindingCellBlocked(ctx.blockedBits, goalGx, goalGy)) {
    if (snapRadius <= 0) {
      return [];
    }
    const snappedGoal = findNearestWalkableCell(ctx.blockedBits, goalGx, goalGy, snapRadius);
    if (!snappedGoal) {
      return [];
    }
    goalGx = snappedGoal.gx;
    goalGy = snappedGoal.gy;
    goalSnapped = true;
  }

  const finalGoalPoint: PathPoint = goalSnapped
    ? { x: gridToPctX(goalGx), y: gridToPctY(goalGy) }
    : { x: goalPct.x, y: goalPct.y };

  if (startGx === goalGx && startGy === goalGy) {
    return [{ x: startPct.x, y: startPct.y }, finalGoalPoint];
  }

  const startIdx = (startGy << 7) | startGx;
  const goalIdx = (goalGy << 7) | goalGx;

  const stamp = nextSearchStamp(ctx);
  const {
    blockedBits,
    openHeapNodes,
    openHeapF,
    heapPos,
    gScore,
    parent,
    visitStamp,
    closedBits,
    rawPathBuffer,
    smoothBuffer,
  } = ctx;

  visitStamp[startIdx] = stamp;
  gScore[startIdx] = 0;
  parent[startIdx] = -1;
  openHeapNodes[0] = startIdx;
  openHeapF[0] = octileHeuristic(startGx, startGy, goalGx, goalGy);
  heapPos[startIdx] = 0;
  let heapSize = 1;

  let found = false;
  let iterations = 0;

  while (heapSize > 0) {
    if (iterations >= maxIterations) {
      return [];
    }
    iterations += 1;

    const currentIdx = openHeapNodes[0];
    heapPos[currentIdx] = -1;
    heapSize -= 1;
    if (heapSize > 0) {
      const tailNode = openHeapNodes[heapSize];
      openHeapNodes[0] = tailNode;
      openHeapF[0] = openHeapF[heapSize];
      heapPos[tailNode] = 0;
      heapSiftDown(openHeapNodes, openHeapF, heapPos, heapSize, 0);
    }

    if (currentIdx === goalIdx) {
      found = true;
      break;
    }

    closedBits[currentIdx >>> 5] |= 1 << (currentIdx & 31);

    const cx = currentIdx & 127;
    const cy = currentIdx >>> 7;
    const currentG = gScore[currentIdx];

    for (let dir = 0; dir < 8; dir += 1) {
      const dx = DIR_DX[dir];
      const dy = DIR_DY[dir];
      const nx = cx + dx;
      const ny = cy + dy;

      if (nx < 0 || nx >= PATHFINDING_GRID_COLS || ny < 0 || ny >= PATHFINDING_GRID_ROWS) {
        continue;
      }
      if (hasPathfindingCellBlocked(blockedBits, nx, ny)) {
        continue;
      }
      // Prevent diagonal corner cutting across walls
      if (dx !== 0 && dy !== 0) {
        if (
          hasPathfindingCellBlocked(blockedBits, cx + dx, cy) ||
          hasPathfindingCellBlocked(blockedBits, cx, cy + dy)
        ) {
          continue;
        }
      }

      const neighborIdx = (ny << 7) | nx;
      if ((closedBits[neighborIdx >>> 5] & (1 << (neighborIdx & 31))) !== 0) {
        continue;
      }

      const tentativeG = currentG + DIR_COST[dir];
      const wasVisited = visitStamp[neighborIdx] === stamp;
      if (!wasVisited || tentativeG < gScore[neighborIdx]) {
        visitStamp[neighborIdx] = stamp;
        gScore[neighborIdx] = tentativeG;
        parent[neighborIdx] = currentIdx;
        const fScore = tentativeG + octileHeuristic(nx, ny, goalGx, goalGy);

        if (!wasVisited || heapPos[neighborIdx] < 0) {
          const insertAt = heapSize;
          openHeapNodes[insertAt] = neighborIdx;
          openHeapF[insertAt] = fScore;
          heapPos[neighborIdx] = insertAt;
          heapSize += 1;
          heapSiftUp(openHeapNodes, openHeapF, heapPos, insertAt);
        } else {
          const existingPos = heapPos[neighborIdx];
          openHeapF[existingPos] = fScore;
          heapSiftUp(openHeapNodes, openHeapF, heapPos, existingPos);
        }
      }
    }
  }

  if (!found) {
    return [];
  }

  // Reconstruct reversed raw cell sequence, then flip in-place into chronological order
  let rawCount = 0;
  let trace = goalIdx;
  while (trace !== -1 && rawCount < PATHFINDING_CELL_COUNT) {
    rawPathBuffer[rawCount] = trace;
    rawCount += 1;
    if (trace === startIdx) {
      break;
    }
    trace = parent[trace];
  }
  if (rawCount === 0) {
    return [];
  }
  for (let i = 0, j = rawCount - 1; i < j; i += 1, j -= 1) {
    const tmp = rawPathBuffer[i];
    rawPathBuffer[i] = rawPathBuffer[j];
    rawPathBuffer[j] = tmp;
  }

  const smooth = options.smooth ?? true;
  if (!smooth || rawCount <= 2) {
    const out: PathPoint[] = new Array(rawCount);
    for (let i = 0; i < rawCount; i += 1) {
      const idx = rawPathBuffer[i];
      out[i] =
        i === rawCount - 1
          ? finalGoalPoint
          : i === 0
            ? { x: startPct.x, y: startPct.y }
            : { x: gridToPctX(idx & 127), y: gridToPctY(idx >>> 7) };
    }
    return out;
  }

  // Bresenham line-of-sight shortcut smoothing
  let smoothCount = 0;
  let anchor = 0;
  smoothBuffer[smoothCount] = rawPathBuffer[0];
  smoothCount += 1;

  while (anchor < rawCount - 1) {
    const ax = rawPathBuffer[anchor] & 127;
    const ay = rawPathBuffer[anchor] >>> 7;
    let furthest = anchor + 1;

    for (let probe = rawCount - 1; probe > anchor + 1; probe -= 1) {
      const px = rawPathBuffer[probe] & 127;
      const py = rawPathBuffer[probe] >>> 7;
      if (hasBitgridLineOfSight(blockedBits, ax, ay, px, py)) {
        furthest = probe;
        break;
      }
    }
    smoothBuffer[smoothCount] = rawPathBuffer[furthest];
    smoothCount += 1;
    anchor = furthest;
  }

  const out: PathPoint[] = new Array(smoothCount);
  for (let i = 0; i < smoothCount; i += 1) {
    if (i === 0) {
      out[i] = { x: startPct.x, y: startPct.y };
    } else if (i === smoothCount - 1) {
      out[i] = finalGoalPoint;
    } else {
      const idx = smoothBuffer[i];
      out[i] = { x: gridToPctX(idx & 127), y: gridToPctY(idx >>> 7) };
    }
  }
  return out;
}
