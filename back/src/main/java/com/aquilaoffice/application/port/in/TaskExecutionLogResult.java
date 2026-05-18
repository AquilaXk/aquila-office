package com.aquilaoffice.application.port.in;

import java.time.LocalDateTime;

public record TaskExecutionLogResult(
        Long logId,
        Long workspaceId,
        Long taskId,
        Long taskExecutionId,
        Long agentId,
        String level,
        String message,
        LocalDateTime createdAt) {}
