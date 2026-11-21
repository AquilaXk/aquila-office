import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const GATE_KEYS = ["backend", "deploy", "docs", "frontend", "infra", "smoke", "workflow"];
const DOCS_ONLY_FRONTEND_ASSETS = new Set([
  "front/public/aquila-office-readme-preview.png",
]);

export function classifyChangedFiles(files, options = {}) {
  const gates = createEmptyGates();
  const normalizedFiles = normalizeFiles(files);

  if (options.runAll) {
    return enableAll(gates);
  }

  for (const file of normalizedFiles) {
    applyFile(gates, file);
  }

  return gates;
}

function createEmptyGates() {
  return Object.fromEntries(GATE_KEYS.map((key) => [key, false]));
}

function enableAll(gates) {
  for (const key of GATE_KEYS) {
    gates[key] = true;
  }
  return gates;
}

function normalizeFiles(files) {
  return files
    .map((file) => file.trim())
    .filter(Boolean)
    .map((file) => file.replace(/^\.\//, ""));
}

function applyFile(gates, file) {
  if (file === ".github/workflows/ci.yml") {
    gates.backend = true;
    gates.docs = true;
    gates.frontend = true;
    gates.infra = true;
    gates.smoke = true;
    gates.workflow = true;
    return;
  }

  if (file === ".github/workflows/backend-cd.yml") {
    gates.workflow = true;
    gates.deploy = true;
    return;
  }

  if (file.startsWith(".github/workflows/")) {
    gates.workflow = true;
    return;
  }

  if (
    file.startsWith(".github/") ||
    file === ".gitignore" ||
    file === "CONTRIBUTING.md" ||
    file === "SECURITY.md" ||
    file === "TODO.md"
  ) {
    gates.workflow = true;
    return;
  }

  if (
    file === "README.md" ||
    file === "LICENSE" ||
    file === "NOTICE" ||
    file === "THIRD_PARTY_NOTICES.md" ||
    file.startsWith("tools/readme/")
  ) {
    gates.docs = true;
    return;
  }

  if (DOCS_ONLY_FRONTEND_ASSETS.has(file) || file.startsWith("front/public/readme/")) {
    gates.docs = true;
    return;
  }

  if (file.startsWith("tools/ci/") || file.startsWith("tools/release/")) {
    gates.workflow = true;
    return;
  }

  if (file.startsWith("back/")) {
    gates.backend = true;
    gates.deploy = true;
    return;
  }

  if (file.startsWith("front/src-tauri/")) {
    gates.frontend = true;
    return;
  }

  if (file.startsWith("front/")) {
    gates.frontend = true;
    gates.deploy = true;
    return;
  }

  if (file.startsWith("docker/") || file === "compose.dev.yml" || file === "compose.prod.yml") {
    gates.backend = true;
    gates.frontend = true;
    gates.deploy = true;
    return;
  }

  if (file.startsWith("infra/")) {
    gates.infra = true;
    return;
  }

  if (file.startsWith("perf/") || file.startsWith("tools/perf/") || file === "tools/test/run-k6-smoke.sh") {
    gates.smoke = true;
    return;
  }

  if (file.startsWith("scripts/deploy/")) {
    gates.workflow = true;
    gates.deploy = true;
    return;
  }

  if (file === "tools/test/check-staging-cd-contract.sh") {
    gates.workflow = true;
  }
}

function printOutputs(gates) {
  for (const key of GATE_KEYS) {
    console.log(`${key}=${gates[key] ? "true" : "false"}`);
  }
}

function readChangedFilesFromCli(argv) {
  const fileListArg = argv.find((arg) => arg.startsWith("--files="));
  if (!fileListArg) {
    throw new Error("usage: node tools/ci/resolve-ci-gates.mjs --files=<path> [--all]");
  }

  const fileListPath = fileListArg.slice("--files=".length);
  return readFileSync(fileListPath, "utf8").split(/\r?\n/);
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const runAll = process.argv.includes("--all");
  printOutputs(classifyChangedFiles(readChangedFilesFromCli(process.argv.slice(2)), { runAll }));
}
