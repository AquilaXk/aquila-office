package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class WorkspaceTokenBudgetGateTest {

    @Test
    void enforcesDailyInFlightTokenBudgetAndReleasesOnClose() {
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        WorkspaceTokenBudgetGate gate = new WorkspaceTokenBudgetGate(10, clock::get);

        WorkspaceTokenBudgetGate.BudgetReservation first = gate.tryReserve(7L, 100, 60L, 25);
        assertThat(first.allowed()).isTrue();
        assertThat(gate.currentInFlightReservedTokens(7L)).isEqualTo(25L);

        // 동시 실행 예약분 25 + 기사용 60 + 요청 20 = 105 > 일일 상한 100
        WorkspaceTokenBudgetGate.BudgetReservation overflow = gate.tryReserve(7L, 100, 60L, 20);
        assertThat(overflow.allowed()).isFalse();
        assertThat(overflow.rejectionReason()).isEqualTo("workspace daily token limit exceeded");

        first.close();
        assertThat(gate.currentInFlightReservedTokens(7L)).isZero();

        try (WorkspaceTokenBudgetGate.BudgetReservation afterRelease = gate.tryReserve(7L, 100, 60L, 20)) {
            assertThat(afterRelease.allowed()).isTrue();
        }
    }

    @Test
    void enforcesSlidingRpmWindowUsingRingBufferAndRecoversAfterOneMinute() {
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        WorkspaceTokenBudgetGate gate = new WorkspaceTokenBudgetGate(3, clock::get);

        try (var r1 = gate.tryReserve(11L, null, 0L, 10);
             var r2 = gate.tryReserve(11L, null, 0L, 10);
             var r3 = gate.tryReserve(11L, null, 0L, 10)) {
            assertThat(r1.allowed()).isTrue();
            assertThat(r2.allowed()).isTrue();
            assertThat(r3.allowed()).isTrue();

            WorkspaceTokenBudgetGate.BudgetReservation fourth = gate.tryReserve(11L, null, 0L, 10);
            assertThat(fourth.allowed()).isFalse();
            assertThat(fourth.rejectionReason()).isEqualTo("workspace model request rate limit exceeded");
            assertThat(gate.currentInFlightReservedTokens(11L)).isEqualTo(30L);
        }

        clock.addAndGet(TimeUnit.SECONDS.toNanos(61));
        try (var afterWindow = gate.tryReserve(11L, null, 0L, 10)) {
            assertThat(afterWindow.allowed()).isTrue();
        }
    }
}
