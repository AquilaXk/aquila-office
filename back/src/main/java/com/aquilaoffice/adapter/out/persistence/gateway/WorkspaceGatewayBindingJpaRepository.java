package com.aquilaoffice.adapter.out.persistence.gateway;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * workspace gateway binding JPA entity 저장과 active binding 조회를 담당하는 Spring Data repository다.
 */
public interface WorkspaceGatewayBindingJpaRepository extends JpaRepository<WorkspaceGatewayBindingJpaEntity, Long> {

    @Query("""
            select gb
            from WorkspaceGatewayBindingJpaEntity gb
            join fetch gb.workspace w
            where w.id = :workspaceId
              and gb.deletedAt is null
            order by gb.id desc
            """)
    List<WorkspaceGatewayBindingJpaEntity> findActiveByWorkspaceIdOrderByIdDesc(
            @Param("workspaceId") Long workspaceId);

    default Optional<WorkspaceGatewayBindingJpaEntity> findLatestActiveByWorkspaceId(Long workspaceId) {
        return findActiveByWorkspaceIdOrderByIdDesc(workspaceId).stream().findFirst();
    }
}
