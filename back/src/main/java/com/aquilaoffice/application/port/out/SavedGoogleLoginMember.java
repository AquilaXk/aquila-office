package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.member.MemberRole;

/**
 * Google 로그인으로 저장/갱신된 회원 결과다.
 */
public record SavedGoogleLoginMember(
        Long memberId,
        String googleSub,
        String email,
        String name,
        MemberRole role) {

    public SavedGoogleLoginMember {
        memberId = requirePositive(memberId, "memberId");
        googleSub = requireNotBlank(googleSub, "googleSub");
        email = requireNotBlank(email, "email");
        name = requireNotBlank(name, "name");
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
