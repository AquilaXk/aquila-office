import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("frontend docker runtime uses standalone output without target npm install", () => {
  const dockerfile = readSource("Dockerfile");
  const runtimeStage = dockerfile.slice(dockerfile.indexOf("AS runtime"));

  assert.match(
    dockerfile,
    /FROM --platform=\$BUILDPLATFORM node:24-alpine AS dependencies/,
  );
  assert.match(
    dockerfile,
    /FROM --platform=\$BUILDPLATFORM node:24-alpine AS builder/,
  );
  assert.match(
    dockerfile,
    /FROM --platform=\$TARGETPLATFORM node:24-alpine AS runtime/,
  );
  assert.match(runtimeStage, /COPY --from=builder .*\.next\/standalone \.\//);
  assert.match(runtimeStage, /CMD \["node", "server\.js"\]/);
  assert.doesNotMatch(runtimeStage, /RUN npm ci/);
});

test("next build emits standalone server files for the container image", () => {
  const source = readSource("next.config.ts");

  assert.match(source, /output:\s*"standalone"/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
