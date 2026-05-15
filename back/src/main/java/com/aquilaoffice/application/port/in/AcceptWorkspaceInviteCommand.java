package com.aquilaoffice.application.port.in;

/**
 * 초대 수락 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record AcceptWorkspaceInviteCommand(String token, String memberGoogleSub) {

    public AcceptWorkspaceInviteCommand {
        token = requireNotBlank(token, "token");
        memberGoogleSub = requireNotBlank(memberGoogleSub, "memberGoogleSub");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
