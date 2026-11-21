package com.aquilaoffice.adapter.out.persistence.execution;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskExecutionJpaRepository extends JpaRepository<TaskExecutionJpaEntity, Long> {

    List<TaskExecutionJpaEntity> findByWorkspace_IdAndTask_IdOrderByCreatedAtDesc(Long workspaceId, Long taskId);

    List<TaskExecutionJpaEntity> findByWorkspace_IdAndTask_IdAndStatusInOrderByCreatedAtDesc(
            Long workspaceId,
            Long taskId,
            Collection<TaskExecutionStatus> statuses);

    @Query("""
            select e
            from TaskExecutionJpaEntity e
            join fetch e.task t
            join e.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and e.status in :statuses
              and e.updatedAt < :cutoff
            order by e.id asc
            """)
    List<TaskExecutionJpaEntity> findStaleByWorkspaceIdAndStatusInBefore(
            @Param("workspaceId") Long workspaceId,
            @Param("statuses") Collection<TaskExecutionStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TaskExecutionJpaEntity e
            set e.status = :nextStatus,
                e.failureReason = :failureReason,
                e.updatedAt = :updatedAt
            where e.id in :ids
            """)
    int markStaleFailedByIds(
            @Param("ids") Collection<Long> ids,
            @Param("nextStatus") TaskExecutionStatus nextStatus,
            @Param("failureReason") String failureReason,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Query("""
            select coalesce(sum(e.promptTokens + e.completionTokens), 0L)
            from TaskExecutionJpaEntity e
            join e.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and e.createdAt >= :todayStart
            """)
    long sumTokensByWorkspaceIdSince(
            @Param("workspaceId") Long workspaceId,
            @Param("todayStart") LocalDateTime todayStart);

    @Query("""
            select avg(e.durationMs)
            from TaskExecutionJpaEntity e
            join e.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and e.durationMs is not null
            """)
    Double findAverageDurationMsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
            select count(distinct e.agentId)
            from TaskExecutionJpaEntity e
            join e.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and e.status = :status
            """)
    long countDistinctAgentIdByWorkspaceIdAndStatus(
            @Param("workspaceId") Long workspaceId,
            @Param("status") TaskExecutionStatus status);

    @Query("""
            select e
            from TaskExecutionJpaEntity e
            join fetch e.task t
            join e.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
            order by e.createdAt desc, e.id desc
            """)
    List<TaskExecutionJpaEntity> findRecentByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            Pageable pageable);
}
