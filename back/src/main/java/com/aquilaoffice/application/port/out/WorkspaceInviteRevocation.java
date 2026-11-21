package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

/**
 * persistence adapter가 초대 취소 상태 전이를 저장할 때 사용하는 요청값이다.
 */
public record WorkspaceInviteRevocation(Long workspaceId, Long inviteId, LocalDateTime revokedAt) {

    public WorkspaceInviteRevocation {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        inviteId = requirePositive(inviteId, "inviteId");
        if (revokedAt == null) {
            throw new IllegalArgumentException("revokedAt must not be null");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
