package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 워크스페이스 초대 생성 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record CreateWorkspaceInviteCommand(Long workspaceId, String requesterGoogleSub, String email) {

    public CreateWorkspaceInviteCommand {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        email = requireNotBlank(email, "email");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
