package com.aquilaoffice.adapter.out.persistence.github;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * GitHub 자격 증명 JPA entity 저장과 workspace 단위 active 조회를 담당하는 Spring Data repository다.
 */
public interface GithubCredentialJpaRepository extends JpaRepository<GithubCredentialJpaEntity, Long> {

    @Query("""
            select gc
            from GithubCredentialJpaEntity gc
            join fetch gc.workspace w
            where w.id = :workspaceId
              and gc.deletedAt is null
            order by gc.id desc
            """)
    List<GithubCredentialJpaEntity> findActiveByWorkspaceIdOrderByIdDesc(@Param("workspaceId") Long workspaceId);

    @Query("""
            select gc
            from GithubCredentialJpaEntity gc
            join fetch gc.workspace w
            where gc.id = :credentialId
              and w.id = :workspaceId
              and gc.deletedAt is null
            """)
    Optional<GithubCredentialJpaEntity> findActiveByIdAndWorkspaceId(
            @Param("credentialId") Long credentialId,
            @Param("workspaceId") Long workspaceId);

    @Query("""
            select count(gc) > 0
            from GithubCredentialJpaEntity gc
            join gc.workspace w
            where w.id = :workspaceId
              and gc.displayName = :displayName
              and gc.deletedAt is null
            """)
    boolean existsActiveDisplayName(
            @Param("workspaceId") Long workspaceId,
            @Param("displayName") String displayName);
}
