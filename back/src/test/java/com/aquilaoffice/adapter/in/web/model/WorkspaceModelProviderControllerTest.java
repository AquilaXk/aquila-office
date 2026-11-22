package com.aquilaoffice.adapter.in.web.model;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.GetModelCatalogUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceModelProvidersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceModelTelemetryUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.WorkspaceModelConnectionTestResult;
import com.aquilaoffice.application.port.in.WorkspaceModelProviderResult;
import com.aquilaoffice.application.port.out.WorkspaceModelTelemetryEntry;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.model.ModelCatalogModel;
import com.aquilaoffice.domain.model.ModelCatalogPreset;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelProviderTier;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import com.aquilaoffice.global.security.AuthenticatedMember;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ModelCatalogController.class, WorkspaceModelProviderController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspaceModelProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetModelCatalogUseCase getModelCatalogUseCase;

    @MockitoBean
    private ListWorkspaceModelProvidersUseCase listWorkspaceModelProvidersUseCase;

    @MockitoBean
    private ListWorkspaceModelTelemetryUseCase listWorkspaceModelTelemetryUseCase;

    @MockitoBean
    private CreateWorkspaceModelProviderUseCase createWorkspaceModelProviderUseCase;

    @MockitoBean
    private UpdateWorkspaceModelProviderUseCase updateWorkspaceModelProviderUseCase;

    @MockitoBean
    private DeleteWorkspaceModelProviderUseCase deleteWorkspaceModelProviderUseCase;

    @MockitoBean
    private TestWorkspaceModelProviderUseCase testWorkspaceModelProviderUseCase;

    @Test
    void modelCatalogReturnsPresetsWhenAuthenticatedAndRejectsAnonymous() throws Exception {
        given(getModelCatalogUseCase.getCatalog()).willReturn(List.of(new ModelCatalogPreset(
                ModelProviderKind.GOOGLE,
                "Google Gemini API",
                "https://example.invalid",
                ModelWireProtocol.GOOGLE_GEMINI,
                ModelProviderTier.FREE,
                true,
                List.of(new ModelCatalogModel("gemini-2.5-flash", "Gemini 2.5 Flash", 1048576, 65536, true, true, true)))));

        mockMvc.perform(get("/api/v1/model-catalog"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/model-catalog")
                        .principal(principal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.presets[0].providerKind").value("GOOGLE"))
                .andExpect(jsonPath("$.data.presets[0].models[0].id").value("gemini-2.5-flash"));
    }

    @Test
    void providerCrudAndConnectionTestFollowSharedRestContract() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 11, 22, 1, 0);
        WorkspaceModelProviderResult result = new WorkspaceModelProviderResult(
                9L, 101L, ModelProviderKind.OPENAI, ModelWireProtocol.OPENAI_RESPONSES,
                "OpenAI", "https://api.openai.com/v1", true, "sk-p...1234",
                "gpt-5.4", List.of("gpt-5.4"), 1, true,
                ModelProviderConnectionStatus.CONNECTED, 45L, now, null, now, now);

        given(listWorkspaceModelProvidersUseCase.list(101L, "google-1")).willReturn(List.of(result));
        given(createWorkspaceModelProviderUseCase.create(any())).willReturn(result);
        given(updateWorkspaceModelProviderUseCase.update(any())).willReturn(result);
        given(testWorkspaceModelProviderUseCase.testConnection(any())).willReturn(
                new WorkspaceModelConnectionTestResult(
                        true, ModelProviderConnectionStatus.CONNECTED, 32L,
                        List.of("gpt-5.4"), "gpt-5.4", "OK", 10, 1, "Connected"));

        mockMvc.perform(get("/api/v1/workspaces/101/model-providers")
                        .principal(principal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].providerId").value(9))
                .andExpect(jsonPath("$.data[0].hasApiKey").value(true))
                .andExpect(jsonPath("$.data[0].maskedApiKey").value("sk-p...1234"));

        mockMvc.perform(post("/api/v1/workspaces/101/model-providers")
                        .principal(principal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerKind": "OPENAI",
                                  "wireProtocol": "OPENAI_RESPONSES",
                                  "displayName": "OpenAI",
                                  "baseUrl": "https://api.openai.com/v1",
                                  "apiKey": "sk-proj-1234",
                                  "defaultModel": "gpt-5.4"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.providerId").value(9));

        mockMvc.perform(patch("/api/v1/workspaces/101/model-providers/9")
                        .principal(principal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "OpenAI Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerId").value(9));

        mockMvc.perform(post("/api/v1/workspaces/101/model-providers/connection-test")
                        .principal(principal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 9,
                                  "mode": "completion"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.replyPreview").value("OK"));

        mockMvc.perform(delete("/api/v1/workspaces/101/model-providers/9")
                        .principal(principal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(deleteWorkspaceModelProviderUseCase).delete(101L, 9L, "google-1");

        given(listWorkspaceModelTelemetryUseCase.listTelemetry(101L, "google-1")).willReturn(List.of(
                new WorkspaceModelTelemetryEntry("gpt-4.1", 4L, 3L, 1L, 300L, 200L, 480.0)));
        mockMvc.perform(get("/api/v1/workspaces/101/model-providers/telemetry")
                        .principal(principal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modelName").value("gpt-4.1"))
                .andExpect(jsonPath("$.data[0].executionCount").value(4))
                .andExpect(jsonPath("$.data[0].totalTokens").value(500));
    }

    private static UsernamePasswordAuthenticationToken principal(String googleSub) {
        AuthenticatedMember member = new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
