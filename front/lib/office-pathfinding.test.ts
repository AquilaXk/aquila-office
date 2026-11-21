import assert from "node:assert/strict";
import { performance } from "node:perf_hooks";
import test from "node:test";
import {
  DEFAULT_MAX_PATHFINDING_ITERATIONS,
  MAX_PATHFINDING_OBSTACLES,
  PATHFINDING_BITSET_WORDS,
  PATHFINDING_CELL_COUNT,
  PATHFINDING_GRID_COLS,
  PATHFINDING_GRID_ROWS,
  clearPathfindingBitgrid,
  createOfficePathfindingContext,
  findNearestWalkableCell,
  findOfficePath,
  hasBitgridLineOfSight,
  hasPathfindingCellBlocked,
  pctToGridX,
  pctToGridY,
  rasterizeWallRectsToBitgrid,
  setPathfindingCellBlocked,
  type WorkspaceWallRect,
} from "./office-pathfinding.ts";

function isSegmentClearOnBitgrid(
  blockedBits: Uint32Array,
  x0Pct: number,
  y0Pct: number,
  x1Pct: number,
  y1Pct: number,
): boolean {
  const gx0 = pctToGridX(x0Pct);
  const gy0 = pctToGridY(y0Pct);
  const gx1 = pctToGridX(x1Pct);
  const gy1 = pctToGridY(y1Pct);
  return hasBitgridLineOfSight(blockedBits, gx0, gy0, gx1, gy1);
}

test("office-pathfinding exposes 128x64 bitgrid constants and word layout", () => {
  assert.equal(PATHFINDING_GRID_COLS, 128);
  assert.equal(PATHFINDING_GRID_ROWS, 64);
  assert.equal(PATHFINDING_CELL_COUNT, 8192);
  assert.equal(PATHFINDING_BITSET_WORDS, 256);
  assert.equal(DEFAULT_MAX_PATHFINDING_ITERATIONS, 4096);

  const ctx = createOfficePathfindingContext();
  assert.equal(ctx.blockedBits.length, 256);
  assert.equal(ctx.openHeapNodes.length, 8192);
  assert.equal(ctx.openHeapF.length, 8192);
  assert.equal(ctx.gScore.length, 8192);
  assert.equal(ctx.parent.length, 8192);
  assert.equal(ctx.visitStamp.length, 8192);
  assert.equal(ctx.closedBits.length, 256);

  setPathfindingCellBlocked(ctx.blockedBits, 35, 17, true);
  assert.equal(hasPathfindingCellBlocked(ctx.blockedBits, 35, 17), true);
  // 17 << 2 = 68, 35 >>> 5 = 1 => word 69, bit (35 & 31) = 3
  assert.equal((ctx.blockedBits[(17 << 2) | (35 >>> 5)] & (1 << (35 & 31))) !== 0, true);

  clearPathfindingBitgrid(ctx.blockedBits);
  assert.equal(hasPathfindingCellBlocked(ctx.blockedBits, 35, 17), false);
});

test("findOfficePath routes cleanly around walls and desks through doorway", () => {
  const ctx = createOfficePathfindingContext();
  const obstacles: WorkspaceWallRect[] = [
    // Vertical wall at x=50%, leaving a doorway around y=72%..84%
    { leftPct: 48, topPct: 0, widthPct: 4, heightPct: 70 },
    { leftPct: 48, topPct: 86, widthPct: 4, heightPct: 14 },
    // Indoor desk obstacle blocking direct line inside left room
    { leftPct: 24, topPct: 42, widthPct: 12, heightPct: 18 },
  ];
  rasterizeWallRectsToBitgrid(ctx.blockedBits, obstacles);

  const path = findOfficePath(ctx, { x: 18, y: 25 }, { x: 82, y: 25 });
  assert.ok(path.length >= 3, `expected doorway detour waypoints, got ${path.length}`);

  const last = path[path.length - 1];
  assert.ok(Math.abs(last.x - 82) < 1.5 && Math.abs(last.y - 25) < 2);

  let passedThroughDoorway = false;
  for (let i = 0; i < path.length; i += 1) {
    if (path[i].x >= 46 && path[i].x <= 54 && path[i].y >= 68 && path[i].y <= 88) {
      passedThroughDoorway = true;
    }
  }
  assert.equal(passedThroughDoorway, true, "path must go through the doorway opening");

  for (let i = 1; i < path.length; i += 1) {
    assert.equal(
      isSegmentClearOnBitgrid(ctx.blockedBits, path[i - 1].x, path[i - 1].y, path[i].x, path[i].y),
      true,
      `segment ${i - 1}->${i} crossed a blocked cell`,
    );
  }
});

test("Bresenham LOS smoothing simplifies open paths and blocks corner clipping", () => {
  const ctx = createOfficePathfindingContext();
  const openPath = findOfficePath(ctx, { x: 10, y: 10 }, { x: 85, y: 78 });
  assert.equal(openPath.length, 2, "open diagonal path should smooth to [start, goal]");

  // Place a thin 1-cell L corner barrier that prevents diagonal corner cutting
  clearPathfindingBitgrid(ctx.blockedBits);
  setPathfindingCellBlocked(ctx.blockedBits, 20, 20, true);
  setPathfindingCellBlocked(ctx.blockedBits, 21, 20, true);
  setPathfindingCellBlocked(ctx.blockedBits, 20, 21, true);
  assert.equal(hasBitgridLineOfSight(ctx.blockedBits, 19, 21, 21, 19), false);
});

test("context visitStamp wraparound beyond 65535 iterations stays accurate", () => {
  const ctx = createOfficePathfindingContext();
  rasterizeWallRectsToBitgrid(ctx.blockedBits, [
    { leftPct: 45, topPct: 0, widthPct: 5, heightPct: 60 },
  ]);
  // Simulate near-overflow state of 16-bit visit stamp
  ctx.stamp = 0xfffe;
  ctx.visitStamp.fill(0xfffe);

  const pathBeforeWrap = findOfficePath(ctx, { x: 20, y: 20 }, { x: 70, y: 20 });
  const pathAtWrap = findOfficePath(ctx, { x: 20, y: 20 }, { x: 70, y: 20 });
  const pathAfterWrap = findOfficePath(ctx, { x: 20, y: 20 }, { x: 70, y: 20 });

  assert.ok(pathBeforeWrap.length >= 2);
  assert.ok(pathAtWrap.length >= 2);
  assert.ok(pathAfterWrap.length >= 2);
  assert.equal(ctx.stamp >= 1 && ctx.stamp < 10, true);
});

test("Fail-Closed bounds reject NaN, Infinity, out-of-range, blocked endpoints, and iteration cap", () => {
  const ctx = createOfficePathfindingContext();
  rasterizeWallRectsToBitgrid(ctx.blockedBits, [
    { leftPct: 40, topPct: 40, widthPct: 20, heightPct: 20 },
  ]);

  assert.deepEqual(findOfficePath(ctx, { x: Number.NaN, y: 20 }, { x: 80, y: 20 }), []);
  assert.deepEqual(findOfficePath(ctx, { x: 20, y: 20 }, { x: Number.POSITIVE_INFINITY, y: 20 }), []);
  assert.deepEqual(findOfficePath(ctx, { x: -1, y: 20 }, { x: 80, y: 20 }), []);
  assert.deepEqual(findOfficePath(ctx, { x: 20, y: 20 }, { x: 101, y: 20 }), []);

  // Goal inside blocked obstacle fails closed unless snapping is requested
  assert.deepEqual(findOfficePath(ctx, { x: 10, y: 10 }, { x: 50, y: 50 }), []);

  const snapped = findNearestWalkableCell(ctx.blockedBits, pctToGridX(50), pctToGridY(50), 16);
  assert.notEqual(snapped, null);
  const snappedPath = findOfficePath(
    ctx,
    { x: 10, y: 10 },
    { x: 50, y: 50 },
    { snapRadiusCells: 16 },
  );
  assert.ok(snappedPath.length >= 2);

  // Too small maxIterations fails closed cleanly
  assert.deepEqual(
    findOfficePath(ctx, { x: 5, y: 5 }, { x: 95, y: 95 }, { maxIterations: 5 }),
    [],
  );

  // Completely enclosed island -> exceeds maxIterations or exhausts open heap -> []
  clearPathfindingBitgrid(ctx.blockedBits);
  rasterizeWallRectsToBitgrid(ctx.blockedBits, [
    { leftPct: 0, topPct: 30, widthPct: 100, heightPct: 6 },
  ]);
  assert.deepEqual(findOfficePath(ctx, { x: 20, y: 10 }, { x: 20, y: 80 }), []);

  // Excessive obstacle array fails closed with explicit error
  const hostileObstacles: WorkspaceWallRect[] = new Array(MAX_PATHFINDING_OBSTACLES + 1).fill({
    leftPct: 10,
    topPct: 10,
    widthPct: 2,
    heightPct: 2,
  });
  assert.throws(() => rasterizeWallRectsToBitgrid(ctx.blockedBits, hostileObstacles), RangeError);
});

function naiveBaselineAStar(
  blockedBits: Uint32Array,
  startGx: number,
  startGy: number,
  goalGx: number,
  goalGy: number,
): number {
  const key = (x: number, y: number) => `${x},${y}`;
  const closed = new Set<string>();
  const gMap = new Map<string, number>();
  const open: Array<{ x: number; y: number; f: number }> = [{ x: startGx, y: startGy, f: 0 }];
  gMap.set(key(startGx, startGy), 0);
  let steps = 0;

  while (open.length > 0 && steps < DEFAULT_MAX_PATHFINDING_ITERATIONS) {
    let bestIdx = 0;
    for (let i = 1; i < open.length; i += 1) {
      if (open[i].f < open[bestIdx].f) bestIdx = i;
    }
    const cur = open.splice(bestIdx, 1)[0];
    if (cur.x === goalGx && cur.y === goalGy) {
      return steps + 1;
    }
    const cKey = key(cur.x, cur.y);
    if (closed.has(cKey)) continue;
    closed.add(cKey);
    steps += 1;

    const curG = gMap.get(cKey) ?? 0;
    for (let dy = -1; dy <= 1; dy += 1) {
      for (let dx = -1; dx <= 1; dx += 1) {
        if (dx === 0 && dy === 0) continue;
        const nx = cur.x + dx;
        const ny = cur.y + dy;
        if (nx < 0 || nx >= PATHFINDING_GRID_COLS || ny < 0 || ny >= PATHFINDING_GRID_ROWS) {
          continue;
        }
        if (hasPathfindingCellBlocked(blockedBits, nx, ny)) continue;
        if (
          dx !== 0 &&
          dy !== 0 &&
          (hasPathfindingCellBlocked(blockedBits, cur.x + dx, cur.y) ||
            hasPathfindingCellBlocked(blockedBits, cur.x, cur.y + dy))
        ) {
          continue;
        }
        const nKey = key(nx, ny);
        if (closed.has(nKey)) continue;
        const ng = curG + (dx !== 0 && dy !== 0 ? 14 : 10);
        const prev = gMap.get(nKey);
        if (prev === undefined || ng < prev) {
          gMap.set(nKey, ng);
          const adx = Math.abs(nx - goalGx);
          const ady = Math.abs(ny - goalGy);
          const h = adx < ady ? 14 * adx + 10 * (ady - adx) : 14 * ady + 10 * (adx - ady);
          open.push({ x: nx, y: ny, f: ng + h });
        }
      }
    }
  }
  return -1;
}

test("Benchmark: zero-alloc bitgrid A* outperforms naive Set/Object baseline", () => {
  const ctx = createOfficePathfindingContext();
  rasterizeWallRectsToBitgrid(ctx.blockedBits, [
    // Realistic multi-room office divider walls with central doorways + furniture
    { leftPct: 32, topPct: 0, widthPct: 3, heightPct: 48 },
    { leftPct: 32, topPct: 60, widthPct: 3, heightPct: 40 },
    { leftPct: 64, topPct: 0, widthPct: 3, heightPct: 32 },
    { leftPct: 64, topPct: 46, widthPct: 3, heightPct: 54 },
    { leftPct: 16, topPct: 42, widthPct: 10, heightPct: 14 },
    { leftPct: 45, topPct: 38, widthPct: 12, heightPct: 14 },
    { leftPct: 75, topPct: 32, widthPct: 10, heightPct: 14 },
  ]);

  const rounds = 4;
  const queriesPerRound = 90;
  const totalQueries = rounds * queriesPerRound;

  // Warmup both implementations before timing
  for (let i = 0; i < 40; i += 1) {
    findOfficePath(ctx, { x: 10, y: 15 }, { x: 92, y: 82 });
    naiveBaselineAStar(ctx.blockedBits, pctToGridX(10), pctToGridY(15), pctToGridX(92), pctToGridY(82));
  }

  let bitgridMs = 0;
  let naiveMs = 0;
  let bitgridFound = 0;
  let naiveFound = 0;

  // Interleave rounds so parallel test workers do not skew only one contestant
  for (let round = 0; round < rounds; round += 1) {
    const t0 = performance.now();
    for (let i = 0; i < queriesPerRound; i += 1) {
      const idx = round * queriesPerRound + i;
      const startY = 12 + (idx % 8) * 6;
      const goalY = 20 + (idx % 7) * 8;
      const path = findOfficePath(ctx, { x: 10, y: startY }, { x: 92, y: goalY });
      if (path.length >= 2) {
        bitgridFound += 1;
      }
    }
    bitgridMs += performance.now() - t0;

    const t1 = performance.now();
    for (let i = 0; i < queriesPerRound; i += 1) {
      const idx = round * queriesPerRound + i;
      const startY = 12 + (idx % 8) * 6;
      const goalY = 20 + (idx % 7) * 8;
      if (
        naiveBaselineAStar(
          ctx.blockedBits,
          pctToGridX(10),
          pctToGridY(startY),
          pctToGridX(92),
          pctToGridY(goalY),
        ) > 0
      ) {
        naiveFound += 1;
      }
    }
    naiveMs += performance.now() - t1;
  }

  assert.equal(bitgridFound, totalQueries);
  assert.equal(naiveFound, totalQueries);
  console.log(
    `[bench][office-pathfinding] ${totalQueries} queries: bitgrid=${bitgridMs.toFixed(2)}ms (${(bitgridMs / totalQueries).toFixed(3)}ms/op), naive=${naiveMs.toFixed(2)}ms, speedup=${(naiveMs / Math.max(0.01, bitgridMs)).toFixed(1)}x`,
  );
  assert.ok(
    bitgridMs * 2.5 < naiveMs,
    `expected bitgrid A* to be >=2.5x faster than naive baseline (bitgrid=${bitgridMs.toFixed(2)}ms, naive=${naiveMs.toFixed(2)}ms)`,
  );
});
