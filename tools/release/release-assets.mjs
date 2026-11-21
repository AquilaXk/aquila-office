#!/usr/bin/env node
/**
 * Release asset naming, deterministic staging, and SHA-256 checksum generation for Aquila Office.
 * Asset names omit the version number so `releases/latest/download/<name>` URLs in README.md remain valid.
 *
 * Adapted from the reference MIT multi-OS desktop release asset pattern (`scripts/release-assets.mjs`,
 * Copyright (c) 2025-2026 Edward Kim), trimmed for Aquila Office's unsigned desktop bundle targets.
 *
 * Usage:
 *   node tools/release/release-assets.mjs bundles <platform>
 *   node tools/release/release-assets.mjs stage <platform> <out-dir>
 *   node tools/release/release-assets.mjs checksums <dir>
 */

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

export const SUMS_FILE = "SHA256SUMS.txt";
export const DOWNLOAD_BASE_URL = "https://github.com/AquilaXk/aquila-office/releases/latest/download";

const UNIVERSAL_MAC_BUNDLE_ROOT = "front/src-tauri/target/universal-apple-darwin/release/bundle";
const NATIVE_BUNDLE_ROOT = "front/src-tauri/target/release/bundle";

export const PLATFORMS = {
  "macos-universal": {
    tauriBundles: ["app", "dmg"],
    bundleRoot: UNIVERSAL_MAC_BUNDLE_ROOT,
    assets: [
      { dir: "dmg", extension: ".dmg", name: "AquilaOffice-macos-universal.dmg" },
    ],
  },
  "windows-x64": {
    tauriBundles: ["nsis"],
    bundleRoot: NATIVE_BUNDLE_ROOT,
    assets: [
      { dir: "nsis", extension: "-setup.exe", name: "AquilaOffice-windows-x64-setup.exe" },
    ],
  },
  "linux-x64": {
    tauriBundles: ["appimage", "deb", "rpm"],
    bundleRoot: NATIVE_BUNDLE_ROOT,
    assets: [
      { dir: "appimage", extension: ".AppImage", name: "AquilaOffice-linux-x64.AppImage" },
      { dir: "deb", extension: ".deb", name: "AquilaOffice-linux-x64.deb" },
      { dir: "rpm", extension: ".rpm", name: "AquilaOffice-linux-x64.rpm" },
    ],
  },
  "linux-arm64": {
    tauriBundles: ["deb"],
    bundleRoot: NATIVE_BUNDLE_ROOT,
    assets: [
      { dir: "deb", extension: ".deb", name: "AquilaOffice-linux-arm64.deb" },
    ],
  },
};

function compareNames(a, b) {
  if (a < b) return -1;
  return a > b ? 1 : 0;
}

function platformOf(id) {
  const platform = PLATFORMS[id];
  if (!platform) {
    throw new Error(`unknown platform '${id}' (expected one of ${Object.keys(PLATFORMS).join(", ")})`);
  }
  return platform;
}

export function bundleArguments(id) {
  return platformOf(id).tauriBundles.join(",");
}

export function allAssetNames() {
  return Object.values(PLATFORMS)
    .flatMap((platform) => platform.assets.map((asset) => asset.name))
    .sort(compareNames);
}

function findBundle(root, platform, asset) {
  const dir = path.join(root, platform.bundleRoot, asset.dir);
  const found = fs.existsSync(dir)
    ? fs
        .readdirSync(dir, { withFileTypes: true })
        .filter((entry) => entry.isFile() && entry.name.endsWith(asset.extension))
        .map((entry) => entry.name)
    : [];

  if (found.length !== 1) {
    throw new Error(
      `expected exactly one ${asset.extension} in ${dir}, found ${found.length}${found.length ? ` (${found.join(", ")})` : ""}`,
    );
  }

  return path.join(dir, found[0]);
}

export function locateBundles(id, root) {
  const platform = platformOf(id);
  return platform.assets.map((asset) => ({
    asset,
    file: findBundle(root, platform, asset),
  }));
}

export function stageAssets(id, root, outDir) {
  const bundles = locateBundles(id, root);
  fs.mkdirSync(outDir, { recursive: true });
  const staged = [];
  for (const { asset, file } of bundles) {
    const targetPath = path.join(outDir, asset.name);
    fs.copyFileSync(file, targetPath);
    staged.push(asset.name);
  }
  return staged;
}

export function writeChecksums(dir) {
  const expected = allAssetNames();
  const present = fs.existsSync(dir)
    ? fs
        .readdirSync(dir, { withFileTypes: true })
        .filter((entry) => entry.isFile() && entry.name !== SUMS_FILE)
        .map((entry) => entry.name)
        .sort(compareNames)
    : [];

  const missing = expected.filter((name) => !present.includes(name));
  const unexpected = present.filter((name) => !expected.includes(name));

  if (missing.length > 0 || unexpected.length > 0) {
    const parts = [];
    if (missing.length > 0) parts.push(`missing: ${missing.join(", ")}`);
    if (unexpected.length > 0) parts.push(`unexpected: ${unexpected.join(", ")}`);
    throw new Error(`release assets in ${dir} are not the expected set (${parts.join("; ")})`);
  }

  const lines = expected.map((name) => {
    const digest = crypto
      .createHash("sha256")
      .update(fs.readFileSync(path.join(dir, name)))
      .digest("hex");
    return `${digest}  ${name}`;
  });

  fs.writeFileSync(path.join(dir, SUMS_FILE), `${lines.join("\n")}\n`, "utf8");
  return lines;
}

function main(argv) {
  const [command, first, second] = argv;
  if (command === "bundles" && first) {
    console.log(bundleArguments(first));
    return;
  }
  if (command === "stage" && first && second) {
    for (const name of stageAssets(first, process.cwd(), second)) {
      console.log(`staged ${name}`);
    }
    return;
  }
  if (command === "checksums" && first) {
    for (const line of writeChecksums(first)) {
      console.log(line);
    }
    return;
  }
  console.error("usage: release-assets.mjs bundles <platform> | stage <platform> <out-dir> | checksums <dir>");
  process.exit(2);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    main(process.argv.slice(2));
  } catch (err) {
    console.error(`::error::${err.message}`);
    process.exit(1);
  }
}
