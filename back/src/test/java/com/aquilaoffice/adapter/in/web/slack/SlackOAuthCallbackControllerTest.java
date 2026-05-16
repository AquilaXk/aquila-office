package com.aquilaoffice.adapter.in.web.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackCommand;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackResult;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackUseCase;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slack OAuth callback web adapter가 query를 application command로 변환하는지 검증한다.
 */
@WebMvcTest(SlackOAuthCallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "aquila.slack.redirect-uri=http://localhost:8080/api/v1/slack/oauth/callback")
class SlackOAuthCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompleteSlackOAuthCallbackUseCase completeSlackOAuthCallbackUseCase;

    @Test
    void completeSlackOAuthCallbackReturnsOkEnvelope() throws Exception {
        given(completeSlackOAuthCallbackUseCase.completeSlackOAuthCallback(
                any(CompleteSlackOAuthCallbackCommand.class)))
                .willReturn(result());

        mockMvc.perform(get("/api/v1/slack/oauth/callback")
                        .param("code", " code-789 ")
                        .param("state", "workspaceId:101:requester:google-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.slackTeamId").value("T123"))
                .andExpect(jsonPath("$.data.slackChannelId").value("C456"))
                .andExpect(jsonPath("$.data.channelName").value("dev-alerts"))
                .andExpect(jsonPath("$.data.maskedBotToken").value("xoxb****cret"))
                .andExpect(jsonPath("$.data.botToken").doesNotExist())
                .andExpect(jsonPath("$.data.active").value(true));

        ArgumentCaptor<CompleteSlackOAuthCallbackCommand> commandCaptor =
                ArgumentCaptor.forClass(CompleteSlackOAuthCallbackCommand.class);
        verify(completeSlackOAuthCallbackUseCase).completeSlackOAuthCallback(commandCaptor.capture());
        CompleteSlackOAuthCallbackCommand command = commandCaptor.getValue();
        assertThat(command.code()).isEqualTo("code-789");
        assertThat(command.state()).isEqualTo("workspaceId:101:requester:google-1");
        assertThat(command.redirectUri()).isEqualTo("http://localhost:8080/api/v1/slack/oauth/callback");
    }

    @Test
    void missingCodeReturnsBadRequestAndDoesNotCallUseCase() throws Exception {
        mockMvc.perform(get("/api/v1/slack/oauth/callback")
                        .param("state", "workspaceId:101:requester:google-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(completeSlackOAuthCallbackUseCase);
    }

    @Test
    void tokenExchangeFailureReturnsBadRequest() throws Exception {
        given(completeSlackOAuthCallbackUseCase.completeSlackOAuthCallback(
                any(CompleteSlackOAuthCallbackCommand.class)))
                .willThrow(new IllegalStateException("slack oauth token exchange failed: invalid_code"));

        mockMvc.perform(get("/api/v1/slack/oauth/callback")
                        .param("code", "bad-code")
                        .param("state", "workspaceId:101:requester:google-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }

    private static CompleteSlackOAuthCallbackResult result() {
        return new CompleteSlackOAuthCallbackResult(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb****cret",
                "google-1",
                true);
    }
}
