package com.aquilaoffice.adapter.out.persistence.orchestration;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestrationSessionJpaRepository extends JpaRepository<OrchestrationSessionJpaEntity, Long> {

    @EntityGraph(attributePaths = "steps")
    Optional<OrchestrationSessionJpaEntity> findByWorkspace_IdAndId(Long workspaceId, Long sessionId);

    List<OrchestrationSessionJpaEntity> findByWorkspace_IdOrderByIdDesc(Long workspaceId, Pageable pageable);
}
