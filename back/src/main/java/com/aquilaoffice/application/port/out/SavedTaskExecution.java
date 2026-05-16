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
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
