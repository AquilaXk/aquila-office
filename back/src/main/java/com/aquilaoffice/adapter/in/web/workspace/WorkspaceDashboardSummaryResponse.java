package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "워크스페이스 대시보드 요약 응답")
public record WorkspaceDashboardSummaryResponse(
        long agentCount,
        long runningAgentCount,
        long idleAgentCount,
        long errorAgentCount,
        long taskCount,
        long runningTaskCount,
        long completedTaskCount,
        long failedTaskCount,
        @Schema(description = "일일 토큰 예산 상한 (null이면 무제한)", nullable = true)
        Integer dailyTokenLimit,
        long todayUsedTokens,
        long waitingApprovalTaskCount,
        @Schema(description = "평균 실행 소요 시간(ms)", nullable = true)
        Long avgExecutionDurationMs,
        List<WorkspaceDashboardRecentReportResponse> recentReports,
        List<WorkspaceDashboardRecentLogResponse> recentLogs) {

    public WorkspaceDashboardSummaryResponse(
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
                result.dailyTokenLimit(),
                result.todayUsedTokens(),
                result.waitingApprovalTaskCount(),
                result.avgExecutionDurationMs(),
                result.recentReports().stream()
                        .map(WorkspaceDashboardRecentReportResponse::from)
                        .toList(),
                result.recentLogs().stream()
                        .map(WorkspaceDashboardRecentLogResponse::from)
                        .toList());
    }
}
