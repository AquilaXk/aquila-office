package com.aquilaoffice.application.port.out;

import java.util.List;

public record WorkspaceDashboardSummaryData(
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
        List<WorkspaceDashboardRecentReportData> recentReports,
        List<WorkspaceDashboardRecentLogData> recentLogs) {

    public WorkspaceDashboardSummaryData(
            long agentCount,
            long runningAgentCount,
            long idleAgentCount,
            long errorAgentCount,
            long taskCount,
            long runningTaskCount,
            long completedTaskCount,
            long failedTaskCount,
            List<WorkspaceDashboardRecentReportData> recentReports,
            List<WorkspaceDashboardRecentLogData> recentLogs) {
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

    public WorkspaceDashboardSummaryData {
        recentReports = recentReports == null ? List.of() : List.copyOf(recentReports);
        recentLogs = recentLogs == null ? List.of() : List.copyOf(recentLogs);
    }
}
