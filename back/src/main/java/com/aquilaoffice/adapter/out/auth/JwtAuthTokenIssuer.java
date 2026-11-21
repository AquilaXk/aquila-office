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
    private static final String GOOGLE_SUB_CLAIM = "google_sub";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String REFRESH_SESSION_ID_CLAIM = "refresh_session_id";
    private static final int MAX_JWT_LENGTH = 4096;

    private final Clock clock;
    private final AuthTokenProperties properties;
    private final JWSHeader hs256Header;
    private final MACSigner macSigner;
    private final MACVerifier macVerifier;

    public JwtAuthTokenIssuer(Clock clock, AuthTokenProperties properties) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        try {
            this.hs256Header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();
            this.macSigner = new MACSigner(secretBytes);
            this.macVerifier = new MACVerifier(secretBytes);
        } catch (JOSEException exception) {
            throw new IllegalStateException("jwt signer initialization failed", exception);
        }
    }

    @Override
    public IssuedAuthTokens issue(Long memberId, String googleSub, String email, MemberRole role) {
        return issue(memberId, googleSub, email, role, UUID.randomUUID().toString());
    }

    @Override
    public IssuedAuthTokens issue(
            Long memberId,
            String googleSub,
            String email,
            MemberRole role,
            String refreshSessionId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be positive");
        }
        String normalizedGoogleSub = normalizeOptional(googleSub);
        String normalizedEmail = requireNotBlank(email, "email");
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        String normalizedRefreshSessionId = requireNotBlank(refreshSessionId, "refreshSessionId");

        Instant issuedAt = clock.instant();
        Instant accessTokenExpiresAt = issuedAt.plus(properties.accessTokenTtl());
        Instant refreshTokenExpiresAt = issuedAt.plus(properties.refreshTokenTtl());
        return new IssuedAuthTokens(
                sign(
                        memberId,
                        normalizedGoogleSub,
                        normalizedEmail,
                        role,
                        ACCESS_TOKEN_TYPE,
                        issuedAt,
                        accessTokenExpiresAt,
                        null),
                sign(
                        memberId,
                        normalizedGoogleSub,
                        normalizedEmail,
                        role,
                        REFRESH_TOKEN_TYPE,
                        issuedAt,
                        refreshTokenExpiresAt,
                        normalizedRefreshSessionId),
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                normalizedRefreshSessionId);
    }

    @Override
    public ResolvedRefreshToken resolve(String refreshToken) {
        JWTClaimsSet claims = parseAndValidate(refreshToken, REFRESH_TOKEN_TYPE, "refreshToken");
        return new ResolvedRefreshToken(
                parseMemberId(claims, "refreshToken"),
                parseStringClaim(claims, EMAIL_CLAIM, "refreshToken"),
                parseRole(claims, "refreshToken"),
                parseStringClaim(claims, REFRESH_SESSION_ID_CLAIM, "refreshToken"));
    }

    @Override
    public ResolvedAccessToken resolveAccessToken(String accessToken) {
        JWTClaimsSet claims = parseAndValidate(accessToken, ACCESS_TOKEN_TYPE, "accessToken");
        return new ResolvedAccessToken(
                parseMemberId(claims, "accessToken"),
                parseOptionalStringClaim(claims, GOOGLE_SUB_CLAIM, "accessToken"),
                parseStringClaim(claims, EMAIL_CLAIM, "accessToken"),
                parseRole(claims, "accessToken"));
    }

    private String sign(
            Long memberId,
            String googleSub,
            String email,
            MemberRole role,
            String tokenType,
            Instant issuedAt,
            Instant expiresAt,
            String refreshSessionId) {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(memberId))
                .claim(EMAIL_CLAIM, email)
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString());
        if (googleSub != null) {
            claimsBuilder.claim(GOOGLE_SUB_CLAIM, googleSub);
        }
        if (refreshSessionId != null) {
            claimsBuilder.claim(REFRESH_SESSION_ID_CLAIM, refreshSessionId);
        }
        JWTClaimsSet claims = claimsBuilder.build();
        SignedJWT signedJwt = new SignedJWT(hs256Header, claims);
        try {
            signedJwt.sign(macSigner);
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("authToken signing failed", exception);
        }
    }

    private JWTClaimsSet parseAndValidate(String token, String expectedTokenType, String tokenName) {
        String normalizedToken = requireNotBlank(token, tokenName);
        if (normalizedToken.length() > MAX_JWT_LENGTH) {
            throw invalidToken(tokenName);
        }
        try {
            SignedJWT signedJwt = SignedJWT.parse(normalizedToken);
            if (!JWSAlgorithm.HS256.equals(signedJwt.getHeader().getAlgorithm())
                    || !signedJwt.verify(macVerifier)) {
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

    private String parseOptionalStringClaim(JWTClaimsSet claims, String claimName, String tokenName) {
        Object rawClaim = claims.getClaim(claimName);
        if (rawClaim == null) {
            return null;
        }
        if (!(rawClaim instanceof String stringClaim)) {
            throw invalidToken(tokenName);
        }
        return normalizeOptional(stringClaim);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
