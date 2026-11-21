package com.aquilaoffice.adapter.out.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * JdkGatewayWebSocketTransport의 프레임 크기 상한(Fail-Closed)과 타임아웃 상수를 검증한다.
 */
class JdkGatewayWebSocketTransportTest {

    @Test
    void connectTimeoutIsTenSecondsAndFragmentedValidFrameDeliversMessage() {
        assertThat(JdkGatewayWebSocketTransport.CONNECT_TIMEOUT).isEqualTo(Duration.ofSeconds(10));

        List<String> received = new ArrayList<>();
        AtomicReference<Throwable> recordedError = new AtomicReference<>();
        RecordingWebSocket fakeSocket = new RecordingWebSocket();
        WebSocket.Listener jdkListener = JdkGatewayWebSocketTransport.createListener(new GatewayWebSocketListener() {
            @Override
            public void onText(String message) {
                received.add(message);
            }

            @Override
            public void onError(Throwable error) {
                recordedError.set(error);
            }
        });

        jdkListener.onOpen(fakeSocket);
        jdkListener.onText(fakeSocket, "{\"id\":\"1\",", false);
        jdkListener.onText(fakeSocket, "\"ok\":true}", true);

        assertThat(received).containsExactly("{\"id\":\"1\",\"ok\":true}");
        assertThat(recordedError.get()).isNull();
        assertThat(fakeSocket.aborted.get()).isFalse();
        assertThat(fakeSocket.requests.get()).isEqualTo(3);
    }

    @Test
    void oversizedFrameTriggersFailClosedAbortAndOnError() {
        List<String> received = new ArrayList<>();
        AtomicReference<Throwable> recordedError = new AtomicReference<>();
        RecordingWebSocket fakeSocket = new RecordingWebSocket();
        WebSocket.Listener jdkListener = JdkGatewayWebSocketTransport.createListener(new GatewayWebSocketListener() {
            @Override
            public void onText(String message) {
                received.add(message);
            }

            @Override
            public void onError(Throwable error) {
                recordedError.set(error);
            }
        });

        String largeChunk = "x".repeat(JdkGatewayWebSocketTransport.MAX_FRAME_CHARS);
        jdkListener.onText(fakeSocket, largeChunk, false);
        jdkListener.onText(fakeSocket, "y", true);
        // 중단 이후 후속 프레임과 에러는 무시된다.
        jdkListener.onText(fakeSocket, "ignored", true);

        assertThat(received).isEmpty();
        assertThat(fakeSocket.aborted.get()).isTrue();
        assertThat(recordedError.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds maximum length");
    }

    private static final class RecordingWebSocket implements WebSocket {

        private final AtomicBoolean aborted = new AtomicBoolean();
        private final AtomicInteger requests = new AtomicInteger();

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
            requests.addAndGet((int) n);
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return aborted.get();
        }

        @Override
        public boolean isInputClosed() {
            return aborted.get();
        }

        @Override
        public void abort() {
            aborted.set(true);
        }
    }
}
