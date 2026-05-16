package com.aquilaoffice.global.security;

import com.aquilaoffice.domain.member.MemberRole;

/**
 * SecurityContext에 저장되는 인증 회원 정보다.
 */
public record AuthenticatedMember(Long memberId, String email, MemberRole role) {}
