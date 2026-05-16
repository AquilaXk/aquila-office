package com.aquilaoffice.adapter.out.persistence.agent;

import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Agent JPA entity 저장과 active Agent 조회를 담당한다.
 */
public interface WorkspaceAgentJpaRepository extends JpaRepository<WorkspaceAgentJpaEntity, Long> {

    @Query("""
            select a
            from WorkspaceAgentJpaEntity a
            join fetch a.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and a.status <> com.aquilaoffice.domain.agent.WorkspaceAgentStatus.DISABLED
            order by a.id desc
            """)
    List<WorkspaceAgentJpaEntity> findActiveByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
            select a
            from WorkspaceAgentJpaEntity a
            join fetch a.workspace w
            where w.id = :workspaceId
              and a.id = :agentId
              and w.deletedAt is null
              and a.status <> com.aquilaoffice.domain.agent.WorkspaceAgentStatus.DISABLED
            """)
    Optional<WorkspaceAgentJpaEntity> findActiveByWorkspaceIdAndId(
            @Param("workspaceId") Long workspaceId,
            @Param("agentId") Long agentId);

    @Query("""
            select count(a)
            from WorkspaceAgentJpaEntity a
            join a.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and a.status <> :status
            """)
    long countByWorkspaceIdAndStatusNot(
            @Param("workspaceId") Long workspaceId,
            @Param("status") WorkspaceAgentStatus status);

    @Query("""
            select count(a)
            from WorkspaceAgentJpaEntity a
            join a.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and a.status = :status
            """)
    long countByWorkspaceIdAndStatus(
            @Param("workspaceId") Long workspaceId,
            @Param("status") WorkspaceAgentStatus status);

    @Query("""
            select count(a)
            from WorkspaceAgentJpaEntity a
            join a.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and a.status in :statuses
            """)
    long countByWorkspaceIdAndStatusIn(
            @Param("workspaceId") Long workspaceId,
            @Param("statuses") Collection<WorkspaceAgentStatus> statuses);
}
