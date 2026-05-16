package com.aquilaoffice.domain.slack;

/**
 * Slack team/channel과 workspace 연결을 관리하는 순수 도메인 모델이다.
 */
public class SlackChannelMapping {

    private final Long id;
    private final Long workspaceId;
    private final String teamId;
    private final String channelId;
    private final String channelName;
    private final String createdByGoogleSub;
    private boolean active;

    private SlackChannelMapping(
            Long id,
            Long workspaceId,
            String teamId,
            String channelId,
            String channelName,
            String createdByGoogleSub,
            boolean active) {
        this.id = id;
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.teamId = requireNotBlank(teamId, "teamId");
        this.channelId = requireNotBlank(channelId, "channelId");
        this.channelName = normalizeOptional(channelName);
        this.createdByGoogleSub = requireNotBlank(createdByGoogleSub, "createdByGoogleSub");
        this.active = active;
    }

    public static SlackChannelMapping create(
            Long workspaceId,
            String teamId,
            String channelId,
            String channelName,
            String createdByGoogleSub) {
        return new SlackChannelMapping(
                null,
                workspaceId,
                teamId,
                channelId,
                channelName,
                createdByGoogleSub,
                true);
    }

    public static SlackChannelMapping restore(
            Long id,
            Long workspaceId,
            String teamId,
            String channelId,
            String channelName,
            String createdByGoogleSub,
            boolean active) {
        return new SlackChannelMapping(
                id,
                workspaceId,
                teamId,
                channelId,
                channelName,
                createdByGoogleSub,
                active);
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean hasSameChannel(String teamId, String channelId) {
        return this.teamId.equals(requireNotBlank(teamId, "teamId"))
                && this.channelId.equals(requireNotBlank(channelId, "channelId"));
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public boolean isActive() {
        return active;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
