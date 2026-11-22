package com.aquilaoffice.global.security;

/**
 * 외부 연동 자격증명을 암·복호화한다. context 기반 호출은 AAD에 용도·테넌트 경계를 함께 묶는다.
 */
public interface ExternalTokenCipher {

    String encrypt(String plaintextToken);

    String decrypt(String storedToken);

    default String encryptWithContext(String plaintextToken, String context) {
        return encrypt(plaintextToken);
    }

    default String decryptWithContext(String storedToken, String context) {
        return decrypt(storedToken);
    }
}
