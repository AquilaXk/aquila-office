package com.aquilaoffice.global.security;

import com.aquilaoffice.domain.member.MemberRole;

/**
 * SecurityContext에 저장되는 인증 회원 정보다.
 */
public record AuthenticatedMember(Long memberId, String googleSub, String email, MemberRole role) {

    public AuthenticatedMember {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be positive");
        }
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("googleSub must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        googleSub = googleSub.trim();
        email = email.trim();
    }
}
