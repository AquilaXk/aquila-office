import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    smoke: {
      executor: "constant-vus",
      vus: 1,
      duration: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

const BASE_URL = __ENV.API_BASE_URL ?? "http://localhost:8080";

export default function aquilaOfficeSmoke() {
  aquilaOfficeSmokeCheck("/api/health", "api health");
  aquilaOfficeSmokeCheck("/actuator/health/liveness", "actuator liveness");
  aquilaOfficeSmokeCheck("/actuator/health/readiness", "actuator readiness");
  aquilaOfficeSmokeCheck("/actuator/prometheus", "prometheus metrics", (body) =>
    body.includes("# HELP") || body.includes("jvm_") || body.includes("process_"),
  );

  sleep(1);
}

function aquilaOfficeSmokeCheck(path, label, bodyCheck = () => true) {
  const response = http.get(`${BASE_URL}${path}`);

  check(response, {
    [`${label} returns 200`]: (result) => result.status === 200,
    [`${label} returns expected body`]: (result) => bodyCheck(result.body ?? ""),
  });
}
