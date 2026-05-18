package com.aquilaoffice.adapter.out.persistence.slack;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Slack event receipt 저장과 event_id 존재 여부 확인을 담당하는 repository다.
 */
public interface SlackEventReceiptJpaRepository extends JpaRepository<SlackEventReceiptJpaEntity, Long> {

    boolean existsByEventId(String eventId);
}
