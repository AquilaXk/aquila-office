package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseQueryIndexConfigurationTest {

    @Test
    void fkBackedQueriesHaveExplicitIndexes() throws IOException {
        String indexSql = Files.readString(repositoryRoot().resolve(
                "back/src/main/resources/db/migration/V3__add_query_indexes.sql"));

        List<String> requiredIndexes = List.of(
                "idx_workspaces_created_by_member_id",
                "idx_workspace_members_member_id",
                "idx_workspace_invites_workspace_id",
                "idx_workspace_gateway_bindings_workspace_id",
                "idx_github_credentials_workspace_id",
                "idx_slack_channel_mappings_workspace_id",
                "idx_workspace_agents_workspace_id",
                "idx_workspace_agent_skill_files_agent_id",
                "idx_task_executions_task_id",
                "idx_chat_sessions_workspace_id",
                "idx_chat_messages_workspace_id",
                "idx_orchestration_sessions_workspace_id",
                "idx_orchestration_steps_session_id");

        assertThat(indexSql).contains(requiredIndexes.toArray(String[]::new));
    }

    @Test
    void highVolumeTimelineQueriesHaveCompositeIndexes() throws IOException {
        String indexSql = Files.readString(repositoryRoot().resolve(
                "back/src/main/resources/db/migration/V3__add_query_indexes.sql"));

        assertThat(indexSql)
                .contains("idx_chat_messages_session_created_at")
                .contains("idx_task_execution_logs_execution_created_at")
                .contains("idx_tasks_workspace_status")
                .contains("idx_task_executions_workspace_status");
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
