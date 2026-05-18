package com.aquilaoffice.global.config;

import com.aquilaoffice.application.port.out.AuthenticateWorkspaceGatewayPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.VerifiedGoogleUser;
import com.aquilaoffice.application.port.out.VerifyGoogleIdTokenPort;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationCommand;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationResult;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * smoke profile에서만 Google/Gateway 외부 의존성을 결정적인 fixture로 대체한다.
 */
@Configuration
@Profile("smoke")
@ConditionalOnProperty(prefix = "aquila.smoke-fixture", name = "enabled", havingValue = "true")
public class SmokeFixtureConfig {

    @Bean
    @Primary
    public VerifyGoogleIdTokenPort smokeVerifyGoogleIdTokenPort(
            @Value("${aquila.smoke-fixture.google-id-token:smoke-google-id-token}") String expectedIdToken,
            @Value("${aquila.smoke-fixture.google-sub:smoke-google-sub}") String googleSub,
            @Value("${aquila.smoke-fixture.email:smoke.user@aquila-office.local}") String email,
            @Value("${aquila.smoke-fixture.name:Smoke User}") String name) {
        return idToken -> {
            if (!expectedIdToken.equals(idToken)) {
                throw new IllegalArgumentException("invalid smoke google id token");
            }
            return new VerifiedGoogleUser(googleSub, email, name);
        };
    }

    @Bean
    @Primary
    public SmokeGatewayFixtureAdapter smokeGatewayFixtureAdapter() {
        return new SmokeGatewayFixtureAdapter();
    }

    public static class SmokeGatewayFixtureAdapter
            implements AuthenticateWorkspaceGatewayPort, CreateWorkspaceAgentGatewayPort, RunWorkspaceAgentPort {

        @Override
        public WorkspaceGatewayAuthenticationResult authenticate(WorkspaceGatewayAuthenticationCommand command) {
            return new WorkspaceGatewayAuthenticationResult(
                    GatewayConnectionStatus.CONNECTED,
                    true,
                    command.gatewayUrl(),
                    "smoke-gateway",
                    "Smoke Gateway",
                    "Smoke gateway connected",
                    1,
                    null);
        }

        @Override
        public CreateWorkspaceAgentGatewayResult create(CreateWorkspaceAgentGatewayCommand command) {
            return new CreateWorkspaceAgentGatewayResult(
                    true,
                    "smoke-agent-" + command.workspaceId(),
                    null,
                    command.skillFiles().stream()
                            .map(skillFile -> new CreateWorkspaceAgentGatewayResult.SkillFileResult(
                                    skillFile.fileName(),
                                    true,
                                    null))
                            .toList());
        }

        @Override
        public WorkspaceAgentRunResult run(WorkspaceAgentRunCommand command) {
            return new WorkspaceAgentRunResult(
                    true,
                    "Smoke task completed: " + command.prompt(),
                    null);
        }
    }
}
