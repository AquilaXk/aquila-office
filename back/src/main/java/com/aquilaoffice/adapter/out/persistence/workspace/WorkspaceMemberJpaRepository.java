package com.aquilaoffice.adapter.out.persistence.workspace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 워크스페이스 멤버 조인 entity 저장과 조회를 담당하는 Spring Data repository다.
 */
public interface WorkspaceMemberJpaRepository extends JpaRepository<WorkspaceMemberJpaEntity, Long> {

    Optional<WorkspaceMemberJpaEntity> findByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);

    @Query("""
            select wm
            from WorkspaceMemberJpaEntity wm
            join fetch wm.workspace w
            join fetch w.createdByMember
            where wm.member.googleSub = :googleSub
              and w.deletedAt is null
            order by w.id desc
            """)
    List<WorkspaceMemberJpaEntity> findActiveJoinedWorkspacesByMemberGoogleSub(@Param("googleSub") String googleSub);

    @Query("""
            select wm
            from WorkspaceMemberJpaEntity wm
            join fetch wm.workspace w
            join fetch w.createdByMember
            where w.id = :workspaceId
              and wm.member.googleSub = :googleSub
              and w.deletedAt is null
            """)
    Optional<WorkspaceMemberJpaEntity> findActiveJoinedWorkspaceByIdAndMemberGoogleSub(
            @Param("workspaceId") Long workspaceId,
            @Param("googleSub") String googleSub);
}
