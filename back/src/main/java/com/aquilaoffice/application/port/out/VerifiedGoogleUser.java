package com.aquilaoffice.application.port.out;

/**
 * 검증된 Google 사용자 정보를 application 계층 값으로 표현한다.
 */
public record VerifiedGoogleUser(String googleSub, String email, String name) {

    public VerifiedGoogleUser {
        googleSub = requireNotBlank(googleSub, "googleSub");
        email = requireNotBlank(email, "email");
        name = requireNotBlank(name, "name");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
