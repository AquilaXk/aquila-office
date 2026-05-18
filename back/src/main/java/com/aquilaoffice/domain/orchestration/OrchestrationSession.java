package com.aquilaoffice.domain.orchestration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multi Agent 실행 계획의 step 그래프와 session 상태를 관리한다.
 */
public class OrchestrationSession {

    private final Long id;
    private final Long workspaceId;
    private final Long sourceTaskId;
    private final String createdByGoogleSub;
    private OrchestrationSessionStatus status;
    private final List<OrchestrationStep> steps;

    private OrchestrationSession(
            Long id,
            Long workspaceId,
            Long sourceTaskId,
            String createdByGoogleSub,
            OrchestrationSessionStatus status,
            List<OrchestrationStep> steps) {
        this.id = normalizeOptionalPositive(id, "id");
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.sourceTaskId = requirePositive(sourceTaskId, "sourceTaskId");
        this.createdByGoogleSub = requireNotBlank(createdByGoogleSub, "createdByGoogleSub");
        this.status = status == null ? OrchestrationSessionStatus.PLANNED : status;
        this.steps = validateSteps(steps);
    }

    public static OrchestrationSession create(
            Long workspaceId,
            Long sourceTaskId,
            String createdByGoogleSub,
            List<OrchestrationStep> steps) {
        return new OrchestrationSession(null, workspaceId, sourceTaskId, createdByGoogleSub, null, steps);
    }

    public static OrchestrationSession restore(
            Long id,
            Long workspaceId,
            Long sourceTaskId,
            String createdByGoogleSub,
            OrchestrationSessionStatus status,
            List<OrchestrationStep> steps) {
        return new OrchestrationSession(id, workspaceId, sourceTaskId, createdByGoogleSub, status, steps);
    }

    public List<OrchestrationStep> readySteps() {
        Set<String> succeededStepKeys = new HashSet<>();
        for (OrchestrationStep step : steps) {
            if (step.getStatus() == OrchestrationStepStatus.SUCCEEDED) {
                succeededStepKeys.add(step.getStepKey());
            }
        }
        return steps.stream()
                .filter(step -> step.getStatus() == OrchestrationStepStatus.PENDING)
                .filter(step -> succeededStepKeys.containsAll(step.getDependsOn()))
                .toList();
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getSourceTaskId() {
        return sourceTaskId;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public OrchestrationSessionStatus getStatus() {
        return status;
    }

    public List<OrchestrationStep> getSteps() {
        return steps;
    }

    public void markRunning() {
        if (status == OrchestrationSessionStatus.PLANNED) {
            status = OrchestrationSessionStatus.RUNNING;
        }
    }

    public void markSucceededIfComplete() {
        if (steps.stream().allMatch(step -> step.getStatus() == OrchestrationStepStatus.SUCCEEDED)) {
            status = OrchestrationSessionStatus.SUCCEEDED;
        }
    }

    private static List<OrchestrationStep> validateSteps(List<OrchestrationStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("orchestration steps must not be empty");
        }
        Set<String> stepKeys = new HashSet<>();
        for (OrchestrationStep step : steps) {
            if (step == null) {
                throw new IllegalArgumentException("orchestration step must not be null");
            }
            if (!stepKeys.add(step.getStepKey())) {
                throw new IllegalArgumentException("orchestration step key must be unique");
            }
        }
        for (OrchestrationStep step : steps) {
            for (String dependency : step.getDependsOn()) {
                if (!stepKeys.contains(dependency)) {
                    throw new IllegalArgumentException("orchestration step dependency not found");
                }
                if (step.getStepKey().equals(dependency)) {
                    throw new IllegalArgumentException("orchestration step cannot depend on itself");
                }
            }
        }
        return List.copyOf(steps);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static Long normalizeOptionalPositive(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        return requirePositive(value, fieldName);
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
