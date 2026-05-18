package com.aquilaoffice.adapter.out.persistence.orchestration;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestrationSessionJpaRepository extends JpaRepository<OrchestrationSessionJpaEntity, Long> {

    Optional<OrchestrationSessionJpaEntity> findByWorkspace_IdAndId(Long workspaceId, Long sessionId);
}
