package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateGithubCredentialCommand;
import com.aquilaoffice.application.port.in.CreateGithubCredentialResult;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListGithubCredentialsQuery;
import com.aquilaoffice.application.port.in.ListGithubCredentialsResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialCommand;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialResult;
import com.aquilaoffice.application.port.out.DeleteGithubCredentialPort;
import com.aquilaoffice.application.port.out.ExistsGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialsPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveGithubCredentialPort;
import com.aquilaoffice.application.port.out.UpdateGithubCredentialPort;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * GitHub 자격 증명 application service가 요청자 확인, 워크스페이스 권한 확인, 저장 port 호출을 조합하는지 검증한다.
 */
class GithubCredentialManagementServiceTest {

    @Test
    void createGithubCredentialSavesCredentialWhenRequesterIsAdmin() {
        FakeSaveGithubCredentialPort savePort = new FakeSaveGithubCredentialPort(
                GithubCredential.restore(301L, 101L, "Main Token", "ghp_1234567890abcdef", "google-1"));
        FakeExistsGithubCredentialPort existsPort = new FakeExistsGithubCredentialPort(false);
        CreateGithubCredentialService service = new CreateGithubCredentialService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                existsPort,
                savePort);

        CreateGithubCredentialResult result = service.createGithubCredential(
                new CreateGithubCredentialCommand(101L, " google-1 ", " Main Token ", " ghp_1234567890abcdef "));

        assertThat(existsPort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(existsPort.requestedDisplayName).isEqualTo("Main Token");
        assertThat(savePort.savedCredential.getWorkspaceId()).isEqualTo(101L);
        assertThat(savePort.savedCredential.getDisplayName()).isEqualTo("Main Token");
        assertThat(savePort.savedCredential.getToken()).isEqualTo("ghp_1234567890abcdef");
        assertThat(savePort.savedCredential.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(result.credentialId()).isEqualTo(301L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.displayName()).isEqualTo("Main Token");
        assertThat(result.maskedToken()).isEqualTo("ghp_****cdef");
    }

    @Test
    void createGithubCredentialRejectsNonAdminAndDuplicateName() {
        FakeSaveGithubCredentialPort savePort = new FakeSaveGithubCredentialPort(null);
        CreateGithubCredentialService nonAdminService = new CreateGithubCredentialService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeExistsGithubCredentialPort(false),
                savePort);

        assertThatThrownBy(() -> nonAdminService.createGithubCredential(
                        new CreateGithubCredentialCommand(101L, "google-1", "Main Token", "token")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(savePort.called).isFalse();

        CreateGithubCredentialService duplicateService = new CreateGithubCredentialService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                new FakeExistsGithubCredentialPort(true),
                savePort);

        assertThatThrownBy(() -> duplicateService.createGithubCredential(
                        new CreateGithubCredentialCommand(101L, "google-1", "Main Token", "token")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("github credential displayName already exists");
        assertThat(savePort.called).isFalse();
    }

    @Test
    void listGithubCredentialsReturnsMaskedCredentialsForWorkspaceMember() {
        FakeLoadGithubCredentialsPort credentialsPort = new FakeLoadGithubCredentialsPort(List.of(
                GithubCredential.restore(301L, 101L, "Main Token", "ghp_1234567890abcdef", "google-1"),
                GithubCredential.restore(302L, 101L, "Backup Token", "short", "google-2")));
        ListGithubCredentialsService service = new ListGithubCredentialsService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                credentialsPort);

        List<ListGithubCredentialsResult> results =
                service.listGithubCredentials(new ListGithubCredentialsQuery(101L, " google-2 "));

        assertThat(credentialsPort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(results).extracting(ListGithubCredentialsResult::maskedToken)
                .containsExactly("ghp_****cdef", "****");
    }

    @Test
    void updateGithubCredentialRequiresAdminAndUpdatesLoadedCredential() {
        GithubCredential existing =
                GithubCredential.restore(301L, 101L, "Main Token", "ghp_1234567890abcdef", "google-1");
        FakeUpdateGithubCredentialPort updatePort = new FakeUpdateGithubCredentialPort();
        UpdateGithubCredentialService service = new UpdateGithubCredentialService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                new FakeLoadGithubCredentialPort(Optional.of(existing)),
                updatePort);

        UpdateGithubCredentialResult result = service.updateGithubCredential(
                new UpdateGithubCredentialCommand(101L, 301L, " google-1 ", " Backup Token ", " ghp_fedcba0987654321 "));

        assertThat(updatePort.updatedCredential.getDisplayName()).isEqualTo("Backup Token");
        assertThat(updatePort.updatedCredential.getToken()).isEqualTo("ghp_fedcba0987654321");
        assertThat(result.credentialId()).isEqualTo(301L);
        assertThat(result.maskedToken()).isEqualTo("ghp_****4321");
    }

    @Test
    void updateGithubCredentialRejectsMissingCredential() {
        FakeUpdateGithubCredentialPort updatePort = new FakeUpdateGithubCredentialPort();
        UpdateGithubCredentialService service = new UpdateGithubCredentialService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                new FakeLoadGithubCredentialPort(Optional.empty()),
                updatePort);

        assertThatThrownBy(() -> service.updateGithubCredential(
                        new UpdateGithubCredentialCommand(101L, 301L, "google-1", "Name", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("github credential not found");
        assertThat(updatePort.called).isFalse();
    }

    @Test
    void deleteGithubCredentialRequiresAdminAndDelegatesDeletion() {
        FakeDeleteGithubCredentialPort deletePort = new FakeDeleteGithubCredentialPort();
        DeleteGithubCredentialService service = new DeleteGithubCredentialService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                new FakeLoadGithubCredentialPort(Optional.of(
                        GithubCredential.restore(301L, 101L, "Main Token", "token", "google-1"))),
                deletePort);

        service.deleteGithubCredential(new DeleteGithubCredentialCommand(101L, 301L, " google-1 "));

        assertThat(deletePort.deletedWorkspaceId).isEqualTo(101L);
        assertThat(deletePort.deletedCredentialId).isEqualTo(301L);
    }

    @Test
    void commandsRejectInvalidRequiredValues() {
        assertThatThrownBy(() -> new CreateGithubCredentialCommand(0L, "google-1", "Main", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new CreateGithubCredentialCommand(101L, " ", "Main", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
        assertThatThrownBy(() -> new CreateGithubCredentialCommand(101L, "google-1", " ", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("displayName must not be blank");
        assertThatThrownBy(() -> new CreateGithubCredentialCommand(101L, "google-1", "Main", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token must not be blank");
        assertThatThrownBy(() -> new ListGithubCredentialsQuery(null, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must not be null");
        assertThatThrownBy(() -> new UpdateGithubCredentialCommand(101L, 0L, "google-1", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("credentialId must be positive");
        assertThatThrownBy(() -> new DeleteGithubCredentialCommand(101L, 0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("credentialId must be positive");
    }

    private static GetWorkspaceDetailResult workspaceDetail(WorkspaceMemberRole role) {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                role,
                LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> detailResult;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in github credential service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            return detailResult;
        }
    }

    private static class FakeExistsGithubCredentialPort implements ExistsGithubCredentialPort {
        private final boolean exists;
        private Long requestedWorkspaceId;
        private String requestedDisplayName;

        private FakeExistsGithubCredentialPort(boolean exists) {
            this.exists = exists;
        }

        @Override
        public boolean existsActiveDisplayName(Long workspaceId, String displayName) {
            this.requestedWorkspaceId = workspaceId;
            this.requestedDisplayName = displayName;
            return exists;
        }
    }

    private static class FakeSaveGithubCredentialPort implements SaveGithubCredentialPort {
        private final GithubCredential result;
        private GithubCredential savedCredential;
        private boolean called;

        private FakeSaveGithubCredentialPort(GithubCredential result) {
            this.result = result;
        }

        @Override
        public GithubCredential save(GithubCredential credential) {
            this.called = true;
            this.savedCredential = credential;
            return result;
        }
    }

    private static class FakeLoadGithubCredentialsPort implements LoadGithubCredentialsPort {
        private final List<GithubCredential> credentials;
        private Long requestedWorkspaceId;

        private FakeLoadGithubCredentialsPort(List<GithubCredential> credentials) {
            this.credentials = credentials;
        }

        @Override
        public List<GithubCredential> findActiveByWorkspaceId(Long workspaceId) {
            this.requestedWorkspaceId = workspaceId;
            return credentials;
        }
    }

    private static class FakeLoadGithubCredentialPort implements LoadGithubCredentialPort {
        private final Optional<GithubCredential> credential;

        private FakeLoadGithubCredentialPort(Optional<GithubCredential> credential) {
            this.credential = credential;
        }

        @Override
        public Optional<GithubCredential> findActiveByIdAndWorkspaceId(Long credentialId, Long workspaceId) {
            return credential;
        }
    }

    private static class FakeUpdateGithubCredentialPort implements UpdateGithubCredentialPort {
        private GithubCredential updatedCredential;
        private boolean called;

        @Override
        public GithubCredential update(GithubCredential credential) {
            this.called = true;
            this.updatedCredential = credential;
            return credential;
        }
    }

    private static class FakeDeleteGithubCredentialPort implements DeleteGithubCredentialPort {
        private Long deletedWorkspaceId;
        private Long deletedCredentialId;

        @Override
        public void delete(Long credentialId, Long workspaceId) {
            this.deletedCredentialId = credentialId;
            this.deletedWorkspaceId = workspaceId;
        }
    }
}
