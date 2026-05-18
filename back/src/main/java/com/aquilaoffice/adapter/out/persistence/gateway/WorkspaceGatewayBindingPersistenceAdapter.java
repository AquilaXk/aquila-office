package com.aquilaoffice.adapter.out.persistence.gateway;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceGatewayBindingPort;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.global.security.ExternalTokenCipher;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * workspace gateway binding outbound port를 JPA repository 조합으로 구현하는 persistence adapter다.
 */
@Repository
public class WorkspaceGatewayBindingPersistenceAdapter
        implements SaveWorkspaceGatewayBindingPort, LoadWorkspaceGatewayBindingPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final WorkspaceGatewayBindingJpaRepository workspaceGatewayBindingJpaRepository;
    private final ExternalTokenCipher externalTokenCipher;

    public WorkspaceGatewayBindingPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceGatewayBindingJpaRepository workspaceGatewayBindingJpaRepository,
            ExternalTokenCipher externalTokenCipher) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.workspaceGatewayBindingJpaRepository = workspaceGatewayBindingJpaRepository;
        this.externalTokenCipher = externalTokenCipher;
    }

    @Override
    @Transactional
    public WorkspaceGatewayBinding save(WorkspaceGatewayBinding binding) {
        Objects.requireNonNull(binding, "binding must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(binding.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));

        LocalDateTime replacedAt = LocalDateTime.now();
        workspaceGatewayBindingJpaRepository.findActiveByWorkspaceIdOrderByIdDesc(binding.getWorkspaceId())
                .forEach(entity -> entity.softDelete(replacedAt));

        WorkspaceGatewayBindingJpaEntity savedEntity = workspaceGatewayBindingJpaRepository.save(
                WorkspaceGatewayBindingJpaEntity.from(
                        binding,
                        workspaceEntity,
                        externalTokenCipher.encrypt(binding.getToken())));
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceGatewayBinding> findActiveByWorkspaceId(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");

        return workspaceGatewayBindingJpaRepository.findLatestActiveByWorkspaceId(workspaceId)
                .map(this::toDomain);
    }

    private WorkspaceGatewayBinding toDomain(WorkspaceGatewayBindingJpaEntity entity) {
        return WorkspaceGatewayBinding.restore(
                entity.getId(),
                entity.getWorkspace().getId(),
                entity.getGatewayUrl(),
                externalTokenCipher.decrypt(entity.getToken()),
                entity.getCreatedByGoogleSub(),
                entity.getDeviceId(),
                entity.getDeviceName(),
                entity.getAuthenticatedAt(),
                entity.getLastStatus(),
                entity.getLastCheckedAt(),
                entity.getLastError());
    }
}
