package com.aquilaoffice.domain.gateway;

import java.time.LocalDateTime;

/**
 * 워크스페이스에 연결할 gateway URL/token 정보를 관리하는 순수 도메인 모델이다.
 */
public class WorkspaceGatewayBinding {

    private static final int MIN_TOKEN_LENGTH_FOR_MASKING = 8;
    private static final int VISIBLE_PREFIX_LENGTH = 4;
    private static final int VISIBLE_SUFFIX_LENGTH = 4;
    private static final String MASK_STRING = "****";

    private final Long id;
    private final Long workspaceId;
    private final String gatewayUrl;
    private final String token;
    private final String createdByGoogleSub;
    private final String deviceId;
    private final String deviceName;
    private final LocalDateTime authenticatedAt;
    private final GatewayConnectionStatus lastStatus;
    private final LocalDateTime lastCheckedAt;
    private final String lastError;

    private WorkspaceGatewayBinding(
            Long id,
            Long workspaceId,
            String gatewayUrl,
            String token,
            String createdByGoogleSub,
            String deviceId,
            String deviceName,
            LocalDateTime authenticatedAt,
            GatewayConnectionStatus lastStatus,
            LocalDateTime lastCheckedAt,
            String lastError) {
        this.id = id;
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.gatewayUrl = requireHttpUrl(gatewayUrl);
        this.token = requireNotBlank(token, "token");
        this.createdByGoogleSub = requireNotBlank(createdByGoogleSub, "createdByGoogleSub");
        this.deviceId = nullableTrim(deviceId);
        this.deviceName = nullableTrim(deviceName);
        this.authenticatedAt = authenticatedAt;
        this.lastStatus = lastStatus;
        this.lastCheckedAt = lastCheckedAt;
        this.lastError = nullableTrim(lastError);
    }

    public static WorkspaceGatewayBinding create(
            Long workspaceId,
            String gatewayUrl,
            String token,
            String createdByGoogleSub) {
        return new WorkspaceGatewayBinding(
                null,
                workspaceId,
                gatewayUrl,
                token,
                createdByGoogleSub,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static WorkspaceGatewayBinding createAuthenticated(
            Long workspaceId,
            String gatewayUrl,
            String token,
            String createdByGoogleSub,
            String deviceId,
            String deviceName,
            LocalDateTime authenticatedAt,
            GatewayConnectionStatus lastStatus,
            LocalDateTime lastCheckedAt,
            String lastError) {
        return new WorkspaceGatewayBinding(
                null,
                workspaceId,
                gatewayUrl,
                token,
                createdByGoogleSub,
                requireNotBlank(deviceId, "deviceId"),
                requireNotBlank(deviceName, "deviceName"),
                requireNotNull(authenticatedAt, "authenticatedAt"),
                requireNotNull(lastStatus, "lastStatus"),
                requireNotNull(lastCheckedAt, "lastCheckedAt"),
                lastError);
    }

    public static WorkspaceGatewayBinding restore(
            Long id,
            Long workspaceId,
            String gatewayUrl,
            String token,
            String createdByGoogleSub) {
        return restore(id, workspaceId, gatewayUrl, token, createdByGoogleSub, null, null, null, null, null, null);
    }

    public static WorkspaceGatewayBinding restore(
            Long id,
            Long workspaceId,
            String gatewayUrl,
            String token,
            String createdByGoogleSub,
            String deviceId,
            String deviceName,
            LocalDateTime authenticatedAt,
            GatewayConnectionStatus lastStatus,
            LocalDateTime lastCheckedAt,
            String lastError) {
        return new WorkspaceGatewayBinding(
                id,
                workspaceId,
                gatewayUrl,
                token,
                createdByGoogleSub,
                deviceId,
                deviceName,
                authenticatedAt,
                lastStatus,
                lastCheckedAt,
                lastError);
    }

    public String maskedToken() {
        if (token.length() <= MIN_TOKEN_LENGTH_FOR_MASKING) {
            return MASK_STRING;
        }
        return token.substring(0, VISIBLE_PREFIX_LENGTH)
                + MASK_STRING
                + token.substring(token.length() - VISIBLE_SUFFIX_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public String getToken() {
        return token;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public LocalDateTime getAuthenticatedAt() {
        return authenticatedAt;
    }

    public GatewayConnectionStatus getLastStatus() {
        return lastStatus;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public String getLastError() {
        return lastError;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireHttpUrl(String gatewayUrl) {
        String normalizedGatewayUrl = requireNotBlank(gatewayUrl, "gatewayUrl");
        if (!normalizedGatewayUrl.startsWith("http://") && !normalizedGatewayUrl.startsWith("https://")) {
            throw new IllegalArgumentException("gatewayUrl must start with http:// or https://");
        }
        return normalizedGatewayUrl;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static <T> T requireNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static String nullableTrim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
