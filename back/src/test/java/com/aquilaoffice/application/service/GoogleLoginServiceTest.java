package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GoogleLoginCommand;
import com.aquilaoffice.application.port.in.GoogleLoginResult;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SavedGoogleLoginMember;
import com.aquilaoffice.application.port.out.VerifyGoogleIdTokenPort;
import com.aquilaoffice.application.port.out.VerifiedGoogleUser;
import com.aquilaoffice.domain.member.MemberRole;
import org.junit.jupiter.api.Test;

/**
 * Google 로그인 service가 검증된 Google 사용자로 회원을 저장하고 token pair를 발급하는지 확인한다.
 */
class GoogleLoginServiceTest {

    @Test
    void googleLoginCreatesUserMemberAndReturnsIssuedTokens() {
        FakeSaveGoogleLoginMemberPort saveMemberPort = new FakeSaveGoogleLoginMemberPort(1L);
        GoogleLoginService service = new GoogleLoginService(
                new FakeVerifyGoogleIdTokenPort(new VerifiedGoogleUser(
                        " google-1 ",
                        " user@example.com ",
                        " User One ")),
                saveMemberPort,
                new FakeIssueAuthTokenPort(),
                "");

        GoogleLoginResult result = service.login(new GoogleLoginCommand(" id-token "));

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
    }

    @Test
    void googleLoginPromotesAllowlistedEmailToAdmin() {
        FakeSaveGoogleLoginMemberPort saveMemberPort = new FakeSaveGoogleLoginMemberPort(2L);
        GoogleLoginService service = new GoogleLoginService(
                new FakeVerifyGoogleIdTokenPort(new VerifiedGoogleUser(
                        "google-admin",
                        "admin@example.com",
                        "Admin")),
                saveMemberPort,
                new FakeIssueAuthTokenPort(),
                " owner@example.com, ADMIN@example.com ");

        GoogleLoginResult result = service.login(new GoogleLoginCommand("id-token"));

        assertThat(saveMemberPort.saved.role()).isEqualTo(MemberRole.ADMIN);
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

        @Override
        public IssuedAuthTokens issue(Long memberId, String email, MemberRole role) {
            return new IssuedAuthTokens("access-" + memberId + "-" + role.name(), "refresh-" + memberId + "-" + role.name());
        }
    }
}
