package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceInvite;
import com.aquilaoffice.application.port.out.WorkspaceInviteLookup;
import com.aquilaoffice.domain.workspace.WorkspaceInvite;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 워크스페이스 초대 outbound port를 JPA repository 조합으로 구현하는 persistence adapter다.
 */
@Repository
public class WorkspaceInvitePersistenceAdapter implements SaveWorkspaceInvitePort, LoadWorkspaceInvitePort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final WorkspaceInviteJpaRepository workspaceInviteJpaRepository;

    public WorkspaceInvitePersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceInviteJpaRepository workspaceInviteJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.workspaceInviteJpaRepository = workspaceInviteJpaRepository;
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
}
