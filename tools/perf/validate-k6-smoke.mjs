import assert from "node:assert/strict";
import { access, readFile, stat } from "node:fs/promises";
import { constants } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const smokeScriptPath = resolve(root, "tools/perf/k6-smoke.js");
const requestedSmokeScriptPath = resolve(root, "perf/k6/scripts/test/smoke-test.js");
const prometheusConfigPath = resolve(root, "docker/monitoring/prometheus.yml");
const monitoringComposePath = resolve(root, "docker/monitoring/docker-compose.monitoring.yml");
const runnerPath = resolve(root, "tools/test/run-k6-smoke.sh");
const smokeFixtureConfigPath = resolve(root, "back/src/main/java/com/aquilaoffice/global/config/SmokeFixtureConfig.java");
const smokeApplicationConfigPath = resolve(root, "back/src/main/resources/application-smoke.yml");
const ciConfigPath = resolve(root, ".github/workflows/ci.yml");

const smokeScript = await readRequiredFile(smokeScriptPath);
const requestedSmokeScript = await readRequiredFile(requestedSmokeScriptPath);
const prometheusConfig = await readRequiredFile(prometheusConfigPath);
const monitoringCompose = await readRequiredFile(monitoringComposePath);
const runner = await readRequiredFile(runnerPath);
const smokeFixtureConfig = await readRequiredFile(smokeFixtureConfigPath);
const smokeApplicationConfig = await readRequiredFile(smokeApplicationConfigPath);
const ciConfig = await readRequiredFile(ciConfigPath);

assert.match(smokeScript, /import http from ["']k6\/http["'];/);
assert.match(smokeScript, /import \{ check, fail, sleep \} from ["']k6["'];/);
assert.match(smokeScript, /export const options = \{/);
assert.match(smokeScript, /http_req_failed/);
assert.match(smokeScript, /http_req_duration/);
assert.match(smokeScript, /__ENV\.API_BASE_URL \?\? ["']http:\/\/localhost:8080["']/);
assert.match(smokeScript, /\/api\/health/);
assert.match(smokeScript, /\/actuator\/health\/liveness/);
assert.match(smokeScript, /\/actuator\/health\/readiness/);
assert.match(smokeScript, /\/actuator\/prometheus/);
assert.match(smokeScript, /\/api\/v1\/auth\/google\/login/);
assert.match(smokeScript, /\/api\/v1\/workspaces/);
assert.match(smokeScript, /\/gateway\/binding/);
assert.match(smokeScript, /\/agents/);
assert.match(smokeScript, /\/tasks/);
assert.match(smokeScript, /\/reports/);
assert.match(smokeScript, /SMOKE_GOOGLE_ID_TOKEN/);
assert.match(smokeScript, /Smoke task completed/);
assert.match(smokeScript, /\[smoke\]/);
assert.match(smokeScript, /aquilaOfficeSmoke/);

assert.match(requestedSmokeScript, /export \{ options \} from/);
assert.match(requestedSmokeScript, /export \{ default \} from/);
assert.match(requestedSmokeScript, /tools\/perf\/k6-smoke\.js/);

assert.match(prometheusConfig, /job_name: ["']aquila-office-backend["']/);
assert.match(prometheusConfig, /\/actuator\/prometheus/);
assert.match(prometheusConfig, /backend:8080/);

assert.match(monitoringCompose, /prometheus:/);
assert.match(monitoringCompose, /prom\/prometheus/);
assert.match(monitoringCompose, /prometheus\.yml/);

assert.match(runner, /^#!\/usr\/bin\/env bash/);
assert.match(runner, /set -euo pipefail/);
assert.match(runner, /k6 run "\$SCRIPT_PATH"/);
assert.match(runner, /API_BASE_URL="\$\{API_BASE_URL:-http:\/\/localhost:8080\}"/);
assert.match(runner, /SMOKE_SERVICE_JOURNEY="\$\{SMOKE_SERVICE_JOURNEY:-true\}"/);
assert.match(runner, /SMOKE_GOOGLE_ID_TOKEN="\$\{SMOKE_GOOGLE_ID_TOKEN:-smoke-google-id-token\}"/);

assert.match(smokeFixtureConfig, /@Profile\("smoke"\)/);
assert.match(smokeFixtureConfig, /aquila\.smoke-fixture/);
assert.match(smokeFixtureConfig, /VerifyGoogleIdTokenPort/);
assert.match(smokeFixtureConfig, /RunWorkspaceAgentPort/);
assert.match(smokeFixtureConfig, /Smoke task completed/);

assert.match(smokeApplicationConfig, /jdbc:h2:mem:aquila-office-smoke/);
assert.match(smokeApplicationConfig, /smoke-fixture:/);
assert.match(smokeApplicationConfig, /SMOKE_GOOGLE_ID_TOKEN/);

assert.match(ciConfig, /Smoke Definition Gate/);
assert.match(ciConfig, /node tools\/perf\/validate-k6-smoke\.mjs/);

await access(runnerPath, constants.X_OK);
const runnerStat = await stat(runnerPath);
assert.equal(runnerStat.isFile(), true);

async function readRequiredFile(path) {
  try {
    return await readFile(path, "utf8");
  } catch (cause) {
    assert.fail(`required k6 smoke file is missing: ${path}`);
  }
}
