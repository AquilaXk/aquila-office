package com.aquilaoffice.adapter.out.auth;

/**
 * Google API Client verifier 호출을 adapter 정책 검증과 분리하기 위한 gateway다.
 */
interface GoogleIdTokenVerifierGateway {

    GoogleIdTokenPayload verify(String idToken);
}
