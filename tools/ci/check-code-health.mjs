import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export const DEFAULT_MAX_SOURCE_LINES = 1200;

/*
 * 기존 비대 파일이 더 커지지 않도록 상한 래칫을 걸어 새 기능의 모듈 분리를 강제한다.
 */
export const LEGACY_FILE_LINE_CEILINGS = Object.freeze({
  "front/app/globals.css": 4450,
  "front/app/workspaces/[workspaceId]/page.tsx": 2850,
  "front/components/workspace/Workspace2DCanvas.tsx": 2120,
  "front/e2e/browser-ux.spec.ts": 1820,
  "front/styles/studio-ui.css": 1650,
  "front/lib/pixel-office/character-renderer.ts": 1550,
  "front/lib/workspace-2d.ts": 1550,
});

const SOURCE_LINE_CEILING_EXTENSIONS = new Set([
  ".ts",
  ".tsx",
  ".js",
  ".mjs",
  ".css",
  ".java",
  ".rs",
]);

const TEXT_SCAN_EXTENSIONS = new Set([
  ".ts",
  ".tsx",
  ".js",
  ".mjs",
  ".cjs",
  ".css",
  ".java",
  ".rs",
  ".yml",
  ".yaml",
  ".sh",
  ".json",
  ".toml",
  ".properties",
  ".xml",
  ".sql",
  ".md",
]);

const MAX_SCAN_FILE_BYTES = 1_500_000;

const HOME_PATH_PATTERN = new RegExp(
  String.raw`(?:^|[\s"'` + "`" + String.raw`=])(?:/` +
    String.raw`Users/[A-Za-z0-9._-]+/|/home/[A-Za-z0-9._-]+/|C:\\Users\\[A-Za-z0-9._-]+\\)`,
);

const SECRET_LITERAL_RULES = [
  {
    name: "AWS access key literal",
    pattern: /\bAKIA[0-9A-Z]{16}\b/,
  },
  {
    name: "GitHub token literal",
    pattern: /\b(?:gh[pous]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,})\b/,
  },
  {
    name: "Slack token literal",
    pattern: /\bxox[baprs]-[0-9]{8,}-[A-Za-z0-9-]{8,}\b/,
  },
  {
    name: "Model API secret literal",
    pattern: /\bsk-(?:(?:proj|ant|live)-[A-Za-z0-9_-]{20,}|[A-Za-z0-9]{32,})\b/,
  },
  {
    name: "PEM private key literal",
    pattern: /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----(?:\\n|\r?\n)[A-Za-z0-9+/=]{24,}/,
  },
  {
    name: "Local OS user home path literal",
    pattern: HOME_PATH_PATTERN,
  },
];

export function resolveFileLineCeiling(relativePath) {
  return LEGACY_FILE_LINE_CEILINGS[relativePath] ?? DEFAULT_MAX_SOURCE_LINES;
}

export function findCodeHealthContentViolations(relativePath, content) {
  const normalizedPath = relativePath.replace(/\\/g, "/");
  const ext = path.posix.extname(normalizedPath).toLowerCase();
  const violations = [];

  if (SOURCE_LINE_CEILING_EXTENSIONS.has(ext)) {
    const lineCount = content.length === 0 ? 0 : content.split("\n").length;
    const ceiling = resolveFileLineCeiling(normalizedPath);
    if (lineCount > ceiling) {
      violations.push(
        `${normalizedPath}: line count ${lineCount} exceeds ceiling ${ceiling} (split module before growing)`,
      );
    }
  }

  if (TEXT_SCAN_EXTENSIONS.has(ext)) {
    for (const rule of SECRET_LITERAL_RULES) {
      if (rule.pattern.test(content)) {
        violations.push(`${normalizedPath}: forbidden ${rule.name}`);
      }
    }
  }

  return violations;
}

export function findCodeHealthViolations(repoRoot, trackedFiles) {
  const violations = [];
  for (const relativePath of trackedFiles) {
    const normalizedPath = relativePath.replace(/\\/g, "/");
    const ext = path.posix.extname(normalizedPath).toLowerCase();
    if (!SOURCE_LINE_CEILING_EXTENSIONS.has(ext) && !TEXT_SCAN_EXTENSIONS.has(ext)) {
      continue;
    }

    const fullPath = path.join(repoRoot, relativePath);
    let stat;
    try {
      stat = fs.statSync(fullPath);
    } catch {
      continue;
    }
    if (!stat.isFile() || stat.size > MAX_SCAN_FILE_BYTES) {
      continue;
    }

    const content = fs.readFileSync(fullPath, "utf8");
    violations.push(...findCodeHealthContentViolations(normalizedPath, content));
  }
  return violations;
}

function listTrackedFiles(repoRoot) {
  const output = execFileSync("git", ["ls-files"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return output
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
}

export function runCodeHealthCheck(repoRoot) {
  const violations = findCodeHealthViolations(repoRoot, listTrackedFiles(repoRoot));
  if (violations.length > 0) {
    throw new Error(`[code-health] check failed:\n- ${violations.join("\n- ")}`);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const repoRoot = path.resolve(import.meta.dirname, "../..");
  runCodeHealthCheck(repoRoot);
  console.log("[code-health] file size ceiling & secret hygiene checks passed");
}
