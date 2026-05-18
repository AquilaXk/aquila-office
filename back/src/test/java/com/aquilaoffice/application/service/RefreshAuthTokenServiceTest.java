package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.RefreshAuthTokenCommand;
import com.aquilaoffice.application.port.in.RefreshAuthTokenResult;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import com.aquilaoffice.application.port.out.RotateRefreshTokenPort;
import com.aquilaoffice.domain.member.MemberRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Refresh token 재발급 service가 저장된 token을 조건부 회전하는지 검증한다.
 */
class RefreshAuthTokenServiceTest {

    @Test
    void refreshRotatesStoredRefreshTokenAndReturnsNewTokens() {
        FakeResolveRefreshTokenPort resolveRefreshTokenPort = new FakeResolveRefreshTokenPort(
                new ResolvedRefreshToken(7L, "user@example.com", MemberRole.USER));
        FakeRotateRefreshTokenPort rotateRefreshTokenPort = new FakeRotateRefreshTokenPort(true);
        RefreshAuthTokenService service = new RefreshAuthTokenService(
                resolveRefreshTokenPort,
                new FakeIssueAuthTokenPort(),
                rotateRefreshTokenPort);

        RefreshAuthTokenResult result = service.refresh(new RefreshAuthTokenCommand(" old-refresh "));

        assertThat(resolveRefreshTokenPort.resolvedToken).isEqualTo("old-refresh");
        assertThat(rotateRefreshTokenPort.rotated.memberId()).isEqualTo(7L);
        assertThat(rotateRefreshTokenPort.rotated.currentRefreshToken()).isEqualTo("old-refresh");
        assertThat(rotateRefreshTokenPort.rotated.newRefreshToken()).isEqualTo("refresh-7-USER");
        assertThat(result.accessToken()).isEqualTo("access-7-USER");
        assertThat(result.refreshToken()).isEqualTo("refresh-7-USER");
    }

    @Test
    void refreshRejectsWhenStoredRefreshTokenDoesNotMatch() {
        RefreshAuthTokenService service = new RefreshAuthTokenService(
                new FakeResolveRefreshTokenPort(new ResolvedRefreshToken(8L, "user@example.com", MemberRole.USER)),
                new FakeIssueAuthTokenPort(),
                new FakeRotateRefreshTokenPort(false));

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

    private record RotatedRefreshToken(Long memberId, String currentRefreshToken, String newRefreshToken) {}

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

        @Override
        public IssuedAuthTokens issue(Long memberId, String email, MemberRole role) {
            return new IssuedAuthTokens(
                    "access-" + memberId + "-" + role.name(),
                    "refresh-" + memberId + "-" + role.name(),
                    Instant.parse("2026-05-18T00:15:00Z"),
                    Instant.parse("2026-06-01T00:00:00Z"));
        }
    }

    private static class FakeRotateRefreshTokenPort implements RotateRefreshTokenPort {
        private final boolean result;
        private RotatedRefreshToken rotated;

        private FakeRotateRefreshTokenPort(boolean result) {
            this.result = result;
        }

        @Override
        public boolean rotateIfMatch(Long memberId, String currentRefreshToken, String newRefreshToken) {
            this.rotated = new RotatedRefreshToken(memberId, currentRefreshToken, newRefreshToken);
            return result;
        }
    }
}
