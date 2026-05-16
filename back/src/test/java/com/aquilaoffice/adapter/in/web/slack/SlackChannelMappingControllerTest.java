package com.aquilaoffice.adapter.in.web.slack;

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

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingCommand;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingUseCase;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsQuery;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsUseCase;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
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
 * Slack channel mapping web adapter가 HTTP 요청을 command/query로 변환하는지 검증한다.
 */
@WebMvcTest(SlackChannelMappingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SlackChannelMappingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase;

    @MockitoBean
    private ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase;

    @Test
    void createSlackChannelMappingReturnsCreatedEnvelope() throws Exception {
        given(createSlackChannelMappingUseCase.createSlackChannelMapping(any(CreateSlackChannelMappingCommand.class)))
                .willReturn(result());

        mockMvc.perform(post("/api/v1/workspaces/101/slack/channel-mappings")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teamId": " T123 ",
                                  "channelId": " C456 ",
                                  "channelName": " dev-alerts "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "/api/v1/workspaces/101/slack/channel-mappings/501"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mappingId").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.teamId").value("T123"))
                .andExpect(jsonPath("$.data.channelId").value("C456"))
                .andExpect(jsonPath("$.data.channelName").value("dev-alerts"))
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
    }

    @Test
    void listSlackChannelMappingsReturnsOkEnvelope() throws Exception {
        given(listSlackChannelMappingsUseCase.listSlackChannelMappings(any(ListSlackChannelMappingsQuery.class)))
                .willReturn(List.of(listResult()));

        mockMvc.perform(get("/api/v1/workspaces/101/slack/channel-mappings")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].mappingId").value(501))
                .andExpect(jsonPath("$.data[0].teamId").value("T123"))
                .andExpect(jsonPath("$.data[0].channelId").value("C456"))
                .andExpect(jsonPath("$.data[0].active").value(true));

        ArgumentCaptor<ListSlackChannelMappingsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListSlackChannelMappingsQuery.class);
        verify(listSlackChannelMappingsUseCase).listSlackChannelMappings(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void missingHeaderReturnsBadRequestAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/slack/channel-mappings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createSlackChannelMappingUseCase, listSlackChannelMappingsUseCase);
    }

    @Test
    void blankCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/slack/channel-mappings")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":\" \",\"channelId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createSlackChannelMappingUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(listSlackChannelMappingsUseCase.listSlackChannelMappings(any(ListSlackChannelMappingsQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/slack/channel-mappings")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    private static CreateSlackChannelMappingResult result() {
        return new CreateSlackChannelMappingResult(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
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
                "google-1",
                true);
    }
}
