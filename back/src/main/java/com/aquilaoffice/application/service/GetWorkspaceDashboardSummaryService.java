package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryUseCase;
import com.aquilaoffice.application.port.in.WorkspaceDashboardRecentLogResult;
import com.aquilaoffice.application.port.in.WorkspaceDashboardRecentReportResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceDashboardSummaryPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceDashboardRecentLogData;
import com.aquilaoffice.application.port.out.WorkspaceDashboardRecentReportData;
import com.aquilaoffice.application.port.out.WorkspaceDashboardSummaryData;
import java.util.Objects;

/**
 * 워크스페이스 대시보드 요약 조회 유스케이스를 수행하는 application service다.
 */
public class GetWorkspaceDashboardSummaryService implements GetWorkspaceDashboardSummaryUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceDashboardSummaryPort loadWorkspaceDashboardSummaryPort;

    public GetWorkspaceDashboardSummaryService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceDashboardSummaryPort loadWorkspaceDashboardSummaryPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceDashboardSummaryPort = Objects.requireNonNull(
                loadWorkspaceDashboardSummaryPort,
                "loadWorkspaceDashboardSummaryPort must not be null");
    }

    @Override
    public GetWorkspaceDashboardSummaryResult getSummary(GetWorkspaceDashboardSummaryQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String memberGoogleSub = normalizeMemberGoogleSub(query.memberGoogleSub());

        loadMemberPort
                .findByGoogleSub(memberGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(query.workspaceId(), memberGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));

        WorkspaceDashboardSummaryData summary = loadWorkspaceDashboardSummaryPort.loadSummary(query.workspaceId());
        return toResult(summary);
    }

    private static String normalizeMemberGoogleSub(String memberGoogleSub) {
        if (memberGoogleSub == null || memberGoogleSub.isBlank()) {
            throw new IllegalArgumentException("member google sub must not be blank");
        }
        return memberGoogleSub.trim();
    }

    private static GetWorkspaceDashboardSummaryResult toResult(WorkspaceDashboardSummaryData summary) {
        return new GetWorkspaceDashboardSummaryResult(
                summary.agentCount(),
                summary.runningAgentCount(),
                summary.idleAgentCount(),
                summary.errorAgentCount(),
                summary.taskCount(),
                summary.runningTaskCount(),
                summary.completedTaskCount(),
                summary.failedTaskCount(),
                summary.recentReports().stream()
                        .map(GetWorkspaceDashboardSummaryService::toReportResult)
                        .toList(),
                summary.recentLogs().stream()
                        .map(GetWorkspaceDashboardSummaryService::toLogResult)
                        .toList());
    }

    private static WorkspaceDashboardRecentReportResult toReportResult(WorkspaceDashboardRecentReportData report) {
        return new WorkspaceDashboardRecentReportResult(
                report.executionId(),
                report.taskId(),
                report.agentId(),
                report.status(),
                report.summary(),
                report.failureReason(),
                report.createdAt());
    }

    private static WorkspaceDashboardRecentLogResult toLogResult(WorkspaceDashboardRecentLogData log) {
        return new WorkspaceDashboardRecentLogResult(
                log.executionId(),
                log.taskId(),
                log.agentId(),
                log.level(),
                log.message(),
                log.createdAt());
    }
}
