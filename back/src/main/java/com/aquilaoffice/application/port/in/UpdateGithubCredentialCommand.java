package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 GitHub 자격 증명 수정 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record UpdateGithubCredentialCommand(
        Long workspaceId,
        Long credentialId,
        String requesterGoogleSub,
        String displayName,
        String token) {

    public UpdateGithubCredentialCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        credentialId = requirePositive(credentialId, "credentialId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        displayName = normalizeNullable(displayName);
        token = normalizeNullable(token);
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

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}
