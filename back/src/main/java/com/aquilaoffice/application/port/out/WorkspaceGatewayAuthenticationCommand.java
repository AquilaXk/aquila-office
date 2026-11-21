package com.aquilaoffice.application.port.out;

/**
 * Gateway 장치 인증을 outbound adapter로 전달하는 command다.
 */
public record WorkspaceGatewayAuthenticationCommand(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken) {

    public WorkspaceGatewayAuthenticationCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        gatewayUrl = requireHttpUrl(gatewayUrl);
        gatewayToken = requireNotBlank(gatewayToken, "gatewayToken");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireHttpUrl(String value) {
        String normalizedValue = requireNotBlank(value, "gatewayUrl");
        if (!normalizedValue.startsWith("http://") && !normalizedValue.startsWith("https://")) {
            throw new IllegalArgumentException("gatewayUrl must start with http:// or https://");
        }
        return normalizedValue;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
