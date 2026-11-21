package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;

/**
 * inbound adapter가 워크스페이스 초대 목록 조회 식별자와 상태 필터를 application 계층으로 넘길 때 사용하는 query다.
 */
public record ListWorkspaceInvitesQuery(
        Long workspaceId,
        String memberGoogleSub,
        WorkspaceInviteStatus status) {

    public ListWorkspaceInvitesQuery {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        memberGoogleSub = requireNotBlank(memberGoogleSub, "memberGoogleSub");
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
}
