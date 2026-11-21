import assert from "node:assert/strict";
import { performance } from "node:perf_hooks";
import test from "node:test";
import {
  MAX_SPATIAL_POINTS,
  buildOfficeSpatialKdTree,
  createOfficeSpatialKdTree,
  type SpatialPointInput,
} from "./office-spatial-kdtree.ts";

function linearRangeScan(
  points: readonly SpatialPointInput[],
  minX: number,
  minY: number,
  maxX: number,
  maxY: number,
): number[] {
  const out: number[] = [];
  for (let i = 0; i < points.length; i += 1) {
    const p = points[i];
    const fx = Math.fround(p.x);
    const fy = Math.fround(p.y);
    if (fx >= minX && fx <= maxX && fy >= minY && fy <= maxY) {
      out.push(p.id);
    }
  }
  out.sort((a, b) => a - b);
  return out;
}

function linearWithinScan(
  points: readonly SpatialPointInput[],
  qx: number,
  qy: number,
  radius: number,
): number[] {
  const r2 = radius * radius;
  const out: number[] = [];
  for (let i = 0; i < points.length; i += 1) {
    const p = points[i];
    const dx = Math.fround(p.x) - qx;
    const dy = Math.fround(p.y) - qy;
    if (dx * dx + dy * dy <= r2) {
      out.push(p.id);
    }
  }
  out.sort((a, b) => a - b);
  return out;
}

test("office-spatial-kdtree builds flat TypedArrays and pre-allocated stack(64)", () => {
  const tree = createOfficeSpatialKdTree(128);
  assert.equal(MAX_SPATIAL_POINTS, 4096);
  assert.ok(tree.ids instanceof Uint16Array);
  assert.ok(tree.coords instanceof Float32Array);
  assert.ok(tree.stack instanceof Int32Array);
  assert.equal(tree.stack.length, 64);

  tree.rebuild([
    { id: 10, x: 20, y: 30 },
    { id: 20, x: 50, y: 50 },
    { id: 30, x: 80, y: 70 },
  ]);
  assert.equal(tree.count, 3);
});

test("range, within, and findNearestWithin match linear scan identically across random points", () => {
  // Deterministic LCG point set across [0, 100] x [0, 100]
  let seed = 0x13579bdf;
  const nextRand = () => {
    seed = (seed * 1664525 + 1013904223) >>> 0;
    return seed / 0xffffffff;
  };

  const points: SpatialPointInput[] = [];
  for (let i = 0; i < 600; i += 1) {
    points.push({
      id: i + 1,
      x: Math.fround(nextRand() * 100),
      y: Math.fround(nextRand() * 100),
    });
  }

  const tree = buildOfficeSpatialKdTree(points);
  const outIds = new Uint16Array(points.length);

  for (let q = 0; q < 25; q += 1) {
    const x0 = Math.fround(nextRand() * 75);
    const y0 = Math.fround(nextRand() * 75);
    const x1 = Math.fround(x0 + 5 + nextRand() * 20);
    const y1 = Math.fround(y0 + 5 + nextRand() * 20);

    const kdtreeCount = tree.range(x0, y0, x1, y1, outIds);
    const actualRange = Array.from(outIds.subarray(0, kdtreeCount)).sort((a, b) => a - b);
    const expectedRange = linearRangeScan(points, x0, y0, x1, y1);
    assert.deepEqual(actualRange, expectedRange);

    const cx = Math.fround(nextRand() * 100);
    const cy = Math.fround(nextRand() * 100);
    const radius = 8 + nextRand() * 12;

    const withinCount = tree.within(cx, cy, radius, outIds);
    const actualWithin = Array.from(outIds.subarray(0, withinCount)).sort((a, b) => a - b);
    const expectedWithin = linearWithinScan(points, cx, cy, radius);
    assert.deepEqual(actualWithin, expectedWithin);

    const nearestId = tree.findNearestIdWithin(cx, cy, radius);
    const nearest = tree.findNearestWithin(cx, cy, radius);
    let expectedBestId: number | null = null;
    let expectedBestDistSq = radius * radius;
    for (let i = 0; i < points.length; i += 1) {
      const p = points[i];
      const dx = p.x - cx;
      const dy = p.y - cy;
      const d2 = dx * dx + dy * dy;
      if (d2 <= expectedBestDistSq) {
        if (d2 < expectedBestDistSq || expectedBestId === null || p.id < expectedBestId) {
          expectedBestDistSq = d2;
          expectedBestId = p.id;
        }
      }
    }
    if (expectedBestId === null) {
      assert.equal(nearestId, -1);
      assert.equal(nearest, null);
    } else {
      assert.equal(nearestId, expectedBestId);
      assert.notEqual(nearest, null);
      assert.equal(nearest?.id, expectedBestId);
    }
  }
});

test("Fail-Closed guards handle >4096 points, NaN/Infinity points, invalid IDs, and bad queries", () => {
  const oversized: SpatialPointInput[] = new Array(MAX_SPATIAL_POINTS + 1);
  for (let i = 0; i < oversized.length; i += 1) {
    oversized[i] = { id: i, x: i % 100, y: Math.floor(i / 100) };
  }
  assert.throws(() => buildOfficeSpatialKdTree(oversized), RangeError);

  const tree = buildOfficeSpatialKdTree([
    { id: 1, x: 10, y: 10 },
    { id: 2, x: Number.NaN, y: 20 },
    { id: 3, x: 30, y: Number.POSITIVE_INFINITY },
    { id: -1, x: 15, y: 15 },
    { id: 70000, x: 15, y: 15 },
    { id: 4, x: 12, y: 12 },
  ]);
  assert.equal(tree.count, 2);

  const out = new Uint16Array(16);
  assert.equal(tree.range(Number.NaN, 0, 100, 100, out), 0);
  assert.equal(tree.range(50, 0, 10, 100, out), 0);
  assert.equal(tree.within(10, 10, -5, out), 0);
  assert.equal(tree.within(Number.POSITIVE_INFINITY, 10, 20, out), 0);
  assert.equal(tree.findNearestIdWithin(Number.NaN, 10, 10), -1);
  assert.equal(tree.findNearestIdWithin(80, 80, 5), -1);
  assert.equal(tree.findNearestWithin(Number.NaN, 10, 10), null);
  assert.equal(tree.findNearestWithin(80, 80, 5), null);
});

test("Benchmark: zero-alloc KD-tree queries outperform linear scan at 2000+ points", () => {
  let seed = 0x2468ace1;
  const nextRand = () => {
    seed = (seed * 1664525 + 1013904223) >>> 0;
    return seed / 0xffffffff;
  };

  const count = 3000;
  const points: SpatialPointInput[] = new Array(count);
  const flatCoords = new Float32Array(count * 2);
  const flatIds = new Uint16Array(count);
  for (let i = 0; i < count; i += 1) {
    const x = Math.fround(nextRand() * 1000);
    const y = Math.fround(nextRand() * 1000);
    points[i] = { id: i, x, y };
    flatCoords[i * 2] = x;
    flatCoords[i * 2 + 1] = y;
    flatIds[i] = i;
  }

  const tree = buildOfficeSpatialKdTree(points);
  const out = new Uint16Array(count);
  const queries = 4000;
  const queryCoords = new Float32Array(queries * 2);
  for (let q = 0; q < queries; q += 1) {
    queryCoords[q * 2] = Math.fround(nextRand() * 1000);
    queryCoords[q * 2 + 1] = Math.fround(nextRand() * 1000);
  }
  const radius = 25;
  const r2 = radius * radius;

  // Warmup both contestants
  for (let q = 0; q < 200; q += 1) {
    const qx = queryCoords[q * 2];
    const qy = queryCoords[q * 2 + 1];
    tree.within(qx, qy, radius, out);
    for (let i = 0; i < count; i += 1) {
      const dx = flatCoords[i * 2] - qx;
      const dy = flatCoords[i * 2 + 1] - qy;
      if (dx * dx + dy * dy <= r2) {
        out[0] = i;
      }
    }
  }

  const rounds = 4;
  const perRound = queries / rounds;
  let kdMs = 0;
  let linearMs = 0;
  let kdHits = 0;
  let linearHits = 0;

  for (let round = 0; round < rounds; round += 1) {
    const start = round * perRound;
    const end = start + perRound;

    const t0 = performance.now();
    for (let q = start; q < end; q += 1) {
      kdHits += tree.within(queryCoords[q * 2], queryCoords[q * 2 + 1], radius, out);
    }
    kdMs += performance.now() - t0;

    const t1 = performance.now();
    for (let q = start; q < end; q += 1) {
      const qx = queryCoords[q * 2];
      const qy = queryCoords[q * 2 + 1];
      for (let i = 0; i < count; i += 1) {
        const dx = flatCoords[i * 2] - qx;
        const dy = flatCoords[i * 2 + 1] - qy;
        if (dx * dx + dy * dy <= r2) {
          linearHits += 1;
        }
      }
    }
    linearMs += performance.now() - t1;
  }

  assert.equal(kdHits, linearHits);
  console.log(
    `[bench][office-spatial-kdtree] ${count} pts x ${queries} within queries: kdtree=${kdMs.toFixed(2)}ms, linear=${linearMs.toFixed(2)}ms, speedup=${(linearMs / Math.max(0.01, kdMs)).toFixed(1)}x`,
  );
  assert.ok(
    kdMs * 2 < linearMs,
    `expected KD-tree within queries to be >=2x faster than linear scan (kd=${kdMs.toFixed(2)}ms, linear=${linearMs.toFixed(2)}ms)`,
  );
});
