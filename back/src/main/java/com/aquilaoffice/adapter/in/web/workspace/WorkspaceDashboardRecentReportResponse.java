package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.WorkspaceDashboardRecentReportResult;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import java.time.LocalDateTime;

public record WorkspaceDashboardRecentReportResponse(
        Long executionId,
        Long taskId,
        Long agentId,
        TaskExecutionStatus status,
        String summary,
        String failureReason,
        LocalDateTime createdAt) {

    static WorkspaceDashboardRecentReportResponse from(WorkspaceDashboardRecentReportResult result) {
        return new WorkspaceDashboardRecentReportResponse(
                result.executionId(),
                result.taskId(),
                result.agentId(),
                result.status(),
                result.summary(),
                result.failureReason(),
                result.createdAt());
    }
}
