package com.aquilaoffice.application.port.in;

import com.aquilaoffice.application.service.WorkspacePresenceEventSink;
import java.util.Objects;

/**
 * presence stream 종료 요청을 application 계층으로 전달하는 command다.
 */
public record CloseWorkspacePresenceStreamCommand(
        Long workspaceId,
        Long memberId,
        WorkspacePresenceEventSink sink) {

    public CloseWorkspacePresenceStreamCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        memberId = requirePositive(memberId, "memberId");
        sink = Objects.requireNonNull(sink, "sink must not be null");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
