package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

/**
 * persistence adapter가 워크스페이스 soft delete 상태 전이를 저장할 때 사용하는 요청값이다.
 */
public record WorkspaceDeletion(Long workspaceId, LocalDateTime deletedAt) {

    public WorkspaceDeletion {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        if (deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
