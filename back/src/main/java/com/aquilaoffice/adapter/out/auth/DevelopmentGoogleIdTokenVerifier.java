package com.aquilaoffice.adapter.out.auth;

import com.aquilaoffice.application.port.out.VerifiedGoogleUser;
import com.aquilaoffice.application.port.out.VerifyGoogleIdTokenPort;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 개발 환경에서 Google 로그인 흐름을 검증하기 위한 token verifier다.
 */
@Component
public class DevelopmentGoogleIdTokenVerifier implements VerifyGoogleIdTokenPort {

    @Override
    public VerifiedGoogleUser verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("idToken must not be blank");
        }
        String trimmed = idToken.trim();
        if (trimmed.contains("|")) {
            return parseDelimitedToken(trimmed);
        }

        String localPart = trimmed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-");
        if (localPart.isBlank()) {
            throw new IllegalArgumentException("invalid google id token");
        }
        return new VerifiedGoogleUser(trimmed, localPart + "@example.com", "Google User");
    }

    private VerifiedGoogleUser parseDelimitedToken(String token) {
        String[] parts = token.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid google id token");
        }
        return new VerifiedGoogleUser(parts[0], parts[1], parts[2]);
    }
}
