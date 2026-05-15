package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceInviteQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteResult;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteLookup;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 초대 조회 service가 token 조회 port 결과를 application 결과로 변환하는지 검증한다.
 */
class GetWorkspaceInviteServiceTest {

    @Test
    void getWorkspaceInviteReturnsInviteByToken() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 22, 10, 0);
        LocalDateTime acceptedAt = LocalDateTime.of(2026, 5, 16, 11, 0);
        FakeLoadWorkspaceInvitePort loadWorkspaceInvitePort = new FakeLoadWorkspaceInvitePort(Optional.of(
                new WorkspaceInviteLookup(
                        501L,
                        101L,
                        "invitee@example.com",
                        "invite-token",
                        WorkspaceInviteStatus.ACCEPTED,
                        createdAt,
                        expiresAt,
                        acceptedAt)));
        GetWorkspaceInviteService service = new GetWorkspaceInviteService(loadWorkspaceInvitePort);

        GetWorkspaceInviteResult result =
                service.getWorkspaceInvite(new GetWorkspaceInviteQuery(" invite-token "));

        assertThat(loadWorkspaceInvitePort.requestedToken).isEqualTo("invite-token");
        assertThat(result.inviteId()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.email()).isEqualTo("invitee@example.com");
        assertThat(result.token()).isEqualTo("invite-token");
        assertThat(result.status()).isEqualTo(WorkspaceInviteStatus.ACCEPTED);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
        assertThat(result.acceptedAt()).isEqualTo(acceptedAt);
    }

    @Test
    void getWorkspaceInviteRejectsMissingToken() {
        GetWorkspaceInviteService service = new GetWorkspaceInviteService(new FakeLoadWorkspaceInvitePort(Optional.empty()));

        assertThatThrownBy(() -> service.getWorkspaceInvite(new GetWorkspaceInviteQuery("missing-token")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace invite not found");
    }

    @Test
    void queryRejectsBlankToken() {
        assertThatThrownBy(() -> new GetWorkspaceInviteQuery(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token must not be blank");
    }

    /**
     * token 조회 port 호출 인자와 반환값을 제어하기 위한 테스트용 fake다.
     */
    private static class FakeLoadWorkspaceInvitePort implements LoadWorkspaceInvitePort {
        private final Optional<WorkspaceInviteLookup> lookup;
        private String requestedToken;

        private FakeLoadWorkspaceInvitePort(Optional<WorkspaceInviteLookup> lookup) {
            this.lookup = lookup;
        }

        @Override
        public Optional<WorkspaceInviteLookup> findByToken(String token) {
            this.requestedToken = token;
            return lookup;
        }
    }
}
