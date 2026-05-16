package com.aquilaoffice.adapter.out.auth;

import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import com.aquilaoffice.domain.member.MemberRole;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 개발 환경에서 사용할 수 있는 self-contained auth token 문자열을 발급하고 해석하는 adapter다.
 */
@Component
public class DeterministicAuthTokenIssuer implements IssueAuthTokenPort, ResolveRefreshTokenPort {

    private static final String ACCESS_PREFIX = "aquila-access.";
    private static final String REFRESH_PREFIX = "aquila-refresh.";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final int MEMBER_ID_INDEX = 0;
    private static final int EMAIL_INDEX = 1;
    private static final int ROLE_INDEX = 2;
    private static final int TOKEN_TYPE_INDEX = 3;
    private static final int TOKEN_PAYLOAD_LENGTH = 5;

    @Override
    public IssuedAuthTokens issue(Long memberId, String email, MemberRole role) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be positive");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return new IssuedAuthTokens(
                ACCESS_PREFIX + encode(memberId, email, role, ACCESS_TOKEN_TYPE),
                REFRESH_PREFIX + encode(memberId, email, role, REFRESH_TOKEN_TYPE));
    }

    @Override
    public ResolvedRefreshToken resolve(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be blank");
        }
        String normalizedToken = refreshToken.trim();
        if (!normalizedToken.startsWith(REFRESH_PREFIX)) {
            throw new IllegalArgumentException("refreshToken is invalid");
        }

        String[] payload = decode(normalizedToken.substring(REFRESH_PREFIX.length()));
        if (payload.length != TOKEN_PAYLOAD_LENGTH || !REFRESH_TOKEN_TYPE.equals(payload[TOKEN_TYPE_INDEX])) {
            throw new IllegalArgumentException("refreshToken is invalid");
        }

        try {
            return new ResolvedRefreshToken(
                    Long.parseLong(payload[MEMBER_ID_INDEX]),
                    payload[EMAIL_INDEX],
                    MemberRole.valueOf(payload[ROLE_INDEX]));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("refreshToken is invalid", exception);
        }
    }

    private String encode(Long memberId, String email, MemberRole role, String tokenType) {
        String payload = memberId + ":" + email.trim() + ":" + role.name() + ":" + tokenType + ":" + UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String[] decode(String encodedPayload) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            return payload.split(":", -1);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("refreshToken is invalid", exception);
        }
    }
}
