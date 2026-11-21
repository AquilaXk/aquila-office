package com.aquilaoffice.adapter.out.persistence.orchestration;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestration session과 step graph의 상위 정보를 저장하는 JPA entity다.
 */
@Entity
@Table(name = "orchestration_sessions")
public class OrchestrationSessionJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "source_task_id", nullable = false)
    private Long sourceTaskId;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrchestrationSessionStatus status;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<OrchestrationStepJpaEntity> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected OrchestrationSessionJpaEntity() {}

    private OrchestrationSessionJpaEntity(
            WorkspaceJpaEntity workspace,
            Long sourceTaskId,
            String createdByGoogleSub,
            OrchestrationSessionStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.workspace = workspace;
        this.sourceTaskId = sourceTaskId;
        this.createdByGoogleSub = createdByGoogleSub;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrchestrationSessionJpaEntity from(
            OrchestrationSession session,
            WorkspaceJpaEntity workspace) {
        LocalDateTime now = LocalDateTime.now();
        OrchestrationSessionJpaEntity entity = new OrchestrationSessionJpaEntity(
                workspace,
                session.getSourceTaskId(),
                session.getCreatedByGoogleSub(),
                session.getStatus(),
                now,
                now);
        for (int index = 0; index < session.getSteps().size(); index++) {
            entity.addStep(OrchestrationStepJpaEntity.from(session.getSteps().get(index), entity, index));
        }
        return entity;
    }

    public void updateStatus(OrchestrationSessionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void addStep(OrchestrationStepJpaEntity step) {
        steps.add(step);
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public Long getSourceTaskId() {
        return sourceTaskId;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public OrchestrationSessionStatus getStatus() {
        return status;
    }

    public List<OrchestrationStepJpaEntity> getSteps() {
        return steps;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
