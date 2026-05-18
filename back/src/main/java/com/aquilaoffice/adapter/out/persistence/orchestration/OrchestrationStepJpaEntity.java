package com.aquilaoffice.adapter.out.persistence.orchestration;

import com.aquilaoffice.domain.orchestration.OrchestrationStep;
import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * Orchestration step과 dependsOn key 목록을 저장하는 JPA entity다.
 */
@Entity
@Table(name = "orchestration_steps")
public class OrchestrationStepJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private OrchestrationSessionJpaEntity session;

    @Column(name = "step_key", nullable = false, length = 80)
    private String stepKey;

    @Column(name = "instruction", nullable = false, length = 1200)
    private String instruction;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "depends_on", length = 1000)
    private String dependsOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrchestrationStepStatus status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected OrchestrationStepJpaEntity() {}

    private OrchestrationStepJpaEntity(
            OrchestrationSessionJpaEntity session,
            String stepKey,
            String instruction,
            Long agentId,
            Long taskId,
            String dependsOn,
            OrchestrationStepStatus status,
            int sortOrder) {
        this.session = session;
        this.stepKey = stepKey;
        this.instruction = instruction;
        this.agentId = agentId;
        this.taskId = taskId;
        this.dependsOn = dependsOn;
        this.status = status;
        this.sortOrder = sortOrder;
    }

    public static OrchestrationStepJpaEntity from(
            OrchestrationStep step,
            OrchestrationSessionJpaEntity session,
            int sortOrder) {
        return new OrchestrationStepJpaEntity(
                session,
                step.getStepKey(),
                step.getInstruction(),
                step.getAgentId(),
                step.getTaskId(),
                serializeDependsOn(step.getDependsOn()),
                step.getStatus(),
                sortOrder);
    }

    public void updateTask(Long taskId, OrchestrationStepStatus status) {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.taskId = taskId;
        this.status = status;
    }

    public OrchestrationSessionJpaEntity getSession() {
        return session;
    }

    public String getStepKey() {
        return stepKey;
    }

    public String getInstruction() {
        return instruction;
    }

    public Long getAgentId() {
        return agentId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public List<String> getDependsOn() {
        if (dependsOn == null || dependsOn.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dependsOn.split("\\n"))
                .filter(value -> !value.isBlank())
                .toList();
    }

    public OrchestrationStepStatus getStatus() {
        return status;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    private static String serializeDependsOn(List<String> dependsOn) {
        if (dependsOn == null || dependsOn.isEmpty()) {
            return null;
        }
        return String.join("\n", dependsOn);
    }
}
