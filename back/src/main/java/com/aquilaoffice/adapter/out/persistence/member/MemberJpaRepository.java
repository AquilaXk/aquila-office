package com.aquilaoffice.adapter.out.persistence.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 JPA entity 조회를 담당하는 Spring Data repository다.
 */
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    Optional<MemberJpaEntity> findByGoogleSub(String googleSub);

    Optional<MemberJpaEntity> findByEmail(String email);
}
