package com.aquilaoffice.adapter.out.persistence.execution;

import com.aquilaoffice.adapter.out.persistence.task.TaskJpaEntity;
import com.aquilaoffice.adapter.out.persistence.task.TaskJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.domain.execution.TaskExecution;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * TaskExecution outbound port를 JPA 저장소로 구현하는 persistence adapter다.
 */
@Repository
public class TaskExecutionPersistenceAdapter implements SaveTaskExecutionPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    private final TaskExecutionJpaRepository taskExecutionJpaRepository;

    public TaskExecutionPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            TaskJpaRepository taskJpaRepository,
            TaskExecutionJpaRepository taskExecutionJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.taskJpaRepository = taskJpaRepository;
        this.taskExecutionJpaRepository = taskExecutionJpaRepository;
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
}
