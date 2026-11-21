package com.aquilaoffice.application.port.in;

import java.util.List;

public record GetWorkspaceDashboardSummaryResult(
        long agentCount,
        long runningAgentCount,
        long idleAgentCount,
        long errorAgentCount,
        long taskCount,
        long runningTaskCount,
        long completedTaskCount,
        long failedTaskCount,
        Integer dailyTokenLimit,
        long todayUsedTokens,
        long waitingApprovalTaskCount,
        Long avgExecutionDurationMs,
        List<WorkspaceDashboardRecentReportResult> recentReports,
        List<WorkspaceDashboardRecentLogResult> recentLogs) {

    public GetWorkspaceDashboardSummaryResult(
            long agentCount,
            long runningAgentCount,
            long idleAgentCount,
            long errorAgentCount,
            long taskCount,
            long runningTaskCount,
            long completedTaskCount,
            long failedTaskCount,
            List<WorkspaceDashboardRecentReportResult> recentReports,
            List<WorkspaceDashboardRecentLogResult> recentLogs) {
        this(
                agentCount,
                runningAgentCount,
                idleAgentCount,
                errorAgentCount,
                taskCount,
                runningTaskCount,
                completedTaskCount,
                failedTaskCount,
                null,
                0L,
                0L,
                null,
                recentReports,
                recentLogs);
    }

    public GetWorkspaceDashboardSummaryResult {
        recentReports = recentReports == null ? List.of() : List.copyOf(recentReports);
        recentLogs = recentLogs == null ? List.of() : List.copyOf(recentLogs);
    }
}
