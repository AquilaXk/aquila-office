package com.aquilaoffice.adapter.out.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceInvite;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceInvite;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 워크스페이스 초대 persistence adapter가 outbound port 계약대로 저장과 token 조회를 수행하는지 검증한다.
 */
@DataJpaTest
@Import(WorkspaceInvitePersistenceAdapter.class)
class WorkspaceInvitePersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private WorkspaceInviteJpaRepository workspaceInviteJpaRepository;

    @Autowired
    private SaveWorkspaceInvitePort saveWorkspaceInvitePort;

    @Autowired
    private LoadWorkspaceInvitePort loadWorkspaceInvitePort;

    @Test
    void savePersistsInviteAndFindByTokenReturnsLookup() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("google-1");
        Workspace workspace = Workspace.create(
                "Team", "Description", Member.createUser("google-1", "creator@example.com", "Creator"));
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 22, 10, 0);
        WorkspaceInvite invite =
                WorkspaceInvite.create(workspace, " invitee@example.com ", " invite-token ", createdAt, expiresAt);

        SavedWorkspaceInvite savedInvite = saveWorkspaceInvitePort.save(workspaceEntity.getId(), invite);

        assertThat(savedInvite.inviteId()).isNotNull();
        assertThat(loadWorkspaceInvitePort.findByToken("invite-token"))
                .hasValueSatisfying(found -> {
                    assertThat(found.inviteId()).isEqualTo(savedInvite.inviteId());
                    assertThat(found.workspaceId()).isEqualTo(workspaceEntity.getId());
                    assertThat(found.email()).isEqualTo("invitee@example.com");
                    assertThat(found.token()).isEqualTo("invite-token");
                    assertThat(found.status()).isEqualTo(WorkspaceInviteStatus.PENDING);
                    assertThat(found.createdAt()).isEqualTo(createdAt);
                    assertThat(found.expiresAt()).isEqualTo(expiresAt);
                    assertThat(found.acceptedAt()).isNull();
                });
        assertThat(loadWorkspaceInvitePort.findByToken("missing-token")).isEmpty();
    }

    @Test
    void saveRejectsMissingWorkspaceEntity() {
        Workspace workspace = Workspace.create(
                "Team", null, Member.createUser("google-missing", "creator@example.com", "Creator"));
        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace,
                "invitee@example.com",
                "invite-token",
                LocalDateTime.of(2026, 5, 15, 10, 0),
                LocalDateTime.of(2026, 5, 22, 10, 0));

        assertThatThrownBy(() -> saveWorkspaceInvitePort.save(404L, invite))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace entity not found");
        assertThat(workspaceInviteJpaRepository.count()).isZero();
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
