package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 워크스페이스 목록 조회 식별자를 application 계층으로 넘길 때 사용하는 query다.
 */
public record ListWorkspacesQuery(String memberGoogleSub) {

    public ListWorkspacesQuery {
        memberGoogleSub = requireNotBlank(memberGoogleSub, "memberGoogleSub");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
