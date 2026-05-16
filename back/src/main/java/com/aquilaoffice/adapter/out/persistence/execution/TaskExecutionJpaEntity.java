package com.aquilaoffice.adapter.out.persistence.execution;

import com.aquilaoffice.adapter.out.persistence.task.TaskJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
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
 * Task 실행 단위 기록을 task_executions 테이블에 저장하는 JPA entity다.
 */
@Entity
@Table(name = "task_executions")
public class TaskExecutionJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskJpaEntity task;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskExecutionStatus status;

    @Column(name = "result_summary", length = 1000)
    private String resultSummary;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TaskExecutionJpaEntity() {}

    private TaskExecutionJpaEntity(
            WorkspaceJpaEntity workspace,
            TaskJpaEntity task,
            Long agentId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.workspace = requireWorkspace(workspace);
        this.task = requireTask(task);
        this.agentId = agentId;
        this.status = status;
        this.resultSummary = resultSummary;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TaskExecutionJpaEntity from(
            TaskExecution execution,
            WorkspaceJpaEntity workspace,
            TaskJpaEntity task) {
        LocalDateTime now = LocalDateTime.now();
        return new TaskExecutionJpaEntity(
                workspace,
                task,
                execution.getAgentId(),
                execution.getStatus(),
                execution.getResultSummary(),
                execution.getFailureReason(),
                now,
                now);
    }

    public void updateStatus(TaskExecutionStatus status, String resultSummary, String failureReason) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
        this.resultSummary = normalizeNullable(resultSummary);
        this.failureReason = normalizeNullable(failureReason);
        this.updatedAt = LocalDateTime.now();
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public TaskJpaEntity getTask() {
        return task;
    }

    public Long getAgentId() {
        return agentId;
    }

    public TaskExecutionStatus getStatus() {
        return status;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public String getFailureReason() {
        return failureReason;
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

    private static TaskJpaEntity requireTask(TaskJpaEntity task) {
        if (task == null) {
            throw new IllegalArgumentException("task entity must not be null");
        }
        return task;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
