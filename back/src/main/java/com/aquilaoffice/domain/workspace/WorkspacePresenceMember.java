package com.aquilaoffice.domain.workspace;

/**
 * presence snapshot에 포함되는 워크스페이스 멤버 상태다.
 */
public record WorkspacePresenceMember(Long memberId, WorkspacePresencePosition position, String status) {

    public WorkspacePresenceMember {
        memberId = requirePositive(memberId, "memberId");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
