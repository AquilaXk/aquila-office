package com.aquilaoffice.global.swagger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SavedGoogleLoginMember;
import com.aquilaoffice.domain.member.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OpenAPI 문서와 Swagger UI가 기본 비공개 정책에서 인증 사용자에게만 열리는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IssueAuthTokenPort issueAuthTokenPort;

    @Autowired
    private SaveGoogleLoginMemberPort saveGoogleLoginMemberPort;

    @Test
    void apiDocsRequireAuthByDefaultAndServeOpenApiWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());

        SavedGoogleLoginMember member = saveGoogleLoginMemberPort.saveOrUpdate(
                "openapi-reader", "openapi@example.com", "OpenAPI Reader", MemberRole.USER);
        String accessToken = issueAuthTokenPort.issue(member.memberId(), member.email(), member.role()).accessToken();

        mockMvc.perform(get("/v3/api-docs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Aquila Office API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void swaggerUiRequiresAuthenticationByDefault() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isUnauthorized());
    }
}
