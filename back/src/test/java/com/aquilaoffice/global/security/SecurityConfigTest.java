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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보안 설정이 공개 엔드포인트, 인증 필요 API, CORS preflight 계약을 지키는지 검증한다.
 */
@SpringBootTest(properties = "aquila.slack.signing-secret=test-slack-signing-secret")
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

    @Autowired
    private PresenceStreamTicketService presenceStreamTicketService;

    @Test
    void healthEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/health")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.ignored"))
                .andExpect(status().isOk());
    }

    @Test
    void publicInviteLookupPermitsAnonymousAndReturnsNotFoundForMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/invites/missing-invite-token"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/workspace-invites/missing-invite-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inviteAcceptAndWorkspaceInviteManagementRemainProtected() throws Exception {
        mockMvc.perform(post("/api/v1/invites/missing-invite-token/accept"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/workspace-invites/missing-invite-token/accept"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/workspaces/101/invites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void globalSlackEventsEndpointPermitsAnonymousAndRejectsUnsignedPayloadWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"event_callback\"}"))
                .andExpect(status().isBadRequest());
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
                .andExpect(header().string("X-Test-Google-Sub", "google-1"))
                .andExpect(header().string("X-Test-Role", "USER"))
                .andExpect(header().string("X-Test-Authority", "ROLE_USER"));
    }

    @Test
    void bearerAccessTokenWithGoogleSubClaimAuthenticatesProtectedApi() throws Exception {
        SavedGoogleLoginMember member = saveGoogleLoginMemberPort.saveOrUpdate(
                "google-sub-claim", "subclaim@example.com", "Sub User", MemberRole.ADMIN);
        String accessToken = issueAuthTokenPort.issue(
                member.memberId(),
                member.googleSub(),
                member.email(),
                member.role()).accessToken();

        mockMvc.perform(get("/api/private")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-Test-Google-Sub", "google-sub-claim"))
                .andExpect(header().string("X-Test-Role", "ADMIN"))
                .andExpect(header().string("X-Test-Authority", "ROLE_ADMIN"));
    }

    @Test
    void bearerAccessTokenUsesCurrentMemberRoleForAuthorities() throws Exception {
        SavedGoogleLoginMember member = saveGoogleLoginMemberPort.saveOrUpdate(
                "google-role", "role-current@example.com", "Role Current", MemberRole.USER);
        String staleAdminAccessToken = issueAuthTokenPort
                .issue(member.memberId(), member.email(), MemberRole.ADMIN)
                .accessToken();

        mockMvc.perform(get("/api/private")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staleAdminAccessToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-Test-Role", "USER"))
                .andExpect(header().string("X-Test-Authority", "ROLE_USER"));
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

    @Test
    void metricsAndDocsRequireAuthenticationByDefaultAndPresenceStreamRejectsRawSub() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/workspaces/9999/presence/stream")
                        .param("token", "google-1"))
                .andExpect(status().isUnauthorized());

        SavedGoogleLoginMember member = saveGoogleLoginMemberPort.saveOrUpdate(
                "google-ticket-user", "ticket@example.com", "Ticket User", MemberRole.USER);
        String accessToken = issueAuthTokenPort.issue(member.memberId(), member.email(), member.role()).accessToken();

        mockMvc.perform(get("/v3/api-docs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        String ticket = presenceStreamTicketService.issueTicket(9999L, member.googleSub()).ticket();
        mockMvc.perform(get("/api/v1/workspaces/9999/presence/stream")
                        .param("ticket", ticket))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/workspaces/9999/presence/stream")
                        .param("ticket", ticket))
                .andExpect(status().isUnauthorized());
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
        ResponseEntity<Void> privateEndpoint(
                @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                Authentication authentication) {
            return ResponseEntity.noContent()
                    .header("X-Test-Google-Sub", authenticatedMember.googleSub())
                    .header("X-Test-Role", authenticatedMember.role().name())
                    .header("X-Test-Authority", authentication.getAuthorities().iterator().next().getAuthority())
                    .build();
        }
    }
}
