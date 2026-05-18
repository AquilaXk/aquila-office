package com.aquilaoffice.adapter.out.persistence.execution;

import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.domain.execution.TaskExecutionLogLevel;
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
 * Task 실행 중 발생한 lifecycle log를 task_execution_logs 테이블에 저장한다.
 */
@Entity
@Table(name = "task_execution_logs")
public class TaskExecutionLogJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private TaskExecutionJpaEntity execution;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private TaskExecutionLogLevel level;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TaskExecutionLogJpaEntity() {}

    private TaskExecutionLogJpaEntity(
            TaskExecutionJpaEntity execution,
            Long agentId,
            TaskExecutionLogLevel level,
            String message,
            LocalDateTime createdAt) {
        this.execution = requireExecution(execution);
        this.agentId = requirePositive(agentId, "agentId");
        this.level = requireLevel(level);
        this.message = requireMessage(message);
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public static TaskExecutionLogJpaEntity from(
            TaskExecutionLogCommand command,
            TaskExecutionJpaEntity execution) {
        return new TaskExecutionLogJpaEntity(
                execution,
                command.agentId(),
                toLevel(command.level()),
                command.message(),
                LocalDateTime.now());
    }

    public TaskExecutionJpaEntity getExecution() {
        return execution;
    }

    public Long getAgentId() {
        return agentId;
    }

    public String getLevel() {
        return level.name();
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static TaskExecutionJpaEntity requireExecution(TaskExecutionJpaEntity execution) {
        if (execution == null) {
            throw new IllegalArgumentException("task execution entity must not be null");
        }
        return execution;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static TaskExecutionLogLevel requireLevel(TaskExecutionLogLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        return level;
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message.trim();
    }

    private static TaskExecutionLogLevel toLevel(String level) {
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("level must not be blank");
        }
        return TaskExecutionLogLevel.valueOf(level.trim().toUpperCase());
    }
}
