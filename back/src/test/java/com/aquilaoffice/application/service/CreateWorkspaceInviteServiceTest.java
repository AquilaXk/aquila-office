package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RecordWorkspaceInviteEmailFailurePort;
import com.aquilaoffice.application.port.out.RunAfterCommitPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceInvite;
import com.aquilaoffice.application.port.out.SendWorkspaceInviteEmailPort;
import com.aquilaoffice.application.port.out.WorkspaceInviteEmailCommand;
import com.aquilaoffice.application.port.out.WorkspaceInviteEmailFailure;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceInvite;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 초대 생성 service가 권한 확인, token 생성, 도메인 생성, 저장 호출을 조합하는지 검증한다.
 */
class CreateWorkspaceInviteServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-15T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void createWorkspaceInviteBuildsPendingInviteAndSavesIt() {
        Member requester = Member.createUser("google-1", "admin@example.com", "Admin");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(requester));
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.of(new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.ADMIN,
                LocalDateTime.of(2026, 5, 15, 9, 0))));
        FakeSaveWorkspaceInvitePort saveWorkspaceInvitePort =
                new FakeSaveWorkspaceInvitePort(new SavedWorkspaceInvite(501L));
        FakeGenerateWorkspaceInviteTokenPort tokenPort =
                new FakeGenerateWorkspaceInviteTokenPort("generated-token");
        CreateWorkspaceInviteService service = new CreateWorkspaceInviteService(
                loadMemberPort, loadWorkspacePort, saveWorkspaceInvitePort, tokenPort, CLOCK);

        CreateWorkspaceInviteResult result = service.createWorkspaceInvite(
                new CreateWorkspaceInviteCommand(101L, " google-1 ", " invitee@example.com "));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspacePort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(loadWorkspacePort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(tokenPort.called).isTrue();
        assertThat(saveWorkspaceInvitePort.savedWorkspaceId).isEqualTo(101L);
        assertThat(saveWorkspaceInvitePort.savedInvite.getEmail()).isEqualTo("invitee@example.com");
        assertThat(saveWorkspaceInvitePort.savedInvite.getToken()).isEqualTo("generated-token");
        assertThat(saveWorkspaceInvitePort.savedInvite.getStatus()).isEqualTo(WorkspaceInviteStatus.PENDING);
        assertThat(saveWorkspaceInvitePort.savedInvite.getCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
        assertThat(saveWorkspaceInvitePort.savedInvite.getExpiresAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 22, 10, 0));

        assertThat(result.inviteId()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.email()).isEqualTo("invitee@example.com");
        assertThat(result.token()).isEqualTo("generated-token");
        assertThat(result.status()).isEqualTo(WorkspaceInviteStatus.PENDING);
        assertThat(result.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
        assertThat(result.expiresAt()).isEqualTo(LocalDateTime.of(2026, 5, 22, 10, 0));
    }

    @Test
    void createWorkspaceInviteSchedulesInviteEmailAfterCommit() {
        FakeRunAfterCommitPort afterCommitPort = new FakeRunAfterCommitPort();
        FakeSendWorkspaceInviteEmailPort emailPort = new FakeSendWorkspaceInviteEmailPort();
        CreateWorkspaceInviteService service = createService(
                WorkspaceMemberRole.ADMIN,
                new FakeSaveWorkspaceInvitePort(new SavedWorkspaceInvite(501L)),
                new FakeGenerateWorkspaceInviteTokenPort("generated-token"),
                afterCommitPort,
                emailPort,
                new FakeRecordWorkspaceInviteEmailFailurePort());

        service.createWorkspaceInvite(
                new CreateWorkspaceInviteCommand(101L, " google-1 ", " invitee@example.com "));

        assertThat(afterCommitPort.callbacks).hasSize(1);
        assertThat(emailPort.sentCommands).isEmpty();

        afterCommitPort.runAll();

        assertThat(emailPort.sentCommands).singleElement().satisfies(command -> {
            assertThat(command.inviteId()).isEqualTo(501L);
            assertThat(command.workspaceId()).isEqualTo(101L);
            assertThat(command.workspaceName()).isEqualTo("Team");
            assertThat(command.email()).isEqualTo("invitee@example.com");
            assertThat(command.token()).isEqualTo("generated-token");
            assertThat(command.expiresAt()).isEqualTo(LocalDateTime.of(2026, 5, 22, 10, 0));
        });
    }

    @Test
    void createWorkspaceInviteRecordsEmailFailureAfterCommit() {
        FakeRunAfterCommitPort afterCommitPort = new FakeRunAfterCommitPort();
        FakeSendWorkspaceInviteEmailPort emailPort =
                new FakeSendWorkspaceInviteEmailPort(new IllegalStateException("smtp down"));
        FakeRecordWorkspaceInviteEmailFailurePort failurePort = new FakeRecordWorkspaceInviteEmailFailurePort();
        CreateWorkspaceInviteService service = createService(
                WorkspaceMemberRole.ADMIN,
                new FakeSaveWorkspaceInvitePort(new SavedWorkspaceInvite(501L)),
                new FakeGenerateWorkspaceInviteTokenPort("generated-token"),
                afterCommitPort,
                emailPort,
                failurePort);

        service.createWorkspaceInvite(
                new CreateWorkspaceInviteCommand(101L, " google-1 ", " invitee@example.com "));
        afterCommitPort.runAll();

        assertThat(failurePort.failures).singleElement().satisfies(failure -> {
            assertThat(failure.inviteId()).isEqualTo(501L);
            assertThat(failure.workspaceId()).isEqualTo(101L);
            assertThat(failure.reason()).isEqualTo("smtp down");
            assertThat(failure.failedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
        });
    }

    @Test
    void createWorkspaceInviteRejectsNonAdminMembershipAndDoesNotSave() {
        Member requester = Member.createUser("google-1", "member@example.com", "Member");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(requester));
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.of(new GetWorkspaceDetailResult(
                101L,
                "Team",
                null,
                "google-2",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 15, 9, 0))));
        FakeSaveWorkspaceInvitePort saveWorkspaceInvitePort =
                new FakeSaveWorkspaceInvitePort(new SavedWorkspaceInvite(501L));
        FakeGenerateWorkspaceInviteTokenPort tokenPort =
                new FakeGenerateWorkspaceInviteTokenPort("generated-token");
        CreateWorkspaceInviteService service = new CreateWorkspaceInviteService(
                loadMemberPort, loadWorkspacePort, saveWorkspaceInvitePort, tokenPort, CLOCK);

        assertThatThrownBy(() -> service.createWorkspaceInvite(
                        new CreateWorkspaceInviteCommand(101L, "google-1", "invitee@example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(tokenPort.called).isFalse();
        assertThat(saveWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void createWorkspaceInviteRejectsUnknownRequesterAndDoesNotSave() {
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.empty());
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.empty());
        FakeSaveWorkspaceInvitePort saveWorkspaceInvitePort =
                new FakeSaveWorkspaceInvitePort(new SavedWorkspaceInvite(501L));
        FakeGenerateWorkspaceInviteTokenPort tokenPort =
                new FakeGenerateWorkspaceInviteTokenPort("generated-token");
        CreateWorkspaceInviteService service = new CreateWorkspaceInviteService(
                loadMemberPort, loadWorkspacePort, saveWorkspaceInvitePort, tokenPort, CLOCK);

        assertThatThrownBy(() -> service.createWorkspaceInvite(
                        new CreateWorkspaceInviteCommand(101L, "missing", "invitee@example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
        assertThat(tokenPort.called).isFalse();
        assertThat(saveWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new CreateWorkspaceInviteCommand(null, "google-1", "invitee@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must not be null");
        assertThatThrownBy(() -> new CreateWorkspaceInviteCommand(101L, " ", "invitee@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
        assertThatThrownBy(() -> new CreateWorkspaceInviteCommand(101L, "google-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");
    }

    private static CreateWorkspaceInviteService createService(
            WorkspaceMemberRole role,
            FakeSaveWorkspaceInvitePort saveWorkspaceInvitePort,
            FakeGenerateWorkspaceInviteTokenPort tokenPort,
            RunAfterCommitPort afterCommitPort,
            SendWorkspaceInviteEmailPort emailPort,
            RecordWorkspaceInviteEmailFailurePort failurePort) {
        return new CreateWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(new GetWorkspaceDetailResult(
                        101L,
                        "Team",
                        "Description",
                        "google-1",
                        role,
                        LocalDateTime.of(2026, 5, 15, 9, 0)))),
                saveWorkspaceInvitePort,
                tokenPort,
                CLOCK,
                afterCommitPort,
                emailPort,
                failurePort);
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
     * 워크스페이스 멤버십 조회 결과를 제어하기 위한 테스트용 fake다.
     */
    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> workspaceDetail;
        private Long requestedWorkspaceId;
        private String requestedGoogleSub;
        private boolean called;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> workspaceDetail) {
            this.workspaceDetail = workspaceDetail;
        }

        @Override
        public java.util.List<com.aquilaoffice.application.port.in.ListWorkspacesResult>
                findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            this.called = true;
            this.requestedWorkspaceId = workspaceId;
            this.requestedGoogleSub = googleSub;
            return workspaceDetail;
        }
    }

    /**
     * 저장 요청된 초대 도메인을 캡처하기 위한 테스트용 fake다.
     */
    private static class FakeSaveWorkspaceInvitePort implements SaveWorkspaceInvitePort {
        private final SavedWorkspaceInvite savedWorkspaceInvite;
        private Long savedWorkspaceId;
        private WorkspaceInvite savedInvite;
        private boolean called;

        private FakeSaveWorkspaceInvitePort(SavedWorkspaceInvite savedWorkspaceInvite) {
            this.savedWorkspaceInvite = savedWorkspaceInvite;
        }

        @Override
        public SavedWorkspaceInvite save(Long workspaceId, WorkspaceInvite invite) {
            this.called = true;
            this.savedWorkspaceId = workspaceId;
            this.savedInvite = invite;
            return savedWorkspaceInvite;
        }
    }

    /**
     * token 생성 결과를 고정하기 위한 테스트용 fake다.
     */
    private static class FakeGenerateWorkspaceInviteTokenPort implements GenerateWorkspaceInviteTokenPort {
        private final String token;
        private boolean called;

        private FakeGenerateWorkspaceInviteTokenPort(String token) {
            this.token = token;
        }

        @Override
        public String generateToken() {
            this.called = true;
            return token;
        }
    }

    private static class FakeRunAfterCommitPort implements RunAfterCommitPort {
        private final List<Runnable> callbacks = new ArrayList<>();

        @Override
        public void runAfterCommit(Runnable callback) {
            callbacks.add(callback);
        }

        private void runAll() {
            callbacks.forEach(Runnable::run);
        }
    }

    private static class FakeSendWorkspaceInviteEmailPort implements SendWorkspaceInviteEmailPort {
        private final List<WorkspaceInviteEmailCommand> sentCommands = new ArrayList<>();
        private final RuntimeException failure;

        private FakeSendWorkspaceInviteEmailPort() {
            this(null);
        }

        private FakeSendWorkspaceInviteEmailPort(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void send(WorkspaceInviteEmailCommand command) {
            if (failure != null) {
                throw failure;
            }
            sentCommands.add(command);
        }
    }

    private static class FakeRecordWorkspaceInviteEmailFailurePort
            implements RecordWorkspaceInviteEmailFailurePort {
        private final List<WorkspaceInviteEmailFailure> failures = new ArrayList<>();

        @Override
        public void record(WorkspaceInviteEmailFailure failure) {
            failures.add(failure);
        }
    }
}
