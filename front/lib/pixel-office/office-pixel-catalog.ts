/**
 * Pixel-art furniture, floor, wall, carpet, speech-bubble, and office mascot sprite catalog.
 *
 * Notice: Speech bubble matrices (BUBBLE_PERMISSION_SPRITE, BUBBLE_WAITING_SPRITE, BUBBLE_HEART_SPRITE)
 * are adapted from pixel-agents-hq/pixel-agents (MIT License, Copyright (c) 2026 Pablo De Lucca).
 * All floor tiles, 3D walls, carpets, workstation desks, dual monitors, bookshelves, sofas,
 * espresso bar, server rack, plants, and mascot sprites are original Apache-2.0 sprites authored
 * for Aquila Office (no third-party redistribution-restricted tilesets included).
 */

import type { SpriteData } from "./colorize.ts";

// ── Speech Bubbles (MIT, pixel-agents) ───────────────────────────────────────

function resolveGridSprite(palette: Record<string, string>, rows: readonly string[]): SpriteData {
  return rows.map((row) => [...row].map((ch) => palette[ch] ?? ""));
}

export const BUBBLE_PERMISSION_SPRITE: SpriteData = resolveGridSprite(
  { _: "", B: "#555566", F: "#EEEEFF", A: "#CCA700" },
  [
    "BBBBBBBBBBB",
    "BFFFFFFFFFB",
    "BFFFFFFFFFB",
    "BFFFFFFFFFB",
    "BFFFFFFFFFB",
    "BFFAFAFAFFB",
    "BFFFFFFFFFB",
    "BFFFFFFFFFB",
    "BFFFFFFFFFB",
    "BBBBBBBBBBB",
    "____BBB____",
    "_____B_____",
  ],
);

export const BUBBLE_WAITING_SPRITE: SpriteData = resolveGridSprite(
  { _: "", B: "#555566", F: "#EEEEFF", G: "#44BB66" },
  [
    "_BBBBBBBBB_",
    "BFFFFFFFFFB",
    "BFFFFFFFFFB",
    "BFFFFFFFGFB",
    "BFFFFFFGFFB",
    "BFFGFFGFFFB",
    "BFFFGGFFFFB",
    "BFFFFFFFFFB",
    "BFFFFFFFFFB",
    "_BBBBBBBBB_",
    "____BBB____",
    "_____B_____",
  ],
);

export const BUBBLE_HEART_SPRITE: SpriteData = resolveGridSprite(
  { _: "", B: "#555566", F: "#FFEEF2", R: "#E14D6A" },
  [
    "_BBBBBBBBB_",
    "BFFFFFFFFFB",
    "BFRRFFFRRFB",
    "BRRRRFRRRRB",
    "BRRRRRRRRRB",
    "BFRRRRRRRFB",
    "BFFRRRRRFFB",
    "BFFFRRRFFFB",
    "BFFFFRFFFFB",
    "_BBBBBBBBB_",
    "____BBB____",
    "_____B_____",
  ],
);

// ── Original Aquila Office Pixel Art Sprites (Apache-2.0) ────────────────────

export interface OfficePaletteTheme {
  isLight: boolean;
  voidBg: string;
  wallTop: string;
  wallFace: string;
  wallTrim: string;
  wallBase: string;
  wallWindowFrame: string;
  wallWindowGlass: string;
  oak: [string, string, string, string];
  slate: [string, string, string, string];
  sand: [string, string, string, string];
  walnut: [string, string, string, string];
  corridor: [string, string, string, string];
  rugEngineering: { main: string; border: string; accent: string };
  rugHuddle: { main: string; border: string; accent: string };
  rugRest: { main: string; border: string; accent: string };
  rugLibrary: { main: string; border: string; accent: string };
}

export function getOfficeThemePalette(isLight: boolean): OfficePaletteTheme {
  if (isLight) {
    return {
      isLight: true,
      voidBg: "#dde4ef",
      wallTop: "#64748b",
      wallFace: "#e2e8f0",
      wallTrim: "#cbd5e1",
      wallBase: "#475569",
      wallWindowFrame: "#94a3b8",
      wallWindowGlass: "#bae6fd",
      oak: ["#d6ad7d", "#c79b68", "#b98a56", "#a87a46"],
      slate: ["#dbe2ec", "#cfd7e3", "#c3ccd9", "#b7c1d0"],
      sand: ["#e8dcc8", "#ddd0b8", "#d2c3aa", "#c5b59b"],
      walnut: ["#8f6848", "#7d583b", "#6f4c32", "#61412a"],
      corridor: ["#e8edf4", "#dce3ec", "#cfd8e3", "#cbd5e1"],
      rugEngineering: { main: "#2c4c7e", border: "#4f7ec9", accent: "#93c5fd" },
      rugHuddle: { main: "#1f5a5c", border: "#3b8a8d", accent: "#99f6e4" },
      rugRest: { main: "#7c4a3a", border: "#b5745f", accent: "#fed7aa" },
      rugLibrary: { main: "#4c3568", border: "#7c5ca8", accent: "#ddd6fe" },
    };
  }
  return {
    isLight: false,
    voidBg: "#0d111a",
    wallTop: "#364156",
    wallFace: "#222938",
    wallTrim: "#2d3648",
    wallBase: "#141923",
    wallWindowFrame: "#4b5975",
    wallWindowGlass: "#1e3a5f",
    oak: ["#523b28", "#483322", "#3e2c1d", "#342417"],
    slate: ["#212836", "#1d2330", "#191e2a", "#161b25"],
    sand: ["#35322e", "#2e2b27", "#282521", "#221f1c"],
    walnut: ["#3c2a1e", "#332319", "#2a1d14", "#231710"],
    corridor: ["#262d3d", "#212736", "#1d2330", "#181d28"],
    rugEngineering: { main: "#1c2d4a", border: "#2c4a7a", accent: "#60a5fa" },
    rugHuddle: { main: "#163338", border: "#245760", accent: "#2dd4bf" },
    rugRest: { main: "#3d2624", border: "#663f3b", accent: "#fb923c" },
    rugLibrary: { main: "#291e3e", border: "#48346e", accent: "#a78bfa" },
  };
}

export const DESK_SPRITE_ACTIVE_1: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#181a22", // dark outline
    W: "#9c7048", // warm oak top
    w: "#b8895e", // oak highlight
    d: "#7a5434", // oak edge shadow
    l: "#593c23", // desk legs/underframe
    M: "#282e3b", // monitor bezel
    m: "#121722", // monitor screen bg
    C: "#38bdf8", // cyan code
    G: "#4ade80", // green code
    Y: "#fbbf24", // amber code
    P: "#c084fc", // violet code
    K: "#cbd5e1", // keyboard base
    k: "#64748b", // keycaps
    U: "#e2e8f0", // mug
    u: "#92400e", // coffee
  },
  [
    "________________________________",
    "__OOOOOOOOOO______OOOOOOOOOO____",
    "__OMMMMMMMMO______OMMMMMMMMO____",
    "__OMmmCCCmMO______OMmGGGmmMO____",
    "__OMmYYmmGMO______OMmmCCCmmO____",
    "__OMmmPPmmMO______OMmYYmmPmO____",
    "__OMMMMMMMMO______OMMMMMMMMO____",
    "__OOOOOOOMO________OMOOOOOOO____",
    "_OOOOOOOOOOmmmmmmmmOOOOOOOOOO___",
    "_OwwwwwwwwwwwwwwwwwwwwwwwwwwO___",
    "_OWWWWWWWWWWWWWWWWWWWWWWWWWWdO__",
    "_OWWWWWKKKKKKKKKKKKWUUUOWWWWdO__",
    "_OWWWWWKkkkkkkkkkkKWUuuOWWWWdO__",
    "_OWWWWWWWWWWWWWWWWWWWWWWWWWWdO__",
    "_OddddddddddddddddddddddddddO___",
    "__OllO__________________OllO____",
    "__OllO__________________OllO____",
    "__OOOO__________________OOOO____",
  ],
);

export const DESK_SPRITE_ACTIVE_2: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#181a22",
    W: "#9c7048",
    w: "#b8895e",
    d: "#7a5434",
    l: "#593c23",
    M: "#282e3b",
    m: "#121722",
    C: "#60a5fa",
    G: "#34d399",
    Y: "#f472b6",
    P: "#facc15",
    K: "#cbd5e1",
    k: "#64748b",
    U: "#e2e8f0",
    u: "#92400e",
  },
  [
    "________________________________",
    "__OOOOOOOOOO______OOOOOOOOOO____",
    "__OMMMMMMMMO______OMMMMMMMMO____",
    "__OMmPPPPmmO______OMmCCCmmmO____",
    "__OMmmGGGmmO______OMmmYYYYmO____",
    "__OMmCCCmmMO______OMmGGGmmmO____",
    "__OMMMMMMMMO______OMMMMMMMMO____",
    "__OOOOOOOMO________OMOOOOOOO____",
    "_OOOOOOOOOOmmmmmmmmOOOOOOOOOO___",
    "_OwwwwwwwwwwwwwwwwwwwwwwwwwwO___",
    "_OWWWWWWWWWWWWWWWWWWWWWWWWWWdO__",
    "_OWWWWWKKKKKKKKKKKKWUUUOWWWWdO__",
    "_OWWWWWKkkkkkkkkkkKWUuuOWWWWdO__",
    "_OWWWWWWWWWWWWWWWWWWWWWWWWWWdO__",
    "_OddddddddddddddddddddddddddO___",
    "__OllO__________________OllO____",
    "__OllO__________________OllO____",
    "__OOOO__________________OOOO____",
  ],
);

export const DESK_SPRITE_IDLE: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#181a22",
    W: "#9c7048",
    w: "#b8895e",
    d: "#7a5434",
    l: "#593c23",
    M: "#282e3b",
    m: "#1f2937",
    D: "#334155",
    K: "#94a3b8",
    k: "#475569",
    U: "#e2e8f0",
    u: "#78350f",
  },
  [
    "________________________________",
    "__OOOOOOOOOO______OOOOOOOOOO____",
    "__OMMMMMMMMO______OMMMMMMMMO____",
    "__OMmDDDDmmO______OMmmDDDDmO____",
    "__OMmDmmmmmO______OMmmmmmmDO____",
    "__OMmmmmmmmO______OMmmmmmmmO____",
    "__OMMMMMMMMO______OMMMMMMMMO____",
    "__OOOOOOOMO________OMOOOOOOO____",
    "_OOOOOOOOOOmmmmmmmmOOOOOOOOOO___",
    "_OwwwwwwwwwwwwwwwwwwwwwwwwwwO___",
    "_OWWWWWWWWWWWWWWWWWWWWWWWWWWdO__",
    "_OWWWWWKKKKKKKKKKKKWUUUOWWWWdO__",
    "_OWWWWWKkkkkkkkkkkKWUuuOWWWWdO__",
    "_OWWWWWWWWWWWWWWWWWWWWWWWWWWdO__",
    "_OddddddddddddddddddddddddddO___",
    "__OllO__________________OllO____",
    "__OllO__________________OllO____",
    "__OOOO__________________OOOO____",
  ],
);

export const CHAIR_SPRITE_SWIVEL: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#141720",
    B: "#334155",
    b: "#475569",
    A: "#3b82f6",
    M: "#64748b",
  },
  [
    "____OOOOOO____",
    "___ObbbbbbO___",
    "___ObAAAAbO___",
    "___OBBBBBBO___",
    "__OObbbbbbOO__",
    "__OMBBBBBBMO__",
    "___OOOOOOOO___",
    "_____OMMO_____",
    "___OOOMMOOO___",
    "__O_O____O_O__",
  ],
);

export const BOOKSHELF_SPRITE: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#1a1410",
    W: "#7c5333",
    w: "#9e6e46",
    d: "#543720",
    R: "#ef4444",
    B: "#3b82f6",
    G: "#22c55e",
    Y: "#eab308",
    P: "#a855f7",
    T: "#14b8a6",
    C: "#e2e8f0",
  },
  [
    "_OOOOOOOOOOOOOOOOOOOOOOOOOOOOOO_",
    "_OwwwwwwwwwwwwwwwwwwwwwwwwwwwwO_",
    "_OWddRRRRBBGGdYYYYPPPTTTdCCCdWO_",
    "_OWddRRRRBBGGdYYYYPPPTTTdCCCdWO_",
    "_OWddddddddddddddddddddddddddWO_",
    "_OwwwwwwwwwwwwwwwwwwwwwwwwwwwwO_",
    "_OWddTTdPPPYYdBBBBGGRRRdCCCCdWO_",
    "_OWddTTdPPPYYdBBBBGGRRRdCCCCdWO_",
    "_OWddddddddddddddddddddddddddWO_",
    "_OwwwwwwwwwwwwwwwwwwwwwwwwwwwwO_",
    "_OWddCCdGGGGBdRRRRTTTYYdPPPPdWO_",
    "_OWddddddddddddddddddddddddddWO_",
    "_OWWWWWWWWWWWWWWWWWWWWWWWWWWWWO_",
    "__OOO______________________OOO__",
  ],
);

export const WHITEBOARD_SPRITE: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#334155",
    F: "#cbd5e1",
    W: "#f8fafc",
    B: "#2563eb",
    G: "#16a34a",
    A: "#f59e0b",
    R: "#ef4444",
  },
  [
    "_OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO_",
    "_OFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFO_",
    "_OFWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWFO_",
    "_OFWWBBBBBBWWWWWWWWWWGGGGGGWWWWWWAAAFFO_",
    "_OFWWBWWWWBWWWWBBBBWWGWWWWGWWWRRWWWAFFO_",
    "_OFWWBBBBBBWWWWWWWWWWGGGGGGWWWWWWAAAFFO_",
    "_OFWWWWWWWWBBBBWWWWWWWWWWWWGGGWWWWWWFFO_",
    "_OFWWBBBBWWWWWWWWGGGGWWWWWWWWBBBBWWWFFO_",
    "_OFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFO_",
    "_OOOOOOOOOOOOOOOOOOOBBRRGGAAAAAAAAOOOOO_",
  ],
);

export const SOFA_SPRITE: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#181a24",
    S: "#4f46e5",
    s: "#6366f1",
    d: "#3730a3",
    C: "#a5b4fc",
    W: "#7c5333",
  },
  [
    "__OOOOOOOOOOOOOOOOOOOOOOOOOOOO__",
    "_OssssssssssssssssssssssssssssO_",
    "_OsSSSSSSSSSSSSSSSSSSSSSSSSSSsO_",
    "OOdsCCCCCCsSSSSSSSSSSsCCCCCCsdOO",
    "OdsSSSSSSSSSSSSSSSSSSSSSSSSSSsdO",
    "OddSSSSSSSSSSSSSSSSSSSSSSSSSSddO",
    "OOddddddddddddddddddddddddddddOO",
    "_OOWW______________________WWOO_",
  ],
);

export const ESPRESSO_BAR_SPRITE: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#161820",
    C: "#64748b",
    c: "#94a3b8",
    B: "#78350f",
    b: "#b45309",
    W: "#f8fafc",
    G: "#22c55e",
    S: "#cbd5e1",
  },
  [
    "_______SS______SS_______",
    "________S_______S_______",
    "__OOOOOOOOOOOOOOOOOOOO__",
    "_OccccccccccccccccccccO_",
    "_OcOOCCCCCCCCCCCCCCOOcO_",
    "_OcOCGGGGGGGGGGGGGGCOcO_",
    "_OcOCCCCCCCCCCCCCCCCOcO_",
    "_ObbbbbbbbbbbbbbbbbbbbO_",
    "_OBBBBWWWWWWWWWWWWBBBBO_",
    "_OBBBBWWWWWWWWWWWWBBBBO_",
    "_OOOOOOOOOOOOOOOOOOOOOO_",
  ],
);

export const SERVER_RACK_SPRITE: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#0f172a",
    M: "#334155",
    D: "#1e293b",
    G: "#22c55e",
    B: "#38bdf8",
    A: "#f59e0b",
  },
  [
    "_OOOOOOOOOOOO_",
    "_OMMMMMMMMMMO_",
    "_OMDDDDGGBDMO_",
    "_OMDDDDDDDDMO_",
    "_OMDDDDGGADMO_",
    "_OMDDDDDDDDMO_",
    "_OMDDDDBGDDMO_",
    "_OMDDDDDDDDMO_",
    "_OMDDDDGGDDMO_",
    "_OMMMMMMMMMMO_",
    "_OOOOOOOOOOOO_",
  ],
);

export const PLANT_SPRITE: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#14261c",
    G: "#22c55e",
    g: "#15803d",
    h: "#4ade80",
    P: "#b45309",
    p: "#78350f",
  },
  [
    "____OO____OO____",
    "___OhGO__OGhO___",
    "__OhGGGOOGGGhO__",
    "_OGGhGGGGGGhgGO_",
    "__OGgGGGGGGggO__",
    "___OOgGGGGgOO___",
    "____OPPPPPPO____",
    "____OppppppO____",
    "_____OOOOOO_____",
  ],
);

export const OFFICE_CAT_SPRITE_A: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#18141c",
    F: "#f59e0b",
    f: "#fcd34d",
    E: "#1e293b",
    P: "#fb7185",
  },
  [
    "_OO______OO_",
    "OFOO____OOFO",
    "OFfOOOOOOFfO",
    "OFFFFFFFFFFO",
    "OFEFFFFFFEFO",
    "OFFFPPPFFFFO",
    "_OFFFFFFFFO_",
    "__OFfFFfFO__",
    "__OO____OO__",
  ],
);

export const OFFICE_CAT_SPRITE_B: SpriteData = resolveGridSprite(
  {
    _: "",
    O: "#18141c",
    F: "#f59e0b",
    f: "#fcd34d",
    E: "#1e293b",
    P: "#fb7185",
  },
  [
    "_OO______OO___",
    "OFOO____OOFO__",
    "OFfOOOOOOFfO__",
    "OFFFFFFFFFFO__",
    "OFEFFFFFFEFO__",
    "OFFFPPPFFFFO_O",
    "_OFFFFFFFFOOO_",
    "__OFfFFfFO____",
    "__O______O____",
  ],
);
