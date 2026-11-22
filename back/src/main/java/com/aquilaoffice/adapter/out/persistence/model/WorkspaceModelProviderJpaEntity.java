package com.aquilaoffice.adapter.out.persistence.model;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * workspace_model_providers 테이블 매핑 JPA 엔티티다.
 */
@Entity
@Table(name = "workspace_model_providers")
public class WorkspaceModelProviderJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_kind", nullable = false, length = 40)
    private ModelProviderKind providerKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "wire_protocol", nullable = false, length = 40)
    private ModelWireProtocol wireProtocol;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "api_key", columnDefinition = "TEXT")
    private String apiKey;

    @Column(name = "default_model", nullable = false, length = 160)
    private String defaultModel;

    @Column(name = "enabled_models_json", nullable = false, columnDefinition = "TEXT")
    private String enabledModelsJson;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", length = 40)
    private ModelProviderConnectionStatus lastStatus;

    @Column(name = "last_latency_ms")
    private Long lastLatencyMs;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected WorkspaceModelProviderJpaEntity() {}

    public WorkspaceModelProviderJpaEntity(
            WorkspaceJpaEntity workspace,
            ModelProviderKind providerKind,
            ModelWireProtocol wireProtocol,
            String displayName,
            String baseUrl,
            String apiKey,
            String defaultModel,
            String enabledModelsJson,
            int priorityOrder,
            boolean enabled,
            ModelProviderConnectionStatus lastStatus,
            Long lastLatencyMs,
            LocalDateTime lastCheckedAt,
            String lastError,
            String createdByGoogleSub,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt) {
        this.workspace = workspace;
        this.providerKind = providerKind;
        this.wireProtocol = wireProtocol;
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.enabledModelsJson = enabledModelsJson;
        this.priorityOrder = priorityOrder;
        this.enabled = enabled;
        this.lastStatus = lastStatus;
        this.lastLatencyMs = lastLatencyMs;
        this.lastCheckedAt = lastCheckedAt;
        this.lastError = lastError;
        this.createdByGoogleSub = createdByGoogleSub;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public void syncMutableFields(
            ModelWireProtocol wireProtocol,
            String displayName,
            String baseUrl,
            String encryptedApiKey,
            String defaultModel,
            String enabledModelsJson,
            int priorityOrder,
            boolean enabled,
            ModelProviderConnectionStatus lastStatus,
            Long lastLatencyMs,
            LocalDateTime lastCheckedAt,
            String lastError,
            LocalDateTime deletedAt) {
        this.wireProtocol = wireProtocol;
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.apiKey = encryptedApiKey;
        this.defaultModel = defaultModel;
        this.enabledModelsJson = enabledModelsJson;
        this.priorityOrder = priorityOrder;
        this.enabled = enabled;
        this.lastStatus = lastStatus;
        this.lastLatencyMs = lastLatencyMs;
        this.lastCheckedAt = lastCheckedAt;
        this.lastError = lastError;
        this.deletedAt = deletedAt;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public ModelProviderKind getProviderKind() {
        return providerKind;
    }

    public ModelWireProtocol getWireProtocol() {
        return wireProtocol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public String getEnabledModelsJson() {
        return enabledModelsJson;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ModelProviderConnectionStatus getLastStatus() {
        return lastStatus;
    }

    public Long getLastLatencyMs() {
        return lastLatencyMs;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
