package com.aquilaoffice.application.port.in;

/**
 * 단건 Multi-Agent orchestration session 조회 query다.
 */
public record GetOrchestrationSessionQuery(Long workspaceId, Long sessionId, String requesterGoogleSub) {

    public GetOrchestrationSessionQuery {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive");
        }
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("sessionId must be positive");
        }
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        requesterGoogleSub = requesterGoogleSub.trim();
    }
}
