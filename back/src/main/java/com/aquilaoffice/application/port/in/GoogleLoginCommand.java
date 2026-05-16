package com.aquilaoffice.application.port.in;

/**
 * Google 로그인 요청을 application 계층으로 전달하는 command다.
 */
public record GoogleLoginCommand(String idToken) {

    public GoogleLoginCommand {
        idToken = requireNotBlank(idToken, "idToken");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
