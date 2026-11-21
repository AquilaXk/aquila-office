package com.aquilaoffice.adapter.out.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import org.springframework.stereotype.Component;

/**
 * JDK WebSocket client를 Gateway transport 계약에 맞게 감싼다.
 */
@Component
public final class JdkGatewayWebSocketTransport implements GatewayWebSocketTransport {

    static final int MAX_FRAME_CHARS = 2 * 1024 * 1024;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public JdkGatewayWebSocketTransport() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
    }

    JdkGatewayWebSocketTransport(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public GatewayWebSocketSession connect(URI uri, GatewayWebSocketListener listener) {
        try {
            WebSocket webSocket = httpClient
                    .newWebSocketBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .buildAsync(uri, new JdkListener(listener))
                    .join();
            return new JdkGatewayWebSocketSession(webSocket);
        } catch (CompletionException exception) {
            throw new IllegalStateException("gateway websocket connection failed", exception.getCause());
        }
    }

    static WebSocket.Listener createListener(GatewayWebSocketListener listener) {
        return new JdkListener(listener);
    }

    private static final class JdkGatewayWebSocketSession implements GatewayWebSocketSession {

        private final WebSocket webSocket;

        private JdkGatewayWebSocketSession(WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public CompletableFuture<Void> sendText(String message) {
            return webSocket.sendText(message, true).thenApply(ignored -> null);
        }

        @Override
        public CompletableFuture<Void> close() {
            return webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").thenApply(ignored -> null);
        }
    }

    private static final class JdkListener implements WebSocket.Listener {

        private final GatewayWebSocketListener listener;
        private final StringBuilder textBuffer = new StringBuilder();
        private boolean aborted;

        private JdkListener(GatewayWebSocketListener listener) {
            this.listener = Objects.requireNonNull(listener, "listener must not be null");
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (aborted) {
                return CompletableFuture.completedFuture(null);
            }
            if (data != null) {
                if (data.length() > MAX_FRAME_CHARS - textBuffer.length()) {
                    aborted = true;
                    textBuffer.setLength(0);
                    webSocket.abort();
                    listener.onError(new IllegalStateException(
                            "gateway websocket frame exceeds maximum length: " + MAX_FRAME_CHARS));
                    return CompletableFuture.completedFuture(null);
                }
                textBuffer.append(data);
            }
            if (last) {
                listener.onText(textBuffer.toString());
                textBuffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            textBuffer.setLength(0);
            if (!aborted) {
                listener.onError(error);
            }
        }
    }
}
