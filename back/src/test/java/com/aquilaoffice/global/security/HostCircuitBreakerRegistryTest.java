package com.aquilaoffice.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class HostCircuitBreakerRegistryTest {

    @Test
    void opensAfterFiveConsecutiveFailuresAndGrantsSingleHalfOpenProbeAfterCooldown() {
        AtomicLong clockNanos = new AtomicLong(1_000_000_000L);
        HostCircuitBreakerRegistry registry = new HostCircuitBreakerRegistry(
                128,
                5,
                Duration.ofMinutes(10),
                Duration.ofSeconds(30),
                clockNanos::get);

        for (int i = 0; i < 5; i++) {
            HostCircuitBreakerRegistry.CircuitLease lease = registry.tryAcquire("api.openai.com");
            assertThat(lease.allowed()).isTrue();
            assertThat(lease.halfOpenProbe()).isFalse();
            registry.recordFailure("api.openai.com", lease);
        }

        assertThat(registry.tryAcquire("api.openai.com").allowed()).isFalse();

        clockNanos.addAndGet(Duration.ofSeconds(31).toNanos());
        HostCircuitBreakerRegistry.CircuitLease probe = registry.tryAcquire("api.openai.com");
        assertThat(probe.allowed()).isTrue();
        assertThat(probe.halfOpenProbe()).isTrue();

        assertThat(registry.tryAcquire("api.openai.com").allowed()).isFalse();

        registry.recordSuccess("api.openai.com", probe);
        HostCircuitBreakerRegistry.CircuitLease afterRecovery = registry.tryAcquire("api.openai.com");
        assertThat(afterRecovery.allowed()).isTrue();
        assertThat(afterRecovery.halfOpenProbe()).isFalse();
    }

    @Test
    void isolatesGenerationsResetsWindowAndBoundsTrackedHostsAt128() {
        AtomicLong clockNanos = new AtomicLong(1_000_000_000L);
        HostCircuitBreakerRegistry registry = new HostCircuitBreakerRegistry(
                128,
                5,
                Duration.ofMinutes(10),
                Duration.ofSeconds(30),
                clockNanos::get);

        HostCircuitBreakerRegistry.CircuitLease staleLease = registry.tryAcquire("api.anthropic.com");
        for (int i = 0; i < 4; i++) {
            registry.recordFailure("api.anthropic.com", registry.tryAcquire("api.anthropic.com"));
        }

        clockNanos.addAndGet(Duration.ofMinutes(11).toNanos());
        HostCircuitBreakerRegistry.CircuitLease freshWindowLease = registry.tryAcquire("api.anthropic.com");
        assertThat(freshWindowLease.allowed()).isTrue();
        registry.recordSuccess("api.anthropic.com", freshWindowLease);

        // 지연된 이전 generation의 실패는 현재 복구된 상태를 오염시키지 못해야 한다.
        registry.recordFailure("api.anthropic.com", staleLease);
        assertThat(registry.tryAcquire("api.anthropic.com").allowed()).isTrue();

        for (int i = 0; i < 150; i++) {
            registry.tryAcquire("host-" + i + ".example.com");
        }
        assertThat(registry.trackedHostCount()).isEqualTo(128);
    }
}
