package com.aquilaoffice.adapter.in.web.github;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.aquilaoffice.application.port.in.CreateGithubCredentialCommand;
import com.aquilaoffice.application.port.in.CreateGithubCredentialResult;
import com.aquilaoffice.application.port.in.CreateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialCommand;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.ListGithubCredentialsQuery;
import com.aquilaoffice.application.port.in.ListGithubCredentialsResult;
import com.aquilaoffice.application.port.in.ListGithubCredentialsUseCase;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialCommand;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialResult;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialUseCase;
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
 * GitHub 자격 증명 web adapter가 HTTP 요청을 command/query로 변환하고 token 원문을 응답하지 않는지 검증한다.
 */
@WebMvcTest(GithubCredentialController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GithubCredentialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGithubCredentialUseCase createGithubCredentialUseCase;

    @MockitoBean
    private ListGithubCredentialsUseCase listGithubCredentialsUseCase;

    @MockitoBean
    private UpdateGithubCredentialUseCase updateGithubCredentialUseCase;

    @MockitoBean
    private DeleteGithubCredentialUseCase deleteGithubCredentialUseCase;

    @Test
    void listGithubCredentialsReturnsOkEnvelope() throws Exception {
        given(listGithubCredentialsUseCase.listGithubCredentials(any(ListGithubCredentialsQuery.class)))
                .willReturn(List.of(new ListGithubCredentialsResult(
                        301L, 101L, "Main Token", "ghp_****cdef")));

        mockMvc.perform(get("/api/v1/workspaces/101/github/credentials")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].credentialId").value(301))
                .andExpect(jsonPath("$.data[0].workspaceId").value(101))
                .andExpect(jsonPath("$.data[0].displayName").value("Main Token"))
                .andExpect(jsonPath("$.data[0].maskedToken").value("ghp_****cdef"))
                .andExpect(jsonPath("$.data[0].token").doesNotExist());

        ArgumentCaptor<ListGithubCredentialsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListGithubCredentialsQuery.class);
        verify(listGithubCredentialsUseCase).listGithubCredentials(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void createGithubCredentialReturnsCreatedEnvelope() throws Exception {
        given(createGithubCredentialUseCase.createGithubCredential(any(CreateGithubCredentialCommand.class)))
                .willReturn(new CreateGithubCredentialResult(
                        301L, 101L, "Main Token", "ghp_****cdef"));

        mockMvc.perform(post("/api/v1/workspaces/101/github/credentials")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\" Main Token \",\"token\":\" ghp_1234567890abcdef \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "/api/v1/workspaces/101/github/credentials/301"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.credentialId").value(301))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.displayName").value("Main Token"))
                .andExpect(jsonPath("$.data.maskedToken").value("ghp_****cdef"))
                .andExpect(jsonPath("$.data.token").doesNotExist());

        ArgumentCaptor<CreateGithubCredentialCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateGithubCredentialCommand.class);
        verify(createGithubCredentialUseCase).createGithubCredential(commandCaptor.capture());
        CreateGithubCredentialCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.displayName()).isEqualTo("Main Token");
        assertThat(command.token()).isEqualTo("ghp_1234567890abcdef");
    }

    @Test
    void updateGithubCredentialReturnsOkEnvelope() throws Exception {
        given(updateGithubCredentialUseCase.updateGithubCredential(any(UpdateGithubCredentialCommand.class)))
                .willReturn(new UpdateGithubCredentialResult(
                        301L, 101L, "Backup Token", "ghp_****4321"));

        mockMvc.perform(patch("/api/v1/workspaces/101/github/credentials/301")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\" Backup Token \",\"token\":\" ghp_fedcba0987654321 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.credentialId").value(301))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.displayName").value("Backup Token"))
                .andExpect(jsonPath("$.data.maskedToken").value("ghp_****4321"))
                .andExpect(jsonPath("$.data.token").doesNotExist());

        ArgumentCaptor<UpdateGithubCredentialCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateGithubCredentialCommand.class);
        verify(updateGithubCredentialUseCase).updateGithubCredential(commandCaptor.capture());
        UpdateGithubCredentialCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.credentialId()).isEqualTo(301L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.displayName()).isEqualTo("Backup Token");
        assertThat(command.token()).isEqualTo("ghp_fedcba0987654321");
    }

    @Test
    void deleteGithubCredentialReturnsOkEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101/github/credentials/301")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"));

        ArgumentCaptor<DeleteGithubCredentialCommand> commandCaptor =
                ArgumentCaptor.forClass(DeleteGithubCredentialCommand.class);
        verify(deleteGithubCredentialUseCase).deleteGithubCredential(commandCaptor.capture());
        DeleteGithubCredentialCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.credentialId()).isEqualTo(301L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/github/credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(
                createGithubCredentialUseCase,
                listGithubCredentialsUseCase,
                updateGithubCredentialUseCase,
                deleteGithubCredentialUseCase);
    }

    @Test
    void blankCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/github/credentials")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\" \",\"token\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createGithubCredentialUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(listGithubCredentialsUseCase.listGithubCredentials(any(ListGithubCredentialsQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/github/credentials")
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
