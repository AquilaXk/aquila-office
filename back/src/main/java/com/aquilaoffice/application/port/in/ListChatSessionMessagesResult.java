package com.aquilaoffice.application.port.in;

import java.util.List;

public record ListChatSessionMessagesResult(
        Long chatSessionId,
        List<ChatMessageResult> messages,
        Long nextCursor,
        boolean hasMore) {

    public ListChatSessionMessagesResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
