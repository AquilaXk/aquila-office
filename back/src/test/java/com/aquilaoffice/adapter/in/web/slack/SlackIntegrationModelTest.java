package com.aquilaoffice.adapter.in.web.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingCommand;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsResult;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SlackIntegrationModelTest {

    @Test
    void createRequestMapsIntegrationFieldsToChannelMappingCommand() {
        CreateSlackIntegrationRequest request = new CreateSlackIntegrationRequest(
                " T123 ",
                " C456 ",
                " dev-alerts ",
                " xoxb-token-secret ");

        CreateSlackChannelMappingCommand command = request.toCommand(101L, " google-1 ");

        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.teamId()).isEqualTo("T123");
        assertThat(command.channelId()).isEqualTo("C456");
        assertThat(command.channelName()).isEqualTo("dev-alerts");
        assertThat(command.botToken()).isEqualTo("xoxb-token-secret");
    }

    @Test
    void responseFromCreateResultUsesIntegrationFieldNames() {
        SlackIntegrationResponse response = SlackIntegrationResponse.from(new CreateSlackChannelMappingResult(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb****cret",
                "google-1",
                true));

        assertThat(response.id()).isEqualTo(501L);
        assertThat(response.workspaceId()).isEqualTo(101L);
        assertThat(response.slackTeamId()).isEqualTo("T123");
        assertThat(response.slackChannelId()).isEqualTo("C456");
        assertThat(response.channelName()).isEqualTo("dev-alerts");
        assertThat(response.maskedBotToken()).isEqualTo("xoxb****cret");
        assertThat(recordComponentNames()).doesNotContain("botToken");
    }

    @Test
    void responseFromListResultUsesIntegrationFieldNames() {
        SlackIntegrationResponse response = SlackIntegrationResponse.from(new ListSlackChannelMappingsResult(
                502L,
                101L,
                "T999",
                "C999",
                null,
                "xoxb****9999",
                "google-2",
                true));

        assertThat(response.id()).isEqualTo(502L);
        assertThat(response.workspaceId()).isEqualTo(101L);
        assertThat(response.slackTeamId()).isEqualTo("T999");
        assertThat(response.slackChannelId()).isEqualTo("C999");
        assertThat(response.channelName()).isNull();
        assertThat(response.maskedBotToken()).isEqualTo("xoxb****9999");
        assertThat(response.active()).isTrue();
    }

    private static java.util.List<String> recordComponentNames() {
        return Arrays.stream(SlackIntegrationResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
    }
}
