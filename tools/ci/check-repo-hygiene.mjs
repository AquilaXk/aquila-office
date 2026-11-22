import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { findCodeHealthViolations } from "./check-code-health.mjs";

const REQUIRED_TRACKED_FILES = [
  "README.md",
  "LICENSE",
  "NOTICE",
  "THIRD_PARTY_NOTICES.md",
  "CONTRIBUTING.md",
  "SECURITY.md",
  "TODO.md",
  ".github/ISSUE_TEMPLATE/config.yml",
  ".github/ISSUE_TEMPLATE/bug_report.yml",
  ".github/release.yml",
];

const FORBIDDEN_TRACKED_FILE_RULES = [
  {
    pattern: /(^|\/)(AGENTS|CLAUDE|GEMINI)\.md$/,
    reason: "local-only agent contract document must not be tracked",
  },
  {
    pattern: /(^|\/)CLAUDE\.local\.md$/,
    reason: "local-only agent override document must not be tracked",
  },
  {
    pattern: /(^|\/)(implementation_plan|walkthrough)\.md$/,
    reason: "local scratch planning artifact must not be tracked",
  },
  {
    pattern: /^\.(claude|codex)\//,
    reason: "local agent directory must not be tracked",
  },
  {
    pattern: /^\.(agents|gemini)\//,
    reason: "local agent directory must not be tracked",
  },
  {
    pattern: /(^|\/)\.coderabbit\.yaml$/,
    reason: "automated review bot configuration must not be tracked",
  },
  {
    pattern: /^\.github\/dependabot\.ya?ml$/,
    reason: "automated dependency PR configuration must not be tracked",
  },
  {
    pattern: /^\.github\/(pull_request_template\.md|PULL_REQUEST_TEMPLATE\/)/i,
    reason: "pull request template must not be tracked in solo workflow",
  },
  {
    pattern: /^\.github\/ISSUE_TEMPLATE\/(epic|feature|bug)\.ya?ml$/,
    reason: "legacy internal issue template must not be tracked; use bug_report.yml only",
  },
  {
    pattern: /(^|\/)\.env$/,
    reason: "plaintext environment file must not be tracked",
  },
  {
    pattern: /(^|\/)vertex-key\.json$/,
    reason: "service account key file must not be tracked",
  },
];

const FORBIDDEN_COMMIT_PATTERNS = [
  {
    pattern: /^Co-Authored-By:/im,
    reason: "co-author attribution trailer is not allowed in commit messages",
  },
  {
    pattern: /Generated with /i,
    reason: "automated generation banner is not allowed in commit messages",
  },
];

const FORBIDDEN_BRANCH_PREFIX = /^(claude|codex)\//i;

export function findMissingRequiredTrackedFiles(trackedFiles) {
  const trackedSet = new Set(trackedFiles.map((file) => file.trim()).filter(Boolean));
  const missing = [];
  for (const requiredFile of REQUIRED_TRACKED_FILES) {
    if (!trackedSet.has(requiredFile)) {
      missing.push(`${requiredFile} (required tracked governance/security contract file is missing)`);
    }
  }
  return missing;
}

export function findForbiddenTrackedFiles(trackedFiles) {
  const violations = [];
  for (const rawFile of trackedFiles) {
    const file = rawFile.trim();
    if (!file) {
      continue;
    }
    for (const rule of FORBIDDEN_TRACKED_FILE_RULES) {
      if (rule.pattern.test(file)) {
        violations.push(`${file} (${rule.reason})`);
        break;
      }
    }
  }
  return violations;
}

export function findForbiddenCommitMessages(commits) {
  const violations = [];
  for (const commit of commits) {
    for (const rule of FORBIDDEN_COMMIT_PATTERNS) {
      if (rule.pattern.test(commit.message)) {
        violations.push(`${commit.sha} (${rule.reason})`);
        break;
      }
    }
  }
  return violations;
}

export function findForbiddenBranchName(branchName) {
  const trimmed = (branchName || "").trim();
  if (trimmed && FORBIDDEN_BRANCH_PREFIX.test(trimmed)) {
    return [`${trimmed} (agent-prefixed branch name is not allowed)`];
  }
  return [];
}

function readTrackedFiles(repoRoot) {
  const output = execFileSync("git", ["ls-files"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return output.split(/\r?\n/);
}

function readCurrentBranch(repoRoot) {
  try {
    return execFileSync("git", ["symbolic-ref", "--quiet", "--short", "HEAD"], {
      cwd: repoRoot,
      encoding: "utf8",
    }).trim();
  } catch {
    return "";
  }
}

function readRecentCommits(repoRoot) {
  const rangeArgs = hasAncestorCommit(repoRoot, "HEAD~5")
    ? ["log", "--format=%h%x1f%B%x1e", "HEAD~5..HEAD"]
    : ["log", "-n", "1", "--format=%h%x1f%B%x1e", "HEAD"];
  const output = execFileSync("git", rangeArgs, {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return output
    .split("\x1e")
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) => {
      const separatorIndex = entry.indexOf("\x1f");
      if (separatorIndex < 0) {
        return { sha: "HEAD", message: entry };
      }
      return {
        sha: entry.slice(0, separatorIndex).trim(),
        message: entry.slice(separatorIndex + 1),
      };
    });
}

function hasAncestorCommit(repoRoot, rev) {
  try {
    execFileSync("git", ["rev-parse", "--verify", "--quiet", rev], {
      cwd: repoRoot,
      stdio: "ignore",
    });
    return true;
  } catch {
    return false;
  }
}

export function runRepoHygieneCheck(repoRoot) {
  const trackedFiles = readTrackedFiles(repoRoot);
  const missingViolations = findMissingRequiredTrackedFiles(trackedFiles);
  const fileViolations = findForbiddenTrackedFiles(trackedFiles);
  const commitViolations = findForbiddenCommitMessages(readRecentCommits(repoRoot));
  const branchViolations = findForbiddenBranchName(readCurrentBranch(repoRoot));
  const codeHealthViolations = findCodeHealthViolations(repoRoot, trackedFiles);
  const violations = [
    ...missingViolations,
    ...fileViolations,
    ...commitViolations,
    ...branchViolations,
    ...codeHealthViolations,
  ];
  if (violations.length > 0) {
    throw new Error(`[repo-hygiene] check failed:\n- ${violations.join("\n- ")}`);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const repoRoot = path.resolve(import.meta.dirname, "../..");
  runRepoHygieneCheck(repoRoot);
  console.log("[repo-hygiene] repository hygiene checks passed");
}
