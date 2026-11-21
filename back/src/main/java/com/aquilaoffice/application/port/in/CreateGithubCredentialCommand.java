package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 GitHub 자격 증명 등록 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record CreateGithubCredentialCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String displayName,
        String token) {

    public CreateGithubCredentialCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        displayName = requireNotBlank(displayName, "displayName");
        token = requireNotBlank(token, "token");
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
