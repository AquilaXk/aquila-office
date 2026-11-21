package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;

/**
 * persistence adapter가 워크스페이스 멤버 역할 변경을 저장할 때 사용하는 요청값이다.
 */
public record WorkspaceMemberRoleChange(Long workspaceId, Long memberId, WorkspaceMemberRole role) {

    public WorkspaceMemberRoleChange {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        memberId = requirePositive(memberId, "memberId");
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
