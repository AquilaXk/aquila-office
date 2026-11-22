package com.aquilaoffice.global.security;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 호스트별 연속 실패를 추적해 회로 차단(OPEN) 및 단일 Half-Open 탐침(probe)을 보장한다.
 * 최대 128개 호스트만 LRU로 유지하며 generation 격리로 지연된 이전 요청의 오작동을 차단한다.
 */
@Component
public class HostCircuitBreakerRegistry {

    public static final int DEFAULT_MAX_HOSTS = 128;
    public static final int DEFAULT_FAILURE_THRESHOLD = 5;
    public static final Duration DEFAULT_WINDOW_DURATION = Duration.ofMinutes(10);
    public static final Duration DEFAULT_COOLDOWN_DURATION = Duration.ofSeconds(30);

    private final int maxHosts;
    private final int failureThreshold;
    private final long windowNanos;
    private final long cooldownNanos;
    private final LongSupplier nanoClock;
    private final Map<String, HostState> states;

    @Autowired
    public HostCircuitBreakerRegistry() {
        this(
                DEFAULT_MAX_HOSTS,
                DEFAULT_FAILURE_THRESHOLD,
                DEFAULT_WINDOW_DURATION,
                DEFAULT_COOLDOWN_DURATION,
                System::nanoTime);
    }

    HostCircuitBreakerRegistry(
            int maxHosts,
            int failureThreshold,
            Duration windowDuration,
            Duration cooldownDuration,
            LongSupplier nanoClock) {
        if (maxHosts <= 0 || failureThreshold <= 0) {
            throw new IllegalArgumentException("maxHosts and failureThreshold must be positive");
        }
        this.maxHosts = maxHosts;
        this.failureThreshold = failureThreshold;
        this.windowNanos = Objects.requireNonNull(windowDuration, "windowDuration must not be null").toNanos();
        this.cooldownNanos = Objects.requireNonNull(cooldownDuration, "cooldownDuration must not be null").toNanos();
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock must not be null");
        this.states = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, HostState> eldest) {
                return size() > HostCircuitBreakerRegistry.this.maxHosts;
            }
        };
    }

    public synchronized CircuitLease tryAcquire(String host) {
        String key = normalizeHost(host);
        if (key == null) {
            return CircuitLease.rejected();
        }
        long now = nanoClock.getAsLong();
        HostState state = states.get(key);
        if (state == null) {
            state = new HostState(now);
            states.put(key, state);
            return new CircuitLease(true, false, state.generation);
        }
        if (state.openUntilNanos != 0L) {
            if (now - state.openUntilNanos < 0L) {
                return CircuitLease.rejected();
            }
            if (state.probeInFlight) {
                return CircuitLease.rejected();
            }
            state.probeInFlight = true;
            state.generation++;
            return new CircuitLease(true, true, state.generation);
        }
        if (now - state.windowStartNanos > windowNanos) {
            state.consecutiveFailures = 0;
            state.windowStartNanos = now;
        }
        return new CircuitLease(true, false, state.generation);
    }

    public synchronized void recordSuccess(String host, CircuitLease lease) {
        if (lease == null || !lease.allowed()) {
            return;
        }
        String key = normalizeHost(host);
        if (key == null) {
            return;
        }
        HostState state = states.get(key);
        if (state == null || state.generation != lease.generation()) {
            return;
        }
        if (lease.halfOpenProbe() || state.consecutiveFailures > 0 || state.openUntilNanos != 0L) {
            state.consecutiveFailures = 0;
            state.openUntilNanos = 0L;
            state.probeInFlight = false;
            state.windowStartNanos = nanoClock.getAsLong();
            state.generation++;
        }
    }

    public synchronized void recordFailure(String host, CircuitLease lease) {
        if (lease == null || !lease.allowed()) {
            return;
        }
        String key = normalizeHost(host);
        if (key == null) {
            return;
        }
        long now = nanoClock.getAsLong();
        HostState state = states.get(key);
        if (state == null || state.generation != lease.generation()) {
            return;
        }
        if (lease.halfOpenProbe()) {
            state.probeInFlight = false;
            state.consecutiveFailures = failureThreshold;
            state.openUntilNanos = now + cooldownNanos;
            state.generation++;
            return;
        }
        if (now - state.windowStartNanos > windowNanos) {
            state.windowStartNanos = now;
            state.consecutiveFailures = 1;
        } else {
            state.consecutiveFailures++;
        }
        if (state.consecutiveFailures >= failureThreshold) {
            state.openUntilNanos = now + cooldownNanos;
            state.probeInFlight = false;
            state.generation++;
        }
    }

    synchronized int trackedHostCount() {
        return states.size();
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        return host.trim().toLowerCase(Locale.ROOT);
    }

    public record CircuitLease(boolean allowed, boolean halfOpenProbe, long generation) {
        private static final CircuitLease REJECTED = new CircuitLease(false, false, -1L);

        static CircuitLease rejected() {
            return REJECTED;
        }
    }

    private static final class HostState {
        private long generation;
        private long windowStartNanos;
        private int consecutiveFailures;
        private long openUntilNanos;
        private boolean probeInFlight;

        private HostState(long now) {
            this.generation = 1L;
            this.windowStartNanos = now;
        }
    }
}
