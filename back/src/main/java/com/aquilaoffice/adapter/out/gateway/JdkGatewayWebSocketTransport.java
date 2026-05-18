package com.aquilaoffice.adapter.out.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
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

    private final HttpClient httpClient;

    public JdkGatewayWebSocketTransport() {
        this(HttpClient.newHttpClient());
    }

    JdkGatewayWebSocketTransport(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public GatewayWebSocketSession connect(URI uri, GatewayWebSocketListener listener) {
        try {
            WebSocket webSocket = httpClient
                    .newWebSocketBuilder()
                    .buildAsync(uri, new JdkListener(listener))
                    .join();
            return new JdkGatewayWebSocketSession(webSocket);
        } catch (CompletionException exception) {
            throw new IllegalStateException("gateway websocket connection failed", exception.getCause());
        }
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

        private JdkListener(GatewayWebSocketListener listener) {
            this.listener = Objects.requireNonNull(listener, "listener must not be null");
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                listener.onText(textBuffer.toString());
                textBuffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            listener.onError(error);
        }
    }
}
