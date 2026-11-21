import { apiFetch } from "./api/client.ts";

export const MAX_ORCHESTRATION_STEPS = 20;
export const MAX_ORCHESTRATION_STEP_KEY_CHARS = 80;
export const MAX_ORCHESTRATION_INSTRUCTION_CHARS = 1000;
export const MAX_ORCHESTRATION_DEPENDS_ON_SERIALIZED_CHARS = 1000;
const STEP_KEY_PATTERN = /^[A-Za-z0-9_-]{1,80}$/;

export type OrchestrationStepStatus =
  | "PENDING"
  | "DISPATCHED"
  | "RUNNING"
  | "SUCCEEDED"
  | "FAILED"
  | "SKIPPED";

export type OrchestrationSessionStatus =
  | "PLANNED"
  | "RUNNING"
  | "SUCCEEDED"
  | "FAILED"
  | "CANCELED";

export type OrchestrationStep = {
  stepKey: string;
  instruction: string;
  agentId: number;
  taskId: number | null;
  dependsOn: string[];
  status: OrchestrationStepStatus;
};

export type OrchestrationSession = {
  sessionId: number;
  workspaceId: number;
  sourceTaskId: number;
  status: OrchestrationSessionStatus;
  createdAt?: string;
  updatedAt?: string;
  steps: OrchestrationStep[];
};

export type CreateOrchestrationStepInput = {
  stepKey: string;
  instruction: string;
  agentId: number;
  dependsOn?: string[];
};

export type CreateOrchestrationSessionInput = {
  sourceTaskId: number;
  steps: CreateOrchestrationStepInput[];
};

export type DispatchOrchestrationSessionInput = {
  createPr?: boolean;
};

export type OrchestrationDispatchResult = {
  sessionId: number;
  workspaceId: number;
  sourceTaskId: number;
  status: OrchestrationSessionStatus;
  dispatchedCount: number;
  steps: OrchestrationStep[];
};

export function workspaceOrchestrationsPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/orchestrations`;
}

export function workspaceOrchestrationPath(workspaceId: number, sessionId: number): string {
  return `${workspaceOrchestrationsPath(workspaceId)}/${sessionId}`;
}

export function workspaceOrchestrationDispatchPath(
  workspaceId: number,
  sessionId: number,
): string {
  return `${workspaceOrchestrationPath(workspaceId, sessionId)}/dispatch`;
}

export function validateCreateOrchestrationInput(
  input: CreateOrchestrationSessionInput,
): CreateOrchestrationSessionInput {
  if (!Number.isInteger(input.sourceTaskId) || input.sourceTaskId <= 0) {
    throw new RangeError("기반 작업(sourceTaskId)을 선택해 주세요.");
  }
  if (!Array.isArray(input.steps) || input.steps.length === 0) {
    throw new RangeError("최소 1개 이상의 오케스트레이션 스텝을 추가해 주세요.");
  }
  if (input.steps.length > MAX_ORCHESTRATION_STEPS) {
    throw new RangeError(
      `오케스트레이션 스텝은 최대 ${MAX_ORCHESTRATION_STEPS}개까지 허용됩니다.`,
    );
  }

  const normalizedSteps: CreateOrchestrationStepInput[] = [];
  const stepKeys = new Set<string>();

  for (let i = 0; i < input.steps.length; i += 1) {
    const rawStep = input.steps[i];
    const stepKey = rawStep.stepKey.trim();
    const instruction = rawStep.instruction.trim();
    if (!stepKey || stepKey.length > MAX_ORCHESTRATION_STEP_KEY_CHARS || !STEP_KEY_PATTERN.test(stepKey)) {
      throw new RangeError(
        `스텝 키(${stepKey || i + 1})는 영문/숫자/하이픈/밑줄 최대 ${MAX_ORCHESTRATION_STEP_KEY_CHARS}자여야 합니다.`,
      );
    }
    if (stepKeys.has(stepKey)) {
      throw new RangeError(`중복된 스텝 키(${stepKey})가 있습니다.`);
    }
    if (!Number.isInteger(rawStep.agentId) || rawStep.agentId <= 0) {
      throw new RangeError(`스텝(${stepKey})에 담당 에이전트를 지정해 주세요.`);
    }
    if (!instruction || instruction.length > MAX_ORCHESTRATION_INSTRUCTION_CHARS) {
      throw new RangeError(
        `스텝(${stepKey}) 지시문은 1~${MAX_ORCHESTRATION_INSTRUCTION_CHARS}자여야 합니다.`,
      );
    }

    const dedupedDependsOn = Array.from(
      new Set(
        (rawStep.dependsOn ?? [])
          .map((dep) => dep.trim())
          .filter((dep) => dep.length > 0),
      ),
    );
    if (dedupedDependsOn.includes(stepKey)) {
      throw new RangeError(`스텝(${stepKey})이 자기 자신을 의존할 수 없습니다.`);
    }
    if (dedupedDependsOn.join("\n").length > MAX_ORCHESTRATION_DEPENDS_ON_SERIALIZED_CHARS) {
      throw new RangeError(
        `스텝(${stepKey}) 선행 의존 키 총합은 ${MAX_ORCHESTRATION_DEPENDS_ON_SERIALIZED_CHARS}자를 초과할 수 없습니다.`,
      );
    }

    stepKeys.add(stepKey);
    normalizedSteps.push({
      stepKey,
      instruction,
      agentId: rawStep.agentId,
      dependsOn: dedupedDependsOn,
    });
  }

  const depsByKey = new Map<string, string[]>();
  for (let i = 0; i < normalizedSteps.length; i += 1) {
    const step = normalizedSteps[i];
    const deps = step.dependsOn ?? [];
    for (let j = 0; j < deps.length; j += 1) {
      if (!stepKeys.has(deps[j])) {
        throw new RangeError(`스텝(${step.stepKey})의 선행 스텝(${deps[j]})이 존재하지 않습니다.`);
      }
    }
    depsByKey.set(step.stepKey, deps);
  }

  // Kahn / DFS 3-color cycle detection for Fail-Closed DAG validation
  const visitState = new Map<string, 1 | 2>();
  const hasCycle = (key: string): boolean => {
    const state = visitState.get(key);
    if (state === 1) return true;
    if (state === 2) return false;
    visitState.set(key, 1);
    const deps = depsByKey.get(key) ?? [];
    for (let i = 0; i < deps.length; i += 1) {
      if (hasCycle(deps[i])) {
        return true;
      }
    }
    visitState.set(key, 2);
    return false;
  };

  for (const key of stepKeys) {
    if (hasCycle(key)) {
      throw new RangeError("스텝 간 순환 의존성(Cycle)이 감지되었습니다.");
    }
  }

  return {
    sourceTaskId: input.sourceTaskId,
    steps: normalizedSteps,
  };
}

export function normalizeOrchestrationSessions(payload: unknown): OrchestrationSession[] {
  if (Array.isArray(payload)) {
    return payload as OrchestrationSession[];
  }
  if (payload && typeof payload === "object") {
    const record = payload as Record<string, unknown>;
    if (Array.isArray(record.content)) {
      return record.content as OrchestrationSession[];
    }
    if (Array.isArray(record.sessions)) {
      return record.sessions as OrchestrationSession[];
    }
  }
  return [];
}

export async function fetchOrchestrationSessions(
  accessToken: string,
  workspaceId: number,
): Promise<OrchestrationSession[]> {
  const payload = await apiFetch<unknown>(workspaceOrchestrationsPath(workspaceId), {
    accessToken,
  });
  return normalizeOrchestrationSessions(payload);
}

export async function fetchOrchestrationSession(
  accessToken: string,
  workspaceId: number,
  sessionId: number,
): Promise<OrchestrationSession> {
  return apiFetch<OrchestrationSession>(workspaceOrchestrationPath(workspaceId, sessionId), {
    accessToken,
  });
}

export async function createOrchestrationSession(
  accessToken: string,
  workspaceId: number,
  input: CreateOrchestrationSessionInput,
): Promise<OrchestrationSession> {
  const validated = validateCreateOrchestrationInput(input);
  return apiFetch<OrchestrationSession>(workspaceOrchestrationsPath(workspaceId), {
    method: "POST",
    accessToken,
    json: validated,
  });
}

export async function dispatchOrchestrationSession(
  accessToken: string,
  workspaceId: number,
  sessionId: number,
  input: DispatchOrchestrationSessionInput = {},
): Promise<OrchestrationDispatchResult> {
  return apiFetch<OrchestrationDispatchResult>(
    workspaceOrchestrationDispatchPath(workspaceId, sessionId),
    {
      method: "POST",
      accessToken,
      json: input,
    },
  );
}
