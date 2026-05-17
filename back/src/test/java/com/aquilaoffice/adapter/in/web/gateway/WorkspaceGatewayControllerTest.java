package com.aquilaoffice.adapter.in.web.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusResult;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionCommand;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionResult;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionUseCase;
import com.aquilaoffice.domain.gateway.GatewayBindingStatus;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;
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
 * workspace gateway web adapter가 HTTP 요청을 command/query로 변환하고 token 원문을 숨기는지 검증한다.
 */
@WebMvcTest(WorkspaceGatewayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspaceGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWorkspaceGatewayStatusUseCase getWorkspaceGatewayStatusUseCase;

    @MockitoBean
    private TestWorkspaceGatewayConnectionUseCase testWorkspaceGatewayConnectionUseCase;

    @MockitoBean
    private CreateWorkspaceGatewayBindingUseCase createWorkspaceGatewayBindingUseCase;

    @Test
    void getWorkspaceGatewayStatusReturnsOkEnvelope() throws Exception {
        given(getWorkspaceGatewayStatusUseCase.getWorkspaceGatewayStatus(any(GetWorkspaceGatewayStatusQuery.class)))
                .willReturn(new GetWorkspaceGatewayStatusResult(
                        GatewayBindingStatus.BOUND,
                        true,
                        501L,
                        "EXTERNAL",
                        "https://gateway.example.com",
                        "gate****cdef",
                        null,
                        null,
                        null,
                        null));

        mockMvc.perform(get("/api/v1/workspaces/101/gateway")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.status").value("BOUND"))
                .andExpect(jsonPath("$.data.bound").value(true))
                .andExpect(jsonPath("$.data.bindingId").value(501))
                .andExpect(jsonPath("$.data.mode").value("EXTERNAL"))
                .andExpect(jsonPath("$.data.gatewayUrl").value("https://gateway.example.com"))
                .andExpect(jsonPath("$.data.maskedToken").value("gate****cdef"))
                .andExpect(jsonPath("$.data.token").doesNotExist());

        ArgumentCaptor<GetWorkspaceGatewayStatusQuery> queryCaptor =
                ArgumentCaptor.forClass(GetWorkspaceGatewayStatusQuery.class);
        verify(getWorkspaceGatewayStatusUseCase).getWorkspaceGatewayStatus(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void testWorkspaceGatewayConnectionReturnsOkEnvelope() throws Exception {
        given(testWorkspaceGatewayConnectionUseCase.testWorkspaceGatewayConnection(
                        any(TestWorkspaceGatewayConnectionCommand.class)))
                .willReturn(new TestWorkspaceGatewayConnectionResult(
                        GatewayConnectionStatus.CONNECTED,
                        true,
                        "https://gateway.example.com",
                        "gateway connection accepted",
                        0));

        mockMvc.perform(post("/api/v1/workspaces/101/gateway/connection-test")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gatewayUrl\":\" https://gateway.example.com \",\"token\":\" gate_1234567890abcdef \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONNECTED"))
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.gatewayUrl").value("https://gateway.example.com"))
                .andExpect(jsonPath("$.data.message").value("gateway connection accepted"))
                .andExpect(jsonPath("$.data.agentCount").value(0))
                .andExpect(jsonPath("$.data.token").doesNotExist());

        ArgumentCaptor<TestWorkspaceGatewayConnectionCommand> commandCaptor =
                ArgumentCaptor.forClass(TestWorkspaceGatewayConnectionCommand.class);
        verify(testWorkspaceGatewayConnectionUseCase).testWorkspaceGatewayConnection(commandCaptor.capture());
        TestWorkspaceGatewayConnectionCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(command.token()).isEqualTo("gate_1234567890abcdef");
    }

    @Test
    void createWorkspaceGatewayBindingReturnsCreatedEnvelope() throws Exception {
        given(createWorkspaceGatewayBindingUseCase.createWorkspaceGatewayBinding(
                        any(CreateWorkspaceGatewayBindingCommand.class)))
                .willReturn(new CreateWorkspaceGatewayBindingResult(
                        501L,
                        101L,
                        "EXTERNAL",
                        "https://gateway.example.com",
                        "gate****cdef"));

        mockMvc.perform(post("/api/v1/workspaces/101/gateway/binding")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gatewayUrl": " https://gateway.example.com ",
                                  "token": " gate_1234567890abcdef ",
                                  "validateConnection": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/101/gateway/binding"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.mode").value("EXTERNAL"))
                .andExpect(jsonPath("$.data.gatewayUrl").value("https://gateway.example.com"))
                .andExpect(jsonPath("$.data.maskedToken").value("gate****cdef"))
                .andExpect(jsonPath("$.data.token").doesNotExist());

        ArgumentCaptor<CreateWorkspaceGatewayBindingCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateWorkspaceGatewayBindingCommand.class);
        verify(createWorkspaceGatewayBindingUseCase).createWorkspaceGatewayBinding(commandCaptor.capture());
        CreateWorkspaceGatewayBindingCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(command.token()).isEqualTo("gate_1234567890abcdef");
        assertThat(command.validateConnection()).isTrue();
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/gateway"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(
                getWorkspaceGatewayStatusUseCase,
                testWorkspaceGatewayConnectionUseCase,
                createWorkspaceGatewayBindingUseCase);
    }

    @Test
    void blankCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/gateway/binding")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gatewayUrl\":\" \",\"token\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceGatewayBindingUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(getWorkspaceGatewayStatusUseCase.getWorkspaceGatewayStatus(any(GetWorkspaceGatewayStatusQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/gateway")
                        .principal(authenticatedPrincipal("google-1")))
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
