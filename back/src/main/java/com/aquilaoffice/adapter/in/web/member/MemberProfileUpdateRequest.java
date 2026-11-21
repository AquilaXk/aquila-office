package com.aquilaoffice.adapter.in.web.member;

import com.aquilaoffice.application.port.in.UpdateMemberProfileCommand;

/**
 * 멤버 프로필 부분 수정 HTTP 요청 body다.
 */
public record MemberProfileUpdateRequest(
        String displayName,
        String avatarKind,
        MemberAvatarColorsRequest avatarColors) {

    UpdateMemberProfileCommand toCommand(String memberGoogleSub) {
        return new UpdateMemberProfileCommand(
                memberGoogleSub,
                displayName,
                avatarKind,
                avatarColors == null ? null : avatarColors.skin(),
                avatarColors == null ? null : avatarColors.hair(),
                avatarColors == null ? null : avatarColors.shirt());
    }
}
