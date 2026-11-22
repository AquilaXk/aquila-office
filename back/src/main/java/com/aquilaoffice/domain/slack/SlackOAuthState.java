package com.aquilaoffice.domain.slack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Slack OAuth callback에서 workspace와 requester를 복원하기 위한 서명된 state 값이다.
 */
public record SlackOAuthState(
        Long workspaceId,
        String requesterGoogleSub) {

    private static final String VERSION = "v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int STATE_PARTS = 5;
    private static final ThreadLocal<Mac> HMAC_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        try {
            return Mac.getInstance(HMAC_ALGORITHM);
        } catch (Exception exception) {
            throw new IllegalStateException("slack oauth state hmac unavailable", exception);
        }
    });

    public SlackOAuthState {
        workspaceId = requirePositive(workspaceId);
        requesterGoogleSub = requireNotBlank(requesterGoogleSub);
    }

    public static SlackOAuthState parse(String value, String secret, Instant now) {
        String normalizedValue = requireRawState(value);
        String[] parts = normalizedValue.split("\\.", -1);
        if (parts.length != STATE_PARTS || !VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("slack oauth state is invalid");
        }
        String signedPayload = signedPayload(parts[0], parts[1], parts[2], parts[3]);
        validateSignature(signedPayload, parts[4], secret);
        validateExpiresAt(parts[3], now);
        return new SlackOAuthState(parseWorkspaceId(parts[1]), decode(parts[2]));
    }

    public static String serialize(
            Long workspaceId,
            String requesterGoogleSub,
            Instant issuedAt,
            Duration ttl,
            String secret) {
        SlackOAuthState state = new SlackOAuthState(workspaceId, requesterGoogleSub);
        Instant expiresAt = requireIssuedAt(issuedAt).plus(requirePositiveDuration(ttl));
        String requester = encode(state.requesterGoogleSub());
        String expiresAtEpochSecond = String.valueOf(expiresAt.getEpochSecond());
        String signedPayload = signedPayload(VERSION, String.valueOf(state.workspaceId()), requester, expiresAtEpochSecond);
        return signedPayload + "." + sign(signedPayload, secret);
    }

    private static String requireRawState(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("slack oauth state is invalid");
        }
        return value.trim();
    }

    private static String signedPayload(String version, String workspaceId, String requester, String expiresAtEpochSecond) {
        return version + "." + workspaceId + "." + requester + "." + expiresAtEpochSecond;
    }

    private static Long parseWorkspaceId(String value) {
        try {
            return requirePositive(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("slack oauth state workspaceId must be positive", exception);
        }
    }

    private static Long requirePositive(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("slack oauth state workspaceId must be positive");
        }
        return value;
    }

    private static Instant requireIssuedAt(Instant value) {
        if (value == null) {
            throw new IllegalArgumentException("slack oauth state issuedAt must not be null");
        }
        return value;
    }

    private static Duration requirePositiveDuration(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("slack oauth state ttl must be positive");
        }
        return value;
    }

    private static Instant requireNow(Instant value) {
        if (value == null) {
            throw new IllegalArgumentException("slack oauth state now must not be null");
        }
        return value;
    }

    private static String requireNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("slack oauth state requesterGoogleSub must not be blank");
        }
        return value.trim();
    }

    private static String requireSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("slack oauth state secret must not be blank");
        }
        return value.trim();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("slack oauth state is invalid", exception);
        }
    }

    private static String sign(String payload, String secret) {
        try {
            Mac mac = HMAC_THREAD_LOCAL.get();
            mac.init(new SecretKeySpec(requireSecret(secret).getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("slack oauth state signing failed", exception);
        }
    }

    private static void validateSignature(String signedPayload, String signature, String secret) {
        String expectedSignature = sign(signedPayload, secret);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("slack oauth state signature is invalid");
        }
    }

    private static void validateExpiresAt(String expiresAtEpochSecond, Instant now) {
        try {
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(expiresAtEpochSecond));
            if (!expiresAt.isAfter(requireNow(now))) {
                throw new IllegalArgumentException("slack oauth state is expired");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("slack oauth state is invalid", exception);
        }
    }
}
