package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.global.security.OutboundDestinationPolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocketListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 WebSocket 클라이언트를 DNS-pinned OkHttp 기반으로 감싸 Gateway transport 계약을 구현한다.
 */
@Component
public final class JdkGatewayWebSocketTransport implements GatewayWebSocketTransport {

    static final int MAX_FRAME_CHARS = 2 * 1024 * 1024;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final OkHttpClient baseHttpClient;
    private final OutboundDestinationPolicy outboundDestinationPolicy;

    public JdkGatewayWebSocketTransport() {
        this(defaultOkHttpClient(), OutboundDestinationPolicy.strict());
    }

    @Autowired
    public JdkGatewayWebSocketTransport(OutboundDestinationPolicy outboundDestinationPolicy) {
        this(defaultOkHttpClient(), outboundDestinationPolicy);
    }

    JdkGatewayWebSocketTransport(HttpClient ignoredLegacyClient) {
        this(defaultOkHttpClient(), OutboundDestinationPolicy.strict());
    }

    JdkGatewayWebSocketTransport(HttpClient ignoredLegacyClient, OutboundDestinationPolicy outboundDestinationPolicy) {
        this(defaultOkHttpClient(), outboundDestinationPolicy);
    }

    JdkGatewayWebSocketTransport(OkHttpClient baseHttpClient, OutboundDestinationPolicy outboundDestinationPolicy) {
        this.baseHttpClient = Objects.requireNonNull(baseHttpClient, "baseHttpClient must not be null");
        this.outboundDestinationPolicy =
                Objects.requireNonNull(outboundDestinationPolicy, "outboundDestinationPolicy must not be null");
    }

    private static OkHttpClient defaultOkHttpClient() {
        return new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public GatewayWebSocketSession connect(URI uri, GatewayWebSocketListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        OutboundDestinationPolicy.ValidatedDestination destination =
                outboundDestinationPolicy.validateAndResolveUri(uri, false);
        String targetUrl = toWebSocketHandshakeHttpUrl(destination.uri());
        OkHttpClient pinnedClient = baseHttpClient.newBuilder()
                .dns(requestedHost -> OutboundDestinationPolicy.lookupPinnedHost(destination, requestedHost))
                .build();
        CompletableFuture<GatewayWebSocketSession> connected = new CompletableFuture<>();
        Request request = new Request.Builder().url(targetUrl).build();
        pinnedClient.newWebSocket(request, createOkHttpListener(listener, connected));
        try {
            return connected.orTimeout(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (CompletionException exception) {
            throw new IllegalStateException("gateway websocket connection failed", exception.getCause());
        }
    }

    private static String toWebSocketHandshakeHttpUrl(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
            throw new IllegalArgumentException("gateway websocket scheme must be ws or wss");
        }
        String httpScheme = "wss".equals(scheme) ? "https" : "http";
        return httpScheme + uri.toString().substring(scheme.length());
    }

    static WebSocket.Listener createListener(GatewayWebSocketListener listener) {
        return new JdkListener(listener);
    }

    static WebSocketListener createOkHttpListener(
            GatewayWebSocketListener listener,
            CompletableFuture<GatewayWebSocketSession> connected) {
        return new OkHttpGatewayListener(listener, connected);
    }

    private static final class OkHttpGatewayWebSocketSession implements GatewayWebSocketSession {

        private final okhttp3.WebSocket webSocket;

        private OkHttpGatewayWebSocketSession(okhttp3.WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public CompletableFuture<Void> sendText(String message) {
            if (!webSocket.send(message)) {
                CompletableFuture<Void> failed = new CompletableFuture<>();
                failed.completeExceptionally(new IllegalStateException("gateway websocket send rejected"));
                return failed;
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> close() {
            webSocket.close(1000, "done");
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class OkHttpGatewayListener extends WebSocketListener {

        private final GatewayWebSocketListener listener;
        private final CompletableFuture<GatewayWebSocketSession> connected;
        private final AtomicBoolean aborted = new AtomicBoolean(false);

        private OkHttpGatewayListener(
                GatewayWebSocketListener listener,
                CompletableFuture<GatewayWebSocketSession> connected) {
            this.listener = Objects.requireNonNull(listener, "listener must not be null");
            this.connected = Objects.requireNonNull(connected, "connected must not be null");
        }

        @Override
        public void onOpen(okhttp3.WebSocket webSocket, Response response) {
            connected.complete(new OkHttpGatewayWebSocketSession(webSocket));
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, String text) {
            if (aborted.get()) {
                return;
            }
            if (text != null && text.length() > MAX_FRAME_CHARS) {
                aborted.set(true);
                webSocket.cancel();
                listener.onError(new IllegalStateException(
                        "gateway websocket frame exceeds maximum length: " + MAX_FRAME_CHARS));
                return;
            }
            if (text != null) {
                listener.onText(text);
            }
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, okio.ByteString bytes) {
            if (aborted.get()) {
                return;
            }
            if (bytes != null && bytes.size() > MAX_FRAME_CHARS) {
                aborted.set(true);
                webSocket.cancel();
                listener.onError(new IllegalStateException(
                        "gateway websocket frame exceeds maximum length: " + MAX_FRAME_CHARS));
            }
        }

        @Override
        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
            if (!connected.isDone()) {
                connected.completeExceptionally(new IllegalStateException("gateway websocket closed before open"));
            }
            webSocket.close(code, reason);
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable throwable, Response response) {
            if (!connected.isDone()) {
                connected.completeExceptionally(throwable);
                return;
            }
            if (!aborted.get()) {
                listener.onError(throwable);
            }
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
