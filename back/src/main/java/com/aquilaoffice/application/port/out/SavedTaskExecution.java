package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import java.time.LocalDateTime;

public record SavedTaskExecution(
        Long executionId,
        Long workspaceId,
        Long taskId,
        Long agentId,
        TaskExecutionStatus status,
        String resultSummary,
        String failureReason,
        Long durationMs,
        int promptTokens,
        int completionTokens,
        String modelName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SavedTaskExecution(
            Long executionId,
            Long workspaceId,
            Long taskId,
            Long agentId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this(
                executionId,
                workspaceId,
                taskId,
                agentId,
                status,
                resultSummary,
                failureReason,
                null,
                0,
                0,
                null,
                createdAt,
                updatedAt);
    }
}
