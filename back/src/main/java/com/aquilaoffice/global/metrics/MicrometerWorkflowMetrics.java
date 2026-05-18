package com.aquilaoffice.global.metrics;

import com.aquilaoffice.application.service.WorkflowMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final MeterRegistry meterRegistry;

    public MicrometerWorkflowMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public OperationSample startTaskExecution() {
        counter("aquila.task.execution.started")
                .description("Task execution start count")
                .register(meterRegistry)
                .increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        return new SingleRecordOperationSample(outcome -> {
            String status = "success".equals(outcome) ? "succeeded" : "failed";
            counter("aquila.task.execution.completed")
                    .description("Task execution completion count")
                    .tag("status", status)
                    .register(meterRegistry)
                    .increment();
            sample.stop(timer("aquila.task.execution.duration")
                    .description("Task execution duration")
                    .tag("status", status)
                    .register(meterRegistry));
        });
    }

    @Override
    public OperationSample startGatewayRpc(String method) {
        String methodLabel = normalizeGatewayMethod(method);
        counter("aquila.gateway.rpc.requests")
                .description("Gateway RPC request count")
                .tag("method", methodLabel)
                .register(meterRegistry)
                .increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        return new SingleRecordOperationSample(outcome -> {
            counter("aquila.gateway.rpc.completed")
                    .description("Gateway RPC completion count")
                    .tag("method", methodLabel)
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .increment();
            sample.stop(timer("aquila.gateway.rpc.latency")
                    .description("Gateway RPC latency")
                    .tag("method", methodLabel)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        });
    }

    @Override
    public void recordSlackEventReceived(String route) {
        counter("aquila.slack.events.received")
                .description("Slack event receive count")
                .tag("route", normalizeSlackRoute(route))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordSlackSignatureFailure(String route) {
        counter("aquila.slack.events.signature_failures")
                .description("Slack signature verification failure count")
                .tag("route", normalizeSlackRoute(route))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordSlackDedupeHit() {
        counter("aquila.slack.events.dedupe_hits")
                .description("Slack event deduplication hit count")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordSlackProcessingFailure(String stage) {
        counter("aquila.slack.events.processing_failures")
                .description("Slack event processing failure count")
                .tag("stage", normalizeSlackStage(stage))
                .register(meterRegistry)
                .increment();
    }

    private String normalizeGatewayMethod(String method) {
        if (method == null || method.isBlank()) {
            return "unknown";
        }
        String normalizedMethod = method.trim();
        if (GATEWAY_METHOD_LABELS.contains(normalizedMethod)) {
            return normalizedMethod;
        }
        return "unknown";
    }

    private String normalizeSlackRoute(String route) {
        if (route == null || route.isBlank()) {
            return "unknown";
        }
        String normalizedRoute = route.trim();
        if (SLACK_ROUTE_LABELS.contains(normalizedRoute)) {
            return normalizedRoute;
        }
        return "unknown";
    }

    private String normalizeSlackStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return "unknown";
        }
        String normalizedStage = stage.trim();
        if (SLACK_STAGE_LABELS.contains(normalizedStage)) {
            return normalizedStage;
        }
        return "unknown";
    }

    private Counter.Builder counter(String name) {
        return Counter.builder(name);
    }

    private Timer.Builder timer(String name) {
        return Timer.builder(name);
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
