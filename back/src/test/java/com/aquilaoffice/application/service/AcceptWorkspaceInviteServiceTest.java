package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteResult;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.AcceptedWorkspaceInvite;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteAcceptance;
import com.aquilaoffice.application.port.out.WorkspaceInviteLookup;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 초대 수락 service가 회원/초대 검증 후 수락 저장 port를 호출하는지 검증한다.
 */
class AcceptWorkspaceInviteServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-16T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime ACCEPTED_AT = LocalDateTime.of(2026, 5, 16, 10, 0);

    @Test
    void acceptWorkspaceInviteValidatesInviteAndCallsAcceptancePort() {
        Member member = Member.createUser("google-1", "invitee@example.com", "Invitee");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(member));
        FakeLoadWorkspaceInvitePort loadWorkspaceInvitePort = new FakeLoadWorkspaceInvitePort(Optional.of(pendingInvite()));
        FakeAcceptWorkspaceInvitePort acceptWorkspaceInvitePort = new FakeAcceptWorkspaceInvitePort(
                new AcceptedWorkspaceInvite(501L, 101L, "google-1", WorkspaceMemberRole.MEMBER, ACCEPTED_AT));
        AcceptWorkspaceInviteService service =
                new AcceptWorkspaceInviteService(loadMemberPort, loadWorkspaceInvitePort, acceptWorkspaceInvitePort, CLOCK);

        AcceptWorkspaceInviteResult result = service.acceptWorkspaceInvite(
                new AcceptWorkspaceInviteCommand(" invite-token ", " google-1 "));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspaceInvitePort.requestedToken).isEqualTo("invite-token");
        assertThat(acceptWorkspaceInvitePort.acceptance.inviteId()).isEqualTo(501L);
        assertThat(acceptWorkspaceInvitePort.acceptance.workspaceId()).isEqualTo(101L);
        assertThat(acceptWorkspaceInvitePort.acceptance.memberGoogleSub()).isEqualTo("google-1");
        assertThat(acceptWorkspaceInvitePort.acceptance.role()).isEqualTo(WorkspaceMemberRole.MEMBER);
        assertThat(acceptWorkspaceInvitePort.acceptance.acceptedAt()).isEqualTo(ACCEPTED_AT);

        assertThat(result.inviteId()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.memberGoogleSub()).isEqualTo("google-1");
        assertThat(result.role()).isEqualTo(WorkspaceMemberRole.MEMBER);
        assertThat(result.acceptedAt()).isEqualTo(ACCEPTED_AT);
    }

    @Test
    void acceptWorkspaceInviteRejectsEmailMismatchAndDoesNotAccept() {
        Member member = Member.createUser("google-1", "other@example.com", "Invitee");
        FakeAcceptWorkspaceInvitePort acceptWorkspaceInvitePort =
                new FakeAcceptWorkspaceInvitePort(new AcceptedWorkspaceInvite(501L, 101L, "google-1", WorkspaceMemberRole.MEMBER, ACCEPTED_AT));
        AcceptWorkspaceInviteService service = new AcceptWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(member)),
                new FakeLoadWorkspaceInvitePort(Optional.of(pendingInvite())),
                acceptWorkspaceInvitePort,
                CLOCK);

        assertThatThrownBy(() -> service.acceptWorkspaceInvite(
                        new AcceptWorkspaceInviteCommand("invite-token", "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invite email mismatch");
        assertThat(acceptWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void acceptWorkspaceInviteRejectsExpiredInviteAndDoesNotAccept() {
        Member member = Member.createUser("google-1", "invitee@example.com", "Invitee");
        WorkspaceInviteLookup expiredInvite = new WorkspaceInviteLookup(
                501L,
                101L,
                "invitee@example.com",
                "invite-token",
                WorkspaceInviteStatus.PENDING,
                LocalDateTime.of(2026, 5, 9, 10, 0),
                ACCEPTED_AT,
                null);
        FakeAcceptWorkspaceInvitePort acceptWorkspaceInvitePort =
                new FakeAcceptWorkspaceInvitePort(new AcceptedWorkspaceInvite(501L, 101L, "google-1", WorkspaceMemberRole.MEMBER, ACCEPTED_AT));
        AcceptWorkspaceInviteService service = new AcceptWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(member)),
                new FakeLoadWorkspaceInvitePort(Optional.of(expiredInvite)),
                acceptWorkspaceInvitePort,
                CLOCK);

        assertThatThrownBy(() -> service.acceptWorkspaceInvite(
                        new AcceptWorkspaceInviteCommand("invite-token", "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace invite is expired");
        assertThat(acceptWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void acceptWorkspaceInviteRejectsNonPendingInviteAndDoesNotAccept() {
        Member member = Member.createUser("google-1", "invitee@example.com", "Invitee");
        WorkspaceInviteLookup acceptedInvite = new WorkspaceInviteLookup(
                501L,
                101L,
                "invitee@example.com",
                "invite-token",
                WorkspaceInviteStatus.ACCEPTED,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                LocalDateTime.of(2026, 5, 22, 10, 0),
                ACCEPTED_AT);
        FakeAcceptWorkspaceInvitePort acceptWorkspaceInvitePort =
                new FakeAcceptWorkspaceInvitePort(new AcceptedWorkspaceInvite(501L, 101L, "google-1", WorkspaceMemberRole.MEMBER, ACCEPTED_AT));
        AcceptWorkspaceInviteService service = new AcceptWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(member)),
                new FakeLoadWorkspaceInvitePort(Optional.of(acceptedInvite)),
                acceptWorkspaceInvitePort,
                CLOCK);

        assertThatThrownBy(() -> service.acceptWorkspaceInvite(
                        new AcceptWorkspaceInviteCommand("invite-token", "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace invite is not pending");
        assertThat(acceptWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void acceptWorkspaceInviteRejectsUnknownMemberAndDoesNotLookupInvite() {
        FakeLoadWorkspaceInvitePort loadWorkspaceInvitePort = new FakeLoadWorkspaceInvitePort(Optional.of(pendingInvite()));
        FakeAcceptWorkspaceInvitePort acceptWorkspaceInvitePort =
                new FakeAcceptWorkspaceInvitePort(new AcceptedWorkspaceInvite(501L, 101L, "google-1", WorkspaceMemberRole.MEMBER, ACCEPTED_AT));
        AcceptWorkspaceInviteService service = new AcceptWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.empty()),
                loadWorkspaceInvitePort,
                acceptWorkspaceInvitePort,
                CLOCK);

        assertThatThrownBy(() -> service.acceptWorkspaceInvite(
                        new AcceptWorkspaceInviteCommand("invite-token", "missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspaceInvitePort.called).isFalse();
        assertThat(acceptWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void acceptWorkspaceInviteRejectsMissingInviteAndDoesNotAccept() {
        Member member = Member.createUser("google-1", "invitee@example.com", "Invitee");
        FakeAcceptWorkspaceInvitePort acceptWorkspaceInvitePort =
                new FakeAcceptWorkspaceInvitePort(new AcceptedWorkspaceInvite(501L, 101L, "google-1", WorkspaceMemberRole.MEMBER, ACCEPTED_AT));
        AcceptWorkspaceInviteService service = new AcceptWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(member)),
                new FakeLoadWorkspaceInvitePort(Optional.empty()),
                acceptWorkspaceInvitePort,
                CLOCK);

        assertThatThrownBy(() -> service.acceptWorkspaceInvite(
                        new AcceptWorkspaceInviteCommand("missing-token", "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace invite not found");
        assertThat(acceptWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void commandRejectsBlankValues() {
        assertThatThrownBy(() -> new AcceptWorkspaceInviteCommand(" ", "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token must not be blank");
        assertThatThrownBy(() -> new AcceptWorkspaceInviteCommand("invite-token", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberGoogleSub must not be blank");
    }

    private static WorkspaceInviteLookup pendingInvite() {
        return new WorkspaceInviteLookup(
                501L,
                101L,
                "invitee@example.com",
                "invite-token",
                WorkspaceInviteStatus.PENDING,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                LocalDateTime.of(2026, 5, 22, 10, 0),
                null);
    }

    /**
     * 회원 조회 port 호출 인자와 반환값을 제어하기 위한 테스트용 fake다.
     */
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

    /**
     * 초대 token 조회 결과를 제어하기 위한 테스트용 fake다.
     */
    private static class FakeLoadWorkspaceInvitePort implements LoadWorkspaceInvitePort {
        private final Optional<WorkspaceInviteLookup> invite;
        private String requestedToken;
        private boolean called;

        private FakeLoadWorkspaceInvitePort(Optional<WorkspaceInviteLookup> invite) {
            this.invite = invite;
        }

        @Override
        public Optional<WorkspaceInviteLookup> findByToken(String token) {
            this.called = true;
            this.requestedToken = token;
            return invite;
        }
    }

    /**
     * 수락 저장 요청을 캡처하기 위한 테스트용 fake다.
     */
    private static class FakeAcceptWorkspaceInvitePort implements AcceptWorkspaceInvitePort {
        private final AcceptedWorkspaceInvite acceptedWorkspaceInvite;
        private WorkspaceInviteAcceptance acceptance;
        private boolean called;

        private FakeAcceptWorkspaceInvitePort(AcceptedWorkspaceInvite acceptedWorkspaceInvite) {
            this.acceptedWorkspaceInvite = acceptedWorkspaceInvite;
        }

        @Override
        public AcceptedWorkspaceInvite accept(WorkspaceInviteAcceptance acceptance) {
            this.called = true;
            this.acceptance = acceptance;
            return acceptedWorkspaceInvite;
        }
    }
}
