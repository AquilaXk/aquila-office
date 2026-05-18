package com.aquilaoffice.adapter.out.persistence.orchestration;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestrationStepJpaRepository extends JpaRepository<OrchestrationStepJpaEntity, Long> {

    Optional<OrchestrationStepJpaEntity> findBySession_Workspace_IdAndSession_IdAndStepKey(
            Long workspaceId,
            Long sessionId,
            String stepKey);
}
