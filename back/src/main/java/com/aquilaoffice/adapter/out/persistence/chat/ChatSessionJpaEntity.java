package com.aquilaoffice.adapter.out.persistence.chat;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.chat.ChatSession;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Workspace chat session을 저장하는 JPA entity다.
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSessionJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ChatSessionJpaEntity() {}

    private ChatSessionJpaEntity(
            WorkspaceJpaEntity workspace,
            String createdByGoogleSub,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.workspace = requireWorkspace(workspace);
        this.createdByGoogleSub = createdByGoogleSub;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChatSessionJpaEntity from(ChatSession session, WorkspaceJpaEntity workspace) {
        LocalDateTime now = LocalDateTime.now();
        return new ChatSessionJpaEntity(workspace, session.getCreatedByGoogleSub(), now, now);
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
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
