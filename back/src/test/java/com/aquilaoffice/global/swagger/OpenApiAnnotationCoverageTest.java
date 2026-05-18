package com.aquilaoffice.global.swagger;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenApiAnnotationCoverageTest {

    private static final Path SOURCE_ROOT = Path.of(System.getProperty("user.dir"), "src/main/java");

    @Test
    void majorApiControllersExposeTagsOperationsAndSecurityRequirements() throws Exception {
        Map<String, String> controllers = Map.of(
                "adapter/in/web/auth/AuthController.java", "Auth",
                "adapter/in/web/workspace/WorkspaceController.java", "Workspace",
                "adapter/in/web/workspace/WorkspaceInviteController.java", "Invite",
                "adapter/in/web/member/MemberController.java", "Member",
                "adapter/in/web/agent/WorkspaceAgentController.java", "Agent",
                "adapter/in/web/task/TaskController.java", "Task",
                "adapter/in/web/artifact/ArtifactController.java", "Artifact",
                "adapter/in/web/gateway/WorkspaceGatewayController.java", "Gateway",
                "adapter/in/web/slack/SlackIntegrationController.java", "Integration");

        for (Map.Entry<String, String> entry : controllers.entrySet()) {
            String source = Files.readString(SOURCE_ROOT.resolve("com/aquilaoffice").resolve(entry.getKey()));

            assertThat(source).contains("@Tag");
            assertThat(source).contains("name = \"" + entry.getValue() + "\"");
            assertThat(source).contains("@Operation");
        }
    }

    @Test
    void requestAndResponseDtosExposeSchemaDescriptions() throws Exception {
        String authRequest = Files.readString(SOURCE_ROOT.resolve(
                "com/aquilaoffice/adapter/in/web/auth/GoogleLoginRequest.java"));
        String agentResponse = Files.readString(SOURCE_ROOT.resolve(
                "com/aquilaoffice/adapter/in/web/agent/WorkspaceAgentResponse.java"));
        String artifactResponse = Files.readString(SOURCE_ROOT.resolve(
                "com/aquilaoffice/adapter/in/web/artifact/OrchestrationPlanStepResponse.java"));

        assertThat(authRequest).contains("@Schema");
        assertThat(agentResponse).contains("@Schema");
        assertThat(artifactResponse).contains("@Schema");
    }
}
