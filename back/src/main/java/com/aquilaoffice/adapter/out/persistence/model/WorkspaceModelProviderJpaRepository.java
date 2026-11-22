package com.aquilaoffice.adapter.out.persistence.model;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 워크스페이스 AI 모델 공급자 Spring Data JPA 리포지토리다.
 */
public interface WorkspaceModelProviderJpaRepository extends JpaRepository<WorkspaceModelProviderJpaEntity, Long> {

    @Query("""
            select p
            from WorkspaceModelProviderJpaEntity p
            join fetch p.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and p.deletedAt is null
            order by p.priorityOrder asc, p.id asc
            """)
    List<WorkspaceModelProviderJpaEntity> findActiveByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            Pageable pageable);

    @Query("""
            select p
            from WorkspaceModelProviderJpaEntity p
            join fetch p.workspace w
            where w.id = :workspaceId
              and p.id = :providerId
              and w.deletedAt is null
              and p.deletedAt is null
            """)
    Optional<WorkspaceModelProviderJpaEntity> findActiveByWorkspaceIdAndId(
            @Param("workspaceId") Long workspaceId,
            @Param("providerId") Long providerId);
}
