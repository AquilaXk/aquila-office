import { spawnSync } from "node:child_process";
import path from "node:path";

// Cross-platform desktop static export wrapper so Windows cmd/PowerShell and Unix shells run consistently.
const frontDir = path.resolve(import.meta.dirname, "..");
const nextBin = path.join(frontDir, "node_modules", "next", "dist", "bin", "next");

const result = spawnSync(process.execPath, [nextBin, "build"], {
  cwd: frontDir,
  stdio: "inherit",
  env: {
    ...process.env,
    BUILD_TARGET: "desktop",
  },
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);
