package com.aquilaoffice.application.port.in;

import java.util.regex.Pattern;

/**
 * 요청자 자신의 멤버 프로필 부분 수정 값을 application 계층으로 넘기는 command다.
 */
public record UpdateMemberProfileCommand(
        String memberGoogleSub,
        String displayName,
        String avatarKind,
        String skinColor,
        String hairColor,
        String shirtColor) {

    private static final int MAX_DISPLAY_NAME_LENGTH = 100;
    private static final int MAX_AVATAR_KIND_LENGTH = 50;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public UpdateMemberProfileCommand {
        memberGoogleSub = requireNotBlank(memberGoogleSub, "memberGoogleSub");
        displayName = normalizeOptionalText(displayName, "displayName", MAX_DISPLAY_NAME_LENGTH);
        avatarKind = normalizeOptionalText(avatarKind, "avatarKind", MAX_AVATAR_KIND_LENGTH);
        skinColor = normalizeOptionalColor(skinColor);
        hairColor = normalizeOptionalColor(hairColor);
        shirtColor = normalizeOptionalColor(shirtColor);
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalStateException(fieldName + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalStateException(fieldName + " must be less than or equal to " + maxLength);
        }
        return trimmed;
    }

    private static String normalizeOptionalColor(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!HEX_COLOR_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalStateException("avatar color must be #RRGGBB");
        }
        return trimmed;
    }
}

