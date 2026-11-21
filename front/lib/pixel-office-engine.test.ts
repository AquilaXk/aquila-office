import assert from "node:assert/strict";
import test from "node:test";
import {
  CHAR_FRAME_H,
  CHAR_FRAME_W,
  CHARACTER_GEAR_OPTIONS,
  decodePackedSheet,
  extractCharacterFrameRgba,
  type PixelAvatarKind,
  resolveCharacterProfile,
  resolveSheetFrameCoord,
  SHEET_BYTE_LENGTH,
  wardrobeCompositor,
} from "./pixel-office/character-renderer.ts";
import { getHorizontalBezierPath, layoutTopologicalDag } from "./pixel-office/dag-bezier-edges.ts";
import { buildWallMask, carpetJunctionCase, TileCell } from "./pixel-office/office-autotile.ts";
import {
  decodePixtuoidFrameToIndices,
  OFFICE_THEMES,
  type OfficeThemeId,
  PIXTUOID_PALETTE,
  PIXTUOID_SPRITES,
  type PixtuoidSpriteKey,
  resolveOfficeTheme,
} from "./pixel-office/pixtuoid-sprites.ts";
import {
  BOOKSHELF_SPRITE,
  BUBBLE_HEART_SPRITE,
  BUBBLE_PERMISSION_SPRITE,
  BUBBLE_WAITING_SPRITE,
  CHAIR_SPRITE_SWIVEL,
  DESK_SPRITE_ACTIVE_1,
  DESK_SPRITE_ACTIVE_2,
  DESK_SPRITE_IDLE,
  ESPRESSO_BAR_SPRITE,
  OFFICE_CAT_SPRITE_A,
  OFFICE_CAT_SPRITE_B,
  PLANT_SPRITE,
  SERVER_RACK_SPRITE,
  SOFA_SPRITE,
  WHITEBOARD_SPRITE,
} from "./pixel-office/office-pixel-catalog.ts";

test("decodePackedSheet rejects truncated or malformed RLE streams fail-closed", () => {
  assert.throws(() => decodePackedSheet({ p: [], d: "" }), /Invalid packed sheet length/);
  assert.throws(
    () => decodePackedSheet({ p: [0xffffffff], d: Buffer.from([0x00]).toString("base64") }),
    /decoded 1 pixels/,
  );
  assert.throws(
    () => decodePackedSheet({ p: [], d: Buffer.from([0x00]).toString("base64") }),
    /exceeded palette/,
  );
});

test("wardrobe compositor renders distinct full-size 128x96 character sheets for every avatar kind", () => {
  const kinds: PixelAvatarKind[] = ["alex", "mira", "kenji", "yuna", "diego", "iris", "agent"];
  for (const kind of kinds) {
    const profile = resolveCharacterProfile(kind, undefined, kind === "agent" ? "QA Agent" : undefined);
    const sheet = wardrobeCompositor.composeSheet(profile);
    assert.equal(sheet.length, SHEET_BYTE_LENGTH);

    const southIdle = extractCharacterFrameRgba(sheet, "S", "idle", 0);
    const westWalk = extractCharacterFrameRgba(sheet, "W", "walk", 0);
    const eastTyping = extractCharacterFrameRgba(sheet, "E", "typing", 1);
    const northRead = extractCharacterFrameRgba(sheet, "N", "reading", 0);

    assert.equal(southIdle.length, CHAR_FRAME_W * CHAR_FRAME_H * 4);
    assert.equal(westWalk.length, CHAR_FRAME_W * CHAR_FRAME_H * 4);
    assert.equal(eastTyping.length, CHAR_FRAME_W * CHAR_FRAME_H * 4);
    assert.equal(northRead.length, CHAR_FRAME_W * CHAR_FRAME_H * 4);

    let opaqueCount = 0;
    for (let i = 3; i < southIdle.length; i += 4) {
      if (southIdle[i] >= 2) opaqueCount += 1;
    }
    assert.ok(opaqueCount > 80, `${kind} idle frame should contain detailed character pixels`);
  }
});

test("paletteOverride applies custom skin, hair, and shirt colors without breaking transparency", () => {
  const baseline = resolveCharacterProfile("kenji");
  const customized = resolveCharacterProfile("kenji", {
    skin: "#d8b48a",
    hair: "#101820",
    shirt: "#1f7a5f",
  });

  const baseSheet = wardrobeCompositor.composeSheet(baseline);
  const customSheet = wardrobeCompositor.composeSheet(customized);
  let diffPixels = 0;
  for (let i = 0; i < baseSheet.length; i += 4) {
    if (
      baseSheet[i] !== customSheet[i] ||
      baseSheet[i + 1] !== customSheet[i + 1] ||
      baseSheet[i + 2] !== customSheet[i + 2]
    ) {
      diffPixels += 1;
    }
  }
  assert.ok(diffPixels > 120, "custom paletteOverride must visibly recolor sprite layers");
  assert.equal(resolveSheetFrameCoord("W", "walk", 2).flipX, true);
  assert.equal(resolveSheetFrameCoord("E", "typing", 1).col, 4);
  assert.equal(resolveSheetFrameCoord("S", "reading", 1).col, 6);
  assert.equal(resolveSheetFrameCoord("N", "seated", 0).col, 7);
});

test("wall bitmask and carpet marching-squares autotilers produce exact 4-bit cases", () => {
  const grid = [
    [TileCell.WALL, TileCell.WALL, TileCell.FLOOR_OAK],
    [TileCell.WALL, TileCell.FLOOR_OAK, TileCell.FLOOR_OAK],
    [TileCell.FLOOR_OAK, TileCell.FLOOR_OAK, TileCell.FLOOR_OAK],
  ] as const;
  // (0,0) has E(2) + S(4) neighbors -> mask 6
  assert.equal(buildWallMask(0, 0, grid), 6);
  // (1,0) has W(8) neighbor -> mask 8
  assert.equal(buildWallMask(1, 0, grid), 8);

  const rug = { variant: 0, order: 1, mainHex: "#1c2d4a", accentHex: "#60a5fa" };
  const carpets = [rug, null, null, null];
  // Corner (1,1) sees tile (0,0) as NW neighbor -> msCase 1
  assert.equal(carpetJunctionCase(1, 1, "0:#1c2d4a:#60a5fa:1", carpets, 2, 2), 1);
  // Corner (0,0) sees tile (0,0) as SE neighbor -> msCase 4
  assert.equal(carpetJunctionCase(0, 0, "0:#1c2d4a:#60a5fa:1", carpets, 2, 2), 4);
});

test("all authored pixel catalog sprites maintain rectangular row symmetry", () => {
  const sprites = [
    BUBBLE_PERMISSION_SPRITE,
    BUBBLE_WAITING_SPRITE,
    BUBBLE_HEART_SPRITE,
    DESK_SPRITE_ACTIVE_1,
    DESK_SPRITE_ACTIVE_2,
    DESK_SPRITE_IDLE,
    CHAIR_SPRITE_SWIVEL,
    BOOKSHELF_SPRITE,
    WHITEBOARD_SPRITE,
    SOFA_SPRITE,
    ESPRESSO_BAR_SPRITE,
    SERVER_RACK_SPRITE,
    PLANT_SPRITE,
    OFFICE_CAT_SPRITE_A,
    OFFICE_CAT_SPRITE_B,
  ];
  for (const sprite of sprites) {
    assert.ok(sprite.length > 0);
    const width = sprite[0].length;
    assert.ok(width > 0);
    for (const row of sprite) {
      assert.equal(row.length, width, "each sprite row must have consistent pixel width");
    }
  }
});

test("all 59 pixtuoid @4x sprites decode cleanly and resolve all 5 studio atmosphere themes", () => {
  const keys = Object.keys(PIXTUOID_SPRITES) as PixtuoidSpriteKey[];
  assert.equal(keys.length, 59);

  for (const key of keys) {
    const spec = PIXTUOID_SPRITES[key];
    assert.ok(spec.w > 0 && spec.h > 0 && spec.frames.length > 0, `invalid spec for ${key}`);
    for (let f = 0; f < spec.frames.length; f++) {
      const decoded = decodePixtuoidFrameToIndices(key, f);
      assert.equal(decoded.indices.length, spec.w * spec.h, `${key}[${f}] size mismatch`);
      let nonZero = 0;
      for (let i = 0; i < decoded.indices.length; i++) {
        const idx = decoded.indices[i];
        assert.ok(idx <= PIXTUOID_PALETTE.length, `${key}[${f}] out-of-range palette idx ${idx}`);
        if (idx > 0) nonZero += 1;
      }
      assert.ok(nonZero > 0, `${key}[${f}] must contain visible opaque pixels`);
    }
  }

  assert.equal(resolveOfficeTheme("auto", "dark").id, "tokyo-night");
  assert.equal(resolveOfficeTheme("auto", "light").id, "daylight");
  for (const id of Object.keys(OFFICE_THEMES) as Exclude<OfficeThemeId, "auto">[]) {
    const resolved = resolveOfficeTheme(id, "dark");
    assert.equal(resolved.id, id);
    assert.equal(resolved.hallwayTiles.length, 4);
  }
});

test("category-aware agent presets and custom headgear overlays render visibly distinct sprites", () => {
  const categories = ["ORCHESTRATOR", "BACKEND", "FRONTEND", "DESIGN", "SECURITY", "QA"] as const;
  const renderedSheets = categories.map((cat) => {
    const profile = resolveCharacterProfile("agent", undefined, `${cat}-Agent`, cat);
    return wardrobeCompositor.composeSheet(profile);
  });

  // Verify presets across specialties are not identical copies
  let crossDiff = 0;
  for (let i = 0; i < renderedSheets[0].length; i += 4) {
    if (renderedSheets[0][i] !== renderedSheets[1][i]) crossDiff += 1;
  }
  assert.ok(crossDiff > 80, "ORCHESTRATOR and BACKEND agents should look visually distinct");

  const baseProfile = resolveCharacterProfile("alex");
  const baseSheet = wardrobeCompositor.composeSheet(baseProfile);
  for (const gear of ["droid", "headphones", "beret", "visor"] as const) {
    const gearedProfile = resolveCharacterProfile("alex", { gear });
    const gearedSheet = wardrobeCompositor.composeSheet(gearedProfile);
    let gearDiff = 0;
    for (let i = 0; i < baseSheet.length; i += 4) {
      if (
        baseSheet[i] !== gearedSheet[i] ||
        baseSheet[i + 1] !== gearedSheet[i + 1] ||
        baseSheet[i + 2] !== gearedSheet[i + 2] ||
        baseSheet[i + 3] !== gearedSheet[i + 3]
      ) {
        gearDiff += 1;
      }
    }
    assert.ok(gearDiff > 25, `gear '${gear}' must paint distinct headroom pixels across sheet`);
  }
  assert.ok(CHARACTER_GEAR_OPTIONS.some((opt) => opt.id === "droid"));
});

test("layoutTopologicalDag computes multi-level diamond node positions and cubic-bezier SVG paths", () => {
  const layout = layoutTopologicalDag([
    { stepKey: "plan", dependsOn: [], status: "SUCCEEDED" },
    { stepKey: "backend-api", dependsOn: ["plan"], status: "SUCCEEDED" },
    { stepKey: "ui-stage", dependsOn: ["plan"], status: "RUNNING" },
    { stepKey: "verify", dependsOn: ["backend-api", "ui-stage"], status: "PENDING" },
  ]);

  assert.equal(layout.levels.length, 3);
  assert.equal(layout.nodes.length, 4);
  assert.equal(layout.edges.length, 4);

  const byKey = new Map(layout.nodes.map((n) => [n.stepKey, n]));
  assert.equal(byKey.get("plan")?.level, 0);
  assert.equal(byKey.get("backend-api")?.level, 1);
  assert.equal(byKey.get("ui-stage")?.level, 1);
  assert.equal(byKey.get("verify")?.level, 2);
  assert.ok((byKey.get("backend-api")?.x ?? 0) > (byKey.get("plan")?.x ?? 0));
  assert.ok((byKey.get("verify")?.x ?? 0) > (byKey.get("ui-stage")?.x ?? 0));

  for (const edge of layout.edges) {
    assert.match(edge.path, /^M [\d.]+,[\d.]+ C [\d.]+,[\d.]+ [\d.]+,[\d.]+ [\d.]+,[\d.]+$/);
    assert.ok(Number.isFinite(edge.labelX) && Number.isFinite(edge.labelY));
  }

  const backward = getHorizontalBezierPath({
    sourceX: 300,
    sourceY: 100,
    targetX: 120,
    targetY: 160,
  });
  assert.match(backward.path, /^M 300,100 C /);
});
