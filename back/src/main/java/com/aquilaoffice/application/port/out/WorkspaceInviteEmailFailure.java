package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

/**
 * 초대 메일 발송 실패를 invite row에 기록하기 위한 값 객체다.
 */
public record WorkspaceInviteEmailFailure(
        Long inviteId,
        Long workspaceId,
        String reason,
        LocalDateTime failedAt) {

    public WorkspaceInviteEmailFailure {
        inviteId = requirePositive(inviteId, "inviteId");
        workspaceId = requirePositive(workspaceId, "workspaceId");
        reason = requireNotBlank(reason, "reason");
        if (failedAt == null) {
            throw new IllegalArgumentException("failedAt must not be null");
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
