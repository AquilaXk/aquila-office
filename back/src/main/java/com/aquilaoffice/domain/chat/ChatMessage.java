package com.aquilaoffice.domain.chat;

/**
 * Chat session에 저장되는 단일 message다.
 */
public class ChatMessage {

    private final Long id;
    private final Long workspaceId;
    private final Long chatSessionId;
    private final Long taskId;
    private final Long taskExecutionId;
    private final ChatMessageRole role;
    private final String content;

    private ChatMessage(
            Long id,
            Long workspaceId,
            Long chatSessionId,
            Long taskId,
            Long taskExecutionId,
            ChatMessageRole role,
            String content) {
        this.id = id;
        this.workspaceId = requireWorkspaceId(workspaceId);
        this.chatSessionId = requireChatSessionId(chatSessionId);
        this.taskId = taskId;
        this.taskExecutionId = taskExecutionId;
        this.role = requireRole(role);
        this.content = requireContent(content);
    }

    public static ChatMessage create(
            Long workspaceId,
            Long chatSessionId,
            Long taskId,
            Long taskExecutionId,
            ChatMessageRole role,
            String content) {
        return new ChatMessage(null, workspaceId, chatSessionId, taskId, taskExecutionId, role, content);
    }

    public static ChatMessage restore(
            Long id,
            Long workspaceId,
            Long chatSessionId,
            Long taskId,
            Long taskExecutionId,
            ChatMessageRole role,
            String content) {
        return new ChatMessage(id, workspaceId, chatSessionId, taskId, taskExecutionId, role, content);
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getChatSessionId() {
        return chatSessionId;
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

    private static Long requireWorkspaceId(Long workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return workspaceId;
    }

    private static Long requireChatSessionId(Long chatSessionId) {
        if (chatSessionId == null) {
            throw new IllegalArgumentException("chatSessionId must not be null");
        }
        return chatSessionId;
    }

    private static ChatMessageRole requireRole(ChatMessageRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return role;
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        return content.trim();
    }
}
