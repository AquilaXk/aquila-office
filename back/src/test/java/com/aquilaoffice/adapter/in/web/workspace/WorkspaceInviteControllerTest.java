package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.GetWorkspaceInviteQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
}
