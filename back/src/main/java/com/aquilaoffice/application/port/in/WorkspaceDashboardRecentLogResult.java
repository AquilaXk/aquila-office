package com.aquilaoffice.application.port.in;

import java.time.LocalDateTime;

public record WorkspaceDashboardRecentLogResult(
        Long executionId,
        Long taskId,
        Long agentId,
        String level,
        String message,
        LocalDateTime createdAt) {}
