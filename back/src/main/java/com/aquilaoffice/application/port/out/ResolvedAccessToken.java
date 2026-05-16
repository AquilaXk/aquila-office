package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.member.MemberRole;

/**
 * 검증된 access token payload다.
 */
public record ResolvedAccessToken(Long memberId, String email, MemberRole role) {

    public ResolvedAccessToken {
        memberId = requirePositive(memberId, "memberId");
        email = requireNotBlank(email, "email");
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
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
