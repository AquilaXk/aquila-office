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
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.SavedTaskExecutionLog;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskExecutionStatusUpdate;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLock;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockCommand;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockPort;
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
        FakeSaveTaskExecutionLogPort saveLogPort = new FakeSaveTaskExecutionLogPort();
        RecordingWorkflowMetrics workflowMetrics = new RecordingWorkflowMetrics();
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
                saveLogPort,
                workflowMetrics,
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, " google-1 ", false));

        assertThat(runPort.command.openClawAgentId()).isEqualTo("gateway-agent-1");
        assertThat(runPort.command.prompt()).isEqualTo("핵심 서비스 테스트를 추가해주세요.");
        assertThat(updateExecutionPort.updates).extracting(TaskExecutionStatusUpdate::status)
                .containsExactly(TaskExecutionStatus.RUNNING, TaskExecutionStatus.SUCCEEDED);
        assertThat(updateTaskStatusPort.updates).extracting(TaskStatusUpdate::status)
                .containsExactly(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED);
        assertThat(saveLogPort.logs).extracting(TaskExecutionLogCommand::level)
                .containsExactly("INFO", "INFO", "INFO");
        assertThat(saveLogPort.logs).extracting(TaskExecutionLogCommand::message)
                .containsExactly(
                        "Task execution queued",
                        "Task execution running",
                        "Task execution succeeded");
        assertThat(result.taskStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(result.executionStatus()).isEqualTo(TaskExecutionStatus.SUCCEEDED);
        assertThat(result.finalText()).isEqualTo("실행 완료");
        assertThat(workflowMetrics.taskStarted).isEqualTo(1);
        assertThat(workflowMetrics.taskSuccess).isEqualTo(1);
        assertThat(workflowMetrics.taskFailure).isZero();
    }

    @Test
    void runTaskFailsExecutionWhenGatewayBindingIsMissing() {
        FakeUpdateTaskExecutionPort updateExecutionPort = new FakeUpdateTaskExecutionPort();
        FakeUpdateTaskStatusPort updateTaskStatusPort = new FakeUpdateTaskStatusPort();
        RecordingWorkflowMetrics workflowMetrics = new RecordingWorkflowMetrics();
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
                new FakeSaveTaskExecutionLogPort(),
                workflowMetrics,
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, "google-1", false));

        assertThat(runPort.called).isFalse();
        assertThat(updateExecutionPort.updates).extracting(TaskExecutionStatusUpdate::status)
                .containsExactly(TaskExecutionStatus.FAILED);
        assertThat(updateTaskStatusPort.updates).extracting(TaskStatusUpdate::status)
                .containsExactly(TaskStatus.FAILED);
        assertThat(result.taskStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("workspace gateway binding not found");
        assertThat(workflowMetrics.taskStarted).isEqualTo(1);
        assertThat(workflowMetrics.taskSuccess).isZero();
        assertThat(workflowMetrics.taskFailure).isEqualTo(1);
    }

    @Test
    void runTaskStoresFailureLogWhenGatewayReturnsFailure() {
        FakeUpdateTaskExecutionPort updateExecutionPort = new FakeUpdateTaskExecutionPort();
        FakeUpdateTaskStatusPort updateTaskStatusPort = new FakeUpdateTaskStatusPort();
        FakeSaveTaskExecutionLogPort saveLogPort = new FakeSaveTaskExecutionLogPort();
        RunTaskService service = newService(
                taskDetail(TaskStatus.ASSIGNED, 501L),
                List.of(savedAgent(501L, WorkspaceAgentStatus.READY, "gateway-agent-1")),
                Optional.of(gatewayBinding()),
                new FakeRunWorkspaceAgentPort(new WorkspaceAgentRunResult(false, null, "agent rejected")),
                new FakeSaveTaskExecutionPort(),
                updateExecutionPort,
                updateTaskStatusPort,
                saveLogPort,
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, "google-1", false));

        assertThat(updateExecutionPort.updates).extracting(TaskExecutionStatusUpdate::status)
                .containsExactly(TaskExecutionStatus.RUNNING, TaskExecutionStatus.FAILED);
        assertThat(saveLogPort.logs).extracting(TaskExecutionLogCommand::level)
                .containsExactly("INFO", "INFO", "ERROR");
        assertThat(saveLogPort.logs.getLast().message()).isEqualTo("agent rejected");
        assertThat(result.taskStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(result.executionStatus()).isEqualTo(TaskExecutionStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("agent rejected");
    }

    @Test
    void runTaskStoresFailureLogWhenGatewayThrowsException() {
        FakeSaveTaskExecutionLogPort saveLogPort = new FakeSaveTaskExecutionLogPort();
        RunTaskService service = newService(
                taskDetail(TaskStatus.ASSIGNED, 501L),
                List.of(savedAgent(501L, WorkspaceAgentStatus.READY, "gateway-agent-1")),
                Optional.of(gatewayBinding()),
                new FakeRunWorkspaceAgentPort(new IllegalStateException("gateway timeout")),
                new FakeSaveTaskExecutionPort(),
                new FakeUpdateTaskExecutionPort(),
                new FakeUpdateTaskStatusPort(),
                saveLogPort,
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, "google-1", false));

        assertThat(saveLogPort.logs).extracting(TaskExecutionLogCommand::level)
                .containsExactly("INFO", "INFO", "ERROR");
        assertThat(saveLogPort.logs.getLast().message()).isEqualTo("gateway timeout");
        assertThat(result.failureReason()).isEqualTo("gateway timeout");
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

    @Test
    void runTaskRejectsWhenWorkspacePathIsAlreadyLocked() {
        FakeRunWorkspaceAgentPort runPort = new FakeRunWorkspaceAgentPort(
                new WorkspaceAgentRunResult(true, "unused", null));
        FakeWorkspaceExecutionLockPort lockPort = new FakeWorkspaceExecutionLockPort(false);
        RunTaskService service = newService(
                taskDetail(TaskStatus.ASSIGNED, 501L),
                List.of(savedAgent(501L, WorkspaceAgentStatus.READY, "gateway-agent-1")),
                Optional.of(gatewayBinding()),
                runPort,
                new FakeSaveTaskExecutionPort(),
                new FakeUpdateTaskExecutionPort(),
                new FakeUpdateTaskStatusPort(),
                lockPort,
                WorkspaceMemberRole.MEMBER);

        assertThatThrownBy(() -> service.runTask(new RunTaskCommand(101L, 301L, "google-1", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("workspace path is already running");
        assertThat(runPort.called).isFalse();
        assertThat(lockPort.lastCommand.workspacePath()).isEqualTo(".aquila/workspaces/101");
    }

    @Test
    void runTaskReleasesWorkspaceLockWhenGatewayThrowsException() {
        FakeWorkspaceExecutionLockPort lockPort = new FakeWorkspaceExecutionLockPort(true);
        RunTaskService service = newService(
                taskDetail(TaskStatus.ASSIGNED, 501L),
                List.of(savedAgent(501L, WorkspaceAgentStatus.READY, "gateway-agent-1")),
                Optional.of(gatewayBinding()),
                new FakeRunWorkspaceAgentPort(new IllegalStateException("gateway timeout")),
                new FakeSaveTaskExecutionPort(),
                new FakeUpdateTaskExecutionPort(),
                new FakeUpdateTaskStatusPort(),
                lockPort,
                WorkspaceMemberRole.MEMBER);

        RunTaskResult result = service.runTask(new RunTaskCommand(101L, 301L, "google-1", false));

        assertThat(result.failureReason()).isEqualTo("gateway timeout");
        assertThat(lockPort.lastLock.closed).isTrue();
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
        return newService(
                task,
                agents,
                binding,
                runPort,
                saveExecutionPort,
                updateExecutionPort,
                updateTaskStatusPort,
                new FakeSaveTaskExecutionLogPort(),
                new FakeWorkspaceExecutionLockPort(true),
                role);
    }

    private static RunTaskService newService(
            TaskDetail task,
            List<SavedWorkspaceAgent> agents,
            Optional<WorkspaceGatewayBinding> binding,
            RunWorkspaceAgentPort runPort,
            SaveTaskExecutionPort saveExecutionPort,
            UpdateTaskExecutionPort updateExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveLogPort,
            WorkspaceMemberRole role) {
        return newService(
                task,
                agents,
                binding,
                runPort,
                saveExecutionPort,
                updateExecutionPort,
                updateTaskStatusPort,
                saveLogPort,
                WorkflowMetrics.noop(),
                role);
    }

    private static RunTaskService newService(
            TaskDetail task,
            List<SavedWorkspaceAgent> agents,
            Optional<WorkspaceGatewayBinding> binding,
            RunWorkspaceAgentPort runPort,
            SaveTaskExecutionPort saveExecutionPort,
            UpdateTaskExecutionPort updateExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveLogPort,
            WorkflowMetrics workflowMetrics,
            WorkspaceMemberRole role) {
        return newService(
                task,
                agents,
                binding,
                runPort,
                saveExecutionPort,
                updateExecutionPort,
                updateTaskStatusPort,
                saveLogPort,
                new FakeWorkspaceExecutionLockPort(true),
                workflowMetrics,
                role);
    }

    private static RunTaskService newService(
            TaskDetail task,
            List<SavedWorkspaceAgent> agents,
            Optional<WorkspaceGatewayBinding> binding,
            RunWorkspaceAgentPort runPort,
            SaveTaskExecutionPort saveExecutionPort,
            UpdateTaskExecutionPort updateExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            WorkspaceExecutionLockPort lockPort,
            WorkspaceMemberRole role) {
        return newService(
                task,
                agents,
                binding,
                runPort,
                saveExecutionPort,
                updateExecutionPort,
                updateTaskStatusPort,
                new FakeSaveTaskExecutionLogPort(),
                lockPort,
                role);
    }

    private static RunTaskService newService(
            TaskDetail task,
            List<SavedWorkspaceAgent> agents,
            Optional<WorkspaceGatewayBinding> binding,
            RunWorkspaceAgentPort runPort,
            SaveTaskExecutionPort saveExecutionPort,
            UpdateTaskExecutionPort updateExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveLogPort,
            WorkspaceExecutionLockPort lockPort,
            WorkspaceMemberRole role) {
        return newService(
                task,
                agents,
                binding,
                runPort,
                saveExecutionPort,
                updateExecutionPort,
                updateTaskStatusPort,
                saveLogPort,
                lockPort,
                WorkflowMetrics.noop(),
                role);
    }

    private static RunTaskService newService(
            TaskDetail task,
            List<SavedWorkspaceAgent> agents,
            Optional<WorkspaceGatewayBinding> binding,
            RunWorkspaceAgentPort runPort,
            SaveTaskExecutionPort saveExecutionPort,
            UpdateTaskExecutionPort updateExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveLogPort,
            WorkspaceExecutionLockPort lockPort,
            WorkflowMetrics workflowMetrics,
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
                saveLogPort,
                runPort,
                lockPort,
                workflowMetrics);
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
                null,
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

    private static class RecordingWorkflowMetrics implements WorkflowMetrics {
        private int taskStarted;
        private int taskSuccess;
        private int taskFailure;

        @Override
        public OperationSample startTaskExecution() {
            taskStarted++;
            return new OperationSample() {
                @Override
                public void recordSuccess() {
                    taskSuccess++;
                }

                @Override
                public void recordFailure() {
                    taskFailure++;
                }

                @Override
                public void recordTimeout() {
                }
            };
        }

        @Override
        public OperationSample startGatewayRpc(String method) {
            return OperationSample.noop();
        }

        @Override
        public void recordSlackEventReceived(String route) {
        }

        @Override
        public void recordSlackSignatureFailure(String route) {
        }

        @Override
        public void recordSlackDedupeHit() {
        }

        @Override
        public void recordSlackProcessingFailure(String stage) {
        }
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

    private static class FakeSaveTaskExecutionLogPort implements SaveTaskExecutionLogPort {
        private final List<TaskExecutionLogCommand> logs = new java.util.ArrayList<>();

        @Override
        public SavedTaskExecutionLog save(TaskExecutionLogCommand command) {
            logs.add(command);
            return new SavedTaskExecutionLog(
                    (long) logs.size(),
                    command.workspaceId(),
                    command.taskId(),
                    command.executionId(),
                    command.agentId(),
                    command.level(),
                    command.message(),
                    LocalDateTime.of(2026, 5, 16, 10, logs.size()));
        }
    }

    private static class FakeRunWorkspaceAgentPort implements RunWorkspaceAgentPort {
        private final WorkspaceAgentRunResult result;
        private final RuntimeException exception;
        private WorkspaceAgentRunCommand command;
        private boolean called;

        private FakeRunWorkspaceAgentPort(WorkspaceAgentRunResult result) {
            this.result = result;
            this.exception = null;
        }

        private FakeRunWorkspaceAgentPort(RuntimeException exception) {
            this.result = null;
            this.exception = exception;
        }

        @Override
        public WorkspaceAgentRunResult run(WorkspaceAgentRunCommand command) {
            this.called = true;
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static class FakeWorkspaceExecutionLockPort implements WorkspaceExecutionLockPort {
        private final boolean available;
        private WorkspaceExecutionLockCommand lastCommand;
        private FakeWorkspaceExecutionLock lastLock;

        private FakeWorkspaceExecutionLockPort(boolean available) {
            this.available = available;
        }

        @Override
        public Optional<WorkspaceExecutionLock> tryAcquire(WorkspaceExecutionLockCommand command) {
            this.lastCommand = command;
            if (!available) {
                return Optional.empty();
            }
            this.lastLock = new FakeWorkspaceExecutionLock();
            return Optional.of(lastLock);
        }
    }

    private static class FakeWorkspaceExecutionLock implements WorkspaceExecutionLock {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
