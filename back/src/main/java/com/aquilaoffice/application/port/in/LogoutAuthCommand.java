package com.aquilaoffice.application.port.in;

/**
 * 로그아웃 요청을 application 계층으로 전달하는 command다.
 */
public record LogoutAuthCommand(String refreshToken) {

    public LogoutAuthCommand {
        refreshToken = requireNotBlank(refreshToken, "refreshToken");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
