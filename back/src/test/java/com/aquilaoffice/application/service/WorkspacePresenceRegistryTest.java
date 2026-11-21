package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import org.junit.jupiter.api.Test;

/**
 * WorkspacePresenceRegistry의 room 단위 idle 상태 상한(LRU eviction)과 online sink 보호를 검증한다.
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

    private static final class NoopSink implements WorkspacePresenceEventSink {
        @Override
        public void send(WorkspacePresenceMessage message) {}
    }
}
