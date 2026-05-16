package com.aquilaoffice.application.port.in;

/**
 * presence stream 연결 요청을 application 계층으로 전달하는 command다.
 */
public record OpenWorkspacePresenceStreamCommand(Long workspaceId, String memberGoogleSub) {

    public OpenWorkspacePresenceStreamCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        memberGoogleSub = requireNotBlank(memberGoogleSub, "memberGoogleSub");
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
