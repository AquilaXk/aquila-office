package com.aquilaoffice.adapter.out.persistence.chat;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.chat.ChatMessage;
import com.aquilaoffice.domain.chat.ChatMessageRole;
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
 * Chat session에 속한 message를 저장하는 JPA entity다.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessageJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSessionJpaEntity chatSession;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_execution_id")
    private Long taskExecutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private ChatMessageRole role;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ChatMessageJpaEntity() {}

    private ChatMessageJpaEntity(
            WorkspaceJpaEntity workspace,
            ChatSessionJpaEntity chatSession,
            Long taskId,
            Long taskExecutionId,
            ChatMessageRole role,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.workspace = requireWorkspace(workspace);
        this.chatSession = requireChatSession(chatSession);
        this.taskId = taskId;
        this.taskExecutionId = taskExecutionId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChatMessageJpaEntity from(
            ChatMessage message,
            WorkspaceJpaEntity workspace,
            ChatSessionJpaEntity chatSession) {
        LocalDateTime now = LocalDateTime.now();
        return new ChatMessageJpaEntity(
                workspace,
                chatSession,
                message.getTaskId(),
                message.getTaskExecutionId(),
                message.getRole(),
                message.getContent(),
                now,
                now);
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public ChatSessionJpaEntity getChatSession() {
        return chatSession;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getTaskExecutionId() {
        return taskExecutionId;
    }

    public ChatMessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
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

    private static ChatSessionJpaEntity requireChatSession(ChatSessionJpaEntity chatSession) {
        if (chatSession == null) {
            throw new IllegalArgumentException("chat session entity must not be null");
        }
        return chatSession;
    }
}
