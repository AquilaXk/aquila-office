package com.aquilaoffice.adapter.out.persistence.agent;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
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
import java.time.ZoneOffset;

/**
 * 워크스페이스 Agent 메타데이터를 workspace_agents 테이블에 저장하는 JPA entity다.
 */
@Entity
@Table(name = "workspace_agents")
public class WorkspaceAgentJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private WorkspaceAgentCategory category;

    @Column(name = "open_claw_agent_id", length = 160)
    private String openClawAgentId;

    @Column(name = "workspace_path", nullable = false, length = 512)
    private String workspacePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WorkspaceAgentStatus status;

    @Column(name = "sync_error", length = 1000)
    private String syncError;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected WorkspaceAgentJpaEntity() {}

    private WorkspaceAgentJpaEntity(
            WorkspaceJpaEntity workspace,
            String name,
            WorkspaceAgentCategory category,
            String openClawAgentId,
            String workspacePath,
            WorkspaceAgentStatus status,
            String syncError,
            String createdByGoogleSub,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.workspace = requireWorkspace(workspace);
        this.name = name;
        this.category = category;
        this.openClawAgentId = openClawAgentId;
        this.workspacePath = workspacePath;
        this.status = status;
        this.syncError = syncError;
        this.createdByGoogleSub = createdByGoogleSub;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WorkspaceAgentJpaEntity from(WorkspaceAgent agent, WorkspaceJpaEntity workspace) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return new WorkspaceAgentJpaEntity(
                workspace,
                agent.getName(),
                agent.getCategory(),
                agent.getOpenClawAgentId(),
                agent.getWorkspacePath(),
                agent.getStatus(),
                agent.getSyncError(),
                agent.getCreatedByGoogleSub(),
                now,
                now);
    }

    public void update(
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            WorkspaceAgentStatus status,
            String syncError) {
        this.name = name;
        this.category = category;
        this.workspacePath = workspacePath;
        this.status = status;
        this.syncError = syncError;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void disable() {
        this.status = WorkspaceAgentStatus.DISABLED;
        this.syncError = null;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public String getName() {
        return name;
    }

    public WorkspaceAgentCategory getCategory() {
        return category;
    }

    public String getOpenClawAgentId() {
        return openClawAgentId;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public WorkspaceAgentStatus getStatus() {
        return status;
    }

    public String getSyncError() {
        return syncError;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
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
