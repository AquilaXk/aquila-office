package com.aquilaoffice.adapter.out.auth;

/**
 * Google verifier가 검증한 ID token payload 중 application에서 필요한 값이다.
 */
record GoogleIdTokenPayload(
        String subject,
        String email,
        String name,
        boolean emailVerified,
        String hostedDomain) {}
