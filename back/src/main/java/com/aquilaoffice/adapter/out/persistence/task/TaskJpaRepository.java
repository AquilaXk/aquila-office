package com.aquilaoffice.adapter.out.persistence.task;

import com.aquilaoffice.application.port.out.WorkspaceMemberTaskStatusCount;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.task.TaskStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, Long> {

    List<TaskJpaEntity> findByWorkspace_IdOrderByIdDesc(Long workspaceId, Pageable pageable);

    Optional<TaskJpaEntity> findByWorkspace_IdAndId(Long workspaceId, Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TaskJpaEntity t
            set t.status = :failedStatus,
                t.updatedAt = :updatedAt
            where t.workspace.id = :workspaceId
              and t.status = :inProgressStatus
              and t.updatedAt < :cutoff
            """)
    int recoverStaleTasks(
            @Param("workspaceId") Long workspaceId,
            @Param("inProgressStatus") TaskStatus inProgressStatus,
            @Param("failedStatus") TaskStatus failedStatus,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Query("""
            select new com.aquilaoffice.adapter.out.persistence.task.TaskDashboardCounts(
                    count(t),
                    coalesce(sum(case when t.status in :runningStatuses then 1L else 0L end), 0L),
                    coalesce(sum(case when t.status = :waitingUserStatus then 1L else 0L end), 0L),
                    coalesce(sum(case when t.status = :completedStatus then 1L else 0L end), 0L),
                    coalesce(sum(case when t.status = :failedStatus then 1L else 0L end), 0L))
            from TaskJpaEntity t
            join t.workspace w
            where w.id = :workspaceId
              and w.deletedAt is null
            """)
    TaskDashboardCounts countDashboardMetricsByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("runningStatuses") Collection<TaskStatus> runningStatuses,
            @Param("waitingUserStatus") TaskStatus waitingUserStatus,
            @Param("completedStatus") TaskStatus completedStatus,
            @Param("failedStatus") TaskStatus failedStatus);

    @Query("""
            select new com.aquilaoffice.application.port.out.WorkspaceMemberTaskStatusCount(
                    a.createdByGoogleSub,
                    t.status,
                    count(t))
            from TaskJpaEntity t
            join t.workspace w
            join WorkspaceAgentJpaEntity a on a.id = t.assignedAgentId
            where w.id = :workspaceId
              and w.deletedAt is null
              and a.workspace.id = :workspaceId
              and a.status <> :disabledStatus
              and t.assignedAgentId is not null
            group by a.createdByGoogleSub, t.status
            """)
    List<WorkspaceMemberTaskStatusCount> countGroupedByAgentOwnerAndStatus(
            @Param("workspaceId") Long workspaceId,
            @Param("disabledStatus") WorkspaceAgentStatus disabledStatus);
}
