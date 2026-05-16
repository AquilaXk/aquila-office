package com.aquilaoffice.adapter.out.persistence.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, Long> {

    List<TaskJpaEntity> findByWorkspace_IdOrderByIdDesc(Long workspaceId);

    Optional<TaskJpaEntity> findByWorkspace_IdAndId(Long workspaceId, Long id);
}
