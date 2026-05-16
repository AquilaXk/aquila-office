package com.aquilaoffice.application.port.out;

/**
 * Google ID token 검증 구현을 application 계층과 분리하는 outbound port다.
 */
public interface VerifyGoogleIdTokenPort {

    VerifiedGoogleUser verify(String idToken);
}
