package com.aquilaoffice.adapter.out.persistence.execution;

import com.aquilaoffice.adapter.out.persistence.task.TaskJpaEntity;
import com.aquilaoffice.adapter.out.persistence.task.TaskJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SavedTaskExecutionLog;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskExecutionStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.domain.execution.TaskExecution;
import java.util.List;
import java.util.Objects;
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
                LoadTaskExecutionReportsPort {

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
        executionEntity.updateStatus(update.status(), update.resultSummary(), update.failureReason());
        return toSavedTaskExecution(executionEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedTaskExecution> findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(Long workspaceId, Long taskId) {
        return taskExecutionJpaRepository.findByWorkspace_IdAndTask_IdOrderByCreatedAtDesc(workspaceId, taskId).stream()
                .map(this::toSavedTaskExecution)
                .toList();
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
