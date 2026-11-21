package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

public record SavedTaskExecutionLog(
        Long logId,
        Long workspaceId,
        Long taskId,
        Long executionId,
        Long agentId,
        String level,
        String message,
        LocalDateTime createdAt) {}
