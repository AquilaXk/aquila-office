/**
 * Tilemap auto-tiling for walls (4-bit N/E/S/W mask) and carpets (4-corner marching squares).
 *
 * Ported from pixel-agents-hq/pixel-agents (MIT License, Copyright (c) 2026 Pablo De Lucca):
 * - webview-ui/src/office/wallTiles.ts
 * - webview-ui/src/office/sprites/carpetTiles.ts
 */

export const TileCell = {
  VOID: 0,
  WALL: 1,
  DOOR: 2,
  CORRIDOR: 3,
  FLOOR_OAK: 4,
  FLOOR_SLATE: 5,
  FLOOR_SAND: 6,
  FLOOR_WALNUT: 7,
} as const;

export type TileCellType = (typeof TileCell)[keyof typeof TileCell];

export interface CarpetZone {
  variant: number;
  order: number;
  mainHex: string;
  accentHex: string;
}

/**
 * 4-bit cardinal wall neighbor bitmask: N=1, E=2, S=4, W=8.
 * Used to select exact corners, T-junctions, caps, and wall face shadows.
 */
export function buildWallMask(col: number, row: number, grid: readonly (readonly TileCellType[])[]): number {
  const rows = grid.length;
  const cols = rows > 0 ? grid[0].length : 0;
  let mask = 0;
  if (row > 0 && grid[row - 1][col] === TileCell.WALL) mask |= 1; // N
  if (col < cols - 1 && grid[row][col + 1] === TileCell.WALL) mask |= 2; // E
  if (row < rows - 1 && grid[row + 1][col] === TileCell.WALL) mask |= 4; // S
  if (col > 0 && grid[row][col - 1] === TileCell.WALL) mask |= 8; // W
  return mask;
}

/**
 * 4-bit marching-squares case at corner junction `(jx, jy)`:
 * NW=1, NE=2, SE=4, SW=8 matching the carpet's key (`variant:order:mainHex`).
 */
export function carpetJunctionCase(
  jx: number,
  jy: number,
  key: string,
  carpets: readonly (CarpetZone | null)[],
  cols: number,
  rows: number,
): number {
  const matches = (c: number, r: number): boolean => {
    if (c < 0 || r < 0 || c >= cols || r >= rows) return false;
    const tile = carpets[r * cols + c];
    if (!tile) return false;
    return `${tile.variant}:${tile.mainHex}:${tile.accentHex}:${tile.order}` === key;
  };
  let msCase = 0;
  if (matches(jx - 1, jy - 1)) msCase |= 1; // NW
  if (matches(jx, jy - 1)) msCase |= 2; // NE
  if (matches(jx, jy)) msCase |= 4; // SE
  if (matches(jx - 1, jy)) msCase |= 8; // SW
  return msCase;
}
