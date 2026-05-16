package com.aquilaoffice.adapter.out.persistence.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 멤버 프로필 entity 조회를 담당하는 Spring Data repository다.
 */
public interface MemberProfileJpaRepository extends JpaRepository<MemberProfileJpaEntity, Long> {

    Optional<MemberProfileJpaEntity> findByMember_GoogleSub(String googleSub);
}

