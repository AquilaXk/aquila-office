package com.aquilaoffice.adapter.out.persistence.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.ClaimSlackEventPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Slack event_id 저장 claim이 같은 이벤트를 한 번만 처리하게 하는지 검증한다.
 */
@DataJpaTest
@Import(SlackEventDeduplicationPersistenceAdapter.class)
class SlackEventDeduplicationPersistenceAdapterTest {

    @Autowired
    private ClaimSlackEventPort claimSlackEventPort;

    @Autowired
    private SlackEventReceiptJpaRepository slackEventReceiptJpaRepository;

    @Test
    void claimEventReturnsTrueOnlyForFirstEventId() {
        assertThat(claimSlackEventPort.claimEvent("E123")).isTrue();
        assertThat(claimSlackEventPort.claimEvent("E123")).isFalse();
        assertThat(claimSlackEventPort.claimEvent("E456")).isTrue();

        assertThat(slackEventReceiptJpaRepository.count()).isEqualTo(2L);
    }
}
