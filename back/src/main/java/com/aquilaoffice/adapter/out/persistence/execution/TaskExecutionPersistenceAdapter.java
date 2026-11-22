package com.aquilaoffice.adapter.out.persistence.execution;

import com.aquilaoffice.adapter.out.persistence.task.TaskJpaEntity;
import com.aquilaoffice.adapter.out.persistence.task.TaskJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.CancelTaskExecutionsPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionLogsPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionModelTelemetryPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionTokenUsagePort;
import com.aquilaoffice.application.port.out.WorkspaceModelTelemetryEntry;
import com.aquilaoffice.application.port.out.RecoverStaleExecutionsPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SavedTaskExecutionLog;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskExecutionStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * TaskExecution outbound port를 JPA 저장소로 구현하는 persistence adapter다.
 */
@Repository
public class TaskExecutionPersistenceAdapter
        implements SaveTaskExecutionPort,
                SaveTaskExecutionLogPort,
                UpdateTaskExecutionPort,
                LoadTaskExecutionReportsPort,
                LoadTaskExecutionLogsPort,
                LoadTaskExecutionTokenUsagePort,
                LoadTaskExecutionModelTelemetryPort,
                CancelTaskExecutionsPort,
                RecoverStaleExecutionsPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    private final TaskExecutionJpaRepository taskExecutionJpaRepository;
    private final TaskExecutionLogJpaRepository taskExecutionLogJpaRepository;

    public TaskExecutionPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            TaskJpaRepository taskJpaRepository,
            TaskExecutionJpaRepository taskExecutionJpaRepository,
            TaskExecutionLogJpaRepository taskExecutionLogJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.taskJpaRepository = taskJpaRepository;
        this.taskExecutionJpaRepository = taskExecutionJpaRepository;
        this.taskExecutionLogJpaRepository = taskExecutionLogJpaRepository;
    }

    @Override
    @Transactional
    public SavedTaskExecution save(TaskExecution execution) {
        Objects.requireNonNull(execution, "execution must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(execution.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        TaskJpaEntity taskEntity = taskJpaRepository
                .findByWorkspace_IdAndId(execution.getWorkspaceId(), execution.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("task entity not found"));
        return toSavedTaskExecution(taskExecutionJpaRepository.save(
                TaskExecutionJpaEntity.from(execution, workspaceEntity, taskEntity)));
    }

    @Override
    @Transactional
    public SavedTaskExecutionLog save(TaskExecutionLogCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TaskExecutionJpaEntity executionEntity = taskExecutionJpaRepository
                .findById(command.executionId())
                .filter(entity -> entity.getWorkspace().getId().equals(command.workspaceId()))
                .filter(entity -> entity.getTask().getId().equals(command.taskId()))
                .orElseThrow(() -> new IllegalArgumentException("task execution entity not found"));
        return toSavedTaskExecutionLog(taskExecutionLogJpaRepository.save(
                TaskExecutionLogJpaEntity.from(command, executionEntity)));
    }

    @Override
    @Transactional
    public SavedTaskExecution updateStatus(TaskExecutionStatusUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        TaskExecutionJpaEntity executionEntity = taskExecutionJpaRepository
                .findById(update.executionId())
                .filter(entity -> entity.getWorkspace().getId().equals(update.workspaceId()))
                .filter(entity -> entity.getTask().getId().equals(update.taskId()))
                .orElseThrow(() -> new IllegalArgumentException("task execution entity not found"));
        executionEntity.updateStatus(
                update.status(),
                update.resultSummary(),
                update.failureReason(),
                update.durationMs(),
                update.promptTokens(),
                update.completionTokens(),
                update.modelName());
        return toSavedTaskExecution(executionEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long sumTokensSince(Long workspaceId, LocalDateTime sinceUtc) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(sinceUtc, "sinceUtc must not be null");
        return taskExecutionJpaRepository.sumTokensByWorkspaceIdSince(workspaceId, sinceUtc);
    }

    @Override
    @Transactional
    public List<SavedTaskExecution> cancelActiveByWorkspaceIdAndTaskId(
            Long workspaceId,
            Long taskId,
            String failureReason) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        List<TaskExecutionJpaEntity> activeExecutions = taskExecutionJpaRepository
                .findByWorkspace_IdAndTask_IdAndStatusInOrderByCreatedAtDesc(
                        workspaceId,
                        taskId,
                        List.of(TaskExecutionStatus.QUEUED, TaskExecutionStatus.RUNNING));
        if (activeExecutions.isEmpty()) {
            return List.of();
        }
        for (TaskExecutionJpaEntity execution : activeExecutions) {
            execution.updateStatus(TaskExecutionStatus.CANCELED, execution.getResultSummary(), failureReason);
        }
        return activeExecutions.stream()
                .map(this::toSavedTaskExecution)
                .toList();
    }

    @Override
    @Transactional
    public int recoverStaleExecutions(
            Long workspaceId,
            LocalDateTime cutoffUtc,
            LocalDateTime nowUtc,
            String failureReason) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(cutoffUtc, "cutoffUtc must not be null");
        Objects.requireNonNull(nowUtc, "nowUtc must not be null");
        List<TaskExecutionJpaEntity> staleExecutions =
                taskExecutionJpaRepository.findStaleByWorkspaceIdAndStatusInBefore(
                        workspaceId,
                        List.of(TaskExecutionStatus.QUEUED, TaskExecutionStatus.RUNNING),
                        cutoffUtc);
        if (staleExecutions.isEmpty()) {
            return 0;
        }
        List<TaskExecutionLogJpaEntity> logs = staleExecutions.stream()
                .map(execution -> TaskExecutionLogJpaEntity.from(
                        new TaskExecutionLogCommand(
                                workspaceId,
                                execution.getTask().getId(),
                                execution.getId(),
                                execution.getAgentId(),
                                "ERROR",
                                failureReason),
                        execution))
                .toList();
        taskExecutionLogJpaRepository.saveAll(logs);
        List<Long> ids = staleExecutions.stream().map(TaskExecutionJpaEntity::getId).toList();
        return taskExecutionJpaRepository.markStaleFailedByIds(
                ids,
                TaskExecutionStatus.FAILED,
                failureReason,
                nowUtc);
    }

    private static final int MAX_EXECUTION_REPORTS_PAGE_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public List<SavedTaskExecution> findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(Long workspaceId, Long taskId) {
        return taskExecutionJpaRepository
                .findByWorkspace_IdAndTask_IdOrderByCreatedAtDesc(
                        workspaceId,
                        taskId,
                        PageRequest.of(0, MAX_EXECUTION_REPORTS_PAGE_SIZE))
                .stream()
                .map(this::toSavedTaskExecution)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedTaskExecutionLog> findByWorkspaceIdAndTaskIdOrderByCreatedAtAsc(Long workspaceId, Long taskId) {
        return taskExecutionLogJpaRepository
                .findByWorkspaceIdAndTaskIdOrderByCreatedAtAsc(workspaceId, taskId)
                .stream()
                .map(this::toSavedTaskExecutionLog)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceModelTelemetryEntry> loadModelTelemetry(Long workspaceId, int limit) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        int boundedLimit = Math.clamp(limit, 1, 50);
        return taskExecutionJpaRepository
                .aggregateModelTelemetryByWorkspaceId(
                        workspaceId,
                        TaskExecutionStatus.SUCCEEDED,
                        TaskExecutionStatus.FAILED,
                        PageRequest.of(0, boundedLimit))
                .stream()
                .map(TaskExecutionPersistenceAdapter::toModelTelemetryEntry)
                .toList();
    }

    private static WorkspaceModelTelemetryEntry toModelTelemetryEntry(Object[] row) {
        String modelName = row[0] == null ? "default" : String.valueOf(row[0]);
        long executionCount = row[1] instanceof Number num ? num.longValue() : 0L;
        long successCount = row[2] instanceof Number num ? num.longValue() : 0L;
        long failedCount = row[3] instanceof Number num ? num.longValue() : 0L;
        long promptTokens = row[4] instanceof Number num ? num.longValue() : 0L;
        long completionTokens = row[5] instanceof Number num ? num.longValue() : 0L;
        Double avgDurationMs = row[6] instanceof Number num ? num.doubleValue() : null;
        return new WorkspaceModelTelemetryEntry(
                modelName,
                executionCount,
                successCount,
                failedCount,
                promptTokens,
                completionTokens,
                avgDurationMs);
    }

    private SavedTaskExecution toSavedTaskExecution(TaskExecutionJpaEntity execution) {
        return new SavedTaskExecution(
                execution.getId(),
                execution.getWorkspace().getId(),
                execution.getTask().getId(),
                execution.getAgentId(),
                execution.getStatus(),
                execution.getResultSummary(),
                execution.getFailureReason(),
                execution.getDurationMs(),
                execution.getPromptTokens(),
                execution.getCompletionTokens(),
                execution.getModelName(),
                execution.getCreatedAt(),
                execution.getUpdatedAt());
    }

    private SavedTaskExecutionLog toSavedTaskExecutionLog(TaskExecutionLogJpaEntity log) {
        TaskExecutionJpaEntity execution = log.getExecution();
        return new SavedTaskExecutionLog(
                log.getId(),
                execution.getWorkspace().getId(),
                execution.getTask().getId(),
                execution.getId(),
                log.getAgentId(),
                log.getLevel(),
                log.getMessage(),
                log.getCreatedAt());
    }
}
