"use client";

/**
 * Multi-layer HTML5 Canvas 2D Pixel Office Architecture & Live Collaboration Renderer.
 *
 * Combines:
 * - Panoramic city-skyline windows, @4x office furniture/props, animated aquarium & pets,
 *   and 5 developer themes (tokyo-night, catppuccin, dracula, gruvbox, daylight) adapted
 *   from pixtuoid/pixtuoid (MIT License, Copyright (c) 2026 Ivan Wang).
 * - Multi-zone parquet atrium, collaboration arcs, and animated room states inspired by
 *   GreenSheep01201/claw-empire (Apache-2.0 License) and pixel-agents-hq/pixel-agents (MIT License).
 */

import { useEffect, useMemo, useRef, useState } from "react";
import { getCachedSprite } from "@/lib/pixel-office/sprite-cache";
import { startVisibilityPoll } from "@/lib/visibility-poll";
import {
  BOOKSHELF_SPRITE,
  ESPRESSO_BAR_SPRITE,
  SERVER_RACK_SPRITE,
} from "@/lib/pixel-office/office-pixel-catalog";
import {
  getPixtuoidSpriteCanvas,
  resolveOfficeTheme,
  type OfficeAtmosphereTheme,
  type OfficeThemeId,
  type PixtuoidSpriteKey,
} from "@/lib/pixel-office/pixtuoid-sprites";
import {
  getDeskHeightPct,
  getDeskRowCenters,
  parseStagePct,
  type StageRoomSpec,
  type StageSize,
  type WorkspaceStageActor,
} from "@/lib/workspace-2d";

const AGENT_DESK_COL_CENTERS = [14, 38, 62, 85] as const;
const TILE_PX = 24;
const SKYLINE_SEQUENCE: readonly PixtuoidSpriteKey[] = [
  "building_walkup",
  "building_spire",
  "building_curtain",
  "building_setback",
  "building_dome",
  "building_twin",
  "building_slab",
  "building_sign",
  "building_low",
];

interface Rect {
  x: number;
  y: number;
  w: number;
  h: number;
}

function roomRect(room: StageRoomSpec, worldSize: StageSize): Rect {
  return {
    x: Math.round((parseStagePct(room.left) / 100) * worldSize.width),
    y: Math.round((parseStagePct(room.top) / 100) * worldSize.height),
    w: Math.round((parseStagePct(room.width) / 100) * worldSize.width),
    h: Math.round((parseStagePct(room.height) / 100) * worldSize.height),
  };
}

function drawPixtuoid(
  ctx: CanvasRenderingContext2D,
  key: PixtuoidSpriteKey,
  x: number,
  y: number,
  scale = 1.45,
  frame = 0,
): { w: number; h: number } {
  const sprite = getPixtuoidSpriteCanvas(key, frame);
  const w = Math.round(sprite.width * scale);
  const h = Math.round(sprite.height * scale);
  ctx.drawImage(sprite, Math.round(x), Math.round(y), w, h);
  return { w, h };
}

function drawWoodPlankFloor(
  ctx: CanvasRenderingContext2D,
  rect: Rect,
  tones: readonly [string, string, string, string],
): void {
  ctx.save();
  ctx.beginPath();
  ctx.rect(rect.x, rect.y, rect.w, rect.h);
  ctx.clip();

  const plankW = 36;
  const plankH = 14;
  const rows = Math.ceil(rect.h / plankH) + 1;
  const cols = Math.ceil(rect.w / plankW) + 2;

  for (let r = 0; r < rows; r++) {
    const stagger = (r & 1) === 1 ? -plankW / 2 : 0;
    const py = rect.y + r * plankH;
    for (let c = 0; c < cols; c++) {
      const px = rect.x + c * plankW + stagger;
      const hash = ((r * 7 + c * 13) ^ (r + c)) & 3;
      ctx.fillStyle = tones[hash];
      ctx.fillRect(px, py, plankW, plankH);
      ctx.fillStyle = "rgba(0, 0, 0, 0.14)";
      ctx.fillRect(px, py + plankH - 1, plankW, 1);
      ctx.fillRect(px + plankW - 1, py, 1, plankH);
      ctx.fillStyle = "rgba(255, 255, 255, 0.045)";
      ctx.fillRect(px + 2, py + 1, plankW - 6, 1);
    }
  }
  ctx.restore();
}

function drawParquetTileFloor(
  ctx: CanvasRenderingContext2D,
  rect: Rect,
  tones: readonly [string, string, string, string],
  tileSize = TILE_PX,
): void {
  if (rect.w <= 0 || rect.h <= 0) return;
  ctx.save();
  ctx.beginPath();
  ctx.rect(rect.x, rect.y, rect.w, rect.h);
  ctx.clip();

  const rows = Math.ceil(rect.h / tileSize);
  const cols = Math.ceil(rect.w / tileSize);
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const x = rect.x + c * tileSize;
      const y = rect.y + r * tileSize;
      const toneIdx = ((r + c) & 1) === 0 ? ((r * 3 + c) & 1) : 2 + ((r + c * 5) & 1);
      ctx.fillStyle = tones[toneIdx];
      ctx.fillRect(x, y, tileSize, tileSize);
      ctx.fillStyle = "rgba(0, 0, 0, 0.10)";
      ctx.fillRect(x, y + tileSize - 1, tileSize, 1);
      ctx.fillRect(x + tileSize - 1, y, 1, tileSize);
      ctx.fillStyle = "rgba(255, 255, 255, 0.04)";
      ctx.fillRect(x + 1, y + 1, tileSize - 3, 1);
    }
  }
  ctx.restore();
}

function drawWovenCarpet(
  ctx: CanvasRenderingContext2D,
  rect: Rect,
  colors: { main: string; border: string; accent: string },
): void {
  const x = Math.round(rect.x);
  const y = Math.round(rect.y);
  const w = Math.round(rect.w);
  const h = Math.round(rect.h);
  if (w < 24 || h < 24) return;

  ctx.fillStyle = "rgba(0, 0, 0, 0.24)";
  ctx.fillRect(x + 2, y + 2, w, h);

  ctx.fillStyle = colors.border;
  ctx.fillRect(x, y, w, h);

  ctx.fillStyle = colors.main;
  ctx.fillRect(x + 4, y + 4, w - 8, h - 8);

  ctx.strokeStyle = colors.accent;
  ctx.lineWidth = 2;
  ctx.globalAlpha = 0.34;
  ctx.strokeRect(x + 9, y + 9, w - 18, h - 18);

  ctx.globalAlpha = 0.08;
  ctx.fillStyle = colors.accent;
  for (let py = y + 14; py < y + h - 14; py += 16) {
    for (let px = x + 14; px < x + w - 14; px += 16) {
      if (((px + py) & 16) === 0) {
        ctx.fillRect(px, py, 6, 6);
      }
    }
  }

  ctx.globalAlpha = 0.42;
  for (let fy = y + 4; fy < y + h - 4; fy += 6) {
    ctx.fillRect(x - 3, fy, 3, 3);
    ctx.fillRect(x + w, fy, 3, 3);
  }
  ctx.globalAlpha = 1;
}

function drawWarmLampHalo(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  radius: number,
  theme: OfficeAtmosphereTheme,
): void {
  ctx.save();
  ctx.globalAlpha = theme.lampHaloAlpha;
  const grad = ctx.createRadialGradient(cx, cy, 4, cx, cy, radius);
  grad.addColorStop(0, theme.lampHaloColor);
  grad.addColorStop(1, "rgba(0, 0, 0, 0)");
  ctx.fillStyle = grad;
  ctx.fillRect(cx - radius, cy - radius, radius * 2, radius * 2);
  ctx.restore();
}

function drawSkylineWindowStrip(
  ctx: CanvasRenderingContext2D,
  x: number,
  w: number,
  theme: OfficeAtmosphereTheme,
): void {
  const skyX = x;
  const skyY = 8;
  const skyH = 74;

  // Frame trim
  ctx.fillStyle = theme.wallOuter;
  ctx.fillRect(skyX - 4, skyY - 4, w + 8, skyH + 10);

  // Sky gradient
  const skyGrad = ctx.createLinearGradient(0, skyY, 0, skyY + skyH);
  skyGrad.addColorStop(0, theme.skyTop);
  skyGrad.addColorStop(1, theme.skyBottom);
  ctx.fillStyle = skyGrad;
  ctx.fillRect(skyX, skyY, w, skyH);

  // Stars (night/dark themes) or sunlit clouds (daylight)
  if (theme.kind === "dark") {
    ctx.fillStyle = theme.starColor;
    for (let i = 0; i < 42; i++) {
      const sx = skyX + ((i * 197 + 31) % (w - 16)) + 8;
      const sy = skyY + ((i * 53 + 7) % 34) + 4;
      ctx.globalAlpha = i % 3 === 0 ? 0.85 : 0.45;
      ctx.fillRect(sx, sy, i % 4 === 0 ? 2 : 1, i % 4 === 0 ? 2 : 1);
    }
    ctx.globalAlpha = 1;
  } else {
    ctx.fillStyle = "rgba(255, 255, 255, 0.72)";
    for (let cx = skyX + 48; cx < skyX + w - 90; cx += 210) {
      ctx.fillRect(cx, skyY + 12, 54, 10);
      ctx.fillRect(cx + 12, skyY + 6, 30, 6);
    }
  }

  // City skyline silhouette row from pixtuoid building sprites
  ctx.save();
  ctx.beginPath();
  ctx.rect(skyX, skyY, w, skyH);
  ctx.clip();

  let cursorX = skyX + 8;
  let seqIdx = 0;
  while (cursorX < skyX + w - 12) {
    const key = SKYLINE_SEQUENCE[seqIdx % SKYLINE_SEQUENCE.length];
    const sprite = getPixtuoidSpriteCanvas(key);
    const scale = 1.05;
    const bw = Math.round(sprite.width * scale);
    const bh = Math.round(sprite.height * scale);
    const by = skyY + skyH - Math.min(skyH - 4, bh);
    ctx.drawImage(sprite, cursorX, by, bw, Math.min(skyH - 2, bh));

    // Accent rooftop beacon or lit window glints matching theme
    ctx.fillStyle = theme.buildingWindowLit;
    ctx.globalAlpha = theme.kind === "dark" ? 0.72 : 0.45;
    for (let wy = by + 8; wy < skyY + skyH - 8; wy += 9) {
      for (let wx = cursorX + 4; wx < cursorX + bw - 6; wx += 7) {
        if (((wx + wy + seqIdx) % 3) === 0) {
          ctx.fillRect(wx, wy, 3, 4);
        }
      }
    }
    ctx.globalAlpha = 1;
    cursorX += bw + (seqIdx % 2 === 0 ? 6 : 3);
    seqIdx += 1;
  }
  ctx.restore();

  // Panoramic window glass mullions and sill
  ctx.fillStyle = "rgba(255, 255, 255, 0.07)";
  ctx.fillRect(skyX, skyY + 4, w, 14);
  ctx.fillStyle = theme.wallOuter;
  for (let mx = skyX + 120; mx < skyX + w - 60; mx += 140) {
    ctx.fillRect(mx, skyY, 4, skyH);
    ctx.fillStyle = theme.wallTrimLight;
    ctx.fillRect(mx + 1, skyY, 1, skyH);
    ctx.fillStyle = theme.wallOuter;
  }
  ctx.fillStyle = theme.wallTrimLight;
  ctx.fillRect(skyX - 4, skyY + skyH, w + 8, 5);
}

function drawArchitecturalWalls(
  ctx: CanvasRenderingContext2D,
  room: StageRoomSpec,
  rect: Rect,
  theme: OfficeAtmosphereTheme,
): void {
  const t = 8;
  const faceH = 14;
  const doorCenterX = rect.x + rect.w * room.doorAt;
  const doorCenterY = rect.y + rect.h * room.doorAt;
  const doorHalfW = Math.max(26, rect.w * 0.05);
  const doorHalfH = Math.max(22, rect.h * 0.06);

  const drawWallSegment = (sx: number, sy: number, sw: number, sh: number, isHorizontal: boolean) => {
    if (sw <= 0 || sh <= 0) return;
    ctx.fillStyle = theme.wallOuter;
    ctx.fillRect(sx, sy, sw, sh);
    if (isHorizontal) {
      ctx.fillStyle = theme.wallInner;
      ctx.fillRect(sx, sy + sh, sw, faceH);
      ctx.fillStyle = theme.wallTrimLight;
      ctx.fillRect(sx, sy + sh + faceH - 3, sw, 3);
      ctx.fillStyle = theme.wallTrimDark;
      ctx.fillRect(sx, sy, sw, 2);
    } else {
      ctx.fillStyle = theme.wallTrimDark;
      ctx.fillRect(sx, sy, 2, sh);
      ctx.fillRect(sx + sw - 2, sy, 2, sh);
    }
  };

  if (room.doorSide === "top") {
    const gapL = Math.max(rect.x, doorCenterX - doorHalfW);
    const gapR = Math.min(rect.x + rect.w, doorCenterX + doorHalfW);
    drawWallSegment(rect.x - t / 2, rect.y - t, gapL - (rect.x - t / 2), t, true);
    drawWallSegment(gapR, rect.y - t, rect.x + rect.w + t / 2 - gapR, t, true);
    ctx.fillStyle = "#c08446";
    ctx.fillRect(gapL, rect.y - 2, gapR - gapL, 6);
  } else {
    drawWallSegment(rect.x - t / 2, rect.y - t, rect.w + t, t, true);
    const windowSpacing = 136;
    for (let wx = rect.x + 42; wx < rect.x + rect.w - 76; wx += windowSpacing) {
      ctx.fillStyle = theme.wallTrimDark;
      ctx.fillRect(wx, rect.y + 1, 56, faceH - 4);
      ctx.fillStyle = theme.skyBottom;
      ctx.fillRect(wx + 2, rect.y + 3, 52, faceH - 8);
      ctx.fillStyle = "rgba(255, 255, 255, 0.25)";
      ctx.fillRect(wx + 6, rect.y + 4, 16, 2);
    }
  }

  if (room.doorSide === "bottom") {
    const gapL = Math.max(rect.x, doorCenterX - doorHalfW);
    const gapR = Math.min(rect.x + rect.w, doorCenterX + doorHalfW);
    drawWallSegment(rect.x - t / 2, rect.y + rect.h - t / 2, gapL - (rect.x - t / 2), t, false);
    drawWallSegment(gapR, rect.y + rect.h - t / 2, rect.x + rect.w + t / 2 - gapR, t, false);
    ctx.fillStyle = "#c08446";
    ctx.fillRect(gapL, rect.y + rect.h - 4, gapR - gapL, 6);
  } else {
    drawWallSegment(rect.x - t / 2, rect.y + rect.h - t / 2, rect.w + t, t, false);
  }

  if (room.doorSide === "left") {
    const gapT = Math.max(rect.y, doorCenterY - doorHalfH);
    const gapB = Math.min(rect.y + rect.h, doorCenterY + doorHalfH);
    drawWallSegment(rect.x - t / 2, rect.y - t, t, gapT - (rect.y - t), false);
    drawWallSegment(rect.x - t / 2, gapB, t, rect.y + rect.h + t / 2 - gapB, false);
    ctx.fillStyle = "#c08446";
    ctx.fillRect(rect.x - 4, gapT, 6, gapB - gapT);
  } else {
    drawWallSegment(rect.x - t / 2, rect.y - t, t, rect.h + t * 1.5, false);
  }

  if (room.doorSide === "right") {
    const gapT = Math.max(rect.y, doorCenterY - doorHalfH);
    const gapB = Math.min(rect.y + rect.h, doorCenterY + doorHalfH);
    drawWallSegment(rect.x + rect.w - t / 2, rect.y - t, t, gapT - (rect.y - t), false);
    drawWallSegment(rect.x + rect.w - t / 2, gapB, t, rect.y + rect.h + t / 2 - gapB, false);
    ctx.fillStyle = "#c08446";
    ctx.fillRect(rect.x + rect.w - 2, gapT, 6, gapB - gapT);
  } else {
    drawWallSegment(rect.x + rect.w - t / 2, rect.y - t, t, rect.h + t * 1.5, false);
  }
}

function drawAtriumAndTerrace(
  ctx: CanvasRenderingContext2D,
  eng: Rect,
  hud: Rect,
  rest: Rect,
  lib: Rect,
  theme: OfficeAtmosphereTheme,
): void {
  const outerLeft = Math.min(eng.x, rest.x);
  const outerRight = Math.max(hud.x + hud.w, lib.x + lib.w);
  const fullBottom = Math.max(rest.y + rest.h, lib.y + lib.h);

  // Full contiguous interior oak/parquet base floor (zero dead void inside studio boundary)
  drawParquetTileFloor(
    ctx,
    { x: outerLeft, y: eng.y, w: outerRight - outerLeft, h: fullBottom - eng.y },
    theme.hallwayTiles,
    TILE_PX,
  );

  // Woven runner rugs along central vertical & horizontal circulation corridors
  const atriumLeft = eng.x + eng.w;
  const atriumW = Math.max(36, hud.x - atriumLeft);
  const runnerV = {
    x: atriumLeft + Math.round(atriumW * 0.25),
    y: eng.y + 18,
    w: Math.max(24, Math.round(atriumW * 0.5)),
    h: fullBottom - eng.y - 36,
  };
  ctx.fillStyle = theme.runnerEdge;
  ctx.fillRect(runnerV.x - 2, runnerV.y, runnerV.w + 4, runnerV.h);
  ctx.fillStyle = theme.runnerBase;
  ctx.fillRect(runnerV.x, runnerV.y + 2, runnerV.w, runnerV.h - 4);

  const crossTop = eng.y + eng.h;
  const crossH = Math.max(40, rest.y - crossTop);
  const runnerH = {
    x: outerLeft + 32,
    y: crossTop + Math.round(crossH * 0.34),
    w: outerRight - outerLeft - 64,
    h: Math.max(28, Math.round(crossH * 0.36)),
  };
  ctx.fillStyle = theme.runnerEdge;
  ctx.fillRect(runnerH.x, runnerH.y - 2, runnerH.w, runnerH.h + 4);
  ctx.fillStyle = theme.runnerBase;
  ctx.fillRect(runnerH.x + 2, runnerH.y, runnerH.w - 4, runnerH.h);

  // North foyer props along atrium entrance & cross corridor
  drawPixtuoid(ctx, "door", atriumLeft - 2, eng.y - 28, 1.0, 1);
  drawPixtuoid(ctx, "exit_sign", atriumLeft + 16, eng.y + 34, 1.35);
  drawPixtuoid(ctx, "coat_rack", outerLeft + 24, crossTop + 20, 1.45);
  drawPixtuoid(ctx, "plant_tall", outerLeft + 74, crossTop + 26, 1.35);

  // Cross-corridor refreshment & welcome station near central intersection
  drawPixtuoid(ctx, "vending_machine", atriumLeft + 92, crossTop + 16, 1.45, 0);
  drawPixtuoid(ctx, "water_cooler", atriumLeft + 144, crossTop + 34, 1.4, 0);
  drawPixtuoid(ctx, "plant_flower", atriumLeft + 184, crossTop + 40, 1.35);
  drawPixtuoid(ctx, "wall_clock", atriumLeft + Math.round(atriumW / 2) - 18, crossTop + 14, 1.2);
  drawPixtuoid(ctx, "plant_tall", outerRight - 64, crossTop + 24, 1.45);

  // South-west promenade terrace below Lounge (rest.y + rest.h .. lib.y + lib.h)
  const terraceTop = rest.y + rest.h;
  const terraceH = fullBottom - terraceTop;
  if (terraceH >= 60) {
    const terraceRect = { x: rest.x, y: terraceTop, w: rest.w, h: terraceH };
    drawWovenCarpet(
      ctx,
      { x: terraceRect.x + 36, y: terraceRect.y + 18, w: terraceRect.w - 72, h: terraceH - 36 },
      { main: theme.runnerBase, border: theme.runnerEdge, accent: theme.lampHaloColor },
    );
    drawWarmLampHalo(ctx, terraceRect.x + 88, terraceRect.y + 34, 72, theme);
    drawPixtuoid(ctx, "floor_lamp", terraceRect.x + 76, terraceRect.y + 16, 1.35);
    drawPixtuoid(ctx, "back_couch", terraceRect.x + 128, terraceRect.y + 28, 1.5);
    drawPixtuoid(ctx, "side_table", terraceRect.x + 190, terraceRect.y + 46, 1.4);
    drawPixtuoid(ctx, "desk_cup", terraceRect.x + 202, terraceRect.y + 48, 1.4);
    drawPixtuoid(ctx, "back_couch", terraceRect.x + 244, terraceRect.y + 28, 1.5);
    drawPixtuoid(ctx, "poinsettia", terraceRect.x + 332, terraceRect.y + 38, 1.4);
    drawPixtuoid(ctx, "plant_flower", terraceRect.x + 410, terraceRect.y + 32, 1.45);
    drawPixtuoid(ctx, "meeting_sofa", terraceRect.x + 468, terraceRect.y + 34, 1.35);

    // Subtle glass balcony rail along bottom edge of terrace
    ctx.fillStyle = theme.wallTrimDark;
    ctx.fillRect(terraceRect.x, fullBottom - 6, terraceRect.w, 6);
    ctx.fillStyle = theme.wallTrimLight;
    ctx.fillRect(terraceRect.x, fullBottom - 6, terraceRect.w, 2);
  }
}

function drawRoomInteriors(
  ctx: CanvasRenderingContext2D,
  rooms: StageRoomSpec[],
  worldSize: StageSize,
  theme: OfficeAtmosphereTheme,
): void {
  const roomMap = new Map(rooms.map((r) => [r.variant, roomRect(r, worldSize)]));
  const eng = roomMap.get("engineering");
  const hud = roomMap.get("huddle");
  const rest = roomMap.get("rest");
  const lib = roomMap.get("library");

  if (eng && hud && rest && lib) {
    drawAtriumAndTerrace(ctx, eng, hud, rest, lib, theme);
  }

  for (const room of rooms) {
    const rect = roomMap.get(room.variant);
    if (!rect) continue;

    if (room.variant === "engineering") {
      drawWoodPlankFloor(ctx, rect, theme.engineeringFloor);
      drawWovenCarpet(
        ctx,
        { x: rect.x + 24, y: rect.y + 32, w: rect.w - 92, h: rect.h - 54 },
        { main: theme.runnerBase, border: theme.wallTrimLight, accent: theme.buildingWindowLit },
      );
    } else if (room.variant === "huddle") {
      drawParquetTileFloor(ctx, rect, theme.huddleFloor, TILE_PX);
      drawWovenCarpet(
        ctx,
        { x: rect.x + 44, y: rect.y + 72, w: Math.round(rect.w * 0.54), h: rect.h - 96 },
        { main: theme.runnerBase, border: theme.runnerEdge, accent: theme.lampHaloColor },
      );
      drawWovenCarpet(
        ctx,
        {
          x: rect.x + Math.round(rect.w * 0.66),
          y: rect.y + 72,
          w: Math.round(rect.w * 0.3),
          h: rect.h - 96,
        },
        { main: theme.runnerBase, border: theme.wallTrimLight, accent: theme.starColor },
      );
    } else if (room.variant === "rest") {
      drawParquetTileFloor(ctx, rect, theme.loungeFloor, TILE_PX);
      drawWovenCarpet(
        ctx,
        { x: rect.x + 68, y: rect.y + 92, w: rect.w - 136, h: rect.h - 122 },
        { main: theme.runnerBase, border: theme.runnerEdge, accent: theme.lampHaloColor },
      );
    } else if (room.variant === "library") {
      drawWoodPlankFloor(ctx, rect, theme.libraryFloor);
      drawWovenCarpet(
        ctx,
        { x: rect.x + 44, y: rect.y + 96, w: Math.round(rect.w * 0.54), h: rect.h - 140 },
        { main: theme.runnerBase, border: theme.runnerEdge, accent: theme.lampHaloColor },
      );
      drawWovenCarpet(
        ctx,
        {
          x: rect.x + Math.round(rect.w * 0.64),
          y: rect.y + 96,
          w: Math.round(rect.w * 0.31),
          h: rect.h - 140,
        },
        { main: theme.runnerBase, border: theme.wallTrimLight, accent: theme.buildingWindowLit },
      );
    }
  }

  for (const room of rooms) {
    const rect = roomMap.get(room.variant);
    if (!rect) continue;
    drawArchitecturalWalls(ctx, room, rect, theme);
  }

  // Static Zone Props & Architecture Details
  if (eng) {
    drawPixtuoid(ctx, "bulletin_board", eng.x + 36, eng.y + 18, 1.35);
    drawPixtuoid(ctx, "wall_clock", eng.x + Math.round(eng.w * 0.5) - 18, eng.y + 16, 1.15);
    drawPixtuoid(ctx, "standing_desk", eng.x + Math.round(eng.w * 0.72), eng.y + 22, 1.35);

    const rack = getCachedSprite(SERVER_RACK_SPRITE, 3);
    ctx.drawImage(rack, eng.x + eng.w - rack.width - 14, eng.y + 52);
    drawPixtuoid(ctx, "filing_cabinet", eng.x + eng.w - 42, eng.y + 148, 1.45);
    drawWarmLampHalo(ctx, eng.x + eng.w - 30, eng.y + 34, 72, theme);
    drawPixtuoid(ctx, "floor_lamp", eng.x + eng.w - 40, eng.y + 20, 1.3);
    drawPixtuoid(ctx, "plant_tall", eng.x + 14, eng.y + eng.h - 68, 1.4);
  }

  if (hud) {
    // West Boardroom wall boards & screen
    drawPixtuoid(ctx, "notice_board", hud.x + 36, hud.y + 20, 1.35);
    drawPixtuoid(ctx, "meeting_screen", hud.x + 196, hud.y + 18, 1.45);
    drawPixtuoid(ctx, "whiteboard", hud.x + 318, hud.y + 20, 1.45);

    // Sleek conference table between HUDDLE_SAFE_SEATS (20% and 42% width)
    const tx = hud.x + Math.round(hud.w * 0.25);
    const ty = hud.y + Math.round(hud.h * 0.49);
    const tw = Math.round(hud.w * 0.135);
    const th = Math.round(hud.h * 0.3);
    ctx.fillStyle = "rgba(0, 0, 0, 0.28)";
    ctx.fillRect(tx + 4, ty + 6, tw, th);
    ctx.fillStyle = "#5c3d24";
    ctx.fillRect(tx, ty, tw, th);
    ctx.fillStyle = "#8a5f3b";
    ctx.fillRect(tx + 4, ty + 4, tw - 8, th - 10);
    ctx.fillStyle = "rgba(56, 189, 248, 0.24)";
    ctx.fillRect(tx + 12, ty + 12, tw - 24, th - 26);

    drawPixtuoid(ctx, "meeting_chair", tx - 38, ty + 6, 1.35);
    drawPixtuoid(ctx, "meeting_chair", tx + tw + 4, ty + 6, 1.35);
    drawPixtuoid(ctx, "meeting_chair", tx - 38, ty + th - 38, 1.35);
    drawPixtuoid(ctx, "meeting_chair", tx + tw + 4, ty + th - 38, 1.35);

    // Interior glass partition splitting Boardroom & Focus Pod Suite
    const partX = hud.x + Math.round(hud.w * 0.62);
    ctx.fillStyle = theme.wallOuter;
    ctx.fillRect(partX, hud.y + 14, 6, 98);
    ctx.fillRect(partX, hud.y + hud.h - 72, 6, 68);
    ctx.fillStyle = theme.skyBottom;
    ctx.globalAlpha = 0.45;
    ctx.fillRect(partX + 1, hud.y + 18, 4, 90);
    ctx.fillRect(partX + 1, hud.y + hud.h - 68, 4, 60);
    ctx.globalAlpha = 1;

    // East Focus Pod Suite
    drawPixtuoid(ctx, "phone_booth", partX + 26, hud.y + 24, 1.45);
    drawPixtuoid(ctx, "phone_booth", partX + 76, hud.y + 24, 1.45);
    drawPixtuoid(ctx, "tv_stand", hud.x + hud.w - 150, hud.y + 26, 1.45);
    drawWarmLampHalo(ctx, hud.x + hud.w - 44, hud.y + 44, 76, theme);
    drawPixtuoid(ctx, "floor_lamp", hud.x + hud.w - 54, hud.y + 22, 1.35);
    drawPixtuoid(ctx, "meeting_sofa", partX + 52, hud.y + 188, 1.45);
    drawPixtuoid(ctx, "side_table", partX + 180, hud.y + 196, 1.45);
    drawPixtuoid(ctx, "plant_tall", partX + 236, hud.y + 168, 1.45);
  }

  if (rest) {
    // Pantry + Snack Shelf + Espresso bar along north/west edge
    drawPixtuoid(ctx, "pantry", rest.x + 28, rest.y + 20, 1.25);
    drawPixtuoid(ctx, "snack_shelf", rest.x + 202, rest.y + 20, 1.35);
    drawPixtuoid(ctx, "kitchen_island", rest.x + 128, rest.y + 110, 1.35);

    const espresso = getCachedSprite(ESPRESSO_BAR_SPRITE, 3);
    ctx.drawImage(espresso, rest.x + 28, rest.y + rest.h - espresso.height - 22);

    drawWarmLampHalo(ctx, rest.x + rest.w - 64, rest.y + 148, 78, theme);
    drawPixtuoid(ctx, "floor_lamp", rest.x + rest.w - 72, rest.y + 124, 1.35);
    drawPixtuoid(ctx, "meeting_sofa", rest.x + Math.round(rest.w * 0.42), rest.y + Math.round(rest.h * 0.62), 1.4);
    drawPixtuoid(ctx, "side_table", rest.x + Math.round(rest.w * 0.66), rest.y + Math.round(rest.h * 0.66), 1.35);
    drawPixtuoid(ctx, "poinsettia", rest.x + 24, rest.y + 28, 1.3);
    drawPixtuoid(ctx, "plant_succulent", rest.x + rest.w - 52, rest.y + rest.h - 46, 1.35);
  }

  if (lib) {
    // West Archive & Reading Desks
    const shelf = getCachedSprite(BOOKSHELF_SPRITE, 3);
    for (let i = 0; i < 4; i++) {
      ctx.drawImage(shelf, lib.x + 34 + i * (shelf.width + 26), lib.y + 22);
    }
    drawPixtuoid(ctx, "filing_cabinet", lib.x + 420, lib.y + 24, 1.5);
    drawPixtuoid(ctx, "filing_cabinet", lib.x + 452, lib.y + 24, 1.5);

    // Study desks aligned between LIBRARY_SAFE_SEATS (22%, 38% width)
    const rx = lib.x + Math.round(lib.w * 0.19);
    const ry = lib.y + Math.round(lib.h * 0.52);
    const rw = Math.round(lib.w * 0.24);
    const rh = 38;
    ctx.fillStyle = "rgba(0,0,0,0.32)";
    ctx.fillRect(rx + 4, ry + 5, rw, rh);
    ctx.fillStyle = "#5c3d24";
    ctx.fillRect(rx, ry, rw, rh);
    ctx.fillStyle = "#8a5f3b";
    ctx.fillRect(rx + 4, ry + 3, rw - 8, rh - 10);
    drawWarmLampHalo(ctx, rx + 56, ry + 16, 68, theme);
    ctx.fillStyle = "#16a34a";
    ctx.fillRect(rx + 48, ry + 8, 16, 10);
    ctx.fillStyle = "#fbbf24";
    ctx.fillRect(rx + 54, ry + 18, 4, 8);
    ctx.fillStyle = "#e2e8f0";
    ctx.fillRect(rx + 108, ry + 10, 26, 18);
    ctx.fillRect(rx + 178, ry + 12, 24, 16);
    drawPixtuoid(ctx, "desk_cup", rx + 148, ry + 14, 1.35);

    ctx.drawImage(shelf, lib.x + 34, lib.y + lib.h - shelf.height - 22);
    ctx.drawImage(shelf, lib.x + 34 + shelf.width + 26, lib.y + lib.h - shelf.height - 22);

    // East Artifact Vault (hosts QUEST_MARKER_POSITION)
    const vaultX = lib.x + Math.round(lib.w * 0.64);
    drawPixtuoid(ctx, "bulletin_board", vaultX + 24, lib.y + 22, 1.4);
    drawPixtuoid(ctx, "standing_desk", vaultX + 128, lib.y + 28, 1.45);
    drawPixtuoid(ctx, "wall_clock", vaultX + 220, lib.y + 20, 1.25);

    drawWarmLampHalo(ctx, lib.x + lib.w - 118, lib.y + Math.round(lib.h * 0.62), 92, theme);
    drawPixtuoid(ctx, "floor_lamp", lib.x + lib.w - 58, lib.y + 30, 1.45);
    drawPixtuoid(ctx, "plant_tall", lib.x + lib.w - 58, lib.y + lib.h - 78, 1.45);
    drawPixtuoid(ctx, "plant_flower", vaultX + 18, lib.y + lib.h - 68, 1.45);
  }
}

function drawLiveCollaborationArcs(
  ctx: CanvasRenderingContext2D,
  actors: WorkspaceStageActor[],
  playerActorId: string | null,
  worldSize: StageSize,
  animTick: number,
  animateDots: boolean,
  theme: OfficeAtmosphereTheme,
): void {
  const hub =
    (playerActorId ? actors.find((a) => a.id === playerActorId) : undefined) ??
    actors.find((a) => a.agentCategory?.toUpperCase() === "ORCHESTRATOR") ??
    actors[0];
  if (!hub) return;

  const hx = (parseStagePct(hub.position.left) / 100) * worldSize.width;
  const hy = (parseStagePct(hub.position.top) / 100) * worldSize.height - 14;

  const targets = actors
    .filter(
      (actor) =>
        actor.id !== hub.id &&
        actor.kind === "agent" &&
        (actor.status === "working" || actor.status === "review"),
    )
    .slice(0, 6);

  if (targets.length === 0) return;

  const arcStroke = theme.kind === "dark" ? "#38bdf8" : "#0284c7";
  const dotColor = theme.lampHaloColor;
  const phase = animateDots ? (animTick % 12) / 12 : 0.5;

  ctx.save();
  for (let i = 0; i < targets.length; i++) {
    const target = targets[i];
    const tx = (parseStagePct(target.position.left) / 100) * worldSize.width;
    const ty = (parseStagePct(target.position.top) / 100) * worldSize.height - 14;
    const mx = (hx + tx) * 0.5;
    const dist = Math.hypot(tx - hx, ty - hy);
    const lift = Math.min(90, Math.max(28, dist * 0.16));
    const cy = Math.min(hy, ty) - lift;

    ctx.beginPath();
    ctx.setLineDash([6, 6]);
    ctx.lineDashOffset = animateDots ? -(animTick * 3) : 0;
    ctx.lineWidth = 2;
    ctx.strokeStyle = arcStroke;
    ctx.globalAlpha = 0.42;
    ctx.moveTo(hx, hy);
    ctx.quadraticCurveTo(mx, cy, tx, ty);
    ctx.stroke();

    // Traveling or static collaboration signal packet along quadratic curve
    const t = phase;
    const inv = 1 - t;
    const px = inv * inv * hx + 2 * inv * t * mx + t * t * tx;
    const py = inv * inv * hy + 2 * inv * t * cy + t * t * ty;

    ctx.setLineDash([]);
    ctx.globalAlpha = 0.92;
    ctx.fillStyle = dotColor;
    ctx.beginPath();
    ctx.arc(px, py, 3.5, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.restore();
}

export function OfficeTilemapCanvas({
  rooms,
  engineeringRows,
  actors,
  worldSize,
  themeId = "auto",
  showCollabLinks = true,
  motionPaused = false,
  playerActorId = null,
}: {
  rooms: StageRoomSpec[];
  engineeringRows: number;
  actors: WorkspaceStageActor[];
  worldSize: StageSize;
  themeId?: OfficeThemeId;
  showCollabLinks?: boolean;
  motionPaused?: boolean;
  playerActorId?: string | null;
}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const staticBackdropRef = useRef<HTMLCanvasElement | null>(null);
  const [animTick, setAnimTick] = useState(0);
  const [isLightTheme, setIsLightTheme] = useState(() =>
    typeof document !== "undefined" && document.documentElement.dataset.theme === "light",
  );
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    if (typeof document === "undefined") return;
    const syncTheme = () => {
      setIsLightTheme(document.documentElement.dataset.theme === "light");
    };
    syncTheme();
    const observer = new MutationObserver(syncTheme);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["data-theme"],
    });
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (typeof window === "undefined" || typeof window.matchMedia !== "function") return;
    const mql = window.matchMedia("(prefers-reduced-motion: reduce)");
    const syncMotion = () => setPrefersReducedMotion(mql.matches);
    syncMotion();
    mql.addEventListener?.("change", syncMotion);
    return () => mql.removeEventListener?.("change", syncMotion);
  }, []);

  const animateStage = !motionPaused && !prefersReducedMotion;

  useEffect(() => {
    if (!animateStage) return;
    return startVisibilityPoll(() => {
      setAnimTick((prev) => (prev + 1) % 60);
    }, 650);
  }, [animateStage]);

  const theme = useMemo(
    () => resolveOfficeTheme(themeId, isLightTheme ? "light" : "dark"),
    [isLightTheme, themeId],
  );

  const engineeringAgents = useMemo(
    () =>
      actors.filter(
        (actor) =>
          actor.kind === "agent" &&
          (actor.roomId === "engineering" || !actor.roomId),
      ),
    [actors],
  );

  useEffect(() => {
    if (typeof document === "undefined") return;
    const offscreen = document.createElement("canvas");
    offscreen.width = worldSize.width;
    offscreen.height = worldSize.height;
    const ctx = offscreen.getContext("2d");
    if (!ctx) return;
    ctx.imageSmoothingEnabled = false;

    // 1. Studio Backdrop
    ctx.fillStyle = theme.backdrop;
    ctx.fillRect(0, 0, worldSize.width, worldSize.height);
    ctx.fillStyle = theme.kind === "light" ? "rgba(30, 64, 175, 0.07)" : "rgba(148, 163, 184, 0.08)";
    for (let y = TILE_PX; y < worldSize.height; y += TILE_PX * 2) {
      for (let x = TILE_PX; x < worldSize.width; x += TILE_PX * 2) {
        ctx.fillRect(x, y, 2, 2);
      }
    }

    // 2. Panoramic City-Skyline Exterior Window Band
    drawSkylineWindowStrip(ctx, 40, 1710, theme);

    // 3. Connected Parquet Atrium, Terrace & Multi-Zone Room Interiors
    drawRoomInteriors(ctx, rooms, worldSize, theme);

    staticBackdropRef.current = offscreen;
  }, [rooms, theme, worldSize]);

  useEffect(() => {
    const canvas = canvasRef.current;
    const backdrop = staticBackdropRef.current;
    if (!canvas || !backdrop) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(backdrop, 0, 0);

    const engSpec = rooms.find((r) => r.variant === "engineering");
    const restSpec = rooms.find((r) => r.variant === "rest");
    const libSpec = rooms.find((r) => r.variant === "library");
    const frameEven = (animTick & 1) === 0;

    // 1. Animated Engineering Workstations (1:1 aligned to AGENT_DESK_COL_CENTERS x getDeskRowCenters)
    if (engSpec) {
      const eng = roomRect(engSpec, worldSize);
      const rowCenters = getDeskRowCenters(engineeringRows);
      const deskPctH = getDeskHeightPct(engineeringRows);
      const chairCanvas = getPixtuoidSpriteCanvas("desk_chair");
      const deskCanvas = getPixtuoidSpriteCanvas("desk_north");
      const chairScale = 1.35;
      const deskScale = 1.42;
      const chairW = Math.round(chairCanvas.width * chairScale);
      const chairH = Math.round(chairCanvas.height * chairScale);
      const deskW = Math.round(deskCanvas.width * deskScale);
      const deskH = Math.round(deskCanvas.height * deskScale);

      let slotIndex = 0;
      for (let r = 0; r < rowCenters.length; r++) {
        const centerY = eng.y + (rowCenters[r] / 100) * eng.h + ((deskPctH / 100) * eng.h) * 0.14;
        for (let c = 0; c < AGENT_DESK_COL_CENTERS.length; c++) {
          const centerX = eng.x + (AGENT_DESK_COL_CENTERS[c] / 100) * eng.w;
          const slotAgent = engineeringAgents[slotIndex];
          const isOccupied = Boolean(slotAgent);
          const deskX = Math.round(centerX - deskW / 2);
          const deskY = Math.round(centerY - deskH / 2);

          if (isOccupied) {
            const glow = ctx.createRadialGradient(centerX, deskY + 18, 4, centerX, deskY + 18, 56);
            glow.addColorStop(0, "rgba(56, 189, 248, 0.24)");
            glow.addColorStop(1, "rgba(56, 189, 248, 0)");
            ctx.fillStyle = glow;
            ctx.fillRect(centerX - 58, deskY - 22, 116, 94);
          }

          ctx.drawImage(
            chairCanvas,
            Math.round(centerX - chairW / 2),
            deskY - Math.round(chairH * 0.44),
            chairW,
            chairH,
          );
          ctx.drawImage(deskCanvas, deskX, deskY, deskW, deskH);
          drawPixtuoid(ctx, "desk_cup", deskX + deskW - 14, deskY + 14, 1.25);

          if (isOccupied) {
            // Animated dual-monitor code lines & status badge
            const monY = deskY + 9;
            ctx.fillStyle = frameEven ? "#38bdf8" : "#22d3ee";
            ctx.fillRect(deskX + 18, monY, 16, 8);
            ctx.fillStyle = frameEven ? "#a78bfa" : "#34d399";
            ctx.fillRect(deskX + 42, monY, 16, 8);
            const iconKey: PixtuoidSpriteKey =
              slotAgent.status === "blocked"
                ? "icon_warning"
                : slotAgent.status === "review"
                  ? "icon_requires_action"
                  : slotAgent.status === "working"
                    ? "icon_running"
                    : "icon_check";
            drawPixtuoid(ctx, iconKey, deskX + deskW - 18, deskY - 6, 1.3);
          }
          slotIndex += 1;
        }
      }

      // Live animated token telemetry stack + printer near east telemetry bay
      drawPixtuoid(ctx, "token_tower", eng.x + eng.w - 46, eng.y + 206, 1.45, animTick & 1);
      drawPixtuoid(ctx, "token_sheet", eng.x + eng.w - 42, eng.y + 244, 1.45);
      drawPixtuoid(ctx, "printer", eng.x + eng.w - 58, eng.y + 260, 1.4, (animTick >> 1) & 1);
    }

    // 2. Lounge Animated 4-Frame Aquarium + Office Mascot Cat & Dog
    if (restSpec) {
      const rest = roomRect(restSpec, worldSize);
      drawPixtuoid(ctx, "fish_tank", rest.x + 258, rest.y + 18, 1.45, animTick % 4);
      drawPixtuoid(
        ctx,
        frameEven ? "cat_sit_a" : "cat_sit_b",
        rest.x + Math.round(rest.w * 0.24),
        rest.y + Math.round(rest.h * 0.66),
        1.7,
      );
      drawPixtuoid(
        ctx,
        frameEven ? "dog_sit_a" : "dog_sit_b",
        rest.x + Math.round(rest.w * 0.72),
        rest.y + Math.round(rest.h * 0.68),
        1.65,
      );
    }

    // 3. Library East Vault Animated Printer
    if (libSpec) {
      const lib = roomRect(libSpec, worldSize);
      drawPixtuoid(
        ctx,
        "printer",
        lib.x + Math.round(lib.w * 0.67),
        lib.y + 114,
        1.45,
        (animTick >> 1) & 1,
      );
    }

    // 4. Live Collaboration Arcs between player/orchestrator & working agents
    if (showCollabLinks) {
      drawLiveCollaborationArcs(
        ctx,
        actors,
        playerActorId,
        worldSize,
        animTick,
        animateStage,
        theme,
      );
    }
  }, [
    actors,
    animateStage,
    animTick,
    engineeringAgents,
    engineeringRows,
    playerActorId,
    rooms,
    showCollabLinks,
    theme,
    worldSize,
  ]);

  return (
    <canvas
      ref={canvasRef}
      width={worldSize.width}
      height={worldSize.height}
      aria-hidden="true"
      className="workspace-2d-tilemap-canvas"
      style={{
        position: "absolute",
        inset: 0,
        width: worldSize.width,
        height: worldSize.height,
        display: "block",
        imageRendering: "pixelated",
        pointerEvents: "none",
      }}
    />
  );
}
