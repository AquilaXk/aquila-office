package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceDashboardSummaryPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceDashboardRecentLogData;
import com.aquilaoffice.application.port.out.WorkspaceDashboardRecentReportData;
import com.aquilaoffice.application.port.out.WorkspaceDashboardSummaryData;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetWorkspaceDashboardSummaryServiceTest {

    @Test
    void getSummaryMapsCountsAndRecentReportsForWorkspaceMember() {
        FakeLoadWorkspaceDashboardSummaryPort dashboardPort =
                new FakeLoadWorkspaceDashboardSummaryPort(summaryData());
        GetWorkspaceDashboardSummaryUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                dashboardPort);

        GetWorkspaceDashboardSummaryResult result =
                service.getSummary(new GetWorkspaceDashboardSummaryQuery(101L, " google-1 "));

        assertThat(dashboardPort.workspaceId).isEqualTo(101L);
        assertThat(result.agentCount()).isEqualTo(4L);
        assertThat(result.runningAgentCount()).isEqualTo(1L);
        assertThat(result.idleAgentCount()).isEqualTo(2L);
        assertThat(result.errorAgentCount()).isEqualTo(1L);
        assertThat(result.taskCount()).isEqualTo(10L);
        assertThat(result.runningTaskCount()).isEqualTo(3L);
        assertThat(result.completedTaskCount()).isEqualTo(6L);
        assertThat(result.failedTaskCount()).isEqualTo(1L);
        assertThat(result.recentReports()).hasSize(1);
        assertThat(result.recentReports().getFirst().executionId()).isEqualTo(900L);
        assertThat(result.recentReports().getFirst().status()).isEqualTo(TaskExecutionStatus.SUCCEEDED);
        assertThat(result.recentReports().getFirst().summary()).isEqualTo("작업 완료");
        assertThat(result.recentLogs()).isEmpty();
    }

    @Test
    void getSummaryRejectsRequesterWithoutWorkspaceMembership() {
        FakeLoadWorkspaceDashboardSummaryPort dashboardPort =
                new FakeLoadWorkspaceDashboardSummaryPort(summaryData());
        GetWorkspaceDashboardSummaryUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.empty(),
                dashboardPort);

        assertThatThrownBy(() ->
                        service.getSummary(new GetWorkspaceDashboardSummaryQuery(101L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace membership not found");
        assertThat(dashboardPort.called).isFalse();
    }

    private static GetWorkspaceDashboardSummaryUseCase newService(
            Optional<Member> member,
            Optional<GetWorkspaceDetailResult> workspace,
            LoadWorkspaceDashboardSummaryPort dashboardPort) {
        return new GetWorkspaceDashboardSummaryService(
                new FakeLoadMemberPort(member),
                new FakeLoadWorkspacePort(workspace),
                dashboardPort);
    }

    private static GetWorkspaceDetailResult workspaceDetail() {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static WorkspaceDashboardSummaryData summaryData() {
        return new WorkspaceDashboardSummaryData(
                4L,
                1L,
                2L,
                1L,
                10L,
                3L,
                6L,
                1L,
                List.of(new WorkspaceDashboardRecentReportData(
                        900L,
                        301L,
                        501L,
                        TaskExecutionStatus.SUCCEEDED,
                        "작업 완료",
                        null,
                        LocalDateTime.of(2026, 5, 16, 12, 0))),
                List.<WorkspaceDashboardRecentLogData>of());
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> workspace;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> workspace) {
            this.workspace = workspace;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in dashboard summary service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return workspace;
        }
    }

    private static class FakeLoadWorkspaceDashboardSummaryPort implements LoadWorkspaceDashboardSummaryPort {
        private final WorkspaceDashboardSummaryData summary;
        private boolean called;
        private Long workspaceId;

        private FakeLoadWorkspaceDashboardSummaryPort(WorkspaceDashboardSummaryData summary) {
            this.summary = summary;
        }

        @Override
        public WorkspaceDashboardSummaryData loadSummary(Long workspaceId) {
            this.called = true;
            this.workspaceId = workspaceId;
            return summary;
        }
    }
}
