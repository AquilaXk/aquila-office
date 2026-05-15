package com.aquilaoffice.domain.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.domain.member.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 초대 도메인 모델의 토큰, 만료, 상태 전이 규칙을 검증한다.
 */
class WorkspaceInviteTest {

    @Test
    void createTrimsEmailAndTokenAndStartsPending() {
        Workspace workspace = workspace();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        LocalDateTime expiresAt = createdAt.plusDays(7);

        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace, " user@example.com ", " token-123 ", createdAt, expiresAt);

        assertThat(invite.getWorkspace()).isSameAs(workspace);
        assertThat(invite.getEmail()).isEqualTo("user@example.com");
        assertThat(invite.getToken()).isEqualTo("token-123");
        assertThat(invite.getCreatedAt()).isEqualTo(createdAt);
        assertThat(invite.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(invite.getStatus()).isEqualTo(WorkspaceInviteStatus.PENDING);
        assertThat(invite.getAcceptedAt()).isNull();
    }

    @Test
    void createRejectsMissingArguments() {
        Workspace workspace = workspace();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        LocalDateTime expiresAt = createdAt.plusDays(7);

        assertThatThrownBy(() -> WorkspaceInvite.create(null, "user@example.com", "token", createdAt, expiresAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace must not be null");
        assertThatThrownBy(() -> WorkspaceInvite.create(workspace, " ", "token", createdAt, expiresAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");
        assertThatThrownBy(() -> WorkspaceInvite.create(workspace, "user@example.com", " ", createdAt, expiresAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token must not be blank");
        assertThatThrownBy(() -> WorkspaceInvite.create(workspace, "user@example.com", "token", null, expiresAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdAt must not be null");
        assertThatThrownBy(() -> WorkspaceInvite.create(workspace, "user@example.com", "token", createdAt, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresAt must not be null");
        assertThatThrownBy(() -> WorkspaceInvite.create(workspace, "user@example.com", "token", createdAt, createdAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresAt must be after createdAt");
    }

    @Test
    void acceptChangesPendingInviteToAccepted() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace(), "user@example.com", "token", createdAt, createdAt.plusDays(7));
        LocalDateTime acceptedAt = LocalDateTime.of(2026, 5, 15, 11, 0);

        invite.accept(acceptedAt);

        assertThat(invite.getStatus()).isEqualTo(WorkspaceInviteStatus.ACCEPTED);
        assertThat(invite.getAcceptedAt()).isEqualTo(acceptedAt);
    }

    @Test
    void acceptRejectsExpiredInvite() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace(), "user@example.com", "token", createdAt, createdAt.plusDays(1));

        assertThatThrownBy(() -> invite.accept(LocalDateTime.of(2026, 5, 17, 10, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("invite is expired");
        assertThat(invite.getStatus()).isEqualTo(WorkspaceInviteStatus.PENDING);
        assertThat(invite.getAcceptedAt()).isNull();
    }

    @Test
    void expireChangesPendingInviteToExpiredAfterExpiresAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace(), "user@example.com", "token", createdAt, createdAt.plusDays(1));

        invite.expire(LocalDateTime.of(2026, 5, 16, 10, 0));

        assertThat(invite.getStatus()).isEqualTo(WorkspaceInviteStatus.EXPIRED);
        assertThat(invite.isExpiredAt(LocalDateTime.of(2026, 5, 16, 10, 1))).isTrue();
    }

    @Test
    void acceptedInviteDoesNotExpire() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace(), "user@example.com", "token", createdAt, createdAt.plusDays(1));
        invite.accept(LocalDateTime.of(2026, 5, 15, 11, 0));

        invite.expire(LocalDateTime.of(2026, 5, 16, 10, 0));

        assertThat(invite.getStatus()).isEqualTo(WorkspaceInviteStatus.ACCEPTED);
        assertThat(invite.isExpiredAt(LocalDateTime.of(2026, 5, 17, 10, 0))).isFalse();
    }

    private Workspace workspace() {
        Member creator = Member.createUser("google-1", "owner@example.com", "Owner");
        return Workspace.create("Team", null, creator);
    }
}
