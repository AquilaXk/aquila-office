package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.TaskSummary;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 멤버 작업 통계 service가 Agent 담당자별 작업 상태 count와 점수를 계산하는지 확인한다.
 */
class ListWorkspaceMemberTaskStatsServiceTest {

    @Test
    void listMemberTaskStatsCalculatesCountsScoresAndRanks() {
        ListWorkspaceMemberTaskStatsService service = service(
                Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER)),
                members(adminMember(1L), memberMember(2L), observerMember(3L)),
                agents(agent(501L, "google-2"), agent(502L, "google-1"), agent(503L, "google-unknown")),
                tasks(
                        task(301L, 501L, TaskStatus.COMPLETED),
                        task(302L, 501L, TaskStatus.COMPLETED),
                        task(303L, 501L, TaskStatus.IN_PROGRESS),
                        task(304L, 501L, TaskStatus.WAITING_USER),
                        task(305L, 502L, TaskStatus.COMPLETED),
                        task(306L, 502L, TaskStatus.FAILED),
                        task(307L, 503L, TaskStatus.COMPLETED),
                        task(308L, null, TaskStatus.COMPLETED)));

        List<ListWorkspaceMemberTaskStatsResult> results =
                service.listMemberTaskStats(new ListWorkspaceMemberTaskStatsQuery(101L, " google-2 "));

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).satisfies(result -> {
            assertThat(result.memberId()).isEqualTo(2L);
            assertThat(result.memberName()).isEqualTo("Member");
            assertThat(result.rank()).isEqualTo(1);
            assertThat(result.taskCount()).isEqualTo(4);
            assertThat(result.completedTaskCount()).isEqualTo(2);
            assertThat(result.runningTaskCount()).isEqualTo(1);
            assertThat(result.failedTaskCount()).isZero();
            assertThat(result.waitingUserTaskCount()).isEqualTo(1);
            assertThat(result.healthScore()).isEqualTo(87);
            assertThat(result.flowScore()).isEqualTo(36);
            assertThat(result.impactScore()).isEqualTo(50);
            assertThat(result.totalScore()).isEqualTo(60);
        });
        assertThat(results.get(1)).satisfies(result -> {
            assertThat(result.memberId()).isEqualTo(1L);
            assertThat(result.rank()).isEqualTo(2);
            assertThat(result.taskCount()).isEqualTo(2);
            assertThat(result.completedTaskCount()).isEqualTo(1);
            assertThat(result.failedTaskCount()).isEqualTo(1);
            assertThat(result.totalScore()).isEqualTo(40);
        });
        assertThat(results.get(2)).satisfies(result -> {
            assertThat(result.memberId()).isEqualTo(3L);
            assertThat(result.rank()).isEqualTo(3);
            assertThat(result.taskCount()).isZero();
            assertThat(result.healthScore()).isEqualTo(100);
            assertThat(result.totalScore()).isEqualTo(35);
        });
    }

    @Test
    void listMemberTaskStatsRejectsMissingJoinedWorkspace() {
        ListWorkspaceMemberTaskStatsService service = service(
                Optional.empty(),
                members(adminMember(1L)),
                List.of(),
                List.of());

        assertThatThrownBy(() -> service.listMemberTaskStats(
                        new ListWorkspaceMemberTaskStatsQuery(101L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace not found");
    }

    @Test
    void queryRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new ListWorkspaceMemberTaskStatsQuery(0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new ListWorkspaceMemberTaskStatsQuery(101L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
    }

    private ListWorkspaceMemberTaskStatsService service(
            Optional<GetWorkspaceDetailResult> workspaceDetail,
            List<ListWorkspaceMembersResult> members,
            List<SavedWorkspaceAgent> agents,
            List<TaskSummary> tasks) {
        return new ListWorkspaceMemberTaskStatsService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(workspaceDetail),
                new FakeLoadWorkspaceMemberPort(members),
                new FakeLoadWorkspaceAgentPort(agents),
                new FakeLoadTasksPort(tasks));
    }

    private static GetWorkspaceDetailResult workspaceDetail(WorkspaceMemberRole role) {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                role,
                LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    private static ListWorkspaceMembersResult adminMember(Long memberId) {
        return new ListWorkspaceMembersResult(
                memberId,
                "google-" + memberId,
                "admin" + memberId + "@example.com",
                "Admin",
                WorkspaceMemberRole.ADMIN,
                LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    private static ListWorkspaceMembersResult memberMember(Long memberId) {
        return new ListWorkspaceMembersResult(
                memberId,
                "google-" + memberId,
                "member" + memberId + "@example.com",
                "Member",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 15, 11, 0));
    }

    private static ListWorkspaceMembersResult observerMember(Long memberId) {
        return new ListWorkspaceMembersResult(
                memberId,
                "google-" + memberId,
                "observer" + memberId + "@example.com",
                "Observer",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 15, 12, 0));
    }

    private static List<ListWorkspaceMembersResult> members(ListWorkspaceMembersResult... members) {
        return List.of(members);
    }

    private static List<TaskSummary> tasks(TaskSummary... tasks) {
        return List.of(tasks);
    }

    private static List<SavedWorkspaceAgent> agents(SavedWorkspaceAgent... agents) {
        return List.of(agents);
    }

    private static SavedWorkspaceAgent agent(Long agentId, String createdByGoogleSub) {
        return new SavedWorkspaceAgent(
                agentId,
                101L,
                "Agent " + agentId,
                WorkspaceAgentCategory.BACKEND,
                "open-claw-" + agentId,
                ".aquila/workspaces/101",
                WorkspaceAgentStatus.READY,
                null,
                createdByGoogleSub,
                LocalDateTime.of(2026, 5, 15, 9, 0),
                LocalDateTime.of(2026, 5, 15, 9, 0),
                List.of());
    }

    private static TaskSummary task(Long taskId, Long assignedAgentId, TaskStatus status) {
        return new TaskSummary(
                taskId,
                101L,
                "google-1",
                "Task " + taskId,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.MEDIUM,
                assignedAgentId,
                status,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                LocalDateTime.of(2026, 5, 15, 10, 0));
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
        private final Optional<GetWorkspaceDetailResult> detailResult;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in member task stats service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            return detailResult;
        }
    }

    private static class FakeLoadWorkspaceMemberPort implements LoadWorkspaceMemberPort {
        private final List<ListWorkspaceMembersResult> members;

        private FakeLoadWorkspaceMemberPort(List<ListWorkspaceMembersResult> members) {
            this.members = members;
        }

        @Override
        public List<ListWorkspaceMembersResult> findActiveWorkspaceMembersByWorkspaceId(Long workspaceId) {
            return members;
        }
    }

    private static class FakeLoadWorkspaceAgentPort implements LoadWorkspaceAgentPort {
        private final List<SavedWorkspaceAgent> agents;

        private FakeLoadWorkspaceAgentPort(List<SavedWorkspaceAgent> agents) {
            this.agents = agents;
        }

        @Override
        public List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId) {
            return agents;
        }

        @Override
        public Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(Long workspaceId, Long agentId) {
            throw new UnsupportedOperationException("not used in member task stats service test");
        }
    }

    private static class FakeLoadTasksPort implements LoadTasksPort {
        private final List<TaskSummary> tasks;

        private FakeLoadTasksPort(List<TaskSummary> tasks) {
            this.tasks = tasks;
        }

        @Override
        public List<TaskSummary> findByWorkspaceId(Long workspaceId) {
            return tasks;
        }
    }
}
