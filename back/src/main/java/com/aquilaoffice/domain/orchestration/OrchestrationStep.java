package com.aquilaoffice.domain.orchestration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 하나의 worker Agent가 수행할 orchestration step과 선행 step 조건을 표현한다.
 */
public class OrchestrationStep {

    public static final int MAX_STEP_KEY_LENGTH = 80;
    public static final int MAX_INSTRUCTION_LENGTH = 1000;
    public static final int MAX_SERIALIZED_DEPENDS_ON_LENGTH = 1000;
    public static final String STEP_KEY_REGEX = "^[A-Za-z0-9_-]{1,80}$";
    private static final Pattern STEP_KEY_PATTERN = Pattern.compile(STEP_KEY_REGEX);

    private final Long id;
    private final String stepKey;
    private final String instruction;
    private final Long agentId;
    private final Long taskId;
    private final List<String> dependsOn;
    private final OrchestrationStepStatus status;

    private OrchestrationStep(
            Long id,
            String stepKey,
            String instruction,
            Long agentId,
            Long taskId,
            List<String> dependsOn,
            OrchestrationStepStatus status) {
        this.id = normalizeOptionalPositive(id, "id");
        this.stepKey = normalizeStepKey(stepKey, "stepKey");
        this.instruction = requireNotBlankWithMaxLength(instruction, "instruction", MAX_INSTRUCTION_LENGTH);
        this.agentId = requirePositive(agentId, "agentId");
        this.taskId = normalizeOptionalPositive(taskId, "taskId");
        this.dependsOn = normalizeDependsOn(dependsOn);
        this.status = status == null ? OrchestrationStepStatus.PENDING : status;
    }

    public static OrchestrationStep create(
            String stepKey,
            String instruction,
            Long agentId,
            List<String> dependsOn) {
        return new OrchestrationStep(null, stepKey, instruction, agentId, null, dependsOn, null);
    }

    public static OrchestrationStep restore(
            Long id,
            String stepKey,
            String instruction,
            Long agentId,
            Long taskId,
            List<String> dependsOn,
            OrchestrationStepStatus status) {
        return new OrchestrationStep(id, stepKey, instruction, agentId, taskId, dependsOn, status);
    }

    public Long getId() {
        return id;
    }

    public String getStepKey() {
        return stepKey;
    }

    public String getInstruction() {
        return instruction;
    }

    public Long getAgentId() {
        return agentId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public OrchestrationStepStatus getStatus() {
        return status;
    }

    private static List<String> normalizeDependsOn(List<String> dependsOn) {
        if (dependsOn == null || dependsOn.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String dependency : dependsOn) {
            normalized.add(normalizeStepKey(dependency, "dependsOn"));
        }
        int serializedLength = normalized.stream().mapToInt(String::length).sum() + Math.max(0, normalized.size() - 1);
        if (serializedLength > MAX_SERIALIZED_DEPENDS_ON_LENGTH) {
            throw new IllegalArgumentException(
                    "dependsOn serialized length must not exceed " + MAX_SERIALIZED_DEPENDS_ON_LENGTH + " characters");
        }
        return List.copyOf(normalized);
    }

    private static String normalizeStepKey(String value, String fieldName) {
        String normalized = requireNotBlankWithMaxLength(value, fieldName, MAX_STEP_KEY_LENGTH);
        if (!STEP_KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must contain only letters, digits, hyphens, or underscores");
        }
        return normalized;
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

    private static String requireNotBlankWithMaxLength(String value, String fieldName, int maxLength) {
        String normalized = requireNotBlank(value, fieldName);
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
