/**
 * Offscreen HTMLCanvasElement sprite cache and 1px pixel outline generator.
 *
 * Ported from pixel-agents-hq/pixel-agents (MIT License, Copyright (c) 2026 Pablo De Lucca).
 */

import type { SpriteData } from "./colorize.ts";

const zoomCaches = new Map<number, WeakMap<SpriteData, HTMLCanvasElement>>();
const outlineCache = new WeakMap<SpriteData, SpriteData>();

/** Generate a 1px crisp selection/hover outline SpriteData (+2px in each dimension). */
export function getOutlineSprite(sprite: SpriteData, outlineColor = "#FFFFFF"): SpriteData {
  if (outlineColor === "#FFFFFF") {
    const cached = outlineCache.get(sprite);
    if (cached) return cached;
  }

  const rows = sprite.length;
  const cols = rows > 0 ? sprite[0].length : 0;
  const outline: string[][] = Array.from({ length: rows + 2 }, () =>
    new Array<string>(cols + 2).fill(""),
  );

  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (!sprite[r][c]) continue;
      const er = r + 1;
      const ec = c + 1;
      if (!outline[er - 1][ec]) outline[er - 1][ec] = outlineColor;
      if (!outline[er + 1][ec]) outline[er + 1][ec] = outlineColor;
      if (!outline[er][ec - 1]) outline[er][ec - 1] = outlineColor;
      if (!outline[er][ec + 1]) outline[er][ec + 1] = outlineColor;
    }
  }

  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (sprite[r][c]) {
        outline[r + 1][c + 1] = "";
      }
    }
  }

  if (outlineColor === "#FFFFFF") {
    outlineCache.set(sprite, outline);
  }
  return outline;
}

/** Rasterize a hex SpriteData grid once per zoom factor and reuse the offscreen canvas. */
export function getCachedSprite(sprite: SpriteData, zoom = 1): HTMLCanvasElement {
  let cache = zoomCaches.get(zoom);
  if (!cache) {
    cache = new WeakMap<SpriteData, HTMLCanvasElement>();
    zoomCaches.set(zoom, cache);
  }

  const cached = cache.get(sprite);
  if (cached) return cached;

  const rows = sprite.length;
  const cols = rows > 0 ? sprite[0].length : 0;
  const canvas = document.createElement("canvas");
  canvas.width = Math.max(1, Math.round(cols * zoom));
  canvas.height = Math.max(1, Math.round(rows * zoom));
  const ctx = canvas.getContext("2d");
  if (ctx) {
    ctx.imageSmoothingEnabled = false;
    for (let r = 0; r < rows; r++) {
      const row = sprite[r];
      for (let c = 0; c < cols; c++) {
        const color = row[c];
        if (!color) continue;
        ctx.fillStyle = color;
        ctx.fillRect(Math.floor(c * zoom), Math.floor(r * zoom), Math.ceil(zoom), Math.ceil(zoom));
      }
    }
  }

  cache.set(sprite, canvas);
  return canvas;
}
