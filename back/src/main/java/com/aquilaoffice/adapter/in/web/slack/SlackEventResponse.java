package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;

public record SlackEventResponse(
        boolean processed,
        String ignoredReason,
        Long taskId,
        Long chatSessionId,
        String sourceRef) {

    static SlackEventResponse from(ReceiveSlackEventResult result) {
        return new SlackEventResponse(
                result.processed(),
                result.ignoredReason(),
                result.taskId(),
                result.chatSessionId(),
                result.sourceRef());
    }
}
