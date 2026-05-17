package com.aquilaoffice.adapter.in.web.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.ReceiveSlackEventCommand;
import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SlackEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SlackEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReceiveSlackEventUseCase receiveSlackEventUseCase;

    @MockitoBean
    private SlackSignatureVerifier slackSignatureVerifier;

    @Test
    void receiveSlackEventReturnsRawChallenge() throws Exception {
        given(receiveSlackEventUseCase.receiveSlackEvent(any(ReceiveSlackEventCommand.class)))
                .willReturn(new ReceiveSlackEventResult("challenge-token", false, "url verification", null, null, null));

        mockMvc.perform(post("/api/v1/workspaces/101/slack/events")
                        .principal(authenticatedPrincipal("google-1"))
                        .header("X-Slack-Request-Timestamp", "1715841000")
                        .header("X-Slack-Signature", "v0=test-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "url_verification",
                                  "challenge": "challenge-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("challenge-token"));

        ArgumentCaptor<ReceiveSlackEventCommand> commandCaptor =
                ArgumentCaptor.forClass(ReceiveSlackEventCommand.class);
        verify(receiveSlackEventUseCase).receiveSlackEvent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
        assertThat(commandCaptor.getValue().type()).isEqualTo("url_verification");
        assertThat(commandCaptor.getValue().challenge()).isEqualTo("challenge-token");
        verify(slackSignatureVerifier).verifyOrThrow(
                eq("1715841000"),
                eq("v0=test-signature"),
                any(String.class));
    }

    @Test
    void receiveSlackEventReturnsOkEnvelopeForAppMention() throws Exception {
        given(receiveSlackEventUseCase.receiveSlackEvent(any(ReceiveSlackEventCommand.class)))
                .willReturn(new ReceiveSlackEventResult(
                        null,
                        true,
                        null,
                        301L,
                        901L,
                        "T123:C456:1715841000.000100"));

        mockMvc.perform(post("/api/v1/workspaces/101/slack/events")
                        .principal(authenticatedPrincipal("google-1"))
                        .header("X-Slack-Request-Timestamp", "1715841000")
                        .header("X-Slack-Signature", "v0=test-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "event_callback",
                                  "team_id": "T123",
                                  "event_id": "E123",
                                  "event": {
                                    "type": "app_mention",
                                    "channel": "C456",
                                    "user": "U789",
                                    "text": "<@U123ABC> /agent 501 결제 오류 수정",
                                    "ts": "1715841000.000100",
                                    "thread_ts": "1715841000.000100"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processed").value(true))
                .andExpect(jsonPath("$.data.taskId").value(301))
                .andExpect(jsonPath("$.data.chatSessionId").value(901))
                .andExpect(jsonPath("$.data.sourceRef").value("T123:C456:1715841000.000100"));

        ArgumentCaptor<ReceiveSlackEventCommand> commandCaptor =
                ArgumentCaptor.forClass(ReceiveSlackEventCommand.class);
        verify(receiveSlackEventUseCase).receiveSlackEvent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().teamId()).isEqualTo("T123");
        assertThat(commandCaptor.getValue().eventId()).isEqualTo("E123");
        assertThat(commandCaptor.getValue().event().threadTs()).isEqualTo("1715841000.000100");
        verify(slackSignatureVerifier).verifyOrThrow(
                eq("1715841000"),
                eq("v0=test-signature"),
                any(String.class));
    }

    @Test
    void receiveGlobalSlackEventAllowsMappedWorkspaceResolutionWithoutRequesterHeader() throws Exception {
        given(receiveSlackEventUseCase.receiveSlackEvent(any(ReceiveSlackEventCommand.class)))
                .willReturn(new ReceiveSlackEventResult(
                        null,
                        true,
                        null,
                        301L,
                        901L,
                        "T123:C456:1715841000.000100"));

        mockMvc.perform(post("/api/v1/slack/events")
                        .header("X-Slack-Request-Timestamp", "1715841000")
                        .header("X-Slack-Signature", "v0=test-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "event_callback",
                                  "team_id": "T123",
                                  "event_id": "E123",
                                  "event": {
                                    "type": "app_mention",
                                    "channel": "C456",
                                    "user": "U789",
                                    "text": "<@U123ABC> /agent 501 결제 오류 수정",
                                    "ts": "1715841000.000100"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processed").value(true));

        ArgumentCaptor<ReceiveSlackEventCommand> commandCaptor =
                ArgumentCaptor.forClass(ReceiveSlackEventCommand.class);
        verify(receiveSlackEventUseCase).receiveSlackEvent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isNull();
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isNull();
        assertThat(commandCaptor.getValue().teamId()).isEqualTo("T123");
        assertThat(commandCaptor.getValue().event().channel()).isEqualTo("C456");
        verify(slackSignatureVerifier).verifyOrThrow(
                eq("1715841000"),
                eq("v0=test-signature"),
                any(String.class));
    }

    @Test
    void invalidSignatureReturnsBadRequestAndDoesNotCallUseCase() throws Exception {
        willThrow(new IllegalArgumentException("slack request signature mismatch"))
                .given(slackSignatureVerifier)
                .verifyOrThrow(any(String.class), any(String.class), any(String.class));

        mockMvc.perform(post("/api/v1/workspaces/101/slack/events")
                        .principal(authenticatedPrincipal("google-1"))
                        .header("X-Slack-Request-Timestamp", "1715841000")
                        .header("X-Slack-Signature", "v0=invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"event_callback\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(receiveSlackEventUseCase);
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"event_callback\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(receiveSlackEventUseCase);
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
