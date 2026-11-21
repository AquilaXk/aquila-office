package com.aquilaoffice.adapter.out.persistence.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 워크스페이스 JPA entity 저장과 조회를 담당하는 Spring Data repository다.
 */
public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, Long> {

    @Query("""
            select w
            from WorkspaceJpaEntity w
            join fetch w.createdByMember
            where w.id = :workspaceId
              and w.deletedAt is null
            """)
    Optional<WorkspaceJpaEntity> findActiveWorkspaceById(@Param("workspaceId") Long workspaceId);

    @Query("""
            select w.dailyTokenLimit
            from WorkspaceJpaEntity w
            where w.id = :workspaceId
              and w.deletedAt is null
            """)
    Optional<Integer> findActiveDailyTokenLimitById(@Param("workspaceId") Long workspaceId);
}
