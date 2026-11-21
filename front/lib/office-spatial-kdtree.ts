/**
 * SPDX-License-Identifier: Apache-2.0
 *
 * Static flat-TypedArray 2D KD-tree with zero-allocation range, radius, and nearest queries.
 * Reference implementation: mourner/kdbush (ISC License, https://github.com/mourner/kdbush)
 * Adapted with reusable pre-allocated TypedArrays (ids: Uint16Array, coords: Float32Array, stack: Int32Array(64))
 * and fail-closed capacity/coordinate guards.
 */

export const MAX_SPATIAL_POINTS = 4096;
export const DEFAULT_KDTREE_NODE_SIZE = 16;
export const KDTREE_STACK_CAPACITY = 64;

export type SpatialPointInput = {
  id: number;
  x: number;
  y: number;
};

export type SpatialNearestMatch = {
  id: number;
  x: number;
  y: number;
  distSq: number;
};

function swapItem(ids: Uint16Array, coords: Float32Array, i: number, j: number): void {
  if (i === j) {
    return;
  }
  const tmpId = ids[i];
  ids[i] = ids[j];
  ids[j] = tmpId;

  const i2 = i << 1;
  const j2 = j << 1;
  const tx = coords[i2];
  const ty = coords[i2 + 1];
  coords[i2] = coords[j2];
  coords[i2 + 1] = coords[j2 + 1];
  coords[j2] = tx;
  coords[j2 + 1] = ty;
}

/**
 * Floyd-Rivest selection around k on coords[(idx << 1) + axis].
 * Derived from mourner/kdbush (ISC).
 */
function selectAxis(
  ids: Uint16Array,
  coords: Float32Array,
  k: number,
  leftInit: number,
  rightInit: number,
  axis: number,
): void {
  let left = leftInit;
  let right = rightInit;

  while (right > left) {
    if (right - left > 600) {
      const n = right - left + 1;
      const m = k - left + 1;
      const z = Math.log(n);
      const s = 0.5 * Math.exp((2 * z) / 3);
      const sd = 0.5 * Math.sqrt((z * s * (n - s)) / n) * (m - n / 2 < 0 ? -1 : 1);
      const newLeft = Math.max(left, Math.floor(k - (m * s) / n + sd));
      const newRight = Math.min(right, Math.floor(k + ((n - m) * s) / n + sd));
      selectAxis(ids, coords, k, newLeft, newRight, axis);
    }

    const t = coords[(k << 1) + axis];
    let i = left;
    let j = right;

    swapItem(ids, coords, left, k);
    if (coords[(right << 1) + axis] > t) {
      swapItem(ids, coords, left, right);
    }

    while (i < j) {
      swapItem(ids, coords, i, j);
      i += 1;
      j -= 1;
      while (coords[(i << 1) + axis] < t) {
        i += 1;
      }
      while (coords[(j << 1) + axis] > t) {
        j -= 1;
      }
    }

    if (coords[(left << 1) + axis] === t) {
      swapItem(ids, coords, left, j);
    } else {
      j += 1;
      swapItem(ids, coords, j, right);
    }

    if (j <= k) {
      left = j + 1;
    }
    if (k <= j) {
      right = j - 1;
    }
  }
}

export class OfficeSpatialKdTree {
  readonly capacity: number;
  readonly nodeSize: number;
  readonly ids: Uint16Array;
  readonly coords: Float32Array;
  readonly stack: Int32Array;
  count = 0;

  constructor(capacity = MAX_SPATIAL_POINTS, nodeSize = DEFAULT_KDTREE_NODE_SIZE) {
    if (!Number.isInteger(capacity) || capacity <= 0 || capacity > MAX_SPATIAL_POINTS) {
      throw new RangeError(
        `KD-tree capacity (${capacity}) must be an integer in [1, ${MAX_SPATIAL_POINTS}]`,
      );
    }
    this.capacity = capacity;
    this.nodeSize = Math.max(8, Math.min(64, Math.floor(nodeSize)));
    this.ids = new Uint16Array(capacity);
    this.coords = new Float32Array(capacity * 2);
    this.stack = new Int32Array(KDTREE_STACK_CAPACITY);
  }

  rebuild(points: readonly SpatialPointInput[]): number {
    if (!Array.isArray(points)) {
      throw new TypeError("points must be an array");
    }
    if (points.length > this.capacity) {
      throw new RangeError(
        `points length (${points.length}) exceeds KD-tree capacity (${this.capacity})`,
      );
    }

    let validCount = 0;
    for (let i = 0; i < points.length; i += 1) {
      const p = points[i];
      if (
        !Number.isInteger(p.id) ||
        p.id < 0 ||
        p.id > 0xffff ||
        !Number.isFinite(p.x) ||
        !Number.isFinite(p.y)
      ) {
        continue;
      }
      this.ids[validCount] = p.id;
      const base = validCount << 1;
      this.coords[base] = p.x;
      this.coords[base + 1] = p.y;
      validCount += 1;
    }

    this.count = validCount;
    if (validCount > 1) {
      this.sortIterative(0, validCount - 1);
    }
    return validCount;
  }

  private sortIterative(rootLeft: number, rootRight: number): void {
    const { ids, coords, nodeSize, stack } = this;
    let stackTop = 0;
    stack[stackTop++] = rootLeft;
    stack[stackTop++] = rootRight;
    stack[stackTop++] = 0;

    while (stackTop > 0) {
      const axis = stack[--stackTop];
      const right = stack[--stackTop];
      const left = stack[--stackTop];

      if (right - left <= nodeSize) {
        continue;
      }

      const mid = (left + right) >> 1;
      selectAxis(ids, coords, mid, left, right, axis);

      const nextAxis = 1 - axis;
      if (mid - 1 > left && stackTop + 3 <= stack.length) {
        stack[stackTop++] = left;
        stack[stackTop++] = mid - 1;
        stack[stackTop++] = nextAxis;
      }
      if (right > mid + 1 && stackTop + 3 <= stack.length) {
        stack[stackTop++] = mid + 1;
        stack[stackTop++] = right;
        stack[stackTop++] = nextAxis;
      }
    }
  }

  range(
    minX: number,
    minY: number,
    maxX: number,
    maxY: number,
    outIds: Uint16Array,
  ): number {
    if (
      this.count === 0 ||
      !Number.isFinite(minX) ||
      !Number.isFinite(minY) ||
      !Number.isFinite(maxX) ||
      !Number.isFinite(maxY) ||
      minX > maxX ||
      minY > maxY ||
      outIds.length === 0
    ) {
      return 0;
    }

    const { ids, coords, nodeSize, stack } = this;
    let outCount = 0;
    let stackTop = 0;
    stack[stackTop++] = 0;
    stack[stackTop++] = this.count - 1;
    stack[stackTop++] = 0;

    while (stackTop > 0) {
      const axis = stack[--stackTop];
      const right = stack[--stackTop];
      const left = stack[--stackTop];

      if (right - left <= nodeSize) {
        for (let i = left; i <= right; i += 1) {
          const base = i << 1;
          const x = coords[base];
          const y = coords[base + 1];
          if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
            outIds[outCount++] = ids[i];
            if (outCount >= outIds.length) {
              return outCount;
            }
          }
        }
        continue;
      }

      const mid = (left + right) >> 1;
      const base = mid << 1;
      const x = coords[base];
      const y = coords[base + 1];

      if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
        outIds[outCount++] = ids[mid];
        if (outCount >= outIds.length) {
          return outCount;
        }
      }

      const nextAxis = 1 - axis;
      const minVal = axis === 0 ? minX : minY;
      const maxVal = axis === 0 ? maxX : maxY;
      const splitVal = axis === 0 ? x : y;

      if (minVal <= splitVal && mid - 1 >= left && stackTop + 3 <= stack.length) {
        stack[stackTop++] = left;
        stack[stackTop++] = mid - 1;
        stack[stackTop++] = nextAxis;
      }
      if (maxVal >= splitVal && right >= mid + 1 && stackTop + 3 <= stack.length) {
        stack[stackTop++] = mid + 1;
        stack[stackTop++] = right;
        stack[stackTop++] = nextAxis;
      }
    }

    return outCount;
  }

  within(qx: number, qy: number, radius: number, outIds: Uint16Array): number {
    if (
      this.count === 0 ||
      !Number.isFinite(qx) ||
      !Number.isFinite(qy) ||
      !Number.isFinite(radius) ||
      radius < 0 ||
      outIds.length === 0
    ) {
      return 0;
    }

    const { ids, coords, nodeSize, stack } = this;
    const r2 = radius * radius;
    let outCount = 0;
    let stackTop = 0;
    stack[stackTop++] = 0;
    stack[stackTop++] = this.count - 1;
    stack[stackTop++] = 0;

    while (stackTop > 0) {
      const axis = stack[--stackTop];
      const right = stack[--stackTop];
      const left = stack[--stackTop];

      if (right - left <= nodeSize) {
        for (let i = left; i <= right; i += 1) {
          const base = i << 1;
          const dx = coords[base] - qx;
          const dy = coords[base + 1] - qy;
          if (dx * dx + dy * dy <= r2) {
            outIds[outCount++] = ids[i];
            if (outCount >= outIds.length) {
              return outCount;
            }
          }
        }
        continue;
      }

      const mid = (left + right) >> 1;
      const base = mid << 1;
      const x = coords[base];
      const y = coords[base + 1];
      const dx = x - qx;
      const dy = y - qy;

      if (dx * dx + dy * dy <= r2) {
        outIds[outCount++] = ids[mid];
        if (outCount >= outIds.length) {
          return outCount;
        }
      }

      const nextAxis = 1 - axis;
      const qCoord = axis === 0 ? qx : qy;
      const splitVal = axis === 0 ? x : y;

      if (qCoord - radius <= splitVal && mid - 1 >= left && stackTop + 3 <= stack.length) {
        stack[stackTop++] = left;
        stack[stackTop++] = mid - 1;
        stack[stackTop++] = nextAxis;
      }
      if (qCoord + radius >= splitVal && right >= mid + 1 && stackTop + 3 <= stack.length) {
        stack[stackTop++] = mid + 1;
        stack[stackTop++] = right;
        stack[stackTop++] = nextAxis;
      }
    }

    return outCount;
  }

  /**
   * Zero-allocation nearest point ID query within radius. Returns -1 when no point lies within radius.
   */
  findNearestIdWithin(qx: number, qy: number, radius: number): number {
    const idx = this.findNearestIndexWithin(qx, qy, radius);
    return idx >= 0 ? this.ids[idx] : -1;
  }

  private findNearestIndexWithin(qx: number, qy: number, radius: number): number {
    if (
      this.count === 0 ||
      !Number.isFinite(qx) ||
      !Number.isFinite(qy) ||
      !Number.isFinite(radius) ||
      radius < 0
    ) {
      return -1;
    }

    const { ids, coords, nodeSize, stack } = this;
    let bestDistSq = radius * radius;
    let bestIndex = -1;
    let bestId = -1;

    let stackTop = 0;
    stack[stackTop++] = 0;
    stack[stackTop++] = this.count - 1;
    stack[stackTop++] = 0;

    while (stackTop > 0) {
      const axis = stack[--stackTop];
      const right = stack[--stackTop];
      const left = stack[--stackTop];

      if (right - left <= nodeSize) {
        for (let i = left; i <= right; i += 1) {
          const base = i << 1;
          const dx = coords[base] - qx;
          const dy = coords[base + 1] - qy;
          const d2 = dx * dx + dy * dy;
          if (d2 <= bestDistSq) {
            const candId = ids[i];
            if (d2 < bestDistSq || bestIndex === -1 || candId < bestId) {
              bestDistSq = d2;
              bestIndex = i;
              bestId = candId;
            }
          }
        }
        continue;
      }

      const mid = (left + right) >> 1;
      const base = mid << 1;
      const x = coords[base];
      const y = coords[base + 1];
      const dx = x - qx;
      const dy = y - qy;
      const d2 = dx * dx + dy * dy;

      if (d2 <= bestDistSq) {
        const candId = ids[mid];
        if (d2 < bestDistSq || bestIndex === -1 || candId < bestId) {
          bestDistSq = d2;
          bestIndex = mid;
          bestId = candId;
        }
      }

      const nextAxis = 1 - axis;
      const diff = (axis === 0 ? qx : qy) - (axis === 0 ? x : y);
      const goLeftFirst = diff <= 0;
      const firstLeft = goLeftFirst ? left : mid + 1;
      const firstRight = goLeftFirst ? mid - 1 : right;
      const secondLeft = goLeftFirst ? mid + 1 : left;
      const secondRight = goLeftFirst ? right : mid - 1;

      // Push far branch first so near branch pops first (shrinks bestDistSq early)
      if (diff * diff <= bestDistSq && secondRight >= secondLeft && stackTop + 3 <= stack.length) {
        stack[stackTop++] = secondLeft;
        stack[stackTop++] = secondRight;
        stack[stackTop++] = nextAxis;
      }
      if (firstRight >= firstLeft && stackTop + 3 <= stack.length) {
        stack[stackTop++] = firstLeft;
        stack[stackTop++] = firstRight;
        stack[stackTop++] = nextAxis;
      }
    }

    return bestIndex;
  }

  findNearestWithin(qx: number, qy: number, radius: number): SpatialNearestMatch | null {
    const idx = this.findNearestIndexWithin(qx, qy, radius);
    if (idx < 0) {
      return null;
    }
    const base = idx << 1;
    const x = this.coords[base];
    const y = this.coords[base + 1];
    const dx = x - qx;
    const dy = y - qy;
    return {
      id: this.ids[idx],
      x,
      y,
      distSq: dx * dx + dy * dy,
    };
  }
}

export function createOfficeSpatialKdTree(
  capacity = MAX_SPATIAL_POINTS,
  nodeSize = DEFAULT_KDTREE_NODE_SIZE,
): OfficeSpatialKdTree {
  return new OfficeSpatialKdTree(capacity, nodeSize);
}

export function buildOfficeSpatialKdTree(
  points: readonly SpatialPointInput[],
  nodeSize = DEFAULT_KDTREE_NODE_SIZE,
): OfficeSpatialKdTree {
  if (!Array.isArray(points)) {
    throw new TypeError("points must be an array");
  }
  if (points.length > MAX_SPATIAL_POINTS) {
    throw new RangeError(
      `points length (${points.length}) exceeds MAX_SPATIAL_POINTS (${MAX_SPATIAL_POINTS})`,
    );
  }
  const tree = new OfficeSpatialKdTree(Math.max(1, points.length), nodeSize);
  tree.rebuild(points);
  return tree;
}
