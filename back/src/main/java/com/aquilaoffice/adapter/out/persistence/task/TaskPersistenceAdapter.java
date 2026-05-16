package com.aquilaoffice.adapter.out.persistence.task;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SavedTask;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.TaskSummary;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.domain.task.Task;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task outbound port를 JPA 저장소로 구현하는 persistence adapter다.
 */
@Repository
public class TaskPersistenceAdapter
        implements SaveTaskPort, LoadTasksPort, LoadTaskPort, UpdateTaskStatusPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final TaskJpaRepository taskJpaRepository;

    public TaskPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            TaskJpaRepository taskJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.taskJpaRepository = taskJpaRepository;
    }

    @Override
    @Transactional
    public SavedTask save(Task task) {
        Objects.requireNonNull(task, "task must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(task.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        return toSavedTask(taskJpaRepository.save(TaskJpaEntity.from(task, workspaceEntity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSummary> findByWorkspaceId(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return taskJpaRepository.findByWorkspace_IdOrderByIdDesc(workspaceId).stream()
                .map(this::toTaskSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskDetail> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        return taskJpaRepository.findByWorkspace_IdAndId(workspaceId, taskId)
                .map(this::toTaskDetail);
    }

    @Override
    @Transactional
    public TaskDetail updateStatus(TaskStatusUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        TaskJpaEntity taskEntity = taskJpaRepository
                .findByWorkspace_IdAndId(update.workspaceId(), update.taskId())
                .orElseThrow(() -> new IllegalArgumentException("task entity not found"));
        taskEntity.updateStatus(update.status());
        return toTaskDetail(taskEntity);
    }

    private SavedTask toSavedTask(TaskJpaEntity task) {
        return new SavedTask(
                task.getId(),
                task.getWorkspace().getId(),
                task.getCreatedByGoogleSub(),
                task.getTitle(),
                task.getDescription(),
                task.getType(),
                task.getPriority(),
                task.getAssignedAgentId(),
                task.getSourceRef(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    private TaskSummary toTaskSummary(TaskJpaEntity task) {
        return new TaskSummary(
                task.getId(),
                task.getWorkspace().getId(),
                task.getCreatedByGoogleSub(),
                task.getTitle(),
                task.getType(),
                task.getPriority(),
                task.getAssignedAgentId(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    private TaskDetail toTaskDetail(TaskJpaEntity task) {
        return new TaskDetail(
                task.getId(),
                task.getWorkspace().getId(),
                task.getCreatedByGoogleSub(),
                task.getTitle(),
                task.getDescription(),
                task.getType(),
                task.getPriority(),
                task.getAssignedAgentId(),
                task.getSourceRef(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
