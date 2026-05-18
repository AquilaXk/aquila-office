package com.aquilaoffice.adapter.out.persistence.slack;

import com.aquilaoffice.application.port.out.ClaimSlackEventPort;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Slack event_id를 unique row로 claim해 재전송 이벤트의 중복 실행을 막는다.
 */
@Repository
public class SlackEventDeduplicationPersistenceAdapter implements ClaimSlackEventPort {

    private final SlackEventReceiptJpaRepository slackEventReceiptJpaRepository;

    public SlackEventDeduplicationPersistenceAdapter(
            SlackEventReceiptJpaRepository slackEventReceiptJpaRepository) {
        this.slackEventReceiptJpaRepository =
                Objects.requireNonNull(slackEventReceiptJpaRepository, "slackEventReceiptJpaRepository must not be null");
    }

    @Override
    @Transactional
    public boolean claimEvent(String eventId) {
        String normalizedEventId = normalizeRequired(eventId);
        if (slackEventReceiptJpaRepository.existsByEventId(normalizedEventId)) {
            return false;
        }

        try {
            slackEventReceiptJpaRepository.saveAndFlush(SlackEventReceiptJpaEntity.create(normalizedEventId));
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private String normalizeRequired(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return eventId.trim();
    }
}
