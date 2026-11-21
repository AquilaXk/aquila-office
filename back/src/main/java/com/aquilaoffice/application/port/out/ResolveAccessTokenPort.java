package com.aquilaoffice.application.port.out;

/**
 * Access token 문자열에서 인증에 필요한 subject 정보를 추출하는 outbound port다.
 */
public interface ResolveAccessTokenPort {

    ResolvedAccessToken resolveAccessToken(String accessToken);
}
