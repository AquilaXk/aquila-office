package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import java.time.LocalDateTime;

public record WorkspaceDashboardRecentReportResult(
        Long executionId,
        Long taskId,
        Long agentId,
        TaskExecutionStatus status,
        String summary,
        String failureReason,
        LocalDateTime createdAt) {}
