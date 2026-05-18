package com.aquilaoffice.adapter.out.auth;

import com.aquilaoffice.application.port.out.VerifiedGoogleUser;
import com.aquilaoffice.application.port.out.VerifyGoogleIdTokenPort;
import com.aquilaoffice.global.config.properties.GoogleOAuthProperties;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Google ID token 검증 결과에 서비스 정책을 적용해 application 사용자 값으로 변환한다.
 */
@Component
public class GoogleIdTokenVerifierAdapter implements VerifyGoogleIdTokenPort {

    private final GoogleIdTokenVerifierGateway googleIdTokenVerifierGateway;
    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleIdTokenVerifierAdapter(
            GoogleIdTokenVerifierGateway googleIdTokenVerifierGateway,
            GoogleOAuthProperties googleOAuthProperties) {
        this.googleIdTokenVerifierGateway = Objects.requireNonNull(
                googleIdTokenVerifierGateway,
                "googleIdTokenVerifierGateway must not be null");
        this.googleOAuthProperties =
                Objects.requireNonNull(googleOAuthProperties, "googleOAuthProperties must not be null");
    }

    @Override
    public VerifiedGoogleUser verify(String idToken) {
        String normalizedToken = requireNotBlank(idToken, "idToken");
        GoogleIdTokenPayload payload = googleIdTokenVerifierGateway.verify(normalizedToken);
        if (payload == null) {
            throw new IllegalArgumentException("invalid google id token");
        }
        if (googleOAuthProperties.requireEmailVerified() && !payload.emailVerified()) {
            throw new IllegalArgumentException("google email is not verified");
        }
        validateHostedDomain(payload.hostedDomain());
        return new VerifiedGoogleUser(
                requireNotBlank(payload.subject(), "googleSub"),
                requireNotBlank(payload.email(), "email"),
                requireNotBlank(payload.name(), "name"));
    }

    private void validateHostedDomain(String hostedDomain) {
        if (googleOAuthProperties.allowedHostedDomains().isEmpty()) {
            return;
        }
        String normalizedHostedDomain = requireNotBlank(hostedDomain, "hostedDomain").toLowerCase(Locale.ROOT);
        boolean allowed = googleOAuthProperties.allowedHostedDomains().stream()
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedHostedDomain::equals);
        if (!allowed) {
            throw new IllegalArgumentException("google hosted domain is not allowed");
        }
    }

    private String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
