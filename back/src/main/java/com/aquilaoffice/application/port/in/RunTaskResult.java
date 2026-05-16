package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskStatus;

public record RunTaskResult(
        Long workspaceId,
        Long taskId,
        Long executionId,
        Long agentId,
        TaskStatus taskStatus,
        TaskExecutionStatus executionStatus,
        String finalText,
        String failureReason) {}
