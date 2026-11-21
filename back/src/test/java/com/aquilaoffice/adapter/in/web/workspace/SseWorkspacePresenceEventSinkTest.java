package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SseWorkspacePresenceEventSink가 send/complete 동기화와 완료 후 Fail-Closed 전송 거부를 보장하는지 검증한다.
 */
class SseWorkspacePresenceEventSinkTest {

    @Test
    void sendDeliversEventAndRejectsAfterIdempotentComplete() {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        SseWorkspacePresenceEventSink sink = new SseWorkspacePresenceEventSink(emitter);

        sink.send(WorkspacePresenceMessage.position(
                101L, 11L, new WorkspacePresencePosition("10%", "20%"), "IDLE"));
        assertThat(emitter.sendCalls.get()).isEqualTo(1);

        sink.complete();
        sink.complete();
        assertThat(emitter.completeCalls.get()).isEqualTo(1);

        assertThatThrownBy(() -> sink.send(WorkspacePresenceMessage.leave(101L, 11L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("presence event sink already completed");
    }

    @Test
    void sendWrapsIoFailureAsIllegalStateException() {
        SseEmitter failingEmitter = new SseEmitter() {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                throw new IOException("broken pipe");
            }
        };
        SseWorkspacePresenceEventSink sink = new SseWorkspacePresenceEventSink(failingEmitter);

        assertThatThrownBy(() -> sink.send(WorkspacePresenceMessage.leave(101L, 11L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("presence event send failed");
    }

    private static final class RecordingSseEmitter extends SseEmitter {

        private final AtomicInteger sendCalls = new AtomicInteger();
        private final AtomicInteger completeCalls = new AtomicInteger();

        @Override
        public void send(SseEventBuilder builder) {
            sendCalls.incrementAndGet();
        }

        @Override
        public synchronized void complete() {
            completeCalls.incrementAndGet();
        }
    }
}
