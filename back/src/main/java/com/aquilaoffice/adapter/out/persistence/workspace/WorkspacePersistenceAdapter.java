package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.ChangeWorkspaceMemberRolePort;
import com.aquilaoffice.application.port.out.DeleteWorkspacePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RemoveWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspace;
import com.aquilaoffice.application.port.out.UpdateWorkspacePort;
import com.aquilaoffice.application.port.out.UpdatedWorkspace;
import com.aquilaoffice.application.port.out.WorkspaceDeletion;
import com.aquilaoffice.application.port.out.WorkspaceMemberRemoval;
import com.aquilaoffice.application.port.out.WorkspaceMemberRoleChange;
import com.aquilaoffice.application.port.out.WorkspaceUpdate;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceMember;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 워크스페이스 저장 outbound port를 JPA repository 조합으로 구현하는 persistence adapter다.
 */
@Repository
public class WorkspacePersistenceAdapter
        implements SaveWorkspacePort,
                LoadWorkspacePort,
                LoadWorkspaceMemberPort,
                UpdateWorkspacePort,
                DeleteWorkspacePort,
                ChangeWorkspaceMemberRolePort,
                RemoveWorkspaceMemberPort {

    private final MemberJpaRepository memberJpaRepository;
    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final WorkspaceMemberJpaRepository workspaceMemberJpaRepository;

    public WorkspacePersistenceAdapter(
            MemberJpaRepository memberJpaRepository,
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceMemberJpaRepository workspaceMemberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.workspaceMemberJpaRepository = workspaceMemberJpaRepository;
    }

    @Override
    @Transactional
    public SavedWorkspace save(Workspace workspace, WorkspaceMember workspaceMember) {
        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(workspaceMember, "workspaceMember must not be null");

        MemberJpaEntity creatorEntity = findMemberEntity(workspace.getCreatedByMember());
        MemberJpaEntity memberEntity = findMemberEntity(workspaceMember.getMember());
        // aggregate root 저장 후 생성된 PK를 멤버 조인 entity에 연결한다.
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
        workspaceMemberJpaRepository.save(WorkspaceMemberJpaEntity.from(workspaceMember, workspaceEntity, memberEntity));

        return new SavedWorkspace(workspaceEntity.getId());
    }

    private MemberJpaEntity findMemberEntity(Member member) {
        return memberJpaRepository
                .findByGoogleSub(member.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member entity not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
        Objects.requireNonNull(googleSub, "googleSub must not be null");

        return workspaceMemberJpaRepository.findActiveJoinedWorkspacesByMemberGoogleSub(googleSub).stream()
                .map(this::toListResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
            Long workspaceId, String googleSub) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(googleSub, "googleSub must not be null");

        return workspaceMemberJpaRepository
                .findActiveJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .map(this::toDetailResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListWorkspaceMembersResult> findActiveWorkspaceMembersByWorkspaceId(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");

        return workspaceMemberJpaRepository.findActiveWorkspaceMembersByWorkspaceId(workspaceId).stream()
                .map(this::toMemberResult)
                .toList();
    }

    @Override
    @Transactional
    public UpdatedWorkspace update(WorkspaceUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(update.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));

        workspaceEntity.update(update.name(), update.description());
        return new UpdatedWorkspace(
                workspaceEntity.getId(),
                workspaceEntity.getName(),
                workspaceEntity.getDescription(),
                workspaceEntity.getCreatedByMember().getGoogleSub());
    }

    @Override
    @Transactional
    public void delete(WorkspaceDeletion deletion) {
        Objects.requireNonNull(deletion, "deletion must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(deletion.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));

        workspaceEntity.softDelete(deletion.deletedAt());
    }

    @Override
    @Transactional
    public void changeRole(WorkspaceMemberRoleChange change) {
        Objects.requireNonNull(change, "change must not be null");
        WorkspaceMemberJpaEntity workspaceMemberEntity = workspaceMemberJpaRepository
                .findActiveWorkspaceMemberByWorkspaceIdAndMemberId(change.workspaceId(), change.memberId())
                .orElseThrow(() -> new IllegalArgumentException("workspace member entity not found"));

        workspaceMemberEntity.changeRole(change.role());
    }

    @Override
    @Transactional
    public void remove(WorkspaceMemberRemoval removal) {
        Objects.requireNonNull(removal, "removal must not be null");
        WorkspaceMemberJpaEntity workspaceMemberEntity = workspaceMemberJpaRepository
                .findActiveWorkspaceMemberByWorkspaceIdAndMemberId(removal.workspaceId(), removal.memberId())
                .orElseThrow(() -> new IllegalArgumentException("workspace member entity not found"));

        workspaceMemberJpaRepository.delete(workspaceMemberEntity);
    }

    private ListWorkspacesResult toListResult(WorkspaceMemberJpaEntity workspaceMemberEntity) {
        WorkspaceJpaEntity workspaceEntity = workspaceMemberEntity.getWorkspace();
        return new ListWorkspacesResult(
                workspaceEntity.getId(),
                workspaceEntity.getName(),
                workspaceEntity.getDescription(),
                workspaceEntity.getCreatedByMember().getGoogleSub(),
                workspaceMemberEntity.getRole(),
                workspaceMemberEntity.getJoinedAt());
    }

    private GetWorkspaceDetailResult toDetailResult(WorkspaceMemberJpaEntity workspaceMemberEntity) {
        WorkspaceJpaEntity workspaceEntity = workspaceMemberEntity.getWorkspace();
        return new GetWorkspaceDetailResult(
                workspaceEntity.getId(),
                workspaceEntity.getName(),
                workspaceEntity.getDescription(),
                workspaceEntity.getCreatedByMember().getGoogleSub(),
                workspaceMemberEntity.getRole(),
                workspaceMemberEntity.getJoinedAt());
    }

    private ListWorkspaceMembersResult toMemberResult(WorkspaceMemberJpaEntity workspaceMemberEntity) {
        MemberJpaEntity memberEntity = workspaceMemberEntity.getMember();
        return new ListWorkspaceMembersResult(
                memberEntity.getId(),
                memberEntity.getGoogleSub(),
                memberEntity.getEmail(),
                memberEntity.getName(),
                workspaceMemberEntity.getRole(),
                workspaceMemberEntity.getJoinedAt());
    }
}
