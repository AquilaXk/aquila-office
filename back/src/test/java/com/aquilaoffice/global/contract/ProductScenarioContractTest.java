package com.aquilaoffice.global.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductScenarioContractTest {

    @Test
    void frontendContractFixtureListsBackendCriticalApiPaths() throws Exception {
        Path contractPath = Path.of(System.getProperty("user.dir"))
                .getParent()
                .resolve("front/lib/product-contract.ts");
        String source = Files.readString(contractPath);

        assertThat(source)
                .contains("/api/v1/auth/google/login")
                .contains("/api/v1/workspaces")
                .contains("/api/v1/invites/:token/accept")
                .contains("/api/v1/workspaces/:workspaceId/agents")
                .contains("/api/v1/workspaces/:workspaceId/chat/messages")
                .contains("/api/v1/workspaces/:workspaceId/artifacts/orchestration-plans/:planId/steps/:stepId")
                .contains("TOKEN_EXPIRED")
                .contains("GATEWAY_FAILURE")
                .contains("EMPTY_STATE");
    }
}
