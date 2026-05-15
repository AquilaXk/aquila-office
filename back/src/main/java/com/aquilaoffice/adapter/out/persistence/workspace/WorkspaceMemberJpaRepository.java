package com.aquilaoffice.adapter.out.persistence.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberJpaRepository extends JpaRepository<WorkspaceMemberJpaEntity, Long> {

    Optional<WorkspaceMemberJpaEntity> findByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);
}
