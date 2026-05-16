package com.aquilaoffice.application.port.in;

/**
 * 멤버 프로필 조회/수정 결과를 application 값으로 표현한다.
 */
public record MemberProfileResult(
        Long memberId,
        String displayName,
        String avatarKind,
        MemberAvatarColorsResult avatarColors) {}

