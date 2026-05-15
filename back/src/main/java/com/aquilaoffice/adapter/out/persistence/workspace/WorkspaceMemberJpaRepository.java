package com.aquilaoffice.adapter.out.persistence.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 워크스페이스 멤버 조인 entity 저장과 조회를 담당하는 Spring Data repository다.
 */
public interface WorkspaceMemberJpaRepository extends JpaRepository<WorkspaceMemberJpaEntity, Long> {

    Optional<WorkspaceMemberJpaEntity> findByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);
}
