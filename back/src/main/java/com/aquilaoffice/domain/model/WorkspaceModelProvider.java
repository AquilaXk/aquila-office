package com.aquilaoffice.domain.model;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 워크스페이스에 연결된 AI 모델 공급자 설정과 최근 연결 진단 상태를 표현하는 도메인 엔티티다.
 */
public class WorkspaceModelProvider {

    private static final int MAX_DISPLAY_NAME_LENGTH = 120;
    private static final int MAX_BASE_URL_LENGTH = 500;
    private static final int MAX_MODEL_ID_LENGTH = 160;
    private static final int MAX_ENABLED_MODELS = 100;
    private static final int DEFAULT_PRIORITY_ORDER = 100;

    private final Long id;
    private final Long workspaceId;
    private final ModelProviderKind providerKind;
    private ModelWireProtocol wireProtocol;
    private String displayName;
    private String baseUrl;
    private String apiKey;
    private String defaultModel;
    private List<String> enabledModels;
    private int priorityOrder;
    private boolean enabled;
    private ModelProviderConnectionStatus lastStatus;
    private Long lastLatencyMs;
    private LocalDateTime lastCheckedAt;
    private String lastError;
    private final String createdByGoogleSub;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private WorkspaceModelProvider(
            Long id,
            Long workspaceId,
            ModelProviderKind providerKind,
            ModelWireProtocol wireProtocol,
            String displayName,
            String baseUrl,
            String apiKey,
            String defaultModel,
            List<String> enabledModels,
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
        this.id = id;
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        if (providerKind == null) {
            throw new IllegalArgumentException("providerKind must not be null");
        }
        this.providerKind = providerKind;
        this.wireProtocol = wireProtocol != null ? wireProtocol : providerKind.defaultWireProtocol();
        this.displayName = validateLength(displayName, "displayName", MAX_DISPLAY_NAME_LENGTH);
        this.baseUrl = validateBaseUrl(baseUrl);
        this.apiKey = normalizeNullable(apiKey);
        if (providerKind.requiresApiKey() && this.apiKey == null) {
            throw new IllegalArgumentException("apiKey is required for provider " + providerKind.name());
        }
        this.defaultModel = validateLength(defaultModel, "defaultModel", MAX_MODEL_ID_LENGTH);
        this.enabledModels = normalizeEnabledModels(enabledModels, this.defaultModel);
        this.priorityOrder = Math.max(0, priorityOrder);
        this.enabled = enabled;
        this.lastStatus = lastStatus;
        this.lastLatencyMs = lastLatencyMs;
        this.lastCheckedAt = lastCheckedAt;
        this.lastError = normalizeNullable(lastError);
        this.createdByGoogleSub = validateLength(createdByGoogleSub, "createdByGoogleSub", MAX_DISPLAY_NAME_LENGTH);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static WorkspaceModelProvider create(
            Long workspaceId,
            ModelProviderKind providerKind,
            ModelWireProtocol wireProtocol,
            String displayName,
            String baseUrl,
            String apiKey,
            String defaultModel,
            List<String> enabledModels,
            Integer priorityOrder,
            Boolean enabled,
            String createdByGoogleSub) {
        return new WorkspaceModelProvider(
                null,
                workspaceId,
                providerKind,
                wireProtocol,
                displayName,
                baseUrl,
                apiKey,
                defaultModel,
                enabledModels,
                priorityOrder == null ? DEFAULT_PRIORITY_ORDER : priorityOrder,
                enabled == null || enabled,
                null,
                null,
                null,
                null,
                createdByGoogleSub,
                null,
                null,
                null);
    }

    public static WorkspaceModelProvider restore(
            Long id,
            Long workspaceId,
            ModelProviderKind providerKind,
            ModelWireProtocol wireProtocol,
            String displayName,
            String baseUrl,
            String apiKey,
            String defaultModel,
            List<String> enabledModels,
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
        return new WorkspaceModelProvider(
                requirePositive(id, "id"),
                workspaceId,
                providerKind,
                wireProtocol,
                displayName,
                baseUrl,
                apiKey,
                defaultModel,
                enabledModels,
                priorityOrder,
                enabled,
                lastStatus,
                lastLatencyMs,
                lastCheckedAt,
                lastError,
                createdByGoogleSub,
                createdAt,
                updatedAt,
                deletedAt);
    }

    public void update(
            String displayName,
            ModelWireProtocol wireProtocol,
            String baseUrl,
            String newApiKey,
            String defaultModel,
            List<String> enabledModels,
            Integer priorityOrder,
            Boolean enabled) {
        if (displayName != null) {
            this.displayName = validateLength(displayName, "displayName", MAX_DISPLAY_NAME_LENGTH);
        }
        if (wireProtocol != null) {
            this.wireProtocol = wireProtocol;
        }
        if (baseUrl != null) {
            this.baseUrl = validateBaseUrl(baseUrl);
        }
        if (newApiKey != null && !newApiKey.isBlank()) {
            this.apiKey = newApiKey.trim();
        }
        if (defaultModel != null) {
            this.defaultModel = validateLength(defaultModel, "defaultModel", MAX_MODEL_ID_LENGTH);
        }
        if (enabledModels != null) {
            this.enabledModels = normalizeEnabledModels(enabledModels, this.defaultModel);
        } else if (defaultModel != null && !this.enabledModels.contains(this.defaultModel)) {
            List<String> merged = new ArrayList<>(this.enabledModels);
            merged.addFirst(this.defaultModel);
            this.enabledModels = List.copyOf(merged);
        }
        if (priorityOrder != null) {
            this.priorityOrder = Math.max(0, priorityOrder);
        }
        if (enabled != null) {
            this.enabled = enabled;
        }
    }

    public void recordConnectionCheck(
            ModelProviderConnectionStatus status,
            Long latencyMs,
            LocalDateTime checkedAt,
            String errorMessage) {
        this.lastStatus = status;
        this.lastLatencyMs = latencyMs;
        this.lastCheckedAt = checkedAt;
        this.lastError = normalizeError(errorMessage);
    }

    public void markDeleted(LocalDateTime now) {
        this.deletedAt = now;
        this.enabled = false;
    }

    public static String maskApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return null;
        }
        String trimmed = rawApiKey.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
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

    public List<String> getEnabledModels() {
        return enabledModels;
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

    private static List<String> normalizeEnabledModels(List<String> rawModels, String defaultModel) {
        Set<String> deduplicated = new LinkedHashSet<>();
        if (rawModels != null) {
            for (String item : rawModels) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                String trimmed = validateLength(item, "enabledModel", MAX_MODEL_ID_LENGTH);
                deduplicated.add(trimmed);
                if (deduplicated.size() >= MAX_ENABLED_MODELS) {
                    break;
                }
            }
        }
        if (deduplicated.isEmpty()) {
            deduplicated.add(defaultModel);
        }
        return List.copyOf(deduplicated);
    }

    private static String validateBaseUrl(String baseUrl) {
        String normalized = validateLength(baseUrl, "baseUrl", MAX_BASE_URL_LENGTH);
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("baseUrl must be a valid HTTP or HTTPS URI", exception);
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("baseUrl scheme is required");
        }
        String lowerScheme = scheme.toLowerCase(Locale.ROOT);
        if (!"https".equals(lowerScheme) && !"http".equals(lowerScheme)) {
            throw new IllegalArgumentException("baseUrl must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("baseUrl host is required");
        }
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String validateLength(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length: " + maxLength);
        }
        return trimmed;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeError(String error) {
        String normalized = normalizeNullable(error);
        if (normalized == null || normalized.length() <= 1000) {
            return normalized;
        }
        return normalized.substring(0, 1000);
    }
}
