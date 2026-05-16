package com.aquilaoffice.domain.chat;

/**
 * Workspace 안에서 이어지는 chat 대화 단위다.
 */
public class ChatSession {

    private final Long id;
    private final Long workspaceId;
    private final String createdByGoogleSub;

    private ChatSession(Long id, Long workspaceId, String createdByGoogleSub) {
        this.id = id;
        this.workspaceId = requireWorkspaceId(workspaceId);
        this.createdByGoogleSub = requireText(createdByGoogleSub, "createdByGoogleSub");
    }

    public static ChatSession create(Long workspaceId, String createdByGoogleSub) {
        return new ChatSession(null, workspaceId, createdByGoogleSub);
    }

    public static ChatSession restore(Long id, Long workspaceId, String createdByGoogleSub) {
        return new ChatSession(id, workspaceId, createdByGoogleSub);
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    private static Long requireWorkspaceId(Long workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return workspaceId;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
