package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import java.time.LocalDateTime;

public record WorkspaceDashboardRecentReportData(
        Long executionId,
        Long taskId,
        Long agentId,
        TaskExecutionStatus status,
        String summary,
        String failureReason,
        LocalDateTime createdAt) {}
