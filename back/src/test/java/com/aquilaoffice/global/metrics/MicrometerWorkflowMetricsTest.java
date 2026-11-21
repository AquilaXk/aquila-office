package com.aquilaoffice.global.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.service.WorkflowMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

class MicrometerWorkflowMetricsTest {

    @Test
    void recordsTaskExecutionStartOutcomeAndDuration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WorkflowMetrics metrics = new MicrometerWorkflowMetrics(registry);

        WorkflowMetrics.OperationSample sample = metrics.startTaskExecution();
        sample.recordSuccess();

        assertThat(registry.find("aquila.task.execution.started").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("aquila.task.execution.completed")
                        .tag("status", "succeeded")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.find("aquila.task.execution.duration")
                        .tag("status", "succeeded")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void recordsGatewayRpcRequestOutcomesAndLatency() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WorkflowMetrics metrics = new MicrometerWorkflowMetrics(registry);

        metrics.startGatewayRpc("chat.send").recordSuccess();
        metrics.startGatewayRpc("chat.send").recordTimeout();
        metrics.startGatewayRpc("agents.create").recordFailure();

        assertThat(registry.find("aquila.gateway.rpc.requests")
                        .tag("method", "chat.send")
                        .counter()
                        .count())
                .isEqualTo(2.0);
        assertThat(registry.find("aquila.gateway.rpc.completed")
                        .tag("method", "chat.send")
                        .tag("outcome", "timeout")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.find("aquila.gateway.rpc.completed")
                        .tag("method", "agents.create")
                        .tag("outcome", "failure")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.find("aquila.gateway.rpc.latency")
                        .tag("method", "chat.send")
                        .tag("outcome", "success")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void recordsSlackEventCountersWithLowCardinalityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WorkflowMetrics metrics = new MicrometerWorkflowMetrics(registry);

        metrics.recordSlackEventReceived("global");
        metrics.recordSlackSignatureFailure("workspace");
        metrics.recordSlackDedupeHit();
        metrics.recordSlackProcessingFailure("use_case");

        assertThat(registry.find("aquila.slack.events.received")
                        .tag("route", "global")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.find("aquila.slack.events.signature_failures")
                        .tag("route", "workspace")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.find("aquila.slack.events.dedupe_hits").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.find("aquila.slack.events.processing_failures")
                        .tag("stage", "use_case")
                        .counter()
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void metricTagsDoNotContainSecretMaterial() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WorkflowMetrics metrics = new MicrometerWorkflowMetrics(registry);

        metrics.startGatewayRpc("secret-token").recordFailure();

        assertThat(registry.getMeters().stream()
                        .flatMap(meter -> meter.getId().getTags().stream())
                        .map(tag -> tag.getValue())
                        .toList())
                .doesNotContain("secret-token");
    }

    @Test
    void prometheusScrapeExposesWorkflowMetricNames() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        WorkflowMetrics metrics = new MicrometerWorkflowMetrics(registry);

        metrics.startTaskExecution().recordSuccess();
        metrics.startGatewayRpc("chat.send").recordTimeout();
        metrics.recordSlackEventReceived("global");

        assertThat(registry.scrape())
                .contains("aquila_task_execution_started_total")
                .contains("aquila_task_execution_duration_seconds")
                .contains("aquila_gateway_rpc_completed_total")
                .contains("aquila_gateway_rpc_latency_seconds")
                .contains("aquila_slack_events_received_total");
    }

    @Test
    void cachedMetersAreBoundedByMaxCachedMeters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerWorkflowMetrics metrics = new MicrometerWorkflowMetrics(registry);

        for (int i = 0; i < MicrometerWorkflowMetrics.MAX_CACHED_METERS + 40; i++) {
            int index = i;
            metrics.counter(
                    "custom.counter." + index,
                    () -> io.micrometer.core.instrument.Counter.builder("custom.test.counter")
                            .tag("idx", String.valueOf(index)))
                    .increment();
        }
        metrics.timer(
                "custom.timer.overflow",
                () -> io.micrometer.core.instrument.Timer.builder("custom.test.timer"))
                .record(java.time.Duration.ofMillis(1));

        assertThat(metrics.cachedMeterCount()).isEqualTo(MicrometerWorkflowMetrics.MAX_CACHED_METERS);
    }
}
