package com.aquilaoffice.application.port.out;

/**
 * persistence adapter가 워크스페이스 멤버 제거를 저장할 때 사용하는 요청값이다.
 */
public record WorkspaceMemberRemoval(Long workspaceId, Long memberId) {

    public WorkspaceMemberRemoval {
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
