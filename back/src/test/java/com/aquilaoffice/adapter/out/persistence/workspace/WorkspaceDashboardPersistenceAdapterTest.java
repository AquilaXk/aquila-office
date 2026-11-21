package com.aquilaoffice.adapter.out.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentPersistenceAdapter;
import com.aquilaoffice.adapter.out.persistence.execution.TaskExecutionPersistenceAdapter;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.task.TaskPersistenceAdapter;
import com.aquilaoffice.application.port.out.LoadWorkspaceDashboardSummaryPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskExecutionStatusUpdate;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.Workspace;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
    WorkspaceAgentPersistenceAdapter.class,
    TaskPersistenceAdapter.class,
    TaskExecutionPersistenceAdapter.class,
    WorkspaceDashboardPersistenceAdapter.class
})
class WorkspaceDashboardPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private SaveTaskPort saveTaskPort;

    @Autowired
    private UpdateTaskStatusPort updateTaskStatusPort;

    @Autowired
    private SaveWorkspaceAgentPort saveWorkspaceAgentPort;

    @Autowired
    private SaveTaskExecutionPort saveTaskExecutionPort;

    @Autowired
    private UpdateTaskExecutionPort updateTaskExecutionPort;

    @Autowired
    private SaveTaskExecutionLogPort saveTaskExecutionLogPort;

    @Autowired
    private LoadWorkspaceDashboardSummaryPort loadWorkspaceDashboardSummaryPort;

    @Test
    void loadSummaryReturnsRecentExecutionLogsNewestFirst() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));
        var olderExecution = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                workspaceEntity.getId(),
                task.taskId(),
                olderExecution.executionId(),
                501L,
                "INFO",
                "Task execution queued"));
        var newerExecution = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                workspaceEntity.getId(),
                task.taskId(),
                newerExecution.executionId(),
                501L,
                "ERROR",
                "gateway timeout"));

        var summary = loadWorkspaceDashboardSummaryPort.loadSummary(workspaceEntity.getId());

        assertThat(summary.recentLogs()).hasSize(2);
        assertThat(summary.recentLogs()).extracting("executionId")
                .containsExactly(newerExecution.executionId(), olderExecution.executionId());
        assertThat(summary.recentLogs().getFirst().level()).isEqualTo("ERROR");
        assertThat(summary.recentLogs().getFirst().message()).isEqualTo("gateway timeout");
    }

    @Test
    void loadSummaryAggregatesAgentAndTaskCountsAndHandlesEmptyWorkspace() {
        WorkspaceJpaEntity emptyWorkspace = saveWorkspaceEntity("empty-google");

        var emptySummary = loadWorkspaceDashboardSummaryPort.loadSummary(emptyWorkspace.getId());
        assertThat(emptySummary.agentCount()).isZero();
        assertThat(emptySummary.runningAgentCount()).isZero();
        assertThat(emptySummary.idleAgentCount()).isZero();
        assertThat(emptySummary.errorAgentCount()).isZero();
        assertThat(emptySummary.taskCount()).isZero();
        assertThat(emptySummary.runningTaskCount()).isZero();
        assertThat(emptySummary.completedTaskCount()).isZero();
        assertThat(emptySummary.failedTaskCount()).isZero();

        WorkspaceJpaEntity populatedWorkspace = saveWorkspaceEntity("owner-google");
        WorkspaceAgent readyRunning = WorkspaceAgent.create(
                populatedWorkspace.getId(),
                "Ready Running",
                WorkspaceAgentCategory.BACKEND,
                ".aquila/1",
                "owner-google");
        readyRunning.markReady("oc-1");
        var readyRunningAgent = saveWorkspaceAgentPort.save(readyRunning, List.of());

        WorkspaceAgent readyIdle = WorkspaceAgent.create(
                populatedWorkspace.getId(),
                "Ready Idle",
                WorkspaceAgentCategory.QA,
                ".aquila/2",
                "owner-google");
        readyIdle.markReady("oc-2");
        saveWorkspaceAgentPort.save(readyIdle, List.of());

        WorkspaceAgent errored = WorkspaceAgent.create(
                populatedWorkspace.getId(),
                "Errored",
                WorkspaceAgentCategory.CUSTOM,
                ".aquila/3",
                "owner-google");
        errored.markError("boom");
        saveWorkspaceAgentPort.save(errored, List.of());

        WorkspaceAgent disabled = WorkspaceAgent.create(
                populatedWorkspace.getId(),
                "Disabled",
                WorkspaceAgentCategory.CUSTOM,
                ".aquila/4",
                "owner-google");
        disabled.disable();
        saveWorkspaceAgentPort.save(disabled, List.of());

        var runningTask = saveTaskPort.save(Task.create(
                populatedWorkspace.getId(),
                "owner-google",
                "진행 중 작업",
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                readyRunningAgent.agentId()));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                populatedWorkspace.getId(), runningTask.taskId(), TaskStatus.IN_PROGRESS));
        TaskExecution runningExecution = TaskExecution.create(
                populatedWorkspace.getId(), runningTask.taskId(), readyRunningAgent.agentId());
        runningExecution.markRunning();
        saveTaskExecutionPort.save(runningExecution);

        var completedTask = saveTaskPort.save(Task.create(
                populatedWorkspace.getId(),
                "owner-google",
                "완료 작업",
                null,
                TaskType.CODE_REVIEW,
                TaskPriority.MEDIUM,
                readyRunningAgent.agentId()));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                populatedWorkspace.getId(), completedTask.taskId(), TaskStatus.COMPLETED));

        var failedTask = saveTaskPort.save(Task.create(
                populatedWorkspace.getId(),
                "owner-google",
                "실패 작업",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.LOW,
                readyRunningAgent.agentId()));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                populatedWorkspace.getId(), failedTask.taskId(), TaskStatus.FAILED));

        var populatedSummary = loadWorkspaceDashboardSummaryPort.loadSummary(populatedWorkspace.getId());
        assertThat(populatedSummary.agentCount()).isEqualTo(3);
        assertThat(populatedSummary.runningAgentCount()).isEqualTo(1);
        assertThat(populatedSummary.idleAgentCount()).isEqualTo(1);
        assertThat(populatedSummary.errorAgentCount()).isEqualTo(1);
        assertThat(populatedSummary.taskCount()).isEqualTo(3);
        assertThat(populatedSummary.runningTaskCount()).isEqualTo(1);
        assertThat(populatedSummary.completedTaskCount()).isEqualTo(1);
        assertThat(populatedSummary.failedTaskCount()).isEqualTo(1);
    }

    @Test
    void loadSummaryAggregatesTokenLimitTodayUsageWaitingApprovalAndAvgDuration() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        workspaceEntity.update("Team", "Description", 50_000);
        workspaceJpaRepository.flush();

        var agent = saveWorkspaceAgentPort.save(
                WorkspaceAgent.create(
                        workspaceEntity.getId(),
                        "Worker",
                        WorkspaceAgentCategory.BACKEND,
                        ".aquila/1",
                        "creator-google"),
                List.of());

        var waitingTask = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "승인 대기 작업",
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                agent.agentId()));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                workspaceEntity.getId(), waitingTask.taskId(), TaskStatus.WAITING_USER));

        var exec1 = saveTaskExecutionPort.save(TaskExecution.create(
                workspaceEntity.getId(), waitingTask.taskId(), agent.agentId()));
        updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                waitingTask.taskId(),
                exec1.executionId(),
                TaskExecutionStatus.SUCCEEDED,
                "1차 완료",
                null,
                200L,
                100,
                50,
                "gemini-2.5-flash"));

        var exec2 = saveTaskExecutionPort.save(TaskExecution.create(
                workspaceEntity.getId(), waitingTask.taskId(), agent.agentId()));
        updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                waitingTask.taskId(),
                exec2.executionId(),
                TaskExecutionStatus.FAILED,
                null,
                "실패",
                400L,
                200,
                50,
                "gemini-2.5-flash"));

        var summary = loadWorkspaceDashboardSummaryPort.loadSummary(workspaceEntity.getId());

        assertThat(summary.dailyTokenLimit()).isEqualTo(50_000);
        assertThat(summary.todayUsedTokens()).isEqualTo(400L);
        assertThat(summary.waitingApprovalTaskCount()).isEqualTo(1L);
        assertThat(summary.avgExecutionDurationMs()).isEqualTo(300L);
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
