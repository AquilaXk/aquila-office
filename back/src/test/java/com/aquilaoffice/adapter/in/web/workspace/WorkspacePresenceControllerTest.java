package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamCommand;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamResult;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionUseCase;
import com.aquilaoffice.application.service.WorkspacePresenceEventSink;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 워크스페이스 presence web adapter가 위치 갱신 요청을 command로 변환하는지 검증한다.
 */
@WebMvcTest(WorkspacePresenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspacePresenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UpdateWorkspacePresencePositionUseCase updateWorkspacePresencePositionUseCase;

    @MockitoBean
    private OpenWorkspacePresenceStreamUseCase openWorkspacePresenceStreamUseCase;

    @MockitoBean
    private CloseWorkspacePresenceStreamUseCase closeWorkspacePresenceStreamUseCase;

    @Test
    void openStreamReturnsSseEmitterAndPassesHeaderCommand() throws Exception {
        given(openWorkspacePresenceStreamUseCase.openStream(
                        any(OpenWorkspacePresenceStreamCommand.class),
                        any(WorkspacePresenceEventSink.class)))
                .willReturn(new OpenWorkspacePresenceStreamResult(101L, 2L));

        MvcResult result = mockMvc.perform(get("/api/v1/workspaces/101/presence/stream")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-2 "))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        ArgumentCaptor<OpenWorkspacePresenceStreamCommand> commandCaptor =
                ArgumentCaptor.forClass(OpenWorkspacePresenceStreamCommand.class);
        verify(openWorkspacePresenceStreamUseCase)
                .openStream(commandCaptor.capture(), any(WorkspacePresenceEventSink.class));
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().memberGoogleSub()).isEqualTo("google-2");
    }

    @Test
    void openStreamAcceptsTokenQueryParameter() throws Exception {
        given(openWorkspacePresenceStreamUseCase.openStream(
                        any(OpenWorkspacePresenceStreamCommand.class),
                        any(WorkspacePresenceEventSink.class)))
                .willReturn(new OpenWorkspacePresenceStreamResult(101L, 2L));

        mockMvc.perform(get("/api/v1/workspaces/101/presence/stream")
                        .param("token", " google-2 "))
                .andExpect(request().asyncStarted());

        ArgumentCaptor<OpenWorkspacePresenceStreamCommand> commandCaptor =
                ArgumentCaptor.forClass(OpenWorkspacePresenceStreamCommand.class);
        verify(openWorkspacePresenceStreamUseCase)
                .openStream(commandCaptor.capture(), any(WorkspacePresenceEventSink.class));
        assertThat(commandCaptor.getValue().memberGoogleSub()).isEqualTo("google-2");
    }

    @Test
    void openStreamAcceptsAccessTokenQueryParameter() throws Exception {
        given(openWorkspacePresenceStreamUseCase.openStream(
                        any(OpenWorkspacePresenceStreamCommand.class),
                        any(WorkspacePresenceEventSink.class)))
                .willReturn(new OpenWorkspacePresenceStreamResult(101L, 2L));

        mockMvc.perform(get("/api/v1/workspaces/101/presence/stream")
                        .param("accessToken", " google-2 "))
                .andExpect(request().asyncStarted());

        ArgumentCaptor<OpenWorkspacePresenceStreamCommand> commandCaptor =
                ArgumentCaptor.forClass(OpenWorkspacePresenceStreamCommand.class);
        verify(openWorkspacePresenceStreamUseCase)
                .openStream(commandCaptor.capture(), any(WorkspacePresenceEventSink.class));
        assertThat(commandCaptor.getValue().memberGoogleSub()).isEqualTo("google-2");
    }

    @Test
    void openStreamRejectsMissingIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/presence/stream"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(openWorkspacePresenceStreamUseCase);
    }

    @Test
    void updateMyPositionReturnsOkEnvelopeAndPassesCommand() throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/101/presence/me/position")
                        .principal(authenticatedPrincipal("google-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": {
                                    "left": " 18px ",
                                    "top": " 24px "
                                  },
                                  "status": " reviewing "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<UpdateWorkspacePresencePositionCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateWorkspacePresencePositionCommand.class);
        verify(updateWorkspacePresencePositionUseCase).updatePosition(commandCaptor.capture());
        UpdateWorkspacePresencePositionCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.memberGoogleSub()).isEqualTo("google-2");
        assertThat(command.position().left()).isEqualTo("18px");
        assertThat(command.position().top()).isEqualTo("24px");
        assertThat(command.status()).isEqualTo("reviewing");
    }

    @Test
    void updateMyPositionRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/101/presence/me/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": {
                                    "left": "18px",
                                    "top": "24px"
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(updateWorkspacePresencePositionUseCase);
    }

    @Test
    void updateMyPositionRejectsMissingPosition() throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/101/presence/me/position")
                        .principal(authenticatedPrincipal("google-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"online\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(updateWorkspacePresencePositionUseCase);
    }

    @Test
    void updateMyPositionRejectsBlankPositionText() throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/101/presence/me/position")
                        .principal(authenticatedPrincipal("google-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": {
                                    "left": " ",
                                    "top": "24px"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(updateWorkspacePresencePositionUseCase);
    }

    @Test
    void updateMyPositionReturnsNotFoundWhenMembershipIsMissing() throws Exception {
        willThrow(new IllegalArgumentException("workspace not found"))
                .given(updateWorkspacePresencePositionUseCase)
                .updatePosition(any(UpdateWorkspacePresencePositionCommand.class));

        mockMvc.perform(patch("/api/v1/workspaces/101/presence/me/position")
                        .principal(authenticatedPrincipal("google-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": {
                                    "left": "18px",
                                    "top": "24px"
                                  },
                                  "status": "online"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
