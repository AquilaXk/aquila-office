package com.aquilaoffice.adapter.in.web.member;

import com.aquilaoffice.application.port.in.MemberProfileResult;

/**
 * 멤버 프로필 조회/수정 응답 DTO다.
 */
public record MemberProfileResponse(
        Long memberId,
        String displayName,
        String avatarKind,
        MemberAvatarColorsResponse avatarColors) {

    static MemberProfileResponse from(MemberProfileResult result) {
        return new MemberProfileResponse(
                result.memberId(),
                result.displayName(),
                result.avatarKind(),
                MemberAvatarColorsResponse.from(result.avatarColors()));
    }
}

