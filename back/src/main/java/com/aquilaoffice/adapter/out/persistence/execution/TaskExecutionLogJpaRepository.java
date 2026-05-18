package com.aquilaoffice.adapter.out.persistence.execution;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskExecutionLogJpaRepository extends JpaRepository<TaskExecutionLogJpaEntity, Long> {

    @Query("""
            select l
            from TaskExecutionLogJpaEntity l
            join fetch l.execution e
            join fetch e.task t
            join e.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
            order by l.createdAt desc, l.id desc
            """)
    List<TaskExecutionLogJpaEntity> findRecentByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            Pageable pageable);
}
