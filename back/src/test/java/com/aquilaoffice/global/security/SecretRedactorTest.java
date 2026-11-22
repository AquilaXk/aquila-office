package com.aquilaoffice.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretRedactorTest {

    @Test
    void redactsBearerProviderKeysJwtAndUrlCredentialsInSinglePass() {
        String raw = "Authorization: Bearer secret-bearer-123 failed with sk-ant-api03-abcdef123456, "
                + "sk-proj-abcdef1234567890, AIzaSyDummyKey1234567890, ghp_1234567890abcdef, "
                + "xoxb-1234567890-abcdef, jwt=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.signaturepart, "
                + "url=https://admin:topsecret@proxy.example.com/v1";

        String redacted = SecretRedactor.redactText(raw);

        assertThat(redacted)
                .doesNotContain("secret-bearer-123")
                .doesNotContain("sk-ant-api03")
                .doesNotContain("sk-proj-")
                .doesNotContain("AIzaSyDummyKey")
                .doesNotContain("ghp_1234567890abcdef")
                .doesNotContain("xoxb-1234567890")
                .doesNotContain("eyJhbGciOiJIUzI1NiJ9")
                .doesNotContain("topsecret")
                .contains("Bearer [REDACTED]")
                .contains("https://[REDACTED]@proxy.example.com/v1");
    }

    @Test
    void redactThrowableCapsLengthAndRedactsCredentials() {
        RuntimeException exception = new IllegalStateException("Auth Bearer my-secret-token-value failed: " + "x".repeat(400));

        String redacted = SecretRedactor.redactThrowable(exception, 120);

        assertThat(redacted).hasSize(120);
        assertThat(redacted).doesNotContain("my-secret-token-value");
        assertThat(redacted).contains("Bearer [REDACTED]");
    }
}
