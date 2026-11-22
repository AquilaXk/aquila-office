package com.aquilaoffice.adapter.out.persistence.model;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceModelProviderPort;
import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import com.aquilaoffice.global.security.ExternalTokenCipher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 워크스페이스 AI 모델 공급자 영속성과 컨텍스트 바인딩 AES-GCM API 키 암·복호화를 담당한다.
 */
@Repository
public class WorkspaceModelProviderPersistenceAdapter
        implements LoadWorkspaceModelProviderPort, SaveWorkspaceModelProviderPort {

    private static final int MAX_PROVIDERS_PER_WORKSPACE = 50;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final WorkspaceModelProviderJpaRepository repository;
    private final ExternalTokenCipher externalTokenCipher;
    private final ObjectMapper objectMapper;

    @Autowired
    public WorkspaceModelProviderPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceModelProviderJpaRepository repository,
            ExternalTokenCipher externalTokenCipher,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        this(
                workspaceJpaRepository,
                repository,
                externalTokenCipher,
                objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    WorkspaceModelProviderPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceModelProviderJpaRepository repository,
            ExternalTokenCipher externalTokenCipher,
            ObjectMapper objectMapper) {
        this.workspaceJpaRepository = Objects.requireNonNull(
                workspaceJpaRepository, "workspaceJpaRepository must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.externalTokenCipher = Objects.requireNonNull(
                externalTokenCipher, "externalTokenCipher must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceModelProvider> findActiveByWorkspaceId(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return repository.findActiveByWorkspaceId(workspaceId, PageRequest.of(0, MAX_PROVIDERS_PER_WORKSPACE))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceModelProvider> findActiveByWorkspaceIdAndProviderId(
            Long workspaceId,
            Long providerId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(providerId, "providerId must not be null");
        return repository.findActiveByWorkspaceIdAndId(workspaceId, providerId).map(this::toDomain);
    }

    @Override
    @Transactional
    public WorkspaceModelProvider save(WorkspaceModelProvider provider) {
        Objects.requireNonNull(provider, "provider must not be null");
        String context = encryptionContext(provider.getWorkspaceId());
        String encryptedKey = provider.getApiKey() == null
                ? null
                : externalTokenCipher.encryptWithContext(provider.getApiKey(), context);
        String enabledModelsJson = serializeModels(provider.getEnabledModels());

        if (provider.getId() != null) {
            WorkspaceModelProviderJpaEntity existing = repository
                    .findActiveByWorkspaceIdAndId(provider.getWorkspaceId(), provider.getId())
                    .orElseThrow(() -> new IllegalArgumentException("workspace model provider not found"));
            existing.syncMutableFields(
                    provider.getWireProtocol(),
                    provider.getDisplayName(),
                    provider.getBaseUrl(),
                    encryptedKey,
                    provider.getDefaultModel(),
                    enabledModelsJson,
                    provider.getPriorityOrder(),
                    provider.isEnabled(),
                    provider.getLastStatus(),
                    provider.getLastLatencyMs(),
                    provider.getLastCheckedAt(),
                    provider.getLastError(),
                    provider.getDeletedAt());
            return toDomain(existing);
        }

        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(provider.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        WorkspaceModelProviderJpaEntity saved = repository.save(new WorkspaceModelProviderJpaEntity(
                workspaceEntity,
                provider.getProviderKind(),
                provider.getWireProtocol(),
                provider.getDisplayName(),
                provider.getBaseUrl(),
                encryptedKey,
                provider.getDefaultModel(),
                enabledModelsJson,
                provider.getPriorityOrder(),
                provider.isEnabled(),
                provider.getLastStatus(),
                provider.getLastLatencyMs(),
                provider.getLastCheckedAt(),
                provider.getLastError(),
                provider.getCreatedByGoogleSub(),
                now,
                now,
                null));
        return toDomain(saved);
    }

    private WorkspaceModelProvider toDomain(WorkspaceModelProviderJpaEntity entity) {
        Long workspaceId = entity.getWorkspace().getId();
        String decryptedKey = entity.getApiKey() == null
                ? null
                : externalTokenCipher.decryptWithContext(entity.getApiKey(), encryptionContext(workspaceId));
        return WorkspaceModelProvider.restore(
                entity.getId(),
                workspaceId,
                entity.getProviderKind(),
                entity.getWireProtocol(),
                entity.getDisplayName(),
                entity.getBaseUrl(),
                decryptedKey,
                entity.getDefaultModel(),
                deserializeModels(entity.getEnabledModelsJson(), entity.getDefaultModel()),
                entity.getPriorityOrder(),
                entity.isEnabled(),
                entity.getLastStatus(),
                entity.getLastLatencyMs(),
                entity.getLastCheckedAt(),
                entity.getLastError(),
                entity.getCreatedByGoogleSub(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    private String serializeModels(List<String> models) {
        try {
            return objectMapper.writeValueAsString(models);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("enabled models serialization failed", exception);
        }
    }

    private List<String> deserializeModels(String json, String defaultModel) {
        if (json == null || json.isBlank()) {
            return List.of(defaultModel);
        }
        try {
            List<String> parsed = objectMapper.readValue(json, STRING_LIST_TYPE);
            return parsed == null || parsed.isEmpty() ? List.of(defaultModel) : parsed;
        } catch (JsonProcessingException exception) {
            return List.of(defaultModel);
        }
    }

    private static String encryptionContext(Long workspaceId) {
        return "ws:" + workspaceId + ":model-provider";
    }
}
