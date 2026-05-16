package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;

public record TaskExecutionStatusUpdate(
        Long workspaceId,
        Long taskId,
        Long executionId,
        TaskExecutionStatus status,
        String resultSummary,
        String failureReason) {}
