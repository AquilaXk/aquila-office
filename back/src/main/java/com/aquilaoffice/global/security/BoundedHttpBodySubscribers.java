package com.aquilaoffice.global.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * 바이트 상한을 초과하는 HTTP 응답 본문을 즉시 중단(Fail-Closed)하는 BodyHandler/Subscriber다.
 */
public final class BoundedHttpBodySubscribers {

    public static final int DEFAULT_MAX_BYTES = 1024 * 1024;
    public static final int DISCOVERY_MAX_BYTES = 512 * 1024;
    public static final int CONNECTION_TEST_MAX_BYTES = 64 * 1024;
    public static final int MODEL_RUN_MAX_BYTES = 2 * 1024 * 1024;

    private BoundedHttpBodySubscribers() {}

    public static HttpResponse.BodyHandler<String> ofString(int maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        return responseInfo -> new BoundedStringSubscriber(maxBytes);
    }

    public static HttpResponse.BodyHandler<String> ofDefaultString() {
        return ofString(DEFAULT_MAX_BYTES);
    }

    public static final class BodySizeLimitExceededException extends IOException {

        public BodySizeLimitExceededException(int maxBytes) {
            super("HTTP response body exceeded max byte limit: " + maxBytes);
        }
    }

    private static final class BoundedStringSubscriber implements HttpResponse.BodySubscriber<String> {

        private final int maxBytes;
        private final CompletableFuture<String> result = new CompletableFuture<>();
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        private int totalBytes;
        private boolean failed;

        private BoundedStringSubscriber(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            if (failed) {
                return;
            }
            for (ByteBuffer item : items) {
                int remaining = item.remaining();
                if (totalBytes + remaining > maxBytes) {
                    failed = true;
                    if (subscription != null) {
                        subscription.cancel();
                    }
                    result.completeExceptionally(new BodySizeLimitExceededException(maxBytes));
                    return;
                }
                byte[] chunk = new byte[remaining];
                item.get(chunk);
                buffer.write(chunk, 0, chunk.length);
                totalBytes += remaining;
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!failed) {
                failed = true;
                result.completeExceptionally(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!failed) {
                result.complete(buffer.toString(StandardCharsets.UTF_8));
            }
        }

        @Override
        public CompletionStage<String> getBody() {
            return result;
        }
    }
}
