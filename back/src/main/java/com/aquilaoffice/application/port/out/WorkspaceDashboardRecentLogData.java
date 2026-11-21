package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

public record WorkspaceDashboardRecentLogData(
        Long executionId,
        Long taskId,
        Long agentId,
        String level,
        String message,
        LocalDateTime createdAt) {}
