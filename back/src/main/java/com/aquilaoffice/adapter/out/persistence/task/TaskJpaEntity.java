package com.aquilaoffice.adapter.out.persistence.task;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 워크스페이스 Task를 tasks 테이블에 저장하는 JPA entity다.
 */
@Entity
@Table(name = "tasks")
public class TaskJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 40)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;

    @Column(name = "source_ref", length = 240)
    private String sourceRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TaskJpaEntity() {}

    private TaskJpaEntity(
            WorkspaceJpaEntity workspace,
            String createdByGoogleSub,
            String title,
            String description,
            TaskType type,
            TaskPriority priority,
            Long assignedAgentId,
            String sourceRef,
            TaskStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.workspace = requireWorkspace(workspace);
        this.createdByGoogleSub = createdByGoogleSub;
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.assignedAgentId = assignedAgentId;
        this.sourceRef = sourceRef;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TaskJpaEntity from(Task task, WorkspaceJpaEntity workspace) {
        LocalDateTime now = LocalDateTime.now();
        return new TaskJpaEntity(
                workspace,
                task.getCreatedByGoogleSub(),
                task.getTitle(),
                task.getDescription(),
                task.getType(),
                task.getPriority(),
                task.getAssignedAgentId(),
                task.getSourceRef(),
                task.getStatus(),
                now,
                now);
    }

    public void updateStatus(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskType getType() {
        return type;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Long getAssignedAgentId() {
        return assignedAgentId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private static WorkspaceJpaEntity requireWorkspace(WorkspaceJpaEntity workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace entity must not be null");
        }
        return workspace;
    }
}
