package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateTaskCommand;
import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.GetTaskDetailQuery;
import com.aquilaoffice.application.port.in.GetTaskDetailResult;
import com.aquilaoffice.application.port.in.ListTasksQuery;
import com.aquilaoffice.application.port.in.ListTasksResult;
import com.aquilaoffice.application.port.in.UpdateTaskStatusCommand;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SavedTask;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.TaskSummary;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Task application service가 워크스페이스 멤버십과 담당 Agent 상태를 검증하는지 확인한다.
 */
class TaskManagementServiceTest {

    @Test
    void createTaskSavesAssignedTaskWhenAgentIsReadyInWorkspace() {
        FakeSaveTaskPort saveTaskPort = new FakeSaveTaskPort(savedTask(TaskStatus.ASSIGNED, 501L));
        CreateTaskService service = new CreateTaskService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadWorkspaceAgentPort(Optional.of(savedAgent(WorkspaceAgentStatus.READY))),
                saveTaskPort);

        CreateTaskResult result = service.createTask(new CreateTaskCommand(
                101L,
                " google-1 ",
                " 테스트 작성 ",
                " 핵심 서비스 테스트를 추가해주세요. ",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));

        assertThat(saveTaskPort.savedTask.getWorkspaceId()).isEqualTo(101L);
        assertThat(saveTaskPort.savedTask.getTitle()).isEqualTo("테스트 작성");
        assertThat(saveTaskPort.savedTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(result.taskId()).isEqualTo(301L);
        assertThat(result.assignedAgentId()).isEqualTo(501L);
        assertThat(result.status()).isEqualTo(TaskStatus.ASSIGNED);
    }

    @Test
    void createTaskRejectsDisabledAgent() {
        CreateTaskService service = new CreateTaskService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadWorkspaceAgentPort(Optional.of(savedAgent(WorkspaceAgentStatus.DISABLED))),
                new FakeSaveTaskPort(savedTask(TaskStatus.ASSIGNED, 501L)));

        assertThatThrownBy(() -> service.createTask(new CreateTaskCommand(
                        101L,
                        "google-1",
                        "테스트 작성",
                        null,
                        TaskType.TEST_CREATION,
                        TaskPriority.HIGH,
                        501L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ready workspace agent not found");
    }

    @Test
    void listTasksRequiresJoinedWorkspace() {
        ListTasksService service = new ListTasksService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadTasksPort(List.of(taskSummary())));

        List<ListTasksResult> results = service.listTasks(new ListTasksQuery(101L, " google-1 "));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.taskId()).isEqualTo(301L);
            assertThat(result.title()).isEqualTo("테스트 작성");
            assertThat(result.status()).isEqualTo(TaskStatus.ASSIGNED);
        });
    }

    @Test
    void getTaskDetailReturnsWorkspaceScopedTask() {
        GetTaskDetailService service = new GetTaskDetailService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadTaskPort(Optional.of(taskDetail(TaskStatus.ASSIGNED, 501L))));

        GetTaskDetailResult result = service.getTaskDetail(new GetTaskDetailQuery(101L, 301L, " google-1 "));

        assertThat(result.taskId()).isEqualTo(301L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.assignedAgentId()).isEqualTo(501L);
    }

    @Test
    void updateTaskStatusRequiresAdminAndValidTransition() {
        FakeUpdateTaskStatusPort updatePort = new FakeUpdateTaskStatusPort(taskDetail(TaskStatus.IN_PROGRESS, 501L));
        UpdateTaskStatusService service = new UpdateTaskStatusService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                new FakeLoadTaskPort(Optional.of(taskDetail(TaskStatus.ASSIGNED, 501L))),
                updatePort);

        UpdateTaskStatusResult result = service.updateTaskStatus(new UpdateTaskStatusCommand(
                101L,
                301L,
                " google-1 ",
                TaskStatus.IN_PROGRESS));

        assertThat(updatePort.update.workspaceId()).isEqualTo(101L);
        assertThat(updatePort.update.taskId()).isEqualTo(301L);
        assertThat(updatePort.update.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateTaskStatusRejectsMemberRole() {
        UpdateTaskStatusService service = new UpdateTaskStatusService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadTaskPort(Optional.of(taskDetail(TaskStatus.ASSIGNED, 501L))),
                new FakeUpdateTaskStatusPort(taskDetail(TaskStatus.IN_PROGRESS, 501L)));

        assertThatThrownBy(() -> service.updateTaskStatus(new UpdateTaskStatusCommand(
                        101L,
                        301L,
                        "google-2",
                        TaskStatus.IN_PROGRESS)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
    }

    private static GetWorkspaceDetailResult workspaceDetail(WorkspaceMemberRole role) {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                role,
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static SavedTask savedTask(TaskStatus status, Long assignedAgentId) {
        return new SavedTask(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                "핵심 서비스 테스트를 추가해주세요.",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                assignedAgentId,
                null,
                status,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static TaskSummary taskSummary() {
        return new TaskSummary(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.ASSIGNED,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static TaskDetail taskDetail(TaskStatus status, Long assignedAgentId) {
        return new TaskDetail(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                "핵심 서비스 테스트를 추가해주세요.",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                assignedAgentId,
                null,
                status,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static SavedWorkspaceAgent savedAgent(WorkspaceAgentStatus status) {
        return new SavedWorkspaceAgent(
                501L,
                101L,
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                "gateway-agent-1",
                ".aquila/workspaces/101",
                status,
                null,
                "google-1",
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0),
                List.of(new SavedWorkspaceAgentSkillFile(
                        601L,
                        "AGENTS.md",
                        WorkspaceAgentSkillFileSyncStatus.SYNCED,
                        null)));
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
        private final Optional<GetWorkspaceDetailResult> detail;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detail) {
            this.detail = detail;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in task service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detail;
        }
    }

    private static class FakeLoadWorkspaceAgentPort implements LoadWorkspaceAgentPort {
        private final Optional<SavedWorkspaceAgent> agent;

        private FakeLoadWorkspaceAgentPort(Optional<SavedWorkspaceAgent> agent) {
            this.agent = agent;
        }

        @Override
        public List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId) {
            throw new UnsupportedOperationException("not used in task service test");
        }

        @Override
        public Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(Long workspaceId, Long agentId) {
            return agent;
        }
    }

    private static class FakeSaveTaskPort implements SaveTaskPort {
        private final SavedTask result;
        private Task savedTask;

        private FakeSaveTaskPort(SavedTask result) {
            this.result = result;
        }

        @Override
        public SavedTask save(Task task) {
            this.savedTask = task;
            return result;
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

    private static class FakeLoadTaskPort implements LoadTaskPort {
        private final Optional<TaskDetail> detail;

        private FakeLoadTaskPort(Optional<TaskDetail> detail) {
            this.detail = detail;
        }

        @Override
        public Optional<TaskDetail> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
            return detail;
        }
    }

    private static class FakeUpdateTaskStatusPort implements UpdateTaskStatusPort {
        private final TaskDetail result;
        private TaskStatusUpdate update;

        private FakeUpdateTaskStatusPort(TaskDetail result) {
            this.result = result;
        }

        @Override
        public TaskDetail updateStatus(TaskStatusUpdate update) {
            this.update = update;
            return result;
        }
    }
}
