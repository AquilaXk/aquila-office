package com.aquilaoffice.application.port.in;

/**
 * Slack channel mapping 목록 조회 query다.
 */
public record ListSlackChannelMappingsQuery(Long workspaceId, String requesterGoogleSub) {

    public ListSlackChannelMappingsQuery {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        requesterGoogleSub = requesterGoogleSub.trim();
    }
}
