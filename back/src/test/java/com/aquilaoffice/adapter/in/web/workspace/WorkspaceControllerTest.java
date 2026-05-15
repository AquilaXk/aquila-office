package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 워크스페이스 web adapter가 HTTP 요청을 command로 변환하고 공통 응답 계약을 지키는지 검증한다.
 */
@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWorkspaceUseCase createWorkspaceUseCase;

    @MockitoBean
    private ListWorkspacesUseCase listWorkspacesUseCase;

    @Test
    void createWorkspaceReturnsCreatedEnvelope() throws Exception {
        given(createWorkspaceUseCase.createWorkspace(any(CreateWorkspaceCommand.class)))
                .willReturn(new CreateWorkspaceResult(
                        101L,
                        "Team",
                        "Description",
                        "google-1",
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0)));

        mockMvc.perform(post("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" Team \",\"description\":\" Description \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/101"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.name").value("Team"))
                .andExpect(jsonPath("$.data.description").value("Description"))
                .andExpect(jsonPath("$.data.creatorGoogleSub").value("google-1"))
                .andExpect(jsonPath("$.data.myRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.joinedAt", notNullValue()));

        ArgumentCaptor<CreateWorkspaceCommand> commandCaptor = ArgumentCaptor.forClass(CreateWorkspaceCommand.class);
        verify(createWorkspaceUseCase).createWorkspace(commandCaptor.capture());
        CreateWorkspaceCommand command = commandCaptor.getValue();
        // web adapter는 creator 식별자만 정규화하고, name/description 정규화는 domain 계층에 남긴다.
        assertThat(command.name()).isEqualTo(" Team ");
        assertThat(command.description()).isEqualTo(" Description ");
        assertThat(command.creatorGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void createWorkspaceRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"description\":\"Description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceUseCase);
    }

    @Test
    void createWorkspaceRejectsBlankCreatorHeader() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Team\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceUseCase);
    }

    @Test
    void listWorkspacesReturnsOkEnvelope() throws Exception {
        given(listWorkspacesUseCase.listWorkspaces(any(ListWorkspacesQuery.class)))
                .willReturn(List.of(new ListWorkspacesResult(
                        101L,
                        "Team",
                        "Description",
                        "google-1",
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0))));

        mockMvc.perform(get("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].workspaceId").value(101))
                .andExpect(jsonPath("$.data[0].name").value("Team"))
                .andExpect(jsonPath("$.data[0].description").value("Description"))
                .andExpect(jsonPath("$.data[0].creatorGoogleSub").value("google-1"))
                .andExpect(jsonPath("$.data[0].myRole").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].joinedAt", notNullValue()));

        ArgumentCaptor<ListWorkspacesQuery> queryCaptor = ArgumentCaptor.forClass(ListWorkspacesQuery.class);
        verify(listWorkspacesUseCase).listWorkspaces(queryCaptor.capture());
        assertThat(queryCaptor.getValue().memberGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void listWorkspacesRejectsBlankCreatorHeader() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(listWorkspacesUseCase);
    }
}
