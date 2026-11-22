package com.aquilaoffice.adapter.out.persistence.chat;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    List<ChatMessageJpaEntity> findByWorkspace_IdAndTaskIdOrderByIdDesc(
            Long workspaceId,
            Long taskId,
            Pageable pageable);

    @Query(
            """
            select m
            from ChatMessageJpaEntity m
            where m.workspace.id = :workspaceId
              and m.chatSession.id = :chatSessionId
              and (:afterMessageId is null or m.id > :afterMessageId)
            order by m.id asc
            """)
    List<ChatMessageJpaEntity> findByWorkspaceIdAndChatSessionIdAfter(
            @Param("workspaceId") Long workspaceId,
            @Param("chatSessionId") Long chatSessionId,
            @Param("afterMessageId") Long afterMessageId,
            Pageable pageable);
}
