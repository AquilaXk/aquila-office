package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.chat.ChatSession;

public interface SaveChatSessionPort {

    SavedChatSession save(ChatSession session);
}
