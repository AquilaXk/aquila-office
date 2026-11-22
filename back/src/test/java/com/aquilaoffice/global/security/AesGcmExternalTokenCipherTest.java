package com.aquilaoffice.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import java.util.Base64;
import java.util.Map;
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
    void supportsKeyRotationRejectsInvalidIvLengthAndBindsContextAad() {
        ExternalTokenEncryptionProperties oldProps = new ExternalTokenEncryptionProperties(
                "old-external-token-key-0123456789abcdef",
                "key-v1");
        AesGcmExternalTokenCipher oldCipher = new AesGcmExternalTokenCipher(oldProps);
        String encryptedWithOldKey = oldCipher.encryptWithContext("sk-provider-secret", "ws:7:provider:openai");

        ExternalTokenEncryptionProperties rotatedProps = new ExternalTokenEncryptionProperties(
                "new-external-token-key-0123456789abcdef",
                "key-v2",
                Map.of("key-v1", "old-external-token-key-0123456789abcdef"));
        AesGcmExternalTokenCipher rotatedCipher = new AesGcmExternalTokenCipher(rotatedProps);

        assertThat(encryptedWithOldKey).startsWith("enc:v2:key-v1:");
        assertThat(rotatedCipher.decryptWithContext(encryptedWithOldKey, "ws:7:provider:openai"))
                .isEqualTo("sk-provider-secret");
        assertThatThrownBy(() -> rotatedCipher.decryptWithContext(encryptedWithOldKey, "ws:8:provider:openai"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rotatedCipher.decrypt(encryptedWithOldKey))
                .isInstanceOf(IllegalArgumentException.class);

        String badIvToken = "enc:v2:key-v1:"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[] {1, 2, 3, 4})
                + ":dGVzdA";
        assertThatThrownBy(() -> rotatedCipher.decryptWithContext(badIvToken, "ws:7:provider:openai"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("external token IV length is invalid");
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
