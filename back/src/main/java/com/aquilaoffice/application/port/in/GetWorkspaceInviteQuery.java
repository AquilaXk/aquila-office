package com.aquilaoffice.application.port.in;

/**
 * 초대 token 조회 요청을 application 계층으로 넘길 때 사용하는 query다.
 */
public record GetWorkspaceInviteQuery(String token) {

    public GetWorkspaceInviteQuery {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        token = token.trim();
    }
}
