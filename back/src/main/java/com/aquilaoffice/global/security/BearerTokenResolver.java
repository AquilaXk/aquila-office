package com.aquilaoffice.global.security;

/**
 * Authorization 헤더에서 Bearer access token 문자열을 추출한다.
 */
public class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    public String resolve(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Authorization header must start with Bearer");
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
        return accessToken;
    }
}
