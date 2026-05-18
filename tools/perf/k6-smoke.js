import http from "k6/http";
import { check, fail, sleep } from "k6";

export const options = {
  scenarios: {
    smoke: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      maxDuration: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

const BASE_URL = __ENV.API_BASE_URL ?? "http://localhost:8080";
const RUN_SERVICE_JOURNEY = (__ENV.SMOKE_SERVICE_JOURNEY ?? "true") !== "false";
const SMOKE_GOOGLE_ID_TOKEN = __ENV.SMOKE_GOOGLE_ID_TOKEN ?? "smoke-google-id-token";

export default function aquilaOfficeSmoke() {
  aquilaOfficeSmokeCheck("/api/health", "api health");
  aquilaOfficeSmokeCheck("/actuator/health/liveness", "actuator liveness");
  aquilaOfficeSmokeCheck("/actuator/health/readiness", "actuator readiness");
  aquilaOfficeSmokeCheck("/actuator/prometheus", "prometheus metrics", (body) =>
    body.includes("# HELP") || body.includes("jvm_") || body.includes("process_"),
  );
  if (RUN_SERVICE_JOURNEY) {
    runServiceJourney();
  }

  sleep(1);
}

function aquilaOfficeSmokeCheck(path, label, bodyCheck = () => true) {
  const response = http.get(`${BASE_URL}${path}`);

  check(response, {
    [`${label} returns 200`]: (result) => result.status === 200,
    [`${label} returns expected body`]: (result) => bodyCheck(result.body ?? ""),
  });
}

function runServiceJourney() {
  const runId = `${Date.now()}-${__VU}-${__ITER}`;
  const accessToken = smokeStep("login", () => {
    const data = postJson(
      "/api/v1/auth/google/login",
      { idToken: SMOKE_GOOGLE_ID_TOKEN },
      null,
      "login",
      200,
    );
    assertValue(data.accessToken, "login accessToken");
    return data.accessToken;
  });

  const workspaceId = smokeStep("workspace", () => {
    const data = postJson(
      "/api/v1/workspaces",
      {
        name: `Smoke Workspace ${runId}`,
        description: "k6 service journey smoke fixture",
      },
      accessToken,
      "workspace create",
      201,
    );
    assertValue(data.workspaceId, "workspaceId");
    return data.workspaceId;
  });

  smokeStep("gateway binding", () =>
    postJson(
      `/api/v1/workspaces/${workspaceId}/gateway/binding`,
      {
        gatewayUrl: "https://gateway.smoke.local",
        token: "smoke-gateway-token",
        validateConnection: false,
      },
      accessToken,
      "gateway binding",
      201,
    ));

  const agentId = smokeStep("agent", () => {
    const data = postJson(
      `/api/v1/workspaces/${workspaceId}/agents`,
      {
        name: `Smoke Agent ${runId}`,
        category: "BACKEND",
        workspacePath: `.aquila/smoke/${runId}`,
        skillFiles: [],
      },
      accessToken,
      "agent create",
      201,
    );
    assertValue(data.agentId, "agentId");
    return data.agentId;
  });

  const taskId = smokeStep("task", () => {
    const data = postJson(
      `/api/v1/workspaces/${workspaceId}/tasks`,
      {
        title: `Smoke Task ${runId}`,
        description: "Smoke task completed by deterministic Gateway fixture",
        type: "TEST_CREATION",
        priority: "MEDIUM",
        assignedAgentId: agentId,
      },
      accessToken,
      "task create",
      201,
    );
    assertValue(data.taskId, "taskId");
    return data.taskId;
  });

  smokeStep("task run", () => {
    const data = postJson(
      `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/run`,
      { createPr: false },
      accessToken,
      "task run",
      200,
    );
    checkOrFail("task run succeeded", data, {
      "task execution succeeded": (result) => result.executionStatus === "SUCCEEDED",
      "task completed": (result) => result.taskStatus === "COMPLETED",
      "task has smoke text": (result) => `${result.finalText ?? ""}`.includes("Smoke task completed"),
    });
  });

  smokeStep("report", () => {
    const reports = getJson(
      `/api/v1/workspaces/${workspaceId}/tasks/${taskId}/reports`,
      accessToken,
      "task reports",
      200,
    );
    checkOrFail("task reports", reports, {
      "report list returned": (result) => Array.isArray(result),
      "report includes execution": (result) => result.length > 0,
    });
  });
}

function smokeStep(label, callback) {
  console.log(`[smoke] ${label} started`);
  const result = callback();
  console.log(`[smoke] ${label} completed`);
  return result;
}

function postJson(path, payload, accessToken, label, expectedStatus) {
  const response = http.post(`${BASE_URL}${path}`, JSON.stringify(payload), jsonParams(accessToken));
  assertEnvelope(response, label, expectedStatus);
  return response.json("data");
}

function getJson(path, accessToken, label, expectedStatus) {
  const response = http.get(`${BASE_URL}${path}`, jsonParams(accessToken));
  assertEnvelope(response, label, expectedStatus);
  return response.json("data");
}

function jsonParams(accessToken) {
  const headers = { "Content-Type": "application/json" };
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }
  return { headers };
}

function assertEnvelope(response, label, expectedStatus) {
  checkOrFail(label, response, {
    [`${label} returns ${expectedStatus}`]: (result) => result.status === expectedStatus,
    [`${label} returns success envelope`]: (result) => responseJson(result, "success") === true,
  });
}

function checkOrFail(label, value, checks) {
  if (!check(value, checks)) {
    fail(`[smoke] ${label} failed: ${formatFailure(value)}`);
  }
}

function assertValue(value, label) {
  if (value === null || value === undefined || value === "") {
    fail(`[smoke] ${label} missing`);
  }
}

function responseJson(response, path) {
  try {
    return response.json(path);
  } catch (error) {
    return undefined;
  }
}

function formatFailure(value) {
  if (value && typeof value.status === "number") {
    return `status=${value.status} body=${`${value.body ?? ""}`.slice(0, 500)}`;
  }
  return JSON.stringify(value);
}
