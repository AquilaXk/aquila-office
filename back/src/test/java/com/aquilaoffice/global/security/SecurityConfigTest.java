package com.aquilaoffice.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.adapter.out.auth.JwtAuthTokenIssuer;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SavedGoogleLoginMember;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.config.properties.AuthTokenProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보안 설정이 공개 엔드포인트, 인증 필요 API, CORS preflight 계약을 지키는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IssueAuthTokenPort issueAuthTokenPort;

    @Autowired
    private SaveGoogleLoginMemberPort saveGoogleLoginMemberPort;

    @Autowired
    private AuthTokenProperties authTokenProperties;

    @Test
    void healthEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerAccessTokenAuthenticatesProtectedApi() throws Exception {
        SavedGoogleLoginMember member = saveGoogleLoginMemberPort.saveOrUpdate(
                "google-1", "user@example.com", "User", MemberRole.USER);
        String accessToken = issueAuthTokenPort.issue(member.memberId(), member.email(), member.role()).accessToken();

        mockMvc.perform(get("/api/private")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-Test-Google-Sub", "google-1"));
    }

    @Test
    void bearerAccessTokenForMissingMemberReturnsUnauthorized() throws Exception {
        String accessToken = issueAuthTokenPort.issue(999L, "missing@example.com", MemberRole.USER).accessToken();

        mockMvc.perform(get("/api/private")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredBearerAccessTokenReturnsUnauthorized() throws Exception {
        JwtAuthTokenIssuer expiredTokenIssuer = new JwtAuthTokenIssuer(
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                authTokenProperties);
        String accessToken = expiredTokenIssuer.issue(999L, "expired@example.com", MemberRole.USER).accessToken();

        mockMvc.perform(get("/api/private")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLoginEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightAllowsLocalFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProtectedEndpointConfig {

        @Bean
        ProtectedEndpointController protectedEndpointController() {
            return new ProtectedEndpointController();
        }
    }

    @RestController
    static class ProtectedEndpointController {

        @GetMapping("/api/private")
        ResponseEntity<Void> privateEndpoint(@AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
            return ResponseEntity.noContent()
                    .header("X-Test-Google-Sub", authenticatedMember.googleSub())
                    .build();
        }
    }
}
