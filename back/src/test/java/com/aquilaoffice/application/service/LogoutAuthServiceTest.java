package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.LogoutAuthCommand;
import com.aquilaoffice.application.port.out.DeleteRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import com.aquilaoffice.domain.member.MemberRole;
import org.junit.jupiter.api.Test;

/**
 * 로그아웃 service가 refresh token 소유자를 확인하고 저장값을 삭제하는지 검증한다.
 */
class LogoutAuthServiceTest {

    @Test
    void logoutDeletesStoredRefreshTokenWhenTokenMatches() {
        FakeResolveRefreshTokenPort resolveRefreshTokenPort = new FakeResolveRefreshTokenPort(
                new ResolvedRefreshToken(9L, "user@example.com", MemberRole.USER));
        FakeDeleteRefreshTokenPort deleteRefreshTokenPort = new FakeDeleteRefreshTokenPort(true);
        LogoutAuthService service = new LogoutAuthService(resolveRefreshTokenPort, deleteRefreshTokenPort);

        service.logout(new LogoutAuthCommand(" old-refresh "));

        assertThat(resolveRefreshTokenPort.resolvedToken).isEqualTo("old-refresh");
        assertThat(deleteRefreshTokenPort.deleted.memberId()).isEqualTo(9L);
        assertThat(deleteRefreshTokenPort.deleted.refreshToken()).isEqualTo("old-refresh");
    }

    @Test
    void logoutRejectsWhenStoredRefreshTokenDoesNotMatch() {
        LogoutAuthService service = new LogoutAuthService(
                new FakeResolveRefreshTokenPort(new ResolvedRefreshToken(10L, "user@example.com", MemberRole.USER)),
                new FakeDeleteRefreshTokenPort(false));

        assertThatThrownBy(() -> service.logout(new LogoutAuthCommand("old-refresh")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("refreshToken is invalid");
    }

    @Test
    void commandRejectsBlankRefreshToken() {
        assertThatThrownBy(() -> new LogoutAuthCommand(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("refreshToken must not be blank");
    }

    private record DeletedRefreshToken(Long memberId, String refreshToken) {}

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

    private static class FakeDeleteRefreshTokenPort implements DeleteRefreshTokenPort {
        private final boolean result;
        private DeletedRefreshToken deleted;

        private FakeDeleteRefreshTokenPort(boolean result) {
            this.result = result;
        }

        @Override
        public boolean deleteIfMatch(Long memberId, String refreshToken) {
            this.deleted = new DeletedRefreshToken(memberId, refreshToken);
            return result;
        }
    }
}
