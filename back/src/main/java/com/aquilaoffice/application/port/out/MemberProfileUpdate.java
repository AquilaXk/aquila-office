package com.aquilaoffice.application.port.out;

/**
 * persistence adapter에 전달할 멤버 프로필 부분 수정 값이다.
 */
public record MemberProfileUpdate(
        String googleSub,
        String displayName,
        String avatarKind,
        String skinColor,
        String hairColor,
        String shirtColor) {}

