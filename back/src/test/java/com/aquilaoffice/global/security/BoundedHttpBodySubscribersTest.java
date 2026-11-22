package com.aquilaoffice.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class BoundedHttpBodySubscribersTest {

    private static final HttpResponse.ResponseInfo DUMMY_INFO = new HttpResponse.ResponseInfo() {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public java.net.http.HttpClient.Version version() {
            return java.net.http.HttpClient.Version.HTTP_1_1;
        }
    };

    @Test
    void acceptsPayloadsWithinLimitAndRejectsOversizedStreamAndCancelsSubscription() {
        HttpResponse.BodySubscriber<String> okSub = BoundedHttpBodySubscribers.ofString(10).apply(DUMMY_INFO);
        AtomicBoolean cancelledOk = new AtomicBoolean();
        okSub.onSubscribe(new TestSubscription(cancelledOk));
        okSub.onNext(List.of(ByteBuffer.wrap("0123456789".getBytes(StandardCharsets.UTF_8))));
        okSub.onComplete();
        assertThat(okSub.getBody().toCompletableFuture().join()).isEqualTo("0123456789");
        assertThat(cancelledOk.get()).isFalse();

        HttpResponse.BodySubscriber<String> overSub = BoundedHttpBodySubscribers.ofString(10).apply(DUMMY_INFO);
        AtomicBoolean cancelledOver = new AtomicBoolean();
        overSub.onSubscribe(new TestSubscription(cancelledOver));
        overSub.onNext(List.of(
                ByteBuffer.wrap("012345".getBytes(StandardCharsets.UTF_8)),
                ByteBuffer.wrap("67890".getBytes(StandardCharsets.UTF_8))));

        assertThat(cancelledOver.get()).isTrue();
        assertThatThrownBy(() -> overSub.getBody().toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BoundedHttpBodySubscribers.BodySizeLimitExceededException.class);
    }

    private static final class TestSubscription implements Flow.Subscription {

        private final AtomicBoolean cancelled;

        private TestSubscription(AtomicBoolean cancelled) {
            this.cancelled = cancelled;
        }

        @Override
        public void request(long n) {}

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }
}
