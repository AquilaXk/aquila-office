package com.aquilaoffice.application.port.in;

/**
 * Slack 수동 연동 삭제 command다.
 */
public record DeleteSlackIntegrationCommand(
        Long workspaceId,
        String requesterGoogleSub,
        Long integrationId) {

    public DeleteSlackIntegrationCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        integrationId = requirePositive(integrationId, "integrationId");
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
