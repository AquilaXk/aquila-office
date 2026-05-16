package com.aquilaoffice.adapter.out.persistence.execution;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskExecutionJpaRepository extends JpaRepository<TaskExecutionJpaEntity, Long> {

    List<TaskExecutionJpaEntity> findByWorkspace_IdAndTask_IdOrderByCreatedAtDesc(Long workspaceId, Long taskId);

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
