package com.aquilaoffice.adapter.in.web.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingCommand;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingUseCase;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationCommand;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationResult;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationUseCase;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsQuery;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsUseCase;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationCommand;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationResult;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationUseCase;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import com.aquilaoffice.global.security.AuthenticatedMember;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slack 수동 연동 web adapter가 화면 계약을 application command/query로 변환하는지 검증한다.
 */
@WebMvcTest(SlackIntegrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SlackIntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase;

    @MockitoBean
    private ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase;

    @MockitoBean
    private UpdateSlackIntegrationUseCase updateSlackIntegrationUseCase;

    @MockitoBean
    private DeleteSlackIntegrationUseCase deleteSlackIntegrationUseCase;

    @Test
    void createSlackIntegrationReturnsCreatedEnvelope() throws Exception {
        given(createSlackChannelMappingUseCase.createSlackChannelMapping(any(CreateSlackChannelMappingCommand.class)))
                .willReturn(result());

        mockMvc.perform(post("/api/v1/workspaces/101/slack/integrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slackTeamId": " T123 ",
                                  "slackChannelId": " C456 ",
                                  "channelName": " dev-alerts ",
                                  "botToken": " xoxb-token-secret "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "/api/v1/workspaces/101/slack/integrations/501"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.slackTeamId").value("T123"))
                .andExpect(jsonPath("$.data.slackChannelId").value("C456"))
                .andExpect(jsonPath("$.data.channelName").value("dev-alerts"))
                .andExpect(jsonPath("$.data.maskedBotToken").value("xoxb****cret"))
                .andExpect(jsonPath("$.data.botToken").doesNotExist())
                .andExpect(jsonPath("$.data.active").value(true));

        ArgumentCaptor<CreateSlackChannelMappingCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateSlackChannelMappingCommand.class);
        verify(createSlackChannelMappingUseCase).createSlackChannelMapping(commandCaptor.capture());
        CreateSlackChannelMappingCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.teamId()).isEqualTo("T123");
        assertThat(command.channelId()).isEqualTo("C456");
        assertThat(command.channelName()).isEqualTo("dev-alerts");
        assertThat(command.botToken()).isEqualTo("xoxb-token-secret");
    }

    @Test
    void listSlackIntegrationsReturnsOkEnvelope() throws Exception {
        given(listSlackChannelMappingsUseCase.listSlackChannelMappings(any(ListSlackChannelMappingsQuery.class)))
                .willReturn(List.of(listResult()));

        mockMvc.perform(get("/api/v1/workspaces/101/slack/integrations")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(501))
                .andExpect(jsonPath("$.data[0].slackTeamId").value("T123"))
                .andExpect(jsonPath("$.data[0].slackChannelId").value("C456"))
                .andExpect(jsonPath("$.data[0].maskedBotToken").value("xoxb****cret"))
                .andExpect(jsonPath("$.data[0].botToken").doesNotExist())
                .andExpect(jsonPath("$.data[0].active").value(true));

        ArgumentCaptor<ListSlackChannelMappingsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListSlackChannelMappingsQuery.class);
        verify(listSlackChannelMappingsUseCase).listSlackChannelMappings(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void updateSlackIntegrationReturnsOkEnvelope() throws Exception {
        given(updateSlackIntegrationUseCase.updateSlackIntegration(any(UpdateSlackIntegrationCommand.class)))
                .willReturn(updateResult());

        mockMvc.perform(patch("/api/v1/workspaces/101/slack/integrations/501")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slackTeamId": " T999 ",
                                  "channelName": " prod-alerts ",
                                  "botToken": " xoxb-new-secret "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.slackTeamId").value("T999"))
                .andExpect(jsonPath("$.data.slackChannelId").value("C456"))
                .andExpect(jsonPath("$.data.channelName").value("prod-alerts"))
                .andExpect(jsonPath("$.data.maskedBotToken").value("xoxb****cret"))
                .andExpect(jsonPath("$.data.botToken").doesNotExist())
                .andExpect(jsonPath("$.data.active").value(true));

        ArgumentCaptor<UpdateSlackIntegrationCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateSlackIntegrationCommand.class);
        verify(updateSlackIntegrationUseCase).updateSlackIntegration(commandCaptor.capture());
        UpdateSlackIntegrationCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.integrationId()).isEqualTo(501L);
        assertThat(command.slackTeamId()).isEqualTo("T999");
        assertThat(command.slackChannelId()).isNull();
        assertThat(command.channelName()).isEqualTo("prod-alerts");
        assertThat(command.botToken()).isEqualTo("xoxb-new-secret");
    }

    @Test
    void deleteSlackIntegrationReturnsOkEnvelope() throws Exception {
        given(deleteSlackIntegrationUseCase.deleteSlackIntegration(any(DeleteSlackIntegrationCommand.class)))
                .willReturn(deleteResult());

        mockMvc.perform(delete("/api/v1/workspaces/101/slack/integrations/501")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.active").value(false));

        ArgumentCaptor<DeleteSlackIntegrationCommand> commandCaptor =
                ArgumentCaptor.forClass(DeleteSlackIntegrationCommand.class);
        verify(deleteSlackIntegrationUseCase).deleteSlackIntegration(commandCaptor.capture());
        DeleteSlackIntegrationCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.integrationId()).isEqualTo(501L);
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/slack/integrations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(
                createSlackChannelMappingUseCase,
                listSlackChannelMappingsUseCase,
                updateSlackIntegrationUseCase,
                deleteSlackIntegrationUseCase);
    }

    @Test
    void blankCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/slack/integrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slackTeamId\":\" \",\"slackChannelId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createSlackChannelMappingUseCase);
    }

    @Test
    void slackIntegrationControllerDoesNotExposeLegacyRequesterHeaderHelper() {
        assertThatThrownBy(
                        () -> SlackIntegrationController.class.getDeclaredMethod("requireRequesterGoogleSub", String.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    private static CreateSlackChannelMappingResult result() {
        return new CreateSlackChannelMappingResult(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb****cret",
                "google-1",
                true);
    }

    private static ListSlackChannelMappingsResult listResult() {
        return new ListSlackChannelMappingsResult(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb****cret",
                "google-1",
                true);
    }

    private static UpdateSlackIntegrationResult updateResult() {
        return new UpdateSlackIntegrationResult(
                501L,
                101L,
                "T999",
                "C456",
                "prod-alerts",
                "xoxb****cret",
                "google-1",
                true);
    }

    private static DeleteSlackIntegrationResult deleteResult() {
        return new DeleteSlackIntegrationResult(
                501L,
                101L,
                false);
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
