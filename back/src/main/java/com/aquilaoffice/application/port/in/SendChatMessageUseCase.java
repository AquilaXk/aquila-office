package com.aquilaoffice.application.port.in;

public interface SendChatMessageUseCase {

    SendChatMessageResult sendChatMessage(SendChatMessageCommand command);
}
