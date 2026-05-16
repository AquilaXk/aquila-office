package com.aquilaoffice.adapter.in.web.slack;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slack 설치 URL web adapter가 requester header를 검증하고 URL 응답을 반환하는지 검증한다.
 */
@WebMvcTest(SlackInstallController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SlackInstallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlackInstallUrlBuilder slackInstallUrlBuilder;

    @Test
    void getSlackInstallUrlReturnsOkEnvelope() throws Exception {
        given(slackInstallUrlBuilder.build(101L, "google-1"))
                .willReturn("https://slack.com/oauth/v2/authorize?client_id=client-123");

        mockMvc.perform(get("/api/v1/workspaces/101/slack/install")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url")
                        .value("https://slack.com/oauth/v2/authorize?client_id=client-123"));

        ArgumentCaptor<Long> workspaceIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> requesterCaptor = ArgumentCaptor.forClass(String.class);
        verify(slackInstallUrlBuilder).build(workspaceIdCaptor.capture(), requesterCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(workspaceIdCaptor.getValue()).isEqualTo(101L);
        org.assertj.core.api.Assertions.assertThat(requesterCaptor.getValue()).isEqualTo("google-1");
    }

    @Test
    void missingHeaderReturnsBadRequestAndDoesNotCallBuilder() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/slack/install"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(slackInstallUrlBuilder);
    }
}
