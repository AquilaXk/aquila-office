package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 워크스페이스 초대 JPA entity 저장과 token 조회를 담당하는 Spring Data repository다.
 */
public interface WorkspaceInviteJpaRepository extends JpaRepository<WorkspaceInviteJpaEntity, Long> {

    Optional<WorkspaceInviteJpaEntity> findByToken(String token);

    @Query("""
            select wi
            from WorkspaceInviteJpaEntity wi
            join fetch wi.workspace w
            where w.id = :workspaceId
            order by wi.id desc
            """)
    List<WorkspaceInviteJpaEntity> findAllByWorkspaceIdOrderByIdDesc(
            @Param("workspaceId") Long workspaceId,
            Pageable pageable);

    @Query("""
            select wi
            from WorkspaceInviteJpaEntity wi
            join fetch wi.workspace w
            where w.id = :workspaceId
              and wi.status = :status
            order by wi.id desc
            """)
    List<WorkspaceInviteJpaEntity> findAllByWorkspaceIdAndStatusOrderByIdDesc(
            @Param("workspaceId") Long workspaceId,
            @Param("status") WorkspaceInviteStatus status,
            Pageable pageable);

    @Query("""
            select wi
            from WorkspaceInviteJpaEntity wi
            join fetch wi.workspace w
            where wi.id = :inviteId
              and w.id = :workspaceId
            """)
    Optional<WorkspaceInviteJpaEntity> findByIdAndWorkspaceId(
            @Param("inviteId") Long inviteId,
            @Param("workspaceId") Long workspaceId);
}
