package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.RefreshAuthTokenCommand;
import com.aquilaoffice.application.port.in.RefreshAuthTokenResult;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import com.aquilaoffice.application.port.out.RotateRefreshTokenPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Refresh token 재발급 service가 저장된 token을 조건부 회전하는지 검증한다.
 */
class RefreshAuthTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T00:00:00Z");
    private static final Instant REFRESH_EXPIRES_AT = Instant.parse("2026-06-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void refreshRotatesStoredRefreshTokenAndReturnsNewTokens() {
        FakeResolveRefreshTokenPort resolveRefreshTokenPort = new FakeResolveRefreshTokenPort(
                new ResolvedRefreshToken(7L, "user@example.com", MemberRole.USER, "session-a"));
        FakeIssueAuthTokenPort issueAuthTokenPort = new FakeIssueAuthTokenPort();
        FakeRotateRefreshTokenPort rotateRefreshTokenPort = new FakeRotateRefreshTokenPort(true);
        RefreshAuthTokenService service = new RefreshAuthTokenService(
                resolveRefreshTokenPort,
                new FakeLoadMemberPort(Member.createUser("google-7", "user@example.com", "User")),
                issueAuthTokenPort,
                rotateRefreshTokenPort,
                CLOCK);

        RefreshAuthTokenResult result = service.refresh(new RefreshAuthTokenCommand(" old-refresh "));

        assertThat(resolveRefreshTokenPort.resolvedToken).isEqualTo("old-refresh");
        assertThat(issueAuthTokenPort.issued.googleSub()).isEqualTo("google-7");
        assertThat(issueAuthTokenPort.issued.refreshSessionId()).isEqualTo("session-a");
        assertThat(issueAuthTokenPort.issued.email()).isEqualTo("user@example.com");
        assertThat(issueAuthTokenPort.issued.role()).isEqualTo(MemberRole.USER);
        assertThat(rotateRefreshTokenPort.rotated.memberId()).isEqualTo(7L);
        assertThat(rotateRefreshTokenPort.rotated.refreshSessionId()).isEqualTo("session-a");
        assertThat(rotateRefreshTokenPort.rotated.currentRefreshToken()).isEqualTo("old-refresh");
        assertThat(rotateRefreshTokenPort.rotated.newRefreshToken()).isEqualTo("refresh-7-USER");
        assertThat(rotateRefreshTokenPort.rotated.newRefreshTokenExpiresAt()).isEqualTo(REFRESH_EXPIRES_AT);
        assertThat(rotateRefreshTokenPort.rotated.now()).isEqualTo(NOW);
        assertThat(result.accessToken()).isEqualTo("access-7-USER");
        assertThat(result.refreshToken()).isEqualTo("refresh-7-USER");
    }

    @Test
    void refreshIssuesTokensWithCurrentMemberRoleWhenStoredRoleChanged() {
        FakeResolveRefreshTokenPort resolveRefreshTokenPort = new FakeResolveRefreshTokenPort(
                new ResolvedRefreshToken(9L, "role-changed@example.com", MemberRole.ADMIN, "session-role"));
        FakeIssueAuthTokenPort issueAuthTokenPort = new FakeIssueAuthTokenPort();
        RefreshAuthTokenService service = new RefreshAuthTokenService(
                resolveRefreshTokenPort,
                new FakeLoadMemberPort(Member.createUser("google-9", "role-changed@example.com", "Role Changed")),
                issueAuthTokenPort,
                new FakeRotateRefreshTokenPort(true),
                CLOCK);

        RefreshAuthTokenResult result = service.refresh(new RefreshAuthTokenCommand("role-changed-refresh"));

        assertThat(issueAuthTokenPort.issued.email()).isEqualTo("role-changed@example.com");
        assertThat(issueAuthTokenPort.issued.role()).isEqualTo(MemberRole.USER);
        assertThat(result.accessToken()).isEqualTo("access-9-USER");
        assertThat(result.refreshToken()).isEqualTo("refresh-9-USER");
    }

    @Test
    void refreshRejectsWhenStoredRefreshTokenDoesNotMatch() {
        RefreshAuthTokenService service = new RefreshAuthTokenService(
                new FakeResolveRefreshTokenPort(
                        new ResolvedRefreshToken(8L, "user@example.com", MemberRole.USER, "session-b")),
                new FakeLoadMemberPort(Member.createUser("google-8", "user@example.com", "User")),
                new FakeIssueAuthTokenPort(),
                new FakeRotateRefreshTokenPort(false),
                CLOCK);

        assertThatThrownBy(() -> service.refresh(new RefreshAuthTokenCommand("old-refresh")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("refreshToken is invalid");
    }

    @Test
    void commandRejectsBlankRefreshToken() {
        assertThatThrownBy(() -> new RefreshAuthTokenCommand(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("refreshToken must not be blank");
    }

    private record RotatedRefreshToken(
            Long memberId,
            String refreshSessionId,
            String currentRefreshToken,
            String newRefreshToken,
            Instant newRefreshTokenExpiresAt,
            Instant now) {}

    private record IssuedRefreshToken(
            Long memberId,
            String googleSub,
            String email,
            MemberRole role,
            String refreshSessionId) {}

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Member member;

        private FakeLoadMemberPort(Member member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return Optional.empty();
        }

        @Override
        public Optional<Member> findByEmail(String email) {
            if (member.getEmail().equals(email.trim())) {
                return Optional.of(member);
            }
            return Optional.empty();
        }
    }

    private static class FakeResolveRefreshTokenPort implements ResolveRefreshTokenPort {
        private final ResolvedRefreshToken resolvedRefreshToken;
        private String resolvedToken;

        private FakeResolveRefreshTokenPort(ResolvedRefreshToken resolvedRefreshToken) {
            this.resolvedRefreshToken = resolvedRefreshToken;
        }

        @Override
        public ResolvedRefreshToken resolve(String refreshToken) {
            this.resolvedToken = refreshToken;
            return resolvedRefreshToken;
        }
    }

    private static class FakeIssueAuthTokenPort implements IssueAuthTokenPort {
        private IssuedRefreshToken issued;

        @Override
        public IssuedAuthTokens issue(Long memberId, String googleSub, String email, MemberRole role) {
            return issue(memberId, googleSub, email, role, "generated-session");
        }

        @Override
        public IssuedAuthTokens issue(
                Long memberId,
                String googleSub,
                String email,
                MemberRole role,
                String refreshSessionId) {
            this.issued = new IssuedRefreshToken(memberId, googleSub, email, role, refreshSessionId);
            return new IssuedAuthTokens(
                    "access-" + memberId + "-" + role.name(),
                    "refresh-" + memberId + "-" + role.name(),
                    Instant.parse("2026-05-18T00:15:00Z"),
                    REFRESH_EXPIRES_AT,
                    refreshSessionId);
        }
    }

    private static class FakeRotateRefreshTokenPort implements RotateRefreshTokenPort {
        private final boolean result;
        private RotatedRefreshToken rotated;

        private FakeRotateRefreshTokenPort(boolean result) {
            this.result = result;
        }

        @Override
        public boolean rotateIfMatch(
                Long memberId,
                String refreshSessionId,
                String currentRefreshToken,
                String newRefreshToken,
                Instant newRefreshTokenExpiresAt,
                Instant now) {
            this.rotated = new RotatedRefreshToken(
                    memberId,
                    refreshSessionId,
                    currentRefreshToken,
                    newRefreshToken,
                    newRefreshTokenExpiresAt,
                    now);
            return result;
        }
    }
}
