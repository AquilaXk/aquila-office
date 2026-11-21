package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.member.MemberRole;

/**
 * 검증된 access token payload다.
 */
public record ResolvedAccessToken(Long memberId, String googleSub, String email, MemberRole role) {

    public ResolvedAccessToken(Long memberId, String email, MemberRole role) {
        this(memberId, null, email, role);
    }

    public ResolvedAccessToken {
        memberId = requirePositive(memberId, "memberId");
        googleSub = normalizeOptional(googleSub);
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

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
