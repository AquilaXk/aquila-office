/**
 * Color math and sprite tinting helpers (DOM-free RGBA + hex SpriteData).
 *
 * Ported from pixel-agents-hq/pixel-agents (MIT License, Copyright (c) 2026 Pablo De Lucca).
 */

export type SpriteData = string[][];

export interface ColorValue {
  h: number;
  s: number;
  b: number;
  c: number;
  colorize?: boolean;
  palette?: string;
}

export type Rgb = [number, number, number];

export const PNG_ALPHA_THRESHOLD = 2;

const colorizeCache = new Map<string, SpriteData>();
const COLORIZE_CACHE_LIMIT = 512;

export function hexToRgb(hex: string): Rgb {
  const clean = hex.startsWith("#") ? hex.slice(1) : hex;
  const n = Number.parseInt(clean.slice(0, 6), 16);
  if (!Number.isFinite(n)) {
    return [0, 0, 0];
  }
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

const hex2 = (v: number): string => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, "0");

export function rgbHex(r: number, g: number, b: number): string {
  return `#${hex2(r)}${hex2(g)}${hex2(b)}`;
}

/** Perceived luminance (0..1) from 0..255 RGB channels. */
export function luminance(r: number, g: number, b: number): number {
  return (0.299 * r + 0.587 * g + 0.114 * b) / 255;
}

export function hexLum(hex: string): number {
  const [r, g, b] = hexToRgb(hex);
  return luminance(r, g, b);
}

/** Convert 0..255 RGB to HSL with h in degrees (0..360), s/l in 0..1. */
export function rgbToHslDegrees(r: number, g: number, b: number): [number, number, number] {
  const rf = r / 255;
  const gf = g / 255;
  const bf = b / 255;
  const max = Math.max(rf, gf, bf);
  const min = Math.min(rf, gf, bf);
  const l = (max + min) / 2;
  if (max === min) return [0, 0, l];
  const d = max - min;
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
  let h: number;
  if (max === rf) h = ((gf - bf) / d + (gf < bf ? 6 : 0)) * 60;
  else if (max === gf) h = ((bf - rf) / d + 2) * 60;
  else h = ((rf - gf) / d + 4) * 60;
  return [h, s, l];
}

/** Convert 0..255 RGB to HSL with all channels in 0..1. */
export function rgbToHsl01(r: number, g: number, b: number): [number, number, number] {
  const [h, s, l] = rgbToHslDegrees(r, g, b);
  return [h / 360, s, l];
}

export function hsl01ToRgb(h: number, s: number, l: number): Rgb {
  if (s === 0) {
    const v = Math.round(l * 255);
    return [v, v, v];
  }
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
  const p = 2 * l - q;
  const f = (tRaw: number): number => {
    const t = ((tRaw % 1) + 1) % 1;
    if (t < 1 / 6) return p + (q - p) * 6 * t;
    if (t < 1 / 2) return q;
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
    return p;
  };
  return [
    Math.round(f(h + 1 / 3) * 255),
    Math.round(f(h) * 255),
    Math.round(f(h - 1 / 3) * 255),
  ];
}

export function hslDegreesToRgb(h: number, s: number, l: number): Rgb {
  const c = (1 - Math.abs(2 * l - 1)) * s;
  const hp = (((h % 360) + 360) % 360) / 60;
  const x = c * (1 - Math.abs((hp % 2) - 1));
  let r1 = 0;
  let g1 = 0;
  let b1 = 0;
  if (hp < 1) [r1, g1, b1] = [c, x, 0];
  else if (hp < 2) [r1, g1, b1] = [x, c, 0];
  else if (hp < 3) [r1, g1, b1] = [0, c, x];
  else if (hp < 4) [r1, g1, b1] = [0, x, c];
  else if (hp < 5) [r1, g1, b1] = [x, 0, c];
  else [r1, g1, b1] = [c, 0, x];
  const m = l - c / 2;
  return [
    Math.round((r1 + m) * 255),
    Math.round((g1 + m) * 255),
    Math.round((b1 + m) * 255),
  ];
}

function clamp255(v: number): number {
  return Math.max(0, Math.min(255, v));
}

function hslDegreesToHex(h: number, s: number, l: number): string {
  const [r, g, b] = hslDegreesToRgb(h, s, l);
  return `#${clamp255(r).toString(16).padStart(2, "0")}${clamp255(g).toString(16).padStart(2, "0")}${clamp255(b).toString(16).padStart(2, "0")}`.toUpperCase();
}

export function wrapHue(h: number): number {
  return ((h % 360) + 360) % 360;
}

export function colorKey(cv: ColorValue): string {
  return `${cv.h}|${cv.s}|${cv.b}|${cv.c}|${cv.colorize ? 1 : 0}|${cv.palette ?? ""}`;
}

/**
 * Recolor an RGBA pixel buffer using exact palette swap table + H/S/B/C transform.
 */
export function tintPixels(
  src: Uint8ClampedArray,
  cv: ColorValue,
  swaps?: Readonly<Record<string, Readonly<Record<string, string>>>>,
): Uint8ClampedArray {
  const swap = cv.palette ? swaps?.[cv.palette] : undefined;
  const swapRgb = swap
    ? new Map<string, Rgb>(Object.entries(swap).map(([from, to]) => [from.toLowerCase(), hexToRgb(to)]))
    : null;
  const shifts = Boolean(cv.colorize || cv.h || cv.s || cv.b || cv.c);
  const out = new Uint8ClampedArray(src.length);
  const factor = (100 + cv.c) / 100;

  for (let i = 0; i < src.length; i += 4) {
    const a = src[i + 3];
    if (a < PNG_ALPHA_THRESHOLD) continue;
    let r0 = src[i];
    let g0 = src[i + 1];
    let b0 = src[i + 2];
    if (swapRgb) {
      const swapped = swapRgb.get(rgbHex(r0, g0, b0));
      if (swapped) {
        r0 = swapped[0];
        g0 = swapped[1];
        b0 = swapped[2];
      }
    }
    if (!shifts) {
      out[i] = r0;
      out[i + 1] = g0;
      out[i + 2] = b0;
      out[i + 3] = a;
      continue;
    }
    let h: number;
    let s: number;
    let l: number;
    if (cv.colorize) {
      h = wrapHue(cv.h) / 360;
      s = Math.max(0, Math.min(1, cv.s / 100));
      l = luminance(r0, g0, b0);
    } else {
      const [oh, os, ol] = rgbToHslDegrees(r0, g0, b0);
      h = wrapHue(oh + cv.h) / 360;
      s = Math.max(0, Math.min(1, os + cv.s / 100));
      l = ol;
    }
    if (cv.c !== 0) {
      l = 0.5 + (l - 0.5) * factor;
    }
    l = Math.max(0, Math.min(1, l + cv.b / 200));
    const [r, g, b] = hsl01ToRgb(h, s, l);
    out[i] = clamp255(r);
    out[i + 1] = clamp255(g);
    out[i + 2] = clamp255(b);
    out[i + 3] = a;
  }
  return out;
}

function extractAlpha(pixel: string): number {
  return pixel.length > 7 ? Number.parseInt(pixel.slice(7, 9), 16) : 255;
}

function appendAlpha(hex: string, alpha: number): string {
  if (alpha >= 255) return hex;
  return `${hex}${clamp255(alpha).toString(16).padStart(2, "0").toUpperCase()}`;
}

export function colorizeSprite(sprite: SpriteData, color: ColorValue): SpriteData {
  const { h, s, b, c } = color;
  const result: SpriteData = [];
  const satFrac = Math.max(0, Math.min(1, s / 100));
  const factor = (100 + c) / 100;

  for (let y = 0; y < sprite.length; y++) {
    const row = sprite[y];
    const newRow: string[] = new Array(row.length);
    for (let x = 0; x < row.length; x++) {
      const pixel = row[x];
      if (!pixel) {
        newRow[x] = "";
        continue;
      }
      const r = Number.parseInt(pixel.slice(1, 3), 16);
      const g = Number.parseInt(pixel.slice(3, 5), 16);
      const bv = Number.parseInt(pixel.slice(5, 7), 16);
      let lightness = luminance(r, g, bv);
      if (c !== 0) lightness = 0.5 + (lightness - 0.5) * factor;
      if (b !== 0) lightness += b / 200;
      lightness = Math.max(0, Math.min(1, lightness));
      const hex = hslDegreesToHex(h, satFrac, lightness);
      newRow[x] = appendAlpha(hex, extractAlpha(pixel));
    }
    result.push(newRow);
  }
  return result;
}

export function adjustSprite(sprite: SpriteData, color: ColorValue): SpriteData {
  const { h: hShift, s: sShift, b, c } = color;
  const result: SpriteData = [];
  const factor = (100 + c) / 100;

  for (let y = 0; y < sprite.length; y++) {
    const row = sprite[y];
    const newRow: string[] = new Array(row.length);
    for (let x = 0; x < row.length; x++) {
      const pixel = row[x];
      if (!pixel) {
        newRow[x] = "";
        continue;
      }
      const r = Number.parseInt(pixel.slice(1, 3), 16);
      const g = Number.parseInt(pixel.slice(3, 5), 16);
      const bv = Number.parseInt(pixel.slice(5, 7), 16);
      const [origH, origS, origL] = rgbToHslDegrees(r, g, bv);
      const newH = wrapHue(origH + hShift);
      const newS = Math.max(0, Math.min(1, origS + sShift / 100));
      let lightness = origL;
      if (c !== 0) lightness = 0.5 + (lightness - 0.5) * factor;
      if (b !== 0) lightness += b / 200;
      lightness = Math.max(0, Math.min(1, lightness));
      const hex = hslDegreesToHex(newH, newS, lightness);
      newRow[x] = appendAlpha(hex, extractAlpha(pixel));
    }
    result.push(newRow);
  }
  return result;
}

export function flatColorizeSprite(sprite: SpriteData, color: ColorValue): SpriteData {
  const { h, s, b, c } = color;
  let lightness = 0.5;
  if (c !== 0) lightness = 0.5 + (lightness - 0.5) * ((100 + c) / 100);
  if (b !== 0) lightness += b / 200;
  lightness = Math.max(0, Math.min(1, lightness));
  const hex = hslDegreesToHex(h, Math.max(0, Math.min(1, s / 100)), lightness);
  return sprite.map((row) =>
    row.map((pixel) => (pixel ? appendAlpha(hex, extractAlpha(pixel)) : "")),
  );
}

export function getColorizedSprite(cacheKey: string, sprite: SpriteData, color: ColorValue): SpriteData {
  const cached = colorizeCache.get(cacheKey);
  if (cached) return cached;
  if (colorizeCache.size > COLORIZE_CACHE_LIMIT) {
    colorizeCache.clear();
  }
  const result = color.colorize ? colorizeSprite(sprite, color) : adjustSprite(sprite, color);
  colorizeCache.set(cacheKey, result);
  return result;
}
