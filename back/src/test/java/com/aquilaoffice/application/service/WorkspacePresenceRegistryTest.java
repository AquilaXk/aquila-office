package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * WorkspacePresenceRegistry의 room 단위 idle 상태 상한(LRU eviction), 멤버/룸 sink 상한, stale 연결 정리를 검증한다.
 */
class WorkspacePresenceRegistryTest {

    @Test
    void evictsOldestIdleMembersOverLimitWhileKeepingOnlineMembersAndAllowingPositionWithoutJoin() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        WorkspacePresenceEventSink activeSink = new NoopSink();

        // join 없이 updatePosition 호출도 정상 동작한다.
        registry.updatePosition(101L, 1L, new WorkspacePresencePosition("10%", "20%"), "ACTIVE");
        assertThat(registry.findMemberState(101L, 1L))
                .hasValueSatisfying(state -> assertThat(state.position().left()).isEqualTo("10%"));

        // 2번 멤버는 활성 SSE sink를 보유하므로 oldest라도 eviction 대상에서 제외된다.
        registry.join(101L, 2L, activeSink);
        registry.updatePosition(101L, 2L, new WorkspacePresencePosition("20%", "30%"), "ACTIVE");

        // idle 멤버를 MAX + 20명 추가한다. (1번 idle 멤버부터 순서대로 eviction되어야 한다.)
        int totalIdle = WorkspacePresenceRegistry.MAX_IDLE_MEMBER_STATES_PER_ROOM + 20;
        for (int i = 0; i < totalIdle; i++) {
            long memberId = 1000L + i;
            registry.updatePosition(101L, memberId, new WorkspacePresencePosition("50%", "50%"), "IDLE");
        }

        // online 멤버 2번은 반드시 유지된다.
        assertThat(registry.findMemberState(101L, 2L)).isPresent();
        // 초기에 추가한 idle 멤버 1번과 1000~1019는 밀려나고 최신 idle 256명은 유지된다.
        assertThat(registry.findMemberState(101L, 1L)).isEmpty();
        assertThat(registry.findMemberState(101L, 1000L)).isEmpty();
        assertThat(registry.findMemberState(101L, 1019L)).isEmpty();
        assertThat(registry.findMemberState(101L, 1020L)).isPresent();
        assertThat(registry.findMemberState(101L, 1000L + totalIdle - 1)).isPresent();
    }

    @Test
    void capsSinksPerMemberAndEvictsStaleConnections() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-22T00:00:00Z"));
        Clock clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry(clock);
        NoopSink first = new NoopSink();
        NoopSink second = new NoopSink();
        NoopSink third = new NoopSink();
        NoopSink fourth = new NoopSink();
        NoopSink fifth = new NoopSink();

        registry.join(101L, 1L, first);
        registry.join(101L, 1L, second);
        registry.join(101L, 1L, third);
        registry.join(101L, 1L, fourth);
        registry.join(101L, 1L, fifth);

        assertThat(first.closed.get()).isTrue();
        assertThat(registry.connectionsInWorkspace(101L)).hasSize(WorkspacePresenceRegistry.MAX_SINKS_PER_MEMBER);

        now.set(now.get().plusSeconds(600));
        registry.touchSinkHeartbeat(101L, 1L, fifth);
        List<WorkspacePresenceMessage> leaves = registry.evictStaleConnections(Duration.ofSeconds(300));
        assertThat(leaves).isEmpty();
        assertThat(registry.connectionsInWorkspace(101L)).hasSize(1);
        assertThat(second.closed.get()).isTrue();
    }

    private static final class NoopSink implements WorkspacePresenceEventSink {
        private final AtomicBoolean closed = new AtomicBoolean();

        @Override
        public void send(WorkspacePresenceMessage message) {}

        @Override
        public void closeSilently() {
            closed.set(true);
        }
    }
}
