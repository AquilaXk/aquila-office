package com.aquilaoffice.adapter.out.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.member.MemberPersistenceAdapter;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspace;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceMember;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 워크스페이스 persistence adapter가 JPA entity와 domain 모델을 올바르게 변환하는지 검증한다.
 */
@DataJpaTest
@Import({MemberPersistenceAdapter.class, WorkspacePersistenceAdapter.class})
class WorkspacePersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private WorkspaceMemberJpaRepository workspaceMemberJpaRepository;

    @Autowired
    private LoadMemberPort loadMemberPort;

    @Autowired
    private SaveWorkspacePort saveWorkspacePort;

    @Autowired
    private LoadWorkspacePort loadWorkspacePort;

    @Autowired
    private LoadWorkspaceMemberPort loadWorkspaceMemberPort;

    @Test
    void loadMemberByGoogleSubReturnsDomainMember() {
        memberJpaRepository.save(MemberJpaEntity.create(
                "google-1", "user@example.com", "User", MemberRole.USER));

        assertThat(loadMemberPort.findByGoogleSub("google-1"))
                .hasValueSatisfying(member -> {
                    assertThat(member.getGoogleSub()).isEqualTo("google-1");
                    assertThat(member.getEmail()).isEqualTo("user@example.com");
                    assertThat(member.getName()).isEqualTo("User");
                    assertThat(member.getRole()).isEqualTo(MemberRole.USER);
                });
        assertThat(loadMemberPort.findByGoogleSub("missing")).isEmpty();
    }

    @Test
    void savePersistsWorkspaceAndAdminMembership() {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                "google-1", "user@example.com", "User", MemberRole.USER));
        Member creator = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", "Description", creator);
        LocalDateTime joinedAt = LocalDateTime.of(2026, 5, 15, 10, 0);
        WorkspaceMember workspaceMember =
                WorkspaceMember.create(workspace, creator, WorkspaceMemberRole.ADMIN, joinedAt);

        SavedWorkspace savedWorkspace = saveWorkspacePort.save(workspace, workspaceMember);

        assertThat(savedWorkspace.workspaceId()).isNotNull();
        WorkspaceJpaEntity savedWorkspaceEntity =
                workspaceJpaRepository.findById(savedWorkspace.workspaceId()).orElseThrow();
        assertThat(savedWorkspaceEntity.getName()).isEqualTo("Team");
        assertThat(savedWorkspaceEntity.getDescription()).isEqualTo("Description");
        assertThat(savedWorkspaceEntity.getCreatedByMember().getId()).isEqualTo(creatorEntity.getId());

        WorkspaceMemberJpaEntity savedMemberEntity = workspaceMemberJpaRepository
                .findByWorkspaceIdAndMemberId(savedWorkspace.workspaceId(), creatorEntity.getId())
                .orElseThrow();
        assertThat(savedMemberEntity.getRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(savedMemberEntity.getJoinedAt()).isEqualTo(joinedAt);
    }

    @Test
    void saveRejectsMissingMemberEntity() {
        Member creator = Member.createUser("missing", "user@example.com", "User");
        Workspace workspace = Workspace.create("Team", null, creator);
        WorkspaceMember workspaceMember =
                WorkspaceMember.create(workspace, creator, WorkspaceMemberRole.ADMIN, LocalDateTime.now());

        assertThatThrownBy(() -> saveWorkspacePort.save(workspace, workspaceMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member entity not found");
        assertThat(workspaceJpaRepository.count()).isZero();
        assertThat(workspaceMemberJpaRepository.count()).isZero();
    }

    @Test
    void findJoinedWorkspacesByMemberGoogleSubReturnsOnlyActiveJoinedWorkspaces() {
        MemberJpaEntity memberEntity = memberJpaRepository.save(MemberJpaEntity.create(
                "google-1", "user@example.com", "User", MemberRole.USER));
        memberJpaRepository.save(MemberJpaEntity.create(
                "google-2", "other@example.com", "Other", MemberRole.USER));
        Member member = Member.createUser("google-1", "user@example.com", "User");
        Member other = Member.createUser("google-2", "other@example.com", "Other");

        Workspace activeWorkspace = Workspace.create("Active", "Visible", member);
        saveWorkspacePort.save(
                activeWorkspace,
                WorkspaceMember.create(
                        activeWorkspace,
                        member,
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0)));

        Workspace deletedWorkspace = Workspace.create("Deleted", "Hidden", member);
        deletedWorkspace.softDelete(LocalDateTime.of(2026, 5, 15, 11, 0));
        saveWorkspacePort.save(
                deletedWorkspace,
                WorkspaceMember.create(
                        deletedWorkspace,
                        member,
                        WorkspaceMemberRole.MEMBER,
                        LocalDateTime.of(2026, 5, 15, 10, 30)));

        Workspace otherWorkspace = Workspace.create("Other", "Not joined", other);
        saveWorkspacePort.save(
                otherWorkspace,
                WorkspaceMember.create(
                        otherWorkspace,
                        other,
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 12, 0)));

        assertThat(loadWorkspacePort.findJoinedWorkspacesByMemberGoogleSub("google-1"))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.name()).isEqualTo("Active");
                    assertThat(result.description()).isEqualTo("Visible");
                    assertThat(result.creatorGoogleSub()).isEqualTo("google-1");
                    assertThat(result.myRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
                    assertThat(result.joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
                    assertThat(result.workspaceId()).isNotNull();
                });
        assertThat(workspaceMemberJpaRepository
                        .findByWorkspaceIdAndMemberId(
                                loadWorkspacePort.findJoinedWorkspacesByMemberGoogleSub("google-1")
                                        .getFirst()
                                        .workspaceId(),
                                memberEntity.getId()))
                .isPresent();
    }

    @Test
    void findJoinedWorkspaceByIdAndMemberGoogleSubReturnsActiveJoinedWorkspaceDetail() {
        MemberJpaEntity memberEntity = memberJpaRepository.save(MemberJpaEntity.create(
                "google-1", "user@example.com", "User", MemberRole.USER));
        Member member = Member.createUser("google-1", "user@example.com", "User");
        Workspace workspace = Workspace.create("Detail", "Metadata", member);
        Long workspaceId = saveWorkspacePort.save(
                        workspace,
                        WorkspaceMember.create(
                                workspace,
                                member,
                                WorkspaceMemberRole.ADMIN,
                                LocalDateTime.of(2026, 5, 15, 10, 0)))
                .workspaceId();

        assertThat(loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, "google-1"))
                .hasValueSatisfying(result -> {
                    assertThat(result.workspaceId()).isEqualTo(workspaceId);
                    assertThat(result.name()).isEqualTo("Detail");
                    assertThat(result.description()).isEqualTo("Metadata");
                    assertThat(result.creatorGoogleSub()).isEqualTo("google-1");
                    assertThat(result.myRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
                    assertThat(result.joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
                });
        assertThat(workspaceMemberJpaRepository.findByWorkspaceIdAndMemberId(workspaceId, memberEntity.getId()))
                .isPresent();
    }

    @Test
    void findJoinedWorkspaceByIdAndMemberGoogleSubExcludesDeletedOrUnjoinedWorkspace() {
        memberJpaRepository.save(MemberJpaEntity.create(
                "google-1", "user@example.com", "User", MemberRole.USER));
        memberJpaRepository.save(MemberJpaEntity.create(
                "google-2", "other@example.com", "Other", MemberRole.USER));
        Member member = Member.createUser("google-1", "user@example.com", "User");
        Member other = Member.createUser("google-2", "other@example.com", "Other");

        Workspace deletedWorkspace = Workspace.create("Deleted", null, member);
        deletedWorkspace.softDelete(LocalDateTime.of(2026, 5, 15, 11, 0));
        Long deletedWorkspaceId = saveWorkspacePort.save(
                        deletedWorkspace,
                        WorkspaceMember.create(
                                deletedWorkspace,
                                member,
                                WorkspaceMemberRole.ADMIN,
                                LocalDateTime.of(2026, 5, 15, 10, 0)))
                .workspaceId();

        Workspace otherWorkspace = Workspace.create("Other", null, other);
        Long otherWorkspaceId = saveWorkspacePort.save(
                        otherWorkspace,
                        WorkspaceMember.create(
                                otherWorkspace,
                                other,
                                WorkspaceMemberRole.ADMIN,
                                LocalDateTime.of(2026, 5, 15, 12, 0)))
                .workspaceId();

        assertThat(loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(deletedWorkspaceId, "google-1"))
                .isEmpty();
        assertThat(loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(otherWorkspaceId, "google-1"))
                .isEmpty();
    }

    @Test
    void findActiveWorkspaceMembersByWorkspaceIdReturnsOnlyMembersOfActiveWorkspace() {
        MemberJpaEntity adminEntity = memberJpaRepository.save(MemberJpaEntity.create(
                "google-1", "admin@example.com", "Admin", MemberRole.USER));
        MemberJpaEntity memberEntity = memberJpaRepository.save(MemberJpaEntity.create(
                "google-2", "member@example.com", "Member", MemberRole.USER));
        Member admin = Member.createUser("google-1", "admin@example.com", "Admin");

        Workspace activeWorkspace = Workspace.create("Active", "Visible", admin);
        Long activeWorkspaceId = saveWorkspacePort.save(
                        activeWorkspace,
                        WorkspaceMember.create(
                                activeWorkspace,
                                admin,
                                WorkspaceMemberRole.ADMIN,
                                LocalDateTime.of(2026, 5, 15, 10, 0)))
                .workspaceId();
        WorkspaceJpaEntity activeWorkspaceEntity =
                workspaceJpaRepository.findById(activeWorkspaceId).orElseThrow();
        workspaceMemberJpaRepository.save(WorkspaceMemberJpaEntity.create(
                activeWorkspaceEntity,
                memberEntity,
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 15, 11, 0)));

        Workspace deletedWorkspace = Workspace.create("Deleted", "Hidden", admin);
        deletedWorkspace.softDelete(LocalDateTime.of(2026, 5, 15, 12, 0));
        Long deletedWorkspaceId = saveWorkspacePort.save(
                        deletedWorkspace,
                        WorkspaceMember.create(
                                deletedWorkspace,
                                admin,
                                WorkspaceMemberRole.ADMIN,
                                LocalDateTime.of(2026, 5, 15, 12, 30)))
                .workspaceId();

        List<ListWorkspaceMembersResult> results =
                loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(activeWorkspaceId);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).memberId()).isEqualTo(adminEntity.getId());
        assertThat(results.get(0).memberGoogleSub()).isEqualTo("google-1");
        assertThat(results.get(0).email()).isEqualTo("admin@example.com");
        assertThat(results.get(0).name()).isEqualTo("Admin");
        assertThat(results.get(0).role()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(results.get(0).joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
        assertThat(results.get(1).memberId()).isEqualTo(memberEntity.getId());
        assertThat(results.get(1).memberGoogleSub()).isEqualTo("google-2");
        assertThat(results.get(1).role()).isEqualTo(WorkspaceMemberRole.MEMBER);
        assertThat(results.get(1).joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 11, 0));

        assertThat(loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(deletedWorkspaceId))
                .isEmpty();
    }
}
