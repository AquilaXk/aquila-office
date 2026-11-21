package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 Multi-Agent orchestration session 목록 조회 query다.
 */
public record ListOrchestrationSessionsQuery(Long workspaceId, String requesterGoogleSub) {

    public ListOrchestrationSessionsQuery {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive");
        }
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        requesterGoogleSub = requesterGoogleSub.trim();
    }
}
