package com.aquilaoffice.adapter.out.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceInvite;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * 워크스페이스 초대 JPA repository가 token 단위 저장과 조회를 보장하는지 검증한다.
 */
@DataJpaTest
class WorkspaceInviteJpaRepositoryTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private WorkspaceInviteJpaRepository workspaceInviteJpaRepository;

    @Test
    void savePersistsInviteValuesAndFindsByToken() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("google-1");
        Workspace workspace = Workspace.create(
                "Team", "Description", Member.createUser("google-1", "creator@example.com", "Creator"));
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 22, 10, 0);
        WorkspaceInvite invite = WorkspaceInvite.create(
                workspace, " invitee@example.com ", " invite-token ", createdAt, expiresAt);

        workspaceInviteJpaRepository.save(WorkspaceInviteJpaEntity.from(invite, workspaceEntity));

        assertThat(workspaceInviteJpaRepository.findByToken("invite-token"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getWorkspace().getId()).isEqualTo(workspaceEntity.getId());
                    assertThat(found.getEmail()).isEqualTo("invitee@example.com");
                    assertThat(found.getToken()).isEqualTo("invite-token");
                    assertThat(found.getStatus()).isEqualTo(WorkspaceInviteStatus.PENDING);
                    assertThat(found.getCreatedAt()).isEqualTo(createdAt);
                    assertThat(found.getExpiresAt()).isEqualTo(expiresAt);
                    assertThat(found.getAcceptedAt()).isNull();
                });
        assertThat(workspaceInviteJpaRepository.findByToken("missing-token")).isEmpty();
    }

    @Test
    void savePersistsAcceptedInviteState() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("google-2");
        Workspace workspace = Workspace.create(
                "Team", null, Member.createUser("google-2", "creator2@example.com", "Creator2"));
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 22, 10, 0);
        LocalDateTime acceptedAt = LocalDateTime.of(2026, 5, 16, 11, 0);
        WorkspaceInvite invite =
                WorkspaceInvite.create(workspace, "invitee2@example.com", "accepted-token", createdAt, expiresAt);
        invite.accept(acceptedAt);

        workspaceInviteJpaRepository.save(WorkspaceInviteJpaEntity.from(invite, workspaceEntity));

        assertThat(workspaceInviteJpaRepository.findByToken("accepted-token"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getStatus()).isEqualTo(WorkspaceInviteStatus.ACCEPTED);
                    assertThat(found.getAcceptedAt()).isEqualTo(acceptedAt);
                });
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
