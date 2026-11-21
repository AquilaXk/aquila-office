package com.aquilaoffice.adapter.out.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Google API Client의 ID token verifier를 호출해 서명, issuer, audience, 만료 검증을 수행한다.
 */
@Component
class GoogleApiIdTokenVerifierGateway implements GoogleIdTokenVerifierGateway {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    GoogleApiIdTokenVerifierGateway(GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.googleIdTokenVerifier =
                Objects.requireNonNull(googleIdTokenVerifier, "googleIdTokenVerifier must not be null");
    }

    @Override
    public GoogleIdTokenPayload verify(String idToken) {
        try {
            GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(idToken);
            if (googleIdToken == null) {
                return null;
            }
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            return new GoogleIdTokenPayload(
                    payload.getSubject(),
                    payload.getEmail(),
                    String.valueOf(payload.get("name")),
                    Boolean.TRUE.equals(payload.getEmailVerified()),
                    payload.getHostedDomain());
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalArgumentException("invalid google id token", exception);
        }
    }
}
