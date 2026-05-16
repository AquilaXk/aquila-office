package com.aquilaoffice.adapter.out.persistence.slack;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Slack channel mapping을 slack_channel_mappings 테이블에 저장하는 JPA entity다.
 */
@Entity
@Table(
        name = "slack_channel_mappings",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_slack_channel_mappings_team_channel",
                    columnNames = {"team_id", "channel_id"})
        })
public class SlackChannelMappingJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "team_id", nullable = false, length = 120)
    private String teamId;

    @Column(name = "channel_id", nullable = false, length = 120)
    private String channelId;

    @Column(name = "channel_name", length = 120)
    private String channelName;

    @Column(name = "bot_token", nullable = false, columnDefinition = "TEXT")
    private String botToken;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected SlackChannelMappingJpaEntity() {}

    private SlackChannelMappingJpaEntity(
            WorkspaceJpaEntity workspace,
            String teamId,
            String channelId,
            String channelName,
            String botToken,
            String createdByGoogleSub,
            boolean active) {
        this.workspace = requireWorkspaceEntity(workspace);
        this.teamId = teamId;
        this.channelId = channelId;
        this.channelName = channelName;
        this.botToken = botToken;
        this.createdByGoogleSub = createdByGoogleSub;
        this.active = active;
    }

    public static SlackChannelMappingJpaEntity from(
            SlackChannelMapping mapping,
            WorkspaceJpaEntity workspace) {
        return new SlackChannelMappingJpaEntity(
                workspace,
                mapping.getTeamId(),
                mapping.getChannelId(),
                mapping.getChannelName(),
                mapping.getBotToken(),
                mapping.getCreatedByGoogleSub(),
                mapping.isActive());
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
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

    public String getBotToken() {
        return botToken;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public boolean isActive() {
        return active;
    }

    private static WorkspaceJpaEntity requireWorkspaceEntity(WorkspaceJpaEntity workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace entity must not be null");
        }
        return workspace;
    }
}
