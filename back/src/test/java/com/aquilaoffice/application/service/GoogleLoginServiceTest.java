package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GoogleLoginCommand;
import com.aquilaoffice.application.port.in.GoogleLoginResult;
import com.aquilaoffice.application.port.out.InitializeMemberProfilePort;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SaveRefreshTokenPort;
import com.aquilaoffice.application.port.out.SavedGoogleLoginMember;
import com.aquilaoffice.application.port.out.VerifyGoogleIdTokenPort;
import com.aquilaoffice.application.port.out.VerifiedGoogleUser;
import com.aquilaoffice.domain.member.MemberRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Google 로그인 service가 검증된 Google 사용자로 회원을 저장하고 token pair를 발급하는지 확인한다.
 */
class GoogleLoginServiceTest {

    @Test
    void googleLoginCreatesUserMemberAndReturnsIssuedTokens() {
        FakeSaveGoogleLoginMemberPort saveMemberPort = new FakeSaveGoogleLoginMemberPort(1L);
        FakeSaveRefreshTokenPort saveRefreshTokenPort = new FakeSaveRefreshTokenPort();
        FakeInitializeMemberProfilePort initializeMemberProfilePort = new FakeInitializeMemberProfilePort();
        FakeIssueAuthTokenPort issueAuthTokenPort = new FakeIssueAuthTokenPort();
        GoogleLoginService service = new GoogleLoginService(
                new FakeVerifyGoogleIdTokenPort(new VerifiedGoogleUser(
                        " google-1 ",
                        " user@example.com ",
                        " User One ")),
                saveMemberPort,
                issueAuthTokenPort,
                saveRefreshTokenPort,
                initializeMemberProfilePort,
                "");

        GoogleLoginResult result = service.login(new GoogleLoginCommand(" id-token "));

        assertThat(issueAuthTokenPort.lastGoogleSub).isEqualTo("google-1");

        assertThat(saveMemberPort.saved.googleSub()).isEqualTo("google-1");
        assertThat(saveMemberPort.saved.email()).isEqualTo("user@example.com");
        assertThat(saveMemberPort.saved.name()).isEqualTo("User One");
        assertThat(saveMemberPort.saved.role()).isEqualTo(MemberRole.USER);
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("User One");
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.role()).isEqualTo("USER");
        assertThat(result.accessToken()).isEqualTo("access-1-USER");
        assertThat(result.refreshToken()).isEqualTo("refresh-1-USER");
        assertThat(saveRefreshTokenPort.saved.memberId()).isEqualTo(1L);
        assertThat(saveRefreshTokenPort.saved.refreshSessionId()).isEqualTo("session-1-USER");
        assertThat(saveRefreshTokenPort.saved.refreshToken()).isEqualTo("refresh-1-USER");
        assertThat(saveRefreshTokenPort.saved.refreshTokenExpiresAt())
                .isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(initializeMemberProfilePort.initializedGoogleSub).isEqualTo("google-1");
    }

    @Test
    void googleLoginPromotesAllowlistedEmailToAdmin() {
        FakeSaveGoogleLoginMemberPort saveMemberPort = new FakeSaveGoogleLoginMemberPort(2L);
        FakeSaveRefreshTokenPort saveRefreshTokenPort = new FakeSaveRefreshTokenPort();
        FakeInitializeMemberProfilePort initializeMemberProfilePort = new FakeInitializeMemberProfilePort();
        GoogleLoginService service = new GoogleLoginService(
                new FakeVerifyGoogleIdTokenPort(new VerifiedGoogleUser(
                        "google-admin",
                        "admin@example.com",
                        "Admin")),
                saveMemberPort,
                new FakeIssueAuthTokenPort(),
                saveRefreshTokenPort,
                initializeMemberProfilePort,
                " owner@example.com, ADMIN@example.com ");

        GoogleLoginResult result = service.login(new GoogleLoginCommand("id-token"));

        assertThat(saveMemberPort.saved.role()).isEqualTo(MemberRole.ADMIN);
        assertThat(saveRefreshTokenPort.saved.refreshToken()).isEqualTo("refresh-2-ADMIN");
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.accessToken()).isEqualTo("access-2-ADMIN");
    }

    @Test
    void commandRejectsBlankIdToken() {
        assertThatThrownBy(() -> new GoogleLoginCommand(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idToken must not be blank");
    }

    private record SavedRequest(String googleSub, String email, String name, MemberRole role) {}

    private record SavedRefreshToken(
            Long memberId,
            String refreshSessionId,
            String refreshToken,
            Instant refreshTokenExpiresAt) {}

    private static class FakeVerifyGoogleIdTokenPort implements VerifyGoogleIdTokenPort {
        private final VerifiedGoogleUser user;

        private FakeVerifyGoogleIdTokenPort(VerifiedGoogleUser user) {
            this.user = user;
        }

        @Override
        public VerifiedGoogleUser verify(String idToken) {
            return user;
        }
    }

    private static class FakeSaveGoogleLoginMemberPort implements SaveGoogleLoginMemberPort {
        private final Long memberId;
        private SavedRequest saved;

        private FakeSaveGoogleLoginMemberPort(Long memberId) {
            this.memberId = memberId;
        }

        @Override
        public SavedGoogleLoginMember saveOrUpdate(
                String googleSub,
                String email,
                String name,
                MemberRole role) {
            this.saved = new SavedRequest(googleSub, email, name, role);
            return new SavedGoogleLoginMember(memberId, googleSub, email, name, role);
        }
    }

    private static class FakeIssueAuthTokenPort implements IssueAuthTokenPort {

        private String lastGoogleSub;

        @Override
        public IssuedAuthTokens issue(Long memberId, String googleSub, String email, MemberRole role) {
            return issue(memberId, googleSub, email, role, "session-" + memberId + "-" + role.name());
        }

        @Override
        public IssuedAuthTokens issue(
                Long memberId,
                String googleSub,
                String email,
                MemberRole role,
                String refreshSessionId) {
            this.lastGoogleSub = googleSub;
            return new IssuedAuthTokens(
                    "access-" + memberId + "-" + role.name(),
                    "refresh-" + memberId + "-" + role.name(),
                    Instant.parse("2026-05-18T00:15:00Z"),
                    Instant.parse("2026-06-01T00:00:00Z"),
                    refreshSessionId);
        }
    }

    private static class FakeSaveRefreshTokenPort implements SaveRefreshTokenPort {
        private SavedRefreshToken saved;

        @Override
        public void saveOrRotate(
                Long memberId,
                String refreshSessionId,
                String refreshToken,
                Instant refreshTokenExpiresAt) {
            this.saved = new SavedRefreshToken(memberId, refreshSessionId, refreshToken, refreshTokenExpiresAt);
        }
    }

    private static class FakeInitializeMemberProfilePort implements InitializeMemberProfilePort {
        private String initializedGoogleSub;

        @Override
        public void initializeDefaultProfile(String googleSub) {
            this.initializedGoogleSub = googleSub;
        }
    }
}
