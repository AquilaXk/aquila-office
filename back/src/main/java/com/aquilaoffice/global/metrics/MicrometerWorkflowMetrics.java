package com.aquilaoffice.global.metrics;

import com.aquilaoffice.application.service.WorkflowMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Prometheus에서 조회할 수 있도록 핵심 업무 흐름 metric을 Micrometer에 기록한다.
 */
@Component
public class MicrometerWorkflowMetrics implements WorkflowMetrics {

    private static final Set<String> GATEWAY_METHOD_LABELS = Set.of(
            "agents.create",
            "agents.delete",
            "agents.files.set",
            "agents.list",
            "chat.send",
            "connect");
    private static final Set<String> SLACK_ROUTE_LABELS = Set.of("global", "workspace");
    private static final Set<String> SLACK_STAGE_LABELS = Set.of("payload_parse", "signature_config", "use_case");
    static final int MAX_CACHED_METERS = 256;

    private final MeterRegistry meterRegistry;
    // 핫 경로에서 Counter.builder / Timer.builder 반복 할당을 피하기 위해 라벨 키별로 사전 등록된 Meter를 캐싱한다.
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final Object cacheLimitLock = new Object();

    public MicrometerWorkflowMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public OperationSample startTaskExecution() {
        counter(
                "aquila.task.execution.started",
                () -> Counter.builder("aquila.task.execution.started")
                        .description("Task execution start count"))
                .increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        return new SingleRecordOperationSample(outcome -> {
            String status = "success".equals(outcome) ? "succeeded" : "failed";
            counter(
                    "aquila.task.execution.completed:" + status,
                    () -> Counter.builder("aquila.task.execution.completed")
                            .description("Task execution completion count")
                            .tag("status", status))
                    .increment();
            sample.stop(timer(
                    "aquila.task.execution.duration:" + status,
                    () -> Timer.builder("aquila.task.execution.duration")
                            .description("Task execution duration")
                            .tag("status", status)));
        });
    }

    @Override
    public OperationSample startGatewayRpc(String method) {
        String methodLabel = normalizeAllowedLabel(method, GATEWAY_METHOD_LABELS);
        counter(
                "aquila.gateway.rpc.requests:" + methodLabel,
                () -> Counter.builder("aquila.gateway.rpc.requests")
                        .description("Gateway RPC request count")
                        .tag("method", methodLabel))
                .increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        return new SingleRecordOperationSample(outcome -> {
            String cacheSuffix = methodLabel + ":" + outcome;
            counter(
                    "aquila.gateway.rpc.completed:" + cacheSuffix,
                    () -> Counter.builder("aquila.gateway.rpc.completed")
                            .description("Gateway RPC completion count")
                            .tag("method", methodLabel)
                            .tag("outcome", outcome))
                    .increment();
            sample.stop(timer(
                    "aquila.gateway.rpc.latency:" + cacheSuffix,
                    () -> Timer.builder("aquila.gateway.rpc.latency")
                            .description("Gateway RPC latency")
                            .tag("method", methodLabel)
                            .tag("outcome", outcome)));
        });
    }

    @Override
    public void recordSlackEventReceived(String route) {
        String routeLabel = normalizeAllowedLabel(route, SLACK_ROUTE_LABELS);
        counter(
                "aquila.slack.events.received:" + routeLabel,
                () -> Counter.builder("aquila.slack.events.received")
                        .description("Slack event receive count")
                        .tag("route", routeLabel))
                .increment();
    }

    @Override
    public void recordSlackSignatureFailure(String route) {
        String routeLabel = normalizeAllowedLabel(route, SLACK_ROUTE_LABELS);
        counter(
                "aquila.slack.events.signature_failures:" + routeLabel,
                () -> Counter.builder("aquila.slack.events.signature_failures")
                        .description("Slack signature verification failure count")
                        .tag("route", routeLabel))
                .increment();
    }

    @Override
    public void recordSlackDedupeHit() {
        counter(
                "aquila.slack.events.dedupe_hits",
                () -> Counter.builder("aquila.slack.events.dedupe_hits")
                        .description("Slack event deduplication hit count"))
                .increment();
    }

    @Override
    public void recordSlackProcessingFailure(String stage) {
        String stageLabel = normalizeAllowedLabel(stage, SLACK_STAGE_LABELS);
        counter(
                "aquila.slack.events.processing_failures:" + stageLabel,
                () -> Counter.builder("aquila.slack.events.processing_failures")
                        .description("Slack event processing failure count")
                        .tag("stage", stageLabel))
                .increment();
    }

    private static String normalizeAllowedLabel(String rawValue, Set<String> allowedLabels) {
        if (rawValue == null || rawValue.isBlank()) {
            return "unknown";
        }
        String normalizedValue = rawValue.trim();
        if (allowedLabels.contains(normalizedValue)) {
            return normalizedValue;
        }
        return "unknown";
    }

    Counter counter(String cacheKey, Supplier<Counter.Builder> builderSupplier) {
        Counter cached = counters.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Counter registered = builderSupplier.get().register(meterRegistry);
        synchronized (cacheLimitLock) {
            Counter existing = counters.get(cacheKey);
            if (existing != null) {
                return existing;
            }
            if (counters.size() + timers.size() < MAX_CACHED_METERS) {
                counters.put(cacheKey, registered);
            }
            return registered;
        }
    }

    Timer timer(String cacheKey, Supplier<Timer.Builder> builderSupplier) {
        Timer cached = timers.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Timer registered = builderSupplier.get().register(meterRegistry);
        synchronized (cacheLimitLock) {
            Timer existing = timers.get(cacheKey);
            if (existing != null) {
                return existing;
            }
            if (counters.size() + timers.size() < MAX_CACHED_METERS) {
                timers.put(cacheKey, registered);
            }
            return registered;
        }
    }

    int cachedMeterCount() {
        return counters.size() + timers.size();
    }

    private static final class SingleRecordOperationSample implements OperationSample {

        private final OutcomeRecorder outcomeRecorder;
        private final AtomicBoolean recorded = new AtomicBoolean(false);

        private SingleRecordOperationSample(OutcomeRecorder outcomeRecorder) {
            this.outcomeRecorder = outcomeRecorder;
        }

        @Override
        public void recordSuccess() {
            record("success");
        }

        @Override
        public void recordFailure() {
            record("failure");
        }

        @Override
        public void recordTimeout() {
            record("timeout");
        }

        private void record(String outcome) {
            if (recorded.compareAndSet(false, true)) {
                outcomeRecorder.record(outcome);
            }
        }
    }

    @FunctionalInterface
    private interface OutcomeRecorder {
        void record(String outcome);
    }
}
