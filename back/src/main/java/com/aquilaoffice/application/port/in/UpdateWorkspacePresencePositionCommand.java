package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.util.Objects;

/**
 * 현재 사용자의 워크스페이스 presence 위치 갱신 요청을 application 계층으로 전달하는 command다.
 */
public record UpdateWorkspacePresencePositionCommand(
        Long workspaceId,
        String memberGoogleSub,
        WorkspacePresencePosition position,
        String status) {

    public UpdateWorkspacePresencePositionCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        memberGoogleSub = requireNotBlank(memberGoogleSub, "memberGoogleSub");
        position = Objects.requireNonNull(position, "position must not be null");
        status = normalizeStatus(status);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
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

    private static String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 30) {
            throw new IllegalArgumentException("status must be 30 characters or less");
        }
        return trimmed;
    }
}
