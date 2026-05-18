package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.ReceiveSlackEventCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackEventRequest(
        String type,
        String challenge,
        @JsonProperty("team_id") String teamId,
        @JsonProperty("event_id") String eventId,
        SlackEventDetailRequest event) {

    ReceiveSlackEventCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new ReceiveSlackEventCommand(
                workspaceId,
                requesterGoogleSub,
                type,
                challenge,
                teamId,
                eventId,
                event == null ? null : event.toPayload());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SlackEventDetailRequest(
            String type,
            String channel,
            String user,
            String text,
            String ts,
            @JsonProperty("thread_ts") String threadTs,
            String subtype,
            @JsonProperty("bot_id") String botId) {

        ReceiveSlackEventCommand.SlackEventPayload toPayload() {
            return new ReceiveSlackEventCommand.SlackEventPayload(
                    type,
                    channel,
                    user,
                    text,
                    ts,
                    threadTs,
                    subtype,
                    botId);
        }
    }
}
