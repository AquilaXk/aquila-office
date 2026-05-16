package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 GitHub 자격 증명 목록 조회 식별자를 application 계층으로 넘길 때 사용하는 query다.
 */
public record ListGithubCredentialsQuery(Long workspaceId, String requesterGoogleSub) {

    public ListGithubCredentialsQuery {
        workspaceId = requireWorkspaceId(workspaceId);
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
    }

    private static Long requireWorkspaceId(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive");
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
