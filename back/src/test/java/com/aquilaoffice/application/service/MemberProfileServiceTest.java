package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetMemberProfileQuery;
import com.aquilaoffice.application.port.in.MemberAvatarColorsResult;
import com.aquilaoffice.application.port.in.MemberProfileResult;
import com.aquilaoffice.application.port.in.UpdateMemberProfileCommand;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadMemberProfilePort;
import com.aquilaoffice.application.port.out.MemberProfileUpdate;
import com.aquilaoffice.application.port.out.UpdateMemberProfilePort;
import com.aquilaoffice.domain.member.Member;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 멤버 프로필 service가 요청자 검증, 기본 프로필 조회, 부분 수정 검증을 수행하는지 확인한다.
 */
class MemberProfileServiceTest {

    @Test
    void getMemberProfileReturnsProfileForKnownMember() {
        Member requester = Member.createUser("google-1", "user@example.com", "User");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(requester));
        FakeLoadMemberProfilePort loadMemberProfilePort = new FakeLoadMemberProfilePort(Optional.of(profileResult()));
        FakeUpdateMemberProfilePort updateMemberProfilePort =
                new FakeUpdateMemberProfilePort(Optional.of(profileResult()));
        MemberProfileService service =
                new MemberProfileService(loadMemberPort, loadMemberProfilePort, updateMemberProfilePort);

        MemberProfileResult result = service.getMemberProfile(new GetMemberProfileQuery(" google-1 "));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadMemberProfilePort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.displayName()).isEqualTo("User");
        assertThat(result.avatarKind()).isEqualTo("mira");
        assertThat(result.avatarColors().skin()).isEqualTo("#f8d4b0");
    }

    @Test
    void getMemberProfileRejectsUnknownMemberAndDoesNotLoadProfile() {
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.empty());
        FakeLoadMemberProfilePort loadMemberProfilePort = new FakeLoadMemberProfilePort(Optional.empty());
        MemberProfileService service = new MemberProfileService(
                loadMemberPort,
                loadMemberProfilePort,
                new FakeUpdateMemberProfilePort(Optional.of(profileResult())));

        assertThatThrownBy(() -> service.getMemberProfile(new GetMemberProfileQuery("missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadMemberProfilePort.called).isFalse();
    }

    @Test
    void updateMemberProfileTrimsTextAndAcceptsPartialColors() {
        Member requester = Member.createUser("google-1", "user@example.com", "User");
        FakeUpdateMemberProfilePort updateMemberProfilePort =
                new FakeUpdateMemberProfilePort(Optional.of(updatedProfileResult()));
        MemberProfileService service = new MemberProfileService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadMemberProfilePort(Optional.of(profileResult())),
                updateMemberProfilePort);

        MemberProfileResult result = service.updateMemberProfile(new UpdateMemberProfileCommand(
                " google-1 ", "  Operator  ", " servo-skull ", "#111111", null, "#333333"));

        assertThat(updateMemberProfilePort.update.googleSub()).isEqualTo("google-1");
        assertThat(updateMemberProfilePort.update.displayName()).isEqualTo("Operator");
        assertThat(updateMemberProfilePort.update.avatarKind()).isEqualTo("servo-skull");
        assertThat(updateMemberProfilePort.update.skinColor()).isEqualTo("#111111");
        assertThat(updateMemberProfilePort.update.hairColor()).isNull();
        assertThat(updateMemberProfilePort.update.shirtColor()).isEqualTo("#333333");
        assertThat(result.displayName()).isEqualTo("Operator");
    }

    @Test
    void updateMemberProfileRejectsBlankDisplayName() {
        Member requester = Member.createUser("google-1", "user@example.com", "User");
        FakeUpdateMemberProfilePort updateMemberProfilePort =
                new FakeUpdateMemberProfilePort(Optional.of(profileResult()));
        MemberProfileService service = new MemberProfileService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadMemberProfilePort(Optional.of(profileResult())),
                updateMemberProfilePort);

        assertThatThrownBy(() -> service.updateMemberProfile(
                        new UpdateMemberProfileCommand("google-1", " ", null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("displayName must not be blank");
        assertThat(updateMemberProfilePort.called).isFalse();
    }

    @Test
    void updateMemberProfileRejectsInvalidColor() {
        Member requester = Member.createUser("google-1", "user@example.com", "User");
        FakeUpdateMemberProfilePort updateMemberProfilePort =
                new FakeUpdateMemberProfilePort(Optional.of(profileResult()));
        MemberProfileService service = new MemberProfileService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadMemberProfilePort(Optional.of(profileResult())),
                updateMemberProfilePort);

        assertThatThrownBy(() -> service.updateMemberProfile(
                        new UpdateMemberProfileCommand("google-1", null, null, "red", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("avatar color must be #RRGGBB");
        assertThat(updateMemberProfilePort.called).isFalse();
    }

    @Test
    void queryRejectsBlankGoogleSub() {
        assertThatThrownBy(() -> new GetMemberProfileQuery(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberGoogleSub must not be blank");
    }

    private static MemberProfileResult profileResult() {
        return new MemberProfileResult(
                1L,
                "User",
                "mira",
                new MemberAvatarColorsResult("#f8d4b0", "#1a1a1a", "#2a3a4a"));
    }

    private static MemberProfileResult updatedProfileResult() {
        return new MemberProfileResult(
                1L,
                "Operator",
                "servo-skull",
                new MemberAvatarColorsResult("#111111", "#1a1a1a", "#333333"));
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;
        private String requestedGoogleSub;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            this.requestedGoogleSub = googleSub;
            return member;
        }
    }

    private static class FakeLoadMemberProfilePort implements LoadMemberProfilePort {
        private final Optional<MemberProfileResult> result;
        private String requestedGoogleSub;
        private boolean called;

        private FakeLoadMemberProfilePort(Optional<MemberProfileResult> result) {
            this.result = result;
        }

        @Override
        public Optional<MemberProfileResult> findOrCreateDefaultByGoogleSub(String googleSub) {
            this.called = true;
            this.requestedGoogleSub = googleSub;
            return result;
        }
    }

    private static class FakeUpdateMemberProfilePort implements UpdateMemberProfilePort {
        private final Optional<MemberProfileResult> result;
        private MemberProfileUpdate update;
        private boolean called;

        private FakeUpdateMemberProfilePort(Optional<MemberProfileResult> result) {
            this.result = result;
        }

        @Override
        public Optional<MemberProfileResult> update(MemberProfileUpdate update) {
            this.called = true;
            this.update = update;
            return result;
        }
    }
}
