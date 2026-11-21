package com.aquilaoffice.domain.execution;

/**
 * Agent가 Task를 실행한 단위 기록을 표현한다.
 */
public class TaskExecution {

    private final Long id;
    private final Long workspaceId;
    private final Long taskId;
    private final Long agentId;
    private TaskExecutionStatus status;
    private String resultSummary;
    private String failureReason;
    private Long durationMs;
    private int promptTokens;
    private int completionTokens;
    private String modelName;

    private TaskExecution(
            Long id,
            Long workspaceId,
            Long taskId,
            Long agentId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason,
            Long durationMs,
            int promptTokens,
            int completionTokens,
            String modelName) {
        this.id = id;
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.taskId = requirePositive(taskId, "taskId");
        this.agentId = requirePositive(agentId, "agentId");
        this.status = status == null ? TaskExecutionStatus.QUEUED : status;
        this.resultSummary = normalizeNullable(resultSummary);
        this.failureReason = normalizeNullable(failureReason);
        this.durationMs = durationMs == null || durationMs < 0 ? null : durationMs;
        this.promptTokens = Math.max(0, promptTokens);
        this.completionTokens = Math.max(0, completionTokens);
        this.modelName = normalizeNullable(modelName);
    }

    public static TaskExecution create(Long workspaceId, Long taskId, Long agentId) {
        return new TaskExecution(
                null,
                workspaceId,
                taskId,
                agentId,
                TaskExecutionStatus.QUEUED,
                null,
                null,
                null,
                0,
                0,
                null);
    }

    public static TaskExecution restore(
            Long id,
            Long workspaceId,
            Long taskId,
            Long agentId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason) {
        return restore(id, workspaceId, taskId, agentId, status, resultSummary, failureReason, null, 0, 0, null);
    }

    public static TaskExecution restore(
            Long id,
            Long workspaceId,
            Long taskId,
            Long agentId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason,
            Long durationMs,
            int promptTokens,
            int completionTokens,
            String modelName) {
        return new TaskExecution(
                id,
                workspaceId,
                taskId,
                agentId,
                status,
                resultSummary,
                failureReason,
                durationMs,
                promptTokens,
                completionTokens,
                modelName);
    }

    public void markRunning() {
        this.status = TaskExecutionStatus.RUNNING;
        this.failureReason = null;
    }

    public void markSucceeded(String resultSummary) {
        this.status = TaskExecutionStatus.SUCCEEDED;
        this.resultSummary = normalizeNullable(resultSummary);
        this.failureReason = null;
    }

    public void markSucceeded(
            String resultSummary,
            Long durationMs,
            int promptTokens,
            int completionTokens,
            String modelName) {
        markSucceeded(resultSummary);
        this.durationMs = durationMs == null || durationMs < 0 ? null : durationMs;
        this.promptTokens = Math.max(0, promptTokens);
        this.completionTokens = Math.max(0, completionTokens);
        this.modelName = normalizeNullable(modelName);
    }

    public void markFailed(String failureReason) {
        this.status = TaskExecutionStatus.FAILED;
        this.failureReason = normalizeNullable(failureReason);
    }

    public void markCanceled(String failureReason) {
        this.status = TaskExecutionStatus.CANCELED;
        this.failureReason = normalizeNullable(failureReason);
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public String getModelName() {
        return modelName;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public TaskExecutionStatus getStatus() {
        return status;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public String getFailureReason() {
        return failureReason;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
