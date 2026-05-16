package com.aquilaoffice.adapter.out.persistence.agent;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Agent skill 파일 JPA entity 조회를 담당한다.
 */
public interface WorkspaceAgentSkillFileJpaRepository extends JpaRepository<WorkspaceAgentSkillFileJpaEntity, Long> {

    List<WorkspaceAgentSkillFileJpaEntity> findByAgentIdOrderByIdAsc(Long agentId);

    List<WorkspaceAgentSkillFileJpaEntity> findByAgentIdInOrderByAgentIdAscIdAsc(Collection<Long> agentIds);
}
