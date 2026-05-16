package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.service.WorkspacePresenceEventSink;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * application presence event를 Spring SSE 응답으로 변환하는 web adapter다.
 */
public class SseWorkspacePresenceEventSink implements WorkspacePresenceEventSink {

    private final SseEmitter emitter;

    public SseWorkspacePresenceEventSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void send(WorkspacePresenceMessage message) {
        try {
            emitter.send(SseEmitter.event()
                    .reconnectTime(3_000L)
                    .data(message, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException exception) {
            throw new IllegalStateException("presence event send failed", exception);
        }
    }
}
