package com.aquilaoffice.adapter.out.persistence.task;

import com.aquilaoffice.domain.task.TaskStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, Long> {

    List<TaskJpaEntity> findByWorkspace_IdOrderByIdDesc(Long workspaceId);

    Optional<TaskJpaEntity> findByWorkspace_IdAndId(Long workspaceId, Long id);

    @Query("""
            select count(t)
            from TaskJpaEntity t
            join t.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
            """)
    long countByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
            select count(t)
            from TaskJpaEntity t
            join t.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and t.status = :status
            """)
    long countByWorkspaceIdAndStatus(
            @Param("workspaceId") Long workspaceId,
            @Param("status") TaskStatus status);

    @Query("""
            select count(t)
            from TaskJpaEntity t
            join t.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
              and t.status in :statuses
            """)
    long countByWorkspaceIdAndStatusIn(
            @Param("workspaceId") Long workspaceId,
            @Param("statuses") Collection<TaskStatus> statuses);
}
