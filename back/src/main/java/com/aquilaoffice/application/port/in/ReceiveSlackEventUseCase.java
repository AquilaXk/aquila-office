package com.aquilaoffice.application.port.in;

public interface ReceiveSlackEventUseCase {

    ReceiveSlackEventResult receiveSlackEvent(ReceiveSlackEventCommand command);
}
