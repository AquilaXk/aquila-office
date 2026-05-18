package com.aquilaoffice.adapter.out.auth;

import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.ResolveAccessTokenPort;
import com.aquilaoffice.application.port.out.ResolvedAccessToken;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.config.properties.AuthTokenProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * HS256 JWT access/refresh token을 발급하고 서명, issuer, type, 만료를 검증하는 adapter다.
 */
@Component
public class JwtAuthTokenIssuer implements IssueAuthTokenPort, ResolveRefreshTokenPort, ResolveAccessTokenPort {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    private final Clock clock;
    private final AuthTokenProperties properties;
    private final byte[] secretBytes;

    public JwtAuthTokenIssuer(Clock clock, AuthTokenProperties properties) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public IssuedAuthTokens issue(Long memberId, String email, MemberRole role) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be positive");
        }
        String normalizedEmail = requireNotBlank(email, "email");
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }

        Instant issuedAt = clock.instant();
        Instant accessTokenExpiresAt = issuedAt.plus(properties.accessTokenTtl());
        Instant refreshTokenExpiresAt = issuedAt.plus(properties.refreshTokenTtl());
        return new IssuedAuthTokens(
                sign(memberId, normalizedEmail, role, ACCESS_TOKEN_TYPE, issuedAt, accessTokenExpiresAt),
                sign(memberId, normalizedEmail, role, REFRESH_TOKEN_TYPE, issuedAt, refreshTokenExpiresAt),
                accessTokenExpiresAt,
                refreshTokenExpiresAt);
    }

    @Override
    public ResolvedRefreshToken resolve(String refreshToken) {
        JWTClaimsSet claims = parseAndValidate(refreshToken, REFRESH_TOKEN_TYPE, "refreshToken");
        return new ResolvedRefreshToken(
                parseMemberId(claims, "refreshToken"),
                parseStringClaim(claims, EMAIL_CLAIM, "refreshToken"),
                parseRole(claims, "refreshToken"));
    }

    @Override
    public ResolvedAccessToken resolveAccessToken(String accessToken) {
        JWTClaimsSet claims = parseAndValidate(accessToken, ACCESS_TOKEN_TYPE, "accessToken");
        return new ResolvedAccessToken(
                parseMemberId(claims, "accessToken"),
                parseStringClaim(claims, EMAIL_CLAIM, "accessToken"),
                parseRole(claims, "accessToken"));
    }

    private String sign(
            Long memberId,
            String email,
            MemberRole role,
            String tokenType,
            Instant issuedAt,
            Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(memberId))
                .claim(EMAIL_CLAIM, email)
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claims);
        try {
            signedJwt.sign(new MACSigner(secretBytes));
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("authToken signing failed", exception);
        }
    }

    private JWTClaimsSet parseAndValidate(String token, String expectedTokenType, String tokenName) {
        String normalizedToken = requireNotBlank(token, tokenName);
        try {
            SignedJWT signedJwt = SignedJWT.parse(normalizedToken);
            if (!signedJwt.verify(new MACVerifier(secretBytes))) {
                throw invalidToken(tokenName);
            }
            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            validateIssuer(claims, tokenName);
            validateTokenType(claims, expectedTokenType, tokenName);
            validateExpiration(claims, tokenName);
            return claims;
        } catch (ParseException | JOSEException exception) {
            throw invalidToken(tokenName, exception);
        }
    }

    private void validateIssuer(JWTClaimsSet claims, String tokenName) {
        if (!properties.issuer().equals(claims.getIssuer())) {
            throw invalidToken(tokenName);
        }
    }

    private void validateTokenType(JWTClaimsSet claims, String expectedTokenType, String tokenName) {
        String tokenType = parseStringClaim(claims, TOKEN_TYPE_CLAIM, tokenName);
        if (!expectedTokenType.equals(tokenType)) {
            throw invalidToken(tokenName);
        }
    }

    private void validateExpiration(JWTClaimsSet claims, String tokenName) {
        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null) {
            throw invalidToken(tokenName);
        }
        if (!expirationTime.toInstant().isAfter(clock.instant())) {
            throw new IllegalArgumentException(tokenName + " is expired");
        }
    }

    private Long parseMemberId(JWTClaimsSet claims, String tokenName) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException exception) {
            throw invalidToken(tokenName, exception);
        }
    }

    private MemberRole parseRole(JWTClaimsSet claims, String tokenName) {
        try {
            return MemberRole.valueOf(parseStringClaim(claims, ROLE_CLAIM, tokenName));
        } catch (IllegalArgumentException exception) {
            throw invalidToken(tokenName, exception);
        }
    }

    private String parseStringClaim(JWTClaimsSet claims, String claimName, String tokenName) {
        try {
            String claim = claims.getStringClaim(claimName);
            return requireNotBlank(claim, claimName);
        } catch (ParseException exception) {
            throw invalidToken(tokenName, exception);
        }
    }

    private String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private IllegalArgumentException invalidToken(String tokenName) {
        return new IllegalArgumentException(tokenName + " is invalid");
    }

    private IllegalArgumentException invalidToken(String tokenName, Exception cause) {
        return new IllegalArgumentException(tokenName + " is invalid", cause);
    }
}
