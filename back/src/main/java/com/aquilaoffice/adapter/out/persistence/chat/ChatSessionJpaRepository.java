package com.aquilaoffice.adapter.out.persistence.chat;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionJpaRepository extends JpaRepository<ChatSessionJpaEntity, Long> {

    Optional<ChatSessionJpaEntity> findByWorkspace_IdAndId(Long workspaceId, Long id);
}
