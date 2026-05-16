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

    private TaskExecution(
            Long id,
            Long workspaceId,
            Long taskId,
            Long agentId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason) {
        this.id = id;
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.taskId = requirePositive(taskId, "taskId");
        this.agentId = requirePositive(agentId, "agentId");
        this.status = status == null ? TaskExecutionStatus.QUEUED : status;
        this.resultSummary = normalizeNullable(resultSummary);
        this.failureReason = normalizeNullable(failureReason);
    }

    public static TaskExecution create(Long workspaceId, Long taskId, Long agentId) {
        return new TaskExecution(null, workspaceId, taskId, agentId, TaskExecutionStatus.QUEUED, null, null);
    }

    public static TaskExecution restore(
            Long id,
            Long workspaceId,
            Long taskId,
            Long agentId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason) {
        return new TaskExecution(id, workspaceId, taskId, agentId, status, resultSummary, failureReason);
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

    public void markFailed(String failureReason) {
        this.status = TaskExecutionStatus.FAILED;
        this.failureReason = normalizeNullable(failureReason);
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
