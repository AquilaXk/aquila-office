package com.aquilaoffice.adapter.out.persistence.agent;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Agent skill 파일과 동기화 상태를 저장하는 JPA entity다.
 */
@Entity
@Table(name = "workspace_agent_skill_files")
public class WorkspaceAgentSkillFileJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private WorkspaceAgentJpaEntity agent;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private WorkspaceAgentSkillFileSyncStatus syncStatus;

    @Column(name = "sync_error", length = 1000)
    private String syncError;

    protected WorkspaceAgentSkillFileJpaEntity() {}

    private WorkspaceAgentSkillFileJpaEntity(
            WorkspaceAgentJpaEntity agent,
            String fileName,
            String content,
            WorkspaceAgentSkillFileSyncStatus syncStatus,
            String syncError) {
        this.agent = requireAgent(agent);
        this.fileName = fileName;
        this.content = content;
        this.syncStatus = syncStatus;
        this.syncError = syncError;
    }

    public static WorkspaceAgentSkillFileJpaEntity from(
            WorkspaceAgentSkillFile skillFile,
            WorkspaceAgentJpaEntity agent) {
        return new WorkspaceAgentSkillFileJpaEntity(
                agent,
                skillFile.getFileName(),
                skillFile.getContent(),
                skillFile.getSyncStatus(),
                skillFile.getSyncError());
    }

    public WorkspaceAgentJpaEntity getAgent() {
        return agent;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }

    public WorkspaceAgentSkillFileSyncStatus getSyncStatus() {
        return syncStatus;
    }

    public String getSyncError() {
        return syncError;
    }

    private static WorkspaceAgentJpaEntity requireAgent(WorkspaceAgentJpaEntity agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent entity must not be null");
        }
        return agent;
    }
}
