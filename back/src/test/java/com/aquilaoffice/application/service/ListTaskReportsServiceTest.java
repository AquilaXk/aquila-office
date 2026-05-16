package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListTaskReportsQuery;
import com.aquilaoffice.application.port.in.ListTaskReportsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.TaskReportResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListTaskReportsServiceTest {

    @Test
    void listTaskReportsMapsExecutionsForWorkspaceMember() {
        FakeLoadTaskExecutionReportsPort executionReportsPort =
                new FakeLoadTaskExecutionReportsPort(List.of(
                        execution(902L, TaskExecutionStatus.FAILED, null, "Gateway binding not found"),
                        execution(901L, TaskExecutionStatus.SUCCEEDED, "테스트 생성 완료", null)));
        ListTaskReportsUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                Optional.of(taskDetail()),
                executionReportsPort);

        List<TaskReportResult> reports =
                service.listTaskReports(new ListTaskReportsQuery(101L, 301L, " google-1 "));

        assertThat(executionReportsPort.workspaceId).isEqualTo(101L);
        assertThat(executionReportsPort.taskId).isEqualTo(301L);
        assertThat(reports).hasSize(2);
        assertThat(reports.get(0).reportId()).isEqualTo(902L);
        assertThat(reports.get(0).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(reports.get(0).recommendedAction()).isEqualTo("Gateway binding not found");
        assertThat(reports.get(0).artifacts()).isEmpty();
        assertThat(reports.get(1).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(reports.get(1).summary()).isEqualTo("테스트 생성 완료");
    }

    @Test
    void listTaskReportsRejectsRequesterWithoutWorkspaceMembership() {
        FakeLoadTaskExecutionReportsPort executionReportsPort = new FakeLoadTaskExecutionReportsPort(List.of());
        ListTaskReportsUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.empty(),
                Optional.of(taskDetail()),
                executionReportsPort);

        assertThatThrownBy(() -> service.listTaskReports(new ListTaskReportsQuery(101L, 301L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace membership not found");
        assertThat(executionReportsPort.called).isFalse();
    }

    @Test
    void listTaskReportsRejectsMissingTask() {
        FakeLoadTaskExecutionReportsPort executionReportsPort = new FakeLoadTaskExecutionReportsPort(List.of());
        ListTaskReportsUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                Optional.empty(),
                executionReportsPort);

        assertThatThrownBy(() -> service.listTaskReports(new ListTaskReportsQuery(101L, 301L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task not found");
        assertThat(executionReportsPort.called).isFalse();
    }

    private static ListTaskReportsUseCase newService(
            Optional<Member> member,
            Optional<GetWorkspaceDetailResult> workspace,
            Optional<TaskDetail> task,
            LoadTaskExecutionReportsPort executionReportsPort) {
        return new ListTaskReportsService(
                new FakeLoadMemberPort(member),
                new FakeLoadWorkspacePort(workspace),
                new FakeLoadTaskPort(task),
                executionReportsPort);
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

    private static TaskDetail taskDetail() {
        return new TaskDetail(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                "핵심 서비스 테스트를 추가해주세요.",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 11, 0));
    }

    private static SavedTaskExecution execution(
            Long executionId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason) {
        return new SavedTaskExecution(
                executionId,
                101L,
                301L,
                501L,
                status,
                resultSummary,
                failureReason,
                LocalDateTime.of(2026, 5, 16, 12, 0),
                LocalDateTime.of(2026, 5, 16, 12, 30));
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
            throw new UnsupportedOperationException("not used in task report service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return workspace;
        }
    }

    private static class FakeLoadTaskPort implements LoadTaskPort {
        private final Optional<TaskDetail> task;

        private FakeLoadTaskPort(Optional<TaskDetail> task) {
            this.task = task;
        }

        @Override
        public Optional<TaskDetail> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
            return task;
        }
    }

    private static class FakeLoadTaskExecutionReportsPort implements LoadTaskExecutionReportsPort {
        private final List<SavedTaskExecution> executions;
        private boolean called;
        private Long workspaceId;
        private Long taskId;

        private FakeLoadTaskExecutionReportsPort(List<SavedTaskExecution> executions) {
            this.executions = executions;
        }

        @Override
        public List<SavedTaskExecution> findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(
                Long workspaceId,
                Long taskId) {
            this.called = true;
            this.workspaceId = workspaceId;
            this.taskId = taskId;
            return executions;
        }
    }
}
