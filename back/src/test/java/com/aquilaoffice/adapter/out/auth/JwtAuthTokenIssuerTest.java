package com.aquilaoffice.adapter.out.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.ResolvedAccessToken;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.config.properties.AuthTokenProperties;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
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
        ResolvedRefreshToken resolvedRefreshToken = issuer.resolve(tokens.refreshToken());
        assertThat(resolvedRefreshToken.memberId()).isEqualTo(11L);
        assertThat(resolvedRefreshToken.refreshSessionId()).isEqualTo(tokens.refreshSessionId());
        assertThat(tokens.refreshSessionId()).isNotBlank();
    }

    @Test
    void issuesRefreshRotationTokenWithExistingSessionId() {
        IssuedAuthTokens tokens = issuer.issue(15L, "user@example.com", MemberRole.USER);

        IssuedAuthTokens rotated = issuer.issue(
                15L,
                "user@example.com",
                MemberRole.USER,
                issuer.resolve(tokens.refreshToken()).refreshSessionId());

        assertThat(rotated.refreshSessionId()).isEqualTo(tokens.refreshSessionId());
        assertThat(issuer.resolve(rotated.refreshToken()).refreshSessionId()).isEqualTo(tokens.refreshSessionId());
    }

    @Test
    void issuesSignedJwtWithGoogleSubClaimAndResolvesIt() throws Exception {
        IssuedAuthTokens tokens = issuer.issue(21L, " google-sub-21 ", " user21@example.com ", MemberRole.USER);

        ResolvedAccessToken resolved = issuer.resolveAccessToken(tokens.accessToken());

        assertThat(resolved.memberId()).isEqualTo(21L);
        assertThat(resolved.googleSub()).isEqualTo("google-sub-21");
        assertThat(resolved.email()).isEqualTo("user21@example.com");
        assertThat(resolved.role()).isEqualTo(MemberRole.USER);

        JWTClaimsSet nonStringGoogleSubClaims = new JWTClaimsSet.Builder()
                .issuer(PROPERTIES.issuer())
                .subject("21")
                .claim("google_sub", 12345)
                .claim("email", "user21@example.com")
                .claim("role", MemberRole.USER.name())
                .claim("token_type", "access")
                .issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plus(Duration.ofMinutes(15))))
                .build();
        SignedJWT invalidJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                nonStringGoogleSubClaims);
        invalidJwt.sign(new MACSigner(PROPERTIES.secret().getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> issuer.resolveAccessToken(invalidJwt.serialize()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken is invalid");
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

    @Test
    void rejectsBlankOversizedAndNonHs256TokensBeforeOrDuringVerification() throws Exception {
        assertThatThrownBy(() -> issuer.resolveAccessToken("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken must not be blank");
        assertThatThrownBy(() -> issuer.resolve(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("refreshToken must not be blank");

        String oversizedToken = "a".repeat(4097);
        assertThatThrownBy(() -> issuer.resolveAccessToken(oversizedToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken is invalid");

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(PROPERTIES.issuer())
                .subject("14")
                .claim("email", "user@example.com")
                .claim("role", MemberRole.USER.name())
                .claim("token_type", "access")
                .issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plus(Duration.ofMinutes(15))))
                .build();
        SignedJWT hs384Token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS384).type(JOSEObjectType.JWT).build(),
                claims);
        hs384Token.sign(new MACSigner(PROPERTIES.secret().getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> issuer.resolveAccessToken(hs384Token.serialize()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessToken is invalid");
    }
}
