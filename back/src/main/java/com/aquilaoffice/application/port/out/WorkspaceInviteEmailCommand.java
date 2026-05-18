package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

/**
 * 초대 메일 본문을 구성하는 데 필요한 workspace invite 발송 정보다.
 */
public record WorkspaceInviteEmailCommand(
        Long inviteId,
        Long workspaceId,
        String workspaceName,
        String email,
        String token,
        LocalDateTime expiresAt) {

    public WorkspaceInviteEmailCommand {
        inviteId = requirePositive(inviteId, "inviteId");
        workspaceId = requirePositive(workspaceId, "workspaceId");
        workspaceName = requireNotBlank(workspaceName, "workspaceName");
        email = requireNotBlank(email, "email");
        token = requireNotBlank(token, "token");
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        if (value < 1) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
