package com.aquilaoffice.application.port.out;

import java.util.List;

public interface LoadChatMessagesPort {

    List<SavedChatMessage> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId);

    List<SavedChatMessage> findByWorkspaceIdAndChatSessionIdAfter(
            Long workspaceId,
            Long chatSessionId,
            Long afterMessageId,
            int limit);
}
