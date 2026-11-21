package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 워크스페이스 생성 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record CreateWorkspaceCommand(String name, String description, String creatorGoogleSub) {

    public CreateWorkspaceCommand {
        creatorGoogleSub = requireNotBlank(creatorGoogleSub, "creatorGoogleSub");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
