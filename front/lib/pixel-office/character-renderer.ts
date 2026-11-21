/**
 * 128x96 Layered Pixel Character Sheet Compositor, Frame Extractor & Tint Resolver.
 *
 * Ported from pixel-agents-hq/pixel-agents (MIT License, Copyright (c) 2026 Pablo De Lucca):
 * - core/src/assets/seatedFrame.ts
 * - webview-ui/src/characters/slots.ts
 * - webview-ui/src/characters/wardrobe.ts
 * - webview-ui/src/characters/renderer.ts
 * - webview-ui/src/components/CharacterMugShot.tsx
 */

import {
  type ColorValue,
  colorKey,
  hexToRgb,
  luminance,
  PNG_ALPHA_THRESHOLD,
  rgbToHsl01,
  tintPixels,
} from "./colorize.ts";
import {
  PACKED_WARDROBE_SHEETS,
  type PackedSheet,
  WARDROBE_DEFAULT_COLORS,
  WARDROBE_MANIFEST,
} from "./wardrobe-data.ts";

export const CHAR_FRAME_W = 16;
export const CHAR_FRAME_H = 32;
export const CHAR_FRAMES_PER_ROW = 8;
export const CHAR_DIRECTION_ROWS = 3; // down(0), up(1), right(2)
export const SHEET_W = CHAR_FRAME_W * CHAR_FRAMES_PER_ROW; // 128
export const SHEET_H = CHAR_FRAME_H * CHAR_DIRECTION_ROWS; // 96
export const SHEET_PIXEL_COUNT = SHEET_W * SHEET_H; // 12,288
export const SHEET_BYTE_LENGTH = SHEET_PIXEL_COUNT * 4;
const MAX_RLE_BYTES = 16 * 1024; // fail-closed cap per packed RLE layer

export const SHEET_FRAMES = {
  walk: [0, 1, 2, 1] as const,
  type: [3, 4] as const,
  read: [5, 6] as const,
  seated: 7,
} as const;

export type PixelDirection = "S" | "N" | "E" | "W";
export type PixelActivity = "idle" | "walk" | "typing" | "reading" | "seated";

export type PixelAvatarKind =
  | "alex"
  | "mira"
  | "kenji"
  | "yuna"
  | "diego"
  | "iris"
  | "agent";

export type CharacterGearOverlay =
  | "none"
  | "visor"
  | "headphones"
  | "droid"
  | "beret"
  | "headset"
  | "crown";

export type PixelAvatarGearChoice =
  | CharacterGearOverlay
  | "glasses"
  | "shades"
  | "hairshades"
  | "cap";

export type PixelAvatarPaletteOverride = Partial<{
  skin: string;
  skin2: string;
  hair: string;
  hair2: string;
  shirt: string;
  shirt2: string;
  pants: string;
  shoe: string;
  eye: string;
  mouth: string;
  hairStyle: string;
  outfitStyle: string;
  gear: PixelAvatarGearChoice;
  skinTone: "Original" | "Pale" | "Tan" | "Dark";
}>;

export const CHARACTER_HAIR_OPTIONS = [
  { id: "spiky", label: "스파이키" },
  { id: "parted", label: "가르마" },
  { id: "bob", label: "보브컷" },
  { id: "long_waves", label: "롱 웨이브" },
  { id: "high_ponytail", label: "포니테일" },
  { id: "slicked_back", label: "슬릭백" },
  { id: "side_swept", label: "사이드 스웹" },
  { id: "short", label: "숏컷" },
  { id: "mop", label: "모프컷" },
  { id: "shoulder_length", label: "미디엄" },
  { id: "bowl_cut", label: "바울컷" },
  { id: "long_bangs", label: "뱅 롱헤어" },
] as const;

export const CHARACTER_OUTFIT_OPTIONS = [
  { id: "bomber", label: "봄버 재킷" },
  { id: "blazer", label: "블레이저" },
  { id: "long_coat", label: "롱 코트" },
  { id: "open_shirt", label: "오픈 셔츠" },
  { id: "vest", label: "워크 베스트" },
  { id: "business_suit", label: "비즈니스 수트" },
  { id: "suit_and_tie", label: "수트 & 타이" },
  { id: "turtleneck", label: "터틀넥" },
  { id: "dress", label: "원피스" },
] as const;

export const CHARACTER_GEAR_OPTIONS: ReadonlyArray<{ id: PixelAvatarGearChoice; label: string }> = [
  { id: "none", label: "기본" },
  { id: "glasses", label: "안경" },
  { id: "shades", label: "선글라스" },
  { id: "hairshades", label: "헤어 선글라스" },
  { id: "cap", label: "캡 모자" },
  { id: "headphones", label: "헤드폰" },
  { id: "headset", label: "헤드셋" },
  { id: "beret", label: "베레모" },
  { id: "visor", label: "사이버 바이저" },
  { id: "droid", label: "드로이드 헬멧" },
] as const;

export interface CharacterColor extends ColorValue {
  colorize?: boolean;
  palette?: string;
}

export interface CharacterProfile {
  id?: string;
  name?: string;
  body: "flat" | "bust";
  utop?: string;
  under?: string;
  eyeShape?: string;
  eyeRows?: "top" | "bottom" | "both";
  hair?: string;
  beard?: string;
  top?: string;
  jacket?: string;
  pants?: string;
  shoes?: string;
  full?: string;
  hat?: string;
  necklace?: string;
  lanyard?: string;
  earrings?: string;
  shades?: string;
  hairshades?: string;
  watch?: string;
  blink?: boolean;
  isAgent?: boolean;
  agentAccentHex?: string;
  gearOverlay?: CharacterGearOverlay;
  colors?: Partial<Record<ColorKey, CharacterColor>>;
}

export type ColorKey =
  | "skin"
  | "eyes"
  | "lashes"
  | "tie"
  | "utop"
  | "under"
  | "hair"
  | "beard"
  | "top"
  | "jacket"
  | "pants"
  | "shoes"
  | "full"
  | "hat"
  | "necklace"
  | "lanyard"
  | "earrings"
  | "shades"
  | "hairshades"
  | "watch";

interface LayerDef {
  pix: string;
  kind: "part" | "eye";
  sel?: keyof CharacterProfile;
  filedBy?: "shape" | "body";
  rows?: boolean;
  color: ColorKey | null;
  when?: "under" | "utop";
  behind?: string;
  neutral?: boolean;
}

const LAYERS: readonly LayerDef[] = [
  { kind: "part", pix: "body", sel: "body", color: "skin" },
  { kind: "part", pix: "under", sel: "body", color: "under", when: "under", neutral: true },
  { kind: "part", pix: "utop", sel: "body", color: "utop", when: "utop", neutral: true },
  { kind: "eye", pix: "whites", filedBy: "body", color: null },
  { kind: "eye", pix: "iris", filedBy: "body", color: "eyes", rows: true },
  { kind: "eye", pix: "lashes", filedBy: "shape", color: "lashes" },
  { kind: "part", pix: "pants", sel: "pants", color: "pants" },
  { kind: "part", pix: "top", sel: "top", color: "top" },
  { kind: "part", pix: "full", sel: "full", color: "full" },
  { kind: "part", pix: "tie", sel: "full", color: "tie" },
  { kind: "part", pix: "jacket", sel: "jacket", color: "jacket" },
  { kind: "part", pix: "lanyard", sel: "lanyard", color: "lanyard" },
  { kind: "part", pix: "necklace", sel: "necklace", color: "necklace" },
  { kind: "part", pix: "watch", sel: "watch", color: "watch" },
  { kind: "part", pix: "shoes", sel: "shoes", color: "shoes" },
  { kind: "part", pix: "hair", sel: "hair", color: "hair" },
  { kind: "part", pix: "earrings", sel: "earrings", color: "earrings", behind: "hair" },
  { kind: "part", pix: "beard", sel: "beard", color: "beard" },
  { kind: "part", pix: "shades", sel: "shades", color: "shades" },
  { kind: "part", pix: "hairshades", sel: "hairshades", color: "hairshades" },
  { kind: "part", pix: "hat", sel: "hat", color: "hat" },
];

const CLOTHES_COLOR_KEYS: readonly ColorKey[] = [
  "under",
  "utop",
  "pants",
  "top",
  "full",
  "jacket",
  "shoes",
  "hat",
];

const EYE_BLINK_LASHES = new Set(["stern"]);

const FLAT_SWATCH: Partial<Record<ColorKey, number>> = { eyes: 0.9, lashes: 0.55 };
const SWATCH_FIT = {
  glint: 0.92,
  outlineAt: 0.05,
  flatSpread: 0.08,
  flatLift: 0.55,
  highlightAt: 0.98,
  maxLight: 0.97,
  outlineFloor: 0.4,
  minContrast: 0.35,
  maxContrast: 2,
} as const;

const CACHE_LIMIT = { decode: 120, tint: 400, sheet: 160 };

function decodeBase64ToBytes(b64: string): Uint8Array {
  if (typeof Buffer !== "undefined") {
    const buf = Buffer.from(b64, "base64");
    return new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength);
  }
  const binary = atob(b64);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    out[i] = binary.charCodeAt(i);
  }
  return out;
}

/** Decode a bit-packed RLE wardrobe layer strictly with fail-closed pixel bounds. */
export function decodePackedSheet(packed: PackedSheet): Uint8ClampedArray {
  const bytes = decodeBase64ToBytes(packed.d);
  if (bytes.length === 0 || bytes.length > MAX_RLE_BYTES) {
    throw new Error(`Invalid packed sheet length: ${bytes.length}`);
  }
  const rgba = new Uint8ClampedArray(SHEET_BYTE_LENGTH);
  let outPx = 0;

  for (let i = 0; i < bytes.length; i++) {
    const token = bytes[i];
    if (token >= 0x80) {
      const skip = (token & 0x7f) + 1;
      outPx += skip;
      if (outPx > SHEET_PIXEL_COUNT) {
        throw new Error("Packed sheet transparent skip exceeded sheet bounds");
      }
      continue;
    }
    const colorIdx = token >>> 3;
    const run = (token & 0x07) + 1;
    if (colorIdx >= packed.p.length || outPx + run > SHEET_PIXEL_COUNT) {
      throw new Error("Packed sheet color run exceeded palette or bounds");
    }
    const col = packed.p[colorIdx];
    const r = (col >>> 24) & 0xff;
    const g = (col >>> 16) & 0xff;
    const b = (col >>> 8) & 0xff;
    const a = col & 0xff;
    for (let k = 0; k < run; k++, outPx++) {
      const o = outPx * 4;
      rgba[o] = r;
      rgba[o + 1] = g;
      rgba[o + 2] = b;
      rgba[o + 3] = a;
    }
  }

  if (outPx !== SHEET_PIXEL_COUNT) {
    throw new Error(`Packed sheet decoded ${outPx} pixels instead of ${SHEET_PIXEL_COUNT}`);
  }
  return rgba;
}

class WardrobeCompositor {
  private readonly decodedCache = new Map<string, Uint8ClampedArray>();
  private readonly tintCache = new Map<string, Uint8ClampedArray>();
  private readonly sheetCache = new Map<string, Uint8ClampedArray>();
  private readonly neutralSet = new Set<string>();
  private readonly swapsMap = new Map<string, Record<string, Record<string, string>>>();
  private readonly colorLayerMap = new Map<ColorKey, LayerDef>();

  constructor() {
    for (const [category, parts] of Object.entries(WARDROBE_MANIFEST.parts)) {
      for (const part of parts as ReadonlyArray<{
        id: string;
        neutral?: boolean;
        swaps?: Record<string, Record<string, string>>;
      }>) {
        const path = `${category}/${part.id}`;
        if (part.neutral) this.neutralSet.add(path);
        if (part.swaps) this.swapsMap.set(path, part.swaps);
      }
    }
    for (const layer of LAYERS) {
      if (layer.color && !this.colorLayerMap.has(layer.color)) {
        this.colorLayerMap.set(layer.color, layer);
      }
    }
  }

  private getArt(path: string): Uint8ClampedArray | null {
    const cached = this.decodedCache.get(path);
    if (cached) return cached;
    const packed = PACKED_WARDROBE_SHEETS[path];
    if (!packed) return null;
    if (this.decodedCache.size >= CACHE_LIMIT.decode) {
      // keep decoded sheets; 81 sheets max is well below 120 (~4MB total across the whole wardrobe)
    }
    const decoded = decodePackedSheet(packed);
    this.decodedCache.set(path, decoded);
    return decoded;
  }

  private shownArt(p: CharacterProfile, layer: LayerDef): { path: string; part: string | null } | null {
    if (layer.when) {
      const shownUnder =
        layer.when === "utop"
          ? Boolean(p.utop && !p.top && !p.full)
          : Boolean(p.under && !p.pants && !p.full);
      if (!shownUnder) return null;
    }
    if (layer.kind === "eye") {
      const ownShape = p.eyeShape ?? (p.body === "bust" ? "lashes" : "plain");
      const activeShape = p.blink ? "closed" : ownShape;
      const folder =
        p.blink && layer.pix === "lashes" && EYE_BLINK_LASHES.has(ownShape)
          ? `closed-${ownShape}`
          : activeShape;
      const rows = layer.rows ? `.${p.eyeRows ?? "bottom"}` : "";
      return { path: `eyes/${folder}/${layer.pix}${rows}`, part: null };
    }
    const sel = layer.sel;
    const id = sel ? (p[sel] as string | undefined) : undefined;
    if (!id) return null;
    return { path: `${layer.pix}/${id}`, part: id };
  }

  private colorArt(p: CharacterProfile, key: ColorKey): { path: string; layer: LayerDef; part: string | null } | null {
    const layer = this.colorLayerMap.get(key);
    if (!layer) return null;
    if (layer.when && !(p[layer.when] as string | undefined)) return null;
    const art = this.shownArt({ ...p, blink: false }, layer);
    return art ? { ...art, layer } : null;
  }

  private isNeutral(art: { path: string; layer: LayerDef } | null): boolean {
    return Boolean(art && (this.neutralSet.has(art.path) || art.layer.neutral));
  }

  private defaultColor(p: CharacterProfile, key: ColorKey): CharacterColor | null {
    const art = this.colorArt(p, key);
    if (!this.isNeutral(art) || !art?.part) return null;
    const palettes = WARDROBE_MANIFEST.palettes as unknown as Record<
      string,
      {
        readonly squares: readonly { readonly label: string; readonly hex: string }[];
        readonly values: Record<string, readonly CharacterColor[]>;
      }
    >;
    const pal = palettes[art.layer.pix];
    const row = pal?.values?.[art.part];
    if (!pal || !row) return null;
    const isClothes = CLOTHES_COLOR_KEYS.includes(key);
    const idx = pal.squares.findIndex((sq) =>
      key === "hair"
        ? sq.hex === WARDROBE_DEFAULT_COLORS.hairHex
        : isClothes && sq.label === WARDROBE_DEFAULT_COLORS.clothesSquare,
    );
    return idx >= 0 ? row[idx] : null;
  }

  private colorOf(p: CharacterProfile, key: ColorKey): CharacterColor | null {
    const own = p.colors?.[key];
    if (own) return own;
    if (key === "tie") return this.colorOf(p, "full");
    return this.defaultColor(p, key);
  }

  swatchValue(artPath: string | null, hex: string, key: ColorKey): CharacterColor {
    const [h, s, tl] = rgbToHsl01(...hexToRgb(hex));
    const src = artPath ? this.getArt(artPath) : null;
    const ls: number[] = [];
    if (src) {
      for (let i = 0; i < src.length; i += 4) {
        if (src[i + 3] < PNG_ALPHA_THRESHOLD) continue;
        const v = luminance(src[i], src[i + 1], src[i + 2]);
        if (v < SWATCH_FIT.glint) ls.push(v);
      }
    }
    ls.sort((a, b) => a - b);
    const lmin = ls.length ? ls[Math.floor(ls.length * SWATCH_FIT.outlineAt)] : 0;
    const mean = ls.length ? ls.reduce((a, b) => a + b, 0) / ls.length : 0.5;
    let c = 0;
    let b: number;
    const flat = FLAT_SWATCH[key];
    if (flat) {
      b = (tl * flat - mean) * 200;
    } else if (mean - lmin < SWATCH_FIT.flatSpread) {
      b = (tl * SWATCH_FIT.flatLift - mean) * 200;
    } else {
      const lminT = Math.min(lmin, tl * SWATCH_FIT.outlineFloor);
      const lmax = ls[Math.floor(ls.length * SWATCH_FIT.highlightAt)] || mean;
      const toWhite =
        lmax > mean ? (SWATCH_FIT.maxLight - tl) / (lmax - mean) : SWATCH_FIT.maxContrast;
      const denom = Math.max(0.02, mean - lmin);
      const f = Math.max(
        SWATCH_FIT.minContrast,
        Math.min(SWATCH_FIT.maxContrast, toWhite, (tl - lminT) / denom),
      );
      c = (f - 1) * 100;
      b = (tl - 0.5 - (mean - 0.5) * f) * 200;
    }
    const clamp = (v: number): number => Math.max(-100, Math.min(100, Math.round(v)));
    return {
      h: Math.round(h * 360),
      s: Math.round(s * 100),
      b: clamp(b),
      c: clamp(c),
      colorize: true,
    };
  }

  private tint(path: string, src: Uint8ClampedArray, cv: CharacterColor, swapsPath: string | null): Uint8ClampedArray {
    const ck = `${path}|${colorKey(cv)}|${swapsPath ?? ""}`;
    const existing = this.tintCache.get(ck);
    if (existing) return existing;
    const out = tintPixels(src, cv, swapsPath ? this.swapsMap.get(swapsPath) : undefined);
    if (this.tintCache.size > CACHE_LIMIT.tint) this.tintCache.clear();
    this.tintCache.set(ck, out);
    return out;
  }

  composeSheet(p: CharacterProfile): Uint8ClampedArray {
    const ck = JSON.stringify({
      b: p.body,
      u: p.utop,
      n: p.under,
      es: p.eyeShape,
      er: p.eyeRows,
      h: p.hair,
      bd: p.beard,
      t: p.top,
      j: p.jacket,
      pn: p.pants,
      sh: p.shoes,
      fl: p.full,
      ht: p.hat,
      nk: p.necklace,
      ln: p.lanyard,
      ea: p.earrings,
      sd: p.shades,
      hs: p.hairshades,
      w: p.watch,
      bl: Boolean(p.blink),
      ag: Boolean(p.isAgent),
      go: p.gearOverlay ?? (p.isAgent ? "visor" : "none"),
      aa: p.agentAccentHex ?? "",
      c: p.colors ?? null,
    });
    const cached = this.sheetCache.get(ck);
    if (cached) return cached;

    const out = new Uint8ClampedArray(SHEET_BYTE_LENGTH);
    const drawn = new Map<string, Uint8ClampedArray>();
    const backStart = CHAR_FRAME_H * SHEET_W * 4;
    const backEnd = 2 * CHAR_FRAME_H * SHEET_W * 4;

    for (const layer of LAYERS) {
      const art = this.shownArt(p, layer);
      if (!art) continue;
      const src = this.getArt(art.path);
      if (!src) continue;
      const cv = layer.color ? this.colorOf(p, layer.color) : null;
      const swapsPath = layer.color ? this.colorArt(p, layer.color)?.path ?? null : null;
      const px = cv ? this.tint(art.path, src, cv, swapsPath) : src;
      const behind = layer.behind ? drawn.get(layer.behind) : undefined;
      compositeOver(out, px, behind, backStart, backEnd);
      drawn.set(layer.pix, px);
    }

    const activeGear: CharacterGearOverlay = p.gearOverlay ?? (p.isAgent ? "visor" : "none");
    if (activeGear !== "none") {
      paintCharacterGearOverlay(out, activeGear, p.agentAccentHex ?? "#38bdf8");
    }

    if (this.sheetCache.size > CACHE_LIMIT.sheet) this.sheetCache.clear();
    this.sheetCache.set(ck, out);
    return out;
  }
}

function compositeOver(
  dst: Uint8ClampedArray,
  src: Uint8ClampedArray,
  behind: Uint8ClampedArray | undefined,
  backStart: number,
  backEnd: number,
): void {
  for (let i = 0; i < src.length; i += 4) {
    const a = src[i + 3];
    if (a < PNG_ALPHA_THRESHOLD) continue;
    if (behind && i >= backStart && i < backEnd && behind[i + 3] >= PNG_ALPHA_THRESHOLD) {
      continue;
    }
    if (a === 255) {
      dst[i] = src[i];
      dst[i + 1] = src[i + 1];
      dst[i + 2] = src[i + 2];
      dst[i + 3] = 255;
      continue;
    }
    const ao = dst[i + 3] / 255;
    const as = a / 255;
    const ar = as + ao * (1 - as);
    if (ar <= 0) continue;
    dst[i] = (src[i] * as + dst[i] * ao * (1 - as)) / ar;
    dst[i + 1] = (src[i + 1] * as + dst[i + 1] * ao * (1 - as)) / ar;
    dst[i + 2] = (src[i + 2] * as + dst[i + 2] * ao * (1 - as)) / ar;
    dst[i + 3] = ar * 255;
  }
}

/** Head top Y anchor per frame index (0..7) measured from MetroCity sprite body sheets. */
const FRAME_HEAD_TOP_Y = [2, 3, 2, 6, 5, 5, 4, 2] as const;

/** Paint sculpted pixel headgear/tech accessories aligned to per-frame head bounce. */
function paintCharacterGearOverlay(
  sheet: Uint8ClampedArray,
  gear: CharacterGearOverlay,
  accentHex: string,
): void {
  const [ar, ag, ab] = hexToRgb(accentHex);
  const brightR = Math.min(255, ar + 72);
  const brightG = Math.min(255, ag + 72);
  const brightB = Math.min(255, ab + 72);
  const darkR = Math.max(0, Math.round(ar * 0.45));
  const darkG = Math.max(0, Math.round(ag * 0.45));
  const darkB = Math.max(0, Math.round(ab * 0.45));

  const setPx = (
    rowDir: number,
    colFrame: number,
    x: number,
    y: number,
    r: number,
    g: number,
    b: number,
  ) => {
    if (x < 0 || x >= CHAR_FRAME_W || y < 0 || y >= CHAR_FRAME_H) return;
    const gy = rowDir * CHAR_FRAME_H + y;
    const gx = colFrame * CHAR_FRAME_W + x;
    const off = (gy * SHEET_W + gx) * 4;
    if (off < 0 || off + 3 >= sheet.length) return;
    sheet[off] = r;
    sheet[off + 1] = g;
    sheet[off + 2] = b;
    sheet[off + 3] = 255;
  };

  const hLine = (
    rowDir: number,
    colFrame: number,
    x0: number,
    x1: number,
    y: number,
    r: number,
    g: number,
    b: number,
  ) => {
    for (let x = x0; x <= x1; x++) setPx(rowDir, colFrame, x, y, r, g, b);
  };

  for (let frame = 0; frame < CHAR_FRAMES_PER_ROW; frame++) {
    const ht = FRAME_HEAD_TOP_Y[frame];
    const eyeY = ht + 10;
    const chestY = ht + 18;

    if (gear === "visor") {
      hLine(0, frame, 4, 11, eyeY - 1, 18, 26, 44);
      hLine(0, frame, 4, 11, eyeY + 1, 18, 26, 44);
      hLine(0, frame, 5, 10, eyeY, brightR, brightG, brightB);
      setPx(0, frame, 7, eyeY, 240, 250, 255);
      setPx(0, frame, 8, eyeY, 240, 250, 255);
      setPx(0, frame, 7, chestY, brightR, brightG, brightB);

      hLine(2, frame, 8, 12, eyeY - 1, 18, 26, 44);
      hLine(2, frame, 8, 12, eyeY + 1, 18, 26, 44);
      hLine(2, frame, 9, 11, eyeY, brightR, brightG, brightB);
      setPx(2, frame, 9, chestY, brightR, brightG, brightB);
      continue;
    }

    if (gear === "headphones") {
      // Over-ear headband across crown + padded left/right earcups
      hLine(0, frame, 4, 11, ht + 1, 30, 34, 48);
      hLine(0, frame, 3, 12, ht + 2, ar, ag, ab);
      for (let y = ht + 3; y <= ht + 7; y++) {
        setPx(0, frame, 3, y, 36, 42, 58);
        setPx(0, frame, 12, y, 36, 42, 58);
      }
      for (let y = ht + 7; y <= ht + 11; y++) {
        setPx(0, frame, 2, y, darkR, darkG, darkB);
        setPx(0, frame, 3, y, brightR, brightG, brightB);
        setPx(0, frame, 12, y, brightR, brightG, brightB);
        setPx(0, frame, 13, y, darkR, darkG, darkB);
      }
      // Back row
      hLine(1, frame, 4, 11, ht + 1, 30, 34, 48);
      hLine(1, frame, 3, 12, ht + 2, ar, ag, ab);
      for (let y = ht + 7; y <= ht + 11; y++) {
        setPx(1, frame, 2, y, darkR, darkG, darkB);
        setPx(1, frame, 3, y, ar, ag, ab);
        setPx(1, frame, 12, y, ar, ag, ab);
        setPx(1, frame, 13, y, darkR, darkG, darkB);
      }
      // Side row
      hLine(2, frame, 5, 9, ht + 1, ar, ag, ab);
      for (let y = ht + 2; y <= ht + 7; y++) setPx(2, frame, 6, y, 42, 48, 66);
      for (let y = ht + 7; y <= ht + 11; y++) {
        setPx(2, frame, 5, y, darkR, darkG, darkB);
        setPx(2, frame, 6, y, brightR, brightG, brightB);
        setPx(2, frame, 7, y, ar, ag, ab);
      }
      continue;
    }

    if (gear === "droid") {
      // Full retro-futuristic alloy AI droid shell with antenna & glowing optical sensors
      setPx(0, frame, 7, ht - 1, brightR, brightG, brightB);
      setPx(0, frame, 8, ht - 1, brightR, brightG, brightB);
      setPx(0, frame, 7, ht, 148, 163, 184);
      setPx(0, frame, 8, ht, 148, 163, 184);
      hLine(0, frame, 5, 10, ht + 1, 71, 85, 105);
      hLine(0, frame, 4, 11, ht + 2, 203, 213, 225);
      for (let y = ht + 3; y <= ht + 12; y++) {
        const edgeL = y < ht + 5 ? 3 : 3;
        const edgeR = y < ht + 5 ? 12 : 12;
        setPx(0, frame, edgeL, y, 51, 65, 85);
        setPx(0, frame, edgeR, y, 51, 65, 85);
        for (let x = edgeL + 1; x < edgeR; x++) {
          const isScreen = y >= eyeY - 2 && y <= eyeY + 1 && x >= 4 && x <= 11;
          if (isScreen) {
            setPx(0, frame, x, y, 15, 23, 42);
          } else {
            const hi = y <= ht + 4 || x <= 6;
            setPx(0, frame, x, y, hi ? 226 : 148, hi ? 232 : 163, hi ? 240 : 184);
          }
        }
      }
      // Dual glowing optical eyes + accent ear vents
      setPx(0, frame, 5, eyeY - 1, brightR, brightG, brightB);
      setPx(0, frame, 6, eyeY - 1, 240, 250, 255);
      setPx(0, frame, 9, eyeY - 1, 240, 250, 255);
      setPx(0, frame, 10, eyeY - 1, brightR, brightG, brightB);
      setPx(0, frame, 5, eyeY, ar, ag, ab);
      setPx(0, frame, 6, eyeY, brightR, brightG, brightB);
      setPx(0, frame, 9, eyeY, brightR, brightG, brightB);
      setPx(0, frame, 10, eyeY, ar, ag, ab);
      hLine(0, frame, 5, 10, eyeY + 2, 100, 116, 139);
      setPx(0, frame, 3, eyeY - 1, ar, ag, ab);
      setPx(0, frame, 12, eyeY - 1, ar, ag, ab);
      setPx(0, frame, 7, chestY, brightR, brightG, brightB);
      setPx(0, frame, 8, chestY, ar, ag, ab);

      // Back view of droid head
      setPx(1, frame, 7, ht - 1, brightR, brightG, brightB);
      setPx(1, frame, 7, ht, 148, 163, 184);
      hLine(1, frame, 5, 10, ht + 1, 71, 85, 105);
      for (let y = ht + 2; y <= ht + 11; y++) {
        hLine(1, frame, 4, 11, y, y % 2 === 0 ? 148 : 168, 163, 184);
        setPx(1, frame, 3, y, 51, 65, 85);
        setPx(1, frame, 12, y, 51, 65, 85);
      }
      hLine(1, frame, 5, 10, ht + 7, ar, ag, ab);

      // Side view of droid head
      setPx(2, frame, 7, ht - 1, brightR, brightG, brightB);
      setPx(2, frame, 7, ht, 148, 163, 184);
      hLine(2, frame, 4, 10, ht + 1, 71, 85, 105);
      for (let y = ht + 2; y <= ht + 12; y++) {
        hLine(2, frame, 4, 11, y, 203, 213, 225);
        setPx(2, frame, 3, y, 51, 65, 85);
        setPx(2, frame, 12, y, 51, 65, 85);
      }
      for (let y = eyeY - 2; y <= eyeY + 1; y++) hLine(2, frame, 8, 11, y, 15, 23, 42);
      setPx(2, frame, 9, eyeY - 1, brightR, brightG, brightB);
      setPx(2, frame, 10, eyeY - 1, 240, 250, 255);
      setPx(2, frame, 9, chestY, brightR, brightG, brightB);
      continue;
    }

    if (gear === "beret") {
      for (const row of [0, 1]) {
        setPx(row, frame, 6, ht, darkR, darkG, darkB);
        hLine(row, frame, 4, 10, ht + 1, brightR, brightG, brightB);
        hLine(row, frame, 3, 11, ht + 2, ar, ag, ab);
        hLine(row, frame, 3, 12, ht + 3, ar, ag, ab);
        hLine(row, frame, 4, 11, ht + 4, darkR, darkG, darkB);
      }
      setPx(2, frame, 6, ht, darkR, darkG, darkB);
      hLine(2, frame, 4, 10, ht + 1, brightR, brightG, brightB);
      hLine(2, frame, 3, 11, ht + 2, ar, ag, ab);
      hLine(2, frame, 4, 10, ht + 3, darkR, darkG, darkB);
      continue;
    }

    if (gear === "headset") {
      hLine(0, frame, 4, 11, ht + 1, 30, 41, 59);
      for (let y = ht + 2; y <= ht + 10; y++) setPx(0, frame, 3, y, 51, 65, 85);
      setPx(0, frame, 2, eyeY - 1, 30, 41, 59);
      setPx(0, frame, 2, eyeY, ar, ag, ab);
      setPx(0, frame, 3, eyeY, brightR, brightG, brightB);
      setPx(0, frame, 3, eyeY + 1, 51, 65, 85);
      setPx(0, frame, 4, eyeY + 2, 100, 116, 139);
      setPx(0, frame, 5, eyeY + 2, brightR, brightG, brightB);
      setPx(2, frame, 6, eyeY, ar, ag, ab);
      setPx(2, frame, 7, eyeY + 1, 100, 116, 139);
      setPx(2, frame, 9, eyeY + 1, brightR, brightG, brightB);
      continue;
    }

    if (gear === "crown") {
      for (const row of [0, 1, 2]) {
        setPx(row, frame, 5, ht, 251, 191, 36);
        setPx(row, frame, 7, ht, 254, 240, 138);
        setPx(row, frame, 8, ht, 254, 240, 138);
        setPx(row, frame, 10, ht, 251, 191, 36);
        hLine(row, frame, 5, 10, ht + 1, 245, 158, 11);
        hLine(row, frame, 5, 10, ht + 2, 180, 83, 9);
        setPx(row, frame, 7, ht + 1, brightR, brightG, brightB);
        setPx(row, frame, 8, ht + 1, brightR, brightG, brightB);
      }
    }
  }
}

export const wardrobeCompositor = new WardrobeCompositor();

/** Curated default archetypes mapped cleanly onto Aquila Office's 7 PixelAvatarKinds. */
const ARCHETYPE_PROFILES: Record<PixelAvatarKind, CharacterProfile> = {
  alex: {
    id: "alex",
    body: "flat",
    eyeShape: "plain",
    hair: "spiky",
    top: "tshirt",
    jacket: "bomber",
    pants: "jeans",
    shoes: "sneakers",
    lanyard: "lanyard",
    colors: {
      hair: { h: 24, s: 54, b: -38, c: -20, colorize: true },
      top: { h: 215, s: 25, b: 85, c: 20, colorize: true },
      jacket: { h: 218, s: 52, b: -24, c: 8, colorize: true },
      pants: { h: 220, s: 34, b: -30, c: -10, colorize: true },
      lanyard: { h: 218, s: 88, b: 8, c: 12, colorize: true },
    },
  },
  mira: {
    id: "mira",
    body: "bust",
    eyeShape: "lashes",
    utop: "basic",
    hair: "bob",
    top: "turtleneck",
    jacket: "blazer",
    pants: "skirt",
    shoes: "boots",
    hairshades: "hairshades",
    colors: {
      hair: { h: 8, s: 66, b: -16, c: 12, colorize: true },
      top: { h: 38, s: 32, b: 78, c: 10, colorize: true },
      jacket: { h: 332, s: 44, b: -12, c: 10, colorize: true },
      pants: { h: 235, s: 22, b: -50, c: -10, colorize: true },
    },
  },
  kenji: {
    id: "kenji",
    body: "flat",
    eyeShape: "plain",
    hair: "side_swept",
    top: "long_sleeved_shirt",
    jacket: "open_shirt",
    pants: "low_cut_jeans",
    shoes: "boots",
    shades: "glasses",
    watch: "watch",
    colors: {
      hair: { h: 215, s: 18, b: -64, c: -45, colorize: true },
      top: { h: 0, s: 0, b: 88, c: 25, colorize: true },
      jacket: { h: 158, s: 58, b: -24, c: 12, colorize: true },
      pants: { h: 216, s: 38, b: -36, c: -10, colorize: true },
    },
  },
  yuna: {
    id: "yuna",
    body: "bust",
    eyeShape: "lashes",
    utop: "basic",
    hair: "high_ponytail",
    full: "suit_and_tie",
    shoes: "boots",
    necklace: "necklace",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Pale" },
      hair: { h: 38, s: 68, b: 14, c: 12, colorize: true },
      full: { h: 238, s: 36, b: -28, c: 10, colorize: true },
      tie: { h: 344, s: 76, b: 4, c: 15, colorize: true },
    },
  },
  diego: {
    id: "diego",
    body: "flat",
    eyeShape: "plain",
    hair: "short",
    beard: "stubble",
    hat: "plain_cap",
    top: "tshirt",
    jacket: "vest",
    pants: "jeans",
    shoes: "boots",
    lanyard: "lanyard",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Tan" },
      hair: { h: 22, s: 36, b: -48, c: -30, colorize: true },
      hat: { h: 262, s: 58, b: -14, c: 10, colorize: true },
      top: { h: 0, s: 0, b: 86, c: 20, colorize: true },
      jacket: { h: 262, s: 58, b: -24, c: 12, colorize: true },
      lanyard: { h: 42, s: 90, b: 14, c: 12, colorize: true },
    },
  },
  iris: {
    id: "iris",
    body: "bust",
    eyeShape: "lashes",
    utop: "basic",
    hair: "long_waves",
    top: "turtleneck",
    jacket: "long_coat",
    pants: "jeans",
    shoes: "boots",
    lanyard: "lanyard",
    colors: {
      hair: { h: 274, s: 54, b: -14, c: 12, colorize: true },
      top: { h: 208, s: 44, b: -8, c: 8, colorize: true },
      jacket: { h: 222, s: 26, b: -38, c: 10, colorize: true },
      lanyard: { h: 190, s: 88, b: 10, c: 10, colorize: true },
    },
  },
  agent: {
    id: "agent",
    body: "flat",
    eyeShape: "plain",
    hair: "slicked_back",
    full: "business_suit",
    shoes: "boots",
    lanyard: "lanyard",
    isAgent: true,
    agentAccentHex: "#3279f9",
    colors: {
      hair: { h: 215, s: 42, b: -38, c: 8, colorize: true },
      full: { h: 220, s: 52, b: -28, c: 14, colorize: true },
      lanyard: { h: 196, s: 92, b: 18, c: 16, colorize: true },
    },
  },
};

interface SpecialistPreset extends Partial<CharacterProfile> {
  readonly specialty?: "QA" | "ORCHESTRATOR" | "BACKEND" | "FRONTEND" | "CUSTOM";
}

const AGENT_PRESETS: readonly SpecialistPreset[] = [
  {
    specialty: "QA",
    body: "bust",
    eyeShape: "lashes",
    hair: "bob",
    top: "turtleneck",
    jacket: "bomber",
    pants: "jeans",
    gearOverlay: "headphones",
    agentAccentHex: "#38bdf8",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Pale" },
      hair: { h: 175, s: 72, b: -12, c: 12, colorize: true },
      top: { h: 195, s: 35, b: 82, c: 20, colorize: true },
      jacket: { h: 218, s: 64, b: -22, c: 14, colorize: true },
    },
  },
  {
    specialty: "ORCHESTRATOR",
    hair: "slicked_back",
    full: "suit_and_tie",
    jacket: undefined,
    top: undefined,
    pants: undefined,
    gearOverlay: "crown",
    agentAccentHex: "#a78bfa",
    colors: {
      hair: { h: 255, s: 52, b: -24, c: 12, colorize: true },
      full: { h: 245, s: 44, b: -30, c: 16, colorize: true },
      tie: { h: 192, s: 90, b: 14, c: 18, colorize: true },
    },
  },
  {
    specialty: "BACKEND",
    hair: "short",
    top: "long_sleeved_shirt",
    jacket: "long_coat",
    pants: "low_cut_jeans",
    gearOverlay: "droid",
    agentAccentHex: "#22d3ee",
    colors: {
      jacket: { h: 222, s: 34, b: -38, c: 14, colorize: true },
      top: { h: 210, s: 45, b: 78, c: 18, colorize: true },
    },
  },
  {
    specialty: "FRONTEND",
    body: "bust",
    eyeShape: "lashes",
    hair: "long_waves",
    top: "tshirt",
    jacket: "open_shirt",
    pants: "short_skirt",
    gearOverlay: "beret",
    agentAccentHex: "#f472b6",
    colors: {
      hair: { h: 330, s: 66, b: -6, c: 12, colorize: true },
      top: { h: 0, s: 0, b: 88, c: 16, colorize: true },
      jacket: { h: 340, s: 68, b: -16, c: 14, colorize: true },
    },
  },
  {
    specialty: "QA",
    hair: "spiky",
    hat: "badge_cap",
    top: "tshirt",
    jacket: "vest",
    pants: "jeans",
    gearOverlay: "headset",
    agentAccentHex: "#34d399",
    colors: {
      hair: { h: 210, s: 40, b: -36, c: 10, colorize: true },
      hat: { h: 158, s: 72, b: -8, c: 14, colorize: true },
      jacket: { h: 162, s: 58, b: -24, c: 14, colorize: true },
    },
  },
  {
    specialty: "ORCHESTRATOR",
    hair: "parted",
    top: "turtleneck",
    jacket: "blazer",
    pants: "jeans",
    shades: "glasses",
    gearOverlay: "droid",
    agentAccentHex: "#f59e0b",
    colors: {
      jacket: { h: 268, s: 52, b: -26, c: 14, colorize: true },
      top: { h: 42, s: 65, b: 22, c: 14, colorize: true },
    },
  },
  {
    specialty: "BACKEND",
    hair: "side_swept",
    beard: "goatee",
    top: "turtleneck",
    jacket: "bomber",
    pants: "jeans",
    shades: "glasses",
    gearOverlay: "headphones",
    agentAccentHex: "#818cf8",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Tan" },
      hair: { h: 24, s: 44, b: -44, c: 10, colorize: true },
      jacket: { h: 238, s: 58, b: -22, c: 14, colorize: true },
    },
  },
  {
    specialty: "FRONTEND",
    body: "bust",
    eyeShape: "lashes",
    hair: "high_ponytail",
    full: "fitted_dress",
    hairshades: "hairshades",
    gearOverlay: "headset",
    agentAccentHex: "#c084fc",
    colors: {
      hair: { h: 44, s: 76, b: 8, c: 12, colorize: true },
      full: { h: 274, s: 54, b: -18, c: 14, colorize: true },
    },
  },
  {
    specialty: "CUSTOM",
    hair: "mop",
    top: "tshirt",
    jacket: "open_shirt",
    pants: "shorts",
    gearOverlay: "droid",
    agentAccentHex: "#fb7185",
    colors: {
      jacket: { h: 350, s: 72, b: -14, c: 14, colorize: true },
    },
  },
  {
    specialty: "QA",
    body: "bust",
    eyeShape: "lashes",
    hair: "shoulder_length",
    top: "long_sleeved_shirt",
    jacket: "long_coat",
    pants: "skirt",
    shades: "glasses",
    gearOverlay: "visor",
    agentAccentHex: "#2dd4bf",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Dark" },
      hair: { h: 168, s: 52, b: -20, c: 12, colorize: true },
      jacket: { h: 215, s: 28, b: 78, c: 12, colorize: true },
    },
  },
  {
    specialty: "ORCHESTRATOR",
    body: "bust",
    eyeShape: "lashes",
    hair: "long_bangs",
    full: "business_suit",
    necklace: "necklace",
    gearOverlay: "crown",
    agentAccentHex: "#fbbf24",
    colors: {
      hair: { h: 14, s: 68, b: -12, c: 12, colorize: true },
      full: { h: 224, s: 48, b: -24, c: 12, colorize: true },
    },
  },
  {
    specialty: "BACKEND",
    hair: "bowl_cut",
    top: "tshirt",
    jacket: "blazer",
    pants: "jeans",
    gearOverlay: "visor",
    agentAccentHex: "#60a5fa",
    colors: {
      hair: { h: 214, s: 68, b: 4, c: 14, colorize: true },
      jacket: { h: 218, s: 46, b: -32, c: 12, colorize: true },
    },
  },
  {
    specialty: "CUSTOM",
    hair: "spiky",
    hat: "plain_cap",
    top: "long_sleeved_shirt",
    jacket: "bomber",
    pants: "jeans",
    gearOverlay: "beret",
    agentAccentHex: "#fb923c",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Tan" },
      jacket: { h: 28, s: 76, b: -16, c: 14, colorize: true },
    },
  },
  {
    specialty: "FRONTEND",
    hair: "parted",
    top: "turtleneck",
    jacket: "vest",
    pants: "low_cut_jeans",
    shades: "shades",
    gearOverlay: "headphones",
    agentAccentHex: "#e879f9",
    colors: {
      hair: { h: 285, s: 62, b: 4, c: 12, colorize: true },
      jacket: { h: 292, s: 52, b: -22, c: 14, colorize: true },
    },
  },
  {
    specialty: "BACKEND",
    body: "bust",
    eyeShape: "lashes",
    hair: "low_ponytail",
    top: "turtleneck",
    jacket: "blazer",
    pants: "jeans",
    gearOverlay: "droid",
    agentAccentHex: "#10b981",
    colors: {
      jacket: { h: 158, s: 56, b: -26, c: 14, colorize: true },
    },
  },
  {
    specialty: "QA",
    hair: "short",
    beard: "stubble",
    top: "tshirt",
    jacket: "open_shirt",
    pants: "jeans",
    gearOverlay: "visor",
    agentAccentHex: "#f43f5e",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Dark" },
      hair: { h: 0, s: 0, b: 84, c: 12, colorize: true },
      jacket: { h: 348, s: 66, b: -22, c: 14, colorize: true },
    },
  },
  {
    specialty: "CUSTOM",
    body: "bust",
    eyeShape: "lashes",
    hair: "bob",
    full: "dress",
    earrings: "earrings",
    gearOverlay: "beret",
    agentAccentHex: "#a855f7",
    colors: {
      skin: { h: 0, s: 0, b: 0, c: 0, palette: "Pale" },
      hair: { h: 35, s: 72, b: 10, c: 12, colorize: true },
      full: { h: 266, s: 58, b: -18, c: 14, colorize: true },
    },
  },
  {
    specialty: "ORCHESTRATOR",
    hair: "side_swept",
    full: "business_suit",
    watch: "watch_both",
    gearOverlay: "headset",
    agentAccentHex: "#0ea5e9",
    colors: {
      hair: { h: 205, s: 54, b: -22, c: 12, colorize: true },
      full: { h: 212, s: 58, b: -28, c: 14, colorize: true },
    },
  },
  {
    specialty: "FRONTEND",
    hair: "mop",
    top: "tshirt",
    jacket: "bomber",
    pants: "low_cut_jeans",
    gearOverlay: "droid",
    agentAccentHex: "#facc15",
    colors: {
      jacket: { h: 42, s: 78, b: -10, c: 14, colorize: true },
    },
  },
  {
    specialty: "BACKEND",
    hair: "slicked_back",
    beard: "moustache",
    top: "long_sleeved_shirt",
    jacket: "vest",
    pants: "jeans",
    shades: "glasses",
    gearOverlay: "headset",
    agentAccentHex: "#14b8a6",
    colors: {
      hair: { h: 18, s: 44, b: -36, c: 10, colorize: true },
      jacket: { h: 174, s: 62, b: -24, c: 14, colorize: true },
    },
  },
  {
    specialty: "QA",
    body: "bust",
    eyeShape: "lashes",
    hair: "high_ponytail",
    top: "tshirt",
    jacket: "blazer",
    pants: "fitted_shorts",
    gearOverlay: "headphones",
    agentAccentHex: "#ec4899",
    colors: {
      hair: { h: 318, s: 66, b: -6, c: 12, colorize: true },
      jacket: { h: 328, s: 54, b: -20, c: 14, colorize: true },
    },
  },
  {
    specialty: "CUSTOM",
    hair: "long_bangs",
    top: "turtleneck",
    jacket: "long_coat",
    pants: "jeans",
    gearOverlay: "beret",
    agentAccentHex: "#84cc16",
    colors: {
      hair: { h: 98, s: 56, b: -16, c: 12, colorize: true },
      jacket: { h: 88, s: 44, b: -28, c: 12, colorize: true },
    },
  },
  {
    specialty: "ORCHESTRATOR",
    hair: "spiky",
    top: "tshirt",
    jacket: "open_shirt",
    pants: "jeans",
    shades: "shades",
    gearOverlay: "visor",
    agentAccentHex: "#fb923c",
    colors: {
      hair: { h: 24, s: 82, b: 6, c: 14, colorize: true },
      jacket: { h: 22, s: 78, b: -12, c: 14, colorize: true },
    },
  },
  {
    specialty: "CUSTOM",
    body: "bust",
    eyeShape: "lashes",
    hair: "long",
    top: "tank_long",
    jacket: "bomber",
    pants: "long_skirt",
    gearOverlay: "droid",
    agentAccentHex: "#38bdf8",
    colors: {
      hair: { h: 196, s: 68, b: 8, c: 14, colorize: true },
      jacket: { h: 204, s: 64, b: -18, c: 14, colorize: true },
    },
  },
];

const MEMBER_STYLE_PRESETS: readonly Partial<CharacterProfile>[] = [
  { hair: "spiky", shades: "glasses", gearOverlay: "none" },
  { hair: "parted", jacket: "blazer", gearOverlay: "headphones", agentAccentHex: "#38bdf8" },
  { hair: "bob", gearOverlay: "beret", agentAccentHex: "#f472b6" },
  { hair: "short", hat: "badge_cap", gearOverlay: "none" },
  { hair: "side_swept", beard: "stubble", jacket: "open_shirt" },
  { hair: "high_ponytail", hairshades: "hairshades", necklace: "necklace" },
  { hair: "mop", jacket: "vest", watch: "watch" },
  { hair: "shoulder_length", earrings: "earrings", shades: "glasses" },
  { hair: "long_waves", jacket: "long_coat", gearOverlay: "headset", agentAccentHex: "#10b981" },
  { hair: "slicked_back", full: "suit_and_tie", gearOverlay: "none" },
  { hair: "bowl_cut", jacket: "bomber", hat: "plain_cap" },
  { hair: "long_bangs", full: "dress", gearOverlay: "beret", agentAccentHex: "#a78bfa" },
  { hair: "low_ponytail", top: "turtleneck", shades: "glasses" },
  { hair: "long", jacket: "blazer", watch: "watch_right" },
  { hair: "short", beard: "goatee", shades: "shades" },
  { hair: "bob", gearOverlay: "headphones", agentAccentHex: "#f59e0b" },
  { hair: "spiky", hat: "flap_cap", jacket: "bomber" },
  { hair: "parted", full: "business_suit", shades: "glasses" },
];

function hashSeed(input: string): number {
  let hash = 2166136261;
  for (let i = 0; i < input.length; i++) {
    hash ^= input.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

export function resolveCharacterProfile(
  kind: PixelAvatarKind,
  paletteOverride?: PixelAvatarPaletteOverride,
  variantKey?: string,
  agentCategory?: string,
): CharacterProfile {
  let base: CharacterProfile = ARCHETYPE_PROFILES[kind] ?? ARCHETYPE_PROFILES.alex;

  if (kind === "agent" && (variantKey || agentCategory)) {
    const normalizedCat = agentCategory?.toUpperCase();
    const matching =
      normalizedCat && normalizedCat !== "CUSTOM"
        ? AGENT_PRESETS.filter((p) => p.specialty === normalizedCat)
        : AGENT_PRESETS;
    const pool = matching.length > 0 ? matching : AGENT_PRESETS;
    const seedInput = `${agentCategory ?? ""}:${variantKey ?? kind}`;
    const idx = hashSeed(seedInput) % pool.length;
    const preset = pool[idx];
    base = {
      ...base,
      ...preset,
      colors: { ...base.colors, ...preset.colors },
    };
  } else if (kind !== "agent" && variantKey && !paletteOverride) {
    const stylePreset = MEMBER_STYLE_PRESETS[hashSeed(variantKey) % MEMBER_STYLE_PRESETS.length];
    base = {
      ...base,
      ...stylePreset,
      colors: { ...base.colors, ...stylePreset.colors },
    };
  }

  if (!paletteOverride) {
    return base;
  }

  const next: CharacterProfile = { ...base };
  const nextColors: Partial<Record<ColorKey, CharacterColor>> = { ...base.colors };

  if (paletteOverride.hairStyle) {
    next.hair = paletteOverride.hairStyle;
  }

  if (paletteOverride.outfitStyle) {
    const outfit = paletteOverride.outfitStyle;
    if (outfit === "business_suit" || outfit === "suit_and_tie" || outfit === "dress" || outfit === "fitted_dress") {
      next.full = outfit;
      next.jacket = undefined;
      next.top = undefined;
    } else if (outfit === "turtleneck") {
      next.full = undefined;
      next.jacket = undefined;
      next.top = "turtleneck";
      next.pants = next.pants ?? "jeans";
    } else {
      next.full = undefined;
      next.top = next.top ?? "tshirt";
      next.jacket = outfit;
      next.pants = next.pants ?? "jeans";
    }
  }

  if (paletteOverride.gear) {
    const g = paletteOverride.gear;
    next.shades = undefined;
    next.hairshades = undefined;
    next.hat = undefined;
    next.gearOverlay = "none";
    if (g === "glasses" || g === "shades") {
      next.shades = g;
    } else if (g === "hairshades") {
      next.hairshades = "hairshades";
    } else if (g === "cap") {
      next.hat = "badge_cap";
    } else if (g !== "none") {
      next.gearOverlay = g;
    }
  }

  if (paletteOverride.skinTone) {
    nextColors.skin = { h: 0, s: 0, b: 0, c: 0, palette: paletteOverride.skinTone };
  }

  if (paletteOverride.skin) {
    nextColors.skin = wardrobeCompositor.swatchValue(
      `body/${next.body}`,
      paletteOverride.skin,
      "skin",
    );
  }
  if (paletteOverride.hair && next.hair) {
    nextColors.hair = wardrobeCompositor.swatchValue(
      `hair/${next.hair}`,
      paletteOverride.hair,
      "hair",
    );
  }
  if (paletteOverride.shirt) {
    if (next.full) {
      nextColors.full = wardrobeCompositor.swatchValue(
        `full/${next.full}`,
        paletteOverride.shirt,
        "full",
      );
    } else if (next.jacket) {
      nextColors.jacket = wardrobeCompositor.swatchValue(
        `jacket/${next.jacket}`,
        paletteOverride.shirt,
        "jacket",
      );
    } else if (next.top) {
      nextColors.top = wardrobeCompositor.swatchValue(
        `top/${next.top}`,
        paletteOverride.shirt,
        "top",
      );
    }
  }

  return {
    ...next,
    colors: nextColors,
  };
}

/** Resolve `(sheetRow, frameCol, flipX)` inside the 128x96 sheet for a pose & animation step. */
export function resolveSheetFrameCoord(
  direction: PixelDirection,
  activity: PixelActivity,
  step: number,
): { row: number; col: number; flipX: boolean } {
  const row = direction === "N" ? 1 : direction === "E" || direction === "W" ? 2 : 0;
  const flipX = direction === "W";

  let col = 1; // resting standing idle frame
  if (activity === "walk") {
    col = SHEET_FRAMES.walk[((step % SHEET_FRAMES.walk.length) + SHEET_FRAMES.walk.length) % SHEET_FRAMES.walk.length];
  } else if (activity === "typing") {
    col = SHEET_FRAMES.type[((step % SHEET_FRAMES.type.length) + SHEET_FRAMES.type.length) % SHEET_FRAMES.type.length];
  } else if (activity === "reading") {
    col = SHEET_FRAMES.read[((step % SHEET_FRAMES.read.length) + SHEET_FRAMES.read.length) % SHEET_FRAMES.read.length];
  } else if (activity === "seated") {
    col = SHEET_FRAMES.seated;
  }

  return { row, col, flipX };
}

/** Extract a 16x32 RGBA frame buffer (with optional horizontal flip for West facing). */
export function extractCharacterFrameRgba(
  sheet: Uint8ClampedArray,
  direction: PixelDirection,
  activity: PixelActivity,
  step = 0,
): Uint8ClampedArray {
  const { row, col, flipX } = resolveSheetFrameCoord(direction, activity, step);
  const frame = new Uint8ClampedArray(CHAR_FRAME_W * CHAR_FRAME_H * 4);
  const baseRowY = row * CHAR_FRAME_H;
  const baseColX = col * CHAR_FRAME_W;

  for (let y = 0; y < CHAR_FRAME_H; y++) {
    const sy = baseRowY + y;
    for (let x = 0; x < CHAR_FRAME_W; x++) {
      const sx = baseColX + (flipX ? CHAR_FRAME_W - 1 - x : x);
      const srcOff = (sy * SHEET_W + sx) * 4;
      const dstOff = (y * CHAR_FRAME_W + x) * 4;
      frame[dstOff] = sheet[srcOff];
      frame[dstOff + 1] = sheet[srcOff + 1];
      frame[dstOff + 2] = sheet[srcOff + 2];
      frame[dstOff + 3] = sheet[srcOff + 3];
    }
  }
  return frame;
}

const FRAME_CANVAS_CACHE = new WeakMap<Uint8ClampedArray, Map<string, HTMLCanvasElement>>();

function getFrameCanvas(
  sheet: Uint8ClampedArray,
  direction: PixelDirection,
  activity: PixelActivity,
  step: number,
): HTMLCanvasElement {
  let byPose = FRAME_CANVAS_CACHE.get(sheet);
  if (!byPose) {
    byPose = new Map();
    FRAME_CANVAS_CACHE.set(sheet, byPose);
  }
  const key = `${direction}:${activity}:${step}`;
  const existing = byPose.get(key);
  if (existing) return existing;

  const rgba = extractCharacterFrameRgba(sheet, direction, activity, step);
  const off = document.createElement("canvas");
  off.width = CHAR_FRAME_W;
  off.height = CHAR_FRAME_H;
  const ctx = off.getContext("2d");
  if (ctx) {
    const img = ctx.createImageData(CHAR_FRAME_W, CHAR_FRAME_H);
    img.data.set(rgba);
    ctx.putImageData(img, 0, 0);
  }
  byPose.set(key, off);
  return off;
}

/**
 * Render either the full 16x32 pixel character (scaled crisply) or a centered upper-bust mugshot
 * onto a target 2D canvas context.
 */
export function drawPixelCharacterToCanvas(
  canvas: HTMLCanvasElement,
  profile: CharacterProfile,
  options: {
    variant: "full" | "mugshot";
    direction: PixelDirection;
    activity: PixelActivity;
    step: number;
    zoom: number;
    outline?: boolean;
  },
): void {
  const sheet = wardrobeCompositor.composeSheet(profile);
  const frameCanvas = getFrameCanvas(sheet, options.direction, options.activity, options.step);
  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  const zoom = Math.max(1, Math.round(options.zoom));
  if (options.variant === "mugshot") {
    // 16x16 crop from y=2 focusing on crown/headgear, face & collar without clipping hats/antennae
    const cropSize = 16;
    const width = cropSize * zoom;
    const height = cropSize * zoom;
    if (canvas.width !== width) canvas.width = width;
    if (canvas.height !== height) canvas.height = height;
    ctx.imageSmoothingEnabled = false;
    ctx.clearRect(0, 0, width, height);
    ctx.drawImage(frameCanvas, 0, 2, cropSize, cropSize, 0, 0, width, height);
    return;
  }

  const width = CHAR_FRAME_W * zoom;
  const height = CHAR_FRAME_H * zoom;
  if (canvas.width !== width) canvas.width = width;
  if (canvas.height !== height) canvas.height = height;
  ctx.imageSmoothingEnabled = false;
  ctx.clearRect(0, 0, width, height);
  ctx.drawImage(frameCanvas, 0, 0, CHAR_FRAME_W, CHAR_FRAME_H, 0, 0, width, height);
}
