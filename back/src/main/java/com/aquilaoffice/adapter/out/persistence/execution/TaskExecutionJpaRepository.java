package com.aquilaoffice.adapter.out.persistence.execution;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionJpaRepository extends JpaRepository<TaskExecutionJpaEntity, Long> {

    List<TaskExecutionJpaEntity> findByWorkspace_IdAndTask_IdOrderByCreatedAtDesc(Long workspaceId, Long taskId);
}
