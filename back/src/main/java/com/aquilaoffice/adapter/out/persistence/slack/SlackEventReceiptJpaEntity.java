package com.aquilaoffice.adapter.out.persistence.slack;

import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Slack event_id를 저장해 재전송된 이벤트를 한 번만 처리하게 하는 receipt entity다.
 */
@Entity
@Table(
        name = "slack_event_receipts",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_slack_event_receipts_event_id",
                    columnNames = "event_id")
        })
public class SlackEventReceiptJpaEntity extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 160)
    private String eventId;

    protected SlackEventReceiptJpaEntity() {}

    private SlackEventReceiptJpaEntity(String eventId) {
        this.eventId = eventId;
    }

    public static SlackEventReceiptJpaEntity create(String eventId) {
        return new SlackEventReceiptJpaEntity(normalizeRequired(eventId));
    }

    public String getEventId() {
        return eventId;
    }

    private static String normalizeRequired(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return eventId.trim();
    }
}
