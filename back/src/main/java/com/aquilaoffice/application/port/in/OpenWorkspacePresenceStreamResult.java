package com.aquilaoffice.application.port.in;

/**
 * stream 연결 후 web adapter가 종료 callback을 등록할 때 필요한 식별자다.
 */
public record OpenWorkspacePresenceStreamResult(Long workspaceId, Long memberId) {

    public OpenWorkspacePresenceStreamResult {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        memberId = requirePositive(memberId, "memberId");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
