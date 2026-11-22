package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.AcceptedWorkspaceInvite;
import com.aquilaoffice.application.port.out.ExtendWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.RecordWorkspaceInviteEmailFailurePort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceInvite;
import com.aquilaoffice.application.port.out.WorkspaceInviteAcceptance;
import com.aquilaoffice.application.port.out.WorkspaceInviteEmailFailure;
import com.aquilaoffice.application.port.out.WorkspaceInviteExtension;
import com.aquilaoffice.application.port.out.WorkspaceInviteLookup;
import com.aquilaoffice.application.port.out.WorkspaceInviteManagement;
import com.aquilaoffice.application.port.out.WorkspaceInviteRevocation;
import com.aquilaoffice.domain.workspace.WorkspaceInvite;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 워크스페이스 초대 outbound port를 JPA repository 조합으로 구현하는 persistence adapter다.
 */
@Repository
public class WorkspaceInvitePersistenceAdapter
        implements SaveWorkspaceInvitePort,
                LoadWorkspaceInvitePort,
                AcceptWorkspaceInvitePort,
                LoadWorkspaceInvitesPort,
                RevokeWorkspaceInvitePort,
                ExtendWorkspaceInvitePort,
                RecordWorkspaceInviteEmailFailurePort {

    private final MemberJpaRepository memberJpaRepository;
    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final WorkspaceInviteJpaRepository workspaceInviteJpaRepository;
    private final WorkspaceMemberJpaRepository workspaceMemberJpaRepository;

    public WorkspaceInvitePersistenceAdapter(
            MemberJpaRepository memberJpaRepository,
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceInviteJpaRepository workspaceInviteJpaRepository,
            WorkspaceMemberJpaRepository workspaceMemberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.workspaceInviteJpaRepository = workspaceInviteJpaRepository;
        this.workspaceMemberJpaRepository = workspaceMemberJpaRepository;
    }

    @Override
    @Transactional
    public SavedWorkspaceInvite save(Long workspaceId, WorkspaceInvite invite) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(invite, "invite must not be null");

        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        // 초대 저장은 기존 워크스페이스 FK에만 연결해야 하므로 id 조회 후 entity를 조합한다.
        WorkspaceInviteJpaEntity inviteEntity =
                workspaceInviteJpaRepository.save(WorkspaceInviteJpaEntity.from(invite, workspaceEntity));

        return new SavedWorkspaceInvite(inviteEntity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceInviteLookup> findByToken(String token) {
        Objects.requireNonNull(token, "token must not be null");

        return workspaceInviteJpaRepository.findByToken(token).map(this::toLookup);
    }

    private static final int MAX_INVITES_PAGE_SIZE = 200;

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceInviteManagement> findWorkspaceInvites(Long workspaceId, WorkspaceInviteStatus status) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");

        PageRequest pageRequest = PageRequest.of(0, MAX_INVITES_PAGE_SIZE);
        List<WorkspaceInviteJpaEntity> inviteEntities = status == null
                ? workspaceInviteJpaRepository.findAllByWorkspaceIdOrderByIdDesc(workspaceId, pageRequest)
                : workspaceInviteJpaRepository.findAllByWorkspaceIdAndStatusOrderByIdDesc(
                        workspaceId, status, pageRequest);
        return inviteEntities.stream()
                .map(this::toManagement)
                .toList();
    }

    @Override
    @Transactional
    public AcceptedWorkspaceInvite accept(WorkspaceInviteAcceptance acceptance) {
        Objects.requireNonNull(acceptance, "acceptance must not be null");
        Objects.requireNonNull(acceptance.inviteId(), "inviteId must not be null");
        Objects.requireNonNull(acceptance.workspaceId(), "workspaceId must not be null");
        Objects.requireNonNull(acceptance.memberGoogleSub(), "memberGoogleSub must not be null");
        Objects.requireNonNull(acceptance.role(), "role must not be null");
        Objects.requireNonNull(acceptance.acceptedAt(), "acceptedAt must not be null");

        WorkspaceInviteJpaEntity inviteEntity = workspaceInviteJpaRepository
                .findById(acceptance.inviteId())
                .orElseThrow(() -> new IllegalArgumentException("workspace invite entity not found"));
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findById(acceptance.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        MemberJpaEntity memberEntity = memberJpaRepository
                .findByGoogleSub(acceptance.memberGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member entity not found"));

        inviteEntity.accept(acceptance.acceptedAt());
        WorkspaceMemberJpaEntity memberJoinEntity = WorkspaceMemberJpaEntity.create(
                workspaceEntity, memberEntity, acceptance.role(), acceptance.acceptedAt());
        workspaceMemberJpaRepository.save(memberJoinEntity);

        return new AcceptedWorkspaceInvite(
                inviteEntity.getId(),
                workspaceEntity.getId(),
                memberEntity.getGoogleSub(),
                memberJoinEntity.getRole(),
                inviteEntity.getAcceptedAt());
    }

    @Override
    @Transactional
    public void revoke(WorkspaceInviteRevocation revocation) {
        Objects.requireNonNull(revocation, "revocation must not be null");
        WorkspaceInviteJpaEntity inviteEntity = workspaceInviteJpaRepository
                .findByIdAndWorkspaceId(revocation.inviteId(), revocation.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace invite entity not found"));

        inviteEntity.revoke(revocation.revokedAt());
    }

    @Override
    @Transactional
    public WorkspaceInviteManagement extend(WorkspaceInviteExtension extension) {
        Objects.requireNonNull(extension, "extension must not be null");
        WorkspaceInviteJpaEntity inviteEntity = workspaceInviteJpaRepository
                .findByIdAndWorkspaceId(extension.inviteId(), extension.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace invite entity not found"));

        inviteEntity.extendExpiresAt(extension.additionalDays(), extension.extendedAt());
        return toManagement(inviteEntity);
    }

    @Override
    @Transactional
    public void record(WorkspaceInviteEmailFailure failure) {
        Objects.requireNonNull(failure, "failure must not be null");
        WorkspaceInviteJpaEntity inviteEntity = workspaceInviteJpaRepository
                .findByIdAndWorkspaceId(failure.inviteId(), failure.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace invite entity not found"));

        inviteEntity.recordEmailFailure(failure.reason(), failure.failedAt());
    }

    private WorkspaceInviteLookup toLookup(WorkspaceInviteJpaEntity inviteEntity) {
        return new WorkspaceInviteLookup(
                inviteEntity.getId(),
                inviteEntity.getWorkspace().getId(),
                inviteEntity.getEmail(),
                inviteEntity.getToken(),
                inviteEntity.getStatus(),
                inviteEntity.getCreatedAt(),
                inviteEntity.getExpiresAt(),
                inviteEntity.getAcceptedAt());
    }

    private WorkspaceInviteManagement toManagement(WorkspaceInviteJpaEntity inviteEntity) {
        return new WorkspaceInviteManagement(
                inviteEntity.getId(),
                inviteEntity.getWorkspace().getId(),
                inviteEntity.getEmail(),
                inviteEntity.getToken(),
                inviteEntity.getStatus(),
                inviteEntity.getCreatedAt(),
                inviteEntity.getExpiresAt(),
                inviteEntity.getAcceptedAt(),
                inviteEntity.getRevokedAt());
    }
}
