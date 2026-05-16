package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskExecutionStatusUpdate;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
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
 * RunTaskService가 Task, Agent, Gateway binding을 검증하고 실행 결과를 상태로 반영하는지 확인한다.
 */
class RunTaskServiceTest {

    @Test
    void runTaskExecutesAssignedReadyAgentAndCompletesTask() {
        FakeUpdateTaskExecutionPort updateExecutionPort = new FakeUpdateTaskExecutionPort();
        FakeUpdateTaskStatusPort updateTaskStatusPort = new FakeUpdateTaskStatusPort();
        FakeRunWorkspaceAgentPort runPort = new FakeRunWorkspaceAgentPort(
                new WorkspaceAgentRunResult(true, "실행 완료", null));
        RunTaskService service = newService(
                taskDetail(TaskStatus.ASSIGNED, 501L),
                List.of(savedAgent(501L, WorkspaceAgentStatus.READY, "gateway-agent-1")),
                Optional.of(gatewayBinding()),
                runPort,
                new FakeSaveTaskExecutionPort(),
                updateExecutionPort,
                updateTaskStatusPort,
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, " google-1 ", false));

        assertThat(runPort.command.openClawAgentId()).isEqualTo("gateway-agent-1");
        assertThat(runPort.command.prompt()).isEqualTo("핵심 서비스 테스트를 추가해주세요.");
        assertThat(updateExecutionPort.updates).extracting(TaskExecutionStatusUpdate::status)
                .containsExactly(TaskExecutionStatus.RUNNING, TaskExecutionStatus.SUCCEEDED);
        assertThat(updateTaskStatusPort.updates).extracting(TaskStatusUpdate::status)
                .containsExactly(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED);
        assertThat(result.taskStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(result.executionStatus()).isEqualTo(TaskExecutionStatus.SUCCEEDED);
        assertThat(result.finalText()).isEqualTo("실행 완료");
    }

    @Test
    void runTaskFailsExecutionWhenGatewayBindingIsMissing() {
        FakeUpdateTaskExecutionPort updateExecutionPort = new FakeUpdateTaskExecutionPort();
        FakeUpdateTaskStatusPort updateTaskStatusPort = new FakeUpdateTaskStatusPort();
        FakeRunWorkspaceAgentPort runPort = new FakeRunWorkspaceAgentPort(
                new WorkspaceAgentRunResult(true, "unused", null));
        RunTaskService service = newService(
                taskDetail(TaskStatus.ASSIGNED, 501L),
                List.of(savedAgent(501L, WorkspaceAgentStatus.READY, "gateway-agent-1")),
                Optional.empty(),
                runPort,
                new FakeSaveTaskExecutionPort(),
                updateExecutionPort,
                updateTaskStatusPort,
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, "google-1", false));

        assertThat(runPort.called).isFalse();
        assertThat(updateExecutionPort.updates).extracting(TaskExecutionStatusUpdate::status)
                .containsExactly(TaskExecutionStatus.FAILED);
        assertThat(updateTaskStatusPort.updates).extracting(TaskStatusUpdate::status)
                .containsExactly(TaskStatus.FAILED);
        assertThat(result.taskStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("workspace gateway binding not found");
    }

    @Test
    void runTaskRejectsAssignedAgentThatIsNotReady() {
        RunTaskService service = newService(
                taskDetail(TaskStatus.ASSIGNED, 501L),
                List.of(savedAgent(501L, WorkspaceAgentStatus.DISABLED, "gateway-agent-1")),
                Optional.of(gatewayBinding()),
                new FakeRunWorkspaceAgentPort(new WorkspaceAgentRunResult(true, "unused", null)),
                new FakeSaveTaskExecutionPort(),
                new FakeUpdateTaskExecutionPort(),
                new FakeUpdateTaskStatusPort(),
                WorkspaceMemberRole.MEMBER);

        assertThatThrownBy(() -> service.runTask(new RunTaskCommand(101L, 301L, "google-1", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ready workspace agent not found");
    }

    @Test
    void runTaskSelectsFirstReadyAgentWhenTaskHasNoAssignedAgent() {
        FakeRunWorkspaceAgentPort runPort = new FakeRunWorkspaceAgentPort(
                new WorkspaceAgentRunResult(true, "실행 완료", null));
        RunTaskService service = newService(
                taskDetail(TaskStatus.REQUESTED, null),
                List.of(
                        savedAgent(501L, WorkspaceAgentStatus.SYNC_FAILED, "gateway-agent-1"),
                        savedAgent(502L, WorkspaceAgentStatus.READY, "gateway-agent-2")),
                Optional.of(gatewayBinding()),
                runPort,
                new FakeSaveTaskExecutionPort(),
                new FakeUpdateTaskExecutionPort(),
                new FakeUpdateTaskStatusPort(),
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, "google-1", true));

        assertThat(runPort.command.agentId()).isEqualTo(502L);
        assertThat(runPort.command.openClawAgentId()).isEqualTo("gateway-agent-2");
        assertThat(runPort.command.createPr()).isTrue();
        assertThat(result.agentId()).isEqualTo(502L);
    }

    private static RunTaskService newService(
            TaskDetail task,
            List<SavedWorkspaceAgent> agents,
            Optional<WorkspaceGatewayBinding> binding,
            RunWorkspaceAgentPort runPort,
            SaveTaskExecutionPort saveExecutionPort,
            UpdateTaskExecutionPort updateExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            WorkspaceMemberRole role) {
        return new RunTaskService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(role))),
                new FakeLoadTaskPort(Optional.of(task)),
                new FakeLoadWorkspaceAgentPort(agents),
                new FakeLoadWorkspaceGatewayBindingPort(binding),
                saveExecutionPort,
                updateExecutionPort,
                updateTaskStatusPort,
                runPort);
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
                status,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static SavedWorkspaceAgent savedAgent(Long agentId, WorkspaceAgentStatus status, String openClawAgentId) {
        return new SavedWorkspaceAgent(
                agentId,
                101L,
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                openClawAgentId,
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

    private static WorkspaceGatewayBinding gatewayBinding() {
        return WorkspaceGatewayBinding.restore(
                701L,
                101L,
                "https://gateway.example.com",
                "gateway-token-secret",
                "google-1");
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
            throw new UnsupportedOperationException("not used in run task service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detail;
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
            return agents.stream().filter(agent -> agent.agentId().equals(agentId)).findFirst();
        }
    }

    private static class FakeLoadWorkspaceGatewayBindingPort implements LoadWorkspaceGatewayBindingPort {
        private final Optional<WorkspaceGatewayBinding> binding;

        private FakeLoadWorkspaceGatewayBindingPort(Optional<WorkspaceGatewayBinding> binding) {
            this.binding = binding;
        }

        @Override
        public Optional<WorkspaceGatewayBinding> findActiveByWorkspaceId(Long workspaceId) {
            return binding;
        }
    }

    private static class FakeSaveTaskExecutionPort implements SaveTaskExecutionPort {

        @Override
        public SavedTaskExecution save(TaskExecution execution) {
            return new SavedTaskExecution(
                    801L,
                    execution.getWorkspaceId(),
                    execution.getTaskId(),
                    execution.getAgentId(),
                    execution.getStatus(),
                    execution.getResultSummary(),
                    execution.getFailureReason(),
                    LocalDateTime.of(2026, 5, 16, 10, 0),
                    LocalDateTime.of(2026, 5, 16, 10, 0));
        }
    }

    private static class FakeUpdateTaskExecutionPort implements UpdateTaskExecutionPort {
        private final List<TaskExecutionStatusUpdate> updates = new java.util.ArrayList<>();

        @Override
        public SavedTaskExecution updateStatus(TaskExecutionStatusUpdate update) {
            updates.add(update);
            return new SavedTaskExecution(
                    update.executionId(),
                    update.workspaceId(),
                    update.taskId(),
                    501L,
                    update.status(),
                    update.resultSummary(),
                    update.failureReason(),
                    LocalDateTime.of(2026, 5, 16, 10, 0),
                    LocalDateTime.of(2026, 5, 16, 10, 1));
        }
    }

    private static class FakeUpdateTaskStatusPort implements UpdateTaskStatusPort {
        private final List<TaskStatusUpdate> updates = new java.util.ArrayList<>();

        @Override
        public TaskDetail updateStatus(TaskStatusUpdate update) {
            updates.add(update);
            return taskDetail(update.status(), 501L);
        }
    }

    private static class FakeRunWorkspaceAgentPort implements RunWorkspaceAgentPort {
        private final WorkspaceAgentRunResult result;
        private WorkspaceAgentRunCommand command;
        private boolean called;

        private FakeRunWorkspaceAgentPort(WorkspaceAgentRunResult result) {
            this.result = result;
        }

        @Override
        public WorkspaceAgentRunResult run(WorkspaceAgentRunCommand command) {
            this.called = true;
            this.command = command;
            return result;
        }
    }
}
