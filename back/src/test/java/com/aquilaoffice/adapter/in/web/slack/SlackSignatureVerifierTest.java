package com.aquilaoffice.adapter.in.web.slack;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class SlackSignatureVerifierTest {

    private static final Instant NOW = Instant.ofEpochSecond(1_715_841_000L);

    @Test
    void verifyOrThrowAcceptsValidSignature() {
        SlackSignatureVerifier verifier = newVerifier("signing-secret");
        String body = "{\"type\":\"event_callback\"}";
        String timestamp = String.valueOf(NOW.getEpochSecond());
        String signature = signature("signing-secret", timestamp, body);

        assertThatCode(() -> verifier.verifyOrThrow(timestamp, signature, body))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyOrThrowRejectsStaleTimestamp() {
        SlackSignatureVerifier verifier = newVerifier("signing-secret");
        String body = "{\"type\":\"event_callback\"}";
        String timestamp = String.valueOf(NOW.minusSeconds(301).getEpochSecond());
        String signature = signature("signing-secret", timestamp, body);

        assertThatThrownBy(() -> verifier.verifyOrThrow(timestamp, signature, body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack request timestamp is outside tolerance");
    }

    @Test
    void verifyOrThrowRejectsSignatureMismatch() {
        SlackSignatureVerifier verifier = newVerifier("signing-secret");

        assertThatThrownBy(() -> verifier.verifyOrThrow(
                        String.valueOf(NOW.getEpochSecond()),
                        "v0=invalid",
                        "{\"type\":\"event_callback\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack request signature mismatch");
    }

    @Test
    void verifyOrThrowRejectsMissingSigningSecret() {
        SlackSignatureVerifier verifier = newVerifier(" ");

        assertThatThrownBy(() -> verifier.verifyOrThrow(
                        String.valueOf(NOW.getEpochSecond()),
                        "v0=invalid",
                        "{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("slack signing secret is not configured");
    }

    private static SlackSignatureVerifier newVerifier(String signingSecret) {
        return new SlackSignatureVerifier(
                signingSecret,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(5));
    }

    private static String signature(String signingSecret, String timestamp, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(("v0:" + timestamp + ":" + body).getBytes(StandardCharsets.UTF_8));
            return "v0=" + hex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("test signature creation failed", exception);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
