package com.aquilaoffice.adapter.in.web.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.ArtifactFileResult;
import com.aquilaoffice.application.port.in.ArtifactNodeResult;
import com.aquilaoffice.application.port.in.ArtifactTreeResult;
import com.aquilaoffice.application.port.in.GetArtifactFileQuery;
import com.aquilaoffice.application.port.in.GetArtifactFileUseCase;
import com.aquilaoffice.application.port.in.GetArtifactTreeQuery;
import com.aquilaoffice.application.port.in.GetArtifactTreeUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactUseCase;
import com.aquilaoffice.application.port.in.OrchestrationPlanArtifactResult;
import com.aquilaoffice.application.port.in.OrchestrationPlanFileResult;
import com.aquilaoffice.application.port.in.OrchestrationPlanStepResult;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArtifactController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ArtifactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetArtifactTreeUseCase getArtifactTreeUseCase;

    @MockitoBean
    private GetArtifactFileUseCase getArtifactFileUseCase;

    @MockitoBean
    private GetOrchestrationPlanArtifactUseCase getOrchestrationPlanArtifactUseCase;

    @Test
    void getArtifactTreeReturnsOkEnvelope() throws Exception {
        given(getArtifactTreeUseCase.getArtifactTree(any(GetArtifactTreeQuery.class)))
                .willReturn(treeResult());

        mockMvc.perform(get("/api/v1/workspaces/101/artifacts/tree")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.rootPath").value("/"))
                .andExpect(jsonPath("$.data.children[0].name").value("reports"))
                .andExpect(jsonPath("$.data.children[0].type").value("DIRECTORY"))
                .andExpect(jsonPath("$.data.children[0].children[0].path").value("reports/result.md"));

        ArgumentCaptor<GetArtifactTreeQuery> queryCaptor = ArgumentCaptor.forClass(GetArtifactTreeQuery.class);
        verify(getArtifactTreeUseCase).getArtifactTree(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void getArtifactFileReturnsOkEnvelope() throws Exception {
        given(getArtifactFileUseCase.getArtifactFile(any(GetArtifactFileQuery.class)))
                .willReturn(fileResult());

        mockMvc.perform(get("/api/v1/workspaces/101/artifacts/files")
                        .queryParam("path", "reports/result.md")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.path").value("reports/result.md"))
                .andExpect(jsonPath("$.data.name").value("result.md"))
                .andExpect(jsonPath("$.data.content").value("# 완료"));

        ArgumentCaptor<GetArtifactFileQuery> queryCaptor = ArgumentCaptor.forClass(GetArtifactFileQuery.class);
        verify(getArtifactFileUseCase).getArtifactFile(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().path()).isEqualTo("reports/result.md");
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void getOrchestrationPlanArtifactReturnsOkEnvelope() throws Exception {
        given(getOrchestrationPlanArtifactUseCase.getOrchestrationPlanArtifact(
                        any(GetOrchestrationPlanArtifactQuery.class)))
                .willReturn(orchestrationPlanResult());

        mockMvc.perform(get("/api/v1/workspaces/101/artifacts/orchestration-plans/701")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.planId").value(701))
                .andExpect(jsonPath("$.data.steps[0].stepKey").value("requirements"))
                .andExpect(jsonPath("$.data.steps[0].files[0].path")
                        .value("plans/701/reports/requirements.md"))
                .andExpect(jsonPath("$.data.steps[0].files[0].exists").value(true));

        ArgumentCaptor<GetOrchestrationPlanArtifactQuery> queryCaptor =
                ArgumentCaptor.forClass(GetOrchestrationPlanArtifactQuery.class);
        verify(getOrchestrationPlanArtifactUseCase).getOrchestrationPlanArtifact(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().planId()).isEqualTo(701L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void missingFilePathReturnsBadRequestAndDoesNotCallUseCase() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/artifacts/files")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(getArtifactFileUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(getArtifactFileUseCase.getArtifactFile(any(GetArtifactFileQuery.class)))
                .willThrow(new IllegalArgumentException("artifact path is outside workspace root"));

        mockMvc.perform(get("/api/v1/workspaces/101/artifacts/files")
                        .queryParam("path", "../secret.txt")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/artifacts/tree"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(
                getArtifactTreeUseCase,
                getArtifactFileUseCase,
                getOrchestrationPlanArtifactUseCase);
    }

    private static ArtifactTreeResult treeResult() {
        return new ArtifactTreeResult(
                101L,
                "/",
                List.of(new ArtifactNodeResult(
                        "reports",
                        "reports",
                        "DIRECTORY",
                        null,
                        null,
                        List.of(new ArtifactNodeResult(
                                "result.md",
                                "reports/result.md",
                                "FILE",
                                "text/markdown",
                                8L,
                                List.of())))));
    }

    private static ArtifactFileResult fileResult() {
        return new ArtifactFileResult(
                101L,
                "reports/result.md",
                "result.md",
                "text/markdown",
                8L,
                "# 완료");
    }

    private static OrchestrationPlanArtifactResult orchestrationPlanResult() {
        return new OrchestrationPlanArtifactResult(
                101L,
                701L,
                List.of(new OrchestrationPlanStepResult(
                        11L,
                        "requirements",
                        1,
                        "요구사항 확인",
                        List.of(new OrchestrationPlanFileResult(
                                "plans/701/reports/requirements.md",
                                "requirements.md",
                                "text/markdown",
                                128L,
                                true)))));
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
