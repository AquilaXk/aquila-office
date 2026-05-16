package com.aquilaoffice.application.port.in;

import java.util.List;

public interface ListChatTaskMessagesUseCase {

    List<ChatMessageResult> listChatTaskMessages(ListChatTaskMessagesQuery query);
}
