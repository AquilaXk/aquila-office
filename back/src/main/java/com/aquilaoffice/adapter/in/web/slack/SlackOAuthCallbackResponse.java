package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackResult;

/**
 * Slack OAuth callback HTTP 응답 모델이다.
 */
public record SlackOAuthCallbackResponse(
        Long id,
        Long workspaceId,
        String slackTeamId,
        String slackChannelId,
        String channelName,
        String maskedBotToken,
        String createdByGoogleSub,
        boolean active) {

    public static SlackOAuthCallbackResponse from(CompleteSlackOAuthCallbackResult result) {
        return new SlackOAuthCallbackResponse(
                result.id(),
                result.workspaceId(),
                result.slackTeamId(),
                result.slackChannelId(),
                result.channelName(),
                result.maskedBotToken(),
                result.createdByGoogleSub(),
                result.active());
    }
}
