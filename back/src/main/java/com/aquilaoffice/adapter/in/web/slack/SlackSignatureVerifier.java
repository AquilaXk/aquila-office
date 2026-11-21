package com.aquilaoffice.adapter.in.web.slack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SlackSignatureVerifier {

    private static final String SIGNATURE_VERSION = "v0";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String signingSecret;
    private final SecretKeySpec signingKeySpec;
    private final Clock clock;
    private final Duration timestampTolerance;

    public SlackSignatureVerifier(
            String signingSecret,
            Clock clock,
            Duration timestampTolerance) {
        this.signingSecret = signingSecret == null ? "" : signingSecret.trim();
        this.signingKeySpec = this.signingSecret.isBlank()
                ? null
                : new SecretKeySpec(this.signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.timestampTolerance = Objects.requireNonNull(timestampTolerance, "timestampTolerance must not be null");
    }

    public void verifyOrThrow(String timestamp, String signature, String rawBody) {
        if (signingKeySpec == null) {
            throw new IllegalStateException("slack signing secret is not configured");
        }
        long requestEpochSecond = parseTimestamp(timestamp);
        long nowEpochSecond = clock.instant().getEpochSecond();
        if (Math.abs(nowEpochSecond - requestEpochSecond) > timestampTolerance.toSeconds()) {
            throw new IllegalArgumentException("slack request timestamp is outside tolerance");
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("slack request signature mismatch");
        }

        String expectedSignature = computeSignature(timestamp, rawBody == null ? "" : rawBody);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("slack request signature mismatch");
        }
    }

    private long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException("slack request timestamp is missing");
        }
        try {
            return Long.parseLong(timestamp.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("slack request timestamp is missing", exception);
        }
    }

    private String computeSignature(String timestamp, String rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKeySpec);
            String baseString = SIGNATURE_VERSION + ":" + timestamp.trim() + ":" + rawBody;
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_VERSION + "=" + HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("slack request signature verification failed", exception);
        }
    }
}
