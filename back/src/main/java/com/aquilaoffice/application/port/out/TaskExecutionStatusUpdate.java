package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;

public record TaskExecutionStatusUpdate(
        Long workspaceId,
        Long taskId,
        Long executionId,
        TaskExecutionStatus status,
        String resultSummary,
        String failureReason,
        Long durationMs,
        Integer promptTokens,
        Integer completionTokens,
        String modelName) {

    public TaskExecutionStatusUpdate(
            Long workspaceId,
            Long taskId,
            Long executionId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason) {
        this(workspaceId, taskId, executionId, status, resultSummary, failureReason, null, null, null, null);
    }
}
