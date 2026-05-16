package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

/**
 * persistence adapter에 전달할 워크스페이스 초대 연장 요청 값이다.
 */
public record WorkspaceInviteExtension(
        Long workspaceId,
        Long inviteId,
        int additionalDays,
        LocalDateTime extendedAt) {

    public WorkspaceInviteExtension {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        inviteId = requirePositive(inviteId, "inviteId");
        if (additionalDays < 1 || additionalDays > 30) {
            throw new IllegalArgumentException("additionalDays must be between 1 and 30");
        }
        if (extendedAt == null) {
            throw new IllegalArgumentException("extendedAt must not be null");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}

