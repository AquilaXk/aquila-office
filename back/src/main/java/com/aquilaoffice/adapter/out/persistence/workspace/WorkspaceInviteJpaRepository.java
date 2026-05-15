package com.aquilaoffice.adapter.out.persistence.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 워크스페이스 초대 JPA entity 저장과 token 조회를 담당하는 Spring Data repository다.
 */
public interface WorkspaceInviteJpaRepository extends JpaRepository<WorkspaceInviteJpaEntity, Long> {

    Optional<WorkspaceInviteJpaEntity> findByToken(String token);
}
