package com.aquilaoffice.adapter.out.auth;

import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.domain.member.MemberRole;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * refresh 저장소 도입 전까지 내부 token 문자열을 일관되게 발급하는 adapter다.
 */
@Component
public class DeterministicAuthTokenIssuer implements IssueAuthTokenPort {

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
                "aquila-access." + encode(memberId, email, role, "access"),
                "aquila-refresh." + encode(memberId, email, role, "refresh"));
    }

    private String encode(Long memberId, String email, MemberRole role, String tokenType) {
        String payload = memberId + ":" + email.trim() + ":" + role.name() + ":" + tokenType;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
