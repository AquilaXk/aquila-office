package com.aquilaoffice.adapter.out.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.ResolvedAccessToken;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.config.properties.AuthTokenProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * JWT auth token issuer가 서명과 만료가 있는 token pair를 발급하고 검증하는지 확인한다.
 */
class JwtAuthTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-05-18T00:00:00Z");
    private static final AuthTokenProperties PROPERTIES = new AuthTokenProperties(
            "aquila-office-test",
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            Duration.ofMinutes(15),
            Duration.ofDays(14));

    private final JwtAuthTokenIssuer issuer = new JwtAuthTokenIssuer(
            Clock.fixed(NOW, ZoneOffset.UTC),
            PROPERTIES);

    @Test
    void issuesSignedJwtPairWithExpectedClaimsAndExpiry() {
        IssuedAuthTokens tokens = issuer.issue(11L, " user@example.com ", MemberRole.ADMIN);

        ResolvedAccessToken resolved = issuer.resolveAccessToken(tokens.accessToken());

        assertThat(tokens.accessToken()).contains(".");
        assertThat(tokens.refreshToken()).contains(".");
        assertThat(tokens.accessTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
        assertThat(resolved.memberId()).isEqualTo(11L);
        assertThat(resolved.email()).isEqualTo("user@example.com");
        assertThat(resolved.role()).isEqualTo(MemberRole.ADMIN);
        assertThat(issuer.resolve(tokens.refreshToken()).memberId()).isEqualTo(11L);
    }

    @Test
    void rejectsRefreshTokenAsAccessToken() {
        IssuedAuthTokens tokens = issuer.issue(12L, "user@example.com", MemberRole.USER);

        assertThatThrownBy(() -> issuer.resolveAccessToken(tokens.refreshToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken is invalid");
    }

    @Test
    void rejectsTamperedAccessToken() {
        IssuedAuthTokens tokens = issuer.issue(13L, "user@example.com", MemberRole.USER);
        String tamperedToken = tokens.accessToken().substring(0, tokens.accessToken().length() - 2) + "aa";

        assertThatThrownBy(() -> issuer.resolveAccessToken(tamperedToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken is invalid");
    }

    @Test
    void rejectsExpiredAccessToken() {
        IssuedAuthTokens tokens = issuer.issue(14L, "user@example.com", MemberRole.USER);
        JwtAuthTokenIssuer expiredIssuer = new JwtAuthTokenIssuer(
                Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC),
                PROPERTIES);

        assertThatThrownBy(() -> expiredIssuer.resolveAccessToken(tokens.accessToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken is expired");
    }
}
