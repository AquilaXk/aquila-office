package com.aquilaoffice.adapter.in.web.chat;

import com.aquilaoffice.application.port.in.ListChatSessionMessagesResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Chat session message page HTTP 응답 모델이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatSessionMessagesResponse(
        Long chatSessionId,
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasMore) {

    public static ChatSessionMessagesResponse from(ListChatSessionMessagesResult result) {
        return new ChatSessionMessagesResponse(
                result.chatSessionId(),
                result.messages().stream().map(ChatMessageResponse::from).toList(),
                result.nextCursor(),
                result.hasMore());
    }
}
