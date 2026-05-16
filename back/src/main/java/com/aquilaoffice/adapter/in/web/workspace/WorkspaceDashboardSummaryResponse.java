package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryResult;
import java.util.List;

public record WorkspaceDashboardSummaryResponse(
        long agentCount,
        long runningAgentCount,
        long idleAgentCount,
        long errorAgentCount,
        long taskCount,
        long runningTaskCount,
        long completedTaskCount,
        long failedTaskCount,
        List<WorkspaceDashboardRecentReportResponse> recentReports,
        List<WorkspaceDashboardRecentLogResponse> recentLogs) {

    static WorkspaceDashboardSummaryResponse from(GetWorkspaceDashboardSummaryResult result) {
        return new WorkspaceDashboardSummaryResponse(
                result.agentCount(),
                result.runningAgentCount(),
                result.idleAgentCount(),
                result.errorAgentCount(),
                result.taskCount(),
                result.runningTaskCount(),
                result.completedTaskCount(),
                result.failedTaskCount(),
                result.recentReports().stream()
                        .map(WorkspaceDashboardRecentReportResponse::from)
                        .toList(),
                result.recentLogs().stream()
                        .map(WorkspaceDashboardRecentLogResponse::from)
                        .toList());
    }
}
