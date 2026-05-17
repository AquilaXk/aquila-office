package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import com.aquilaoffice.global.security.AuthenticatedMember;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 워크스페이스 초대 web adapter가 token 조회 요청을 use case로 변환하고 공통 응답 계약을 지키는지 검증한다.
 */
@WebMvcTest(WorkspaceInviteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspaceInviteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWorkspaceInviteUseCase getWorkspaceInviteUseCase;

    @MockitoBean
    private AcceptWorkspaceInviteUseCase acceptWorkspaceInviteUseCase;

    @Test
    void getWorkspaceInviteReturnsOkEnvelope() throws Exception {
        given(getWorkspaceInviteUseCase.getWorkspaceInvite(any(GetWorkspaceInviteQuery.class)))
                .willReturn(new GetWorkspaceInviteResult(
                        501L,
                        101L,
                        "invitee@example.com",
                        "invite-token",
                        WorkspaceInviteStatus.PENDING,
                        LocalDateTime.of(2026, 5, 15, 10, 0),
                        LocalDateTime.of(2026, 5, 22, 10, 0),
                        null));

        mockMvc.perform(get("/api/v1/workspace-invites/invite-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.inviteId").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.email").value("invitee@example.com"))
                .andExpect(jsonPath("$.data.token").value("invite-token"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.expiresAt", notNullValue()));

        ArgumentCaptor<GetWorkspaceInviteQuery> queryCaptor = ArgumentCaptor.forClass(GetWorkspaceInviteQuery.class);
        verify(getWorkspaceInviteUseCase).getWorkspaceInvite(queryCaptor.capture());
        assertThat(queryCaptor.getValue().token()).isEqualTo("invite-token");
    }

    @Test
    void getWorkspaceInviteReturnsNotFoundEnvelope() throws Exception {
        given(getWorkspaceInviteUseCase.getWorkspaceInvite(any(GetWorkspaceInviteQuery.class)))
                .willThrow(new IllegalArgumentException("workspace invite not found"));

        mockMvc.perform(get("/api/v1/workspace-invites/missing-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void acceptWorkspaceInviteReturnsOkEnvelope() throws Exception {
        LocalDateTime acceptedAt = LocalDateTime.of(2026, 5, 16, 10, 0);
        given(acceptWorkspaceInviteUseCase.acceptWorkspaceInvite(any(AcceptWorkspaceInviteCommand.class)))
                .willReturn(new AcceptWorkspaceInviteResult(
                        501L,
                        101L,
                        "google-1",
                        WorkspaceMemberRole.MEMBER,
                        acceptedAt));

        mockMvc.perform(post("/api/v1/workspace-invites/invite-token/accept")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.inviteId").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.memberGoogleSub").value("google-1"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.acceptedAt", notNullValue()));

        ArgumentCaptor<AcceptWorkspaceInviteCommand> commandCaptor =
                ArgumentCaptor.forClass(AcceptWorkspaceInviteCommand.class);
        verify(acceptWorkspaceInviteUseCase).acceptWorkspaceInvite(commandCaptor.capture());
        AcceptWorkspaceInviteCommand command = commandCaptor.getValue();
        assertThat(command.token()).isEqualTo("invite-token");
        assertThat(command.memberGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void acceptWorkspaceInviteRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(post("/api/v1/workspace-invites/invite-token/accept"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(acceptWorkspaceInviteUseCase);
    }

    @Test
    void acceptWorkspaceInviteReturnsNotFoundEnvelope() throws Exception {
        given(acceptWorkspaceInviteUseCase.acceptWorkspaceInvite(any(AcceptWorkspaceInviteCommand.class)))
                .willThrow(new IllegalArgumentException("workspace invite not found"));

        mockMvc.perform(post("/api/v1/workspace-invites/missing-token/accept")
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
