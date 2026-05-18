package com.aquilaoffice.application.port.in;

public record ReceiveSlackEventCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String type,
        String challenge,
        String teamId,
        String eventId,
        SlackEventPayload event) {

    public record SlackEventPayload(
            String type,
            String channel,
            String user,
            String text,
            String ts,
            String threadTs,
            String subtype,
            String botId) {

        public SlackEventPayload(
                String type,
                String channel,
                String user,
                String text,
                String ts,
                String threadTs) {
            this(type, channel, user, text, ts, threadTs, null, null);
        }
    }
}
