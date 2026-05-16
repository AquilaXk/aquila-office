package com.aquilaoffice.adapter.out.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.ResolvedAccessToken;
import com.aquilaoffice.domain.member.MemberRole;
import org.junit.jupiter.api.Test;

/**
 * 개발용 auth token issuer가 access token payload를 다시 해석하는지 검증한다.
 */
class DeterministicAuthTokenIssuerTest {

    private final DeterministicAuthTokenIssuer issuer = new DeterministicAuthTokenIssuer();

    @Test
    void resolvesIssuedAccessTokenPayload() {
        IssuedAuthTokens tokens = issuer.issue(11L, "user@example.com", MemberRole.ADMIN);

        ResolvedAccessToken resolved = issuer.resolveAccessToken(tokens.accessToken());

        assertThat(resolved.memberId()).isEqualTo(11L);
        assertThat(resolved.email()).isEqualTo("user@example.com");
        assertThat(resolved.role()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void rejectsRefreshTokenAsAccessToken() {
        IssuedAuthTokens tokens = issuer.issue(12L, "user@example.com", MemberRole.USER);

        assertThatThrownBy(() -> issuer.resolveAccessToken(tokens.refreshToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken is invalid");
    }

    @Test
    void rejectsBlankAccessToken() {
        assertThatThrownBy(() -> issuer.resolveAccessToken(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken must not be blank");
    }
}
