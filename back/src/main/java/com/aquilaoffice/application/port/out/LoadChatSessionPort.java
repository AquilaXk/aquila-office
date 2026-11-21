package com.aquilaoffice.application.port.out;

import java.util.Optional;

public interface LoadChatSessionPort {

    Optional<SavedChatSession> findByWorkspaceIdAndChatSessionId(Long workspaceId, Long chatSessionId);
}
