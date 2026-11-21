import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
  SUMS_FILE,
  allAssetNames,
  bundleArguments,
  locateBundles,
  stageAssets,
  writeChecksums,
} from "./release-assets.mjs";

test("각 플랫폼별 Tauri 번들 타깃과 고정 릴리스 에셋 목록을 정확히 반환한다", () => {
  assert.equal(bundleArguments("macos-universal"), "app,dmg");
  assert.equal(bundleArguments("windows-x64"), "nsis");
  assert.equal(bundleArguments("linux-x64"), "appimage,deb,rpm");
  assert.equal(bundleArguments("linux-arm64"), "deb");
  assert.throws(() => bundleArguments("unknown-os"), /unknown platform/);

  assert.deepEqual(allAssetNames(), [
    "AquilaOffice-linux-arm64.deb",
    "AquilaOffice-linux-x64.AppImage",
    "AquilaOffice-linux-x64.deb",
    "AquilaOffice-linux-x64.rpm",
    "AquilaOffice-macos-universal.dmg",
    "AquilaOffice-windows-x64-setup.exe",
  ]);
});

test("모든 플랫폼 에셋 스테이징과 SHA256SUMS.txt 생성은 누락·중복 없이 정확히 동작한다", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "aquila-release-assets-"));
  try {
    const outDir = path.join(tempRoot, "release-assets");

    const fixtures = [
      "front/src-tauri/target/universal-apple-darwin/release/bundle/dmg/Aquila Office_0.1.0_universal.dmg",
      "front/src-tauri/target/release/bundle/nsis/Aquila Office_0.1.0_x64-setup.exe",
      "front/src-tauri/target/release/bundle/appimage/Aquila Office_0.1.0_amd64.AppImage",
      "front/src-tauri/target/release/bundle/deb/Aquila Office_0.1.0_amd64.deb",
      "front/src-tauri/target/release/bundle/rpm/Aquila Office-0.1.0-1.x86_64.rpm",
    ];
    for (const rel of fixtures) {
      const abs = path.join(tempRoot, rel);
      fs.mkdirSync(path.dirname(abs), { recursive: true });
      fs.writeFileSync(abs, `fixture:${rel}\n`, "utf8");
    }

    assert.deepEqual(stageAssets("macos-universal", tempRoot, outDir), ["AquilaOffice-macos-universal.dmg"]);
    assert.deepEqual(stageAssets("windows-x64", tempRoot, outDir), ["AquilaOffice-windows-x64-setup.exe"]);
    assert.deepEqual(stageAssets("linux-x64", tempRoot, outDir), [
      "AquilaOffice-linux-x64.AppImage",
      "AquilaOffice-linux-x64.deb",
      "AquilaOffice-linux-x64.rpm",
    ]);

    // linux-arm64 누락 상태에서는 checksums 생성을 Fail-Closed로 거부한다.
    assert.throws(() => writeChecksums(outDir), /missing: AquilaOffice-linux-arm64\.deb/);

    // arm64 .deb를 별도 스테이징 루트에서 채운다.
    const armRoot = path.join(tempRoot, "arm");
    const armDeb = path.join(armRoot, "front/src-tauri/target/release/bundle/deb/Aquila Office_0.1.0_arm64.deb");
    fs.mkdirSync(path.dirname(armDeb), { recursive: true });
    fs.writeFileSync(armDeb, "fixture:arm64-deb\n", "utf8");
    assert.deepEqual(stageAssets("linux-arm64", armRoot, outDir), ["AquilaOffice-linux-arm64.deb"]);

    const lines = writeChecksums(outDir);
    assert.equal(lines.length, 6);
    assert.match(lines[0], /^[0-9a-f]{64}  AquilaOffice-linux-arm64\.deb$/);
    assert.equal(fs.existsSync(path.join(outDir, SUMS_FILE)), true);

    // 예상 외 파일이 섞이면 Fail-Closed로 거부한다.
    fs.writeFileSync(path.join(outDir, "unexpected.txt"), "extra", "utf8");
    assert.throws(() => writeChecksums(outDir), /unexpected: unexpected\.txt/);
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
});

test("동일 디렉터리에 번들이 없거나 여러 개면 Fail-Closed로 실패한다", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "aquila-release-ambiguous-"));
  try {
    assert.throws(() => locateBundles("windows-x64", tempRoot), /found 0/);

    const nsisDir = path.join(tempRoot, "front/src-tauri/target/release/bundle/nsis");
    fs.mkdirSync(nsisDir, { recursive: true });
    fs.writeFileSync(path.join(nsisDir, "a-setup.exe"), "a", "utf8");
    fs.writeFileSync(path.join(nsisDir, "b-setup.exe"), "b", "utf8");
    assert.throws(() => locateBundles("windows-x64", tempRoot), /found 2/);
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
});
