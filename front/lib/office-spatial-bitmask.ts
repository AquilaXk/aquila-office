/**
 * 2D 타일 그리드 좌표(x, y: 0..255)를 단일 16-bit 부호 없는 정수로 패킹합니다.
 * 하위 8비트는 X 좌표, 상위 8비트는 Y 좌표를 저장합니다.
 */
export function packGridCoord(x: number, y: number): number {
  return (((y & 0xff) << 8) | (x & 0xff)) >>> 0;
}

export function unpackGridX(packed: number): number {
  return packed & 0xff;
}

export function unpackGridY(packed: number): number {
  return (packed >>> 8) & 0xff;
}

export type SpatialBitset = Uint32Array & {
  width: number;
  height: number;
  words: Uint32Array;
};

/**
 * 2D 공간 점유 상태를 워드당 32개 셀씩 압축 저장하는 Uint32Array 비트셋을 생성합니다.
 */
export function createSpatialBitset(width: number, height: number): SpatialBitset {
  const words = new Uint32Array(((width * height) + 31) >>> 5) as SpatialBitset;
  words.width = width;
  words.height = height;
  words.words = words;
  return words;
}

/**
 * 힙 재할당 없이 기존 SpatialBitset 워드 버퍼를 0으로 초기화해 재사용합니다.
 */
export function resetSpatialBitset(
  bitset: SpatialBitset | { words: Uint32Array },
): void {
  if ("words" in bitset && bitset.words) {
    bitset.words.fill(0);
    return;
  }
  (bitset as Uint32Array).fill(0);
}

export function setSpatialBit(
  bitset: Uint32Array,
  width: number,
  x: number,
  y: number,
): void {
  const idx = y * width + x;
  bitset[idx >>> 5] |= 1 << (idx & 31);
}

export function clearSpatialBit(
  bitset: Uint32Array,
  width: number,
  x: number,
  y: number,
): void {
  const idx = y * width + x;
  bitset[idx >>> 5] &= ~(1 << (idx & 31));
}

export function hasSpatialBit(
  bitset: Uint32Array,
  width: number,
  x: number,
  y: number,
): boolean {
  const idx = y * width + x;
  return (bitset[idx >>> 5] & (1 << (idx & 31))) !== 0;
}

/**
 * 퍼센트(0..100) 직사각형 영역(벽·방 경계)을 그리드 비트마스크에 래스터화합니다.
 */
export function rasterizeRectToSpatialBitset(
  bitset: SpatialBitset | { width: number; height: number; words: Uint32Array },
  leftPct: number,
  topPct: number,
  widthPct: number,
  heightPct: number,
): void {
  if (widthPct < 0 || heightPct < 0) {
    return;
  }
  const gridWidth = bitset.width || 40;
  const gridHeight = bitset.height || 24;
  const words = bitset.words ?? (bitset as Uint32Array);
  const clampedLeft = Math.max(0, Math.min(100, leftPct));
  const clampedTop = Math.max(0, Math.min(100, topPct));
  const clampedRight = Math.max(clampedLeft, Math.min(100, leftPct + widthPct));
  const clampedBottom = Math.max(clampedTop, Math.min(100, topPct + heightPct));

  const minX = Math.min(gridWidth - 1, Math.max(0, Math.floor((clampedLeft / 100) * gridWidth)));
  const maxX = Math.min(gridWidth - 1, Math.max(minX, Math.floor((clampedRight / 100) * gridWidth)));
  const minY = Math.min(gridHeight - 1, Math.max(0, Math.floor((clampedTop / 100) * gridHeight)));
  const maxY = Math.min(gridHeight - 1, Math.max(minY, Math.floor((clampedBottom / 100) * gridHeight)));

  for (let y = minY; y <= maxY; y += 1) {
    const rowOffset = y * gridWidth;
    for (let x = minX; x <= maxX; x += 1) {
      const idx = rowOffset + x;
      words[idx >>> 5] |= 1 << (idx & 31);
    }
  }
}

/**
 * 퍼센트(0..100) 좌표가 그리드 비트마스크에 점유되어 있는지 O(1) 비트 연산으로 판정합니다.
 */
export function isPctPointOccupiedInBitset(
  bitset: SpatialBitset | { width: number; height: number; words: Uint32Array },
  xPct: number,
  yPct: number,
): boolean {
  if (xPct < 0 || xPct > 100 || yPct < 0 || yPct > 100) {
    return false;
  }
  const gridWidth = bitset.width || 40;
  const gridHeight = bitset.height || 24;
  const gx = Math.min(gridWidth - 1, Math.max(0, Math.floor((xPct / 100) * gridWidth)));
  const gy = Math.min(gridHeight - 1, Math.max(0, Math.floor((yPct / 100) * gridHeight)));
  const idx = gy * gridWidth + gx;
  const words = bitset.words ?? (bitset as Uint32Array);
  return (words[idx >>> 5] & (1 << (idx & 31))) !== 0;
}

/**
 * 분기 없이 32비트 정수의 켜진 비트 수를 계산하는 SWAR(SIMD Within A Register) popcount 알고리즘입니다.
 */
export function popcount32(value: number): number {
  let v = value - ((value >>> 1) & 0x55555555);
  v = (v & 0x33333333) + ((v >>> 2) & 0x33333333);
  return (((v + (v >>> 4)) & 0x0f0f0f0f) * 0x01010101) >>> 24;
}

export function countOccupiedBits(
  bitset: Uint32Array | { words: Uint32Array },
): number {
  const words = "words" in bitset && bitset.words ? bitset.words : (bitset as Uint32Array);
  let total = 0;
  for (let i = 0; i < words.length; i += 1) {
    total += popcount32(words[i]);
  }
  return total;
}

/**
 * 가상 오피스 액터의 런타임 상태 플래그 비트 상수입니다.
 */
export const ACTOR_FLAG = {
  ONLINE: 1 << 0,
  AGENT: 1 << 1,
  SELECTED: 1 << 2,
  FOCUSED: 1 << 3,
  ACTIVE_TASK: 1 << 4,
  MOVING: 1 << 5,
  SELF: 1 << 6,
} as const;

/**
 * 액터의 상태 플래그(bits 0..7), 구역 ID(bits 8..11), 역할 ID(bits 12..15)를
 * 단일 32비트 부호 없는 정수로 패킹합니다.
 */
export function packActorBitfield(
  flags: number,
  zoneId: number,
  roleId: number,
): number {
  return (
    ((flags & 0xff) | ((zoneId & 0x0f) << 8) | ((roleId & 0x0f) << 12)) >>> 0
  );
}

export function unpackActorFlags(bitfield: number): number {
  return bitfield & 0xff;
}

export function unpackActorZoneId(bitfield: number): number {
  return (bitfield >>> 8) & 0x0f;
}

export function unpackActorRoleId(bitfield: number): number {
  return (bitfield >>> 12) & 0x0f;
}

export function matchesActorBitmask(
  bitfield: number,
  requiredFlags: number,
  forbiddenFlags = 0,
): boolean {
  return (
    (bitfield & requiredFlags) === requiredFlags &&
    (bitfield & forbiddenFlags) === 0
  );
}

export function packRgb24(r: number, g: number, b: number): number {
  return (((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff)) >>> 0;
}

export function unpackRgb24(rgb: number): [number, number, number] {
  return [(rgb >>> 16) & 0xff, (rgb >>> 8) & 0xff, rgb & 0xff];
}

/**
 * 부동소수점 연산 없이 정수 가중치(54 + 183 + 19 = 256)와 비트 시프트(>>> 8)만으로
 * Rec.709 체감 휘도(0..255)를 고속 계산합니다.
 */
export function fastPerceivedLuma(rgb: number): number {
  return (
    (((rgb >>> 16) & 0xff) * 54 +
      ((rgb >>> 8) & 0xff) * 183 +
      (rgb & 0xff) * 19) >>>
    8
  );
}
