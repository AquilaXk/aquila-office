package com.aquilaoffice.adapter.out.persistence.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 워크스페이스 JPA entity 저장과 조회를 담당하는 Spring Data repository다.
 */
public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, Long> {}
