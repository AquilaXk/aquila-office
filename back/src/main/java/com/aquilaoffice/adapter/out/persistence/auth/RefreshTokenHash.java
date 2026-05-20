package com.aquilaoffice.adapter.out.persistence.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * refresh token 원문 대신 저장할 고정 길이 digest를 만든다.
 */
final class RefreshTokenHash {

    private static final String PREFIX = "sha256:";

    private RefreshTokenHash() {}

    static String hash(String refreshToken) {
        byte[] digest = sha256(normalizeRaw(refreshToken).getBytes(StandardCharsets.UTF_8));
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    static String normalizeRaw(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be blank");
        }
        return refreshToken.trim();
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
