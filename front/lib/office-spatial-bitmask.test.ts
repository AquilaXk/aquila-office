import assert from "node:assert/strict";
import test from "node:test";
import {
  ACTOR_FLAG,
  clearSpatialBit,
  countOccupiedBits,
  createSpatialBitset,
  fastPerceivedLuma,
  hasSpatialBit,
  isPctPointOccupiedInBitset,
  matchesActorBitmask,
  packActorBitfield,
  packGridCoord,
  packRgb24,
  popcount32,
  rasterizeRectToSpatialBitset,
  resetSpatialBitset,
  setSpatialBit,
  unpackActorFlags,
  unpackActorRoleId,
  unpackActorZoneId,
  unpackGridX,
  unpackGridY,
  unpackRgb24,
} from "./office-spatial-bitmask.ts";

test("16-bit grid coordinate packing round-trips 8-bit x and y coordinates", () => {
  const packedOrigin = packGridCoord(0, 0);
  assert.equal(packedOrigin, 0x0000);
  assert.equal(unpackGridX(packedOrigin), 0);
  assert.equal(unpackGridY(packedOrigin), 0);

  const packedSample = packGridCoord(42, 137);
  assert.equal(packedSample, (137 << 8) | 42);
  assert.equal(unpackGridX(packedSample), 42);
  assert.equal(unpackGridY(packedSample), 137);

  const packedMax = packGridCoord(255, 255);
  assert.equal(packedMax, 0xffff);
  assert.equal(unpackGridX(packedMax), 255);
  assert.equal(unpackGridY(packedMax), 255);

  // Verify masking truncates overflow above 8 bits cleanly
  const packedOverflow = packGridCoord(256 + 19, 512 + 73);
  assert.equal(unpackGridX(packedOverflow), 19);
  assert.equal(unpackGridY(packedOverflow), 73);
});

test("Uint32Array 2D spatial bitset sets, clears, queries, and counts bits via SWAR popcount32", () => {
  const width = 40;
  const height = 24;
  const bitset = createSpatialBitset(width, height);
  // 40 * 24 = 960 bits -> 960 / 32 = 30 Uint32 words
  assert.equal(bitset.length, 30);
  assert.equal(countOccupiedBits(bitset), 0);

  // Also check non-multiple-of-32 allocation rounding
  const oddBitset = createSpatialBitset(10, 10);
  assert.equal(oddBitset.length, 4); // (100 + 31) >>> 5 = 4

  setSpatialBit(bitset, width, 0, 0);
  setSpatialBit(bitset, width, 31, 0); // bit 31 (high bit of word 0)
  setSpatialBit(bitset, width, 32, 0); // bit 0 of word 1
  setSpatialBit(bitset, width, 39, 23); // last cell (idx = 959)

  assert.equal(hasSpatialBit(bitset, width, 0, 0), true);
  assert.equal(hasSpatialBit(bitset, width, 31, 0), true);
  assert.equal(hasSpatialBit(bitset, width, 32, 0), true);
  assert.equal(hasSpatialBit(bitset, width, 39, 23), true);
  assert.equal(hasSpatialBit(bitset, width, 15, 10), false);
  assert.equal(countOccupiedBits(bitset), 4);

  // Idempotent set
  setSpatialBit(bitset, width, 31, 0);
  assert.equal(countOccupiedBits(bitset), 4);

  // Clear bit 31 and a non-zero row bit (39, 23)
  clearSpatialBit(bitset, width, 31, 0);
  assert.equal(hasSpatialBit(bitset, width, 31, 0), false);
  assert.equal(countOccupiedBits(bitset), 3);
  clearSpatialBit(bitset, width, 39, 23);
  assert.equal(hasSpatialBit(bitset, width, 39, 23), false);
  assert.equal(countOccupiedBits(bitset), 2);

  // Rect rasterization, O(1) percentage point lookup, and zero-alloc reset
  resetSpatialBitset(bitset);
  assert.equal(countOccupiedBits(bitset), 0);

  rasterizeRectToSpatialBitset(bitset, 10, 20, 25, 30);
  assert.equal(isPctPointOccupiedInBitset(bitset, 20, 35), true);
  assert.equal(isPctPointOccupiedInBitset(bitset, 5, 5), false);
  assert.equal(isPctPointOccupiedInBitset(bitset, 80, 80), false);
  assert.ok(countOccupiedBits(bitset) > 0);

  resetSpatialBitset(bitset);
  assert.equal(countOccupiedBits(bitset), 0);
  assert.equal(isPctPointOccupiedInBitset(bitset, 20, 35), false);

  // Direct SWAR popcount32 boundary checks
  assert.equal(popcount32(0), 0);
  assert.equal(popcount32(0xffffffff), 32);
  assert.equal(popcount32(0x80000001), 2);
  assert.equal(popcount32(0x55555555), 16);
});

test("32-bit actor state bitfield packs flags, zoneId, and roleId and evaluates bitmask predicates", () => {
  assert.equal(ACTOR_FLAG.ONLINE, 1);
  assert.equal(ACTOR_FLAG.AGENT, 2);
  assert.equal(ACTOR_FLAG.SELECTED, 4);
  assert.equal(ACTOR_FLAG.FOCUSED, 8);
  assert.equal(ACTOR_FLAG.ACTIVE_TASK, 16);
  assert.equal(ACTOR_FLAG.MOVING, 32);
  assert.equal(ACTOR_FLAG.SELF, 64);

  const flags =
    ACTOR_FLAG.ONLINE | ACTOR_FLAG.AGENT | ACTOR_FLAG.ACTIVE_TASK | ACTOR_FLAG.FOCUSED;
  const bitfield = packActorBitfield(flags, 5, 11);

  assert.equal(unpackActorFlags(bitfield), flags);
  assert.equal(unpackActorZoneId(bitfield), 5);
  assert.equal(unpackActorRoleId(bitfield), 11);

  assert.equal(
    matchesActorBitmask(bitfield, ACTOR_FLAG.ONLINE | ACTOR_FLAG.AGENT),
    true,
  );
  assert.equal(
    matchesActorBitmask(
      bitfield,
      ACTOR_FLAG.ONLINE | ACTOR_FLAG.AGENT,
      ACTOR_FLAG.SELF | ACTOR_FLAG.MOVING,
    ),
    true,
  );
  assert.equal(
    matchesActorBitmask(bitfield, ACTOR_FLAG.ONLINE | ACTOR_FLAG.SELECTED),
    false,
  );
  assert.equal(
    matchesActorBitmask(bitfield, ACTOR_FLAG.ONLINE, ACTOR_FLAG.ACTIVE_TASK),
    false,
  );
});

test("24-bit RGB integer packing and integer Rec.709 fastPerceivedLuma operate without float drift", () => {
  const slateBg = packRgb24(0x12, 0x13, 0x17);
  assert.equal(slateBg, 0x121317);
  assert.deepEqual(unpackRgb24(slateBg), [0x12, 0x13, 0x17]);

  const accentBlue = packRgb24(0x32, 0x79, 0xf9);
  assert.equal(accentBlue, 0x3279f9);
  assert.deepEqual(unpackRgb24(accentBlue), [0x32, 0x79, 0xf9]);

  assert.equal(fastPerceivedLuma(packRgb24(0, 0, 0)), 0);
  assert.equal(fastPerceivedLuma(packRgb24(255, 255, 255)), 255);
  // (0x32 * 54 + 0x79 * 183 + 0xf9 * 19) >>> 8 = (50*54 + 121*183 + 249*19) >>> 8 = 29574 >>> 8 = 115
  assert.equal(fastPerceivedLuma(accentBlue), 115);
  assert.ok(fastPerceivedLuma(packRgb24(0xf8, 0xf9, 0xfc)) > fastPerceivedLuma(slateBg));
});

test("workspace 2D stage and arcade wrappers integrate bitmask spatial engine and editorial dark surfaces", async () => {
  const { readFileSync } = await import("node:fs");
  const path = await import("node:path");
  const appRoot = process.cwd();

  const canvasSource = readFileSync(
    path.join(appRoot, "components/workspace/Workspace2DCanvas.tsx"),
    "utf8",
  );
  const screenSource = readFileSync(
    path.join(appRoot, "components/arcade/Screen.tsx"),
    "utf8",
  );
  const panelSource = readFileSync(
    path.join(appRoot, "components/arcade/Panel.tsx"),
    "utf8",
  );
  const avatarSource = readFileSync(
    path.join(appRoot, "components/arcade/PixelAvatar.tsx"),
    "utf8",
  );

  assert.match(canvasSource, /packActorBitfield/);
  assert.match(canvasSource, /matchesActorBitmask/);
  assert.match(canvasSource, /createSpatialBitset/);
  assert.match(canvasSource, /resetSpatialBitset/);
  assert.match(canvasSource, /setSpatialBit/);
  assert.match(canvasSource, /countOccupiedBits/);
  assert.doesNotMatch(canvasSource, /WRITE CHANGELOG/);
  assert.doesNotMatch(canvasSource, /No signal/);

  assert.match(avatarSource, /FRAME_SHADOW_CACHE/);

  assert.doesNotMatch(screenSource, /setInterval/);
  assert.doesNotMatch(screenSource, /BUILD 04\.19/);
  assert.match(screenSource, /Aquila Office/);
  assert.match(panelSource, /borderRadius:\s*6/);
  assert.match(panelSource, /var\(--ui-surface/);
});
