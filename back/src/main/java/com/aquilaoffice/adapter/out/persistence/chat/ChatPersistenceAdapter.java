package com.aquilaoffice.adapter.out.persistence.chat;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadChatMessagesPort;
import com.aquilaoffice.application.port.out.LoadChatSessionPort;
import com.aquilaoffice.application.port.out.SaveChatMessagePort;
import com.aquilaoffice.application.port.out.SaveChatSessionPort;
import com.aquilaoffice.application.port.out.SavedChatMessage;
import com.aquilaoffice.application.port.out.SavedChatSession;
import com.aquilaoffice.domain.chat.ChatMessage;
import com.aquilaoffice.domain.chat.ChatSession;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat session/message outbound port를 JPA 저장소로 구현한다.
 */
@Repository
public class ChatPersistenceAdapter
        implements SaveChatSessionPort, LoadChatSessionPort, SaveChatMessagePort, LoadChatMessagesPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final ChatSessionJpaRepository chatSessionJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    public ChatPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            ChatSessionJpaRepository chatSessionJpaRepository,
            ChatMessageJpaRepository chatMessageJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.chatSessionJpaRepository = chatSessionJpaRepository;
        this.chatMessageJpaRepository = chatMessageJpaRepository;
    }

    @Override
    @Transactional
    public SavedChatSession save(ChatSession session) {
        Objects.requireNonNull(session, "session must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(session.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        return toSavedChatSession(chatSessionJpaRepository.save(ChatSessionJpaEntity.from(session, workspaceEntity)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SavedChatSession> findByWorkspaceIdAndChatSessionId(Long workspaceId, Long chatSessionId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(chatSessionId, "chatSessionId must not be null");
        return chatSessionJpaRepository.findByWorkspace_IdAndId(workspaceId, chatSessionId)
                .map(this::toSavedChatSession);
    }

    @Override
    @Transactional
    public SavedChatMessage save(ChatMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(message.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        ChatSessionJpaEntity sessionEntity = chatSessionJpaRepository
                .findByWorkspace_IdAndId(message.getWorkspaceId(), message.getChatSessionId())
                .orElseThrow(() -> new IllegalArgumentException("chat session entity not found"));
        return toSavedChatMessage(chatMessageJpaRepository.save(
                ChatMessageJpaEntity.from(message, workspaceEntity, sessionEntity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedChatMessage> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        return chatMessageJpaRepository.findByWorkspace_IdAndTaskIdOrderByIdAsc(workspaceId, taskId).stream()
                .map(this::toSavedChatMessage)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedChatMessage> findByWorkspaceIdAndChatSessionIdAfter(
            Long workspaceId,
            Long chatSessionId,
            Long afterMessageId,
            int limit) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(chatSessionId, "chatSessionId must not be null");
        int pageSize = Math.max(limit, 1);
        return chatMessageJpaRepository
                .findByWorkspaceIdAndChatSessionIdAfter(
                        workspaceId,
                        chatSessionId,
                        afterMessageId,
                        PageRequest.of(0, pageSize))
                .stream()
                .map(this::toSavedChatMessage)
                .toList();
    }

    private SavedChatSession toSavedChatSession(ChatSessionJpaEntity session) {
        return new SavedChatSession(
                session.getId(),
                session.getWorkspace().getId(),
                session.getCreatedByGoogleSub(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    private SavedChatMessage toSavedChatMessage(ChatMessageJpaEntity message) {
        return new SavedChatMessage(
                message.getId(),
                message.getWorkspace().getId(),
                message.getChatSession().getId(),
                message.getTaskId(),
                message.getTaskExecutionId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt());
    }
}
