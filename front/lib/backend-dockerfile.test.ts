import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const repoRoot = path.join(process.cwd(), "..");

test("backend docker image uses the prebuilt jar without qemu gradle rebuild", () => {
  const dockerfile = readRepoSource("docker/Dockerfile");

  assert.match(
    dockerfile,
    /FROM --platform=\$TARGETPLATFORM eclipse-temurin:21-jre-alpine AS runtime/,
  );
  assert.match(dockerfile, /COPY back\/build\/libs\/\*\.jar \/app\/aquila-office-back\.jar/);
  assert.doesNotMatch(dockerfile, /gradlew/);
  assert.doesNotMatch(dockerfile, /21-jdk/);
  assert.doesNotMatch(dockerfile, /AS builder/);
});

test("docker context includes only backend jar build output", () => {
  const dockerignore = readRepoSource(".dockerignore");

  assert.match(dockerignore, /^back\/build$/m);
  assert.match(dockerignore, /^!back\/build\/$/m);
  assert.match(dockerignore, /^!back\/build\/libs\/$/m);
  assert.match(dockerignore, /^!back\/build\/libs\/\*\.jar$/m);
});

function readRepoSource(filePath: string): string {
  return readFileSync(path.join(repoRoot, filePath), "utf8");
}
