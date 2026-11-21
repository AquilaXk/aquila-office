package com.aquilaoffice.application.port.in;

/**
 * 요청자의 멤버 프로필 조회 식별자를 application 계층으로 넘기는 query다.
 */
public record GetMemberProfileQuery(String memberGoogleSub) {

    public GetMemberProfileQuery {
        memberGoogleSub = requireNotBlank(memberGoogleSub, "memberGoogleSub");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

