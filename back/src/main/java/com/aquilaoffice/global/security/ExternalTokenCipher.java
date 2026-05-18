package com.aquilaoffice.global.security;

/**
 * 외부 서비스 credential token을 저장 format과 application 사용값 사이에서 변환한다.
 */
public interface ExternalTokenCipher {

    String encrypt(String plaintextToken);

    String decrypt(String storedToken);
}
