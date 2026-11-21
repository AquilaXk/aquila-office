package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CancelTaskCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.RecoverStaleTasksCommand;
import com.aquilaoffice.application.port.in.RecoverStaleTasksResult;
import com.aquilaoffice.application.port.in.RequestTaskApprovalCommand;
import com.aquilaoffice.application.port.in.ReviewTaskApprovalCommand;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.out.CancelTaskExecutionsPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RecoverStaleExecutionsPort;
import com.aquilaoffice.application.port.out.RecoverStaleTasksPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.SavedTaskExecutionLog;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Task 취소, 승인 인박스 전환·검토, 고착 복구 application service 계약을 검증한다.
 */
class TaskGovernanceServiceTest {

    @Test
    void cancelTaskCancelsActiveExecutionsWritesWarnLogAndCancelsTask() {
        FakeTaskStore taskStore = new FakeTaskStore(taskDetail(TaskStatus.IN_PROGRESS));
        FakeExecutionStore executionStore = new FakeExecutionStore(List.of(
                execution(801L, TaskExecutionStatus.RUNNING)));
        FakeLogStore logStore = new FakeLogStore();
        CancelTaskService service = new CancelTaskService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.MEMBER),
                taskStore,
                taskStore,
                executionStore,
                executionStore,
                logStore);

        UpdateTaskStatusResult result = service.cancelTask(new CancelTaskCommand(101L, 301L, "google-1"));

        assertThat(result.status()).isEqualTo(TaskStatus.CANCELED);
        assertThat(executionStore.canceledCount).isEqualTo(1);
        assertThat(logStore.savedLogs).singleElement().satisfies(log -> {
            assertThat(log.level()).isEqualTo("WARN");
            assertThat(log.message()).isEqualTo("Task canceled by member");
        });
    }

    @Test
    void cancelTaskRejectsTerminalStatus() {
        FakeTaskStore taskStore = new FakeTaskStore(taskDetail(TaskStatus.COMPLETED));
        FakeExecutionStore executionStore = new FakeExecutionStore(List.of());
        FakeLogStore logStore = new FakeLogStore();
        CancelTaskService service = new CancelTaskService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.MEMBER),
                taskStore,
                taskStore,
                executionStore,
                executionStore,
                logStore);

        assertThatThrownBy(() -> service.cancelTask(new CancelTaskCommand(101L, 301L, "google-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("terminal task status cannot be changed");
        assertThat(executionStore.canceledCount).isZero();
    }

    @Test
    void requestTaskApprovalMovesToWaitingUserAndRejectsOversizedReason() {
        FakeTaskStore taskStore = new FakeTaskStore(taskDetail(TaskStatus.ASSIGNED));
        FakeExecutionStore executionStore = new FakeExecutionStore(List.of(
                execution(801L, TaskExecutionStatus.SUCCEEDED)));
        FakeLogStore logStore = new FakeLogStore();
        RequestTaskApprovalService service = new RequestTaskApprovalService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.MEMBER),
                taskStore,
                taskStore,
                executionStore,
                logStore);

        UpdateTaskStatusResult result = service.requestApproval(
                new RequestTaskApprovalCommand(101L, 301L, "google-1", "배포 전 검토 필요"));

        assertThat(result.status()).isEqualTo(TaskStatus.WAITING_USER);
        assertThat(logStore.savedLogs).singleElement().satisfies(log -> {
            assertThat(log.level()).isEqualTo("INFO");
            assertThat(log.message()).contains("배포 전 검토 필요");
        });

        assertThatThrownBy(() -> new RequestTaskApprovalCommand(
                        101L, 301L, "google-1", "x".repeat(RequestTaskApprovalCommand.MAX_REASON_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reason must not exceed 500 characters");
    }

    @Test
    void reviewTaskApprovalSupportsApproveWithRerunAndRejectAndRejectsInvalidInputs() {
        FakeTaskStore approveStore = new FakeTaskStore(taskDetail(TaskStatus.WAITING_USER));
        FakeExecutionStore executionStore = new FakeExecutionStore(List.of(
                execution(801L, TaskExecutionStatus.RUNNING)));
        FakeLogStore approveLogs = new FakeLogStore();
        List<RunTaskCommand> reranCommands = new ArrayList<>();
        RunTaskUseCase runTaskUseCase = command -> {
            reranCommands.add(command);
            approveStore.updateStatus(new TaskStatusUpdate(command.workspaceId(), command.taskId(), TaskStatus.COMPLETED));
            return new RunTaskResult(
                    command.workspaceId(),
                    command.taskId(),
                    802L,
                    501L,
                    TaskStatus.COMPLETED,
                    TaskExecutionStatus.SUCCEEDED,
                    "done",
                    null);
        };
        ReviewTaskApprovalService approveService = new ReviewTaskApprovalService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.ADMIN),
                approveStore,
                approveStore,
                executionStore,
                executionStore,
                approveLogs,
                runTaskUseCase);

        UpdateTaskStatusResult approved = approveService.reviewApproval(
                new ReviewTaskApprovalCommand(101L, 301L, "google-1", "APPROVE", "진행 승인", true));
        assertThat(approved.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(reranCommands).hasSize(1);
        assertThat(approveLogs.savedLogs).singleElement().satisfies(log -> {
            assertThat(log.level()).isEqualTo("INFO");
            assertThat(log.message()).contains("Approval granted: 진행 승인");
        });

        FakeTaskStore rejectStore = new FakeTaskStore(taskDetail(TaskStatus.WAITING_USER));
        FakeExecutionStore rejectExecStore = new FakeExecutionStore(List.of(
                execution(801L, TaskExecutionStatus.RUNNING)));
        FakeLogStore rejectLogs = new FakeLogStore();
        ReviewTaskApprovalService rejectService = new ReviewTaskApprovalService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.ADMIN),
                rejectStore,
                rejectStore,
                rejectExecStore,
                rejectExecStore,
                rejectLogs,
                runTaskUseCase);

        UpdateTaskStatusResult rejected = rejectService.reviewApproval(
                new ReviewTaskApprovalCommand(101L, 301L, "google-1", "REJECT", "요구사항 불충족", false));
        assertThat(rejected.status()).isEqualTo(TaskStatus.CANCELED);
        assertThat(rejectExecStore.canceledCount).isEqualTo(1);
        assertThat(rejectLogs.savedLogs).singleElement().satisfies(log -> {
            assertThat(log.level()).isEqualTo("WARN");
            assertThat(log.message()).contains("Approval rejected: 요구사항 불충족");
        });

        FakeTaskStore nonWaitingStore = new FakeTaskStore(taskDetail(TaskStatus.ASSIGNED));
        ReviewTaskApprovalService nonWaitingService = new ReviewTaskApprovalService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.ADMIN),
                nonWaitingStore,
                nonWaitingStore,
                executionStore,
                executionStore,
                approveLogs,
                runTaskUseCase);
        assertThatThrownBy(() -> nonWaitingService.reviewApproval(
                        new ReviewTaskApprovalCommand(101L, 301L, "google-1", "APPROVE", null, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("task is not waiting user approval");

        ReviewTaskApprovalService memberReviewService = new ReviewTaskApprovalService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.MEMBER),
                approveStore,
                approveStore,
                executionStore,
                executionStore,
                approveLogs,
                runTaskUseCase);
        assertThatThrownBy(() -> memberReviewService.reviewApproval(
                        new ReviewTaskApprovalCommand(101L, 301L, "google-1", "APPROVE", null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");

        assertThatThrownBy(() -> new ReviewTaskApprovalCommand(101L, 301L, "google-1", "HOLD", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("decision must be APPROVE or REJECT");
        assertThatThrownBy(() -> new ReviewTaskApprovalCommand(
                        101L, 301L, "google-1", "APPROVE", "c".repeat(501), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("comment must not exceed 500 characters");
    }

    @Test
    void recoverStaleTasksRequiresAdminValidatesMinutesAndUsesUtcCutoff() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-16T12:00:00Z"), ZoneOffset.UTC);
        List<LocalDateTime> cutoffs = new ArrayList<>();
        RecoverStaleExecutionsPort execPort = (workspaceId, cutoffUtc, nowUtc, reason) -> {
            cutoffs.add(cutoffUtc);
            return 2;
        };
        RecoverStaleTasksPort taskPort = (workspaceId, cutoffUtc, nowUtc) -> {
            cutoffs.add(cutoffUtc);
            return 1;
        };

        RecoverStaleTasksService adminService = new RecoverStaleTasksService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.ADMIN),
                execPort,
                taskPort,
                fixedClock);

        RecoverStaleTasksResult result = adminService.recoverStaleTasks(
                RecoverStaleTasksCommand.of(101L, "google-1", null));

        assertThat(result.recoveredTaskCount()).isEqualTo(1);
        assertThat(result.recoveredExecutionCount()).isEqualTo(2);
        assertThat(cutoffs).containsExactly(
                LocalDateTime.of(2026, 5, 16, 11, 45),
                LocalDateTime.of(2026, 5, 16, 11, 45));

        RecoverStaleTasksService memberService = new RecoverStaleTasksService(
                memberPort(),
                workspacePort(WorkspaceMemberRole.MEMBER),
                execPort,
                taskPort,
                fixedClock);
        assertThatThrownBy(() -> memberService.recoverStaleTasks(new RecoverStaleTasksCommand(101L, "google-1", 30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");

        assertThatThrownBy(() -> new RecoverStaleTasksCommand(101L, "google-1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("staleMinutes must be between 1 and 1440");
        assertThatThrownBy(() -> new RecoverStaleTasksCommand(101L, "google-1", 1441))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("staleMinutes must be between 1 and 1440");
    }

    private static LoadMemberPort memberPort() {
        return googleSub -> Optional.of(Member.createUser(googleSub, googleSub + "@example.com", "User"));
    }

    private static LoadWorkspacePort workspacePort(WorkspaceMemberRole role) {
        return new LoadWorkspacePort() {
            @Override
            public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
                return List.of();
            }

            @Override
            public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                    Long workspaceId, String googleSub) {
                return Optional.of(new GetWorkspaceDetailResult(
                        workspaceId,
                        "Team",
                        "Desc",
                        googleSub,
                        role,
                        LocalDateTime.of(2026, 5, 16, 10, 0),
                        null));
            }
        };
    }

    private static TaskDetail taskDetail(TaskStatus status) {
        return new TaskDetail(
                301L,
                101L,
                "google-1",
                "검토 작업",
                "상세 내용",
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                501L,
                null,
                status,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static SavedTaskExecution execution(Long executionId, TaskExecutionStatus status) {
        return new SavedTaskExecution(
                executionId,
                101L,
                301L,
                501L,
                status,
                null,
                null,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static final class FakeTaskStore implements LoadTaskPort, UpdateTaskStatusPort {
        private TaskDetail current;

        private FakeTaskStore(TaskDetail current) {
            this.current = current;
        }

        @Override
        public Optional<TaskDetail> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
            return Optional.ofNullable(current);
        }

        @Override
        public TaskDetail updateStatus(TaskStatusUpdate update) {
            current = new TaskDetail(
                    current.taskId(),
                    current.workspaceId(),
                    current.createdByGoogleSub(),
                    current.title(),
                    current.description(),
                    current.type(),
                    current.priority(),
                    current.assignedAgentId(),
                    current.sourceRef(),
                    update.status(),
                    current.createdAt(),
                    LocalDateTime.of(2026, 5, 16, 10, 5));
            return current;
        }
    }

    private static final class FakeExecutionStore
            implements CancelTaskExecutionsPort, LoadTaskExecutionReportsPort {
        private final List<SavedTaskExecution> executions;
        private int canceledCount;

        private FakeExecutionStore(List<SavedTaskExecution> executions) {
            this.executions = executions;
        }

        @Override
        public List<SavedTaskExecution> cancelActiveByWorkspaceIdAndTaskId(
                Long workspaceId, Long taskId, String failureReason) {
            List<SavedTaskExecution> canceled = new ArrayList<>();
            for (SavedTaskExecution item : executions) {
                if (item.status() == TaskExecutionStatus.QUEUED || item.status() == TaskExecutionStatus.RUNNING) {
                    canceled.add(new SavedTaskExecution(
                            item.executionId(),
                            item.workspaceId(),
                            item.taskId(),
                            item.agentId(),
                            TaskExecutionStatus.CANCELED,
                            item.resultSummary(),
                            failureReason,
                            item.createdAt(),
                            LocalDateTime.of(2026, 5, 16, 10, 5)));
                }
            }
            this.canceledCount += canceled.size();
            return canceled;
        }

        @Override
        public List<SavedTaskExecution> findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(Long workspaceId, Long taskId) {
            return executions;
        }
    }

    private static final class FakeLogStore implements SaveTaskExecutionLogPort {
        private final List<TaskExecutionLogCommand> savedLogs = new ArrayList<>();

        @Override
        public SavedTaskExecutionLog save(TaskExecutionLogCommand command) {
            savedLogs.add(command);
            return new SavedTaskExecutionLog(
                    (long) savedLogs.size(),
                    command.workspaceId(),
                    command.taskId(),
                    command.executionId(),
                    command.agentId(),
                    command.level(),
                    command.message(),
                    LocalDateTime.of(2026, 5, 16, 10, 5));
        }
    }
}
