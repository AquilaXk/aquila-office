package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 Agent 목록 조회 query다.
 */
public record ListWorkspaceAgentsQuery(Long workspaceId, String requesterGoogleSub) {

    public ListWorkspaceAgentsQuery {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
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
