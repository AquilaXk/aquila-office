"use client";

/**
 * Multi-layer 16x32 & Mugshot Pixel Character Renderer.
 *
 * Uses the synchronous wardrobe compositor adapted from pixel-agents-hq/pixel-agents
 * (MIT License, Copyright (c) 2026 Pablo De Lucca) with JIK-A-4 MetroCity character art.
 */

import { useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import {
  CHAR_FRAME_H,
  CHAR_FRAME_W,
  drawPixelCharacterToCanvas,
  type PixelActivity,
  type PixelAvatarKind as ImportedPixelAvatarKind,
  type PixelAvatarPaletteOverride,
  resolveCharacterProfile,
} from "@/lib/pixel-office/character-renderer";
import { startVisibilityPoll } from "@/lib/visibility-poll";

export type PixelAvatarKind = ImportedPixelAvatarKind;
export type { PixelAvatarPaletteOverride };

export const PIXEL_AVATAR_KINDS: readonly PixelAvatarKind[] = [
  "alex",
  "mira",
  "kenji",
  "yuna",
  "diego",
  "iris",
  "agent",
];

const FRAME_SHADOW_CACHE = new Map<string, string>();

function cachedMarkerShadow(colorHex: string): string {
  const hit = FRAME_SHADOW_CACHE.get(colorHex);
  if (hit) return hit;
  const shadow = `0 0 0 1px ${colorHex}`;
  FRAME_SHADOW_CACHE.set(colorHex, shadow);
  return shadow;
}

type Palette = {
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
};

export const PA_PALETTES: Record<PixelAvatarKind, Palette> = {
  alex:  { skin:"#f4c89a", skin2:"#c89968", hair:"#3a2a1a", hair2:"#1f1410", shirt:"#2a3a4a", shirt2:"#15202c", pants:"#1a1a1a", shoe:"#0a0a0a", eye:"#0a0a0a", mouth:"#7a4a3a" },
  mira:  { skin:"#e8b894", skin2:"#b88860", hair:"#a83a3a", hair2:"#6a1f1f", shirt:"#d8c89a", shirt2:"#a89868", pants:"#3a2a4a", shoe:"#1a1a1a", eye:"#0a0a0a", mouth:"#8a3a3a" },
  kenji: { skin:"#f0c098", skin2:"#c08868", hair:"#1a1a1a", hair2:"#0a0a0a", shirt:"#3a5a3a", shirt2:"#1a3a1a", pants:"#2a2a3a", shoe:"#0a0a0a", eye:"#0a0a0a", mouth:"#6a3a2a" },
  yuna:  { skin:"#f8d4b0", skin2:"#c8a47a", hair:"#d8a838", hair2:"#a87808", shirt:"#a83a6a", shirt2:"#681a3a", pants:"#1a1a2a", shoe:"#0a0a0a", eye:"#0a0a0a", mouth:"#8a3a4a" },
  diego: { skin:"#d8a878", skin2:"#a87848", hair:"#2a1a0a", hair2:"#1a0a00", shirt:"#5a3a8a", shirt2:"#2a1a4a", pants:"#1a1a1a", shoe:"#0a0a0a", eye:"#0a0a0a", mouth:"#7a3a2a" },
  iris:  { skin:"#f0c8a0", skin2:"#c0986a", hair:"#5a3a8a", hair2:"#2a1a4a", shirt:"#e8c8d8", shirt2:"#a888a0", pants:"#3a3a3a", shoe:"#1a1a1a", eye:"#0a0a0a", mouth:"#8a4a5a" },
  agent: { skin:"#d0d8e0", skin2:"#8090a0", hair:"#1a1a2a", hair2:"#0a0a1a", shirt:"#1a1a2a", shirt2:"#0a0a1a", pants:"#1a1a2a", shoe:"#0a0a0a", eye:"#7afff0", mouth:"#7afff0" },
};

export type PixelAvatarDirection = "S" | "N" | "E" | "W";

/** Frame layout descriptor kept for compatibility with avatar sheet contracts. */
export const HERO_FRAMES: Record<PixelAvatarDirection, readonly string[]> = {
  S: ["down:idle"],
  N: ["up:idle"],
  E: ["right:idle"],
  W: ["right:idle:flip"],
};

export const HERO_WALK_FRAMES: Record<PixelAvatarDirection, readonly string[]> = {
  S: ["down:walk0", "down:walk1", "down:walk2", "down:walk1"],
  N: ["up:walk0", "up:walk1", "up:walk2", "up:walk1"],
  E: ["right:walk0", "right:walk1", "right:walk2", "right:walk1"],
  W: ["right:walk0:flip", "right:walk1:flip", "right:walk2:flip", "right:walk1:flip"],
};

interface PixelAvatarProps {
  kind?: PixelAvatarKind;
  direction?: PixelAvatarDirection;
  size?: number;
  walking?: boolean;
  activity?: PixelActivity;
  variant?: "full" | "mugshot";
  variantKey?: string;
  agentCategory?: string;
  paletteOverride?: PixelAvatarPaletteOverride;
  style?: CSSProperties;
  className?: string;
}

export default function PixelAvatar({
  kind = "alex",
  direction = "S",
  size = 4,
  walking = false,
  activity,
  variant = "full",
  variantKey,
  agentCategory,
  paletteOverride,
  style,
  className,
}: PixelAvatarProps) {
  const basePal = PA_PALETTES[kind] || PA_PALETTES.alex;
  const pal = useMemo(
    () => (paletteOverride ? { ...basePal, ...paletteOverride } : basePal),
    [basePal, paletteOverride],
  );
  const resolvedActivity: PixelActivity = activity ?? (walking ? "walk" : "idle");

  const [step, setStep] = useState(0);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const isAnimated =
      resolvedActivity === "walk" ||
      resolvedActivity === "typing" ||
      resolvedActivity === "reading";
    if (!isAnimated) {
      return;
    }
    const intervalMs = resolvedActivity === "walk" ? 160 : 280;
    return startVisibilityPoll(() => {
      setStep((prev) => (prev + 1) % 4);
    }, intervalMs);
  }, [resolvedActivity]);

  const profile = useMemo(
    () => resolveCharacterProfile(kind, paletteOverride, variantKey, agentCategory),
    [agentCategory, kind, paletteOverride, variantKey],
  );

  // Integer canvas backing scale for ultra-crisp pixel edges
  const renderZoom =
    variant === "mugshot"
      ? Math.max(2, Math.round(size))
      : size >= 5
        ? 4
        : size >= 3
          ? 2
          : Math.max(1, Math.round(size));

  const displayWidth =
    variant === "mugshot"
      ? Math.round(size * 14)
      : size === 3
        ? 36
        : size === 6
          ? 64
          : Math.round((CHAR_FRAME_W * renderZoom));
  const displayHeight =
    variant === "mugshot"
      ? Math.round(size * 14)
      : size === 3
        ? 72
        : size === 6
          ? 128
          : Math.round((CHAR_FRAME_H * renderZoom));

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    drawPixelCharacterToCanvas(canvas, profile, {
      variant,
      direction,
      activity: resolvedActivity,
      step,
      zoom: renderZoom,
    });
  }, [direction, profile, renderZoom, resolvedActivity, step, variant]);

  return (
    <div
      className={[
        "pixel-avatar",
        walking || resolvedActivity === "walk" ? "pixel-avatar--walking" : "",
        `pixel-avatar--${variant}`,
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      style={{
        position: "relative",
        width: displayWidth,
        height: displayHeight,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        flexShrink: 0,
        imageRendering: "pixelated",
        ...style,
      }}
    >
      <canvas
        ref={canvasRef}
        className="pixel-avatar__canvas"
        aria-hidden="true"
        style={{
          display: "block",
          width: displayWidth,
          height: displayHeight,
          imageRendering: "pixelated",
        }}
      />
      <span
        className="pixel-avatar__frame"
        aria-hidden="true"
        style={{
          position: "absolute",
          width: 1,
          height: 1,
          right: 0,
          bottom: 0,
          opacity: 0,
          pointerEvents: "none",
          boxShadow: cachedMarkerShadow(pal.shirt),
        }}
      />
    </div>
  );
}

/* ---------- Pixel furniture sprites (compatibility wrapper) ---------- */

export type PixelSpriteKind =
  | "desk"
  | "monitor"
  | "chair"
  | "plant"
  | "rug"
  | "table"
  | "whiteboard"
  | "couch"
  | "coffee";

const PA_SPRITES: Record<PixelSpriteKind, string[]> = {
  desk: [
    "............",
    ".DDDDDDDDDD.",
    ".DddddddddD.",
    ".DDDDDDDDDD.",
    "..D......D..",
    "..D......D..",
  ],
  monitor: [
    "..MMMMMMMM..",
    "..MmmmmmmM..",
    "..MmSSSSmM..",
    "..MmSSSSmM..",
    "..MmmmmmmM..",
    "..MMMMMMMM..",
    "....MMMM....",
    "...MMMMMM...",
  ],
  chair: [
    "..CCCCCC..",
    "..CccccC..",
    "..CCCCCC..",
    "....CC....",
    "...CCCC...",
    "..CC..CC..",
  ],
  plant: [
    "..GG.GGG..",
    ".GGGGGGGG.",
    ".GGGGGGGG.",
    "..GGGGGG..",
    "...PPPP...",
    "...PPPP...",
  ],
  rug: [
    "RRRRRRRRRRRR",
    "RrrrrrrrrrR.",
    "RrRRRRRRRrR.",
    "RrRrrrrrRrR.",
    "RrRRRRRRRrR.",
    "RrrrrrrrrrR.",
    "RRRRRRRRRRR.",
  ],
  table: [
    "TTTTTTTTTTTT",
    "TttttttttttT",
    "TttttttttttT",
    "TTTTTTTTTTTT",
    ".T........T.",
    ".T........T.",
  ],
  whiteboard: [
    "WWWWWWWWWWWW",
    "WwwwwwwwwwwW",
    "WwLLLLLLLwW.",
    "WwLLLwwwLwW.",
    "WwLwwwwwLwW.",
    "WwwwwwwwwwwW",
    "WWWWWWWWWWWW",
    "..W......W..",
  ],
  couch: [
    "UUUUUUUUUUUU",
    "UuuuuuuuuuU.",
    "UuUUUUUUUuU.",
    "UuUuuuuuUuU.",
    "UUuUUUUUuUU.",
    ".UUuuuuuUU..",
  ],
  coffee: [
    "..QQQQQ..",
    "..QqqqQ..",
    "..QqqqQ..",
    "..QQQQQ..",
  ],
};

const PA_SPRITE_PALETTES: Record<PixelSpriteKind, Record<string, string>> = {
  desk: { D: "#6a4a2a", d: "#8a6a4a" },
  monitor: { M: "#1a1a1a", m: "#2a2a2a", S: "#3a8a8a" },
  chair: { C: "#2a2a2a", c: "#5a5a5a" },
  plant: { G: "#3a7a3a", P: "#6a4a2a" },
  rug: { R: "#8a3a3a", r: "#d8b878" },
  table: { T: "#4a3a2a", t: "#7a5a3a" },
  whiteboard: { W: "#1a1a1a", w: "#f8f8f8", L: "#3a6aaa" },
  couch: { U: "#3a3a4a", u: "#6a6a8a" },
  coffee: { Q: "#1a1a1a", q: "#a87858" },
};

interface PixelSpriteProps {
  kind?: PixelSpriteKind;
  size?: number;
  style?: CSSProperties;
  palette?: Record<string, string>;
  className?: string;
}

export function PixelSprite({
  kind = "desk",
  size = 4,
  style,
  palette,
  className,
}: PixelSpriteProps) {
  const sprite = PA_SPRITES[kind];
  const pal = palette || PA_SPRITE_PALETTES[kind];
  const shadows = useMemo(() => {
    if (!sprite || !pal) return "";
    const parts: string[] = [];
    for (let y = 0; y < sprite.length; y++) {
      for (let x = 0; x < sprite[y].length; x++) {
        const ch = sprite[y][x];
        if (ch === "." || !pal[ch]) continue;
        parts.push(`${x * size}px ${y * size}px 0 0 ${pal[ch]}`);
      }
    }
    return parts.join(",");
  }, [sprite, pal, size]);
  if (!sprite || !pal) return null;
  return (
    <div
      className={className}
      style={{
        position: "relative",
        width: sprite[0].length * size,
        height: sprite.length * size,
        imageRendering: "pixelated",
        ...style,
      }}
    >
      <div
        style={{
          position: "absolute",
          width: size,
          height: size,
          boxShadow: shadows,
        }}
      />
    </div>
  );
}
