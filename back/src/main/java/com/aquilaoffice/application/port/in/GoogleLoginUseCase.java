package com.aquilaoffice.application.port.in;

/**
 * Google ID token으로 내부 로그인을 수행하는 inbound port다.
 */
public interface GoogleLoginUseCase {

    GoogleLoginResult login(GoogleLoginCommand command);
}
