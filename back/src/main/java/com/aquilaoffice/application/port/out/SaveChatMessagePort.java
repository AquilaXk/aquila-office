package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.chat.ChatMessage;

public interface SaveChatMessagePort {

    SavedChatMessage save(ChatMessage message);
}
