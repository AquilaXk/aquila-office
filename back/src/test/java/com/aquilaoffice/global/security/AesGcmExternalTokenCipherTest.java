package com.aquilaoffice.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import org.junit.jupiter.api.Test;

/**
 * 외부 연동 token 저장값을 AES-GCM ciphertext로 변환하는 보안 adapter 계약을 검증한다.
 */
class AesGcmExternalTokenCipherTest {

    @Test
    void encryptReturnsVersionedCiphertextAndDecryptsToken() {
        ExternalTokenEncryptionProperties properties = new ExternalTokenEncryptionProperties(
                "test-external-token-key-0123456789abcdef",
                "test-key");
        AesGcmExternalTokenCipher cipher = new AesGcmExternalTokenCipher(properties);

        String encryptedToken = cipher.encrypt("ghp_1234567890abcdef");

        assertThat(encryptedToken).startsWith("enc:v1:test-key:");
        assertThat(encryptedToken).doesNotContain("ghp_1234567890abcdef");
        assertThat(cipher.decrypt(encryptedToken)).isEqualTo("ghp_1234567890abcdef");
    }

    @Test
    void decryptReturnsLegacyPlaintext() {
        AesGcmExternalTokenCipher cipher = new AesGcmExternalTokenCipher(new ExternalTokenEncryptionProperties(
                "test-external-token-key-0123456789abcdef",
                "test-key"));

        assertThat(cipher.decrypt("legacy-token-value")).isEqualTo("legacy-token-value");
    }

    @Test
    void propertiesRejectShortEncryptionKey() {
        assertThatThrownBy(() -> new ExternalTokenEncryptionProperties("short-key", "test-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("key must be at least 32 bytes for external token encryption");
    }
}
