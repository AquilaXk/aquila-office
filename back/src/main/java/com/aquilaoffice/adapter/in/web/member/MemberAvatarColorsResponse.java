package com.aquilaoffice.adapter.in.web.member;

import com.aquilaoffice.application.port.in.MemberAvatarColorsResult;

/**
 * 멤버 프로필 응답에 포함되는 아바타 색상 값이다.
 */
public record MemberAvatarColorsResponse(String skin, String hair, String shirt) {

    static MemberAvatarColorsResponse from(MemberAvatarColorsResult result) {
        return new MemberAvatarColorsResponse(result.skin(), result.hair(), result.shirt());
    }
}

