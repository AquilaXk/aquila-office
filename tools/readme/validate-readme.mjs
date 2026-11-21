import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const readme = await readFile(resolve(root, "README.md"), "utf8");
const license = await readFile(resolve(root, "LICENSE"), "utf8");
const notice = await readFile(resolve(root, "NOTICE"), "utf8");
const thirdPartyNotices = await readFile(resolve(root, "THIRD_PARTY_NOTICES.md"), "utf8");

const previewImagePath = "front/public/aquila-office-readme-preview.png";
const screenshotImagePaths = [
  "front/public/readme/workspace-list.png",
  "front/public/readme/workspace-dashboard.png",
  "front/public/readme/task-run-chat.png",
  "front/public/readme/artifact-report.png",
  "front/public/readme/integration-settings.png",
];

const previewImage = await readFile(resolve(root, previewImagePath));
const screenshotImages = await Promise.all(
  screenshotImagePaths.map((imagePath) => readFile(resolve(root, imagePath))),
);

const requiredHeadings = [
  "# Aquila Office",
  "## 할 수 있는 일",
  "## 주요 화면",
  "## 다운로드",
  "## 설치 유의사항",
  "### macOS",
  "### Windows",
  "### Linux",
  "## 개발하기",
  "## 문제 신고",
  "## Credits",
];

for (const heading of requiredHeadings) {
  assert.match(readme, new RegExp(`^${escapeRegExp(heading)}$`, "m"), `missing heading: ${heading}`);
}

const requiredSnippets = [
  previewImagePath,
  ...screenshotImagePaths,
  "https://github.com/AquilaXk/aquila-office/releases/latest/download/AquilaOffice-macos-universal.dmg",
  "https://github.com/AquilaXk/aquila-office/releases/latest/download/AquilaOffice-windows-x64-setup.exe",
  "https://github.com/AquilaXk/aquila-office/releases/latest/download/AquilaOffice-linux-x64.deb",
  "https://github.com/AquilaXk/aquila-office/releases/latest/download/AquilaOffice-linux-x64.rpm",
  "https://github.com/AquilaXk/aquila-office/releases/latest/download/AquilaOffice-linux-x64.AppImage",
  "https://github.com/AquilaXk/aquila-office/releases/latest/download/AquilaOffice-linux-arm64.deb",
  "https://github.com/AquilaXk/aquila-office/releases/latest/download/SHA256SUMS.txt",
  "https://github.com/AquilaXk/aquila-office/releases",
  "https://github.com/AquilaXk/aquila-office/issues/new/choose",
  'xattr -dr com.apple.quarantine "/Applications/Aquila Office.app"',
  "Get-FileHash .\\AquilaOffice-windows-x64-setup.exe",
  "sudo apt install ./AquilaOffice-linux-x64.deb",
  "sudo dnf install ./AquilaOffice-linux-x64.rpm",
  "chmod +x AquilaOffice-linux-x64.AppImage",
  "docker compose --env-file .env.example -f compose.dev.yml up --build",
  "./back/gradlew -p back check",
  "npm --prefix front run lint",
  "npm --prefix front run test:node",
  "npm --prefix front run build",
  "npm --prefix front run build:desktop",
  "cargo check --manifest-path front/src-tauri/Cargo.toml",
  "cargo test --manifest-path front/src-tauri/Cargo.toml",
  "npm --prefix front run test:browser",
  "node tools/readme/validate-readme.mjs",
  "[CONTRIBUTING.md](CONTRIBUTING.md)",
  "[SECURITY.md](SECURITY.md)",
  "[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)",
  "[NOTICE](NOTICE)",
  "License: [Apache-2.0](LICENSE)",
];

for (const snippet of requiredSnippets) {
  assert(readme.includes(snippet), `missing README snippet: ${snippet}`);
}

const forbiddenSnippets = [
  "EasyOffice",
  "easyoffice",
  "COMPLETION_CHECKLIST.md",
  "Phaser",
  "aquila-bank",
  "aquila-blog",
  "Co-Authored-By",
  "Generated with",
];

for (const snippet of forbiddenSnippets) {
  assert(!readme.includes(snippet), `forbidden README snippet remains: ${snippet}`);
}

assert.match(license, /Apache License/m, "LICENSE must declare Apache License");
assert.match(license, /Version 2\.0, January 2004/, "LICENSE must declare Version 2.0, January 2004");
assert.match(
  license,
  /http:\/\/www\.apache\.org\/licenses\//,
  "LICENSE must contain the standard Apache license URL",
);
assert(notice.trim().length > 0, "NOTICE must not be empty");
assert(notice.includes("Aquila Office"), "NOTICE must attribute Aquila Office");
assert(thirdPartyNotices.trim().length > 0, "THIRD_PARTY_NOTICES.md must not be empty");
for (const keyword of ["Aquila Office", "PathFinding.js", "kdbush", "Fuse.js", "Apache License"]) {
  assert(
    thirdPartyNotices.includes(keyword),
    `THIRD_PARTY_NOTICES.md must include attribution keyword: ${keyword}`,
  );
}

const PNG_MAGIC = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
assert(previewImage.subarray(0, 8).equals(PNG_MAGIC), `${previewImagePath} must be a PNG asset`);
for (const [index, screenshotImage] of screenshotImages.entries()) {
  assert(
    screenshotImage.subarray(0, 8).equals(PNG_MAGIC),
    `${screenshotImagePaths[index]} must be a PNG asset`,
  );
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
