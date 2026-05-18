package com.aquilaoffice.adapter.out.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.VerifiedGoogleUser;
import com.aquilaoffice.global.config.properties.GoogleOAuthProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Google ID token adapter가 검증된 payload만 application 사용자 값으로 변환하는지 확인한다.
 */
class GoogleIdTokenVerifierAdapterTest {

    @Test
    void verifiesGoogleIdTokenAndReturnsUserProfile() {
        FakeGoogleIdTokenVerifierGateway gateway = new FakeGoogleIdTokenVerifierGateway(payload(
                "google-1",
                "user@example.com",
                "User One",
                true,
                "example.com"));
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(
                gateway,
                properties(List.of("example.com"), true));

        VerifiedGoogleUser user = adapter.verify(" id-token ");

        assertThat(gateway.requestedIdToken).isEqualTo("id-token");
        assertThat(user.googleSub()).isEqualTo("google-1");
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.name()).isEqualTo("User One");
    }

    @Test
    void rejectsInvalidGoogleIdToken() {
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(
                new FakeGoogleIdTokenVerifierGateway(null),
                properties(List.of(), true));

        assertThatThrownBy(() -> adapter.verify("bad-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid google id token");
    }

    @Test
    void rejectsUnverifiedEmailWhenPolicyRequiresVerifiedEmail() {
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(
                new FakeGoogleIdTokenVerifierGateway(payload(
                        "google-2",
                        "user@example.com",
                        "User Two",
                        false,
                        "example.com")),
                properties(List.of(), true));

        assertThatThrownBy(() -> adapter.verify("id-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("google email is not verified");
    }

    @Test
    void rejectsHostedDomainOutsideAllowlist() {
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(
                new FakeGoogleIdTokenVerifierGateway(payload(
                        "google-3",
                        "user@other.com",
                        "User Three",
                        true,
                        "other.com")),
                properties(List.of("example.com"), true));

        assertThatThrownBy(() -> adapter.verify("id-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("google hosted domain is not allowed");
    }

    private static GoogleOAuthProperties properties(List<String> allowedHostedDomains, boolean requireEmailVerified) {
        return new GoogleOAuthProperties(
                List.of("client.apps.googleusercontent.com"),
                allowedHostedDomains,
                requireEmailVerified,
                "aquila-office");
    }

    private static GoogleIdTokenPayload payload(
            String subject,
            String email,
            String name,
            boolean emailVerified,
            String hostedDomain) {
        return new GoogleIdTokenPayload(subject, email, name, emailVerified, hostedDomain);
    }

    private static class FakeGoogleIdTokenVerifierGateway implements GoogleIdTokenVerifierGateway {

        private final GoogleIdTokenPayload payload;
        private String requestedIdToken;

        private FakeGoogleIdTokenVerifierGateway(GoogleIdTokenPayload payload) {
            this.payload = payload;
        }

        @Override
        public GoogleIdTokenPayload verify(String idToken) {
            this.requestedIdToken = idToken;
            return payload;
        }
    }
}
