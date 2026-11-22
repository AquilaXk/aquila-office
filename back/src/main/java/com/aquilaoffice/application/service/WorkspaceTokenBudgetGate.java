package com.aquilaoffice.application.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 워크스페이스별 일일 토큰 예산(동시 실행 예약분 포함)과 2의 거듭제곱 비트마스크 기반 분당 요청 수(RPM) 제한을 강제한다.
 */
public class WorkspaceTokenBudgetGate {

    public static final int DEFAULT_MAX_RPM = 60;
    private static final long RPM_WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final int maxRpm;
    private final LongSupplier nanoClock;
    private final ConcurrentMap<Long, WorkspaceBudgetState> states = new ConcurrentHashMap<>();

    public WorkspaceTokenBudgetGate() {
        this(DEFAULT_MAX_RPM, System::nanoTime);
    }

    WorkspaceTokenBudgetGate(int maxRpm, LongSupplier nanoClock) {
        if (maxRpm <= 0) {
            throw new IllegalArgumentException("maxRpm must be positive");
        }
        this.maxRpm = maxRpm;
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock must not be null");
    }

    public BudgetReservation tryReserve(
            Long workspaceId,
            Integer dailyTokenLimit,
            long alreadyUsedTodayTokens,
            int estimatedPromptTokens) {
        if (workspaceId == null || workspaceId <= 0L) {
            throw new IllegalArgumentException("workspaceId must be positive");
        }
        long reserveAmount = Math.max(0L, estimatedPromptTokens);
        WorkspaceBudgetState state = states.computeIfAbsent(workspaceId, ignored -> new WorkspaceBudgetState(maxRpm));

        if (dailyTokenLimit != null && dailyTokenLimit > 0) {
            long available = (long) dailyTokenLimit - Math.max(0L, alreadyUsedTodayTokens);
            if (available <= 0L) {
                return BudgetReservation.rejected("workspace daily token limit exceeded");
            }
            while (true) {
                long currentInFlight = state.inFlightReservedTokens.get();
                if (currentInFlight + reserveAmount > available) {
                    return BudgetReservation.rejected("workspace daily token limit exceeded");
                }
                if (state.inFlightReservedTokens.compareAndSet(currentInFlight, currentInFlight + reserveAmount)) {
                    break;
                }
            }
        } else {
            state.inFlightReservedTokens.addAndGet(reserveAmount);
        }

        if (!state.tryRecordRequest(nanoClock.getAsLong())) {
            state.inFlightReservedTokens.addAndGet(-reserveAmount);
            return BudgetReservation.rejected("workspace model request rate limit exceeded");
        }

        return new BudgetReservation(true, null, () -> state.inFlightReservedTokens.addAndGet(-reserveAmount));
    }

    long currentInFlightReservedTokens(Long workspaceId) {
        WorkspaceBudgetState state = states.get(workspaceId);
        return state == null ? 0L : state.inFlightReservedTokens.get();
    }

    public static final class BudgetReservation implements AutoCloseable {

        private final boolean allowed;
        private final String rejectionReason;
        private final Runnable releaseAction;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private BudgetReservation(boolean allowed, String rejectionReason, Runnable releaseAction) {
            this.allowed = allowed;
            this.rejectionReason = rejectionReason;
            this.releaseAction = releaseAction;
        }

        static BudgetReservation rejected(String reason) {
            return new BudgetReservation(false, reason, null);
        }

        public boolean allowed() {
            return allowed;
        }

        public String rejectionReason() {
            return rejectionReason;
        }

        @Override
        public void close() {
            if (releaseAction != null && released.compareAndSet(false, true)) {
                releaseAction.run();
            }
        }
    }

    private static final class WorkspaceBudgetState {

        private final AtomicLong inFlightReservedTokens = new AtomicLong(0L);
        private final LongRingBuffer rpmBuffer;

        private WorkspaceBudgetState(int maxRpm) {
            this.rpmBuffer = new LongRingBuffer(maxRpm);
        }

        private synchronized boolean tryRecordRequest(long nowNanos) {
            rpmBuffer.evictBefore(nowNanos - RPM_WINDOW_NANOS);
            if (!rpmBuffer.hasCapacity()) {
                return false;
            }
            rpmBuffer.push(nowNanos);
            return true;
        }
    }

    /**
     * 고정 할당 링 버퍼: 2의 거듭제곱 크기와 비트마스크(& mask) 인덱싱으로 나눗셈·재할당 없이 O(1) 슬라이딩 윈도우를 유지한다.
     */
    private static final class LongRingBuffer {

        private final long[] timestamps;
        private final int mask;
        private final int limit;
        private int head;
        private int count;

        private LongRingBuffer(int limit) {
            this.limit = limit;
            int capacity = 1;
            while (capacity < Math.max(limit, 16)) {
                capacity <<= 1;
            }
            this.timestamps = new long[capacity];
            this.mask = capacity - 1;
        }

        private void evictBefore(long cutoffNanos) {
            while (count > 0 && timestamps[head] - cutoffNanos < 0L) {
                head = (head + 1) & mask;
                count--;
            }
        }

        private boolean hasCapacity() {
            return count < limit;
        }

        private void push(long timestampNanos) {
            int tail = (head + count) & mask;
            timestamps[tail] = timestampNanos;
            count++;
        }
    }
}
