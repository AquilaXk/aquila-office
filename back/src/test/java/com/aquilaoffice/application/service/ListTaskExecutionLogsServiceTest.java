package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListTaskExecutionLogsQuery;
import com.aquilaoffice.application.port.in.ListTaskExecutionLogsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.TaskExecutionLogResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionLogsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedTaskExecutionLog;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListTaskExecutionLogsServiceTest {

    @Test
    void listTaskExecutionLogsMapsLogsForWorkspaceMember() {
        FakeLoadTaskExecutionLogsPort logsPort = new FakeLoadTaskExecutionLogsPort(List.of(log(801L, "INFO")));
        ListTaskExecutionLogsUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                Optional.of(taskDetail()),
                logsPort);

        List<TaskExecutionLogResult> logs =
                service.listTaskExecutionLogs(new ListTaskExecutionLogsQuery(101L, 301L, " google-1 "));

        assertThat(logsPort.workspaceId).isEqualTo(101L);
        assertThat(logsPort.taskId).isEqualTo(301L);
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().logId()).isEqualTo(801L);
        assertThat(logs.getFirst().taskExecutionId()).isEqualTo(701L);
        assertThat(logs.getFirst().level()).isEqualTo("INFO");
    }

    @Test
    void listTaskExecutionLogsRejectsMissingTask() {
        FakeLoadTaskExecutionLogsPort logsPort = new FakeLoadTaskExecutionLogsPort(List.of());
        ListTaskExecutionLogsUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                Optional.empty(),
                logsPort);

        assertThatThrownBy(() -> service.listTaskExecutionLogs(new ListTaskExecutionLogsQuery(101L, 301L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task not found");
        assertThat(logsPort.called).isFalse();
    }

    private static ListTaskExecutionLogsUseCase newService(
            Optional<Member> member,
            Optional<GetWorkspaceDetailResult> workspace,
            Optional<TaskDetail> task,
            LoadTaskExecutionLogsPort logsPort) {
        return new ListTaskExecutionLogsService(
                new FakeLoadMemberPort(member),
                new FakeLoadWorkspacePort(workspace),
                new FakeLoadTaskPort(task),
                logsPort);
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
                null,
                TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 11, 0));
    }

    private static SavedTaskExecutionLog log(Long logId, String level) {
        return new SavedTaskExecutionLog(
                logId,
                101L,
                301L,
                701L,
                501L,
                level,
                "작업 실행 시작",
                LocalDateTime.of(2026, 5, 16, 12, 0));
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
            throw new UnsupportedOperationException("not used in task execution log service test");
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

    private static class FakeLoadTaskExecutionLogsPort implements LoadTaskExecutionLogsPort {
        private final List<SavedTaskExecutionLog> logs;
        private boolean called;
        private Long workspaceId;
        private Long taskId;

        private FakeLoadTaskExecutionLogsPort(List<SavedTaskExecutionLog> logs) {
            this.logs = logs;
        }

        @Override
        public List<SavedTaskExecutionLog> findByWorkspaceIdAndTaskIdOrderByCreatedAtAsc(
                Long workspaceId,
                Long taskId) {
            this.called = true;
            this.workspaceId = workspaceId;
            this.taskId = taskId;
            return logs;
        }
    }
}
