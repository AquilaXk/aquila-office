package com.aquilaoffice.adapter.out.persistence.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.task.TaskJpaRepository;
import com.aquilaoffice.adapter.out.persistence.task.TaskPersistenceAdapter;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.CancelTaskExecutionsPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionModelTelemetryPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionTokenUsagePort;
import com.aquilaoffice.application.port.out.RecoverStaleExecutionsPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskExecutionStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.Workspace;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * TaskExecution persistence adapter가 후속 실행 흐름에서 사용할 실행 기록을 저장하는지 검증한다.
 */
@DataJpaTest
@Import({TaskPersistenceAdapter.class, TaskExecutionPersistenceAdapter.class})
class TaskExecutionPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private TaskExecutionJpaRepository taskExecutionJpaRepository;

    @Autowired
    private TaskExecutionLogJpaRepository taskExecutionLogJpaRepository;

    @Autowired
    private SaveTaskPort saveTaskPort;

    @Autowired
    private SaveTaskExecutionPort saveTaskExecutionPort;

    @Autowired
    private SaveTaskExecutionLogPort saveTaskExecutionLogPort;

    @Autowired
    private UpdateTaskExecutionPort updateTaskExecutionPort;

    @Autowired
    private LoadTaskExecutionReportsPort loadTaskExecutionReportsPort;

    @Autowired
    private LoadTaskExecutionTokenUsagePort loadTaskExecutionTokenUsagePort;

    @Autowired
    private LoadTaskExecutionModelTelemetryPort loadTaskExecutionModelTelemetryPort;

    @Autowired
    private CancelTaskExecutionsPort cancelTaskExecutionsPort;

    @Autowired
    private RecoverStaleExecutionsPort recoverStaleExecutionsPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savePersistsQueuedTaskExecution() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));

        var saved = saveTaskExecutionPort.save(TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));

        assertThat(saved.executionId()).isNotNull();
        assertThat(saved.workspaceId()).isEqualTo(workspaceEntity.getId());
        assertThat(saved.taskId()).isEqualTo(task.taskId());
        assertThat(saved.agentId()).isEqualTo(501L);
        assertThat(saved.status()).isEqualTo(TaskExecutionStatus.QUEUED);
        assertThat(taskExecutionJpaRepository.count()).isEqualTo(1);
        assertThat(taskJpaRepository.findById(task.taskId())).isPresent();
    }

    @Test
    void updateStatusStoresRunningSucceededAndFailedResults() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));
        var saved = saveTaskExecutionPort.save(TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));

        var running = updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                task.taskId(),
                saved.executionId(),
                TaskExecutionStatus.RUNNING,
                null,
                null));
        var succeeded = updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                task.taskId(),
                saved.executionId(),
                TaskExecutionStatus.SUCCEEDED,
                "실행 완료",
                null));
        var failed = updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                task.taskId(),
                saved.executionId(),
                TaskExecutionStatus.FAILED,
                null,
                "gateway timeout"));

        assertThat(running.status()).isEqualTo(TaskExecutionStatus.RUNNING);
        assertThat(succeeded.resultSummary()).isEqualTo("실행 완료");
        assertThat(failed.status()).isEqualTo(TaskExecutionStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("gateway timeout");
        assertThat(taskExecutionJpaRepository.findById(saved.executionId()))
                .hasValueSatisfying(entity -> {
                    assertThat(entity.getStatus()).isEqualTo(TaskExecutionStatus.FAILED);
                    assertThat(entity.getFailureReason()).isEqualTo("gateway timeout");
                });
    }

    @Test
    void findReportsReturnsTaskExecutionsNewestFirst() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));
        var otherTask = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "문서 작성",
                null,
                TaskType.DOCUMENTATION,
                TaskPriority.MEDIUM,
                501L));
        var olderExecution =
                saveTaskExecutionPort.save(TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                task.taskId(),
                olderExecution.executionId(),
                TaskExecutionStatus.SUCCEEDED,
                "오래된 실행 완료",
                null));
        var newerExecution =
                saveTaskExecutionPort.save(TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                task.taskId(),
                newerExecution.executionId(),
                TaskExecutionStatus.FAILED,
                null,
                "최근 실행 실패"));
        saveTaskExecutionPort.save(TaskExecution.create(workspaceEntity.getId(), otherTask.taskId(), 501L));

        var reports = loadTaskExecutionReportsPort.findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(
                workspaceEntity.getId(),
                task.taskId());

        assertThat(reports).extracting("executionId")
                .containsExactly(newerExecution.executionId(), olderExecution.executionId());
        assertThat(reports.get(0).failureReason()).isEqualTo("최근 실행 실패");
        assertThat(reports.get(1).resultSummary()).isEqualTo("오래된 실행 완료");
    }

    @Test
    void saveLogPersistsTaskExecutionLog() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));
        var execution = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));

        var savedLog = saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                workspaceEntity.getId(),
                task.taskId(),
                execution.executionId(),
                501L,
                "ERROR",
                "gateway timeout"));

        assertThat(savedLog.logId()).isNotNull();
        assertThat(savedLog.workspaceId()).isEqualTo(workspaceEntity.getId());
        assertThat(savedLog.taskId()).isEqualTo(task.taskId());
        assertThat(savedLog.executionId()).isEqualTo(execution.executionId());
        assertThat(savedLog.level()).isEqualTo("ERROR");
        assertThat(savedLog.message()).isEqualTo("gateway timeout");
        assertThat(taskExecutionLogJpaRepository.count()).isEqualTo(1);
        assertThat(taskExecutionLogJpaRepository.findById(savedLog.logId()))
                .hasValueSatisfying(entity -> {
                    assertThat(entity.getExecution().getId()).isEqualTo(execution.executionId());
                    assertThat(entity.getLevel()).isEqualTo("ERROR");
                    assertThat(entity.getMessage()).isEqualTo("gateway timeout");
                });
    }

    @Test
    void persistsTokenMetricsSumsDailyUsageCancelsActiveAndRecoversStale() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "거버넌스 테스트",
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                501L));

        var succeededExec = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                task.taskId(),
                succeededExec.executionId(),
                TaskExecutionStatus.SUCCEEDED,
                "완료",
                null,
                320L,
                140,
                60,
                "gemini-2.5-flash"));

        TaskExecutionJpaEntity succeededEntity =
                taskExecutionJpaRepository.findById(succeededExec.executionId()).orElseThrow();
        assertThat(succeededEntity.getDurationMs()).isEqualTo(320L);
        assertThat(succeededEntity.getPromptTokens()).isEqualTo(140);
        assertThat(succeededEntity.getCompletionTokens()).isEqualTo(60);
        assertThat(succeededEntity.getModelName()).isEqualTo("gemini-2.5-flash");

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        assertThat(loadTaskExecutionTokenUsagePort.sumTokensSince(
                        workspaceEntity.getId(),
                        nowUtc.minusHours(1)))
                .isEqualTo(200L);

        var telemetry = loadTaskExecutionModelTelemetryPort.loadModelTelemetry(workspaceEntity.getId(), 10);
        assertThat(telemetry).singleElement().satisfies(entry -> {
            assertThat(entry.modelName()).isEqualTo("gemini-2.5-flash");
            assertThat(entry.executionCount()).isEqualTo(1L);
            assertThat(entry.successCount()).isEqualTo(1L);
            assertThat(entry.failedCount()).isEqualTo(0L);
            assertThat(entry.totalTokens()).isEqualTo(200L);
            assertThat(entry.avgDurationMs()).isEqualTo(320.0);
        });

        var activeExec = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        var canceled = cancelTaskExecutionsPort.cancelActiveByWorkspaceIdAndTaskId(
                workspaceEntity.getId(),
                task.taskId(),
                "Canceled by user");
        assertThat(canceled).singleElement().satisfies(item -> {
            assertThat(item.executionId()).isEqualTo(activeExec.executionId());
            assertThat(item.status()).isEqualTo(TaskExecutionStatus.CANCELED);
            assertThat(item.failureReason()).isEqualTo("Canceled by user");
        });

        var staleExec = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceEntity.getId(),
                task.taskId(),
                staleExec.executionId(),
                TaskExecutionStatus.RUNNING,
                null,
                null));
        var freshExec = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        entityManager.flush();
        entityManager.createNativeQuery("update task_executions set updated_at = :oldTime where id = :id")
                .setParameter("oldTime", nowUtc.minusMinutes(30))
                .setParameter("id", staleExec.executionId())
                .executeUpdate();
        entityManager.clear();

        int recovered = recoverStaleExecutionsPort.recoverStaleExecutions(
                workspaceEntity.getId(),
                nowUtc.minusMinutes(15),
                nowUtc,
                "Recovered stale execution");

        assertThat(recovered).isEqualTo(1);
        assertThat(taskExecutionJpaRepository.findById(staleExec.executionId()).orElseThrow().getStatus())
                .isEqualTo(TaskExecutionStatus.FAILED);
        assertThat(taskExecutionJpaRepository.findById(freshExec.executionId()).orElseThrow().getStatus())
                .isEqualTo(TaskExecutionStatus.QUEUED);
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
