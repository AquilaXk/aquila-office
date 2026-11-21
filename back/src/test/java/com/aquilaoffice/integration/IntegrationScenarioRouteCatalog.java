package com.aquilaoffice.integration;

import java.util.List;

public final class IntegrationScenarioRouteCatalog {

    public List<IntegrationScenarioRouteStep> steps() {
        return List.of(
                new IntegrationScenarioRouteStep(
                        "login",
                        "POST",
                        "/api/v1/auth/google/login",
                        "back/src/main/java/com/aquilaoffice/adapter/in/web/auth/AuthController.java",
                        List.of("@RequestMapping(\"/api/v1/auth\")", "@PostMapping(\"/google/login\")")),
                new IntegrationScenarioRouteStep(
                        "workspace",
                        "POST",
                        "/api/v1/workspaces",
                        "back/src/main/java/com/aquilaoffice/adapter/in/web/workspace/WorkspaceController.java",
                        List.of("@RequestMapping(\"/api/v1/workspaces\")", "@PostMapping")),
                new IntegrationScenarioRouteStep(
                        "agent",
                        "POST",
                        "/api/v1/workspaces/{workspaceId}/agents",
                        "back/src/main/java/com/aquilaoffice/adapter/in/web/agent/WorkspaceAgentController.java",
                        List.of(
                                "@RequestMapping(\"/api/v1/workspaces/{workspaceId}/agents\")",
                                "@PostMapping")),
                new IntegrationScenarioRouteStep(
                        "task",
                        "POST",
                        "/api/v1/workspaces/{workspaceId}/tasks/run",
                        "back/src/main/java/com/aquilaoffice/adapter/in/web/task/TaskController.java",
                        List.of(
                                "@RequestMapping(\"/api/v1/workspaces/{workspaceId}/tasks\")",
                                "@PostMapping(\"/run\")")),
                new IntegrationScenarioRouteStep(
                        "report",
                        "GET",
                        "/api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports",
                        "back/src/main/java/com/aquilaoffice/adapter/in/web/task/TaskReportController.java",
                        List.of(
                                "@RequestMapping(\"/api/v1/workspaces/{workspaceId}/tasks\")",
                                "@GetMapping(\"/{taskId}/reports\")")));
    }

    public record IntegrationScenarioRouteStep(
            String id,
            String method,
            String path,
            String controllerSourcePath,
            List<String> sourceRouteFragments) {
    }
}
