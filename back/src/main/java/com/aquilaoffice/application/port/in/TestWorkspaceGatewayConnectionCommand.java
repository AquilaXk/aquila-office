package com.aquilaoffice.application.port.in;

/**
 * gateway URL/token 연결 테스트 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record TestWorkspaceGatewayConnectionCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String gatewayUrl,
        String token) {

    public TestWorkspaceGatewayConnectionCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        gatewayUrl = requireHttpUrl(gatewayUrl);
        token = requireNotBlank(token, "token");
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
