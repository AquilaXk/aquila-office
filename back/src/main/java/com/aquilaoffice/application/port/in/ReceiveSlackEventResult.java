package com.aquilaoffice.application.port.in;

public record ReceiveSlackEventResult(
        String challenge,
        boolean processed,
        String ignoredReason,
        Long taskId,
        Long chatSessionId,
        String sourceRef) {}
